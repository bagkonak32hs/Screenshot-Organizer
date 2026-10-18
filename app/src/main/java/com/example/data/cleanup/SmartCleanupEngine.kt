package com.example.data.cleanup

import com.example.data.model.ScreenshotEntity
import kotlin.math.abs

data class DuplicateGroup(
    val original: ScreenshotEntity,
    val duplicates: List<ScreenshotEntity>,
    val reason: String,
    val similarityScore: Int = 95,
    val isBurst: Boolean = false
)

data class OldScreenshotItem(
    val screenshot: ScreenshotEntity,
    val reason: String,
    val badgeLabel: String,
    val ageDays: Int
)

data class SmartCleanupReport(
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val oldScreenshots: List<OldScreenshotItem> = emptyList()
) {
    val totalDuplicateCount: Int get() = duplicateGroups.sumOf { it.duplicates.size }
    val totalOldCount: Int get() = oldScreenshots.size
    val totalCleanableCount: Int get() = totalDuplicateCount + totalOldCount
    val allCleanableIds: Set<Long> get() = duplicateGroups.flatMap { it.duplicates.map { d -> d.id } }.toSet() + oldScreenshots.map { it.screenshot.id }.toSet()
    val allDuplicateIds: Set<Long> get() = duplicateGroups.flatMap { it.duplicates.map { d -> d.id } }.toSet()
    val totalBurstCount: Int get() = duplicateGroups.filter { it.isBurst }.sumOf { it.duplicates.size }

    // Assuming average screenshot size is 2.4 MB (2,516,582 bytes)
    val estimatedSavingsBytes: Long get() = totalCleanableCount * 2_516_582L
    val estimatedSavingsMb: Double get() = (estimatedSavingsBytes / (1024.0 * 1024.0))
    val duplicateSavingsMb: Double get() = (totalDuplicateCount * 2_516_582L) / (1024.0 * 1024.0)
}

object SmartCleanupEngine {

