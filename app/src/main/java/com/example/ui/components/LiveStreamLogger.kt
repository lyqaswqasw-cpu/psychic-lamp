package com.example.ui.components

import android.os.SystemClock
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Production-grade Telemetry & Diagnostic Logger for Live Stream (HLS/TS) Playback.
 * Tracks HTTP requests, manifest updates, segment downloads, buffer health, and state transitions.
 */
@UnstableApi
object LiveStreamLogger {
    private const val TAG = "LiveStreamEngine"
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    // In-memory diagnostic ring buffer for debugging without performance overhead
    private val logBuffer = ArrayDeque<String>(150)

    // Telemetry state tracker
    @Volatile var lastSuccessfulLoadMs: Long = 0L
    @Volatile var lastPlaylistLoadMs: Long = 0L
    @Volatile var lastSegmentLoadMs: Long = 0L
    @Volatile var lastPositionChangeMs: Long = 0L
    @Volatile var totalBufferingEvents: Int = 0
    @Volatile var totalRecoveryEvents: Int = 0
    @Volatile var totalLiveResyncs: Int = 0
    @Volatile var lastKnownHttpCode: Int = 200

    fun log(event: String, details: String = "") {
        val timestamp = dateFormat.format(Date())
        val msg = if (details.isEmpty()) "[$event]" else "[$event] $details"
        Log.d(TAG, "$timestamp $msg")
        synchronized(logBuffer) {
            if (logBuffer.size >= 150) {
                logBuffer.removeFirst()
            }
            logBuffer.addLast("$timestamp: $msg")
        }
    }

    fun w(event: String, details: String = "") {
        val timestamp = dateFormat.format(Date())
        val msg = if (details.isEmpty()) "[$event]" else "[$event] $details"
        Log.w(TAG, "$timestamp $msg")
        synchronized(logBuffer) {
            if (logBuffer.size >= 150) {
                logBuffer.removeFirst()
            }
            logBuffer.addLast("$timestamp: WARN: $msg")
        }
    }

    fun e(event: String, details: String = "", throwable: Throwable? = null) {
        val timestamp = dateFormat.format(Date())
        val msg = if (details.isEmpty()) "[$event]" else "[$event] $details"
        if (throwable != null) {
            Log.e(TAG, "$timestamp $msg", throwable)
        } else {
            Log.e(TAG, "$timestamp $msg")
        }
        synchronized(logBuffer) {
            if (logBuffer.size >= 150) {
                logBuffer.removeFirst()
            }
            logBuffer.addLast("$timestamp: ERROR: $msg ${throwable?.message ?: ""}")
        }
    }

    fun getRecentLogs(): List<String> {
        synchronized(logBuffer) {
            return logBuffer.toList()
        }
    }

    fun resetSession() {
        lastSuccessfulLoadMs = SystemClock.elapsedRealtime()
        lastPlaylistLoadMs = SystemClock.elapsedRealtime()
        lastSegmentLoadMs = SystemClock.elapsedRealtime()
        lastPositionChangeMs = SystemClock.elapsedRealtime()
        totalBufferingEvents = 0
        totalRecoveryEvents = 0
        totalLiveResyncs = 0
        lastKnownHttpCode = 200
    }

