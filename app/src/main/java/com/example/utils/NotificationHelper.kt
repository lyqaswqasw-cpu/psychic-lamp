package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    const val CHANNEL_ID = "plus90_notifications_channel"
    const val CHANNEL_NAME = "إشعارات تطبيق +90"
    const val DOWNLOAD_CHANNEL_ID = "plus90_downloads_channel"
    const val DOWNLOAD_CHANNEL_NAME = "تنزيلات الأفلام والمسلسلات"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val existing = notificationManager?.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "قناة إشعارات البث المباشر والأخبار والمباريات"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager?.createNotificationChannel(channel)
                Log.d(TAG, "Notification Channel '$CHANNEL_ID' ensured successfully.")
            }

            val existingDownload = notificationManager?.getNotificationChannel(DOWNLOAD_CHANNEL_ID)
            if (existingDownload == null) {
                val downloadChannel = NotificationChannel(
                    DOWNLOAD_CHANNEL_ID,
                    DOWNLOAD_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "قناة متابعة تقدم وتنزيل الأفلام وحلقات المسلسلات"
                    enableLights(false)
                    enableVibration(false)
                    setShowBadge(true)
                }
                notificationManager?.createNotificationChannel(downloadChannel)
                Log.d(TAG, "Notification Channel '$DOWNLOAD_CHANNEL_ID' ensured successfully.")
            }
        }
    }

    fun showDownloadProgressNotification(
        context: Context,
        notificationId: Int,
        title: String,
        progressPercent: Int,
        speedText: String,
        isIndeterminate: Boolean = false
    ) {
        try {
            ensureNotificationChannel(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("target_screen", "downloads")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("⬇️ جاري تحميل: $title")
                .setContentText("$progressPercent% • $speedText")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, progressPercent, isIndeterminate)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(notificationId, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Error updating download progress notification: ${e.message}")
        }
    }

    fun showDownloadCompletedNotification(
        context: Context,
        notificationId: Int,
        title: String
    ) {
        try {
            ensureNotificationChannel(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("target_screen", "downloads")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("✅ اكتمل التحميل: $title")
                .setContentText("تم حفظ الملف محلياً وجاهز للتشغيل بدون إنترنت الآن!")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(notificationId, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Error showing download complete notification: ${e.message}")
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(notificationId)
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling notification: ${e.message}")
        }
    }

    suspend fun showSystemNotification(
        context: Context,
        title: String,
        body: String,
        imageUrl: String? = null,
        targetScreen: String = "notifications",
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d(TAG, "POST_NOTIFICATIONS not granted; skipping system notification")
                    return@withContext
                }
            }

            ensureNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("target_screen", targetScreen)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            if (!imageUrl.isNullOrBlank()) {
                try {
                    val url = URL(imageUrl)
                    val connection = url.openConnection()
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.doInput = true
                    connection.connect()
                    val stream = connection.getInputStream()
                    val bitmap: Bitmap? = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        builder.setStyle(
                            NotificationCompat.BigPictureStyle()
                                .bigPicture(bitmap)
                                .setSummaryText(body)
                        )
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Failed to load notification image bitmap: ${e.message}")
                    builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
                }
            } else {
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(notificationId, builder.build())
            Log.d(TAG, "System notification displayed successfully (ID=$notificationId)")
        } catch (e: Throwable) {
            Log.e(TAG, "Error displaying system notification: ${e.message}", e)
        }
    }
}
