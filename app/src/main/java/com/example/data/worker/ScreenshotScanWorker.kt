package com.example.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.local.ScreenshotDatabase
import com.example.data.scanner.MediaStoreScanner
import java.util.concurrent.TimeUnit

class ScreenshotScanWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting periodic screenshot directory scan...")
        return try {
            val scanner = MediaStoreScanner(appContext)
            val db = ScreenshotDatabase.getInstance(appContext)
            val screenshotDao = db.screenshotDao()

            // 1. Get all known URIs from local database
            val existingUris = try {
                screenshotDao.getAllUrisSync().toSet()
            } catch (e: Exception) {
                emptySet()
            }

            // 2. Scan the device screenshot directory for new unsorted images
            val newUnsortedImages = scanner.scanNewUnsortedScreenshots(existingUris)
            val pendingInDb = try {
                screenshotDao.getPendingCountSync()
            } catch (e: Exception) {
                0
            }

            val totalUnsorted = newUnsortedImages.size + pendingInDb
            val now = System.currentTimeMillis()

            // 3. Save scan audit info in SharedPreferences
            saveScanTimestamp(appContext, now, totalUnsorted)

            Log.d(TAG, "Scan complete: found ${newUnsortedImages.size} new images, $pendingInDb pending in DB.")

            // 4. If unsorted images exist, notify the user
            if (totalUnsorted > 0) {
                sendDiscoveredNotification(totalUnsorted)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during periodic screenshot scan", e)
            Result.retry()
        }
    }

    private fun sendDiscoveredNotification(count: Int) {
        val channelId = NOTIFICATION_CHANNEL_ID
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel on Android O (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Yeni Ekran Görüntüleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Cihazda yeni düzenlenmemiş ekran görüntüleri algılandığında haber verir"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Main Tap Intent: Launches MainActivity with EXTRA_START_SCAN = true
        val mainIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_START_SCAN, true)
            putExtra(EXTRA_UNSORTED_COUNT, count)
        }
        val mainPendingIntent = PendingIntent.getActivity(
            appContext,
            REQUEST_CODE_MAIN,
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action 1 Intent: "Şimdi Düzenle"
        val organizeIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_START_SCAN, true)
            action = ACTION_ORGANIZE_NOW
        }
        val organizePendingIntent = PendingIntent.getActivity(
            appContext,
            REQUEST_CODE_ORGANIZE,
            organizeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Notification builder with rich Material styling
        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Yeni Ekran Görüntüleri Düzenlenmeyi Bekliyor")
            .setContentText("$count yeni ekran görüntüsü algılandı. Yapay zekâ ile otomatik düzenlemek ister misin?")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$count adet düzenlenmemiş ekran görüntüsü bulundu.\n" +
                            "Gemini Vision ile fatura, dekont, adres ve konuşmalarınızı anında akıllı koleksiyonlara ayırabilirsiniz."
                )
            )
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .addAction(
                R.mipmap.ic_launcher,
                "Şimdi Düzenle",
                organizePendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val TAG = "ScreenshotScanWorker"
        const val WORK_NAME_PERIODIC = "periodic_screenshot_scan"
        const val WORK_NAME_ONE_TIME = "onetime_screenshot_scan"
        const val NOTIFICATION_CHANNEL_ID = "screenshot_scan_channel"
        const val NOTIFICATION_ID = 2001

        const val EXTRA_START_SCAN = "extra_start_scan"
        const val EXTRA_UNSORTED_COUNT = "extra_unsorted_count"
        const val ACTION_ORGANIZE_NOW = "com.example.action.ORGANIZE_NOW"

        private const val REQUEST_CODE_MAIN = 101
        private const val REQUEST_CODE_ORGANIZE = 102
        private const val PREFS_NAME = "screenshot_worker_prefs"
        private const val KEY_LAST_SCAN_TIME = "last_scan_time"
        private const val KEY_LAST_UNSORTED_COUNT = "last_unsorted_count"

        fun schedulePeriodicScan(
            context: Context,
            intervalHours: Long = 4L,
            wifiOnly: Boolean = false,
            chargingOnly: Boolean = false
        ) {
            val constraints = Constraints.Builder().apply {
                if (wifiOnly) {
                    setRequiredNetworkType(NetworkType.UNMETERED)
                }
                if (chargingOnly) {
                    setRequiresCharging(true)
                }
            }.build()

            // Minimum interval in WorkManager is 15 minutes
            val validIntervalHours = intervalHours.coerceAtLeast(1L)

            val request = PeriodicWorkRequestBuilder<ScreenshotScanWorker>(
                validIntervalHours, TimeUnit.HOURS,
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Log.d(TAG, "Periodic scan scheduled every $validIntervalHours hours (wifiOnly=$wifiOnly, chargingOnly=$chargingOnly)")
        }

        fun triggerImmediateScan(context: Context) {
            val request = OneTimeWorkRequestBuilder<ScreenshotScanWorker>()
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONE_TIME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "Immediate one-time scan enqueued via WorkManager.")
        }

        fun cancelPeriodicScan(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
            Log.d(TAG, "Periodic scan cancelled.")
        }

        private fun saveScanTimestamp(context: Context, timestamp: Long, count: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_SCAN_TIME, timestamp)
                .putInt(KEY_LAST_UNSORTED_COUNT, count)
                .apply()
        }

        fun getLastScanInfo(context: Context): Pair<Long, Int> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val time = prefs.getLong(KEY_LAST_SCAN_TIME, 0L)
            val count = prefs.getInt(KEY_LAST_UNSORTED_COUNT, 0)
            return Pair(time, count)
        }
    }
}
