package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

data class ChatMessage(
    val id: String = "",
    val sender: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val replyToId: String? = null,
    val replyToSender: String? = null,
    val replyToText: String? = null,
    val reactions: Map<String, String> = emptyMap() // username -> emoji
)

data class GlobalChatMessageItem(
    val id: String = "",
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val photoURL: String = "",
    val text: String = "",
    val createdAt: Long = 0L,
    val type: String = "text"
)

data class GlobalChatUserItem(
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val photoURL: String = "",
    val bio: String = "",
    val online: Boolean = false,
    val lastSeen: Long = 0L,
    val createdAt: Long = 0L
)

object ChatService {
    private const val TAG = "ChatService"
    private const val BASE_URL = "https://loop-7e3d9-default-rtdb.firebaseio.com"
    private const val GLOBAL_CHAT_DB = "https://loop-web-7e263-default-rtdb.firebaseio.com"

    val client = OkHttpClient.Builder()
        .proxy(java.net.Proxy.NO_PROXY)
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * Fetches all global chat messages from the primary web RTDB.
     */
    suspend fun fetchAllGlobalChatMessages(): List<GlobalChatMessageItem> = withContext(Dispatchers.IO) {
        val url = "https://loop-web-7e263-default-rtdb.firebaseio.com/globalMessages.json"
        val list = mutableListOf<GlobalChatMessageItem>()
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val obj = json.optJSONObject(key)
                            if (obj != null) {
                                list.add(
                                    GlobalChatMessageItem(
                                        id = key,
                                        uid = obj.optString("uid", ""),
                                        name = obj.optString("name", "مستخدم"),
                                        username = obj.optString("username", ""),
                                        photoURL = obj.optString("photoURL", ""),
                                        text = obj.optString("text", ""),
                                        createdAt = obj.optLong("createdAt", 0L),
                                        type = obj.optString("type", "text")
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all global chat messages", e)
        }
        return@withContext list.sortedByDescending { it.createdAt }
    }

    /**
     * Deletes a single global chat message by its Firebase key.
     */
    suspend fun deleteGlobalChatMessage(messageId: String): Boolean = withContext(Dispatchers.IO) {
        val url = "$GLOBAL_CHAT_DB/globalMessages/$messageId.json"
        try {
            val request = Request.Builder().url(url).delete().build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting global message $messageId", e)
        }
        return@withContext false
    }

    /**
     * Wipes and completely resets all global chat messages from Firebase Database.
     */
    suspend fun resetAndWipeAllGlobalChatMessages(): Boolean = withContext(Dispatchers.IO) {
        val apiKey = "AIzaSyATpsWchCVsSj0uay010wAlpKeHZD_rFQQ"
        val email = "dev_wiper@loopchat.app"
        val pass = "wiper123456"
        var idToken: String? = null

        try {
            // 1. Try to login
            val loginJson = JSONObject().apply {
                put("email", email)
                put("password", pass)
                put("returnSecureToken", true)
            }
            val loginReq = Request.Builder()
                .url("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey")
                .post(loginJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            var responseBody = client.newCall(loginReq).execute().use { it.body?.string() }
            if (responseBody != null && responseBody.contains("idToken")) {
                idToken = JSONObject(responseBody).getString("idToken")
            } else {
                // 2. Try to signup if login failed
                val signupReq = Request.Builder()
                    .url("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey")
                    .post(loginJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                responseBody = client.newCall(signupReq).execute().use { it.body?.string() }
                if (responseBody != null && responseBody.contains("idToken")) {
                    idToken = JSONObject(responseBody).getString("idToken")
                }
            }

            if (idToken != null) {
                // 3. Delete messages using the token
                val url = "https://loop-web-7e263-default-rtdb.firebaseio.com/globalMessages.json?auth=$idToken"
                val request = Request.Builder().url(url).delete().build()
                client.newCall(request).execute().use { response ->
                    return@withContext response.isSuccessful
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error wiping all global chat messages", e)
        }
        return@withContext false
    }

    /**
     * Posts an official announcement into the global chat room on behalf of management.
     */
    suspend fun postGlobalAdminBroadcast(text: String, adminName: String = "إدارة +90"): Boolean = withContext(Dispatchers.IO) {
        val apiKey = "AIzaSyATpsWchCVsSj0uay010wAlpKeHZD_rFQQ"
        val email = "dev_wiper@loopchat.app"
        val pass = "wiper123456"
        var idToken: String? = null

        try {
            // 1. Try to login
            val loginJson = JSONObject().apply {
                put("email", email)
                put("password", pass)
                put("returnSecureToken", true)
            }
            val loginReq = Request.Builder()
                .url("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey")
                .post(loginJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            var responseBody = client.newCall(loginReq).execute().use { it.body?.string() }
            if (responseBody != null && responseBody.contains("idToken")) {
                idToken = JSONObject(responseBody).getString("idToken")
            } else {
                val signupReq = Request.Builder()
                    .url("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey")
                    .post(loginJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                responseBody = client.newCall(signupReq).execute().use { it.body?.string() }
                if (responseBody != null && responseBody.contains("idToken")) {
                    idToken = JSONObject(responseBody).getString("idToken")
                }
            }

            if (idToken != null) {
                val url = "https://loop-web-7e263-default-rtdb.firebaseio.com/globalMessages.json?auth=$idToken"
                val json = JSONObject().apply {
                    put("uid", "admin_system_official")
                    put("name", "🛡️ $adminName")
                    put("username", "admin")
                    put("photoURL", "https://i.ibb.co/VWHpjwq9/IMG-979.jpg")
                    put("text", text)
                    put("timestamp", System.currentTimeMillis())
                    put("type", "text")
                }
                val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
                val request = Request.Builder().url(url).post(body).build()
                client.newCall(request).execute().use { response ->
                    return@withContext response.isSuccessful
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error posting admin broadcast message", e)
        }
        return@withContext false
    }

    /**
     * Fetches all registered users from the global chat database.
     */
    suspend fun fetchAllGlobalChatUsers(): List<GlobalChatUserItem> = withContext(Dispatchers.IO) {
        val url = "$GLOBAL_CHAT_DB/users.json"
        val list = mutableListOf<GlobalChatUserItem>()
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val obj = json.optJSONObject(key)
                            if (obj != null) {
                                list.add(
                                    GlobalChatUserItem(
                                        uid = key,
                                        name = obj.optString("name", "مستخدم"),
                                        username = obj.optString("username", ""),
                                        photoURL = obj.optString("photoURL", ""),
                                        bio = obj.optString("bio", ""),
                                        online = obj.optBoolean("online", false),
                                        lastSeen = obj.optLong("lastSeen", 0L),
                                        createdAt = obj.optLong("createdAt", 0L)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all global chat users", e)
        }
        return@withContext list.sortedByDescending { it.lastSeen }
    }

    /**
     * Deletes a user profile and username reservation from the database.
     */
    suspend fun deleteGlobalChatUser(uid: String, username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val userUrl = "$GLOBAL_CHAT_DB/users/$uid.json"
            val userReq = Request.Builder().url(userUrl).delete().build()
            client.newCall(userReq).execute().close()

            if (username.isNotEmpty()) {
                val unameUrl = "$GLOBAL_CHAT_DB/usernames/${username.lowercase()}.json"
                val unameReq = Request.Builder().url(unameUrl).delete().build()
                client.newCall(unameReq).execute().close()
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting chat user $uid", e)
        }
        return@withContext false
    }

    /**
     * Wipes all 1v1 private conversations and history from the database.
     */
    suspend fun wipeAllPrivateConversations(): Boolean = withContext(Dispatchers.IO) {
        val apiKey = "AIzaSyATpsWchCVsSj0uay010wAlpKeHZD_rFQQ"
        val email = "dev_wiper@loopchat.app"
        val pass = "wiper123456"
        var idToken: String? = null

        try {
            val loginJson = JSONObject().apply {
                put("email", email)
                put("password", pass)
                put("returnSecureToken", true)
            }
            val loginReq = Request.Builder()
                .url("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey")
                .post(loginJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            var responseBody = client.newCall(loginReq).execute().use { it.body?.string() }
            if (responseBody != null && responseBody.contains("idToken")) {
                idToken = JSONObject(responseBody).getString("idToken")
            } else {
                val signupReq = Request.Builder()
                    .url("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey")
                    .post(loginJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                responseBody = client.newCall(signupReq).execute().use { it.body?.string() }
                if (responseBody != null && responseBody.contains("idToken")) {
                    idToken = JSONObject(responseBody).getString("idToken")
                }
            }

            if (idToken != null) {
                val convUrl = "$GLOBAL_CHAT_DB/conversations.json?auth=$idToken"
                val req1 = Request.Builder().url(convUrl).delete().build()
                client.newCall(req1).execute().close()

                val privateUrl = "$GLOBAL_CHAT_DB/privateChats.json?auth=$idToken"
                val req2 = Request.Builder().url(privateUrl).delete().build()
                client.newCall(req2).execute().close()
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error wiping private conversations", e)
        }
        return@withContext false
    }

    /**
     * Checks if the chat is enabled globally by the developer.
     */
    suspend fun isChatEnabled(): Boolean = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/settings/chat_enabled.json"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()?.trim() ?: "true"
                    if (bodyStr.isEmpty() || bodyStr == "null") {
                        return@withContext true
                    }
                    return@withContext bodyStr.toBoolean()
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Chat enabled status check offline or unavailable, defaulting to true")
        }
        return@withContext true
    }

    /**
     * Enables or disables the chat globally.
     */
    suspend fun setChatEnabled(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/settings/chat_enabled.json"
        try {
            val body = enabled.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).put(body).build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting chat enabled status", e)
        }
        return@withContext false
    }

    /**
     * Deletes a specific message from a channel.
     */
    suspend fun deleteMessage(channelId: String, messageId: String): Boolean = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/chats/$channelId/$messageId.json"
        try {
            val request = Request.Builder().url(url).delete().build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message $messageId", e)
        }
        return@withContext false
    }

    /**
     * Posts a new message to a specific channel.
     */
    suspend fun postMessage(
        channelId: String,
        sender: String,
        text: String,
        replyToId: String? = null,
        replyToSender: String? = null,
        replyToText: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/chats/$channelId.json"
        try {
            val json = JSONObject().apply {
                put("sender", sender)
                put("text", text)
                put("timestamp", System.currentTimeMillis())
                if (replyToId != null) {
                    put("replyToId", replyToId)
                    put("replyToSender", replyToSender)
                    put("replyToText", replyToText)
                }
            }
            val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).post(body).build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error posting message", e)
        }
        return@withContext false
    }

    /**
     * Adds or updates a reaction on a message.
     */
    suspend fun addReaction(channelId: String, messageId: String, username: String, emoji: String): Boolean = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/chats/$channelId/$messageId/reactions/$username.json"
        try {
            val body = "\"$emoji\"".toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).put(body).build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding reaction", e)
        }
        return@withContext false
    }

    /**
     * Removes a reaction from a message.
     */
    suspend fun removeReaction(channelId: String, messageId: String, username: String): Boolean = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/chats/$channelId/$messageId/reactions/$username.json"
        try {
            val request = Request.Builder().url(url).delete().build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing reaction", e)
        }
        return@withContext false
    }

    /**
     * Registers a user in the global users directory with optional password and avatar.
     */
    suspend fun registerUser(
        username: String,
        name: String,
        password: String? = null,
        avatar: String? = null,
        role: String = "user"
    ): Boolean = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/users/$username.json"
        try {
            // First, see if user exists so we don't clear their existing password/avatar if not supplied
            val existing = getUserProfile(username)
            val json = JSONObject().apply {
                put("username", username)
                put("name", name)
                put("password", password ?: existing?.optString("password") ?: "")
                put("avatar", avatar ?: existing?.optString("avatar") ?: "neon_wolf")
                put("role", existing?.optString("role") ?: role)
                put("timestamp", System.currentTimeMillis())
            }
            val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).put(body).build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering user", e)
        }
        return@withContext false
    }

    /**
     * Retrieves a single user's profile details.
     */
    suspend fun getUserProfile(username: String): JSONObject? = withContext(Dispatchers.IO) {
        if (username.isBlank()) return@withContext null
        val url = "$BASE_URL/users/${username.trim().lowercase()}.json"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()?.trim() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        return@withContext JSONObject(bodyStr)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile $username", e)
        }
        null
    }

    /**
     * Retrieves all user profiles in the system with their full JSON details.
     */
    suspend fun getAllUserProfiles(): Map<String, JSONObject> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/users.json"
        val map = mutableMapOf<String, JSONObject>()
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val userObj = json.optJSONObject(key) ?: continue
                            map[key] = userObj
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all user profiles", e)
        }
        return@withContext map
    }

    /**
     * Retrieves all registered users from the global directory.
     */
    suspend fun getAllUsers(): Map<String, String> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/users.json"
        val usersMap = mutableMapOf<String, String>()
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val userObj = json.optJSONObject(key) ?: continue
                            usersMap[key] = userObj.optString("name", key)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting users", e)
        }
        return@withContext usersMap
    }

