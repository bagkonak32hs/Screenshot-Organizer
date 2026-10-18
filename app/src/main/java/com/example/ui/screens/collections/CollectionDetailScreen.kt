package com.example.ui.screens.collections

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.data.pdf.PdfExportService
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.ScreenshotEntity
import com.example.ui.components.CategoryIcons
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ScreenshotCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    category: CategoryEntity?,
    screenshots: List<ScreenshotEntity>,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    allCategories: List<CategoryEntity> = emptyList(),
    onScreenshotClick: (Long) -> Unit,
    onToggleFavorite: (ScreenshotEntity) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onMoveSelectedToCategory: (String) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        if (isSelectionMode) {
            onClearSelection()
        } else {
            onBack()
        }
    }

    val catName = category?.name ?: "Koleksiyon"
    val catColor = if (category != null) CategoryIcons.parseColor(category.colorHex) else Color(0xFF6366F1)
    val catIcon = if (category != null) CategoryIcons.getIcon(category.iconName) else Icons.AutoMirrored.Filled.ArrowBack

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showMoveCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isPdfGenerating by remember { mutableStateOf(false) }

    val performShareSelected: () -> Unit = {
        val selectedScreenshots = screenshots.filter { it.id in selectedIds }
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

    val performExportPdfSelected: () -> Unit = {
        val selectedScreenshots = screenshots.filter { it.id in selectedIds }
        if (selectedScreenshots.isNotEmpty() && !isPdfGenerating) {
            isPdfGenerating = true
            coroutineScope.launch {
                val catMap = mapOf((category?.id ?: "") to catName)
                val result = PdfExportService.exportMultipleScreenshotsToPdf(
                    context = context,
                    screenshots = selectedScreenshots,
                    categoryMap = catMap
                )
                isPdfGenerating = false
                result.onSuccess { uri ->
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "PDF Raporunu Paylaş"))
                }.onFailure { err ->
                    Toast.makeText(context, "PDF hatası: ${err.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var filterFavoritesOnly by remember { mutableStateOf(false) }
    var filterSensitiveOnly by remember { mutableStateOf(false) }

    val displayedScreenshots = remember(screenshots, filterFavoritesOnly, filterSensitiveOnly) {
        var list = screenshots
        if (filterFavoritesOnly) list = list.filter { it.isFavorite }
        if (filterSensitiveOnly) list = list.filter { it.isSensitive }
        list
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text("${selectedIds.size} Seçildi", fontWeight = FontWeight.Bold)
                    } else {
                        Text(catName, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSelectionMode) onClearSelection() else onBack()
                        },
                        modifier = Modifier.testTag("collection_back_button")
                    ) {
                        Icon(
                            imageVector = if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = onSelectAll) {
                            Icon(imageVector = Icons.Default.SelectAll, contentDescription = "Tümünü Seç")
                        }
                        IconButton(
                            onClick = { showMoveCategoryDialog = true },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(imageVector = Icons.Default.DriveFileMove, contentDescription = "Kategoriye Taşı")
                        }
                        IconButton(
                            onClick = performExportPdfSelected,
                            enabled = selectedIds.isNotEmpty() && !isPdfGenerating,
                            modifier = Modifier.testTag("collection_bulk_pdf_button")
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF Raporu Oluştur", tint = Color(0xFF8B5CF6))
                        }
                        IconButton(
                            onClick = performShareSelected,
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Paylaş")
                        }
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Seçilenleri Sil", tint = Color(0xFFEF4444))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("collection_detail_grid"),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Card
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = catColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = catIcon,
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = catName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${screenshots.size} adet ekran görüntüsü",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!category?.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = category!!.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Filter Chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = filterFavoritesOnly,
                                onClick = { filterFavoritesOnly = !filterFavoritesOnly },
                                label = { Text("Favoriler") }
                            )
                            FilterChip(
                                selected = filterSensitiveOnly,
                                onClick = { filterSensitiveOnly = !filterSensitiveOnly },
                                label = { Text("Hassas Bilgiler") }
                            )
                        }
                    }
                }
            }

            if (displayedScreenshots.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyStateView(
                        title = "Bu koleksiyon boş",
                        description = "Bu kategoriye ait henüz bir ekran görüntüsü eklenmemiş veya seçili filtrelere uyan görsel yok."
                    )
                }
            } else {
                items(displayedScreenshots) { item ->
                    val isSelected = selectedIds.contains(item.id)
                    ScreenshotCard(
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
                        onFavoriteToggle = { onToggleFavorite(item) }
                    )
                }
            }
        }

        // Move to Category Dialog
        if (showMoveCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showMoveCategoryDialog = false },
                title = {
                    Text("Kategoriye Taşı (${selectedIds.size} Görsel)", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allCategories.forEach { cat ->
                            val cColor = CategoryIcons.parseColor(cat.colorHex)
                            val cIcon = CategoryIcons.getIcon(cat.iconName)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, cColor.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onMoveSelectedToCategory(cat.id)
                                        showMoveCategoryDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = cColor.copy(alpha = 0.2f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(imageVector = cIcon, contentDescription = null, tint = cColor, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = cat.name, fontWeight = FontWeight.SemiBold)
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
                }
            )
        }

        // Delete Confirm Dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                icon = {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444))
                },
                title = { Text("Seçilenleri Sil") },
                text = { Text("${selectedIds.size} adet ekran görüntüsü silinecek.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteSelected()
                            showDeleteConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("Sil", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Vazgeç")
                    }
                }
            )
        }
    }
}
