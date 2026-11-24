package com.example.domain.model

enum class AppCategory {
    ALL,
    PRODUCTIVITY,
    MEDIA,
    COMMUNICATION,
    UTILITIES,
    SYSTEM
}

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

enum class WallpaperStyle {
    TONAL,
    NORDIC_MIST,
    MIDNIGHT_RIDGE
}

enum class WidgetType {
    CLOCK,
    SYSTEM_STATUS,
    QUICK_NOTES
}

enum class ImportStrategy {
    MERGE,
    REPLACE_ALL
}

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val category: AppCategory = AppCategory.UTILITIES,
    val isSystemApp: Boolean = false
)

data class DesktopFolder(
    val id: String,
    val name: String,
    val position: Int,
    val apps: List<InstalledAppInfo>
)

data class DesktopWidgetConfig(
    val type: WidgetType,
    val enabled: Boolean,
    val position: Int
)

data class DeviceStatusSnapshot(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val freeStorageGb: Double
)

data class LauncherPreferences(
    val themeMode: ThemePreference = ThemePreference.SYSTEM,
    val useDynamicColor: Boolean = true,
    val wallpaperStyle: WallpaperStyle = WallpaperStyle.TONAL,
    val showDockLabels: Boolean = false
)

data class SpotlightResults(
    val folders: List<DesktopFolder>,
    val apps: List<InstalledAppInfo>
)
