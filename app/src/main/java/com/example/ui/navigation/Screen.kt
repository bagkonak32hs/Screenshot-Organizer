package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Collections : Screen("collections")
    object CollectionDetail : Screen("collection_detail/{categoryId}") {
        fun createRoute(categoryId: String) = "collection_detail/$categoryId"
    }
    object ScreenshotDetail : Screen("screenshot_detail/{screenshotId}") {
        fun createRoute(screenshotId: Long) = "screenshot_detail/$screenshotId"
    }
    object Search : Screen("search")
    object Stats : Screen("stats")
    object Settings : Screen("settings")
    object HiddenVault : Screen("hidden_vault")
    object Auth : Screen("auth")
    object Landing : Screen("landing")
    object FileNamingSettings : Screen("file_naming_settings")
    object SmartCleanup : Screen("smart_cleanup")
}
