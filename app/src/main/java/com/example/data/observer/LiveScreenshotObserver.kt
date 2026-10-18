package com.example.data.observer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScreenshotDetectionEvent(
    val uri: Uri,
    val fileName: String,
    val timestamp: Long = System.currentTimeMillis()
)

class LiveScreenshotObserver(
    private val context: Context,
    private val onScreenshotDetected: (Uri, String) -> Unit = { _, _ -> }
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private var isObserving = false
    private var lastHandledUriString: String? = null
    private var lastHandledTimestamp: Long = 0

    init {
        createNotificationChannel(context)
    }

    fun start() {
        if (isObserving) return
        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                this
            )
            isObserving = true
        } catch (_: Exception) {}
    }

    fun stop() {
        if (!isObserving) return
        try {
            context.contentResolver.unregisterContentObserver(this)
            isObserving = false
        } catch (_: Exception) {}
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        checkLatestScreenshot()
    }

    private fun checkLatestScreenshot() {
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    val displayName = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)) ?: ""
                    val contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())

                    val lower = displayName.lowercase()
                    val isScreenshot = lower.contains("screenshot") || lower.contains("ekran") || lower.contains("capture")

                    val now = System.currentTimeMillis()
                    if (isScreenshot && contentUri.toString() != lastHandledUriString && (now - lastHandledTimestamp > 3000)) {
                        lastHandledUriString = contentUri.toString()
                        lastHandledTimestamp = now

                        _latestDetectedEvent.value = ScreenshotDetectionEvent(contentUri, displayName, now)
                        showDetectionNotification(context, displayName, contentUri)
                        onScreenshotDetected(contentUri, displayName)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    companion object {
        const val CHANNEL_ID = "screenshot_live_detector_channel"
        const val NOTIFICATION_ID = 2026

        private val _latestDetectedEvent = MutableStateFlow<ScreenshotDetectionEvent?>(null)
        val latestDetectedEvent: StateFlow<ScreenshotDetectionEvent?> = _latestDetectedEvent.asStateFlow()

        fun clearLatestEvent() {
            _latestDetectedEvent.value = null
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Anında Ekran Görüntüsü İzleyici",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Yeni ekran görüntüsü alındığında anında bildirim gönderir"
                    enableVibration(true)
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }

        fun showDetectionNotification(context: Context, fileName: String, uri: Uri? = null) {
            createNotificationChannel(context)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("from_screenshot_detection", true)
                uri?.let { putExtra("detected_uri", it.toString()) }
            }
            val pendingOpen = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.hero_organizer)
                .setContentTitle("📸 Yeni Ekran Görüntüsü Tespit Edildi")
                .setContentText("\"$fileName\" kaydedildi. Yapay zekâ ile anında organize edin.")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "Yeni ekran görüntünüz algılandı. Dokunarak otomatik kategorilendirin, OCR ile metinleri kopyalayın veya gereksizse hemen silin."
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingOpen)
                .setAutoCancel(true)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        }

        fun triggerTestDetection(context: Context) {
            val dummyName = "Screenshot_${System.currentTimeMillis()}.png"
            val dummyUri = Uri.parse("demo://screenshot_test_${System.currentTimeMillis()}")
            _latestDetectedEvent.value = ScreenshotDetectionEvent(dummyUri, dummyName)
            showDetectionNotification(context, dummyName, dummyUri)
        }
    }
}
