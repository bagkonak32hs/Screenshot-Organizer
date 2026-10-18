package com.example.data.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DiscoveredMedia(
    val uri: Uri,
    val fileName: String,
    val dateAdded: Long,
    val size: Long,
    val folderPath: String = ""
)

class MediaStoreScanner(private val context: Context) {

    /**
     * Scans the device's screenshot directory specifically (Pictures/Screenshots, DCIM/Screenshots,
     * or any directory named Screenshots / Ekran Görüntüleri) and returns discovered screenshots.
     */
    suspend fun scanDeviceScreenshots(): List<DiscoveredMedia> = withContext(Dispatchers.IO) {
        val resultList = mutableListOf<DiscoveredMedia>()

        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Images.Media.RELATIVE_PATH)
                add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            } else {
                add(MediaStore.Images.Media.DATA)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                val relativePathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    it.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                } else -1

                val bucketColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    it.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                } else -1

                val dataColumn = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    it.getColumnIndex(MediaStore.Images.Media.DATA)
                } else -1

                while (it.moveToNext() && resultList.size < 200) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn) ?: "screenshot_$id.jpg"
                    val dateAdded = it.getLong(dateColumn) * 1000L
                    val size = it.getLong(sizeColumn)

                    val relativePath = if (relativePathColumn != -1) it.getString(relativePathColumn) ?: "" else ""
                    val bucketName = if (bucketColumn != -1) it.getString(bucketColumn) ?: "" else ""
                    val dataPath = if (dataColumn != -1) it.getString(dataColumn) ?: "" else ""

                    // Check if it originates from a Screenshot folder or matches screenshot filename
                    val isScreenshotFolder = relativePath.contains("Screenshots", ignoreCase = true) ||
                            relativePath.contains("Ekran görüntüleri", ignoreCase = true) ||
                            bucketName.contains("Screenshots", ignoreCase = true) ||
                            bucketName.contains("Ekran görüntüleri", ignoreCase = true) ||
                            dataPath.contains("/Screenshots/", ignoreCase = true) ||
                            dataPath.contains("/Ekran görüntüleri/", ignoreCase = true)

                    val isScreenshotName = name.contains("screenshot", ignoreCase = true) ||
                            name.contains("ekran", ignoreCase = true) ||
                            name.startsWith("scr_", ignoreCase = true) ||
                            name.startsWith("capture", ignoreCase = true)

                    if (isScreenshotFolder || isScreenshotName) {
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        resultList.add(
                            DiscoveredMedia(
                                uri = contentUri,
                                fileName = name,
                                dateAdded = dateAdded,
                                size = size,
                                folderPath = if (relativePath.isNotBlank()) relativePath else bucketName
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MediaStoreScanner", "Error scanning media store for screenshots", e)
        }

        resultList
    }

    /**
     * Filters discovered screenshots against known URIs already persisted in the database.
     * Returns only the newly discovered unsorted images.
     */
    suspend fun scanNewUnsortedScreenshots(existingUris: Set<String>): List<DiscoveredMedia> = withContext(Dispatchers.IO) {
        val allDeviceScreenshots = scanDeviceScreenshots()
        allDeviceScreenshots.filter { discovered ->
            !existingUris.contains(discovered.uri.toString())
        }
    }

    fun getFileNameFromUri(uri: Uri): String {
        var name = "screenshot_${System.currentTimeMillis()}.jpg"
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val cursorName = it.getString(nameIndex)
                        if (!cursorName.isNullOrBlank()) {
                            name = cursorName
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return name
    }
}
