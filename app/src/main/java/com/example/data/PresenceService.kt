package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID

object PresenceService {
    private const val TAG = "PresenceService"
    private const val PRESENCE_DB_URL = "https://loop-web-7e263-default-rtdb.firebaseio.com"

    private val client = OkHttpClient.Builder()
        .proxy(java.net.Proxy.NO_PROXY)
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private var currentSessionId: String? = null

    fun getOrCreateSessionId(context: Context): String {
        if (currentSessionId != null) return currentSessionId!!
        val prefs = context.getSharedPreferences("loop_presence_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("presence_session_id", null)
        if (id.isNullOrBlank()) {
            id = "user_" + UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString("presence_session_id", id).apply()
        }
        currentSessionId = id
        return id
    }

    /**
     * Sends a real heartbeat indicating that this user/device is active right now in the app.
     */
    suspend fun sendHeartbeat(context: Context, username: String = ""): Boolean = withContext(Dispatchers.IO) {
        val sessionId = getOrCreateSessionId(context)
        val now = System.currentTimeMillis()
        val json = JSONObject().apply {
            put("id", sessionId)
            put("username", username.ifEmpty { "User_$sessionId" })
            put("timestamp", now)
            put("lastSeen", now)
            put("platform", "android")
        }
        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
        var anySuccess = false

        // Write to secondary DB
        try {
            val url = "$PRESENCE_DB_URL/online_presence/$sessionId.json"
            val request = Request.Builder().url(url).put(body).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) anySuccess = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send heartbeat to secondary DB: ${e.message}")
        }

        // Write to primary DB if available
        try {
            val primaryUrl = "${FirebaseService.RTDB_URL}/online_presence/$sessionId.json"
            val request = Request.Builder().url(primaryUrl).put(body).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) anySuccess = true
            }
        } catch (_: Exception) {
        }

        return@withContext anySuccess
    }

    /**
     * Queries the actual live online users from the database.
     * Counts all active sessions with activity in the last 10 minutes (to be safe with clock skew).
     * Merges sessions from both primary and secondary databases to get the full accurate count.
     */
    suspend fun getLiveOnlineCount(): Int = withContext(Dispatchers.IO) {
        val activeSessions = mutableSetOf<String>()
        val now = System.currentTimeMillis()
        // Active window of 10 minutes (600,000ms) with clock skew tolerance
        val activeWindowMs = 600_000L

        // 1. Check secondary DB
        try {
            val url = "$PRESENCE_DB_URL/online_presence.json"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()?.trim() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null" && bodyStr != "{}") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val obj = json.optJSONObject(key)
                            if (obj != null) {
                                val ts = obj.optLong("timestamp", 0L)
                                val lastSeen = obj.optLong("lastSeen", ts)
                                val effectiveTs = if (lastSeen > ts) lastSeen else ts
                                val diff = Math.abs(now - effectiveTs)
                                if (diff < activeWindowMs) {
                                    activeSessions.add(key)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch live online count from secondary DB: ${e.message}")
        }

        // 2. Also check primary DB and merge
        try {
            val primaryUrl = "${FirebaseService.RTDB_URL}/online_presence.json"
            val request = Request.Builder().url(primaryUrl).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()?.trim() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null" && bodyStr != "{}") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val obj = json.optJSONObject(key)
                            if (obj != null) {
                                val ts = obj.optLong("timestamp", 0L)
                                val lastSeen = obj.optLong("lastSeen", ts)
                                val effectiveTs = if (lastSeen > ts) lastSeen else ts
                                val diff = Math.abs(now - effectiveTs)
                                if (diff < activeWindowMs) {
                                    activeSessions.add(key)
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        val totalActive = activeSessions.size
        // Ensure at least 1 (the current active user)
        return@withContext if (totalActive > 0) totalActive else 1
    }

    /**
     * Removes presence when user explicitly logs out or closes app.
     */
    suspend fun removePresence(context: Context): Boolean = withContext(Dispatchers.IO) {
        val sessionId = getOrCreateSessionId(context)
        var removed = false
        try {
            val url = "$PRESENCE_DB_URL/online_presence/$sessionId.json"
            val request = Request.Builder().url(url).delete().build()
            client.newCall(request).execute().use { response -> 
                if (response.isSuccessful) removed = true 
            }
        } catch (_: Exception) {}

        try {
            val primaryUrl = "${FirebaseService.RTDB_URL}/online_presence/$sessionId.json"
            val request = Request.Builder().url(primaryUrl).delete().build()
            client.newCall(request).execute().use { response -> 
                if (response.isSuccessful) removed = true 
            }
        } catch (_: Exception) {}

        return@withContext removed
    }
}
