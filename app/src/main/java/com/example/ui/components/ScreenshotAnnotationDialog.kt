package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import java.io.File
import java.io.FileOutputStream

enum class AnnotationMode {
    PEN,
    HIGHLIGHTER,
    REDACT // Privacy box
}

data class DrawnPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val alpha: Float,
    val mode: AnnotationMode,
    val rectStart: Offset? = null,
    val rectEnd: Offset? = null
)

@Composable
fun ScreenshotAnnotationDialog(
    screenshotUri: String,
    title: String,
    onDismiss: () -> Unit,
    onSaveAndShare: (Uri) -> Unit
) {
    val context = LocalContext.current
    var currentMode by remember { mutableStateOf(AnnotationMode.PEN) }
    var selectedColor by remember { mutableStateOf(Color(0xFFEF4444)) } // default Red
    val paths = remember { mutableStateListOf<DrawnPath>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var rectStart by remember { mutableStateOf<Offset?>(null) }
    var rectCurrent by remember { mutableStateOf<Offset?>(null) }

    val colors = listOf(
        Color(0xFFEF4444), // Kırmızı
        Color(0xFFF59E0B), // Turuncu / Sarı
        Color(0xFF10B981), // Yeşil
        Color(0xFF3B82F6), // Mavi
        Color(0xFF8B5CF6), // Mor
        Color(0xFFFFFFFF), // Beyaz
        Color(0xFF000000)  // Siyah
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF111827)),
            color = Color(0xFF111827)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("annotation_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Görseli İşaretle & Sansürle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = when (currentMode) {
                                AnnotationMode.PEN -> "Kalem: Serbest çizim yapın"
                                AnnotationMode.HIGHLIGHTER -> "Vurgulayıcı: Önemli yerleri parlatın"
                                AnnotationMode.REDACT -> "Sansür: Hassas bilgileri gizleyin"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                if (paths.isNotEmpty()) paths.removeAt(paths.lastIndex)
                            },
                            enabled = paths.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Undo,
                                contentDescription = "Geri Al",
                                tint = if (paths.isNotEmpty()) Color.White else Color.Gray
                            )
                        }
                        IconButton(
                            onClick = { paths.clear() },
                            enabled = paths.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Tümünü Temizle",
                                tint = if (paths.isNotEmpty()) Color(0xFFF87171) else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Canvas Area (contains image + drawn paths)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background Image
                        if (screenshotUri.startsWith("demo://")) {
                            DemoScreenshotGraphic(
                                categoryName = "Önizleme",
                                categoryColor = Color(0xFF6366F1),
                                title = title,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(screenshotUri)
                                    .crossfade(true)
                                    .error(R.drawable.hero_organizer)
                                    .build(),
                                contentDescription = title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }

                        // Drawing Canvas Layer
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("annotation_canvas")
                                .pointerInput(currentMode, selectedColor) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            if (currentMode == AnnotationMode.REDACT) {
                                                rectStart = offset
                                                rectCurrent = offset
                                            } else {
                                                currentPoints = listOf(offset)
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            if (currentMode == AnnotationMode.REDACT) {
                                                rectCurrent = change.position
                                            } else {
                                                currentPoints = currentPoints + change.position
                                            }
                                        },
                                        onDragEnd = {
                                            if (currentMode == AnnotationMode.REDACT && rectStart != null && rectCurrent != null) {
                                                paths.add(
                                                    DrawnPath(
                                                        points = emptyList(),
                                                        color = Color.Black,
                                                        strokeWidth = 0f,
                                                        alpha = 1f,
                                                        mode = AnnotationMode.REDACT,
                                                        rectStart = rectStart,
                                                        rectEnd = rectCurrent
                                                    )
                                                )
                                                rectStart = null
                                                rectCurrent = null
                                            } else if (currentPoints.isNotEmpty()) {
                                                val strokeWidth = if (currentMode == AnnotationMode.HIGHLIGHTER) 32f else 8f
                                                val alpha = if (currentMode == AnnotationMode.HIGHLIGHTER) 0.45f else 1f
                                                paths.add(
                                                    DrawnPath(
                                                        points = currentPoints,
                                                        color = selectedColor,
                                                        strokeWidth = strokeWidth,
                                                        alpha = alpha,
                                                        mode = currentMode
                                                    )
                                                )
                                                currentPoints = emptyList()
                                            }
                                        }
                                    )
                                }
                        ) {
                            // Draw saved paths
                            paths.forEach { item ->
                                when (item.mode) {
                                    AnnotationMode.REDACT -> {
                                        if (item.rectStart != null && item.rectEnd != null) {
                                            val left = minOf(item.rectStart.x, item.rectEnd.x)
                                            val top = minOf(item.rectStart.y, item.rectEnd.y)
                                            val right = maxOf(item.rectStart.x, item.rectEnd.x)
                                            val bottom = maxOf(item.rectStart.y, item.rectEnd.y)
                                            drawRect(
                                                color = Color.Black,
                                                topLeft = Offset(left, top),
                                                size = Size(right - left, bottom - top)
                                            )
                                        }
                                    }
                                    else -> {
                                        if (item.points.size > 1) {
                                            val path = Path().apply {
                                                moveTo(item.points.first().x, item.points.first().y)
                                                for (i in 1 until item.points.size) {
                                                    lineTo(item.points[i].x, item.points[i].y)
                                                }
                                            }
                                            drawPath(
                                                path = path,
                                                color = item.color.copy(alpha = item.alpha),
                                                style = Stroke(
                                                    width = item.strokeWidth,
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // Draw current path being drawn
                            if (currentMode == AnnotationMode.REDACT && rectStart != null && rectCurrent != null) {
                                val left = minOf(rectStart!!.x, rectCurrent!!.x)
                                val top = minOf(rectStart!!.y, rectCurrent!!.y)
                                val right = maxOf(rectStart!!.x, rectCurrent!!.x)
                                val bottom = maxOf(rectStart!!.y, rectCurrent!!.y)
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.8f),
                                    topLeft = Offset(left, top),
                                    size = Size(right - left, bottom - top)
                                )
                            } else if (currentPoints.size > 1) {
                                val strokeWidth = if (currentMode == AnnotationMode.HIGHLIGHTER) 32f else 8f
                                val alpha = if (currentMode == AnnotationMode.HIGHLIGHTER) 0.45f else 1f
                                val path = Path().apply {
                                    moveTo(currentPoints.first().x, currentPoints.first().y)
                                    for (i in 1 until currentPoints.size) {
                                        lineTo(currentPoints[i].x, currentPoints[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = selectedColor.copy(alpha = alpha),
                                    style = Stroke(
                                        width = strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tool Selector Bar
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1F2937),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ToolButton(
                            label = "Kalem",
                            icon = Icons.Default.Brush,
                            isSelected = currentMode == AnnotationMode.PEN,
                            onClick = { currentMode = AnnotationMode.PEN }
                        )

                        ToolButton(
                            label = "Vurgulayıcı",
                            icon = Icons.Default.FormatPaint,
                            isSelected = currentMode == AnnotationMode.HIGHLIGHTER,
                            onClick = { currentMode = AnnotationMode.HIGHLIGHTER }
                        )

                        ToolButton(
                            label = "Sansürle",
                            icon = Icons.Default.Security,
                            isSelected = currentMode == AnnotationMode.REDACT,
                            onClick = { currentMode = AnnotationMode.REDACT }
                        )
                    }
                }

                // Color Picker Row (hidden in REDACT mode)
                if (currentMode != AnnotationMode.REDACT) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedColor == color) 3.dp else 1.dp,
                                        color = if (selectedColor == color) Color.White else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = color },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == color) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (color == Color.White) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Vazgeç")
                    }

                    Button(
                        onClick = {
                            saveAndShareAnnotatedBitmap(
                                context = context,
                                paths = paths,
                                onCompleted = { uri ->
                                    onSaveAndShare(uri)
                                    onDismiss()
                                }
                            )
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("annotation_save_share_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kaydet & Paylaş", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            )
        }
    }
}

private fun saveAndShareAnnotatedBitmap(
    context: Context,
    paths: List<DrawnPath>,
    onCompleted: (Uri) -> Unit
) {
    try {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // Draw paths onto bitmap
        paths.forEach { item ->
            when (item.mode) {
                AnnotationMode.REDACT -> {
                    if (item.rectStart != null && item.rectEnd != null) {
                        paint.style = Paint.Style.FILL
                        paint.color = android.graphics.Color.BLACK
                        val left = minOf(item.rectStart.x, item.rectEnd.x)
                        val top = minOf(item.rectStart.y, item.rectEnd.y)
                        val right = maxOf(item.rectStart.x, item.rectEnd.x)
                        val bottom = maxOf(item.rectStart.y, item.rectEnd.y)
                        canvas.drawRect(left, top, right, bottom, paint)
                    }
                }
                else -> {
                    if (item.points.size > 1) {
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = item.strokeWidth * 1.5f
                        val androidColor = android.graphics.Color.argb(
                            (item.alpha * 255).toInt(),
                            (item.color.red * 255).toInt(),
                            (item.color.green * 255).toInt(),
                            (item.color.blue * 255).toInt()
                        )
                        paint.color = androidColor
                        val path = android.graphics.Path()
                        path.moveTo(item.points.first().x, item.points.first().y)
                        for (i in 1 until item.points.size) {
                            path.lineTo(item.points[i].x, item.points[i].y)
                        }
                        canvas.drawPath(path, paint)
                    }
                }
            }
        }

        val cacheDir = File(context.cacheDir, "annotated")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val file = File(cacheDir, "annotated_${System.currentTimeMillis()}.png")
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        out.flush()
        out.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "İşaretlenmiş Görseli Paylaş"))
        Toast.makeText(context, "Sansürlenmiş görsel hazırlandı ve paylaşıldı!", Toast.LENGTH_SHORT).show()
        onCompleted(uri)
    } catch (e: Exception) {
        Toast.makeText(context, "Görsel kaydedilirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
