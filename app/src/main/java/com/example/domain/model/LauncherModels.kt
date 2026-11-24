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
    MIDNIGHT_RIDGE,
    CUSTOM_PHOTO
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
    val gridRow: Int = -1,
    val gridColumn: Int = -1,
    val apps: List<InstalledAppInfo>
)

data class DesktopShortcutItem(
    val app: InstalledAppInfo,
    val position: Int,
    val gridRow: Int = -1,
    val gridColumn: Int = -1
)

data class DesktopWidgetConfig(
    val type: WidgetType,
    val enabled: Boolean,
    val position: Int
)

data class ExternalWidgetConfig(
    val appWidgetId: Int,
    val providerPackage: String,
    val providerLabel: String,
    val spanColumns: Int = 4,
    val heightDp: Int = 180,
    val position: Int = 0
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
    val customWallpaperPath: String = "",
    val showDockLabels: Boolean = false
)

data class SpotlightResults(
    val folders: List<DesktopFolder>,
    val apps: List<InstalledAppInfo>
)
