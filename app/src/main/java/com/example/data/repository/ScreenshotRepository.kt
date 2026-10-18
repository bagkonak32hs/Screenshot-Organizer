package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.DemoDataProvider
import com.example.data.local.ScreenshotDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.ScreenshotEntity
import com.example.data.remote.GeminiVisionAnalyzer
import com.example.data.scanner.MediaStoreScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class ScanProgressState(
    val isScanning: Boolean = false,
    val currentStep: String = "",
    val currentItem: Int = 0,
    val totalItems: Int = 0,
    val newlyOrganizedCount: Int = 0,
    val categoryBreakdown: Map<String, Int> = emptyMap(),
    val isCompleted: Boolean = false
)

class ScreenshotRepository(private val context: Context) {

    private val db = ScreenshotDatabase.getInstance(context)
    private val screenshotDao = db.screenshotDao()
    private val categoryDao = db.categoryDao()
    private val reminderDao = db.reminderDao()
    private val analyzer = GeminiVisionAnalyzer(context)
    private val scanner = MediaStoreScanner(context)
    private val fileService = com.example.data.file.ScreenshotFileService(context)

    private val _scanProgress = MutableStateFlow(ScanProgressState())
    val scanProgress: StateFlow<ScanProgressState> = _scanProgress.asStateFlow()

    val allScreenshots: Flow<List<ScreenshotEntity>> = screenshotDao.getAllVisibleScreenshots()
    val hiddenScreenshots: Flow<List<ScreenshotEntity>> = screenshotDao.getHiddenScreenshots()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val favoriteScreenshots: Flow<List<ScreenshotEntity>> = screenshotDao.getFavoriteScreenshots()
    val sensitiveScreenshots: Flow<List<ScreenshotEntity>> = screenshotDao.getSensitiveScreenshots()
    val pendingScreenshots: Flow<List<ScreenshotEntity>> = screenshotDao.getPendingScreenshots()
    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    suspend fun initializeDefaultsIfNeeded() = withContext(Dispatchers.IO) {
        // Always insert/update core default categories (work, school, entertainment, personal)
        categoryDao.insertCategories(DemoDataProvider.getDefaultCategories())

        val existingScreenshots = screenshotDao.getAllVisibleScreenshotsSync()
        if (existingScreenshots.isEmpty()) {
            // Insert demo screenshots for high-fidelity initial experience
            screenshotDao.insertScreenshots(DemoDataProvider.getInitialDemoScreenshots())

            // Also add demo reminders
            reminderDao.insertReminder(
                ReminderEntity(
                    screenshotId = 2,
                    title = "İleri Algoritmalar Vize Sınavı",
                    reminderDate = System.currentTimeMillis() + (86_400_000L * 13), // 28 Oct
                    isCompleted = false
                )
            )
            reminderDao.insertReminder(
                ReminderEntity(
                    screenshotId = 8,
                    title = "Diş Kontrolü ve Temizlik Randevusu",
                    reminderDate = System.currentTimeMillis() + (86_400_000L * 9), // 24 Oct
                    isCompleted = false
                )
            )
        }
    }

    fun getScreenshotsByCategory(categoryId: String): Flow<List<ScreenshotEntity>> {
        return screenshotDao.getScreenshotsByCategory(categoryId)
    }

    fun getScreenshotById(id: Long): Flow<ScreenshotEntity?> {
        return screenshotDao.getScreenshotById(id)
    }

    fun getIdeasScreenshots(): Flow<List<ScreenshotEntity>> {
        return screenshotDao.getIdeasScreenshots()
    }

    fun searchScreenshots(query: String): Flow<List<ScreenshotEntity>> {
        return screenshotDao.searchScreenshots(query)
    }

    suspend fun analyzeSingleScreenshot(screenshotId: Long) = withContext(Dispatchers.IO) {
        val item = screenshotDao.getScreenshotByIdSync(screenshotId) ?: return@withContext
        val analysis = analyzer.analyzeScreenshot(item.uri, item.fileName)

        val updated = item.copy(
            categoryId = if (analysis.category.isNotBlank()) analysis.category else item.categoryId,
            subcategory = analysis.subcategory,
            title = analysis.title,
            summary = analysis.summary,
            ocrText = analysis.detected_text,
            confidence = analysis.confidence.toFloat(),
            isSensitive = analysis.contains_sensitive_information,
            isAnalyzed = true,
            analysisStatus = "COMPLETED",
            company = analysis.important_fields.company,
            amount = analysis.important_fields.amount,
            dateStr = analysis.important_fields.date,
            invoiceNumber = analysis.important_fields.invoice_number,
            address = analysis.important_fields.address,
            phoneNumber = analysis.important_fields.phone_number,
            orderNumber = analysis.important_fields.order_number,
            trackingNumber = analysis.important_fields.tracking_number,
            tagsJson = analysis.suggested_tags.joinToString(", "),
            suggestedCategoriesJson = analysis.suggested_categories.joinToString(", "),
            actionSuggestion = analysis.action_suggestions.firstOrNull()
        )

        // Physically move the file into the corresponding category folder
        val moveResult = fileService.moveScreenshotToCategory(updated, updated.categoryId)
        val finalUpdated = if (moveResult.isSuccess && moveResult.getOrNull()?.newUri != null) {
            updated.copy(uri = moveResult.getOrNull()!!.newUri)
        } else {
            updated
        }
        screenshotDao.updateScreenshot(finalUpdated)
    }

