package com.example.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.ScreenshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportService {

    // Standard A4 dimensions in PostScript points (72 points per inch)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f

    suspend fun exportSingleScreenshotToPdf(
        context: Context,
        screenshot: ScreenshotEntity,
        categoryName: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            renderScreenshotPage(
                context = context,
                canvas = canvas,
                screenshot = screenshot,
                categoryName = categoryName,
                pageNumber = 1,
                totalPages = 1
            )

            pdfDoc.finishPage(page)

            val exportDir = File(context.cacheDir, "pdf_exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val sanitizedTitle = screenshot.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
            val fileName = "Screenshot_${sanitizedTitle}_${System.currentTimeMillis()}.pdf"
            val file = File(exportDir, fileName)

            val outputStream = FileOutputStream(file)
            pdfDoc.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDoc.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportMultipleScreenshotsToPdf(
        context: Context,
        screenshots: List<ScreenshotEntity>,
        categoryMap: Map<String, String>
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val pdfDoc = PdfDocument()
            val totalPages = screenshots.size + 1 // 1 cover + N screenshot pages

            // 1. Render Cover Page
            val coverPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val coverPage = pdfDoc.startPage(coverPageInfo)
            renderCoverPage(
                context = context,
                canvas = coverPage.canvas,
                screenshots = screenshots,
                totalPages = totalPages
            )
            pdfDoc.finishPage(coverPage)

            // 2. Render each screenshot on its own page
            screenshots.forEachIndexed { index, screenshot ->
                val pageIndex = index + 2
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create()
                val page = pdfDoc.startPage(pageInfo)
                val catName = categoryMap[screenshot.categoryId] ?: "Diğer"
                renderScreenshotPage(
                    context = context,
                    canvas = page.canvas,
                    screenshot = screenshot,
                    categoryName = catName,
                    pageNumber = pageIndex,
                    totalPages = totalPages
                )
                pdfDoc.finishPage(page)
            }

            val exportDir = File(context.cacheDir, "pdf_exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val fileName = "Toplu_Ekran_Goruntuleri_${System.currentTimeMillis()}.pdf"
            val file = File(exportDir, fileName)

            val outputStream = FileOutputStream(file)
            pdfDoc.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDoc.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun renderCoverPage(
        context: Context,
        canvas: Canvas,
        screenshots: List<ScreenshotEntity>,
        totalPages: Int
    ) {
        val paint = Paint().apply { isAntiAlias = true }

        // Top Accent Banner
        paint.color = Color.parseColor("#4F46E5")
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 130f, paint)

        // Title on Banner
        paint.color = Color.WHITE
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("Screenshot Organizer Raporu", MARGIN, 55f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#E0E7FF")
        val dateStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("tr")).format(Date())
        canvas.drawText("Oluşturulma Tarihi: $dateStr", MARGIN, 80f, paint)
        canvas.drawText("Toplam Dışa Aktarılan Görsel: ${screenshots.size} adet", MARGIN, 100f, paint)

        var y = 160f

        // Overview Box
        paint.color = Color.parseColor("#F3F4F6")
        canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 100f, 12f, 12f, paint)

        paint.color = Color.parseColor("#1F2937")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Rapor Özeti & İstatistikler", MARGIN + 14f, y + 25f, paint)

        paint.textSize = 11f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#4B5563")

        val sensitiveCount = screenshots.count { it.isSensitive }
        val favoriteCount = screenshots.count { it.isFavorite }
        val categoryCount = screenshots.map { it.categoryId }.distinct().size

        canvas.drawText("• Toplanmış Kategori Sayısı: $categoryCount farklı kategori", MARGIN + 14f, y + 46f, paint)
        canvas.drawText("• Favori Görseller: $favoriteCount adet", MARGIN + 14f, y + 64f, paint)
        canvas.drawText("• Hassas Veri İçeren: $sensitiveCount adet", MARGIN + 14f, y + 82f, paint)

        y += 130f

        // Table of Contents Header
        paint.color = Color.parseColor("#111827")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("İçindekiler Listesi", MARGIN, y, paint)

        y += 18f
        paint.color = Color.parseColor("#E5E7EB")
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint)

        y += 18f

        // Table of items
        screenshots.take(15).forEachIndexed { i, item ->
            paint.color = Color.parseColor("#374151")
            paint.textSize = 10f
            paint.isFakeBoldText = false

            val indexStr = "${i + 1}."
            val itemTitle = if (item.title.length > 55) item.title.take(52) + "..." else item.title
            val targetPage = "Sayfa ${i + 2}"

            canvas.drawText(indexStr, MARGIN, y, paint)
            canvas.drawText(itemTitle, MARGIN + 22f, y, paint)

            paint.color = Color.parseColor("#6B7280")
            val pageStrWidth = paint.measureText(targetPage)
            canvas.drawText(targetPage, PAGE_WIDTH - MARGIN - pageStrWidth, y, paint)

            y += 22f
            if (y > PAGE_HEIGHT - 60f) return@forEachIndexed
        }

        if (screenshots.size > 15) {
            paint.color = Color.parseColor("#9CA3AF")
            canvas.drawText("... ve ${screenshots.size - 15} görsel daha sonraki sayfalarda yer almaktadır.", MARGIN, y + 6f, paint)
        }

        // Footer
        renderFooter(canvas, 1, totalPages)
    }

    private fun renderScreenshotPage(
        context: Context,
        canvas: Canvas,
        screenshot: ScreenshotEntity,
        categoryName: String,
        pageNumber: Int,
        totalPages: Int
    ) {
        val paint = Paint().apply { isAntiAlias = true }

        // Top Header Line
        paint.color = Color.parseColor("#4F46E5")
        canvas.drawRect(MARGIN, 24f, MARGIN + 4f, 54f, paint)

        paint.color = Color.parseColor("#111827")
        paint.textSize = 15f
        paint.isFakeBoldText = true
        val truncatedTitle = if (screenshot.title.length > 45) screenshot.title.take(42) + "..." else screenshot.title
        canvas.drawText(truncatedTitle, MARGIN + 12f, 42f, paint)

        // Category Badge
        paint.color = Color.parseColor("#EEF2FF")
        val badgeWidth = paint.measureText(categoryName) + 16f
        canvas.drawRoundRect(PAGE_WIDTH - MARGIN - badgeWidth, 28f, PAGE_WIDTH - MARGIN, 50f, 6f, 6f, paint)
        paint.color = Color.parseColor("#4F46E5")
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas.drawText(categoryName, PAGE_WIDTH - MARGIN - badgeWidth + 8f, 43f, paint)

        var y = 70f

        // Screenshot Image Container
        val imgWidth = PAGE_WIDTH - (MARGIN * 2)
        val imgHeight = 260f

        paint.color = Color.parseColor("#F9FAFB")
        canvas.drawRoundRect(MARGIN, y, MARGIN + imgWidth, y + imgHeight, 10f, 10f, paint)

        paint.color = Color.parseColor("#E5E7EB")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(MARGIN, y, MARGIN + imgWidth, y + imgHeight, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        // Load & Draw Bitmap
        val bitmap = loadBitmapForScreenshot(context, screenshot.uri)
        if (bitmap != null) {
            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
            // Aspect fit
            val scale = minOf(imgWidth / bitmap.width.toFloat(), imgHeight / bitmap.height.toFloat())
            val drawW = bitmap.width * scale
            val drawH = bitmap.height * scale
            val left = MARGIN + (imgWidth - drawW) / 2f
            val top = y + (imgHeight - drawH) / 2f
            val dstRect = RectF(left, top, left + drawW, top + drawH)
            canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        } else {
            paint.color = Color.parseColor("#9CA3AF")
            paint.textSize = 12f
            val placeholder = "[ Görsel: ${screenshot.fileName} ]"
            val textW = paint.measureText(placeholder)
            canvas.drawText(placeholder, MARGIN + (imgWidth - textW) / 2f, y + (imgHeight / 2f), paint)
        }

        y += imgHeight + 16f

        // Metadata Table
        paint.color = Color.parseColor("#F3F4F6")
        canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 115f, 10f, 10f, paint)

        paint.color = Color.parseColor("#1F2937")
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("Tespit Edilen Önemli Bilgiler", MARGIN + 12f, y + 20f, paint)

        val metaFields = listOfNotNull(
            screenshot.company?.let { "Firma / Kurum:" to it },
            screenshot.amount?.let { "Tutar:" to it },
            screenshot.dateStr?.let { "Tarih / Vade:" to it },
            screenshot.invoiceNumber?.let { "Fatura / Ref No:" to it },
            screenshot.trackingNumber?.let { "Takip Kodu:" to it },
            screenshot.phoneNumber?.let { "İletişim:" to it }
        )

        var metaY = y + 38f
        paint.textSize = 9.5f
        if (metaFields.isEmpty()) {
            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#6B7280")
            canvas.drawText("Özel alan tespit edilmedi. (Genel Ekran Görüntüsü)", MARGIN + 12f, metaY, paint)
        } else {
            metaFields.take(4).forEach { (label, value) ->
                paint.isFakeBoldText = true
                paint.color = Color.parseColor("#4B5563")
                canvas.drawText(label, MARGIN + 12f, metaY, paint)

                paint.isFakeBoldText = false
                paint.color = Color.parseColor("#111827")
                canvas.drawText(value, MARGIN + 110f, metaY, paint)

                metaY += 18f
            }
        }

        y += 128f

        // User Notes (if present)
        if (!screenshot.userNotes.isNullOrBlank()) {
            paint.color = Color.parseColor("#FEF3C7")
            canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 45f, 8f, 8f, paint)

            paint.color = Color.parseColor("#92400E")
            paint.textSize = 9.5f
            paint.isFakeBoldText = true
            canvas.drawText("📌 Kişisel Not:", MARGIN + 10f, y + 18f, paint)

            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#78350F")
            val noteText = if (screenshot.userNotes.length > 80) screenshot.userNotes.take(77) + "..." else screenshot.userNotes
            canvas.drawText(noteText, MARGIN + 10f, y + 34f, paint)

            y += 55f
        }

        // Summary & OCR Section
        paint.color = Color.parseColor("#111827")
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("Yapay Zekâ Özeti & OCR Metni", MARGIN, y, paint)

        y += 14f

        if (screenshot.summary.isNotBlank()) {
            paint.color = Color.parseColor("#374151")
            paint.textSize = 9.5f
            paint.isFakeBoldText = false
            val wrappedSummary = wrapText(screenshot.summary, paint, PAGE_WIDTH - (MARGIN * 2))
            wrappedSummary.take(2).forEach { line ->
                canvas.drawText(line, MARGIN, y, paint)
                y += 14f
            }
            y += 4f
        }

        if (screenshot.ocrText.isNotBlank()) {
            paint.color = Color.parseColor("#F9FAFB")
            val ocrBoxHeight = minOf(140f, PAGE_HEIGHT - y - 50f)
            if (ocrBoxHeight > 40f) {
                canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + ocrBoxHeight, 8f, 8f, paint)
                paint.color = Color.parseColor("#6B7280")
                paint.textSize = 8.5f
                paint.isFakeBoldText = false

                val wrappedOcr = wrapText(screenshot.ocrText, paint, PAGE_WIDTH - (MARGIN * 2) - 16f)
                var ocrY = y + 16f
                wrappedOcr.take((ocrBoxHeight / 14f).toInt() - 1).forEach { line ->
                    canvas.drawText(line, MARGIN + 8f, ocrY, paint)
                    ocrY += 13f
                }
            }
        }

        // Footer
        renderFooter(canvas, pageNumber, totalPages)
    }

    private fun renderFooter(canvas: Canvas, pageNumber: Int, totalPages: Int) {
        val paint = Paint().apply { isAntiAlias = true }
        paint.color = Color.parseColor("#D1D5DB")
        canvas.drawLine(MARGIN, PAGE_HEIGHT - 32f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 32f, paint)

        paint.color = Color.parseColor("#9CA3AF")
        paint.textSize = 8.5f
        paint.isFakeBoldText = false
        canvas.drawText("Screenshot Organizer • Gizli ve Güvenli Arşivleme", MARGIN, PAGE_HEIGHT - 18f, paint)

        val pageStr = "Sayfa $pageNumber / $totalPages"
        val pageStrW = paint.measureText(pageStr)
        canvas.drawText(pageStr, PAGE_WIDTH - MARGIN - pageStrW, PAGE_HEIGHT - 18f, paint)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.replace("\n", " ").split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    private fun loadBitmapForScreenshot(context: Context, uriString: String): Bitmap? {
        return try {
            if (uriString.startsWith("demo://")) {
                // Generate a lightweight placeholder bitmap for demo item
                val b = Bitmap.createBitmap(400, 300, Bitmap.Config.RGB_565)
                val c = Canvas(b)
                c.drawColor(Color.parseColor("#4F46E5"))
                val p = Paint().apply {
                    color = Color.WHITE
                    textSize = 20f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                c.drawText("Screenshot Organizer Demo", 200f, 150f, p)
                b
            } else {
                val uri = Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bmp
            }
        } catch (_: Exception) {
            null
        }
    }
}
