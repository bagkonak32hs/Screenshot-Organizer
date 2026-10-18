package com.example.ui.screens.cleanup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.cleanup.DuplicateGroup
import com.example.data.cleanup.OldScreenshotItem
import com.example.data.cleanup.SmartCleanupReport
import com.example.data.model.CategoryEntity
import com.example.data.model.ScreenshotEntity
import com.example.ui.components.CategoryIcons
import com.example.ui.components.DemoScreenshotGraphic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCleanupScreen(
    report: SmartCleanupReport,
    selectedIds: Set<Long>,
    allCategories: List<CategoryEntity>,
    onToggleSelect: (Long) -> Unit,
    onSelectAll: (Set<Long>) -> Unit,
    onClearSelection: () -> Unit,
    onExecuteCleanup: (Set<Long>) -> Unit,
    onScreenshotClick: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    var selectedFilterTab by remember { mutableStateOf("all") } // all, duplicates, old
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showQuickDeleteDuplicatesDialog by remember { mutableStateOf(false) }
    var comparingGroup by remember { mutableStateOf<DuplicateGroup?>(null) }

    // Auto-select all detected redundant items initially if selection is empty
    LaunchedEffect(report) {
        if (selectedIds.isEmpty() && report.allCleanableIds.isNotEmpty()) {
            onSelectAll(report.allCleanableIds)
        }
    }

    val selectedSavingsMb = remember(selectedIds.size) {
        String.format(Locale.US, "%.1f", selectedIds.size * 2.4)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Akıllı Temizlik",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("smart_cleanup_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                actions = {
                    if (report.totalCleanableCount > 0) {
                        TextButton(
                            onClick = {
                                if (selectedIds.size == report.allCleanableIds.size) {
                                    onClearSelection()
                                } else {
                                    onSelectAll(report.allCleanableIds)
                                }
                            },
                            modifier = Modifier.testTag("smart_cleanup_select_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedIds.size == report.allCleanableIds.size) "Kaldır" else "Tümünü Seç",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (report.totalCleanableCount > 0) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${selectedIds.size} Görsel Seçildi",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "~$selectedSavingsMb MB alan kazanılacak",
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Button(
                            onClick = { showConfirmDialog = true },
                            enabled = selectedIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("execute_cleanup_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Temizle (${selectedIds.size})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("smart_cleanup_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Storage Recovery Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cleanup_hero_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF065F46),
                                        Color(0xFF0F172A)
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
                                    color = Color(0xFF10B981).copy(alpha = 0.25f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CleaningServices,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Otomatik Depolama Temizliği",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Kazanılabilir Alan: ~${String.format(Locale.US, "%.1f", report.estimatedSavingsMb)} MB",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Yapay zekâ çift çekilmiş mükerrer faturaları, seri (burst) çekimleri ve süresi dolmuş geçici SMS/OTP kodlarını tespit etti.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Badges Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "📁 ${report.totalDuplicateCount} Çift Kopya",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                if (report.totalBurstCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF59E0B).copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = "🔥 ${report.totalBurstCount} Seri Çekim",
                                            color = Color(0xFFFDE68A),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "⏳ ${report.totalOldCount} Süresi Dolmuş",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1.5 ONE-TOUCH DELETE ALL DUPLICATES QUICK ACTION CARD
            if (report.totalDuplicateCount > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_delete_duplicates_banner"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.2.dp, Color(0xFFEF4444).copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Hızlı Temizlik: Tüm Çiftleri Sil",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFEF4444)
                                    )
                                    Text(
                                        text = "${report.totalDuplicateCount} gereksiz kopya (~${String.format(Locale.US, "%.1f", report.duplicateSavingsMb)} MB tasarruf)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { showQuickDeleteDuplicatesDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("one_touch_delete_duplicates_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tek Tuşla Sil",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 2. Filter Tabs
            if (report.totalCleanableCount > 0) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilterTab == "all",
                                onClick = { selectedFilterTab = "all" },
                                label = { Text("Tümü (${report.totalCleanableCount})") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("filter_cleanup_all")
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilterTab == "duplicates",
                                onClick = { selectedFilterTab = "duplicates" },
                                label = { Text("Çift & Seri Çekimler (${report.totalDuplicateCount})") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("filter_cleanup_duplicates")
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilterTab == "old",
                                onClick = { selectedFilterTab = "old" },
                                label = { Text("Eski & OTP (${report.totalOldCount})") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.HourglassBottom,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("filter_cleanup_old")
                            )
                        }
                    }
                }
            }

            // 3. Empty State (Clean device)
            if (report.totalCleanableCount == 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Arşiviniz Tertemiz! 🎉",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Mükerrer kopya veya süresi dolmuş geçici ekran görüntüsü tespit edilmedi. Cihaz depolama alanınız en verimli durumda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 4. DUPLICATE & BURST CLUSTERS
            if ((selectedFilterTab == "all" || selectedFilterTab == "duplicates") && report.duplicateGroups.isNotEmpty()) {
                item {
                    Text(
                        text = "Mükerrer & Seri Çekimler (${report.totalDuplicateCount})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(report.duplicateGroups) { group ->
                    DuplicateClusterCard(
                        group = group,
                        allCategories = allCategories,
                        selectedIds = selectedIds,
                        onToggleSelect = onToggleSelect,
                        onScreenshotClick = onScreenshotClick,
                        onCompareClick = { comparingGroup = group },
                        onDeleteGroupDuplicates = {
                            val ids = group.duplicates.map { it.id }.toSet()
                            onExecuteCleanup(ids)
                        }
                    )
                }
            }

            // 5. OLD & STALE SCREENSHOTS
            if ((selectedFilterTab == "all" || selectedFilterTab == "old") && report.oldScreenshots.isNotEmpty()) {
                item {
                    Text(
                        text = "Süresi Dolmuş & Eski Belgeler (${report.totalOldCount})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(report.oldScreenshots) { oldItem ->
                    OldScreenshotCard(
                        item = oldItem,
                        allCategories = allCategories,
                        isSelected = selectedIds.contains(oldItem.screenshot.id),
                        onToggleSelect = { onToggleSelect(oldItem.screenshot.id) },
                        onScreenshotClick = { onScreenshotClick(oldItem.screenshot.id) }
                    )
                }
            }
        }
    }

    // Confirmation Dialog for Selected Items
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(text = "Gereksiz Görselleri Temizle", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Seçilen ${selectedIds.size} adet mükerrer ve süresi dolmuş ekran görüntüsü kalıcı olarak silinecek (~$selectedSavingsMb MB alan kazanılacak). Devam etmek istiyor musunuz?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onExecuteCleanup(selectedIds)
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_execute_cleanup_button")
                ) {
                    Text("Temizle (${selectedIds.size})", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Vazgeç")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // One-Touch Delete All Duplicates Confirmation Dialog
    if (showQuickDeleteDuplicatesDialog) {
        AlertDialog(
            onDismissRequest = { showQuickDeleteDuplicatesDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(text = "Tüm Çiftleri Tek Tuşla Sil", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Tespit edilen ${report.totalDuplicateCount} adet mükerrer ve seri çekim görsel tek tuşla silinecek (~${String.format(Locale.US, "%.1f", report.duplicateSavingsMb)} MB tasarruf). Asıl kopyalar güvenle korunacaktır. Onaylıyor musunuz?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onExecuteCleanup(report.allDuplicateIds)
                        showQuickDeleteDuplicatesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_quick_delete_duplicates_button")
                ) {
                    Text("Tek Tuşla Sil (${report.totalDuplicateCount})", color = Color.White, fontWeight = FontWeight.Bold)
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

    // Side-by-Side Comparison Dialog
    comparingGroup?.let { group ->
        CompareDuplicatesDialog(
            group = group,
            onDismiss = { comparingGroup = null },
            onDeleteDuplicate = { dupId ->
                onExecuteCleanup(setOf(dupId))
                comparingGroup = null
            }
        )
    }
}

@Composable
fun DuplicateClusterCard(
    group: DuplicateGroup,
    allCategories: List<CategoryEntity>,
    selectedIds: Set<Long>,
    onToggleSelect: (Long) -> Unit,
    onScreenshotClick: (Long) -> Unit,
    onCompareClick: () -> Unit,
    onDeleteGroupDuplicates: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("duplicate_cluster_${group.original.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Reason Header & Similarity Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (group.isBurst) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFF3B82F6).copy(alpha = 0.15f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (group.isBurst) Icons.Default.CameraAlt else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (group.isBurst) Color(0xFFD97706) else Color(0xFF2563EB),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = group.reason,
                            color = if (group.isBurst) Color(0xFFB45309) else Color(0xFF1D4ED8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "%${group.similarityScore} Benzerlik",
                        color = Color(0xFF047857),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Original Kept Item
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF10B981).copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(10.dp)
                    .clickable { onScreenshotClick(group.original.id) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScreenshotThumbnailBox(
                    screenshot = group.original,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981)
                        ) {
                            Text(
                                text = "ASIL GÖRSEL (SAKLANACAK)",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = group.original.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = group.original.fileName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Saklanıyor",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Duplicate Redundant Items to Delete
            group.duplicates.forEach { dup ->
                val isSelected = selectedIds.contains(dup.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            if (isSelected) Color(0xFFEF4444).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(14.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFFEF4444).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(10.dp)
                        .clickable { onToggleSelect(dup.id) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScreenshotThumbnailBox(
                        screenshot = dup,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEF4444)
                            ) {
                                Text(
                                    text = if (group.isBurst) "SERİ ÇEKİM KOPYA" else "MÜKERRER KOPYA",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dup.title,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dup.fileName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect(dup.id) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier.testTag("checkbox_duplicate_${dup.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Actions: Compare & Quick Delete This Group
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCompareClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("compare_duplicates_button_${group.original.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Yan Yana Karşılaştır", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = onDeleteGroupDuplicates,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bu Çifti Sil (${group.duplicates.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CompareDuplicatesDialog(
    group: DuplicateGroup,
    onDismiss: () -> Unit,
    onDeleteDuplicate: (Long) -> Unit
) {
    val dup = group.duplicates.firstOrNull() ?: return
    val originalDate = remember(group.original.createdAt) {
        SimpleDateFormat("HH:mm:ss - dd.MM.yyyy", Locale.getDefault()).format(Date(group.original.createdAt))
    }
    val dupDate = remember(dup.createdAt) {
        SimpleDateFormat("HH:mm:ss - dd.MM.yyyy", Locale.getDefault()).format(Date(dup.createdAt))
    }
    val timeDiffSec = abs(group.original.createdAt - dup.createdAt) / 1000

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Yan Yana Karşılaştırma",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "%${group.similarityScore} Benzerlik • Fark: ${timeDiffSec} saniye",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (group.isBurst) "Seri Çekim" else "Çift Kopya",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Side by Side Visual Comparison
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left: Original
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF10B981).copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .border(1.5.dp, Color(0xFF10B981), RoundedCornerShape(14.dp))
                            .padding(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ASIL GÖRSEL",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        ScreenshotThumbnailBox(
                            screenshot = group.original,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = group.original.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = originalDate,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Right: Duplicate
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFEF4444).copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(14.dp))
                            .padding(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEF4444),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "MÜKERRER KOPYA",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        ScreenshotThumbnailBox(
                            screenshot = dup,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dup.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dupDate,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detail notes
                Text(
                    text = "Tespit Nedeni:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = group.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (group.original.ocrText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Metin İçeriği:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = group.original.ocrText.take(120) + "...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Kapat")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onDeleteDuplicate(dup.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bu Kopyayı Sil", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun OldScreenshotCard(
    item: OldScreenshotItem,
    allCategories: List<CategoryEntity>,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onScreenshotClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggleSelect() }
            .testTag("old_screenshot_card_${item.screenshot.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFFEF4444).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScreenshotThumbnailBox(
                screenshot = item.screenshot,
                modifier = Modifier
                    .size(60.dp)
                    .clickable { onScreenshotClick() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = item.badgeLabel,
                        color = Color(0xFF7C3AED),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.screenshot.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)),
                modifier = Modifier.testTag("checkbox_old_${item.screenshot.id}")
            )
        }
    }
}

@Composable
fun ScreenshotThumbnailBox(
    screenshot: ScreenshotEntity,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (screenshot.uri.startsWith("demo://")) {
            DemoScreenshotGraphic(
                categoryName = screenshot.categoryId,
                categoryColor = Color(0xFF6366F1),
                title = screenshot.title,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(screenshot.uri)
                    .crossfade(true)
                    .error(R.drawable.hero_organizer)
                    .placeholder(R.drawable.hero_organizer)
                    .build(),
                contentDescription = screenshot.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
