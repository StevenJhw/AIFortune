package com.fortune.ai.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fortune.ai.data.api.DeepSeekApi
import com.fortune.ai.data.model.FortuneMethod
import com.fortune.ai.data.model.FortuneResult
import com.fortune.ai.data.model.UserProfile
import com.fortune.ai.data.remote.SupabaseClient
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class MainViewModel : ViewModel() {

    private val api = DeepSeekApi()
    private val apiSemaphore = Semaphore(5)

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    private val _results = MutableStateFlow<Map<FortuneMethod, FortuneResult>>(emptyMap())
    val results: StateFlow<Map<FortuneMethod, FortuneResult>> = _results

    private val _overallSummary = MutableStateFlow<String?>(null)
    val overallSummary: StateFlow<String?> = _overallSummary

    private val _isGeneratingSummary = MutableStateFlow(false)
    val isGeneratingSummary: StateFlow<Boolean> = _isGeneratingSummary

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun hasSavedData(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val data = SupabaseClient.loadUserData()
                if (data != null && data.results.isNotEmpty()) {
                    _profile.value = data.profile
                    _results.value = data.results
                    _overallSummary.value = data.summary
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    fun setProfile(profile: UserProfile) {
        _profile.value = profile
        viewModelScope.launch {
            try {
                SupabaseClient.saveUserData(profile, emptyMap(), null)
            } catch (_: Exception) {}
        }
        startFullReading(profile)
    }

    fun updateProfile(profile: UserProfile) {
        _profile.value = profile
        _overallSummary.value = null
        viewModelScope.launch {
            try {
                SupabaseClient.deleteAllInteractiveHistory()
                SupabaseClient.deleteAllChatHistory()
            } catch (_: Exception) {}
        }
        startFullReading(profile)
    }

    fun reRunFullReading() {
        val profile = _profile.value ?: return
        _overallSummary.value = null
        startFullReading(profile)
    }

    private fun startFullReading(profile: UserProfile) {
        val methods = FortuneMethod.autoMethods()

        _results.value = methods.associateWith { FortuneResult(method = it, isLoading = true) }

        viewModelScope.launch {
            val deferreds = methods.map { method ->
                async {
                    apiSemaphore.withPermit {
                        try {
                            val result = api.divine(method, profile)
                            val parts = result.split("---")
                            val summary = parts.getOrElse(0) { "" }.trim()
                            val plainSummary = if (parts.size >= 3) parts[1].trim() else ""
                            val detail = if (parts.size >= 3) parts.drop(2).joinToString("---").trim()
                                         else parts.getOrElse(1) { result }.trim()
                            method to FortuneResult(method, summary, plainSummary, detail, isLoading = false, isComplete = true)
                        } catch (e: Exception) {
                            method to FortuneResult(method, "算命失败: ${e.message}", "", "", isLoading = false, isComplete = true)
                        }
                    }
                }
            }

            deferreds.forEach { deferred ->
                val (method, result) = deferred.await()
                _results.value = _results.value.toMutableMap().apply { put(method, result) }
            }

            try {
                SupabaseClient.saveUserData(profile, _results.value, null)
            } catch (_: Exception) {}

            generateOverallSummary(profile)
        }
    }

    private suspend fun generateOverallSummary(profile: UserProfile) {
        _isGeneratingSummary.value = true
        try {
            val completedResults = _results.value
                .filter { it.value.isComplete && it.value.detail.isNotBlank() }
                .mapValues { "${it.value.summary}\n${it.value.detail}" }
            val summary = api.generateSummary(completedResults, profile)
            _overallSummary.value = summary

            try {
                SupabaseClient.saveUserData(profile, _results.value, summary)
            } catch (_: Exception) {}
        } catch (e: Exception) {
            _overallSummary.value = "总结生成失败: ${e.message}"
        }
        _isGeneratingSummary.value = false
    }

    fun divineInteractive(method: FortuneMethod, question: String, extra: String, onResult: (String) -> Unit) {
        val profile = _profile.value ?: return
        viewModelScope.launch {
            try {
                val result = api.divine(method, profile, question, extra)
                try {
                    SupabaseClient.saveInteractiveRecord(method, question, result)
                } catch (_: Exception) {}
                onResult(result)
            } catch (e: Exception) {
                onResult("占卜失败: ${e.message}")
            }
        }
    }

    fun chat(method: FortuneMethod, context: String, userQuestion: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val reply = api.chat(method, context, userQuestion)
                onResult(reply)
            } catch (e: Exception) {
                onResult("回复失败: ${e.message}")
            }
        }
    }
}
