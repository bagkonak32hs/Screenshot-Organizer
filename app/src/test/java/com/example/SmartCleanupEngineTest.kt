package com.example

import com.example.data.cleanup.SmartCleanupEngine
import com.example.data.model.ScreenshotEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartCleanupEngineTest {

    @Test
    fun testDuplicateDetectionByInvoiceNumber() {
        val now = System.currentTimeMillis()
        val s1 = ScreenshotEntity(
            id = 1L,
            uri = "content://media/1",
            fileName = "invoice_1.png",
            title = "Turkcell Fatura",
            summary = "Ekim ayı faturası",
            categoryId = "invoice",
            ocrText = "Turkcell İletişim Hizmetleri Fatura No: FTR-2026-981 Tutar 450 TL",
            invoiceNumber = "FTR-2026-981",
            createdAt = now
        )
        val s2 = ScreenshotEntity(
            id = 2L,
            uri = "content://media/2",
            fileName = "invoice_1_dup.png",
            title = "Turkcell Fatura Kopya",
            summary = "Ekim ayı faturası",
            categoryId = "invoice",
            ocrText = "Turkcell İletişim Hizmetleri Fatura No: FTR-2026-981 Tutar 450 TL",
            invoiceNumber = "FTR-2026-981",
            createdAt = now + 1000L
        )

        val report = SmartCleanupEngine.analyze(listOf(s1, s2))

        assertEquals(1, report.duplicateGroups.size)
        assertEquals(1, report.totalDuplicateCount)
        assertTrue(report.duplicateGroups[0].duplicates.any { it.id == s1.id || it.id == s2.id })
    }

    @Test
    fun testOldOtpDetection() {
        val now = System.currentTimeMillis()
        val twoDaysAgo = now - (2 * 86_400_000L)
        val otpScreenshot = ScreenshotEntity(
            id = 10L,
            uri = "content://media/10",
            fileName = "otp_code.png",
            title = "Banka Güvenlik Kodu",
            summary = "Tek kullanımlık SMS onay kodu",
            categoryId = "other",
            ocrText = "Bankanız onay kodu: 849201. Bu şifre 3 dakika geçerlidir. Kimseyle paylaşmayınız.",
            createdAt = twoDaysAgo
        )

        val report = SmartCleanupEngine.analyze(listOf(otpScreenshot))

        assertEquals(1, report.totalOldCount)
        assertEquals(10L, report.oldScreenshots[0].screenshot.id)
        assertTrue(report.oldScreenshots[0].badgeLabel.contains("OTP"))
    }
}
