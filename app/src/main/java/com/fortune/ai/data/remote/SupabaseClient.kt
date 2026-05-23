package com.fortune.ai.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.fortune.ai.data.model.FortuneMethod
import com.fortune.ai.data.model.FortuneResult
import com.fortune.ai.data.model.UserProfile
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object SupabaseClient {

    private const val BASE_URL = "https://pndotictelxkwtvqfyep.supabase.co/rest/v1"
    private const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBuZG90aWN0ZWx4a3d0dnFmeWVwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk1NDk1OTQsImV4cCI6MjA5NTEyNTU5NH0.7w8fKNdIlyFaFwZiOFyxKvhOtOIxY08-NgCWtGck0WI"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private var androidId: String = ""

    @SuppressLint("HardwareIds")
    fun init(context: Context) {
        androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun getAndroidId(): String = androidId

    suspend fun saveUserData(profile: UserProfile, results: Map<FortuneMethod, FortuneResult>, summary: String?) {
        withContext(Dispatchers.IO) {
            val resultsMap = results.map { (k, v) -> k.name to v }.toMap()
            val timestamp = java.time.OffsetDateTime.now().toString()
            val body = mapOf(
                "android_id" to androidId,
                "profile" to profile,
                "results" to resultsMap,
                "overall_summary" to summary,
                "updated_at" to timestamp
            )
            post("$BASE_URL/user_data?on_conflict=android_id", body, "resolution=merge-duplicates")
        }
    }

    suspend fun loadUserData(): UserDataResponse? {
        return withContext(Dispatchers.IO) {
            val body = get("$BASE_URL/user_data?android_id=eq.$androidId&select=*") ?: return@withContext null

            val type = object : TypeToken<List<UserDataRaw>>() {}.type
            val list: List<UserDataRaw> = gson.fromJson(body, type) ?: return@withContext null
            val raw = list.firstOrNull() ?: return@withContext null

            val profile = gson.fromJson(gson.toJson(raw.profile), UserProfile::class.java)
            val resultsType = object : TypeToken<Map<String, FortuneResult>>() {}.type
            val resultsRaw: Map<String, FortuneResult> = gson.fromJson(gson.toJson(raw.results), resultsType) ?: emptyMap()
            val results = resultsRaw.mapNotNull { (key, value) ->
                try { FortuneMethod.valueOf(key) to value } catch (_: Exception) { null }
            }.toMap()

            UserDataResponse(profile, results, raw.overall_summary)
        }
    }

    suspend fun saveInteractiveRecord(method: FortuneMethod, question: String, result: String) {
        withContext(Dispatchers.IO) {
            val body = mapOf(
                "android_id" to androidId,
                "method" to method.name,
                "question" to question,
                "result" to result
            )
            post("$BASE_URL/interactive_history", body)
        }
    }

    suspend fun deleteAllInteractiveHistory() {
        withContext(Dispatchers.IO) {
            delete("$BASE_URL/interactive_history?android_id=eq.$androidId")
        }
    }

    suspend fun deleteInteractiveRecord(id: Long) {
        withContext(Dispatchers.IO) {
            delete("$BASE_URL/interactive_history?id=eq.$id")
        }
    }

    suspend fun loadInteractiveHistory(method: FortuneMethod? = null): List<InteractiveHistoryRecord> {
        return withContext(Dispatchers.IO) {
            val filter = if (method != null) "&method=eq.${method.name}" else ""
            val body = get("$BASE_URL/interactive_history?android_id=eq.$androidId$filter&order=created_at.desc&limit=20")
                ?: return@withContext emptyList()

            val type = object : TypeToken<List<InteractiveHistoryRecord>>() {}.type
            gson.fromJson(body, type) ?: emptyList()
        }
    }

    suspend fun saveChatMessage(method: FortuneMethod, content: String, isUser: Boolean) {
        withContext(Dispatchers.IO) {
            val body = mapOf(
                "android_id" to androidId,
                "method" to method.name,
                "content" to content,
                "is_user" to isUser
            )
            post("$BASE_URL/chat_history", body)
        }
    }

    suspend fun loadChatHistory(method: FortuneMethod): List<ChatHistoryRecord> {
        return withContext(Dispatchers.IO) {
            val body = get("$BASE_URL/chat_history?android_id=eq.$androidId&method=eq.${method.name}&order=created_at.asc")
                ?: return@withContext emptyList()

            val type = object : TypeToken<List<ChatHistoryRecord>>() {}.type
            gson.fromJson(body, type) ?: emptyList()
        }
    }

    suspend fun deleteChatHistory(method: FortuneMethod) {
        withContext(Dispatchers.IO) {
            delete("$BASE_URL/chat_history?android_id=eq.$androidId&method=eq.${method.name}")
        }
    }

    suspend fun deleteAllChatHistory() {
        withContext(Dispatchers.IO) {
            delete("$BASE_URL/chat_history?android_id=eq.$androidId")
        }
    }

    private fun get(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", ANON_KEY)
            .addHeader("Authorization", "Bearer $ANON_KEY")
            .get()
            .build()

        val response = client.newCall(request).execute()
        return if (response.isSuccessful) response.body?.string() else null
    }

    private fun post(url: String, body: Map<String, Any?>, prefer: String? = null) {
        val json = gson.toJson(body)
        val builder = Request.Builder()
            .url(url)
            .addHeader("apikey", ANON_KEY)
            .addHeader("Authorization", "Bearer $ANON_KEY")
            .addHeader("Content-Type", "application/json")
            .post(json.toRequestBody("application/json".toMediaType()))

        if (prefer != null) {
            builder.addHeader("Prefer", prefer)
        }

        client.newCall(builder.build()).execute()
    }

    private fun delete(url: String) {
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", ANON_KEY)
            .addHeader("Authorization", "Bearer $ANON_KEY")
            .delete()
            .build()

        client.newCall(request).execute()
    }
}

data class UserDataRaw(
    val android_id: String = "",
    val profile: Any? = null,
    val results: Any? = null,
    val overall_summary: String? = null
)

data class UserDataResponse(
    val profile: UserProfile,
    val results: Map<FortuneMethod, FortuneResult>,
    val summary: String?
)

data class InteractiveHistoryRecord(
    val id: Long = 0,
    @SerializedName("android_id") val androidId: String = "",
    val method: String = "",
    val question: String = "",
    val result: String = "",
    @SerializedName("created_at") val createdAt: String = ""
)

data class ChatHistoryRecord(
    val id: Long = 0,
    @SerializedName("android_id") val androidId: String = "",
    val method: String = "",
    val content: String = "",
    @SerializedName("is_user") val isUser: Boolean = false,
    @SerializedName("created_at") val createdAt: String = ""
)