    fun analyze(screenshots: List<ScreenshotEntity>): SmartCleanupReport {
        if (screenshots.isEmpty()) return SmartCleanupReport()

        val duplicateGroups = mutableListOf<DuplicateGroup>()
        val processedDuplicateIds = mutableSetOf<Long>()

        // 1. DUPLICATE & BURST DETECTION
        val sortedScreenshots = screenshots.sortedByDescending { it.createdAt }

        for (i in sortedScreenshots.indices) {
            val current = sortedScreenshots[i]
            if (processedDuplicateIds.contains(current.id)) continue

            val duplicatesForCurrent = mutableListOf<ScreenshotEntity>()
            var matchReason = ""
            var bestSimilarity = 95
            var isGroupBurst = false

            for (j in (i + 1) until sortedScreenshots.size) {
                val candidate = sortedScreenshots[j]
                if (processedDuplicateIds.contains(candidate.id)) continue

                val timeDiffMs = abs(current.createdAt - candidate.createdAt)
                val timeDiffSec = (timeDiffMs / 1000).coerceAtLeast(1)

                // Check 1: Identical invoice / order / reference number
                val hasSameInvoice = !current.invoiceNumber.isNullOrBlank() && 
                    current.invoiceNumber.equals(candidate.invoiceNumber, ignoreCase = true)
                val hasSameOrder = !current.orderNumber.isNullOrBlank() && 
                    current.orderNumber.equals(candidate.orderNumber, ignoreCase = true)

                // Check 2: High OCR Text overlap
                val ocrSimScore = calculateOcrSimilarityScore(current.ocrText, candidate.ocrText)

                // Check 3: Short burst shot (<45 seconds apart) with identical category or similar title
                val isShortBurst = timeDiffMs <= 45_000L && (
                    (current.categoryId == candidate.categoryId && current.title.isNotBlank() && current.title.equals(candidate.title, ignoreCase = true)) ||
                    (current.fileName.take(15) == candidate.fileName.take(15)) ||
                    ocrSimScore >= 60
                )

                // Check 4: Same transaction: amount & company
                val isSameTransaction = !current.amount.isNullOrBlank() && 
                    current.amount == candidate.amount &&
                    !current.company.isNullOrBlank() &&
                    current.company.equals(candidate.company, ignoreCase = true)

                // Check 5: Exact identical title and category (if title is meaningful)
                val isSameTitleAndCat = current.title.length > 5 &&
                    current.title.equals(candidate.title, ignoreCase = true) &&
                    current.categoryId == candidate.categoryId

                if (hasSameInvoice) {
                    duplicatesForCurrent.add(candidate)
                    bestSimilarity = 100
                    if (matchReason.isEmpty()) matchReason = "Aynı fatura / işlem numarası (${current.invoiceNumber})"
                } else if (hasSameOrder) {
                    duplicatesForCurrent.add(candidate)
                    bestSimilarity = 100
                    if (matchReason.isEmpty()) matchReason = "Aynı sipariş referansı (${current.orderNumber})"
                } else if (isShortBurst) {
                    duplicatesForCurrent.add(candidate)
                    isGroupBurst = true
                    bestSimilarity = maxOf(bestSimilarity, 96)
                    if (matchReason.isEmpty()) matchReason = "Aralıksız çekilmiş seri (burst) ekran görüntüsü (${timeDiffSec} sn ara)"
                } else if (isSameTransaction) {
                    duplicatesForCurrent.add(candidate)
                    bestSimilarity = maxOf(bestSimilarity, 98)
                    if (matchReason.isEmpty()) matchReason = "Aynı tutar ve kurum kaydı (${current.company} - ${current.amount})"
                } else if (ocrSimScore >= 75) {
                    duplicatesForCurrent.add(candidate)
                    bestSimilarity = maxOf(bestSimilarity, ocrSimScore)
                    if (matchReason.isEmpty()) matchReason = "Özdeş metin ve görsel içeriği (%$ocrSimScore benzerlik)"
                } else if (isSameTitleAndCat) {
                    duplicatesForCurrent.add(candidate)
                    bestSimilarity = maxOf(bestSimilarity, 92)
                    if (matchReason.isEmpty()) matchReason = "Özdeş başlık ve kategori kopyası"
                }
            }

            if (duplicatesForCurrent.isNotEmpty()) {
                // Determine original: prefer favorite or higher confidence / notes / analysis
                val allInGroup = listOf(current) + duplicatesForCurrent
                val original = allInGroup.maxWithOrNull(
                    compareBy<ScreenshotEntity> { it.isFavorite }
                        .thenBy { !it.userNotes.isNullOrBlank() }
                        .thenBy { it.isAnalyzed }
                        .thenBy { it.confidence }
                        .thenBy { it.ocrText.length }
                ) ?: current

                val actualDuplicates = allInGroup.filter { it.id != original.id }
                processedDuplicateIds.add(original.id)
                actualDuplicates.forEach { processedDuplicateIds.add(it.id) }

                duplicateGroups.add(
                    DuplicateGroup(
                        original = original,
                        duplicates = actualDuplicates,
                        reason = matchReason,
                        similarityScore = bestSimilarity,
                        isBurst = isGroupBurst
                    )
                )
            }
        }

        // 2. OLD & STALE SCREENSHOT DETECTION
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L
        val oldScreenshots = mutableListOf<OldScreenshotItem>()

        screenshots.forEach { item ->
            // Skip items that are already marked as duplicates or favorited
            if (processedDuplicateIds.contains(item.id) || item.isFavorite) return@forEach

            val ageDays = ((now - item.createdAt) / dayMs).toInt()
            val text = (item.ocrText + " " + item.title + " " + item.summary).lowercase()

            // A) One-Time Passwords / SMS Verification / 2FA (OTP)
            val isOtpOrVerification = text.contains("doğrulama kodu") ||
                    text.contains("onay kodu") ||
                    text.contains("şifreniz") ||
                    text.contains("tek kullanımlık") ||
                    text.contains("güvenlik kodu") ||
                    text.contains("verification code") ||
                    text.contains("otp") ||
                    text.contains("3 dakika geçerli") ||
                    text.contains("sms şifre")

            // B) Delivered Food / Courier orders older than 14 days
            val isDeliveredOrder = (text.contains("teslim edildi") || text.contains("siparişiniz tamamlandı") ||
                    text.contains("getiryemek") || text.contains("yemeksepeti") || text.contains("trendyol yemek")) && ageDays >= 14

            // C) Old expired boarding passes / events older than 30 days
            val isPastEventOrTravel = (item.categoryId == "travel" || item.categoryId == "reservation") && ageDays >= 30

            // D) General Stale non-favorite screenshots older than 60 days
            val isOldNonFavorite = ageDays >= 60 && !item.isSensitive

            if (isOtpOrVerification && ageDays >= 1) {
                oldScreenshots.add(
                    OldScreenshotItem(
                        screenshot = item,
                        reason = "Süresi dolmuş tek kullanımlık güvenlik kodu (OTP). Artık geçersizdir.",
                        badgeLabel = "Süresi Dolmuş OTP",
                        ageDays = ageDays
                    )
                )
            } else if (isDeliveredOrder) {
                oldScreenshots.add(
                    OldScreenshotItem(
                        screenshot = item,
                        reason = "Teslim edilmiş geçmiş yemek/kurye siparişi. $ageDays gün önce çekilmiş.",
                        badgeLabel = "Eski Sipariş Fişi",
                        ageDays = ageDays
                    )
                )
            } else if (isPastEventOrTravel) {
                oldScreenshots.add(
                    OldScreenshotItem(
                        screenshot = item,
                        reason = "Geçmiş seyahat veya etkinlik kartı. Tarihi geçmiştir.",
                        badgeLabel = "Geçmiş Bilet",
                        ageDays = ageDays
                    )
                )
            } else if (isOldNonFavorite) {
                oldScreenshots.add(
                    OldScreenshotItem(
                        screenshot = item,
                        reason = "$ageDays gün önce kaydedilmiş ve favorilenmemiş arşiv görseli.",
                        badgeLabel = "$ageDays gün önce",
                        ageDays = ageDays
                    )
                )
            }
        }

        return SmartCleanupReport(
            duplicateGroups = duplicateGroups,
            oldScreenshots = oldScreenshots
        )
    }

    private fun calculateOcrSimilarityScore(text1: String, text2: String): Int {
        if (text1.length < 20 || text2.length < 20) return 0
        val words1 = text1.lowercase().split("\\s+".toRegex()).filter { it.length > 3 }.toSet()
        val words2 = text2.lowercase().split("\\s+".toRegex()).filter { it.length > 3 }.toSet()
        if (words1.isEmpty() || words2.isEmpty()) return 0

        val intersection = words1.intersect(words2).size
        val minSize = minOf(words1.size, words2.size)
        val ratio = (intersection.toDouble() / minSize)
        return (ratio * 100).toInt().coerceIn(0, 100)
    }

    fun checkOcrTextSimilarity(text1: String, text2: String): Boolean {
        return calculateOcrSimilarityScore(text1, text2) >= 80
    }
}
