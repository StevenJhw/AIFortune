package com.fortune.ai.data.api

import android.content.Context
import android.content.SharedPreferences

object ApiKeyStore {
    private const val PREFS_NAME = "ai_fortune_prefs"
    private const val KEY_DEEPSEEK = "deepseek_api_key"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getApiKey(): String {
        return prefs?.getString(KEY_DEEPSEEK, "") ?: ""
    }

    fun setApiKey(key: String) {
        prefs?.edit()?.putString(KEY_DEEPSEEK, key)?.apply()
    }

    fun hasApiKey(): Boolean {
        return getApiKey().isNotBlank()
    }
}
