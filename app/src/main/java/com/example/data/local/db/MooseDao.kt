package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MooseDao {

    @Query("SELECT * FROM folders ORDER BY position ASC, createdAt ASC")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY position ASC, createdAt ASC")
    suspend fun getAllFolders(): List<FolderEntity>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM folders")
    suspend fun getNextFolderPosition(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolders(folders: List<FolderEntity>)

    @Query("UPDATE folders SET name = :name WHERE id = :folderId")
    suspend fun renameFolder(folderId: String, name: String)

    @Query("UPDATE folders SET gridRow = :gridRow, gridColumn = :gridColumn WHERE id = :folderId")
    suspend fun updateFolderGridPosition(folderId: String, gridRow: Int, gridColumn: Int)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: String)

    @Query("SELECT * FROM folder_apps ORDER BY position ASC")
    fun observeFolderApps(): Flow<List<FolderAppEntity>>

    @Query("SELECT * FROM folder_apps ORDER BY position ASC")
    suspend fun getAllFolderApps(): List<FolderAppEntity>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM folder_apps WHERE folderId = :folderId")
    suspend fun getNextFolderAppPosition(folderId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolderApp(entry: FolderAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolderApps(entries: List<FolderAppEntity>)

    @Query("DELETE FROM folder_apps WHERE folderId = :folderId AND packageName = :packageName")
    suspend fun deleteFolderApp(folderId: String, packageName: String)

    @Query("SELECT * FROM desktop_shortcuts ORDER BY position ASC")
    fun observeShortcuts(): Flow<List<DesktopShortcutEntity>>

    @Query("SELECT * FROM desktop_shortcuts ORDER BY position ASC")
    suspend fun getAllShortcuts(): List<DesktopShortcutEntity>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM desktop_shortcuts WHERE surfaceType = :surfaceType")
    suspend fun getNextShortcutPosition(surfaceType: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShortcut(shortcut: DesktopShortcutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShortcuts(shortcuts: List<DesktopShortcutEntity>)

    @Query("UPDATE desktop_shortcuts SET gridRow = :gridRow, gridColumn = :gridColumn WHERE packageName = :packageName AND surfaceType = :surfaceType")
    suspend fun updateShortcutGridPosition(packageName: String, surfaceType: String, gridRow: Int, gridColumn: Int)

    @Query("DELETE FROM desktop_shortcuts WHERE packageName = :packageName AND surfaceType = :surfaceType")
    suspend fun deleteShortcut(packageName: String, surfaceType: String)

    @Query("SELECT * FROM desktop_widgets ORDER BY position ASC")
    fun observeWidgets(): Flow<List<DesktopWidgetEntity>>

    @Query("SELECT * FROM desktop_widgets ORDER BY position ASC")
    suspend fun getAllWidgets(): List<DesktopWidgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWidgets(widgets: List<DesktopWidgetEntity>)

    @Query("UPDATE desktop_widgets SET enabled = :enabled WHERE widgetType = :widgetType")
    suspend fun setWidgetEnabled(widgetType: String, enabled: Boolean)

    @Query("SELECT * FROM external_widgets ORDER BY position ASC")
    fun observeExternalWidgets(): Flow<List<ExternalWidgetEntity>>

    @Query("SELECT * FROM external_widgets ORDER BY position ASC")
    suspend fun getAllExternalWidgets(): List<ExternalWidgetEntity>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM external_widgets")
    suspend fun getNextExternalWidgetPosition(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExternalWidget(widget: ExternalWidgetEntity)

    @Query("UPDATE external_widgets SET heightDp = :heightDp WHERE appWidgetId = :appWidgetId")
    suspend fun updateExternalWidgetHeight(appWidgetId: Int, heightDp: Int)

    @Query("DELETE FROM external_widgets WHERE appWidgetId = :appWidgetId")
    suspend fun deleteExternalWidget(appWidgetId: Int)

    @Query("SELECT * FROM quick_notes WHERE id = 'default_note' LIMIT 1")
    fun observeQuickNote(): Flow<QuickNoteEntity?>

    @Query("SELECT * FROM quick_notes WHERE id = 'default_note' LIMIT 1")
    suspend fun getQuickNote(): QuickNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuickNote(note: QuickNoteEntity)

    @Query("DELETE FROM folder_apps")
    suspend fun clearFolderApps()

    @Query("DELETE FROM folders")
    suspend fun clearFolders()

    @Query("DELETE FROM desktop_shortcuts")
    suspend fun clearShortcuts()

    @Query("DELETE FROM desktop_widgets")
    suspend fun clearWidgets()

    @Query("DELETE FROM quick_notes")
    suspend fun clearQuickNotes()

    @Transaction
    suspend fun replaceAllData(
        folders: List<FolderEntity>,
        folderApps: List<FolderAppEntity>,
        shortcuts: List<DesktopShortcutEntity>,
        widgets: List<DesktopWidgetEntity>,
        quickNote: QuickNoteEntity?
    ) {
        clearFolderApps()
        clearFolders()
        clearShortcuts()
        clearWidgets()
        clearQuickNotes()

        upsertFolders(folders)
        val validFolderIds = folders.map { it.id }.toHashSet()
        upsertFolderApps(folderApps.filter { it.folderId in validFolderIds })
        upsertShortcuts(shortcuts)
        upsertWidgets(widgets)
        if (quickNote != null) {
            upsertQuickNote(quickNote)
        }
    }

    @Transaction
    suspend fun mergeData(
        folders: List<FolderEntity>,
        folderApps: List<FolderAppEntity>,
        shortcuts: List<DesktopShortcutEntity>,
        widgets: List<DesktopWidgetEntity>,
        quickNote: QuickNoteEntity?
    ): Int {
        var restored = 0

        val existingFolders = getAllFolders().associateBy { it.id }
        val nextFolderPos = (existingFolders.values.maxOfOrNull { it.position } ?: -1) + 1
        var folderOffset = 0
        val newFolders = folders.filter { it.id !in existingFolders }.map {
            it.copy(position = nextFolderPos + folderOffset++)
        }
        if (newFolders.isNotEmpty()) {
            upsertFolders(newFolders)
            restored += newFolders.size
        }

        val allFolderIds = (existingFolders.keys + folders.map { it.id }).toHashSet()
        val existingFolderApps = getAllFolderApps().associateBy { it.folderId to it.packageName }
        val newFolderApps = mutableListOf<FolderAppEntity>()
        val posByFolder = existingFolderApps.values
            .groupBy { it.folderId }
            .mapValues { (_, items) -> (items.maxOfOrNull { it.position } ?: -1) + 1 }
            .toMutableMap()

        for (entry in folderApps) {
            if (entry.folderId in allFolderIds && (entry.folderId to entry.packageName) !in existingFolderApps) {
                val pos = posByFolder[entry.folderId] ?: 0
                posByFolder[entry.folderId] = pos + 1
                newFolderApps += entry.copy(position = pos)
            }
        }
        if (newFolderApps.isNotEmpty()) {
            upsertFolderApps(newFolderApps)
            restored += newFolderApps.size
        }

        val existingShortcuts = getAllShortcuts().associateBy { it.packageName to it.surfaceType }
        var nextHome = (existingShortcuts.values
            .filter { it.surfaceType == ShortcutSurface.HOME }
            .maxOfOrNull { it.position } ?: -1) + 1
        var nextDock = (existingShortcuts.values
            .filter { it.surfaceType == ShortcutSurface.DOCK }
            .maxOfOrNull { it.position } ?: -1) + 1

        val newShortcuts = mutableListOf<DesktopShortcutEntity>()
        for (shortcut in shortcuts) {
            if ((shortcut.packageName to shortcut.surfaceType) !in existingShortcuts) {
                val pos = if (shortcut.surfaceType == ShortcutSurface.HOME) nextHome++ else nextDock++
                newShortcuts += shortcut.copy(position = pos)
            }
        }
        if (newShortcuts.isNotEmpty()) {
            upsertShortcuts(newShortcuts)
            restored += newShortcuts.size
        }

        val existingWidgets = getAllWidgets().associateBy { it.widgetType }
        val missingWidgets = widgets.filter { it.widgetType !in existingWidgets }
        if (missingWidgets.isNotEmpty()) {
            upsertWidgets(missingWidgets)
            restored += missingWidgets.size
        }

        val existingNote = getQuickNote()
        if (quickNote != null && quickNote.content.isNotBlank()) {
            if (existingNote == null || existingNote.content.isBlank()) {
                upsertQuickNote(quickNote)
                restored += 1
            } else if (quickNote.updatedAt > existingNote.updatedAt && quickNote.content != existingNote.content) {
                upsertQuickNote(
                    existingNote.copy(
                        content = "${existingNote.content}\n${quickNote.content}",
                        updatedAt = quickNote.updatedAt
                    )
                )
                restored += 1
            }
        }

        return restored
    }
}