    /**
     * Creates an AnalyticsListener to attach to ExoPlayer for deep media telemetry.
     */
    fun createAnalyticsListener(): AnalyticsListener {
        return object : AnalyticsListener {
            override fun onLoadStarted(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: androidx.media3.exoplayer.source.LoadEventInfo,
                mediaLoadData: androidx.media3.exoplayer.source.MediaLoadData
            ) {
                val dataTypeStr = when (mediaLoadData.dataType) {
                    C.DATA_TYPE_MANIFEST -> "MANIFEST"
                    C.DATA_TYPE_MEDIA -> "SEGMENT"
                    C.DATA_TYPE_DRM -> "KEY"
                    else -> "DATA"
                }
                val safeUri = loadEventInfo.uri.toString().substringBefore("?")
                if (dataTypeStr == "MANIFEST") {
                    log("PLAYLIST_LOADING", "Uri: $safeUri")
                } else {
                    log("SEGMENT_LOADING", "Uri: $safeUri (TrackType: ${mediaLoadData.trackType})")
                }
            }

            override fun onLoadCompleted(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: androidx.media3.exoplayer.source.LoadEventInfo,
                mediaLoadData: androidx.media3.exoplayer.source.MediaLoadData
            ) {
                val now = SystemClock.elapsedRealtime()
                lastSuccessfulLoadMs = now
                val dataTypeStr = when (mediaLoadData.dataType) {
                    C.DATA_TYPE_MANIFEST -> {
                        lastPlaylistLoadMs = now
                        "PLAYLIST_LOADED"
                    }
                    C.DATA_TYPE_MEDIA -> {
                        lastSegmentLoadMs = now
                        "SEGMENT_LOADED"
                    }
                    else -> "DATA_LOADED"
                }
                val safeUri = loadEventInfo.uri.toString().substringBefore("?")
                log(dataTypeStr, "Duration: ${loadEventInfo.loadDurationMs}ms, Bytes: ${loadEventInfo.bytesLoaded}, Uri: $safeUri")
            }

            override fun onLoadCanceled(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: androidx.media3.exoplayer.source.LoadEventInfo,
                mediaLoadData: androidx.media3.exoplayer.source.MediaLoadData
            ) {
                val safeUri = loadEventInfo.uri.toString().substringBefore("?")
                log("LOAD_CANCELED", "Uri: $safeUri after ${loadEventInfo.loadDurationMs}ms")
            }

            override fun onLoadError(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: androidx.media3.exoplayer.source.LoadEventInfo,
                mediaLoadData: androidx.media3.exoplayer.source.MediaLoadData,
                error: java.io.IOException,
                wasCanceled: Boolean
            ) {
                val dataTypeStr = when (mediaLoadData.dataType) {
                    C.DATA_TYPE_MANIFEST -> "MANIFEST_ERROR"
                    C.DATA_TYPE_MEDIA -> "SEGMENT_ERROR"
                    else -> "LOAD_ERROR"
                }
                val safeUri = loadEventInfo.uri.toString().substringBefore("?")
                e(dataTypeStr, "Uri: $safeUri, Error: ${error.message}", error)
            }

            override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
                val stateName = when (state) {
                    Player.STATE_IDLE -> "STATE_IDLE"
                    Player.STATE_BUFFERING -> {
                        totalBufferingEvents++
                        "STATE_BUFFERING"
                    }
                    Player.STATE_READY -> "STATE_READY"
                    Player.STATE_ENDED -> "STATE_ENDED"
                    else -> "STATE_$state"
                }
                log("PLAYBACK_STATE_CHANGED", "State: $stateName (EventTime: ${eventTime.realtimeMs})")
            }

            override fun onIsPlayingChanged(eventTime: AnalyticsListener.EventTime, isPlaying: Boolean) {
                log("IS_PLAYING_CHANGED", "isPlaying: $isPlaying")
            }

            override fun onIsLoadingChanged(eventTime: AnalyticsListener.EventTime, isLoading: Boolean) {
                log("IS_LOADING_CHANGED", "isLoading: $isLoading")
            }

            override fun onPositionDiscontinuity(
                eventTime: AnalyticsListener.EventTime,
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                val reasonName = when (reason) {
                    Player.DISCONTINUITY_REASON_AUTO_TRANSITION -> "AUTO_TRANSITION"
                    Player.DISCONTINUITY_REASON_SEEK -> "SEEK"
                    Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> "SEEK_ADJUSTMENT"
                    Player.DISCONTINUITY_REASON_SKIP -> "SKIP"
                    Player.DISCONTINUITY_REASON_REMOVE -> "REMOVE"
                    Player.DISCONTINUITY_REASON_INTERNAL -> "INTERNAL"
                    else -> "REASON_$reason"
                }
                log("POSITION_DISCONTINUITY", "Reason: $reasonName, OldPos: ${oldPosition.positionMs}ms, NewPos: ${newPosition.positionMs}ms")
            }

            override fun onTimelineChanged(eventTime: AnalyticsListener.EventTime, reason: Int) {
                log("TIMELINE_CHANGED", "Reason: $reason, WindowCount: ${eventTime.timeline.windowCount}")
            }

            override fun onTracksChanged(eventTime: AnalyticsListener.EventTime, tracks: androidx.media3.common.Tracks) {
                log("TRACKS_CHANGED", "Groups: ${tracks.groups.size}")
            }

            override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
                e("PLAYER_ERROR_ANALYTICS", "ErrorCode: ${error.errorCode} (${error.errorCodeName}), Msg: ${error.message}", error)
            }
        }
    }
}
