package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScreenshotEntity
import java.util.regex.Pattern

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartActionDeck(
    screenshot: ScreenshotEntity,
    onCreateReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Extract potential URLs from OCR text or fields
    val urlPattern = Pattern.compile("https?://[\\w-]+(\\.[\\w-]+)+(/[^\\s]*)?")
    val urlMatcher = urlPattern.matcher(screenshot.ocrText)
    val extractedUrl = if (urlMatcher.find()) urlMatcher.group() else null

    // Extract IBAN if found in OCR or invoice field
    val ibanPattern = Pattern.compile("TR[0-9]{2}\\s?[0-9]{4}\\s?[0-9]{4}\\s?[0-9]{4}\\s?[0-9]{4}\\s?[0-9]{4}\\s?[0-9]{2}", Pattern.CASE_INSENSITIVE)
    val ibanMatcher = ibanPattern.matcher(screenshot.ocrText + " " + (screenshot.invoiceNumber ?: ""))
    val extractedIban = if (ibanMatcher.find()) ibanMatcher.group() else null

    val hasActions = !screenshot.phoneNumber.isNullOrBlank() ||
            !extractedIban.isNullOrBlank() ||
            !extractedUrl.isNullOrBlank() ||
            !screenshot.address.isNullOrBlank() ||
            !screenshot.trackingNumber.isNullOrBlank() ||
            !screenshot.dateStr.isNullOrBlank() ||
            !screenshot.actionSuggestion.isNullOrBlank()

    if (!hasActions) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_action_deck_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Etkileşimli Akıllı Aksiyonlar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Görselden tespit edilen verilere tek dokunuşla ulaşın",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Phone Call Action
                screenshot.phoneNumber?.let { phone ->
                    ActionButtonChip(
                        icon = Icons.Default.Call,
                        label = "Ara: $phone",
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${phone.replace(" ", "")}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                copyToClipboard(context, "Telefon", phone)
                            }
                        }
                    )
                }

                // 2. IBAN Copy Action
                extractedIban?.let { iban ->
                    ActionButtonChip(
                        icon = Icons.Default.CreditCard,
                        label = "IBAN'ı Kopyala",
                        onClick = {
                            val cleanIban = iban.replace(" ", "")
                            copyToClipboard(context, "IBAN", cleanIban)
                            Toast.makeText(context, "IBAN panoya kopyalandı:\n$cleanIban", Toast.LENGTH_LONG).show()
                        }
                    )
                }

                // 3. Web URL Action
                extractedUrl?.let { url ->
                    val cleanUrl = if (!url.startsWith("http")) "https://$url" else url
                    ActionButtonChip(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        label = "Bağlantıyı Aç",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl))
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                copyToClipboard(context, "URL", cleanUrl)
                            }
                        }
                    )
                }

                // 4. Address Map Action
                screenshot.address?.let { address ->
                    ActionButtonChip(
                        icon = Icons.Default.Place,
                        label = "Haritada Gör",
                        onClick = {
                            val geoUri = Uri.parse("geo:0,0?q=" + Uri.encode(address))
                            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                            try {
                                context.startActivity(mapIntent)
                            } catch (_: Exception) {
                                copyToClipboard(context, "Adres", address)
                            }
                        }
                    )
                }

                // 5. Tracking Number Action
                screenshot.trackingNumber?.let { tracking ->
                    ActionButtonChip(
                        icon = Icons.Default.LocalShipping,
                        label = "Kargo Takip: $tracking",
                        onClick = {
                            copyToClipboard(context, "Takip Kodu", tracking)
                            val searchUrl = "https://www.google.com/search?q=" + Uri.encode("kargo takip $tracking")
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    )
                }

                // 6. Calendar Integration (Google Takvime Ekle)
                ActionButtonChip(
                    icon = Icons.Default.DateRange,
                    label = "Google Takvime Ekle",
                    onClick = {
                        val calIntent = Intent(Intent.ACTION_INSERT).apply {
                            data = CalendarContract.Events.CONTENT_URI
                            putExtra(CalendarContract.Events.TITLE, screenshot.title)
                            putExtra(
                                CalendarContract.Events.DESCRIPTION,
                                "${screenshot.summary}\n\nFirma: ${screenshot.company ?: ""}\nTutar: ${screenshot.amount ?: ""}"
                            )
                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis() + (86_400_000L * 3))
                        }
                        try {
                            context.startActivity(calIntent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Takvim uygulaması açılamadı", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                // 7. In-App Reminder Action
                ActionButtonChip(
                    icon = Icons.Default.Alarm,
                    label = "Hatırlatıcı Kur",
                    onClick = onCreateReminder
                )
            }
        }
    }
}

@Composable
private fun ActionButtonChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label panoya kopyalandı", Toast.LENGTH_SHORT).show()
}
