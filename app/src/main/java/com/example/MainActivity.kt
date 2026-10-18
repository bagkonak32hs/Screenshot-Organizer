package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.worker.ScreenshotScanWorker
import com.example.ui.components.AnalysisProgressModal
import com.example.ui.navigation.Screen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.cleanup.SmartCleanupScreen
import com.example.ui.screens.collections.CollectionDetailScreen
import com.example.ui.screens.collections.CollectionsScreen
import com.example.ui.screens.detail.ScreenshotDetailScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.landing.LandingScreen
import com.example.ui.screens.lock.AppLockScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.settings.FileNamingSettingsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.stats.StatsScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var screenshotObserver: com.example.data.observer.LiveScreenshotObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val shouldStartScan = intent?.getBooleanExtra(ScreenshotScanWorker.EXTRA_START_SCAN, false) == true ||
                intent?.action == ScreenshotScanWorker.ACTION_ORGANIZE_NOW

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainApp(viewModel = viewModel, startScanFromIntent = shouldStartScan)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (screenshotObserver == null) {
            screenshotObserver = com.example.data.observer.LiveScreenshotObserver(this) { uri, _ ->
                viewModel.importPickedUris(listOf(uri))
            }
        }
        if (viewModel.isLiveObserverEnabled.value) {
            screenshotObserver?.start()
        }
    }

    override fun onStop() {
        super.onStop()
        screenshotObserver?.stop()
    }
}