    suspend fun importAndAnalyzeUris(uris: List<Uri>) = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext

        _scanProgress.value = ScanProgressState(
            isScanning = true,
            currentStep = "Görseller hazırlanıyor...",
            currentItem = 0,
            totalItems = uris.size
        )

        val categoryCounter = mutableMapOf<String, Int>()

        uris.forEachIndexed { index, uri ->
            val fileName = scanner.getFileNameFromUri(uri)
            _scanProgress.value = _scanProgress.value.copy(
                currentStep = "Görsel ${index + 1}/${uris.size}: Metinler okunuyor ve AI analizi yapılıyor...",
                currentItem = index + 1
            )

            // Analyze
            val analysis = analyzer.analyzeScreenshot(uri.toString(), fileName)

            val category = analysis.category
            categoryCounter[category] = (categoryCounter[category] ?: 0) + 1

            val entity = ScreenshotEntity(
                uri = uri.toString(),
                fileName = fileName,
                categoryId = category,
                subcategory = analysis.subcategory,
                title = analysis.title,
                summary = analysis.summary,
                ocrText = analysis.detected_text,
                confidence = analysis.confidence.toFloat(),
                isSensitive = analysis.contains_sensitive_information,
                isAnalyzed = true,
                analysisStatus = "COMPLETED",
                company = analysis.important_fields.company,
                amount = analysis.important_fields.amount,
                dateStr = analysis.important_fields.date,
                invoiceNumber = analysis.important_fields.invoice_number,
                address = analysis.important_fields.address,
                phoneNumber = analysis.important_fields.phone_number,
                orderNumber = analysis.important_fields.order_number,
                trackingNumber = analysis.important_fields.tracking_number,
                tagsJson = analysis.suggested_tags.joinToString(", "),
                suggestedCategoriesJson = analysis.suggested_categories.joinToString(", "),
                actionSuggestion = analysis.action_suggestions.firstOrNull(),
                isDemo = false
            )
            val insertedId = screenshotDao.insertScreenshot(entity)
            val insertedEntity = entity.copy(id = insertedId)

            // Physically move into category folder on device file system (e.g. Finance, Work)
            val moveResult = fileService.moveScreenshotToCategory(insertedEntity, category)
            moveResult.onSuccess { res ->
                if (res.isSuccess && res.newUri != insertedEntity.uri) {
                    screenshotDao.updateScreenshot(insertedEntity.copy(uri = res.newUri))
                }
            }
        }

