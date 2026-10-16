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

data class Reel(
    val id: String = "",
    val username: String = "",
    val name: String = "",
    val avatar: String = "neon_wolf",
    val videoUrl: String = "",
    val description: String = "",
    val hashtags: List<String> = emptyList(),
    val timestamp: Long = 0L,
    val durationSeconds: Int = 15,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val shareCopiesCount: Int = 0,
    val likedBy: Map<String, Boolean> = emptyMap() // username -> true
)

data class ReelComment(
    val id: String = "",
    val reelId: String = "",
    val username: String = "",
    val name: String = "",
    val avatar: String = "neon_wolf",
    val text: String = "",
    val timestamp: Long = 0L,
    val replies: List<ReelReply> = emptyList()
)

data class ReelReply(
    val id: String = "",
    val username: String = "",
    val name: String = "",
    val avatar: String = "neon_wolf",
    val text: String = "",
    val timestamp: Long = 0L
)

data class ReelsSettings(
    val maxVideoDuration: Int = 15,
    val isReelsEnabled: Boolean = true
)

object ReelsService {
    private const val TAG = "ReelsService"
    private const val BASE_URL = "https://loop-7e3d9-default-rtdb.firebaseio.com"

    private val client = OkHttpClient.Builder()
        .proxy(java.net.Proxy.NO_PROXY)
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * Fetch all Reels from the third Firebase database
     */
    suspend fun fetchReels(): List<Reel> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/reels.json"
        val list = mutableListOf<Reel>()
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
                            
                            val hashtagsList = mutableListOf<String>()
                            val hashtagsArr = obj.optJSONArray("hashtags")
                            if (hashtagsArr != null) {
                                for (i in 0 until hashtagsArr.length()) {
                                    hashtagsList.add(hashtagsArr.getString(i))
                                }
                            }

                            val likedByMap = mutableMapOf<String, Boolean>()
                            val likedByObj = obj.optJSONObject("likedBy")
                            if (likedByObj != null) {
                                val lKeys = likedByObj.keys()
                                while (lKeys.hasNext()) {
                                    val lKey = lKeys.next()
                                    likedByMap[lKey] = likedByObj.optBoolean(lKey, false)
                                }
                            }

                            list.add(
                                Reel(
                                    id = key,
                                    username = obj.optString("username", ""),
                                    name = obj.optString("name", "مجهول"),
                                    avatar = obj.optString("avatar", "neon_wolf"),
                                    videoUrl = obj.optString("videoUrl", ""),
                                    description = obj.optString("description", ""),
                                    hashtags = hashtagsList,
                                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                    durationSeconds = obj.optInt("durationSeconds", 15),
                                    likesCount = obj.optInt("likesCount", 0),
                                    commentsCount = obj.optInt("commentsCount", 0),
                                    shareCopiesCount = obj.optInt("shareCopiesCount", 0),
                                    likedBy = likedByMap
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching reels", e)
        }
        return@withContext list.sortedByDescending { it.timestamp }
    }