@Composable
fun MainApp(viewModel: MainViewModel, startScanFromIntent: Boolean = false) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

    val allScreenshots by viewModel.allScreenshots.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val smartCleanupReport by viewModel.smartCleanupReport.collectAsStateWithLifecycle()
    val selectedCleanupIds by viewModel.selectedCleanupIds.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val isAutoScanEnabled by viewModel.isAutoScanEnabled.collectAsStateWithLifecycle()
    val scanIntervalHours by viewModel.scanIntervalHours.collectAsStateWithLifecycle()
    val isWifiOnly by viewModel.isWifiOnly.collectAsStateWithLifecycle()
    val isChargingOnly by viewModel.isChargingOnly.collectAsStateWithLifecycle()
    val lastScanInfo by viewModel.lastScanInfo.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val isOcrInProgress by viewModel.isOcrInProgress.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val isOrganizingFiles by viewModel.isOrganizingFiles.collectAsStateWithLifecycle()
    val orgConfig by viewModel.orgConfig.collectAsStateWithLifecycle()
    val isLiveObserverEnabled by viewModel.isLiveObserverEnabled.collectAsStateWithLifecycle()

    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(startScanFromIntent) {
        if (startScanFromIntent && currentUser != null) {
            viewModel.startFolderScan()
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    // App Lock Gate
    if (isAppLocked && !isUnlocked) {
        AppLockScreen(
            onUnlockSuccess = { /* Handled in viewModel */ },
            verifyPin = { pin -> viewModel.unlockApp(pin) }
        )
        return
    }

    val bottomNavItems = listOf(
        BottomNavItem(
            route = Screen.Home.route,
            title = "Ana Sayfa",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "nav_item_home"
        ),
        BottomNavItem(
            route = Screen.Collections.route,
            title = "Koleksiyonlar",
            selectedIcon = Icons.Filled.CollectionsBookmark,
            unselectedIcon = Icons.Outlined.CollectionsBookmark,
            testTag = "nav_item_collections"
        ),
        BottomNavItem(
            route = Screen.Search.route,
            title = "Ara",
            selectedIcon = Icons.Filled.Search,
            unselectedIcon = Icons.Outlined.Search,
            testTag = "nav_item_search"
        ),
        BottomNavItem(
            route = Screen.Stats.route,
            title = "İstatistikler",
            selectedIcon = Icons.Filled.BarChart,
            unselectedIcon = Icons.Outlined.BarChart,
            testTag = "nav_item_stats"
        ),
        BottomNavItem(
            route = Screen.Settings.route,
            title = "Ayarlar",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            testTag = "nav_item_settings"
        )
    )

    val showBottomBar = currentUser != null && currentRoute in bottomNavItems.map { it.route } && !isSelectionMode

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar && isOnboardingCompleted) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .testTag("main_bottom_nav_bar")
                        .border(
                            width = 0.8.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val initialDestination = if (!isOnboardingCompleted) {
                Screen.Onboarding.route
            } else if (currentUser == null) {
                Screen.Landing.route
            } else {
                Screen.Home.route
            }

            NavHost(
                navController = navController,
                startDestination = initialDestination
            ) {
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        onFinishOnboarding = {
                            viewModel.completeOnboarding()
                            val nextDestination = if (currentUser == null) Screen.Landing.route else Screen.Home.route
                            navController.navigate(nextDestination) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        allScreenshots = allScreenshots,
                        allCategories = allCategories,
                        allReminders = allReminders,
                        onScreenshotClick = { id ->
                            navController.navigate(Screen.ScreenshotDetail.createRoute(id))
                        },
                        onCategoryClick = { catId ->
                            viewModel.setSelectedCategory(catId)
                            navController.navigate(Screen.CollectionDetail.createRoute(catId))
                        },
                        onStartScan = { viewModel.startFolderScan() },
                        onImportUris = { uris -> viewModel.importPickedUris(uris) },
                        onFavoriteToggle = { s -> viewModel.toggleFavorite(s) },
                        onViewAllCollections = {
                            navController.navigate(Screen.Collections.route)
                        },
                        onDeleteScreenshot = { s -> viewModel.deleteScreenshot(s) },
                        currentUser = currentUser,
                        onNavigateToAuth = { navController.navigate(Screen.Auth.route) },
                        isSelectionMode = isSelectionMode,
                        selectedIds = selectedIds,
                        onToggleSelect = { id -> viewModel.toggleSelection(id) },
                        onSelectAll = { viewModel.selectAll(allScreenshots.map { it.id }) },
                        onClearSelection = { viewModel.clearSelection() },
                        onDeleteSelected = { viewModel.deleteSelected() },
                        onMoveSelectedToCategory = { catId -> viewModel.moveSelectedToCategory(catId) },
                        smartCleanupReport = smartCleanupReport,
                        onNavigateToSmartCleanup = { navController.navigate(Screen.SmartCleanup.route) },
                        onQuickDeleteDuplicates = { ids -> viewModel.quickDeleteDuplicates(ids) },
                        onEnterSelectionMode = { viewModel.enterSelectionMode() },
                        onClassifyAllWithGemini = { viewModel.classifyAllScreenshots() }
                    )
                }

                composable(Screen.Collections.route) {
                    CollectionsScreen(
                        categories = allCategories,
                        allScreenshots = allScreenshots,
                        onCategoryClick = { catId ->
                            viewModel.setSelectedCategory(catId)
                            navController.navigate(Screen.CollectionDetail.createRoute(catId))
                        },
                        onCreateCollection = { name, icon, color, desc ->
                            viewModel.createCustomCategory(name, icon, color, desc)
                        }
                    )
                }

                composable(
                    route = Screen.CollectionDetail.route,
                    arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
                    val catScreenshots by viewModel.categoryScreenshots.collectAsStateWithLifecycle()
                    val category = allCategories.find { it.id == categoryId }

                    CollectionDetailScreen(
                        category = category,
                        screenshots = catScreenshots,
                        isSelectionMode = isSelectionMode,
                        selectedIds = selectedIds,
                        allCategories = allCategories,
                        onScreenshotClick = { id ->
                            navController.navigate(Screen.ScreenshotDetail.createRoute(id))
                        },
                        onToggleFavorite = { s -> viewModel.toggleFavorite(s) },
                        onToggleSelect = { id -> viewModel.toggleSelection(id) },
                        onSelectAll = { viewModel.selectAll(catScreenshots.map { it.id }) },
                        onClearSelection = { viewModel.clearSelection() },
                        onDeleteSelected = { viewModel.deleteSelected() },
                        onMoveSelectedToCategory = { catId -> viewModel.moveSelectedToCategory(catId) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.ScreenshotDetail.route,
                    arguments = listOf(navArgument("screenshotId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val screenshotId = backStackEntry.arguments?.getLong("screenshotId") ?: -1L
                    val screenshot = allScreenshots.find { it.id == screenshotId }

                    val categorySuggestions by viewModel.categorySuggestions.collectAsStateWithLifecycle()

                    ScreenshotDetailScreen(
                        screenshot = screenshot,
                        allCategories = allCategories,
                        onBack = { navController.popBackStack() },
                        onCategoryChange = { id, catId -> viewModel.updateCategory(id, catId) },
                        onFavoriteToggle = { s -> viewModel.toggleFavorite(s) },
                        onHiddenToggle = { s -> viewModel.toggleHidden(s) },
                        onUpdateScreenshot = { s -> viewModel.updateScreenshot(s) },
                        onDeleteScreenshot = { s -> viewModel.deleteScreenshot(s) },
                        onReAnalyze = { id -> viewModel.reAnalyze(id) },
                        onReportIncorrect = { s -> viewModel.reportIncorrectCategory(s) },
                        onCreateReminder = { id, title, date -> viewModel.addReminder(id, title, date) },
                        onPerformGeminiOcr = { id -> viewModel.performGeminiOcr(id) },
                        onClassifyWithGemini = { id -> viewModel.classifyWithGemini(id) },
                        onAnalyzeAndSuggest = { id -> viewModel.analyzeAndSuggestCategories(id) },
                        onApplySuggestedCategory = { id, cat -> viewModel.applySuggestedCategory(id, cat) },
                        suggestedCategories = categorySuggestions[screenshotId] ?: (screenshot?.suggestedCategoriesList ?: listOf("Work", "Personal", "Ideas")),
                        isOcrLoading = isOcrInProgress[screenshotId] == true
                    )
                }

                composable(Screen.Search.route) {
                    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
                    val categoryFilter by viewModel.searchCategoryFilter.collectAsStateWithLifecycle()
                    val sensitiveOnly by viewModel.searchSensitiveOnly.collectAsStateWithLifecycle()
                    val favoritesOnly by viewModel.searchFavoritesOnly.collectAsStateWithLifecycle()

                    SearchScreen(
                        query = searchQuery,
                        searchResults = searchResults,
                        allCategories = allCategories,
                        selectedCategoryFilter = categoryFilter,
                        isSensitiveOnly = sensitiveOnly,
                        isFavoritesOnly = favoritesOnly,
                        onQueryChange = { q -> viewModel.setSearchQuery(q) },
                        onCategoryFilterChange = { c -> viewModel.setSearchCategoryFilter(c) },
                        onToggleSensitiveOnly = { viewModel.toggleSearchSensitiveOnly() },
                        onToggleFavoritesOnly = { viewModel.toggleSearchFavoritesOnly() },
                        onClearFilters = { viewModel.clearSearchFilters() },
                        onScreenshotClick = { id ->
                            navController.navigate(Screen.ScreenshotDetail.createRoute(id))
                        }
                    )
                }

                composable(Screen.Stats.route) {
                    StatsScreen(
                        screenshots = allScreenshots,
                        categories = allCategories
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        isAppLocked = isAppLocked,
                        isAutoScanEnabled = isAutoScanEnabled,
                        scanIntervalHours = scanIntervalHours,
                        isWifiOnly = isWifiOnly,
                        isChargingOnly = isChargingOnly,
                        lastScanInfo = lastScanInfo,
                        customApiKey = customApiKey,
                        onSetAppLock = { enabled, pin -> viewModel.setAppLock(enabled, pin) },
                        onSetAutoScan = { viewModel.setAutoScanEnabled(it) },
                        onSetScanInterval = { viewModel.setScanInterval(it) },
                        onSetWifiOnly = { viewModel.setWifiOnly(it) },
                        onSetChargingOnly = { viewModel.setChargingOnly(it) },
                        onTriggerWorkManagerScan = { viewModel.triggerWorkManagerImmediateScan() },
                        onSetCustomApiKey = { viewModel.setCustomApiKey(it) },
                        onClearAllData = { viewModel.clearAllData() },
                        onRemoveDemoData = { viewModel.removeDemoData() },
                        currentUser = currentUser,
                        onNavigateToAuth = { navController.navigate(Screen.Auth.route) },
                        onSignOut = {
                            viewModel.signOut()
                            navController.navigate(Screen.Landing.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        storageStats = storageStats,
                        isOrganizingFiles = isOrganizingFiles,
                        onOrganizeAllFilesPhysically = { viewModel.organizeAllFilesPhysically() },
                        onNavigateToFileNamingSettings = {
                            navController.navigate(Screen.FileNamingSettings.route)
                        },
                        onNavigateToSmartCleanup = {
                            navController.navigate(Screen.SmartCleanup.route)
                        },
                        cleanableCount = smartCleanupReport.totalCleanableCount,
                        themeMode = themeMode,
                        onSetThemeMode = { mode -> viewModel.setThemeMode(mode) },
                        isLiveObserverEnabled = isLiveObserverEnabled,
                        onToggleLiveObserver = { enabled -> viewModel.toggleLiveObserver(enabled) },
                        onTestLiveObserver = { viewModel.testLiveObserver() }
                    )
                }

                composable(Screen.FileNamingSettings.route) {
                    FileNamingSettingsScreen(
                        currentConfig = orgConfig,
                        onSaveConfig = { config ->
                            viewModel.updateOrganizationConfig(config)
                        },
                        onApplyAndReorganize = { config ->
                            viewModel.updateOrganizationConfig(config)
                            viewModel.organizeAllFilesPhysically()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.SmartCleanup.route) {
                    SmartCleanupScreen(
                        report = smartCleanupReport,
                        selectedIds = selectedCleanupIds,
                        allCategories = allCategories,
                        onToggleSelect = { id -> viewModel.toggleCleanupItem(id) },
                        onSelectAll = { ids -> viewModel.selectAllCleanupItems(ids) },
                        onClearSelection = { viewModel.clearCleanupSelection() },
                        onExecuteCleanup = { ids -> viewModel.executeSmartCleanup(ids) },
                        onScreenshotClick = { id ->
                            navController.navigate(Screen.ScreenshotDetail.createRoute(id))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Landing.route) {
                    LandingScreen(
                        onNavigateToAuth = {
                            navController.navigate(Screen.Auth.route)
                        },
                        onQuickGoogleSignIn = { email, name, onSuccess ->
                            viewModel.signInWithGoogle(email, name) {
                                onSuccess()
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Landing.route) { inclusive = true }
                                }
                            }
                        },
                        onLoginSuccess = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Landing.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Auth.route) {
                    AuthScreen(
                        currentUser = currentUser,
                        onSignInWithEmail = { email, pass, onSuccess ->
                            viewModel.signInWithEmail(email, pass) {
                                onSuccess()
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Landing.route) { inclusive = true }
                                }
                            }
                        },
                        onSignUpWithEmail = { name, email, pass, onSuccess ->
                            viewModel.signUpWithEmail(name, email, pass) {
                                onSuccess()
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Landing.route) { inclusive = true }
                                }
                            }
                        },
                        onSignInWithGoogle = { email, name, onSuccess ->
                            viewModel.signInWithGoogle(email, name) {
                                onSuccess()
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Landing.route) { inclusive = true }
                                }
                            }
                        },
                        onSignOut = {
                            viewModel.signOut()
                            navController.navigate(Screen.Landing.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onResetPassword = { email -> viewModel.resetPassword(email) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Global Analysis Progress Modal
            AnalysisProgressModal(
                progressState = scanProgress,
                onDismiss = { viewModel.dismissScanProgress() },
                onViewResults = {
                    viewModel.dismissScanProgress()
                    navController.navigate(Screen.Home.route)
                }
            )
        }
    }
}