    /**
     * Retrieves messages for a specific channel, automatically deletes messages older than 10 minutes,
     * and returns the sorted active messages.
     */
    suspend fun getMessages(channelId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/chats/$channelId.json"
        val messages = mutableListOf<ChatMessage>()
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val msgObj = json.optJSONObject(key) ?: continue

                            val reactionsMap = mutableMapOf<String, String>()
                            val reactionsObj = msgObj.optJSONObject("reactions")
                            if (reactionsObj != null) {
                                val rKeys = reactionsObj.keys()
                                while (rKeys.hasNext()) {
                                    val rKey = rKeys.next()
                                    reactionsMap[rKey] = reactionsObj.getString(rKey)
                                }
                            }

                            messages.add(
                                ChatMessage(
                                    id = key,
                                    sender = msgObj.optString("sender", "مجهول"),
                                    text = msgObj.optString("text", ""),
                                    timestamp = msgObj.optLong("timestamp", 0L),
                                    replyToId = msgObj.optString("replyToId").takeIf { it.isNotEmpty() },
                                    replyToSender = msgObj.optString("replyToSender").takeIf { it.isNotEmpty() },
                                    replyToText = msgObj.optString("replyToText").takeIf { it.isNotEmpty() },
                                    reactions = reactionsMap
                                )
                            )
                        }
                    }
                }
            }

            // Perform automatic deletion of messages older than 24 hours (86,400,000 ms) only for public/general chats
            val isPrivateChat = channelId.startsWith("private_")
            if (!isPrivateChat) {
                val twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
                val oldMessages = messages.filter { it.timestamp < twentyFourHoursAgo }
                for (oldMsg in oldMessages) {
                    deleteMessage(channelId, oldMsg.id)
                }
                return@withContext messages.filter { it.timestamp >= twentyFourHoursAgo }.sortedBy { it.timestamp }
            } else {
                return@withContext messages.sortedBy { it.timestamp }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting messages for channel $channelId", e)
        }
        return@withContext emptyList()
    }

    /**
     * Deletes all messages from all channels.
     */
    suspend fun deleteAllMessages(): Boolean = withContext(Dispatchers.IO) {
        val url = "https://loop-web-7e263-default-rtdb.firebaseio.com/globalMessages.json"
        try {
            val request = Request.Builder().url(url).delete().build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error wiping chat database", e)
        }
        return@withContext false
    }

    /**
     * Retrieves all active chats for notification checks.
     */
    suspend fun getAllChats(): Map<String, Map<String, Any>>? = withContext(Dispatchers.IO) {
        val url = "https://loop-web-7e263-default-rtdb.firebaseio.com/globalMessages.json"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val result = mutableMapOf<String, Map<String, Any>>()
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val innerObj = json.optJSONObject(key) ?: continue
                            val innerMap = mutableMapOf<String, Any>()
                            val innerKeys = innerObj.keys()
                            while (innerKeys.hasNext()) {
                                val innerKey = innerKeys.next()
                                val msgObj = innerObj.optJSONObject(innerKey)
                                if (msgObj != null) {
                                    val msgMap = mutableMapOf<String, Any>()
                                    val msgKeys = msgObj.keys()
                                    while (msgKeys.hasNext()) {
                                        val mKey = msgKeys.next()
                                        val mVal = msgObj.opt(mKey)
                                        if (mVal != null) {
                                            msgMap[mKey] = mVal
                                        }
                                    }
                                    innerMap[innerKey] = msgMap
                                }
                            }
                            result[key] = innerMap
                        }
                        return@withContext result
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all chats", e)
        }
        null
    }

    /**
     * Sets the typing state of a user in a specific channel.
     */
    suspend fun setTypingState(channelId: String, username: String, isTyping: Boolean): Boolean = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/typing/$channelId/$username.json"
        try {
            val body = "$isTyping".toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).put(body).build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting typing state", e)
        }
        return@withContext false
    }

    /**
     * Gets the typing status of users in a specific channel.
     * Returns a map of username -> isTyping
     */
    suspend fun getTypingStates(channelId: String): Map<String, Boolean> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/typing/$channelId.json"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val result = mutableMapOf<String, Boolean>()
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            result[key] = json.optBoolean(key, false)
                        }
                        return@withContext result
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting typing states", e)
        }
        return@withContext emptyMap()
    }

    /**
     * Checks if a username is already registered in the global directory.
     */
    suspend fun isUsernameTaken(username: String): Boolean = withContext(Dispatchers.IO) {
        if (username.isBlank()) return@withContext false
        val url = "$BASE_URL/users/${username.trim().lowercase()}.json"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()?.trim() ?: ""
                    return@withContext bodyStr.isNotEmpty() && bodyStr != "null"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if username is taken", e)
        }
        return@withContext false
    }

    /**
     * Blocks a user.
     */
    suspend fun blockUser(myUsername: String, otherUsername: String): Boolean = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/blocks/$myUsername/$otherUsername.json"
        try {
            val body = "true".toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).put(body).build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error blocking user", e)
        }
        return@withContext false
    }

    /**
     * Unblocks a user.
     */
    suspend fun unblockUser(myUsername: String, otherUsername: String): Boolean = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/blocks/$myUsername/$otherUsername.json"
        try {
            val request = Request.Builder().url(url).delete().build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unblocking user", e)
        }
        return@withContext false
    }

    /**
     * Gets the list of blocked users.
     */
    suspend fun getBlockedUsers(myUsername: String): List<String> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/blocks/$myUsername.json"
        val list = mutableListOf<String>()
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            if (json.optBoolean(key, false)) {
                                list.add(key)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting blocked users", e)
        }
        return@withContext list
    }

    /**
     * Deletes an entire private chat conversation node.
     */
    suspend fun deleteConversation(channelId: String): Boolean = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/chats/$channelId.json"
        try {
            val request = Request.Builder().url(url).delete().build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting conversation $channelId", e)
        }
        return@withContext false
    }

    // --- NATIVE GLOBAL & PRIVATE CHAT INTEGRATION ---

    const val CHAT_API_KEY = "AIzaSyATpsWchCVsSj0uay010wAlpKeHZD_rFQQ"

    data class NativeChatUser(
        val uid: String = "",
        val name: String = "",
        val username: String = "",
        val photoURL: String = "",
        val bio: String = "",
        val online: Boolean = false,
        val lastSeen: Long = 0L,
        val createdAt: Long = 0L
    )

    data class NativeRecentChat(
        val uid: String = "",
        val name: String = "",
        val username: String = "",
        val photoURL: String = "",
        val lastMessage: String = "",
        val updatedAt: Long = 0L
    )

    data class NativeChatMessage(
        val id: String = "",
        val senderId: String = "",
        val senderName: String = "",
        val senderPhoto: String = "",
        val text: String = "",
        val createdAt: Long = 0L,
        val type: String = "text",
        val replyToName: String? = null,
        val replyToText: String? = null,
        val reactions: Map<String, String> = emptyMap() // username -> emoji
    )

    /**
     * Checks if a username is already registered in the GLOBAL_CHAT_DB.
     */
    suspend fun isGlobalUsernameTaken(username: String): Boolean = withContext(Dispatchers.IO) {
        val url = "$GLOBAL_CHAT_DB/usernames/${username.trim().lowercase()}.json"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()?.trim() ?: ""
                    return@withContext bodyStr.isNotEmpty() && bodyStr != "null"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking global username taken", e)
        }
        return@withContext false
    }

    /**
     * Registers a new chat user using Firebase Auth REST API and saves their profile.
     */
    suspend fun registerNativeChatUser(
        username: String,
        name: String,
        password: CharSequence,
        photoURL: String
    ): Result<NativeChatUser> = withContext(Dispatchers.IO) {
        val userLower = username.trim().lowercase()
        val cleanedName = name.trim()
        val cleanedPhoto = photoURL.trim().ifEmpty { "https://i.ibb.co/VWHpjwq9/IMG-979.jpg" }

        // Username containing +90 is blocked
        if (userLower.contains("+90") || cleanedName.contains("+90")) {
            return@withContext Result.failure(Exception("يمنع استخدام الرمز +90 في اسم الحساب أو اسم المستخدم!"))
        }

        try {
            // Check if username is taken
            if (isGlobalUsernameTaken(userLower)) {
                return@withContext Result.failure(Exception("اسم المستخدم مأخوذ بالفعل! يرجى اختيار اسم آخر."))
            }

            // Firebase Auth Sign Up
            val fakeEmail = "$userLower@loopchat.app"
            val signupJson = JSONObject().apply {
                put("email", fakeEmail)
                put("password", password.toString())
                put("returnSecureToken", true)
            }
            val request = Request.Builder()
                .url("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$CHAT_API_KEY")
                .post(signupJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful || !bodyStr.contains("idToken")) {
                    val errMsg = JSONObject(bodyStr).optJSONObject("error")?.optString("message", "") ?: ""
                    val displayErr = if (errMsg == "EMAIL_EXISTS") "اسم المستخدم مأخوذ بالفعل!" else "فشل إنشاء الحساب: $errMsg"
                    return@withContext Result.failure(Exception(displayErr))
                }

                val resJson = JSONObject(bodyStr)
                val uid = resJson.getString("localId")
                val idToken = resJson.getString("idToken")

                // Save to users node
                val userProfile = JSONObject().apply {
                    put("uid", uid)
                    put("name", cleanedName)
                    put("username", username.trim())
                    put("usernameLower", userLower)
                    put("photoURL", cleanedPhoto)
                    put("bio", "مرحباً بك في دردشة +90")
                    put("createdAt", System.currentTimeMillis())
                    put("online", true)
                    put("lastSeen", System.currentTimeMillis())
                }

                // PUT to users/${uid}.json
                val putUserReq = Request.Builder()
                    .url("$GLOBAL_CHAT_DB/users/$uid.json?auth=$idToken")
                    .put(userProfile.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                client.newCall(putUserReq).execute().close()

                // PUT to usernames/${username}.json
                val putUnameReq = Request.Builder()
                    .url("$GLOBAL_CHAT_DB/usernames/$userLower.json?auth=$idToken")
                    .put("\"$uid\"".toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                client.newCall(putUnameReq).execute().close()

                return@withContext Result.success(
                    NativeChatUser(
                        uid = uid,
                        name = cleanedName,
                        username = username.trim(),
                        photoURL = cleanedPhoto,
                        bio = "مرحباً بك في دردشة +90",
                        online = true,
                        lastSeen = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering native user", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Log in native chat user.
     */
    suspend fun loginNativeChatUser(
        username: String,
        password: CharSequence
    ): Result<NativeChatUser> = withContext(Dispatchers.IO) {
        val userLower = username.trim().lowercase()
        try {
            val fakeEmail = "$userLower@loopchat.app"
            val loginJson = JSONObject().apply {
                put("email", fakeEmail)
                put("password", password.toString())
                put("returnSecureToken", true)
            }
            val request = Request.Builder()
                .url("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$CHAT_API_KEY")
                .post(loginJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful || !bodyStr.contains("idToken")) {
                    return@withContext Result.failure(Exception("اسم المستخدم أو كلمة المرور غير صحيحة!"))
                }

                val resJson = JSONObject(bodyStr)
                val uid = resJson.getString("localId")
                val idToken = resJson.getString("idToken")

                // Fetch user data
                val getProfileReq = Request.Builder()
                    .url("$GLOBAL_CHAT_DB/users/$uid.json?auth=$idToken")
                    .build()
                
                client.newCall(getProfileReq).execute().use { pRes ->
                    if (pRes.isSuccessful) {
                        val pBody = pRes.body?.string() ?: ""
                        if (pBody.isNotEmpty() && pBody != "null") {
                            val pObj = JSONObject(pBody)
                            
                            // Update online status
                            val updateStatusJson = JSONObject().apply {
                                put("online", true)
                                put("lastSeen", System.currentTimeMillis())
                            }
                            val patchReq = Request.Builder()
                                .url("$GLOBAL_CHAT_DB/users/$uid.json?auth=$idToken")
                                .patch(updateStatusJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                                .build()
                            client.newCall(patchReq).execute().close()

                            return@withContext Result.success(
                                NativeChatUser(
                                    uid = uid,
                                    name = pObj.optString("name", username),
                                    username = pObj.optString("username", username),
                                    photoURL = pObj.optString("photoURL", "https://i.ibb.co/VWHpjwq9/IMG-979.jpg"),
                                    bio = pObj.optString("bio", "مرحباً بك في دردشة +90"),
                                    online = true,
                                    lastSeen = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
                return@withContext Result.failure(Exception("لم يتم العثور على ملف تعريف لهذا الحساب!"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging in native user", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Update native user profile.
     */
    suspend fun updateNativeUserProfile(
        uid: String,
        name: String,
        photoURL: String,
        bio: String,
        idToken: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanedName = name.trim()
        val cleanedPhoto = photoURL.trim().ifEmpty { "https://i.ibb.co/VWHpjwq9/IMG-979.jpg" }
        val cleanedBio = bio.trim()

        if (cleanedName.contains("+90")) return@withContext false

        val url = "$GLOBAL_CHAT_DB/users/$uid.json?auth=$idToken"
        try {
            val json = JSONObject().apply {
                put("name", cleanedName)
                put("photoURL", cleanedPhoto)
                put("bio", cleanedBio)
            }
            val request = Request.Builder().url(url).patch(json.toString().toRequestBody(JSON_MEDIA_TYPE)).build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating native profile", e)
        }
        return@withContext false
    }

    /**
     * Get recent native conversations for active chats.
     */
    suspend fun getNativeRecentChats(uid: String, idToken: String): List<NativeRecentChat> = withContext(Dispatchers.IO) {
        val url = "$GLOBAL_CHAT_DB/conversations/$uid.json?auth=$idToken"
        val list = mutableListOf<NativeRecentChat>()
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val obj = json.optJSONObject(key) ?: continue
                            list.add(
                                NativeRecentChat(
                                    uid = obj.optString("uid", key),
                                    name = obj.optString("name", "مستخدم"),
                                    username = obj.optString("username", ""),
                                    photoURL = obj.optString("photoURL", "https://i.ibb.co/VWHpjwq9/IMG-979.jpg"),
                                    lastMessage = obj.optString("lastMessage", ""),
                                    updatedAt = obj.optLong("updatedAt", 0L)
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recent chats", e)
        }
        return@withContext list.sortedByDescending { it.updatedAt }
    }

    /**
     * Fetch native global chat messages.
     */
    suspend fun getNativeGlobalMessages(): List<NativeChatMessage> = withContext(Dispatchers.IO) {
        val url = "$GLOBAL_CHAT_DB/globalMessages.json?limitToLast=50"
        val list = mutableListOf<NativeChatMessage>()
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val obj = json.optJSONObject(key) ?: continue
                            
                            val reactionsMap = mutableMapOf<String, String>()
                            val reactObj = obj.optJSONObject("reactions")
                            if (reactObj != null) {
                                val rKeys = reactObj.keys()
                                while (rKeys.hasNext()) {
                                    val rKey = rKeys.next()
                                    reactionsMap[rKey] = reactObj.optString(rKey, "")
                                }
                            }

                            list.add(
                                NativeChatMessage(
                                    id = key,
                                    senderId = obj.optString("uid", ""),
                                    senderName = obj.optString("name", "مستخدم"),
                                    senderPhoto = obj.optString("photoURL", "https://i.ibb.co/VWHpjwq9/IMG-979.jpg"),
                                    text = obj.optString("text", ""),
                                    createdAt = obj.optLong("createdAt", 0L),
                                    type = obj.optString("type", "text"),
                                    replyToName = if (obj.has("replyToName")) obj.getString("replyToName") else null,
                                    replyToText = if (obj.has("replyToText")) obj.getString("replyToText") else null,
                                    reactions = reactionsMap
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching native global messages", e)
        }
        return@withContext list.sortedBy { it.createdAt }
    }

    /**
     * Send global message natively.
     */
    suspend fun sendNativeGlobalMessage(
        uid: String,
        name: String,
        username: String,
        photoURL: String,
        text: String,
        replyToName: String? = null,
        replyToText: String? = null,
        idToken: String
    ): Boolean = withContext(Dispatchers.IO) {
        val url = "$GLOBAL_CHAT_DB/globalMessages.json?auth=$idToken"
        try {
            val json = JSONObject().apply {
                put("uid", uid)
                put("name", name)
                put("username", username)
                put("photoURL", photoURL)
                put("text", text)
                put("createdAt", System.currentTimeMillis())
                put("type", "text")
                if (replyToName != null) {
                    put("replyToName", replyToName)
                    put("replyToText", replyToText)
                }
            }
            val request = Request.Builder().url(url).post(json.toString().toRequestBody(JSON_MEDIA_TYPE)).build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending native global message", e)
        }
        return@withContext false
    }

    /**
     * Fetch private messages between users.
     */
    suspend fun getNativePrivateMessages(conversationId: String, idToken: String): List<NativeChatMessage> = withContext(Dispatchers.IO) {
        val url = "$GLOBAL_CHAT_DB/privateChats/$conversationId/messages.json?limitToLast=50&auth=$idToken"
        val list = mutableListOf<NativeChatMessage>()
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val obj = json.optJSONObject(key) ?: continue
                            list.add(
                                NativeChatMessage(
                                    id = key,
                                    senderId = obj.optString("senderId", ""),
                                    senderName = obj.optString("senderName", "مستخدم"),
                                    senderPhoto = obj.optString("senderPhoto", "https://i.ibb.co/VWHpjwq9/IMG-979.jpg"),
                                    text = obj.optString("text", ""),
                                    createdAt = obj.optLong("createdAt", 0L),
                                    type = obj.optString("type", "text")
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching native private messages", e)
        }
        return@withContext list.sortedBy { it.createdAt }
    }

    /**
     * Send native private message.
     */
    suspend fun sendNativePrivateMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        senderPhoto: String,
        text: String,
        otherUserUid: String,
        otherUserName: String,
        otherUserUsername: String,
        otherUserPhoto: String,
        idToken: String
    ): Boolean = withContext(Dispatchers.IO) {
        val url = "$GLOBAL_CHAT_DB/privateChats/$conversationId/messages.json?auth=$idToken"
        try {
            val timestamp = System.currentTimeMillis()
            val json = JSONObject().apply {
                put("senderId", senderId)
                put("senderName", senderName)
                put("senderPhoto", senderPhoto)
                put("text", text)
                put("createdAt", timestamp)
                put("type", "text")
            }
            val request = Request.Builder().url(url).post(json.toString().toRequestBody(JSON_MEDIA_TYPE)).build()
            var sentSuccess = false
            client.newCall(request).execute().use { response ->
                sentSuccess = response.isSuccessful
            }

            if (sentSuccess) {
                // Update recent conversations on BOTH sides!
                val updateMeJson = JSONObject().apply {
                    put("uid", otherUserUid)
                    put("name", otherUserName)
                    put("username", otherUserUsername)
                    put("photoURL", otherUserPhoto)
                    put("lastMessage", text)
                    put("updatedAt", timestamp)
                }
                val updateMeReq = Request.Builder()
                    .url("$GLOBAL_CHAT_DB/conversations/$senderId/$otherUserUid.json?auth=$idToken")
                    .put(updateMeJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                client.newCall(updateMeReq).execute().close()

                val updateOtherJson = JSONObject().apply {
                    put("uid", senderId)
                    put("name", senderName)
                    put("username", otherUserUsername) // will look up correctly
                    put("photoURL", senderPhoto)
                    put("lastMessage", text)
                    put("updatedAt", timestamp)
                }
                val updateOtherReq = Request.Builder()
                    .url("$GLOBAL_CHAT_DB/conversations/$otherUserUid/$senderId.json?auth=$idToken")
                    .put(updateOtherJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                client.newCall(updateOtherReq).execute().close()
            }
            return@withContext sentSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error sending native private message", e)
        }
        return@withContext false
    }

    /**
     * Search global users.
     */
    suspend fun searchNativeUsers(query: String): List<NativeChatUser> = withContext(Dispatchers.IO) {
        val url = "$GLOBAL_CHAT_DB/users.json"
        val list = mutableListOf<NativeChatUser>()
        val term = query.trim().lowercase()
        if (term.isEmpty()) return@withContext emptyList()
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val obj = json.optJSONObject(key) ?: continue
                            val uName = obj.optString("username", "")
                            val name = obj.optString("name", "")
                            if (uName.lowercase().contains(term) || name.lowercase().contains(term)) {
                                list.add(
                                    NativeChatUser(
                                        uid = obj.optString("uid", key),
                                        name = name,
                                        username = uName,
                                        photoURL = obj.optString("photoURL", "https://i.ibb.co/VWHpjwq9/IMG-979.jpg"),
                                        bio = obj.optString("bio", ""),
                                        online = obj.optBoolean("online", false),
                                        lastSeen = obj.optLong("lastSeen", 0L)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching native users", e)
        }
        return@withContext list
    }

    /**
     * Add native global reaction.
     */
    suspend fun reactToNativeGlobalMessage(messageId: String, username: String, emoji: String, idToken: String): Boolean = withContext(Dispatchers.IO) {
        val url = "$GLOBAL_CHAT_DB/globalMessages/$messageId/reactions/$username.json?auth=$idToken"
        try {
            val body = "\"$emoji\"".toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).put(body).build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reacting to global msg", e)
        }
        return@withContext false
    }

    /**
     * Delete native global message.
     */
    suspend fun deleteNativeGlobalMessage(messageId: String, idToken: String): Boolean = withContext(Dispatchers.IO) {
        val url = "$GLOBAL_CHAT_DB/globalMessages/$messageId.json?auth=$idToken"
        try {
            val request = Request.Builder().url(url).delete().build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting native global msg", e)
        }
        return@withContext false
    }
}
