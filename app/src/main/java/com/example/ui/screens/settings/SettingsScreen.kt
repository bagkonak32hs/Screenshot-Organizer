package com.example.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.data.auth.UserProfile
import com.example.ui.components.StoragePermissionDialog
import com.example.ui.screens.auth.GoogleGLogo
import com.example.ui.theme.AppThemeMode
import com.example.util.StoragePermissionHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    isAppLocked: Boolean,
    isAutoScanEnabled: Boolean = true,
    scanIntervalHours: Long = 4L,
    isWifiOnly: Boolean = false,
    isChargingOnly: Boolean = false,
    lastScanInfo: Pair<Long, Int> = Pair(0L, 0),
    customApiKey: String = "",
    onSetAppLock: (Boolean, String) -> Unit,
    onSetAutoScan: (Boolean) -> Unit = {},
    onSetScanInterval: (Long) -> Unit = {},
    onSetWifiOnly: (Boolean) -> Unit = {},
    onSetChargingOnly: (Boolean) -> Unit = {},
    onTriggerWorkManagerScan: () -> Unit = {},
    onSetCustomApiKey: (String) -> Unit = {},
    onClearAllData: () -> Unit,
    onRemoveDemoData: () -> Unit,
    currentUser: UserProfile? = null,
    onNavigateToAuth: () -> Unit = {},
    onSignOut: () -> Unit = {},
    storageStats: com.example.data.file.StorageStatsInfo? = null,
    isOrganizingFiles: Boolean = false,
    onOrganizeAllFilesPhysically: () -> Unit = {},
    onNavigateToFileNamingSettings: () -> Unit = {},
    onNavigateToSmartCleanup: () -> Unit = {},
    cleanableCount: Int = 0,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onSetThemeMode: (AppThemeMode) -> Unit = {},
    isLiveObserverEnabled: Boolean = true,
    onToggleLiveObserver: (Boolean) -> Unit = {},
    onTestLiveObserver: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showPremiumDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(customApiKey) }
    var pinInput by remember { mutableStateOf("") }

    // Toggle states for AI
    var ocrEnabled by remember { mutableStateOf(true) }
    var sensitiveWarningsEnabled by remember { mutableStateOf(true) }

    val context = LocalContext.current
    var showStoragePermissionDialog by remember { mutableStateOf(false) }
    var isStoragePermanentlyDenied by remember { mutableStateOf(false) }

    // Storage permission launcher
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val isGranted = permissionsMap.values.any { it }
        if (isGranted) {
            showStoragePermissionDialog = false
            Toast.makeText(context, "Medya okuma izni verildi.", Toast.LENGTH_SHORT).show()
        } else {
            isStoragePermanentlyDenied = true
            showStoragePermissionDialog = true
            Toast.makeText(context, "Ekran görüntülerini taramak için medya izni gereklidir.", Toast.LENGTH_LONG).show()
        }
    }

    // Notification permission launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        Text(
            text = "Ayarlar",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tarama, yapay zekâ, gizlilik ve arşiv tercihlerinizi yönetin.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Account & Authentication Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_account_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (currentUser != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = currentUser.displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentUser.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (currentUser.authProvider == "google") {
                                    GoogleGLogo(modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Google ile Bağlı",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF1967D2),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(
                                        text = "E-posta ile Bağlı",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { showSignOutConfirm = true },
                            modifier = Modifier.testTag("settings_signout_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Çıkış Yap",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Giriş Yapılmadı",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "E-posta veya Google ile giriş yaparak arşivinizi yedekleyin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = onNavigateToAuth,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("settings_login_button")
                        ) {
                            Text("Giriş Yap", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showSignOutConfirm) {
            AlertDialog(
                onDismissRequest = { showSignOutConfirm = false },
                title = { Text("Oturumu Kapat", fontWeight = FontWeight.Bold) },
                text = { Text("Hesabınızdan çıkış yapmak istediğinize emin misiniz? Arşiviniz cihazınızda kalacaktır.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showSignOutConfirm = false
                            onSignOut()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Çıkış Yap")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutConfirm = false }) {
                        Text("İptal")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Premium Promo Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPremiumDialog = true }
                .testTag("settings_premium_banner"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Screenshot Organizer PRO",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Sınırsız AI analizi ve gizli kasa kilidi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 0. Görünüm & Tema Seçici (Dynamic Theme Switcher)
        SettingsSectionHeader(title = "Görünüm & Tema", icon = Icons.Default.Palette)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_theme_switcher_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Uygulama Teması",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Arayüz rengini ve kontrastını tercihlerinize göre anında değiştirin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Three visual theme switcher options
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theme_switcher_options_row"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val themeOptions = listOf(
                        Triple(AppThemeMode.SYSTEM, "Sistem", Icons.Default.BrightnessAuto),
                        Triple(AppThemeMode.LIGHT, "Açık", Icons.Default.LightMode),
                        Triple(AppThemeMode.DARK, "Koyu", Icons.Default.DarkMode)
                    )

                    themeOptions.forEach { (mode, label, icon) ->
                        val isSelected = themeMode == mode
                        val containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        }
                        val borderColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        }
                        val iconColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = containerColor,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                borderColor
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(96.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSetThemeMode(mode) }
                                .testTag("theme_option_${mode.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                        modifier = Modifier.fillMaxSize()
                                    ) {}
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = iconColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                    }
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Explanatory pill of the currently selected theme
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (themeMode) {
                                AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                AppThemeMode.LIGHT -> Icons.Default.LightMode
                                AppThemeMode.DARK -> Icons.Default.DarkMode
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = themeMode.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = themeMode.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 0. Canlı Ekran Görüntüsü Algılayıcı (Real-Time Live Observer)
        SettingsSectionHeader(title = "Anında Algılama (Canlı İzleyici)", icon = Icons.Default.CameraAlt)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().testTag("settings_live_observer_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSwitchRow(
                    title = "Canlı Ekran Görüntüsü Algılayıcı",
                    subtitle = "Yeni bir ekran görüntüsü aldığınızda anında tespit eder, bildirim paneline yapay zekâ ile düzenleme ve hızlı aksiyon uyarısı gönderir",
                    checked = isLiveObserverEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            if (!StoragePermissionHelper.hasStoragePermission(context)) {
                                storagePermissionLauncher.launch(StoragePermissionHelper.getRequiredStoragePermissions())
                            }
                        }
                        onToggleLiveObserver(checked)
                    }
                )

                if (isLiveObserverEnabled) {
                    SettingsDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bildirim ve Algılama Testi",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Titreşimli ve sesli test bildirimi gönderir",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onTestLiveObserver,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("test_live_observer_button")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Et", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Tarama Ayarları (WorkManager background scan)
        SettingsSectionHeader(title = "Otomatik Tarama (WorkManager)", icon = Icons.Default.Sync)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSwitchRow(
                    title = "Periyodik Arka Plan Taraması",
                    subtitle = "WorkManager ile ekran görüntüsü klasörünü düzenli aralıklarla kontrol eder",
                    checked = isAutoScanEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            if (!StoragePermissionHelper.hasStoragePermission(context)) {
                                storagePermissionLauncher.launch(StoragePermissionHelper.getRequiredStoragePermissions())
                            }
                        }
                        onSetAutoScan(checked)
                    }
                )

                if (isAutoScanEnabled) {
                    SettingsDivider()

                    // Scan interval selector
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Tarama Sıklığı",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1L to "1 Saat", 4L to "4 Saat", 12L to "12 Saat", 24L to "24 Saat").forEach { (h, label) ->
                                val isSelected = scanIntervalHours == h
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSetScanInterval(h) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    SettingsDivider()

                    SettingsSwitchRow(
                        title = "Sadece Wi-Fi ile Analiz",
                        subtitle = "Mobil veri tüketimini engellemek için Unmetered ağ şartı",
                        checked = isWifiOnly,
                        onCheckedChange = { onSetWifiOnly(it) }
                    )

                    SettingsDivider()

                    SettingsSwitchRow(
                        title = "Sadece Şarj Sırasında",
                        subtitle = "Pil tasarrufu için cihaz şarja takılıyken çalıştır",
                        checked = isChargingOnly,
                        onCheckedChange = { onSetChargingOnly(it) }
                    )

                    SettingsDivider()

                    // Last Scan status card
                    val lastScanTime = lastScanInfo.first
                    val lastScanCount = lastScanInfo.second
                    val formattedTime = if (lastScanTime > 0) {
                        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr")).format(Date(lastScanTime))
                    } else "Henüz yapılmadı"

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Son WorkManager Taraması",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$formattedTime ($lastScanCount görsel algılandı)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Immediate WorkManager Scan Button
                    Button(
                        onClick = {
                            if (StoragePermissionHelper.hasStoragePermission(context)) {
                                onTriggerWorkManagerScan()
                            } else {
                                isStoragePermanentlyDenied = false
                                showStoragePermissionDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_run_scan_now_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Şimdi Tara (WorkManager Görevi)")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Yapay Zekâ ve OCR Ayarları
        SettingsSectionHeader(title = "Yapay Zekâ ve Gemini Vision", icon = Icons.Default.Psychology)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsClickableRow(
                    title = "Gemini API Anahtarı",
                    subtitle = if (customApiKey.isNotBlank()) "Özel API anahtarı aktif (Bulut Analizi)" else "Sistem / Yerel Sezgisel Motor Aktif",
                    onClick = {
                        apiKeyInput = customApiKey
                        showApiKeyDialog = true
                    }
                )
                SettingsDivider()
                SettingsSwitchRow(
                    title = "Görsel OCR Metin Okuma",
                    subtitle = "Görsellerdeki tüm fatura ve makbuz yazılarını metne döker",
                    checked = ocrEnabled,
                    onCheckedChange = { ocrEnabled = it }
                )
                SettingsDivider()
                SettingsSwitchRow(
                    title = "Hassas Bilgi Uyarıları",
                    subtitle = "IBAN, parola veya kredi kartı bulunan ekranları uyarır",
                    checked = sensitiveWarningsEnabled,
                    onCheckedChange = { sensitiveWarningsEnabled = it }
                )
                SettingsDivider()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Akıllı Otomatik Kategorilendirme & Etiketleme",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ekran görüntüleri Gemini Vision ile analiz edilerek 'Finance', 'Work', 'Inspiration' gibi kategorilere otomatik ayrılır ve semantik etiketler (#Finance, #Work vb.) atanır.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Gizlilik ve Kilit
        SettingsSectionHeader(title = "Gizlilik & Güvenlik", icon = Icons.Default.Security)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSwitchRow(
                    title = "Uygulama PIN Kilidi",
                    subtitle = "Uygulama açılışında 4 haneli PIN kodu ister",
                    checked = isAppLocked,
                    onCheckedChange = { checked ->
                        if (checked) {
                            showPinDialog = true
                        } else {
                            onSetAppLock(false, "")
                        }
                    }
                )
                SettingsDivider()
                SettingsClickableRow(
                    title = "Örnek Demo Verilerini Temizle",
                    subtitle = "Yalnızca kendi gerçek ekran görüntülerinizi görmek için",
                    onClick = onRemoveDemoData
                )
                SettingsDivider()
                SettingsClickableRow(
                    title = "Tüm Arşivi ve Verileri Sıfırla",
                    subtitle = "Tüm analiz sonuçlarını ve koleksiyonları siler",
                    titleColor = Color(0xFFEF4444),
                    onClick = { showClearDataConfirm = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Hakkında & Destek
        SettingsSectionHeader(title = "Hakkında & Destek", icon = Icons.Default.Info)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsClickableRow(
                    title = "Nasıl Çalışır & Genel Bilgi",
                    subtitle = "Yapay zekâ işleme ve otomatik tarama mekanizması",
                    onClick = { showAboutDialog = true }
                )
                SettingsDivider()
                SettingsClickableRow(
                    title = "Gizlilik Politikası (Google Play)",
                    subtitle = "Fotoğraf izinleri, veri güvenliği ve kullanıcı hakları",
                    onClick = { showPrivacyPolicyDialog = true }
                )
                SettingsDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Uygulama Sürümü", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "1.0.0 (Build 2026)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Fiziksel Dosya ve Klasör Yönetimi
        SettingsSectionHeader(title = "Fiziksel Dosya Yönetimi", icon = Icons.Default.Folder)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_physical_file_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cihaz Dosya Sistemi İstifleme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ekran görüntüleri cihazda 'Finance', 'Work', 'Invoice' vb. klasörlere fiziksel olarak taşınır.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Storage stats row
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Klasörlenmiş Görseller",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${storageStats?.totalOrganizedFiles ?: 0} dosya (${storageStats?.formattedSize ?: "0 KB"})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (!storageStats?.categoryCounts.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                storageStats?.categoryCounts?.forEach { (cat, count) ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Text(
                                            text = "$cat: $count",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Dizin: ${storageStats?.rootPath ?: "Pictures/ScreenshotOrganizer/"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Link to Custom Naming & Sub-Folder Structure Screen
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onNavigateToFileNamingSettings)
                        .testTag("settings_custom_naming_row")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "İsimlendirme ve Alt Klasör Yapısı",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Özel şablonlar, {category}/{year_month} hiyerarşisi ve değişkenler",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Trigger organize button
                Button(
                    onClick = onOrganizeAllFilesPhysically,
                    enabled = !isOrganizingFiles,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("organize_storage_now_button")
                ) {
                    if (isOrganizingFiles) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Klasörlere Taşınıyor...", fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tüm Arşivi Fiziksel Klasörlere Taşı", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Akıllı Temizlik Card in Settings
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSmartCleanup() }
                .testTag("settings_smart_cleanup_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Akıllı Temizlik",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (cleanableCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
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
                    Text(
                        text = "Çift çekilmiş fotoğrafları ve süresi dolmuş kodları tespit edip yer açın.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }

    if (showPremiumDialog) {
        PremiumDialog(onDismiss = { showPremiumDialog = false })
    }

    // Set PIN dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("4 Haneli PIN Belirleyin") },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 4) pinInput = it },
                    placeholder = { Text("Örn: 1234") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4) {
                            onSetAppLock(true, pinInput)
                            showPinDialog = false
                            pinInput = ""
                        }
                    },
                    enabled = pinInput.length == 4
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("İptal") }
            }
        )
    }

    // Gemini API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("Gemini API Anahtarı") },
            text = {
                Column {
                    Text(
                        text = "Google AI Studio'dan aldığınız Gemini API anahtarını buraya girebilirsiniz. Girilen anahtar güvenli şekilde kaydedilir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetCustomApiKey(apiKeyInput.trim())
                        showApiKeyDialog = false
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) { Text("İptal") }
            }
        )
    }

    // Clear data confirm dialog
    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text("Tüm Veriler Silinsin mi?") },
            text = { Text("Bu işlem tüm ekran görüntüsü analiz geçmişinizi ve oluşturduğunuz koleksiyonları sıfırlayacaktır.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearDataConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Evet, Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) { Text("İptal") }
            }
        )
    }

    // About dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Screenshot Organizer Hakkında") },
            text = {
                Text(
                    "Ekran görüntüsü karmaşasına son. Her şey artık yerli yerinde.\n\n" +
                            "Screenshot Organizer, telefonunuzdaki ekran görüntülerini yapay zekâ (Gemini Vision) ile analiz ederek fatura, dekont, adres ve konuşmaları otomatik olarak düzenler.\n\n" +
                            "WorkManager arka plan görevi ile cihazın ekran görüntüsü dizinini periyodik olarak tarar ve düzenlenmemiş yeni görseller algılandığında size bildirim gönderir.",
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) { Text("Anladım") }
            }
        )
    }

    // Google Play Privacy Policy Dialog
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = { Text("Gizlilik Politikası & Veri Güvenliği") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "🔒 Cihaz İçi Gizlilik Odaklı Mimari\n" +
                                "Ekran görüntüleriniz varsayılan olarak yalnızca cihazınızın yerel hafızasında saklanır. Sunucularımıza izinsiz yüklenmez.\n\n" +
                                "🖼️ Fotoğraf ve Medya İzni (READ_MEDIA_IMAGES)\n" +
                                "Bu izin yalnızca cihazınızdaki ekran görüntülerini otomatik tespit etmek, küçük resimlerini sunmak ve çift/seri çekimleri temizlemek için kullanılır.\n\n" +
                                "🤖 Gemini Vision AI Güvenliği\n" +
                                "Yalnızca analiz etmeyi seçtiğiniz görseller, içerik türünün belirlenmesi amacıyla uçtan uca şifrelenmiş TLS/HTTPS kanallarıyla güvenli Google Gemini API servisine iletilir.\n\n" +
                                "🚫 Reklam & Takip Yok\n" +
                                "Uygulamamızda hiçbir üçüncü taraf reklam SDK'sı veya kullanıcı takipçisi bulunmamaktadır.\n\n" +
                                "📧 İletişim & Destek:\n" +
                                "bagkonak32hs@gmail.com",
                        lineHeight = 19.sp,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyPolicyDialog = false }) { Text("Kapat") }
            }
        )
    }

    if (showStoragePermissionDialog) {
        StoragePermissionDialog(
            isPermanentlyDenied = isStoragePermanentlyDenied,
            onRequestPermission = {
                storagePermissionLauncher.launch(StoragePermissionHelper.getRequiredStoragePermissions())
            },
            onOpenSettings = {
                showStoragePermissionDialog = false
                StoragePermissionHelper.openAppSettings(context)
            },
            onPickFromGallery = {
                showStoragePermissionDialog = false
            },
            onDismiss = { showStoragePermissionDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsClickableRow(
    title: String,
    subtitle: String,
    titleColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (titleColor != Color.Unspecified) titleColor else MaterialTheme.colorScheme.onSurface
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    )
}
