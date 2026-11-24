package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.backup.BackupCodec
import com.example.data.backup.BackupFolderAppDto
import com.example.data.backup.BackupFolderDto
import com.example.data.backup.BackupPreferencesDto
import com.example.data.backup.BackupQuickNoteDto
import com.example.data.backup.BackupShortcutDto
import com.example.data.backup.BackupWidgetDto
import com.example.data.backup.MooseBackupPayload
import com.example.data.local.db.DesktopShortcutEntity
import com.example.data.local.db.DesktopWidgetEntity
import com.example.data.local.db.FolderAppEntity
import com.example.data.local.db.FolderEntity
import com.example.data.local.db.MooseDatabase
import com.example.data.local.db.QuickNoteEntity
import com.example.data.local.db.ShortcutSurface
import com.example.data.local.prefs.LauncherPreferencesStore
import com.example.domain.engine.FolderAppMembership
import com.example.domain.engine.FolderRecord
import com.example.domain.model.DesktopWidgetConfig
import com.example.domain.model.ImportStrategy
import com.example.domain.model.LauncherPreferences
import com.example.domain.model.ThemePreference
import com.example.domain.model.WallpaperStyle
import com.example.domain.model.WidgetType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LauncherRepository(
    private val database: MooseDatabase,
    private val preferencesStore: LauncherPreferencesStore
) {
    private val dao = database.mooseDao()

    val preferences: Flow<LauncherPreferences> = preferencesStore.preferences

    val folders: Flow<List<FolderRecord>> = dao.observeFolders().map { list ->
        list.map { FolderRecord(it.id, it.name, it.position) }
    }

    val folderApps: Flow<List<FolderAppMembership>> = dao.observeFolderApps().map { list ->
        list.map { FolderAppMembership(it.folderId, it.packageName, it.position) }
    }

    val pinnedHomePackages: Flow<List<String>> = dao.observeShortcuts().map { list ->
        list.filter { it.surfaceType == ShortcutSurface.HOME }
            .sortedBy { it.position }
            .map { it.packageName }
    }

    val pinnedDockPackages: Flow<List<String>> = dao.observeShortcuts().map { list ->
        list.filter { it.surfaceType == ShortcutSurface.DOCK }
            .sortedBy { it.position }
            .map { it.packageName }
    }

    val widgets: Flow<List<DesktopWidgetConfig>> = dao.observeWidgets().map { list ->
        val source = if (list.isEmpty()) MooseDatabase.defaultWidgets() else list
        source.sortedBy { it.position }.mapNotNull { entity ->
            val type = runCatching { WidgetType.valueOf(entity.widgetType) }.getOrNull()
                ?: return@mapNotNull null
            DesktopWidgetConfig(type = type, enabled = entity.enabled, position = entity.position)
        }
    }

    val quickNoteText: Flow<String> = dao.observeQuickNote().map { it?.content.orEmpty() }

    suspend fun ensureSeeded() {
        if (dao.getAllWidgets().isEmpty()) {
            dao.upsertWidgets(MooseDatabase.defaultWidgets())
        }
    }

    suspend fun createFolder(name: String, nowMillis: Long = System.currentTimeMillis()): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty())
        val pos = dao.getNextFolderPosition()
        val id = "folder_${UUID.randomUUID().toString().take(8)}"
        dao.upsertFolder(
            FolderEntity(
                id = id,
                name = trimmed,
                position = pos,
                createdAt = nowMillis
            )
        )
        return id
    }

    suspend fun renameFolder(folderId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty()) {
            dao.renameFolder(folderId, trimmed)
        }
    }

    suspend fun deleteFolder(folderId: String) {
        dao.deleteFolder(folderId)
    }

    suspend fun toggleAppInFolder(folderId: String, packageName: String) {
        val exists = dao.getAllFolderApps()
            .any { it.folderId == folderId && it.packageName == packageName }
        if (exists) {
            dao.deleteFolderApp(folderId, packageName)
        } else {
            val nextPos = dao.getNextFolderAppPosition(folderId)
            dao.upsertFolderApp(FolderAppEntity(folderId, packageName, nextPos))
        }
    }

    suspend fun togglePinHome(packageName: String) {
        toggleShortcut(packageName, ShortcutSurface.HOME)
    }

    suspend fun togglePinDock(packageName: String) {
        toggleShortcut(packageName, ShortcutSurface.DOCK)
    }

    private suspend fun toggleShortcut(packageName: String, surface: String) {
        val exists = dao.getAllShortcuts()
            .any { it.packageName == packageName && it.surfaceType == surface }
        if (exists) {
            dao.deleteShortcut(packageName, surface)
        } else {
            val nextPos = dao.getNextShortcutPosition(surface)
            dao.upsertShortcut(DesktopShortcutEntity(packageName, surface, nextPos))
        }
    }

    suspend fun setWidgetEnabled(widgetType: WidgetType, enabled: Boolean) {
        ensureSeeded()
        dao.setWidgetEnabled(widgetType.name, enabled)
    }

    suspend fun saveQuickNote(content: String, nowMillis: Long = System.currentTimeMillis()) {
        dao.upsertQuickNote(QuickNoteEntity(content = content, updatedAt = nowMillis))
    }

    suspend fun setThemeMode(mode: ThemePreference) = preferencesStore.setThemeMode(mode)

    suspend fun setUseDynamicColor(enabled: Boolean) = preferencesStore.setUseDynamicColor(enabled)

    suspend fun setWallpaperStyle(style: WallpaperStyle) = preferencesStore.setWallpaperStyle(style)

    suspend fun setShowDockLabels(show: Boolean) = preferencesStore.setShowDockLabels(show)

    suspend fun exportBackupJson(nowMillis: Long = System.currentTimeMillis()): String {
        ensureSeeded()
        val prefs = preferencesStore.getCurrent()
        val folders = dao.getAllFolders()
        val folderApps = dao.getAllFolderApps()
        val shortcuts = dao.getAllShortcuts()
        val widgets = dao.getAllWidgets()
        val quickNote = dao.getQuickNote()

        val payload = MooseBackupPayload(
            schemaVersion = MooseBackupPayload.CURRENT_SCHEMA_VERSION,
            exportedAt = nowMillis,
            preferences = BackupPreferencesDto(
                themeMode = prefs.themeMode.name,
                useDynamicColor = prefs.useDynamicColor,
                wallpaperStyle = prefs.wallpaperStyle.name,
                showDockLabels = prefs.showDockLabels
            ),
            folders = folders.map { BackupFolderDto(it.id, it.name, it.position, it.createdAt) },
            folderApps = folderApps.map { BackupFolderAppDto(it.folderId, it.packageName, it.position) },
            shortcuts = shortcuts.map { BackupShortcutDto(it.packageName, it.surfaceType, it.position) },
            widgets = widgets.map { BackupWidgetDto(it.widgetType, it.enabled, it.position) },
            quickNote = quickNote?.let { BackupQuickNoteDto(it.content, it.updatedAt) }
        )
        return BackupCodec.encode(payload)
    }

    suspend fun importBackupJson(rawJson: String, strategy: ImportStrategy): Int {
        ensureSeeded()
        val payload = BackupCodec.decode(rawJson)

        val folders = payload.folders
            .filter { it.id.isNotBlank() && it.name.isNotBlank() }
            .map { FolderEntity(it.id, it.name.trim(), it.position, it.createdAt) }
        val folderIds = folders.map { it.id }.toHashSet()
        val folderApps = payload.folderApps
            .filter { it.folderId in folderIds }
            .map { FolderAppEntity(it.folderId, it.packageName, it.position) }
        val shortcuts = payload.shortcuts
            .filter { it.surfaceType == ShortcutSurface.HOME || it.surfaceType == ShortcutSurface.DOCK }
            .map { DesktopShortcutEntity(it.packageName, it.surfaceType, it.position) }

        val validWidgetTypes = WidgetType.entries.map { it.name }.toSet()
        val incomingWidgetsMap = payload.widgets
            .filter { it.widgetType in validWidgetTypes }
            .associate { it.widgetType to DesktopWidgetEntity(it.widgetType, it.enabled, it.position) }
            .toMutableMap()
        MooseDatabase.defaultWidgets().forEach { defaultWidget ->
            incomingWidgetsMap.putIfAbsent(defaultWidget.widgetType, defaultWidget)
        }
        val widgets = incomingWidgetsMap.values.sortedBy { it.position }

        val note = payload.quickNote?.let {
            QuickNoteEntity(content = it.content, updatedAt = it.updatedAt)
        }

        val incomingPrefs = LauncherPreferences(
            themeMode = runCatching { ThemePreference.valueOf(payload.preferences.themeMode) }
                .getOrDefault(ThemePreference.SYSTEM),
            useDynamicColor = payload.preferences.useDynamicColor,
            wallpaperStyle = runCatching { WallpaperStyle.valueOf(payload.preferences.wallpaperStyle) }
                .getOrDefault(WallpaperStyle.TONAL),
            showDockLabels = payload.preferences.showDockLabels
        )

        return when (strategy) {
            ImportStrategy.REPLACE_ALL -> {
                database.withTransaction {
                    dao.replaceAllData(folders, folderApps, shortcuts, widgets, note)
                }
                preferencesStore.replaceAll(incomingPrefs)
                folders.size + folderApps.size + shortcuts.size + widgets.size + (if (note != null) 1 else 0)
            }
            ImportStrategy.MERGE -> {
                database.withTransaction {
                    dao.mergeData(folders, folderApps, shortcuts, widgets, note)
                }
            }
        }
    }
}
