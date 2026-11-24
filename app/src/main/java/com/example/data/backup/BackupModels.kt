package com.example.data.backup

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MooseBackupPayload(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val appName: String = "Moose Launcher",
    val exportedAt: Long,
    val preferences: BackupPreferencesDto,
    val folders: List<BackupFolderDto>,
    val folderApps: List<BackupFolderAppDto>,
    val shortcuts: List<BackupShortcutDto>,
    val widgets: List<BackupWidgetDto>,
    val quickNote: BackupQuickNoteDto? = null
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
    }
}

@JsonClass(generateAdapter = true)
data class BackupPreferencesDto(
    val themeMode: String,
    val useDynamicColor: Boolean,
    val wallpaperStyle: String,
    val customWallpaperPath: String = "",
    val showDockLabels: Boolean
)

@JsonClass(generateAdapter = true)
data class BackupFolderDto(
    val id: String,
    val name: String,
    val position: Int,
    val createdAt: Long,
    val gridRow: Int = -1,
    val gridColumn: Int = -1
)

@JsonClass(generateAdapter = true)
data class BackupFolderAppDto(
    val folderId: String,
    val packageName: String,
    val position: Int
)

@JsonClass(generateAdapter = true)
data class BackupShortcutDto(
    val packageName: String,
    val surfaceType: String,
    val position: Int,
    val gridRow: Int = -1,
    val gridColumn: Int = -1
)

@JsonClass(generateAdapter = true)
data class BackupWidgetDto(
    val widgetType: String,
    val enabled: Boolean,
    val position: Int
)

@JsonClass(generateAdapter = true)
data class BackupQuickNoteDto(
    val content: String,
    val updatedAt: Long
)
