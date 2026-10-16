package com.example.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.model.ActiveDownloadState
import com.example.model.LocalDownload
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.*
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

object FastDownloaderEngine {
    private const val TAG = "FastDownloaderEngine"

    private val httpClient = OkHttpClient.Builder()
        .proxy(java.net.Proxy.NO_PROXY)
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(java.util.Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
            else -> String.format(java.util.Locale.US, "%.0f KB", kb)
        }
    }

    fun formatSpeed(bytesPerSec: Double): String {
        if (bytesPerSec <= 0) return "0 KB/s"
        val kb = bytesPerSec / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.2f MB/s", mb)
            else -> String.format(java.util.Locale.US, "%.0f KB/s", kb)
        }
    }

    fun formatEta(remainingSeconds: Long): String {
        if (remainingSeconds <= 0 || remainingSeconds > 86400) return ""
        val mins = remainingSeconds / 60
        val secs = remainingSeconds % 60
        return when {
            mins > 60 -> "باقي أكثر من ساعة"
            mins > 0 -> "باقي $mins د و $secs ث"
            else -> "باقي $secs ثانية"
        }
    }

    fun getMediaStorageDir(context: Context): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        val dir = File(baseDir, "plus90_media")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun executeDownload(
        context: Context,
        downloadItem: LocalDownload,
        userAgent: String,
        onProgressUpdate: (ActiveDownloadState) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val notificationId = (downloadItem.id.hashCode() and 0x7FFFFFFF)
        val cleanTitle = downloadItem.title.replace(Regex("[^a-zA-Z0-9أ-ي\\s_\\-]"), "_").trim().replace("\\s+".toRegex(), "_")
        val storageDir = getMediaStorageDir(context)

        // Initial State: Analyzing
        var currentState = ActiveDownloadState(
            id = downloadItem.id,
            title = downloadItem.title,
            posterUrl = downloadItem.posterUrl,
            type = downloadItem.type,
            quality = downloadItem.quality,
            progress = 0.05f,
            speedFormatted = "جاري الاتصال...",
            downloadedSizeFormatted = "0 MB",
            totalSizeBytes = 0L,
            downloadedBytes = 0L,
            etaFormatted = "",
            phase = "analyzing"
        )
        onProgressUpdate(currentState)
        NotificationHelper.showDownloadProgressNotification(context, notificationId, downloadItem.title, 5, "جاري فحص السيرفر...", true)

        try {
            val rawUrl = downloadItem.videoUrl.trim()
            val effectiveUserAgent = userAgent.ifEmpty { "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36" }

            // Check if stream is HLS M3U8
            val isHls = rawUrl.contains(".m3u8", ignoreCase = true) || checkIfHlsPlaylist(rawUrl, effectiveUserAgent)

            val resultFile = if (isHls) {
                downloadHlsStream(
                    context = context,
                    rawUrl = rawUrl,
                    cleanTitle = cleanTitle,
                    storageDir = storageDir,
                    downloadItem = downloadItem,
                    userAgent = effectiveUserAgent,
                    notificationId = notificationId,
                    onProgress = { state ->
                        currentState = state
                        onProgressUpdate(state)
                    }
                )
            } else {
                downloadDirectStream(
                    context = context,
                    rawUrl = rawUrl,
                    cleanTitle = cleanTitle,
                    storageDir = storageDir,
                    downloadItem = downloadItem,
                    userAgent = effectiveUserAgent,
                    notificationId = notificationId,
                    onProgress = { state ->
                        currentState = state
                        onProgressUpdate(state)
                    }
                )
            }

            if (resultFile.exists() && resultFile.length() > 0) {
                currentState = currentState.copy(
                    progress = 1.0f,
                    phase = "completed",
                    downloadedBytes = resultFile.length(),
                    totalSizeBytes = resultFile.length(),
                    downloadedSizeFormatted = "${formatBytes(resultFile.length())} / ${formatBytes(resultFile.length())}",
                    speedFormatted = "اكتمل بنجاح"
                )
                onProgressUpdate(currentState)
                NotificationHelper.showDownloadCompletedNotification(context, notificationId, downloadItem.title)
                Result.success(resultFile)
            } else {
                NotificationHelper.cancelNotification(context, notificationId)
                currentState = currentState.copy(phase = "failed", speedFormatted = "فشل التنزيل")
                onProgressUpdate(currentState)
                Result.failure(IOException("الملف الناتج غير موجود أو فارغ"))
            }
        } catch (e: CancellationException) {
            NotificationHelper.cancelNotification(context, notificationId)
            currentState = currentState.copy(phase = "failed", speedFormatted = "تم الإلغاء")
            onProgressUpdate(currentState)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Download error for ${downloadItem.title}: ${e.message}", e)
            NotificationHelper.cancelNotification(context, notificationId)
            currentState = currentState.copy(phase = "failed", speedFormatted = "فشل: ${e.localizedMessage}")
            onProgressUpdate(currentState)
            Result.failure(e)
        }
    }

    private suspend fun checkIfHlsPlaylist(url: String, userAgent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .head()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                val contentType = resp.header("Content-Type") ?: ""
                contentType.contains("mpegurl", ignoreCase = true) || contentType.contains("application/x-mpegURL", ignoreCase = true)
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun downloadDirectStream(
        context: Context,
        rawUrl: String,
        cleanTitle: String,
        storageDir: File,
        downloadItem: LocalDownload,
        userAgent: String,
        notificationId: Int,
        onProgress: (ActiveDownloadState) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val ext = when {
            rawUrl.contains(".mp4", ignoreCase = true) -> ".mp4"
            rawUrl.contains(".mkv", ignoreCase = true) -> ".mkv"
            rawUrl.contains(".ts", ignoreCase = true) -> ".ts"
            rawUrl.contains(".avi", ignoreCase = true) -> ".avi"
            else -> ".mp4"
        }
        val targetFile = File(storageDir, "Plus90_${downloadItem.id}_$cleanTitle$ext")
        val tempFile = File(storageDir, "Plus90_${downloadItem.id}_$cleanTitle.part")

        var totalBytesRead = 0L
        var success = false
        var attempt = 0
        val maxAttempts = 3

        while (attempt < maxAttempts && !success && currentCoroutineContext().isActive) {
            try {
                attempt++
                val requestBuilder = Request.Builder()
                    .url(rawUrl)
                    .header("User-Agent", userAgent)

                val existingLength = if (tempFile.exists()) tempFile.length() else 0L
                if (existingLength > 0) {
                    requestBuilder.header("Range", "bytes=$existingLength-")
                }

                val request = requestBuilder.build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.code == 416) {
                        // Range Satisfied or completed
                        if (tempFile.exists() && tempFile.length() > 1024) {
                            tempFile.renameTo(targetFile)
                            return@withContext targetFile
                        }
                    }

                    val isPartial = response.code == 206
                    if (!response.isSuccessful && !isPartial) {
                        throw IOException("HTTP error ${response.code}: ${response.message}")
                    }

                    val body = response.body ?: throw IOException("Empty response body")
                    val responseLength = body.contentLength()
                    val totalContentLength = if (responseLength > 0) {
                        if (isPartial) responseLength + existingLength else responseLength
                    } else 0L

                    val appendMode = isPartial && existingLength > 0
                    totalBytesRead = if (appendMode) existingLength else 0L

                    var lastSpeedCalcTime = System.currentTimeMillis()
                    var bytesSinceLastSpeedCalc = 0L
                    var currentSpeed = 0.0
                    var lastNotificationUpdate = 0L

                    body.byteStream().use { inputStream ->
                        FileOutputStream(tempFile, appendMode).use { outputStream ->
                            val buffer = ByteArray(128 * 1024) // 128KB fast stream buffer
                            var read = 0

                            while (currentCoroutineContext().isActive && inputStream.read(buffer).also { read = it } != -1) {
                                outputStream.write(buffer, 0, read)
                                totalBytesRead += read
                                bytesSinceLastSpeedCalc += read

                                val now = System.currentTimeMillis()
                                val timeDelta = now - lastSpeedCalcTime
                                if (timeDelta >= 600) {
                                    currentSpeed = (bytesSinceLastSpeedCalc * 1000.0) / timeDelta
                                    bytesSinceLastSpeedCalc = 0L
                                    lastSpeedCalcTime = now

                                    val progressFloat = if (totalContentLength > 0) {
                                        (totalBytesRead.toFloat() / totalContentLength.toFloat()).coerceIn(0f, 1f)
                                    } else {
                                        0.5f // Indeterminate streaming
                                    }

                                    val remainingBytes = (totalContentLength - totalBytesRead).coerceAtLeast(0L)
                                    val etaSeconds = if (currentSpeed > 0 && remainingBytes > 0) {
                                        (remainingBytes / currentSpeed).toLong()
                                    } else 0L

                                    val downloadedStr = if (totalContentLength > 0) {
                                        "${formatBytes(totalBytesRead)} / ${formatBytes(totalContentLength)}"
                                    } else {
                                        formatBytes(totalBytesRead)
                                    }

                                    val state = ActiveDownloadState(
                                        id = downloadItem.id,
                                        title = downloadItem.title,
                                        posterUrl = downloadItem.posterUrl,
                                        type = downloadItem.type,
                                        quality = downloadItem.quality,
                                        progress = progressFloat,
                                        speedFormatted = formatSpeed(currentSpeed),
                                        downloadedSizeFormatted = downloadedStr,
                                        totalSizeBytes = totalContentLength,
                                        downloadedBytes = totalBytesRead,
                                        etaFormatted = formatEta(etaSeconds),
                                        phase = "downloading"
                                    )
                                    onProgress(state)

                                    if (now - lastNotificationUpdate >= 1200) {
                                        lastNotificationUpdate = now
                                        val percent = (progressFloat * 100).roundToInt()
                                        NotificationHelper.showDownloadProgressNotification(
                                            context = context,
                                            notificationId = notificationId,
                                            title = downloadItem.title,
                                            progressPercent = percent,
                                            speedText = formatSpeed(currentSpeed),
                                            isIndeterminate = totalContentLength <= 0
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (currentCoroutineContext().isActive) {
                        success = true
                    }
                }
            } catch (e: Exception) {
                if (attempt >= maxAttempts) throw e
                Log.w(TAG, "Direct download attempt $attempt failed: ${e.message}, retrying in 1.5s...")
                delay(1500L)
            }
        }

        if (tempFile.exists() && tempFile.length() > 0) {
            if (targetFile.exists()) targetFile.delete()
            tempFile.renameTo(targetFile)
        }
        targetFile
    }

    private suspend fun downloadHlsStream(
        context: Context,
        rawUrl: String,
        cleanTitle: String,
        storageDir: File,
        downloadItem: LocalDownload,
        userAgent: String,
        notificationId: Int,
        onProgress: (ActiveDownloadState) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val targetFile = File(storageDir, "Plus90_${downloadItem.id}_$cleanTitle.ts")
        val segmentsDir = File(storageDir, "hls_chunks_${downloadItem.id}")
        segmentsDir.mkdirs()

        // 1. Fetch Playlist
        val playlistText = fetchUrlString(rawUrl, userAgent)
        val mediaPlaylistUrl = resolveMediaPlaylistUrl(rawUrl, playlistText, userAgent)
        val finalPlaylistContent = if (mediaPlaylistUrl != rawUrl) {
            fetchUrlString(mediaPlaylistUrl, userAgent)
        } else {
            playlistText
        }

        // 2. Extract Segments
        val segmentUrls = parseSegmentsFromM3u8(mediaPlaylistUrl, finalPlaylistContent)
        if (segmentUrls.isEmpty()) {
            throw IOException("لم يتم العثور على أجزاء فيديو في ملف البث المباشر M3U8")
        }

        val totalSegments = segmentUrls.size
        val downloadedSegmentsCount = AtomicLong(0L)
        val totalBytesAccumulator = AtomicLong(0L)
        val startTime = System.currentTimeMillis()
        var lastNotificationUpdate = 0L

        // 3. Parallel Chunk Downloader (4 workers for ultra fast concurrent downloads)
        val segmentFiles = Array<File?>(totalSegments) { null }
        val concurrencyLimit = 4
        val chunks = segmentUrls.mapIndexed { index, url -> Pair(index, url) }

        coroutineScope {
            val channel = kotlinx.coroutines.channels.Channel<Pair<Int, String>>(kotlinx.coroutines.channels.Channel.UNLIMITED)
            chunks.forEach { channel.trySend(it) }
            channel.close()

            val workers = List(concurrencyLimit) {
                launch(Dispatchers.IO) {
                    for ((index, segUrl) in channel) {
                        if (!isActive) break
                        val segFile = File(segmentsDir, "seg_%05d.ts".format(index))
                        if (!segFile.exists() || segFile.length() == 0L) {
                            downloadFileWithRetry(segUrl, segFile, userAgent)
                        }
                        segmentFiles[index] = segFile
                        val segSize = segFile.length()
                        totalBytesAccumulator.addAndGet(segSize)
                        val completed = downloadedSegmentsCount.incrementAndGet()

                        val now = System.currentTimeMillis()
                        val elapsedSec = ((now - startTime) / 1000.0).coerceAtLeast(0.1)
                        val totalBytes = totalBytesAccumulator.get()
                        val speedBytesPerSec = totalBytes / elapsedSec
                        val progressFloat = (completed.toFloat() / totalSegments.toFloat()).coerceIn(0f, 1f)

                        val remainingSegs = (totalSegments - completed).coerceAtLeast(0L)
                        val avgSegTimeSec = elapsedSec / completed.toDouble()
                        val etaSec = (remainingSegs * avgSegTimeSec).toLong()

                        val state = ActiveDownloadState(
                            id = downloadItem.id,
                            title = downloadItem.title,
                            posterUrl = downloadItem.posterUrl,
                            type = downloadItem.type,
                            quality = downloadItem.quality,
                            progress = progressFloat,
                            speedFormatted = formatSpeed(speedBytesPerSec),
                            downloadedSizeFormatted = "${formatBytes(totalBytes)} ($completed/$totalSegments مقطع)",
                            totalSizeBytes = 0L,
                            downloadedBytes = totalBytes,
                            etaFormatted = formatEta(etaSec),
                            phase = "downloading"
                        )
                        onProgress(state)

                        if (now - lastNotificationUpdate >= 1200) {
                            lastNotificationUpdate = now
                            val percent = (progressFloat * 100).roundToInt()
                            NotificationHelper.showDownloadProgressNotification(
                                context = context,
                                notificationId = notificationId,
                                title = downloadItem.title,
                                progressPercent = percent,
                                speedText = formatSpeed(speedBytesPerSec)
                            )
                        }
                    }
                }
            }
            workers.joinAll()
        }

        // 4. Sequential Fast Merge into Single TS Container
        onProgress(
            ActiveDownloadState(
                id = downloadItem.id,
                title = downloadItem.title,
                posterUrl = downloadItem.posterUrl,
                type = downloadItem.type,
                quality = downloadItem.quality,
                progress = 0.99f,
                speedFormatted = "جاري تجميع المقاطع...",
                downloadedSizeFormatted = formatBytes(totalBytesAccumulator.get()),
                totalSizeBytes = totalBytesAccumulator.get(),
                downloadedBytes = totalBytesAccumulator.get(),
                etaFormatted = "ثواني معدودة",
                phase = "merging"
            )
        )

        if (targetFile.exists()) targetFile.delete()
        FileOutputStream(targetFile).use { outStream ->
            val buf = ByteArray(128 * 1024)
            for (i in 0 until totalSegments) {
                val segFile = segmentFiles[i] ?: File(segmentsDir, "seg_%05d.ts".format(i))
                if (segFile.exists() && segFile.length() > 0) {
                    FileInputStream(segFile).use { inStream ->
                        var r: Int
                        while (inStream.read(buf).also { r = it } != -1) {
                            outStream.write(buf, 0, r)
                        }
                    }
                }
            }
        }

        // Clean up chunks
        try {
            segmentsDir.listFiles()?.forEach { it.delete() }
            segmentsDir.delete()
        } catch (e: Exception) {
            Log.w(TAG, "Chunk cleanup error: ${e.message}")
        }

        targetFile
    }

    private fun fetchUrlString(url: String, userAgent: String): String {
        val req = Request.Builder().url(url).header("User-Agent", userAgent).build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Failed to fetch $url (code: ${resp.code})")
            return resp.body?.string() ?: ""
        }
    }

    private fun resolveMediaPlaylistUrl(baseUrl: String, m3u8Content: String, userAgent: String): String {
        if (!m3u8Content.contains("#EXT-X-STREAM-INF")) return baseUrl

        // Master playlist: pick highest bandwidth stream
        var bestUrl = baseUrl
        var bestBandwidth = -1L

        val lines = m3u8Content.lines()
        var currentBandwidth = 0L
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXT-X-STREAM-INF")) {
                val bwMatch = Regex("BANDWIDTH=(\\d+)").find(trimmed)
                currentBandwidth = bwMatch?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                if (currentBandwidth > bestBandwidth) {
                    bestBandwidth = currentBandwidth
                    bestUrl = resolveRelativeUrl(baseUrl, trimmed)
                }
                currentBandwidth = 0L
            }
        }
        return bestUrl
    }

    private fun parseSegmentsFromM3u8(baseUrl: String, m3u8Content: String): List<String> {
        val segments = mutableListOf<String>()
        val lines = m3u8Content.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                segments.add(resolveRelativeUrl(baseUrl, trimmed))
            }
        }
        return segments
    }

    private fun resolveRelativeUrl(baseUrl: String, relativeUrl: String): String {
        return try {
            val baseUri = URI(baseUrl)
            baseUri.resolve(relativeUrl).toString()
        } catch (e: Exception) {
            if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
                relativeUrl
            } else {
                val lastSlash = baseUrl.lastIndexOf('/')
                if (lastSlash != -1) {
                    baseUrl.substring(0, lastSlash + 1) + relativeUrl
                } else {
                    relativeUrl
                }
            }
        }
    }

    private fun downloadFileWithRetry(url: String, targetFile: File, userAgent: String) {
        var success = false
        var attempts = 0
        while (!success && attempts < 3) {
            try {
                attempts++
                val req = Request.Builder().url(url).header("User-Agent", userAgent).build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("Segment download HTTP ${resp.code}")
                    val body = resp.body ?: throw IOException("Empty segment body")
                    FileOutputStream(targetFile).use { out ->
                        body.byteStream().copyTo(out, 64 * 1024)
                    }
                    if (targetFile.length() > 0) {
                        success = true
                    }
                }
            } catch (e: Exception) {
                if (attempts >= 3) throw e
                Thread.sleep(500)
            }
        }
    }
}
