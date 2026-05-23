package com.fortune.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import com.fortune.ai.data.model.FortuneMethod
import com.fortune.ai.data.model.FortuneResult
import com.fortune.ai.data.model.UserProfile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object LocalStorage {
    private const val PREFS_NAME = "ai_fortune_data"
    private const val KEY_PROFILE = "user_profile"
    private const val KEY_RESULTS = "fortune_results"
    private const val KEY_SUMMARY = "overall_summary"
    private const val KEY_INTERACTIVE_HISTORY = "interactive_history"

    private var prefs: SharedPreferences? = null
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Profile
    fun saveProfile(profile: UserProfile) {
        prefs?.edit()?.putString(KEY_PROFILE, gson.toJson(profile))?.apply()
    }

    fun getProfile(): UserProfile? {
        val json = prefs?.getString(KEY_PROFILE, null) ?: return null
        return gson.fromJson(json, UserProfile::class.java)
    }

    fun hasProfile(): Boolean = getProfile() != null

    // Fortune results
    fun saveResults(results: Map<FortuneMethod, FortuneResult>) {
        val serializable = results.map { (k, v) -> k.name to v }.toMap()
        prefs?.edit()?.putString(KEY_RESULTS, gson.toJson(serializable))?.apply()
    }

    fun getResults(): Map<FortuneMethod, FortuneResult> {
        val json = prefs?.getString(KEY_RESULTS, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, FortuneResult>>() {}.type
        val raw: Map<String, FortuneResult> = gson.fromJson(json, type)
        return raw.mapNotNull { (key, value) ->
            try {
                FortuneMethod.valueOf(key) to value
            } catch (e: Exception) {
                null
            }
        }.toMap()
    }

    // Overall summary
    fun saveSummary(summary: String) {
        prefs?.edit()?.putString(KEY_SUMMARY, summary)?.apply()
    }

    fun getSummary(): String? {
        return prefs?.getString(KEY_SUMMARY, null)
    }

    // Interactive history
    fun saveInteractiveResult(method: FortuneMethod, question: String, result: String) {
        val history = getInteractiveHistory().toMutableList()
        history.add(0, InteractiveRecord(method.name, question, result, System.currentTimeMillis()))
        // Keep max 20 records
        val trimmed = history.take(20)
        prefs?.edit()?.putString(KEY_INTERACTIVE_HISTORY, gson.toJson(trimmed))?.apply()
    }

    fun getInteractiveHistory(): List<InteractiveRecord> {
        val json = prefs?.getString(KEY_INTERACTIVE_HISTORY, null) ?: return emptyList()
        val type = object : TypeToken<List<InteractiveRecord>>() {}.type
        return gson.fromJson(json, type)
    }

    fun getInteractiveHistoryForMethod(method: FortuneMethod): List<InteractiveRecord> {
        return getInteractiveHistory().filter { it.methodName == method.name }
    }

    // Clear all
    fun clearAll() {
        prefs?.edit()?.clear()?.apply()
    }
}

data class InteractiveRecord(
    val methodName: String,
    val question: String,
    val result: String,
    val timestamp: Long
)
