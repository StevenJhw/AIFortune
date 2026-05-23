package com.fortune.ai.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fortune.ai.data.api.DeepSeekApi
import com.fortune.ai.data.model.FortuneMethod
import com.fortune.ai.data.model.FortuneResult
import com.fortune.ai.data.model.UserProfile
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val api = DeepSeekApi()

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    private val _results = MutableStateFlow<Map<FortuneMethod, FortuneResult>>(emptyMap())
    val results: StateFlow<Map<FortuneMethod, FortuneResult>> = _results

    private val _overallSummary = MutableStateFlow<String?>(null)
    val overallSummary: StateFlow<String?> = _overallSummary

    private val _isGeneratingSummary = MutableStateFlow(false)
    val isGeneratingSummary: StateFlow<Boolean> = _isGeneratingSummary

    fun setProfile(profile: UserProfile) {
        _profile.value = profile
        startFullReading(profile)
    }

    private fun startFullReading(profile: UserProfile) {
        val methods = FortuneMethod.autoMethods()

        // Init all as loading
        _results.value = methods.associateWith { FortuneResult(method = it, isLoading = true) }

        // Launch all in parallel
        viewModelScope.launch {
            val deferreds = methods.map { method ->
                async {
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

            deferreds.forEach { deferred ->
                val (method, result) = deferred.await()
                _results.value = _results.value.toMutableMap().apply { put(method, result) }
            }

            // Generate overall summary after all complete
            generateOverallSummary(profile)
        }
    }

    private fun generateOverallSummary(profile: UserProfile) {
        viewModelScope.launch {
            _isGeneratingSummary.value = true
            try {
                val completedResults = _results.value
                    .filter { it.value.isComplete && it.value.detail.isNotBlank() }
                    .mapValues { "${it.value.summary}\n${it.value.detail}" }
                _overallSummary.value = api.generateSummary(completedResults, profile)
            } catch (e: Exception) {
                _overallSummary.value = "总结生成失败: ${e.message}"
            }
            _isGeneratingSummary.value = false
        }
    }

    fun divineInteractive(method: FortuneMethod, question: String, extra: String, onResult: (String) -> Unit) {
        val profile = _profile.value ?: return
        viewModelScope.launch {
            try {
                val result = api.divine(method, profile, question, extra)
                onResult(result)
            } catch (e: Exception) {
                onResult("占卜失败: ${e.message}")
            }
        }
    }
}