    /**
     * Post a new Reel
     */
    suspend fun postReel(
        username: String,
        name: String,
        avatar: String,
        videoUrl: String,
        description: String,
        hashtags: List<String>,
        durationSeconds: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/reels.json"
        try {
            val json = JSONObject().apply {
                put("username", username)
                put("name", name)
                put("avatar", avatar)
                put("videoUrl", videoUrl)
                put("description", description)
                put("timestamp", System.currentTimeMillis())
                put("durationSeconds", durationSeconds)
                put("likesCount", 0)
                put("commentsCount", 0)
                put("shareCopiesCount", 0)
                
                val tagsArr = org.json.JSONArray()
                hashtags.forEach { tagsArr.put(it) }
                put("hashtags", tagsArr)
            }
            val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).post(body).build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error posting reel", e)
        }
        return@withContext false
    }

    /**
     * Toggle like state on a Reel
     */
    suspend fun toggleLikeReel(reelId: String, username: String, isLiked: Boolean): Boolean = withContext(Dispatchers.IO) {
        val likeUrl = "$BASE_URL/reels/$reelId/likedBy/$username.json"
        try {
            // Update the user's specific likedBy node
            val likeBody = if (isLiked) "true".toRequestBody(JSON_MEDIA_TYPE) else "null".toRequestBody(JSON_MEDIA_TYPE)
            val requestLike = if (isLiked) {
                Request.Builder().url(likeUrl).put(likeBody).build()
            } else {
                Request.Builder().url(likeUrl).delete().build()
            }

            client.newCall(requestLike).execute().use { response ->
                if (response.isSuccessful) {
                    // Recalculate likes count
                    updateReelLikesCount(reelId)
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling like for reel $reelId", e)
        }
        return@withContext false
    }

    private suspend fun updateReelLikesCount(reelId: String) {
        val url = "$BASE_URL/reels/$reelId/likedBy.json"
        var likesCount = 0
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        likesCount = json.length()
                    }
                }
            }
            // Update the reel's like count
            val updateUrl = "$BASE_URL/reels/$reelId/likesCount.json"
            val body = "$likesCount".toRequestBody(JSON_MEDIA_TYPE)
            val requestUpdate = Request.Builder().url(updateUrl).put(body).build()
            client.newCall(requestUpdate).execute()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating likes count for reel $reelId", e)
        }
    }

    /**
     * Increment share copy count
     */
    suspend fun incrementShareCopyCount(reelId: String): Int = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/reels/$reelId/shareCopiesCount.json"
        var currentCount = 0
        try {
            // Fetch current count
            val requestGet = Request.Builder().url(url).build()
            client.newCall(requestGet).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: "0"
                    currentCount = bodyStr.toIntOrNull() ?: 0
                }
            }
            currentCount += 1
            // Save updated count
            val body = "$currentCount".toRequestBody(JSON_MEDIA_TYPE)
            val requestPut = Request.Builder().url(url).put(body).build()
            client.newCall(requestPut).execute()
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing share copy count", e)
        }
        return@withContext currentCount
    }

    /**
     * Fetch all comments for a specific Reel
     */
    suspend fun fetchComments(reelId: String): List<ReelComment> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/comments/$reelId.json"
        val list = mutableListOf<ReelComment>()
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

                            val repliesList = mutableListOf<ReelReply>()
                            val repliesObj = obj.optJSONObject("replies")
                            if (repliesObj != null) {
                                val rKeys = repliesObj.keys()
                                while (rKeys.hasNext()) {
                                    val rKey = rKeys.next()
                                    val rObj = repliesObj.optJSONObject(rKey) ?: continue
                                    repliesList.add(
                                        ReelReply(
                                            id = rKey,
                                            username = rObj.optString("username", ""),
                                            name = rObj.optString("name", "مجهول"),
                                            avatar = rObj.optString("avatar", "neon_wolf"),
                                            text = rObj.optString("text", ""),
                                            timestamp = rObj.optLong("timestamp", System.currentTimeMillis())
                                        )
                                    )
                                }
                            }

                            list.add(
                                ReelComment(
                                    id = key,
                                    reelId = reelId,
                                    username = obj.optString("username", ""),
                                    name = obj.optString("name", "مجهول"),
                                    avatar = obj.optString("avatar", "neon_wolf"),
                                    text = obj.optString("text", ""),
                                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                    replies = repliesList.sortedBy { it.timestamp }
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching comments for reel $reelId", e)
        }
        return@withContext list.sortedBy { it.timestamp }
    }

    /**
     * Add a comment to a Reel
     */
    suspend fun postComment(
        reelId: String,
        username: String,
        name: String,
        avatar: String,
        text: String
    ): Boolean = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/comments/$reelId.json"
        try {
            val json = JSONObject().apply {
                put("username", username)
                put("name", name)
                put("avatar", avatar)
                put("text", text)
                put("timestamp", System.currentTimeMillis())
            }
            val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).post(body).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    updateReelCommentsCount(reelId)
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error posting comment", e)
        }
        return@withContext false
    }

    /**
     * Delete a comment on a Reel
     */
    suspend fun deleteComment(reelId: String, commentId: String): Boolean = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/comments/$reelId/$commentId.json"
        try {
            val request = Request.Builder().url(url).delete().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    updateReelCommentsCount(reelId)
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting comment $commentId", e)
        }
        return@withContext false
    }

    private suspend fun updateReelCommentsCount(reelId: String) {
        val url = "$BASE_URL/comments/$reelId.json"
        var count = 0
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        count = json.length()
                    }
                }
            }
            // Update commentsCount on the reel
            val updateUrl = "$BASE_URL/reels/$reelId/commentsCount.json"
            val body = "$count".toRequestBody(JSON_MEDIA_TYPE)
            val requestUpdate = Request.Builder().url(updateUrl).put(body).build()
            client.newCall(requestUpdate).execute()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating comments count", e)
        }
    }

    /**
     * Add a reply to a comment
     */
    suspend fun postReply(
        reelId: String,
        commentId: String,
        username: String,
        name: String,
        avatar: String,
        text: String
    ): Boolean = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/comments/$reelId/$commentId/replies.json"
        try {
            val json = JSONObject().apply {
                put("username", username)
                put("name", name)
                put("avatar", avatar)
                put("text", text)
                put("timestamp", System.currentTimeMillis())
            }
            val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).post(body).build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error posting reply", e)
        }
        return@withContext false
    }

    /**
     * Toggle follow relationship
     */
    suspend fun toggleFollow(myUsername: String, otherUsername: String, follow: Boolean): Boolean = withContext(Dispatchers.IO) {
        val followingUrl = "$BASE_URL/following/$myUsername/$otherUsername.json"
        val followersUrl = "$BASE_URL/followers/$otherUsername/$myUsername.json"
        try {
            val body = if (follow) "true".toRequestBody(JSON_MEDIA_TYPE) else "null".toRequestBody(JSON_MEDIA_TYPE)
            
            // Update following
            val req1 = if (follow) Request.Builder().url(followingUrl).put(body).build() else Request.Builder().url(followingUrl).delete().build()
            val res1 = client.newCall(req1).execute()

            // Update followers
            val req2 = if (follow) Request.Builder().url(followersUrl).put(body).build() else Request.Builder().url(followersUrl).delete().build()
            val res2 = client.newCall(req2).execute()

            return@withContext res1.isSuccessful && res2.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling follow status", e)
        }
        return@withContext false
    }

    /**
     * Check follow relationship
     */
    suspend fun isFollowingUser(myUsername: String, otherUsername: String): Boolean = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/following/$myUsername/$otherUsername.json"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()?.trim() ?: "false"
                    return@withContext bodyStr == "true"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking following state", e)
        }
        return@withContext false
    }

    /**
     * Get followers list size
     */
    suspend fun getFollowersCount(username: String): Int = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/followers/$username.json"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: "null"
                    if (bodyStr != "null" && bodyStr.isNotEmpty()) {
                        val json = JSONObject(bodyStr)
                        return@withContext json.length()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting followers count", e)
        }
        return@withContext 0
    }

    /**
     * Get following list size
     */
    suspend fun getFollowingCount(username: String): Int = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/following/$username.json"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: "null"
                    if (bodyStr != "null" && bodyStr.isNotEmpty()) {
                        val json = JSONObject(bodyStr)
                        return@withContext json.length()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting following count", e)
        }
        return@withContext 0
    }

    /**
     * Fetch Reels Settings
     */
    suspend fun fetchSettings(): ReelsSettings = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/settings.json"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        return@withContext ReelsSettings(
                            maxVideoDuration = json.optInt("max_video_duration", 15),
                            isReelsEnabled = json.optBoolean("is_reels_enabled", true)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Reels settings fetch offline or unavailable, using defaults")
        }
        return@withContext ReelsSettings()
    }

    /**
     * Update Reels Settings
     */
    suspend fun updateSettings(settings: ReelsSettings): Boolean = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/settings.json"
        try {
            val json = JSONObject().apply {
                put("max_video_duration", settings.maxVideoDuration)
                put("is_reels_enabled", settings.isReelsEnabled)
            }
            val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).put(body).build()
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating reels settings", e)
        }
        return@withContext false
    }

    /**
     * Delete Reel (for administrative controls)
     */
    suspend fun deleteReel(reelId: String): Boolean = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/reels/$reelId.json"
        val commentsUrl = "$BASE_URL/comments/$reelId.json"
        try {
            val request1 = Request.Builder().url(url).delete().build()
            val res1 = client.newCall(request1).execute().isSuccessful

            val request2 = Request.Builder().url(commentsUrl).delete().build()
            client.newCall(request2).execute()

            return@withContext res1
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting reel", e)
        }
        return@withContext false
    }
}
