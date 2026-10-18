package com.example.data.file

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.data.model.ScreenshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale

data class FileMoveResult(
    val screenshotId: Long,
    val originalUri: String,
    val newUri: String,
    val targetFilePath: String,
    val categoryFolder: String,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

data class StorageStatsInfo(
    val totalOrganizedFiles: Int,
    val totalSizeBytes: Long,
    val rootPath: String,
    val categoryCounts: Map<String, Int>
) {
    val formattedSize: String
        get() {
            if (totalSizeBytes <= 0) return "0 KB"
            val kb = totalSizeBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format(Locale.getDefault(), "%.1f MB", mb)
            } else {
                String.format(Locale.getDefault(), "%.1f KB", kb)
            }
        }
}

class ScreenshotFileService(private val context: Context) {

    private val orgPreferences = FileOrganizationPreferences(context)

    fun getOrganizationConfig(): FileOrganizationConfig = orgPreferences.loadConfig()

    fun saveOrganizationConfig(config: FileOrganizationConfig) = orgPreferences.saveConfig(config)

    companion object {
        private const val TAG = "ScreenshotFileService"
        const val ROOT_ORGANIZER_FOLDER = "ScreenshotOrganizer"

        fun normalizeCategoryName(categoryId: String): String {
            return when (categoryId.trim().lowercase()) {
                "finance", "finans" -> "Finance"
                "work", "is", "iş" -> "Work"
                "invoice", "fatura" -> "Invoice"
                "receipt", "makbuz", "fiş", "fis" -> "Receipt"
                "chat", "sohbet", "mesaj" -> "Chat"
                "travel", "seyahat" -> "Travel"
                "id_card", "kimlik" -> "ID_Card"
                "notes", "not" -> "Notes"
                "social", "sosyal" -> "Social"
                "other", "diger", "diğer" -> "Other"
                else -> categoryId.trim()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        }
    }

    /**
     * Returns the physical root folder for organized screenshots.
     * Prefers external files pictures directory; falls back to internal filesDir.
     */
    fun getOrganizerRootDirectory(): File {
        val extPics = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val targetRoot = if (extPics != null) {
            File(extPics, ROOT_ORGANIZER_FOLDER)
        } else {
            File(context.filesDir, ROOT_ORGANIZER_FOLDER)
        }
        if (!targetRoot.exists()) {
            targetRoot.mkdirs()
        }
        return targetRoot
    }

    /**
     * Returns the directory for a specific category (e.g. Pictures/ScreenshotOrganizer/Finance)
     */
    fun getCategoryDirectory(categoryId: String): File {
        val config = getOrganizationConfig()
        val dummy = ScreenshotEntity(uri = "", fileName = "", categoryId = categoryId)
        val (subFolder, _) = config.resolvePathAndFileName(dummy)
        val catDir = File(getOrganizerRootDirectory(), subFolder)
        if (!catDir.exists()) {
            catDir.mkdirs()
        }
        return catDir
    }

    /**
     * Physically moves a screenshot into its designated category and sub-folder structure,
     * applying the user's custom naming convention.
     */
    suspend fun moveScreenshotToCategory(
        screenshot: ScreenshotEntity,
        targetCategoryId: String = screenshot.categoryId
    ): Result<FileMoveResult> = withContext(Dispatchers.IO) {
        try {
            val config = getOrganizationConfig()
            val effectiveScreenshot = if (targetCategoryId != screenshot.categoryId) {
                screenshot.copy(categoryId = targetCategoryId)
            } else {
                screenshot
            }

            // Resolve custom subfolder structure and custom file name
            val (subFolderPath, customFileName) = config.resolvePathAndFileName(effectiveScreenshot)
            val targetDir = File(getOrganizerRootDirectory(), subFolderPath)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            // Name collision avoidance
            var destinationFile = File(targetDir, customFileName)
            var counter = 1
            val baseName = customFileName.substringBeforeLast(".")
            val ext = customFileName.substringAfterLast(".", "jpg")

            while (destinationFile.exists() && destinationFile.length() > 0) {
                // If it is already pointing to the exact same file in the same folder, avoid duplicate copy
                if (screenshot.uri == Uri.fromFile(destinationFile).toString()) {
                    return@withContext Result.success(
                        FileMoveResult(
                            screenshotId = screenshot.id,
                            originalUri = screenshot.uri,
                            newUri = screenshot.uri,
                            targetFilePath = destinationFile.absolutePath,
                            categoryFolder = subFolderPath,
                            isSuccess = true
                        )
                    )
                }
                destinationFile = File(targetDir, "${baseName}_$counter.$ext")
                counter++
            }

            // Open input stream from source URI (file://, content://, or android.resource://)
            val sourceUri = Uri.parse(screenshot.uri)
            val inputStream: InputStream? = when (sourceUri.scheme) {
                "file" -> {
                    val srcFile = File(sourceUri.path ?: "")
                    if (srcFile.exists()) srcFile.inputStream() else null
                }
                "content" -> {
                    context.contentResolver.openInputStream(sourceUri)
                }
                else -> {
                    // Try file path fallback
                    val directFile = File(screenshot.uri)
                    if (directFile.exists()) {
                        directFile.inputStream()
                    } else {
                        context.contentResolver.openInputStream(sourceUri)
                    }
                }
            }

            if (inputStream == null) {
                return@withContext Result.failure(
                    IllegalStateException("Kaynak dosya açılamadı: ${screenshot.uri}")
                )
            }

            // Copy to target destination
            destinationFile.outputStream().use { outputStream ->
                inputStream.use { input ->
                    input.copyTo(outputStream)
                }
            }

            // If old file was in an old organized category folder, clean up the previous file
            if (sourceUri.scheme == "file") {
                val oldFile = File(sourceUri.path ?: "")
                if (oldFile.exists() && oldFile.absolutePath != destinationFile.absolutePath) {
                    if (oldFile.absolutePath.contains(ROOT_ORGANIZER_FOLDER)) {
                        try {
                            oldFile.delete()
                        } catch (e: Exception) {
                            Log.w(TAG, "Eski dosya silinemedi: ${oldFile.absolutePath}", e)
                        }
                    }
                }
            }

            val newUriString = Uri.fromFile(destinationFile).toString()
            Log.d(TAG, "Görsel taşındı: ${destinationFile.absolutePath}")

            Result.success(
                FileMoveResult(
                    screenshotId = screenshot.id,
                    originalUri = screenshot.uri,
                    newUri = newUriString,
                    targetFilePath = destinationFile.absolutePath,
                    categoryFolder = subFolderPath,
                    isSuccess = true
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Dosya taşıma hatası: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Batch moves a list of screenshots to their respective category folders.
     */
    suspend fun moveScreenshotsBatch(
        screenshots: List<ScreenshotEntity>,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): List<FileMoveResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileMoveResult>()
        val total = screenshots.size

        screenshots.forEachIndexed { index, screenshot ->
            val folder = normalizeCategoryName(screenshot.categoryId)
            onProgress(index + 1, total, folder)
            val result = moveScreenshotToCategory(screenshot, screenshot.categoryId)
            result.onSuccess {
                results.add(it)
            }.onFailure { err ->
                results.add(
                    FileMoveResult(
                        screenshotId = screenshot.id,
                        originalUri = screenshot.uri,
                        newUri = screenshot.uri,
                        targetFilePath = "",
                        categoryFolder = folder,
                        isSuccess = false,
                        errorMessage = err.message
                    )
                )
            }
        }
        results
    }

    /**
     * Calculates storage stats for the organized folders
     */
    suspend fun getStorageStats(): StorageStatsInfo = withContext(Dispatchers.IO) {
        val rootDir = getOrganizerRootDirectory()
        var totalFiles = 0
        var totalBytes = 0L
        val categoryCounts = mutableMapOf<String, Int>()

        if (rootDir.exists() && rootDir.isDirectory) {
            rootDir.listFiles()?.forEach { catFolder ->
                if (catFolder.isDirectory) {
                    val catName = catFolder.name
                    val files = catFolder.listFiles { f -> !f.isDirectory && !f.name.startsWith(".") } ?: emptyArray()
                    val count = files.size
                    categoryCounts[catName] = count
                    totalFiles += count
                    files.forEach { file ->
                        totalBytes += file.length()
                    }
                }
            }
        }

        StorageStatsInfo(
            totalOrganizedFiles = totalFiles,
            totalSizeBytes = totalBytes,
            rootPath = rootDir.absolutePath,
            categoryCounts = categoryCounts
        )
    }
}
