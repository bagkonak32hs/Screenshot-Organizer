package com.example.ui.screens.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.data.pdf.PdfExportService
import com.example.ui.components.ScreenshotAnnotationDialog
import com.example.ui.components.SmartActionDeck
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.CategoryEntity
import com.example.data.model.ScreenshotEntity
import com.example.ui.components.CategoryIcons
import com.example.ui.components.DemoScreenshotGraphic

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScreenshotDetailScreen(
    screenshot: ScreenshotEntity?,
    allCategories: List<CategoryEntity>,
    onBack: () -> Unit,
    onCategoryChange: (Long, String) -> Unit,
    onFavoriteToggle: (ScreenshotEntity) -> Unit,
    onHiddenToggle: (ScreenshotEntity) -> Unit,
    onUpdateScreenshot: (ScreenshotEntity) -> Unit,
    onDeleteScreenshot: (ScreenshotEntity) -> Unit,
    onReAnalyze: (Long) -> Unit,
    onReportIncorrect: (ScreenshotEntity) -> Unit,
    onCreateReminder: (Long, String, Long) -> Unit,
    onPerformGeminiOcr: (Long) -> Unit = {},
    onClassifyWithGemini: (Long) -> Unit = {},
    onAnalyzeAndSuggest: (Long) -> Unit = onClassifyWithGemini,
    onApplySuggestedCategory: (Long, String) -> Unit = { id, cat -> onCategoryChange(id, cat) },
    suggestedCategories: List<String> = screenshot?.suggestedCategoriesList ?: listOf("Work", "Personal", "Ideas"),
    isOcrLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    if (screenshot == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Görsel bulunamadı.")
        }
        return
    }

    var showEditTitleDialog by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf(screenshot.title) }

    var showCategoryMenu by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var reminderTitle by remember { mutableStateOf(screenshot.actionSuggestion ?: "Fatura Ödeme Hatırlatması") }

    val coroutineScope = rememberCoroutineScope()
    var showAnnotationDialog by remember { mutableStateOf(false) }
    var showEditNotesDialog by remember { mutableStateOf(false) }
    var editedNotes by remember { mutableStateOf(screenshot.userNotes ?: "") }
    var isPdfGenerating by remember { mutableStateOf(false) }

    val category = allCategories.find { it.id == screenshot.categoryId }
    val catColor = if (category != null) CategoryIcons.parseColor(category.colorHex) else Color(0xFF6366F1)
    val catName = category?.name ?: "Diğer"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Görsel Detayı", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { onFavoriteToggle(screenshot) }) {
                        Icon(
                            imageVector = if (screenshot.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favori",
                            tint = if (screenshot.isFavorite) Color(0xFFF43F5E) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { onHiddenToggle(screenshot) }) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Gizle",
                            tint = if (screenshot.isHidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { showAnnotationDialog = true },
                        modifier = Modifier.testTag("detail_annotate_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = "Sansürle & Çiz",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            if (!isPdfGenerating) {
                                isPdfGenerating = true
                                coroutineScope.launch {
                                    val result = PdfExportService.exportSingleScreenshotToPdf(
                                        context = context,
                                        screenshot = screenshot,
                                        categoryName = catName
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
                                        Toast.makeText(context, "PDF oluşturulamadı: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("detail_export_pdf_button")
                    ) {
                        if (isPdfGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "PDF Olarak Paylaş",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(
                        onClick = { onPerformGeminiOcr(screenshot.id) },
                        enabled = !isOcrLoading,
                        modifier = Modifier.testTag("detail_gemini_ocr_top_button")
                    ) {
                        if (isOcrLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini OCR & Etiketleme",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, screenshot.title)
                                putExtra(Intent.EXTRA_TEXT, "${screenshot.title}\n\n${screenshot.summary}\n\n${screenshot.ocrText}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Ekran Görüntüsünü Paylaş"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Paylaş")
                    }
                    IconButton(
                        onClick = {
                            onDeleteScreenshot(screenshot)
                            onBack()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color(0xFFEF4444))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 1. Image Preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (screenshot.uri.startsWith("demo://")) {
                        DemoScreenshotGraphic(
                            categoryName = catName,
                            categoryColor = catColor,
                            title = screenshot.title,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(screenshot.uri)
                                .crossfade(true)
                                .error(R.drawable.hero_organizer)
                                .placeholder(R.drawable.hero_organizer)
                                .build(),
                            contentDescription = screenshot.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Tool Buttons Row (Sansürle/Çiz, PDF Raporu, Not Ekle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showAnnotationDialog = true },
                    modifier = Modifier.weight(1f).testTag("detail_quick_annotate_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sansürle", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        if (!isPdfGenerating) {
                            isPdfGenerating = true
                            coroutineScope.launch {
                                val result = PdfExportService.exportSingleScreenshotToPdf(
                                    context = context,
                                    screenshot = screenshot,
                                    categoryName = catName
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
                    },
                    modifier = Modifier.weight(1f).testTag("detail_quick_pdf_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF Rapor", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        editedNotes = screenshot.userNotes ?: ""
                        showEditNotesDialog = true
                    },
                    modifier = Modifier.weight(1f).testTag("detail_quick_notes_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Not Ekle", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Akıllı Aksiyon Butonları (Smart Actionable Chips: Ara, IBAN Kopyala, URL Aç, Harita, Takvim, Hatırlatıcı)
            SmartActionDeck(
                screenshot = screenshot,
                onCreateReminder = { showReminderDialog = true }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Kişisel Notlar Kartı (Personal User Notes)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        editedNotes = screenshot.userNotes ?: ""
                        showEditNotesDialog = true
                    }
                    .testTag("detail_user_notes_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!screenshot.userNotes.isNullOrBlank()) {
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (!screenshot.userNotes.isNullOrBlank()) {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kişisel Not",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        TextButton(
                            onClick = {
                                editedNotes = screenshot.userNotes ?: ""
                                showEditNotesDialog = true
                            }
                        ) {
                            Text(if (screenshot.userNotes.isNullOrBlank()) "Not Ekle" else "Düzenle")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (!screenshot.userNotes.isNullOrBlank()) {
                            screenshot.userNotes!!
                        } else {
                            "Bu ekran görüntüsü için henüz bir kişisel not eklenmemiş. Önemli detayları veya yapılacakları buraya kaydedebilirsiniz."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (!screenshot.userNotes.isNullOrBlank()) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Sensitive Warning Banner (if detected)
            if (screenshot.isSensitive) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Bu ekran görüntüsünde hassas finansal veya kişisel bilgi (IBAN, kart numarası vb.) tespit edildi. Güvenliğiniz için dikkatli saklayın.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEF4444),
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. AI Suggestion Banner
            if (!screenshot.actionSuggestion.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Yapay Zekâ Önerisi",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = screenshot.actionSuggestion!!,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showReminderDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Hatırlatıcı Oluştur")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 4. Title, Confidence & Category
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = catColor
                        ) {
                            Text(
                                text = catName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Güven: %${(screenshot.confidence * 100).toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = screenshot.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            editedTitle = screenshot.title
                            showEditTitleDialog = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Başlığı Düzenle")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = screenshot.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4.5. Gemini AI Category Suggestions Card (Work, Personal, Ideas)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_ai_category_suggestions_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini Kategori Önerileri",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (isOcrLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            TextButton(
                                onClick = { onAnalyzeAndSuggest(screenshot.id) }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Yeniden Öner", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gemini AI görseli analiz ederek en uygun kategorileri sıraladı. Bir kategoriye dokunarak görseli hemen taşıyabilirsiniz:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val displaySuggestions = if (suggestedCategories.isNotEmpty()) {
                            suggestedCategories
                        } else {
                            listOf("Work", "Personal", "Ideas")
                        }

                        displaySuggestions.forEach { catLabel ->
                            val catId = when (catLabel.trim().lowercase()) {
                                "work", "iş" -> ScreenshotEntity.CATEGORY_WORK
                                "personal", "kişisel" -> ScreenshotEntity.CATEGORY_PERSONAL
                                "ideas", "fikirler", "fikirler & ilham", "ilham" -> ScreenshotEntity.CATEGORY_IDEAS
                                "school", "okul" -> ScreenshotEntity.CATEGORY_SCHOOL
                                "entertainment", "eğlence" -> ScreenshotEntity.CATEGORY_ENTERTAINMENT
                                else -> catLabel.trim().lowercase()
                            }
                            val isSelected = screenshot.categoryId == catId
                            val icon = when (catId) {
                                ScreenshotEntity.CATEGORY_WORK -> Icons.Default.Work
                                ScreenshotEntity.CATEGORY_PERSONAL -> Icons.Default.Person
                                ScreenshotEntity.CATEGORY_IDEAS -> Icons.Default.Lightbulb
                                ScreenshotEntity.CATEGORY_SCHOOL -> Icons.Default.School
                                ScreenshotEntity.CATEGORY_ENTERTAINMENT -> Icons.Default.SportsEsports
                                else -> Icons.Default.Folder
                            }

                            SuggestionChip(
                                onClick = { onApplySuggestedCategory(screenshot.id, catLabel) },
                                label = {
                                    Text(
                                        text = when (catId) {
                                            ScreenshotEntity.CATEGORY_WORK -> "İş (Work)"
                                            ScreenshotEntity.CATEGORY_PERSONAL -> "Kişisel (Personal)"
                                            ScreenshotEntity.CATEGORY_IDEAS -> "Fikirler (Ideas)"
                                            ScreenshotEntity.CATEGORY_SCHOOL -> "Okul (School)"
                                            ScreenshotEntity.CATEGORY_ENTERTAINMENT -> "Eğlence (Entertainment)"
                                            else -> catLabel
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Extracted Important Fields Grid
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tespit Edilen Önemli Bilgiler",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val fields = listOfNotNull(
                        screenshot.company?.let { "Firma / Kurum" to it },
                        screenshot.amount?.let { "Tutar" to it },
                        screenshot.dateStr?.let { "Tarih" to it },
                        screenshot.invoiceNumber?.let { "Fatura / Ref No" to it },
                        screenshot.address?.let { "Adres" to it },
                        screenshot.phoneNumber?.let { "Telefon" to it },
                        screenshot.trackingNumber?.let { "Takip Kodu" to it }
                    )

                    if (fields.isEmpty()) {
                        Text(
                            text = "Bu bilgi kesin olarak tespit edilemedi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        fields.forEach { (label, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6 & 7. Gemini OCR & Searchable Tags Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_gemini_ocr_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Gemini OCR & Etiketleme",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Görseldeki metinlerden üretilen veriler",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { onPerformGeminiOcr(screenshot.id) },
                            enabled = !isOcrLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("detail_run_gemini_ocr_button")
                        ) {
                            if (isOcrLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Taranıyor...", fontSize = 12.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (screenshot.ocrText.isBlank()) "OCR Başlat" else "Yeniden Tara",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (isOcrLoading) {
                        Spacer(modifier = Modifier.height(14.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Gemini Vision görseldeki yazıları okuyor ve etiketler üretiyor...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Searchable Tags Generated from Image Text
                    val tagsList = screenshot.tagsJson
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Aranabilir Etiketler (${tagsList.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (tagsList.isEmpty()) {
                        Text(
                            text = "Henüz etiket üretilmedi. 'OCR Başlat' butonuna dokunarak görseldeki yazılardan etiket oluşturabilirsiniz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tagsList.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Tag", tag)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "#$tag panoya kopyalandı", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // OCR Transcribed Text Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Görsel OCR Metni (${screenshot.ocrText.length} karakter)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Screenshot OCR", screenshot.ocrText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "OCR metni panoya kopyalandı", Toast.LENGTH_SHORT).show()
                            },
                            enabled = screenshot.ocrText.isNotBlank()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tümünü Kopyala")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = screenshot.ocrText.ifBlank { "Henüz OCR metni taranmadı. Yukarıdaki 'OCR Başlat' butonuyla görseldeki yazıları okuyabilirsiniz." },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(14.dp),
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 8. Management Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "İşlemler & Geri Bildirim",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Change Category Box
                    Box {
                        OutlinedButton(
                            onClick = { showCategoryMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kategori Değiştir ($catName)")
                        }

                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            allCategories.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = {
                                        onCategoryChange(screenshot.id, c.id)
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { onAnalyzeAndSuggest(screenshot.id) },
                        modifier = Modifier.fillMaxWidth().testTag("detail_gemini_classify_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini ile Kategori Öner & Sınıflandır (Work, Personal, Ideas)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { onReAnalyze(screenshot.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Yapay Zekâ Analizini Yenile")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { onReportIncorrect(screenshot) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Feedback, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Yanlış Kategorilendirildi")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Edit Title Dialog
    if (showEditTitleDialog) {
        Dialog(onDismissRequest = { showEditTitleDialog = false }) {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Başlığı Düzenle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showEditTitleDialog = false }) { Text("İptal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            onUpdateScreenshot(screenshot.copy(title = editedTitle))
                            showEditTitleDialog = false
                        }) { Text("Kaydet") }
                    }
                }
            }
        }
    }

    // Reminder Dialog
    if (showReminderDialog) {
        Dialog(onDismissRequest = { showReminderDialog = false }) {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Hatırlatıcı Oluştur", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reminderTitle,
                        onValueChange = { reminderTitle = it },
                        label = { Text("Hatırlatıcı Başlığı") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showReminderDialog = false }) { Text("İptal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val oneWeekLater = System.currentTimeMillis() + (7 * 86_400_000L)
                            onCreateReminder(screenshot.id, reminderTitle, oneWeekLater)
                            showReminderDialog = false
                        }) { Text("Oluştur") }
                    }
                }
            }
        }
    }

    // Annotation & Privacy Redaction Tool Dialog
    if (showAnnotationDialog) {
        ScreenshotAnnotationDialog(
            screenshotUri = screenshot.uri,
            title = screenshot.title,
            onDismiss = { showAnnotationDialog = false },
            onSaveAndShare = { uri ->
                Toast.makeText(context, "Sansürlenmiş görsel hazırlandı", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Edit Personal Notes Dialog
    if (showEditNotesDialog) {
        Dialog(onDismissRequest = { showEditNotesDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NoteAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Kişisel Not Ekle / Düzenle",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editedNotes,
                        onValueChange = { editedNotes = it },
                        label = { Text("Notunuz") },
                        placeholder = { Text("Örn: Bu faturayı muhasebeye ilet, indirim kodu vb.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showEditNotesDialog = false }) { Text("İptal") }
                        if (screenshot.userNotes != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            TextButton(
                                onClick = {
                                    onUpdateScreenshot(screenshot.copy(userNotes = null))
                                    editedNotes = ""
                                    showEditNotesDialog = false
                                }
                            ) {
                                Text("Notu Sil", color = Color(0xFFEF4444))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val trimmed = editedNotes.trim()
                            onUpdateScreenshot(screenshot.copy(userNotes = if (trimmed.isEmpty()) null else trimmed))
                            showEditNotesDialog = false
                        }) {
                            Text("Kaydet")
                        }
                    }
                }
            }
        }
    }
}
