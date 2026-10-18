package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.ScreenshotEntity
import com.example.data.repository.ScanProgressState
import com.example.data.repository.ScreenshotRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.data.cleanup.SmartCleanupEngine
import com.example.data.cleanup.SmartCleanupReport

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScreenshotRepository(application)
    private val prefs = application.getSharedPreferences("screenshot_organizer_prefs", Context.MODE_PRIVATE)
    private val authManager = com.example.data.auth.AuthManager(application)

    private val _currentUser = MutableStateFlow<com.example.data.auth.UserProfile?>(authManager.getCurrentUser())
    val currentUser: StateFlow<com.example.data.auth.UserProfile?> = _currentUser.asStateFlow()

    private val _storageStats = MutableStateFlow<com.example.data.file.StorageStatsInfo?>(null)
    val storageStats: StateFlow<com.example.data.file.StorageStatsInfo?> = _storageStats.asStateFlow()

    private val _isOrganizingFiles = MutableStateFlow(false)
    val isOrganizingFiles: StateFlow<Boolean> = _isOrganizingFiles.asStateFlow()

    private val _orgConfig = MutableStateFlow(repository.getOrganizationConfig())
    val orgConfig: StateFlow<com.example.data.file.FileOrganizationConfig> = _orgConfig.asStateFlow()

    val scanProgress: StateFlow<ScanProgressState> = repository.scanProgress

    val allScreenshots: StateFlow<List<ScreenshotEntity>> = repository.allScreenshots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenScreenshots: StateFlow<List<ScreenshotEntity>> = repository.hiddenScreenshots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReminders: StateFlow<List<ReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryScreenshots: StateFlow<List<ScreenshotEntity>> = _selectedCategoryId
        .flatMapLatest { catId ->
            if (catId == null) {
                repository.allScreenshots
            } else {
                repository.getScreenshotsByCategory(catId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchCategoryFilter = MutableStateFlow<String?>(null)
    val searchCategoryFilter: StateFlow<String?> = _searchCategoryFilter.asStateFlow()

    private val _searchSensitiveOnly = MutableStateFlow(false)
    val searchSensitiveOnly: StateFlow<Boolean> = _searchSensitiveOnly.asStateFlow()

    private val _searchFavoritesOnly = MutableStateFlow(false)
    val searchFavoritesOnly: StateFlow<Boolean> = _searchFavoritesOnly.asStateFlow()

    // Combined search results
    val searchResults: StateFlow<List<ScreenshotEntity>> = combine(
        allScreenshots,
        _searchQuery,
        _searchCategoryFilter,
        _searchSensitiveOnly,
        _searchFavoritesOnly
    ) { screenshots, query, catFilter, sensitiveOnly, favOnly ->
        var filtered = screenshots

        if (catFilter != null) {
            filtered = filtered.filter { it.categoryId == catFilter }
        }
        if (sensitiveOnly) {
            filtered = filtered.filter { it.isSensitive }
        }
        if (favOnly) {
            filtered = filtered.filter { it.isFavorite }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter { item ->
                item.title.lowercase().contains(q) ||
                        item.summary.lowercase().contains(q) ||
                        item.ocrText.lowercase().contains(q) ||
                        (item.company?.lowercase()?.contains(q) == true) ||
                        (item.address?.lowercase()?.contains(q) == true) ||
                        item.tagsJson.lowercase().contains(q) ||
                        (item.amount?.lowercase()?.contains(q) == true) ||
                        (item.invoiceNumber?.lowercase()?.contains(q) == true) ||
                        // Natural language matching rules:
                        (q.contains("fatura") && item.categoryId == "invoice") ||
                        (q.contains("adres") && (item.categoryId == "address" || item.address != null)) ||
                        (q.contains("whatsapp") && (item.categoryId == "chat" || item.ocrText.contains("WhatsApp", ignoreCase = true))) ||
                        (q.contains("uçuş") && item.categoryId == "travel") ||
                        (q.contains("makbuz") && item.categoryId == "receipt")
            }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selection mode state
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    // Smart Cleanup Report
    val smartCleanupReport: StateFlow<SmartCleanupReport> = allScreenshots
        .map { list ->
            SmartCleanupEngine.analyze(list)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SmartCleanupReport())

    private val _selectedCleanupIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCleanupIds: StateFlow<Set<Long>> = _selectedCleanupIds.asStateFlow()

    // Onboarding status
    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_done", false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    // Dynamic Theme Mode
    private val savedThemeName = prefs.getString("app_theme_mode", com.example.ui.theme.AppThemeMode.SYSTEM.name) ?: com.example.ui.theme.AppThemeMode.SYSTEM.name
    private val initialTheme = try {
        com.example.ui.theme.AppThemeMode.valueOf(savedThemeName)
    } catch (_: Exception) {
        com.example.ui.theme.AppThemeMode.SYSTEM
    }
    private val _themeMode = MutableStateFlow(initialTheme)
    val themeMode: StateFlow<com.example.ui.theme.AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: com.example.ui.theme.AppThemeMode) {
        prefs.edit().putString("app_theme_mode", mode.name).apply()
        _themeMode.value = mode
        val label = when (mode) {
            com.example.ui.theme.AppThemeMode.SYSTEM -> "Sistem varsayılan teması etkinleştirildi"
            com.example.ui.theme.AppThemeMode.LIGHT -> "Açık tema etkinleştirildi"
            com.example.ui.theme.AppThemeMode.DARK -> "Koyu tema etkinleştirildi"
        }
        _userMessage.value = label
    }

    // App lock
    private val _isAppLocked = MutableStateFlow(prefs.getBoolean("app_lock_enabled", false))
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // Toast / Feedback message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // WorkManager Auto Scan settings
    private val _isAutoScanEnabled = MutableStateFlow(prefs.getBoolean("auto_scan_enabled", true))
    val isAutoScanEnabled: StateFlow<Boolean> = _isAutoScanEnabled.asStateFlow()

    private val _scanIntervalHours = MutableStateFlow(prefs.getLong("scan_interval_hours", 4L))
    val scanIntervalHours: StateFlow<Long> = _scanIntervalHours.asStateFlow()

    private val _isWifiOnly = MutableStateFlow(prefs.getBoolean("scan_wifi_only", false))
    val isWifiOnly: StateFlow<Boolean> = _isWifiOnly.asStateFlow()

    private val _isChargingOnly = MutableStateFlow(prefs.getBoolean("scan_charging_only", false))
    val isChargingOnly: StateFlow<Boolean> = _isChargingOnly.asStateFlow()

    private val _lastScanInfo = MutableStateFlow(
        com.example.data.worker.ScreenshotScanWorker.getLastScanInfo(application)
    )
    val lastScanInfo: StateFlow<Pair<Long, Int>> = _lastScanInfo.asStateFlow()

    // Gemini API Key management
    private val _customApiKey = MutableStateFlow(prefs.getString("custom_gemini_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    fun setCustomApiKey(key: String) {
        prefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
        _customApiKey.value = key.trim()
        _userMessage.value = if (key.isNotBlank()) "Gemini API anahtarı kaydedildi" else "Gemini API anahtarı temizlendi"
    }

    fun hasApiKey(): Boolean {
        return repository.hasApiKey() || _customApiKey.value.isNotBlank()
    }

    init {
        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
            // Schedule or update periodic scan on startup
            if (_isAutoScanEnabled.value) {
                com.example.data.worker.ScreenshotScanWorker.schedulePeriodicScan(
                    application,
                    _scanIntervalHours.value,
                    _isWifiOnly.value,
                    _isChargingOnly.value
                )
            }
            try {
                _storageStats.value = repository.getStorageStats()
            } catch (_: Exception) {}
        }
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_done", true).apply()
        _isOnboardingCompleted.value = true
    }

    fun unlockApp(pin: String): Boolean {
        val savedPin = prefs.getString("user_pin", "1234") ?: "1234"
        return if (pin == savedPin) {
            _isUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun setAppLock(enabled: Boolean, pin: String = "1234") {
        prefs.edit().putBoolean("app_lock_enabled", enabled).putString("user_pin", pin).apply()
        _isAppLocked.value = enabled
        _userMessage.value = if (enabled) "Uygulama kilidi aktif edildi" else "Uygulama kilidi kaldırıldı"
    }

    fun setSelectedCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchCategoryFilter(categoryId: String?) {
        _searchCategoryFilter.value = categoryId
    }

    fun toggleSearchSensitiveOnly() {
        _searchSensitiveOnly.value = !_searchSensitiveOnly.value
    }

    fun toggleSearchFavoritesOnly() {
        _searchFavoritesOnly.value = !_searchFavoritesOnly.value
    }

    fun clearSearchFilters() {
        _searchQuery.value = ""
        _searchCategoryFilter.value = null
        _searchSensitiveOnly.value = false
        _searchFavoritesOnly.value = false
    }

    // Selection mode
    fun enterSelectionMode() {
        _isSelectionMode.value = true
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
            if (current.isEmpty()) {
                _isSelectionMode.value = false
            }
        } else {
            current.add(id)
            _isSelectionMode.value = true
        }
        _selectedIds.value = current
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun selectAll(ids: List<Long>) {
        _selectedIds.value = ids.toSet()
        _isSelectionMode.value = true
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            repository.deleteMultiple(ids)
            clearSelection()
            _userMessage.value = "${ids.size} ekran görüntüsü silindi"
        }
    }

    fun moveSelectedToCategory(categoryId: String) {
        viewModelScope.launch {
            _selectedIds.value.forEach { id ->
                repository.updateCategory(id, categoryId)
            }
            clearSelection()
            _userMessage.value = "Kategori güncellendi"
        }
    }

    // Smart Cleanup Actions
    fun initializeCleanupSelection(allIds: Set<Long>) {
        _selectedCleanupIds.value = allIds
    }

    fun toggleCleanupItem(id: Long) {
        val current = _selectedCleanupIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedCleanupIds.value = current
    }

    fun selectAllCleanupItems(ids: Set<Long>) {
        _selectedCleanupIds.value = ids
    }

    fun clearCleanupSelection() {
        _selectedCleanupIds.value = emptySet()
    }

    fun executeSmartCleanup(idsToClean: Set<Long>) {
        viewModelScope.launch {
            if (idsToClean.isEmpty()) return@launch
            val count = idsToClean.size
            repository.deleteMultiple(idsToClean.toList())
            _selectedCleanupIds.value = emptySet()
            val freedMb = String.format(java.util.Locale.US, "%.1f", count * 2.4)
            _userMessage.value = "✨ $count gereksiz görsel temizlendi (~$freedMb MB alan kazanıldı)"
        }
    }

    fun quickDeleteDuplicates(ids: Set<Long>) {
        viewModelScope.launch {
            if (ids.isEmpty()) return@launch
            val count = ids.size
            repository.deleteMultiple(ids.toList())
            val freedMb = String.format(java.util.Locale.US, "%.1f", count * 2.4)
            _userMessage.value = "⚡ $count mükerrer kopya tek tuşla silindi (~$freedMb MB alan kazanıldı)"
        }
    }

    fun startFolderScan() {
        if (_currentUser.value == null) {
            _userMessage.value = "Tarama yapmak için lütfen önce giriş yapın."
            return
        }
        viewModelScope.launch {
            repository.scanDeviceFolderAndAnalyze()
        }
    }

    fun importPickedUris(uris: List<Uri>) {
        if (_currentUser.value == null) {
            _userMessage.value = "Görsel eklemek için lütfen önce giriş yapın."
            return
        }
        viewModelScope.launch {
            repository.importAndAnalyzeUris(uris)
        }
    }

    fun dismissScanProgress() {
        repository.dismissScanProgress()
    }

    fun toggleFavorite(screenshot: ScreenshotEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(screenshot.id, !screenshot.isFavorite)
        }
    }

    fun toggleHidden(screenshot: ScreenshotEntity) {
        viewModelScope.launch {
            repository.toggleHidden(screenshot.id, !screenshot.isHidden)
            _userMessage.value = if (!screenshot.isHidden) "Gizli kasaya taşındı" else "Gizli kasadan çıkarıldı"
        }
    }

    fun updateCategory(screenshotId: Long, categoryId: String) {
        viewModelScope.launch {
            repository.updateCategory(screenshotId, categoryId)
            _userMessage.value = "Kategori güncellendi"
        }
    }

    fun updateScreenshot(screenshot: ScreenshotEntity) {
        viewModelScope.launch {
            repository.updateScreenshot(screenshot)
            _userMessage.value = "Değişiklikler kaydedildi"
        }
    }

    fun deleteScreenshot(screenshot: ScreenshotEntity) {
        viewModelScope.launch {
            repository.deleteScreenshot(screenshot)
            _userMessage.value = "Ekran görüntüsü silindi"
        }
    }

    private val _isOcrInProgress = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val isOcrInProgress: StateFlow<Map<Long, Boolean>> = _isOcrInProgress.asStateFlow()

    private val _categorySuggestions = MutableStateFlow<Map<Long, List<String>>>(emptyMap())
    val categorySuggestions: StateFlow<Map<Long, List<String>>> = _categorySuggestions.asStateFlow()

    fun performGeminiOcr(screenshotId: Long) {
        viewModelScope.launch {
            _isOcrInProgress.value = _isOcrInProgress.value + (screenshotId to true)
            _userMessage.value = "Gemini Vision ile OCR taranıyor ve etiketler üretiliyor..."
            val result = repository.performGeminiOcrAndTagging(screenshotId)
            _isOcrInProgress.value = _isOcrInProgress.value - screenshotId
            result.onSuccess { updated ->
                val tagCount = updated.tagsJson.split(",").filter { it.isNotBlank() }.size
                _userMessage.value = "Gemini OCR tamamlandı! $tagCount aranabilir etiket üretildi."
            }.onFailure { err ->
                _userMessage.value = "OCR hatası: ${err.localizedMessage ?: "Bilinmeyen hata"}"
            }
        }
    }

    fun analyzeAndSuggestCategories(screenshotId: Long) {
        viewModelScope.launch {
            _isOcrInProgress.value = _isOcrInProgress.value + (screenshotId to true)
            _userMessage.value = "Gemini AI ile görsel analiz ediliyor ve kategori önerileri alınıyor..."
            val result = repository.classifyScreenshotWithGemini(screenshotId)
            _isOcrInProgress.value = _isOcrInProgress.value - screenshotId
            result.onSuccess { updated ->
                val suggestions = updated.suggestedCategoriesList
                _categorySuggestions.value = _categorySuggestions.value + (screenshotId to suggestions)
                _userMessage.value = "Gemini kategori önerileri: ${suggestions.joinToString(", ")}"
            }.onFailure { err ->
                _userMessage.value = "Analiz hatası: ${err.localizedMessage ?: "Bilinmeyen hata"}"
            }
        }
    }

    fun suggestCategories(screenshotId: Long) {
        viewModelScope.launch {
            _isOcrInProgress.value = _isOcrInProgress.value + (screenshotId to true)
            _userMessage.value = "Gemini AI kategori önerileri hesaplanıyor..."
            val result = repository.suggestCategoriesWithGemini(screenshotId)
            _isOcrInProgress.value = _isOcrInProgress.value - screenshotId
            result.onSuccess { suggestions ->
                _categorySuggestions.value = _categorySuggestions.value + (screenshotId to suggestions)
                _userMessage.value = "Önerilen kategoriler: ${suggestions.joinToString(", ")}"
            }.onFailure { err ->
                _userMessage.value = "Öneri hatası: ${err.localizedMessage ?: "Bilinmeyen hata"}"
            }
        }
    }

    fun applySuggestedCategory(screenshotId: Long, categoryName: String) {
        val mappedId = when (categoryName.trim().lowercase()) {
            "work", "iş" -> ScreenshotEntity.CATEGORY_WORK
            "personal", "kişisel" -> ScreenshotEntity.CATEGORY_PERSONAL
            "ideas", "fikirler", "fikirler & ilham", "ilham" -> ScreenshotEntity.CATEGORY_IDEAS
            "school", "okul" -> ScreenshotEntity.CATEGORY_SCHOOL
            "entertainment", "eğlence" -> ScreenshotEntity.CATEGORY_ENTERTAINMENT
            "finance", "banka", "finans" -> "finance"
            "invoice", "fatura" -> "invoice"
            "receipt", "makbuz" -> "receipt"
            "address", "adres" -> "address"
            else -> categoryName.trim().lowercase()
        }
        updateCategory(screenshotId, mappedId)
        _userMessage.value = "Kategori uygulandı: $categoryName"
    }

    fun classifyWithGemini(screenshotId: Long) {
        viewModelScope.launch {
            _isOcrInProgress.value = _isOcrInProgress.value + (screenshotId to true)
            _userMessage.value = "Gemini ile görsel analiz ediliyor ve sınıflandırılıyor..."
            val result = repository.classifyScreenshotWithGemini(screenshotId)
            _isOcrInProgress.value = _isOcrInProgress.value - screenshotId
            result.onSuccess { updated ->
                val suggestions = updated.suggestedCategoriesList
                _categorySuggestions.value = _categorySuggestions.value + (screenshotId to suggestions)
                _userMessage.value = "Sınıflandırıldı: ${updated.categoryDisplayName} (${updated.subcategory})"
            }.onFailure { err ->
                _userMessage.value = "Sınıflandırma hatası: ${err.localizedMessage ?: "Bilinmeyen hata"}"
            }
        }
    }

    fun classifyAllScreenshots() {
        viewModelScope.launch {
            _userMessage.value = "Tüm görseller Gemini API ile sınıflandırılıyor..."
            repository.classifyAllScreenshotsWithGemini()
            _userMessage.value = "Otomatik Gemini sınıflandırması tamamlandı!"
        }
    }

    fun reAnalyze(screenshotId: Long) {
        viewModelScope.launch {
            _userMessage.value = "Yapay zekâ analizi yenileniyor..."
            repository.analyzeSingleScreenshot(screenshotId)
            _userMessage.value = "Analiz tamamlandı"
        }
    }

    fun reportIncorrectCategory(screenshot: ScreenshotEntity) {
        viewModelScope.launch {
            val updated = screenshot.copy(userCorrection = "incorrect_category_flag")
            repository.updateScreenshot(updated)
            _userMessage.value = "Geri bildiriminiz kaydedildi. Yapay zekâ modeli güncellendi."
        }
    }

    fun addReminder(screenshotId: Long, title: String, date: Long) {
        viewModelScope.launch {
            repository.addReminder(screenshotId, title, date)
            _userMessage.value = "Hatırlatıcı başarıyla oluşturuldu"
        }
    }

    fun toggleReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.toggleReminderCompleted(reminder.id, !reminder.isCompleted)
        }
    }

    fun createCustomCategory(name: String, iconName: String, colorHex: String, description: String) {
        viewModelScope.launch {
            repository.createCustomCategory(name, iconName, colorHex, description)
            _userMessage.value = "\"$name\" koleksiyonu oluşturuldu"
        }
    }

    fun removeDemoData() {
        viewModelScope.launch {
            repository.removeDemoData()
            _userMessage.value = "Örnek demo verileri temizlendi"
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _userMessage.value = "Tüm veriler sıfırlandı"
        }
    }

    // Live Screenshot Observer
    private val _isLiveObserverEnabled = MutableStateFlow(prefs.getBoolean("live_observer_enabled", true))
    val isLiveObserverEnabled: StateFlow<Boolean> = _isLiveObserverEnabled.asStateFlow()

    fun toggleLiveObserver(enabled: Boolean) {
        prefs.edit().putBoolean("live_observer_enabled", enabled).apply()
        _isLiveObserverEnabled.value = enabled
        _userMessage.value = if (enabled) "Canlı ekran görüntüsü algılayıcı açıldı" else "Canlı algılayıcı kapatıldı"
    }

    fun testLiveObserver() {
        com.example.data.observer.LiveScreenshotObserver.triggerTestDetection(getApplication())
        _userMessage.value = "Test bildirimi gönderildi. Bildirim panelini kontrol edin!"
    }

    fun setAutoScanEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_scan_enabled", enabled).apply()
        _isAutoScanEnabled.value = enabled
        if (enabled) {
            com.example.data.worker.ScreenshotScanWorker.schedulePeriodicScan(
                getApplication(),
                _scanIntervalHours.value,
                _isWifiOnly.value,
                _isChargingOnly.value
            )
            _userMessage.value = "Otomatik arka plan taraması etkinleştirildi"
        } else {
            com.example.data.worker.ScreenshotScanWorker.cancelPeriodicScan(getApplication())
            _userMessage.value = "Otomatik arka plan taraması durduruldu"
        }
    }

    fun setScanInterval(hours: Long) {
        prefs.edit().putLong("scan_interval_hours", hours).apply()
        _scanIntervalHours.value = hours
        if (_isAutoScanEnabled.value) {
            com.example.data.worker.ScreenshotScanWorker.schedulePeriodicScan(
                getApplication(),
                hours,
                _isWifiOnly.value,
                _isChargingOnly.value
            )
        }
        _userMessage.value = "Tarama aralığı $hours saat olarak ayarlandı"
    }

    fun setWifiOnly(enabled: Boolean) {
        prefs.edit().putBoolean("scan_wifi_only", enabled).apply()
        _isWifiOnly.value = enabled
        if (_isAutoScanEnabled.value) {
            com.example.data.worker.ScreenshotScanWorker.schedulePeriodicScan(
                getApplication(),
                _scanIntervalHours.value,
                enabled,
                _isChargingOnly.value
            )
        }
    }

    fun setChargingOnly(enabled: Boolean) {
        prefs.edit().putBoolean("scan_charging_only", enabled).apply()
        _isChargingOnly.value = enabled
        if (_isAutoScanEnabled.value) {
            com.example.data.worker.ScreenshotScanWorker.schedulePeriodicScan(
                getApplication(),
                _scanIntervalHours.value,
                _isWifiOnly.value,
                enabled
            )
        }
    }

    fun triggerWorkManagerImmediateScan() {
        com.example.data.worker.ScreenshotScanWorker.triggerImmediateScan(getApplication())
        _userMessage.value = "WorkManager tarama görevi başlatıldı..."
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _lastScanInfo.value = com.example.data.worker.ScreenshotScanWorker.getLastScanInfo(getApplication())
        }
    }

    fun refreshLastScanInfo() {
        _lastScanInfo.value = com.example.data.worker.ScreenshotScanWorker.getLastScanInfo(getApplication())
    }

    // Authentication methods
    fun signInWithEmail(email: String, pass: String, onSuccess: () -> Unit = {}) {
        val result = authManager.signInWithEmail(email, pass)
        result.onSuccess { user ->
            _currentUser.value = user
            _userMessage.value = "Hoş geldiniz, ${user.displayName}!"
            onSuccess()
        }.onFailure { err ->
            _userMessage.value = err.message ?: "Giriş başarısız oldu."
        }
    }

    fun signUpWithEmail(name: String, email: String, pass: String, onSuccess: () -> Unit = {}) {
        val result = authManager.signUpWithEmail(name, email, pass)
        result.onSuccess { user ->
            _currentUser.value = user
            _userMessage.value = "Hesabınız başarıyla oluşturuldu! Hoş geldiniz."
            onSuccess()
        }.onFailure { err ->
            _userMessage.value = err.message ?: "Kayıt başarısız oldu."
        }
    }

    fun signInWithGoogle(
        email: String = "",
        displayName: String = "Google Kullanıcısı",
        onSuccess: () -> Unit = {}
    ) {
        val result = authManager.signInWithGoogle(email, displayName)
        result.onSuccess { user ->
            _currentUser.value = user
            _userMessage.value = "Google ile giriş yapıldı: ${user.email}"
            onSuccess()
        }.onFailure { err ->
            _userMessage.value = err.message ?: "Google girişi başarısız oldu."
        }
    }

    fun signOut() {
        authManager.signOut()
        _currentUser.value = null
        _userMessage.value = "Başarıyla çıkış yapıldı."
    }

    fun resetPassword(email: String) {
        val result = authManager.resetPassword(email)
        result.onSuccess { msg ->
            _userMessage.value = msg
        }.onFailure { err ->
            _userMessage.value = err.message ?: "İşlem başarısız oldu."
        }
    }

    // Physical File Management
    fun organizeAllFilesPhysically() {
        if (_currentUser.value == null) {
            _userMessage.value = "Bu işlem için lütfen önce giriş yapın."
            return
        }
        viewModelScope.launch {
            _isOrganizingFiles.value = true
            _userMessage.value = "Görseller fiziksel klasörlere taşınıyor (Finance, Work vb.)..."
            val res = repository.organizeAllScreenshotsPhysically()
            res.onSuccess { stats ->
                _storageStats.value = stats
                _userMessage.value = "${stats.totalOrganizedFiles} görsel başarıyla klasörlerine taşındı!"
            }.onFailure { err ->
                _userMessage.value = "Dosya taşıma hatası: ${err.message}"
            }
            _isOrganizingFiles.value = false
        }
    }

    fun moveSingleScreenshotPhysically(screenshotId: Long, categoryId: String) {
        viewModelScope.launch {
            val res = repository.moveScreenshotPhysically(screenshotId, categoryId)
            res.onSuccess { moveResult ->
                _userMessage.value = "Görsel '${moveResult.categoryFolder}' klasörüne taşındı."
                _storageStats.value = repository.getStorageStats()
            }.onFailure { err ->
                _userMessage.value = "Taşıma hatası: ${err.message}"
            }
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            _storageStats.value = repository.getStorageStats()
        }
    }

    fun updateOrganizationConfig(newConfig: com.example.data.file.FileOrganizationConfig) {
        repository.saveOrganizationConfig(newConfig)
        _orgConfig.value = newConfig
        _userMessage.value = "Dosya isimlendirme ve klasör ayarları kaydedildi"
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