        _scanProgress.value = ScanProgressState(
            isScanning = false,
            currentStep = "Analiz Tamamlandı!",
            currentItem = uris.size,
            totalItems = uris.size,
            newlyOrganizedCount = uris.size,
            categoryBreakdown = categoryCounter,
            isCompleted = true
        )
    }

    suspend fun scanDeviceFolderAndAnalyze() = withContext(Dispatchers.IO) {
        _scanProgress.value = ScanProgressState(
            isScanning = true,
            currentStep = "Cihazdaki ekran görüntüsü klasörü taranıyor...",
            currentItem = 0,
            totalItems = 0
        )

        val existingUris = try {
            screenshotDao.getAllUrisSync().toSet()
        } catch (e: Exception) {
            emptySet()
        }

        val discovered = scanner.scanNewUnsortedScreenshots(existingUris)
        if (discovered.isEmpty()) {
            _scanProgress.value = ScanProgressState(
                isScanning = false,
                currentStep = "Tüm ekran görüntüleri düzenli! Yeni taranacak görsel bulunamadı.",
                isCompleted = true
            )
            return@withContext
        }

        val urisToAnalyze = discovered.map { it.uri }
        importAndAnalyzeUris(urisToAnalyze)
    }

    fun triggerWorkManagerScan() {
        com.example.data.worker.ScreenshotScanWorker.triggerImmediateScan(context)
    }

    fun hasApiKey(): Boolean = analyzer.hasApiKey()

    suspend fun performGeminiOcrAndTagging(screenshotId: Long): Result<ScreenshotEntity> = withContext(Dispatchers.IO) {
        val existing = screenshotDao.getScreenshotByIdSync(screenshotId)
            ?: return@withContext Result.failure(Exception("Görsel veritabanında bulunamadı"))

        try {
            val analysis = analyzer.performOcrAndGenerateTags(existing.uri, existing.fileName)
            val updated = existing.copy(
                ocrText = analysis.detected_text.ifBlank { existing.ocrText },
                tagsJson = analysis.suggested_tags.joinToString(", ").ifBlank { existing.tagsJson },
                categoryId = if (existing.categoryId == "other" && analysis.category != "other") analysis.category else existing.categoryId,
                isAnalyzed = true,
                analysisStatus = "COMPLETED",
                company = analysis.important_fields.company ?: existing.company,
                amount = analysis.important_fields.amount ?: existing.amount,
                dateStr = analysis.important_fields.date ?: existing.dateStr,
                invoiceNumber = analysis.important_fields.invoice_number ?: existing.invoiceNumber,
                address = analysis.important_fields.address ?: existing.address,
                phoneNumber = analysis.important_fields.phone_number ?: existing.phoneNumber,
                summary = analysis.summary.ifBlank { existing.summary }
            )
            screenshotDao.updateScreenshot(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun classifyScreenshotWithGemini(screenshotId: Long): Result<ScreenshotEntity> = withContext(Dispatchers.IO) {
        val existing = screenshotDao.getScreenshotByIdSync(screenshotId)
            ?: return@withContext Result.failure(Exception("Görsel bulunamadı"))

        try {
            val analysis = analyzer.analyzeScreenshot(existing.uri, existing.fileName)
            val updated = existing.copy(
                categoryId = analysis.category,
                subcategory = analysis.subcategory,
                title = analysis.title.ifBlank { existing.title },
                summary = analysis.summary.ifBlank { existing.summary },
                ocrText = analysis.detected_text.ifBlank { existing.ocrText },
                confidence = analysis.confidence.toFloat(),
                isSensitive = analysis.contains_sensitive_information,
                isAnalyzed = true,
                analysisStatus = "COMPLETED",
                company = analysis.important_fields.company ?: existing.company,
                amount = analysis.important_fields.amount ?: existing.amount,
                dateStr = analysis.important_fields.date ?: existing.dateStr,
                invoiceNumber = analysis.important_fields.invoice_number ?: existing.invoiceNumber,
                address = analysis.important_fields.address ?: existing.address,
                phoneNumber = analysis.important_fields.phone_number ?: existing.phoneNumber,
                tagsJson = analysis.suggested_tags.joinToString(", ").ifBlank { existing.tagsJson },
                suggestedCategoriesJson = analysis.suggested_categories.joinToString(", "),
                actionSuggestion = analysis.action_suggestions.firstOrNull() ?: existing.actionSuggestion
            )
            val moveResult = fileService.moveScreenshotToCategory(updated, updated.categoryId)
            val finalUpdated = if (moveResult.isSuccess && moveResult.getOrNull()?.newUri != null) {
                updated.copy(uri = moveResult.getOrNull()!!.newUri)
            } else {
                updated
            }
            screenshotDao.updateScreenshot(finalUpdated)
            Result.success(finalUpdated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun suggestCategoriesWithGemini(screenshotId: Long): Result<List<String>> = withContext(Dispatchers.IO) {
        val existing = screenshotDao.getScreenshotByIdSync(screenshotId)
            ?: return@withContext Result.failure(Exception("Görsel bulunamadı"))

        try {
            val analysis = analyzer.analyzeScreenshot(existing.uri, existing.fileName)
            val suggested = if (analysis.suggested_categories.isNotEmpty()) {
                analysis.suggested_categories
            } else {
                when (analysis.category) {
                    "work" -> listOf("Work", "Ideas", "Personal")
                    "ideas" -> listOf("Ideas", "Work", "Personal")
                    else -> listOf("Personal", "Ideas", "Work")
                }
            }
            screenshotDao.updateSuggestedCategories(screenshotId, suggested.joinToString(", "))
            Result.success(suggested)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun classifyAllScreenshotsWithGemini() = withContext(Dispatchers.IO) {
        val items = screenshotDao.getAllVisibleScreenshotsSync()
        if (items.isEmpty()) return@withContext

        _scanProgress.value = ScanProgressState(
            isScanning = true,
            currentStep = "Gemini API ile otomatik sınıflandırılıyor...",
            currentItem = 0,
            totalItems = items.size
        )

        items.forEachIndexed { index, item ->
            _scanProgress.value = _scanProgress.value.copy(
                currentStep = "Sınıflandırılıyor: ${item.fileName} (${index + 1}/${items.size})",
                currentItem = index + 1
            )
            classifyScreenshotWithGemini(item.id)
        }

        _scanProgress.value = ScanProgressState(
            isScanning = false,
            currentStep = "Tüm görseller Gemini ile başarıyla sınıflandırıldı!"
        )
    }

    fun dismissScanProgress() {
        _scanProgress.value = ScanProgressState()
    }

    suspend fun updateCategory(screenshotId: Long, categoryId: String) = withContext(Dispatchers.IO) {
        val existing = screenshotDao.getScreenshotByIdSync(screenshotId)
        if (existing != null) {
            val moveResult = fileService.moveScreenshotToCategory(existing, categoryId)
            val newUri = moveResult.getOrNull()?.newUri ?: existing.uri
            screenshotDao.updateScreenshot(existing.copy(categoryId = categoryId, uri = newUri))
        } else {
            screenshotDao.updateCategory(screenshotId, categoryId)
        }
    }

    suspend fun toggleFavorite(screenshotId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        screenshotDao.updateFavorite(screenshotId, isFavorite)
    }

    suspend fun toggleHidden(screenshotId: Long, isHidden: Boolean) = withContext(Dispatchers.IO) {
        screenshotDao.updateHidden(screenshotId, isHidden)
    }

    suspend fun updateScreenshot(screenshot: ScreenshotEntity) = withContext(Dispatchers.IO) {
        screenshotDao.updateScreenshot(screenshot)
    }

    suspend fun deleteScreenshot(screenshot: ScreenshotEntity) = withContext(Dispatchers.IO) {
        screenshotDao.deleteScreenshot(screenshot)
    }

    suspend fun deleteMultiple(ids: List<Long>) = withContext(Dispatchers.IO) {
        screenshotDao.deleteMultiple(ids)
    }

    suspend fun createCustomCategory(name: String, iconName: String, colorHex: String, description: String) = withContext(Dispatchers.IO) {
        val id = "custom_" + System.currentTimeMillis()
        val cat = CategoryEntity(
            id = id,
            name = name,
            iconName = iconName,
            colorHex = colorHex,
            isDefault = false,
            isCustom = true,
            description = description
        )
        categoryDao.insertCategory(cat)
    }

    suspend fun addReminder(screenshotId: Long, title: String, date: Long) = withContext(Dispatchers.IO) {
        reminderDao.insertReminder(
            ReminderEntity(
                screenshotId = screenshotId,
                title = title,
                reminderDate = date
            )
        )
    }

    suspend fun toggleReminderCompleted(reminderId: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        reminderDao.setCompleted(reminderId, completed)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        screenshotDao.deleteAll()
        categoryDao.insertCategories(DemoDataProvider.getDefaultCategories())
    }

    suspend fun removeDemoData() = withContext(Dispatchers.IO) {
        screenshotDao.deleteDemoData()
    }

    suspend fun moveScreenshotPhysically(
        screenshotId: Long,
        targetCategoryId: String? = null
    ): Result<com.example.data.file.FileMoveResult> = withContext(Dispatchers.IO) {
        val item = screenshotDao.getScreenshotByIdSync(screenshotId)
            ?: return@withContext Result.failure(Exception("Görsel veritabanında bulunamadı"))
        val targetCat = targetCategoryId ?: item.categoryId
        val res = fileService.moveScreenshotToCategory(item, targetCat)
        res.onSuccess { moveResult ->
            if (moveResult.isSuccess && moveResult.newUri != item.uri) {
                screenshotDao.updateScreenshot(item.copy(categoryId = targetCat, uri = moveResult.newUri))
            }
        }
        res
    }

    suspend fun organizeAllScreenshotsPhysically(
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Result<com.example.data.file.StorageStatsInfo> = withContext(Dispatchers.IO) {
        try {
            val list = screenshotDao.getAllVisibleScreenshotsSync()
            val total = list.size
            list.forEachIndexed { index, item ->
                val folder = com.example.data.file.ScreenshotFileService.normalizeCategoryName(item.categoryId)
                onProgress(index + 1, total, folder)
                val res = fileService.moveScreenshotToCategory(item, item.categoryId)
                res.onSuccess { moveResult ->
                    if (moveResult.isSuccess && moveResult.newUri != item.uri) {
                        screenshotDao.updateScreenshot(item.copy(uri = moveResult.newUri))
                    }
                }
            }
            Result.success(fileService.getStorageStats())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStorageStats(): com.example.data.file.StorageStatsInfo = withContext(Dispatchers.IO) {
        fileService.getStorageStats()
    }

    fun getOrganizationConfig(): com.example.data.file.FileOrganizationConfig {
        return fileService.getOrganizationConfig()
    }

    fun saveOrganizationConfig(config: com.example.data.file.FileOrganizationConfig) {
        fileService.saveOrganizationConfig(config)
    }
}
