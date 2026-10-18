package com.example.ui.screens.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Work
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.data.pdf.PdfExportService
import com.example.data.observer.LiveScreenshotObserver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.cleanup.SmartCleanupReport
import com.example.data.model.CategoryEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.ScreenshotEntity
import com.example.ui.components.CategoryIcons
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ScreenshotCard
import com.example.ui.components.StatCard
import com.example.ui.components.StoragePermissionDialog
import com.example.util.StoragePermissionHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    allScreenshots: List<ScreenshotEntity>,
    allCategories: List<CategoryEntity>,
    allReminders: List<ReminderEntity>,
    onScreenshotClick: (Long) -> Unit,
    onCategoryClick: (String) -> Unit,
    onStartScan: () -> Unit,
    onImportUris: (List<android.net.Uri>) -> Unit,
    onFavoriteToggle: (ScreenshotEntity) -> Unit,
    onViewAllCollections: () -> Unit,
    onDeleteScreenshot: (ScreenshotEntity) -> Unit = {},
    currentUser: com.example.data.auth.UserProfile? = null,
    onNavigateToAuth: () -> Unit = {},
    isSelectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    onToggleSelect: (Long) -> Unit = {},
    onSelectAll: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onMoveSelectedToCategory: (String) -> Unit = {},
    onShareSelected: () -> Unit = {},
    smartCleanupReport: SmartCleanupReport = SmartCleanupReport(),
    onNavigateToSmartCleanup: () -> Unit = {},
    onQuickDeleteDuplicates: (Set<Long>) -> Unit = {},
    onEnterSelectionMode: () -> Unit = {},
    onClassifyAllWithGemini: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = isSelectionMode) {
        onClearSelection()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showMoveCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showQuickDeleteDuplicatesDialog by remember { mutableStateOf(false) }
    var isBulkPdfGenerating by remember { mutableStateOf(false) }

    val performExportPdfSelected: () -> Unit = {
        val selectedScreenshots = allScreenshots.filter { it.id in selectedIds }
        if (selectedScreenshots.isNotEmpty() && !isBulkPdfGenerating) {
            isBulkPdfGenerating = true
            coroutineScope.launch {
                val catMap = allCategories.associate { it.id to it.name }
                val result = PdfExportService.exportMultipleScreenshotsToPdf(
                    context = context,
                    screenshots = selectedScreenshots,
                    categoryMap = catMap
                )
                isBulkPdfGenerating = false
                result.onSuccess { uri ->
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Toplu PDF Raporunu Paylaş"))
                }.onFailure { err ->
                    Toast.makeText(context, "PDF oluşturulamadı: ${err.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val performShareSelected: () -> Unit = {
        val selectedScreenshots = allScreenshots.filter { it.id in selectedIds }
        val uris = selectedScreenshots.mapNotNull {
            try { Uri.parse(it.uri) } catch (_: Exception) { null }
        }
        if (uris.size == 1) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Görseli Paylaş"))
        } else if (uris.isNotEmpty()) {
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "${uris.size} Görseli Paylaş"))
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportUris(uris)
        }
    }

    // Media permission handling
    var showPermissionDialog by remember { mutableStateOf(false) }
    var isPermanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val isGranted = permissionsMap.values.any { it }
        if (isGranted) {
            showPermissionDialog = false
            Toast.makeText(context, "İzin verildi, ekran görüntüleri taranıyor...", Toast.LENGTH_SHORT).show()
            onStartScan()
        } else {
            isPermanentlyDenied = true
            showPermissionDialog = true
            Toast.makeText(
                context,
                "Ekran görüntülerini taramak için medya okuma izni gereklidir.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val requestScanWithPermissionCheck: () -> Unit = {
        if (StoragePermissionHelper.hasStoragePermission(context)) {
            onStartScan()
        } else {
            isPermanentlyDenied = false
            showPermissionDialog = true
        }
    }

    // Top stats derived values
    val totalCount = allScreenshots.size
    val analyzedCount = allScreenshots.count { it.isAnalyzed }
    val pendingCount = allScreenshots.count { !it.isAnalyzed }
    val weekAgoMs = System.currentTimeMillis() - (7 * 86_400_000L)
    val thisWeekCount = allScreenshots.count { it.createdAt >= weekAgoMs }

    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val latestDetection by LiveScreenshotObserver.latestDetectedEvent.collectAsStateWithLifecycle()

    val filteredScreenshots = remember(allScreenshots, selectedCategoryFilter, searchQuery) {
        val baseList = if (selectedCategoryFilter == null) {
            allScreenshots
        } else {
            allScreenshots.filter { it.categoryId == selectedCategoryFilter }
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase()
            baseList.filter { s ->
                s.title.lowercase().contains(q) ||
                s.summary.lowercase().contains(q) ||
                s.fileName.lowercase().contains(q) ||
                s.ocrText.lowercase().contains(q) ||
                s.tagsJson.lowercase().contains(q) ||
                (s.company?.lowercase()?.contains(q) == true) ||
                (s.invoiceNumber?.lowercase()?.contains(q) == true) ||
                (s.amount?.lowercase()?.contains(q) == true) ||
                (s.address?.lowercase()?.contains(q) == true) ||
                (s.phoneNumber?.lowercase()?.contains(q) == true) ||
                (s.subcategory?.lowercase()?.contains(q) == true)
            }
        }
    }

    val recentScreenshots = remember(allScreenshots) {
        allScreenshots.take(6)
    }

    val aiSuggestions = remember(allScreenshots) {
        allScreenshots.filter { !it.actionSuggestion.isNullOrBlank() }.take(3)
    }

    val upcomingReminders = remember(allReminders) {
        allReminders.filter { !it.isCompleted }.take(3)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 164.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_grid"),
            contentPadding = PaddingValues(bottom = if (isSelectionMode) 140.dp else 90.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header greeting & Brand or Contextual Selection Bar
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (isSelectionMode) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("contextual_selection_bar")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = onClearSelection,
                                        modifier = Modifier.testTag("clear_selection_mode_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Seçimi Kapat",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${selectedIds.size} Görsel Seçildi",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        if (selectedIds.size == filteredScreenshots.size && filteredScreenshots.isNotEmpty()) {
                                            onClearSelection()
                                        } else {
                                            onSelectAll()
                                        }
                                    },
                                    modifier = Modifier.testTag("select_all_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SelectAll,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (selectedIds.size == filteredScreenshots.size && filteredScreenshots.isNotEmpty()) "Kaldır" else "Tümünü Seç",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentUser != null) "Merhaba, ${currentUser.displayName} 👋" else "Günaydın, Kullanıcı 👋",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (currentUser != null) "${currentUser.email} ile senkronize" else "Ekran görüntülerin artık düzenli.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .clickable { onNavigateToAuth() }
                                    .testTag("home_user_profile_avatar")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (currentUser != null) {
                                        Text(
                                            text = currentUser.displayName.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Giriş Yap",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        // 1.5 Search Bar at Top of Gallery
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Metin içeriği, etiket veya dosya ara...",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Ara",
                            tint = if (searchQuery.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("clear_search_query_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Aramayı Temizle",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gallery_search_bar")
                )

                // Quick Semantic Tag Suggestions
                val sampleTags = listOf("Finance", "Work", "Inspiration", "Fatura", "Dekont", "Starbucks", "Garanti", "Sipariş")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("search_quick_tags_row")
                ) {
                    items(sampleTags) { tag ->
                        val isTagActive = searchQuery.equals(tag, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isTagActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isTagActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .clickable {
                                    searchQuery = if (isTagActive) "" else tag
                                }
                                .testTag("search_tag_chip_$tag")
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isTagActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isTagActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // 1.8 Live Detected Screenshot Banner (if any detected)
        if (latestDetection != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("home_live_detected_screenshot_banner"),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Yeni Ekran Görüntüsü Tespit Edildi!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = "${latestDetection?.fileName} anında organize edilebilir.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF047857),
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = {
                                val ev = latestDetection
                                if (ev != null) {
                                    onImportUris(listOf(ev.uri))
                                    LiveScreenshotObserver.clearLatestEvent()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("Tara", fontSize = 12.sp)
                        }
                        IconButton(
                            onClick = { LiveScreenshotObserver.clearLatestEvent() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color(0xFF065F46), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // 2. Privacy Banner (Crucial per instructions)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Gizlilik Güvencesi",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Ekran görüntüleriniz size aittir. Yalnızca onayınızla taranır, hassas verileriniz cihazınızda güvendedir.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 3. Stats Row (Responsive: 4 across on tablets/wide screens, 2x2 on mobile)
        item(span = { GridItemSpan(maxLineSpan) }) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (maxWidth >= 600.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Toplam",
                            value = "$totalCount",
                            subtitle = "Görsel Arşivde",
                            icon = Icons.Default.PhotoLibrary,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Analiz Edilen",
                            value = "$analyzedCount",
                            subtitle = "%${if (totalCount > 0) (analyzedCount * 100 / totalCount) else 100} Düzenlendi",
                            icon = Icons.Default.CheckCircle,
                            accentColor = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Bekleyen",
                            value = "$pendingCount",
                            subtitle = "Yeni Görsel",
                            icon = Icons.Default.HourglassEmpty,
                            accentColor = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Bu Hafta",
                            value = "$thisWeekCount",
                            subtitle = "Kataloglandı",
                            icon = Icons.Default.DateRange,
                            accentColor = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                title = "Toplam",
                                value = "$totalCount",
                                subtitle = "Görsel Arşivde",
                                icon = Icons.Default.PhotoLibrary,
                                accentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Analiz Edilen",
                                value = "$analyzedCount",
                                subtitle = "%${if (totalCount > 0) (analyzedCount * 100 / totalCount) else 100} Düzenlendi",
                                icon = Icons.Default.CheckCircle,
                                accentColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                title = "Bekleyen",
                                value = "$pendingCount",
                                subtitle = "Yeni Görsel",
                                icon = Icons.Default.HourglassEmpty,
                                accentColor = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Bu Hafta",
                                value = "$thisWeekCount",
                                subtitle = "Kataloglandı",
                                icon = Icons.Default.DateRange,
                                accentColor = Color(0xFF8B5CF6),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 4. Hero Scan Card
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("home_hero_scan_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    Color(0xFF312E81)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Akıllı Yapay Zekâ Analizi",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Yeni Ekran Görüntülerini Düzenle",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Gemini Vision ile cihazındaki veya galerindeki görüntüleri tara; iş, okul, eğlence ve kişisel içerikleri otomatik klasörlere ayır.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { requestScanWithPermissionCheck() },
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(46.dp)
                                    .testTag("home_start_scan_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF1E1B4B)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tara",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Button(
                                onClick = { onClassifyAllWithGemini() },
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(46.dp)
                                    .testTag("home_gemini_classify_all_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AI Sınıflandır",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(46.dp)
                                    .testTag("home_pick_photos_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color.White.copy(alpha = 0.5f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Seç",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4.5 Smart Cleanup Highlight Card
        item(span = { GridItemSpan(maxLineSpan) }) {
            val cleanableCount = smartCleanupReport.totalCleanableCount
            val dupCount = smartCleanupReport.totalDuplicateCount
            val oldCount = smartCleanupReport.totalOldCount
            val savingsMb = String.format(Locale.US, "%.1f", smartCleanupReport.estimatedSavingsMb)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onNavigateToSmartCleanup() }
                    .testTag("home_smart_cleanup_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (cleanableCount > 0)
                        MaterialTheme.colorScheme.surface
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.2.dp,
                    color = if (cleanableCount > 0) Color(0xFF10B981).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (cleanableCount > 0) 3.dp else 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (cleanableCount > 0) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = "Akıllı Temizlik",
                                tint = if (cleanableCount > 0) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Akıllı Temizlik",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (cleanableCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "$cleanableCount Gereksiz",
                                        color = Color(0xFFEF4444),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = if (cleanableCount > 0) {
                                "$dupCount kopya & $oldCount süresi geçmiş görsel (~$savingsMb MB tasarruf)"
                            } else {
                                "Çift veya süresi dolmuş ekran görüntülerini otomatik tespit et ve temizle."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dupCount > 0) {
                            Button(
                                onClick = { showQuickDeleteDuplicatesDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("home_quick_delete_duplicates_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Çiftleri Sil ($dupCount)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = onNavigateToSmartCleanup,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (cleanableCount > 0) Color(0xFF10B981) else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (cleanableCount > 0) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("home_open_smart_cleanup_button")
                        ) {
                            Text(
                                text = if (dupCount > 0) "İncele" else if (cleanableCount > 0) "Temizle" else "İncele",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5. Categories Section Header & Horizontal Carousel
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kategoriler",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = onViewAllCollections,
                        modifier = Modifier.testTag("home_view_all_categories_button")
                    ) {
                        Text("Tümünü Gör (${allCategories.size})")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(allCategories) { category ->
                        val count = allScreenshots.count { it.categoryId == category.id }
                        val color = CategoryIcons.parseColor(category.colorHex)
                        val icon = CategoryIcons.getIcon(category.iconName)

                        Card(
                            modifier = Modifier
                                .width(135.dp)
                                .clickable { onCategoryClick(category.id) }
                                .testTag("home_category_chip_${category.id}"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = color.copy(alpha = 0.15f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = category.name,
                                            tint = color,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "$count dosya",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Upcoming Reminders Section (if any)
        if (upcomingReminders.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = "Yaklaşan Önemli Tarihler",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    upcomingReminders.forEach { reminder ->
                        val dateFormatted = SimpleDateFormat("dd MMMM yyyy", Locale("tr")).format(Date(reminder.reminderDate))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = reminder.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = dateFormatted,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. AI Action Suggestions Section
        if (aiSuggestions.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = "AI Önerileri",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    aiSuggestions.forEach { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onScreenshotClick(item.id) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = item.actionSuggestion ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 8. Category Filter Bar Above Screenshot Grid
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val headerTitle = when {
                                searchQuery.isNotBlank() -> "Arama: \"$searchQuery\""
                                selectedCategoryFilter != null -> {
                                    val cat = allCategories.find { it.id == selectedCategoryFilter }
                                    cat?.name ?: "Filtrelenen Görseller"
                                }
                                else -> "Ekran Görüntüleri"
                            }
                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (selectedCategoryFilter != null || searchQuery.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${filteredScreenshots.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = when {
                                searchQuery.isNotBlank() -> "OCR metni, etiketler ve meta veriler taranıyor"
                                selectedCategoryFilter != null -> "Kategoriye göre filtrelendi"
                                else -> "Tüm düzenlenen ekran görüntüleri"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selectedCategoryFilter != null || searchQuery.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    selectedCategoryFilter = null
                                    searchQuery = ""
                                },
                                modifier = Modifier.testTag("clear_category_filter_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Temizle")
                            }
                        }

                        // Multi-Select toggle button
                        FilledTonalButton(
                            onClick = {
                                if (isSelectionMode) {
                                    onClearSelection()
                                } else {
                                    onEnterSelectionMode()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("gallery_multi_select_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isSelectionMode) Icons.Default.Close else Icons.Default.Checklist,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSelectionMode) "Kapat" else "Çoklu Seç",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Category Filter Bar
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_category_filter_bar")
                ) {
                    // "Tümü" (All) Chip
                    item {
                        val isAllSelected = selectedCategoryFilter == null
                        FilterChip(
                            selected = isAllSelected,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("Tümü (${allScreenshots.size})") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("filter_chip_all")
                        )
                    }

                    // Priority Categories: Work, Personal, Ideas, School, Entertainment
                    val priorityCategories = listOf(
                        Triple(ScreenshotEntity.CATEGORY_WORK, "İş", Icons.Default.Work),
                        Triple(ScreenshotEntity.CATEGORY_PERSONAL, "Kişisel", Icons.Default.Person),
                        Triple(ScreenshotEntity.CATEGORY_IDEAS, "Fikirler", Icons.Default.Lightbulb),
                        Triple(ScreenshotEntity.CATEGORY_SCHOOL, "Okul", Icons.Default.School),
                        Triple(ScreenshotEntity.CATEGORY_ENTERTAINMENT, "Eğlence", Icons.Default.SportsEsports)
                    )

                    items(priorityCategories) { (catId, label, icon) ->
                        val isSelected = selectedCategoryFilter == catId
                        val count = allScreenshots.count { it.categoryId == catId }
                        val categoryObj = allCategories.find { it.id == catId }
                        val chipColor = if (categoryObj != null) CategoryIcons.parseColor(categoryObj.colorHex) else when (catId) {
                            ScreenshotEntity.CATEGORY_WORK -> Color(0xFF2563EB)
                            ScreenshotEntity.CATEGORY_PERSONAL -> Color(0xFF8B5CF6)
                            ScreenshotEntity.CATEGORY_IDEAS -> Color(0xFFEAB308)
                            ScreenshotEntity.CATEGORY_SCHOOL -> Color(0xFF059669)
                            ScreenshotEntity.CATEGORY_ENTERTAINMENT -> Color(0xFFF59E0B)
                            else -> MaterialTheme.colorScheme.primary
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategoryFilter = if (isSelected) null else catId
                            },
                            label = {
                                Text("$label ($count)")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else chipColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("filter_chip_$catId")
                        )
                    }

                    // Other available categories
                    val otherCategories = allCategories.filter { it.id !in ScreenshotEntity.CORE_CATEGORIES }
                    items(otherCategories) { cat ->
                        val isSelected = selectedCategoryFilter == cat.id
                        val count = allScreenshots.count { it.categoryId == cat.id }
                        val catIcon = CategoryIcons.getIcon(cat.iconName)
                        val chipColor = CategoryIcons.parseColor(cat.colorHex)

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategoryFilter = if (isSelected) null else cat.id
                            },
                            label = {
                                Text("${cat.name} ($count)")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = catIcon,
                                    contentDescription = cat.name,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else chipColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("filter_chip_${cat.id}")
                        )
                    }
                }
            }
        }

        // 9. Screenshots Grid Cards (Filtered)
        if (filteredScreenshots.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val categoryName = allCategories.find { it.id == selectedCategoryFilter }?.name ?: "Seçilen"
                val emptyTitle = when {
                    searchQuery.isNotBlank() -> "\"$searchQuery\" ile eşleşen görsel bulunamadı"
                    selectedCategoryFilter != null -> "'$categoryName' kategorisinde görsel yok"
                    else -> "Henüz ekran görüntüsü bulunamadı"
                }
                val emptyDescription = when {
                    searchQuery.isNotBlank() -> "Görsellerin OCR metninde, etiketlerinde veya dosya adlarında bu arama terimi geçmiyor. Aramayı temizleyerek tüm galeriyi görüntüleyebilirsiniz."
                    selectedCategoryFilter != null -> "Bu kategoriye atanmış ekran görüntüsü bulunmuyor. Yeni bir ekran görüntüsü tarayabilir veya tüm görselleri görebilirsiniz."
                    else -> "Cihazınızdaki ekran görüntülerini tarayarak veya 'Görsel Seç' ile ekleyerek arşivinizi başlatabilirsiniz."
                }
                val actionText = when {
                    searchQuery.isNotBlank() -> "Aramayı Temizle"
                    selectedCategoryFilter != null -> "Tümünü Göster"
                    else -> "Görselleri Tara"
                }

                EmptyStateView(
                    title = emptyTitle,
                    description = emptyDescription,
                    actionButtonText = actionText,
                    onActionClick = {
                        if (searchQuery.isNotBlank()) {
                            searchQuery = ""
                        } else if (selectedCategoryFilter != null) {
                            selectedCategoryFilter = null
                        } else {
                            requestScanWithPermissionCheck()
                        }
                    }
                )
            }
        } else {
            items(filteredScreenshots, key = { it.id }) { item ->
                val category = allCategories.find { it.id == item.categoryId }
                val catColor = if (category != null) CategoryIcons.parseColor(category.colorHex) else Color(0xFF6366F1)
                val catName = category?.name ?: "Diğer"
                val isSelected = selectedIds.contains(item.id)

                SwipeableScreenshotGridItem(
                    screenshot = item,
                    categoryColor = catColor,
                    categoryName = catName,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    onClick = {
                        if (isSelectionMode) {
                            onToggleSelect(item.id)
                        } else {
                            onScreenshotClick(item.id)
                        }
                    },
                    onLongClick = {
                        onToggleSelect(item.id)
                    },
                    onFavoriteToggle = { onFavoriteToggle(item) },
                    onDelete = { onDeleteScreenshot(item) },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }

    // ==================== FLOATING BULK ACTIONS DOCK ====================
    AnimatedVisibility(
        visible = isSelectionMode,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bulk_actions_dock")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Move to Category
                BulkActionButton(
                    icon = Icons.Default.DriveFileMove,
                    label = "Kategoriye Taşı",
                    tint = MaterialTheme.colorScheme.primary,
                    enabled = selectedIds.isNotEmpty(),
                    onClick = { showMoveCategoryDialog = true },
                    testTag = "bulk_move_to_category_button"
                )

                // 2. Share
                BulkActionButton(
                    icon = Icons.Default.Share,
                    label = "Paylaş",
                    tint = Color(0xFF0EA5E9),
                    enabled = selectedIds.isNotEmpty(),
                    onClick = { performShareSelected() },
                    testTag = "bulk_share_button"
                )

                // 3. PDF Raporu
                BulkActionButton(
                    icon = Icons.Default.PictureAsPdf,
                    label = "PDF Rapor",
                    tint = Color(0xFF8B5CF6),
                    enabled = selectedIds.isNotEmpty() && !isBulkPdfGenerating,
                    onClick = { performExportPdfSelected() },
                    testTag = "bulk_pdf_report_button"
                )

                // 4. Delete
                BulkActionButton(
                    icon = Icons.Default.Delete,
                    label = "Sil",
                    tint = Color(0xFFEF4444),
                    enabled = selectedIds.isNotEmpty(),
                    onClick = { showDeleteConfirmDialog = true },
                    testTag = "bulk_delete_button"
                )
            }
        }
    }

    // ==================== MOVE TO CATEGORY DIALOG ====================
    if (showMoveCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showMoveCategoryDialog = false },
            title = {
                Text(
                    text = "Kategoriye Taşı (${selectedIds.size} Görsel)",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allCategories.forEach { cat ->
                        val catColor = CategoryIcons.parseColor(cat.colorHex)
                        val catIcon = CategoryIcons.getIcon(cat.iconName)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, catColor.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onMoveSelectedToCategory(cat.id)
                                    showMoveCategoryDialog = false
                                }
                                .testTag("move_target_category_${cat.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = catColor.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = catIcon,
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = cat.name,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveCategoryDialog = false }) {
                    Text("İptal")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ==================== DELETE CONFIRMATION DIALOG ====================
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Seçilen Görselleri Sil", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Seçilen ${selectedIds.size} adet ekran görüntüsü kalıcı olarak silinecek. Devam etmek istiyor musunuz?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSelected()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_bulk_delete_button")
                ) {
                    Text("Sil (${selectedIds.size})", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Vazgeç")
                }
            },
        )
    }

    // ==================== QUICK DELETE DUPLICATES CONFIRMATION DIALOG ====================
    if (showQuickDeleteDuplicatesDialog) {
        AlertDialog(
            onDismissRequest = { showQuickDeleteDuplicatesDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Çift Görselleri Tek Tuşla Sil", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Tespit edilen ${smartCleanupReport.totalDuplicateCount} adet mükerrer ve aralıksız seri çekim kopya cihazınızdan silinecek (~${String.format(Locale.US, "%.1f", smartCleanupReport.duplicateSavingsMb)} MB tasarruf). Asıl kopyalar güvenle korunacaktır. Onaylıyor musunuz?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onQuickDeleteDuplicates(smartCleanupReport.allDuplicateIds)
                        showQuickDeleteDuplicatesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_home_quick_delete_duplicates_button")
                ) {
                    Text("Tek Tuşla Sil (${smartCleanupReport.totalDuplicateCount})", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickDeleteDuplicatesDialog = false }) {
                    Text("Vazgeç")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showPermissionDialog) {
        StoragePermissionDialog(
            isPermanentlyDenied = isPermanentlyDenied,
            onRequestPermission = {
                permissionLauncher.launch(StoragePermissionHelper.getRequiredStoragePermissions())
            },
            onOpenSettings = {
                showPermissionDialog = false
                StoragePermissionHelper.openAppSettings(context)
            },
            onPickFromGallery = {
                showPermissionDialog = false
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismiss = { showPermissionDialog = false }
        )
    }
}
}

@Composable
private fun BulkActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) tint.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, if (enabled) tint.copy(alpha = 0.3f) else Color.Transparent),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableScreenshotGridItem(
    screenshot: ScreenshotEntity,
    categoryColor: Color,
    categoryName: String,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSelectionMode) {
        // In selection mode, click toggles selection, and swipe to dismiss is disabled for clean checkbox taps
        ScreenshotCard(
            screenshot = screenshot,
            categoryColor = categoryColor,
            categoryName = categoryName,
            isSelected = isSelected,
            isSelectionMode = true,
            onClick = onClick,
            onLongClick = onLongClick,
            onFavoriteToggle = onFavoriteToggle,
            modifier = modifier
        )
    } else {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                    onDelete()
                    true
                } else {
                    false
                }
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            modifier = modifier
                .clip(RoundedCornerShape(20.dp))
                .testTag("swipe_to_delete_${screenshot.id}"),
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = true,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val alignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFEF4444), shape = RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = alignment
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Sil",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        ) {
            ScreenshotCard(
                screenshot = screenshot,
                categoryColor = categoryColor,
                categoryName = categoryName,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                onClick = onClick,
                onLongClick = onLongClick,
                onFavoriteToggle = onFavoriteToggle
            )
        }
    }
}
