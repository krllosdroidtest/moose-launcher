package com.example.domain.engine

import com.example.domain.model.DesktopFolder
import com.example.domain.model.DesktopShortcutItem
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.SpotlightResults

data class FolderRecord(
    val id: String,
    val name: String,
    val position: Int,
    val gridRow: Int = -1,
    val gridColumn: Int = -1
)

data class FolderAppMembership(
    val folderId: String,
    val packageName: String,
    val position: Int
)

data class ShortcutRecord(
    val packageName: String,
    val position: Int,
    val gridRow: Int = -1,
    val gridColumn: Int = -1
)

sealed interface DesktopGridContent {
    val itemKey: String

    data class Folder(val folder: DesktopFolder) : DesktopGridContent {
        override val itemKey: String = "folder_${folder.id}"
    }

    data class AppShortcut(val shortcut: DesktopShortcutItem) : DesktopGridContent {
        override val itemKey: String = "app_${shortcut.app.packageName}"
    }
}

data class DesktopGridSlot(
    val content: DesktopGridContent,
    val row: Int,
    val column: Int
)

data class GridCoordinateUpdate(
    val key: String,
    val isFolder: Boolean,
    val row: Int,
    val column: Int
)

/**
 * Pure JVM-testable engine for resolving manual dock items, desktop folders,
 * free-position grid slots, desktop shortcuts, and Spotlight search results.
 */
object DesktopLayoutEngine {

    const val DEFAULT_MAX_DOCK_APPS = 6

    fun resolveManualDock(
        installedApps: List<InstalledAppInfo>,
        pinnedDockPackages: List<String>,
        maxDockApps: Int = DEFAULT_MAX_DOCK_APPS
    ): List<InstalledAppInfo> {
        val appsByPkg = installedApps.associateBy { it.packageName }
        if (appsByPkg.isEmpty()) return emptyList()

        val seen = HashSet<String>()
        val result = mutableListOf<InstalledAppInfo>()
        for (pkg in pinnedDockPackages) {
            if (result.size >= maxDockApps) break
            val app = appsByPkg[pkg] ?: continue
            if (seen.add(pkg)) {
                result += app
            }
        }
        return result
    }

    fun resolveDesktopShortcuts(
        installedApps: List<InstalledAppInfo>,
        pinnedHomeRecords: List<ShortcutRecord>
    ): List<DesktopShortcutItem> {
        val appsByPkg = installedApps.associateBy { it.packageName }
        val seen = HashSet<String>()
        return pinnedHomeRecords
            .sortedBy { it.position }
            .mapNotNull { record ->
                val app = appsByPkg[record.packageName] ?: return@mapNotNull null
                if (seen.add(record.packageName)) {
                    DesktopShortcutItem(
                        app = app,
                        position = record.position,
                        gridRow = record.gridRow,
                        gridColumn = record.gridColumn
                    )
                } else {
                    null
                }
            }
    }

    fun resolveFolders(
        folders: List<FolderRecord>,
        memberships: List<FolderAppMembership>,
        installedApps: List<InstalledAppInfo>
    ): List<DesktopFolder> {
        val appsByPkg = installedApps.associateBy { it.packageName }
        val appsByFolder = memberships
            .sortedBy { it.position }
            .groupBy { it.folderId }

        return folders
            .sortedBy { it.position }
            .map { folder ->
                val seen = HashSet<String>()
                val folderApps = appsByFolder[folder.id].orEmpty().mapNotNull { member ->
                    val app = appsByPkg[member.packageName] ?: return@mapNotNull null
                    if (seen.add(member.packageName)) app else null
                }
                DesktopFolder(
                    id = folder.id,
                    name = folder.name,
                    position = folder.position,
                    gridRow = folder.gridRow,
                    gridColumn = folder.gridColumn,
                    apps = folderApps
                )
            }
    }

    fun assignGridSlots(
        folders: List<DesktopFolder>,
        shortcuts: List<DesktopShortcutItem>,
        columns: Int
    ): List<DesktopGridSlot> {
        val safeCols = columns.coerceAtLeast(1)
        val occupied = HashSet<Pair<Int, Int>>()
        val result = mutableListOf<DesktopGridSlot>()
        val unpositioned = mutableListOf<DesktopGridContent>()

        folders.forEach { folder ->
            val r = folder.gridRow
            val c = folder.gridColumn
            if (r >= 0 && c in 0 until safeCols && occupied.add(r to c)) {
                result += DesktopGridSlot(DesktopGridContent.Folder(folder), r, c)
            } else {
                unpositioned += DesktopGridContent.Folder(folder)
            }
        }

        shortcuts.forEach { shortcut ->
            val r = shortcut.gridRow
            val c = shortcut.gridColumn
            if (r >= 0 && c in 0 until safeCols && occupied.add(r to c)) {
                result += DesktopGridSlot(DesktopGridContent.AppShortcut(shortcut), r, c)
            } else {
                unpositioned += DesktopGridContent.AppShortcut(shortcut)
            }
        }

        var scanIndex = 0
        for (item in unpositioned) {
            while (true) {
                val r = scanIndex / safeCols
                val c = scanIndex % safeCols
                scanIndex++
                if (occupied.add(r to c)) {
                    result += DesktopGridSlot(item, r, c)
                    break
                }
            }
        }

        return result.sortedWith(compareBy<DesktopGridSlot> { it.row }.thenBy { it.column })
    }

    fun computeGridMoveUpdates(
        currentSlots: List<DesktopGridSlot>,
        movingKey: String,
        targetRow: Int,
        targetColumn: Int,
        columns: Int
    ): List<GridCoordinateUpdate> {
        val safeCols = columns.coerceAtLeast(1)
        val destRow = targetRow.coerceAtLeast(0)
        val destCol = targetColumn.coerceIn(0, safeCols - 1)

        val movingSlot = currentSlots.firstOrNull { it.content.itemKey == movingKey }
            ?: return emptyList()

        if (movingSlot.row == destRow && movingSlot.column == destCol) {
            return emptyList()
        }

        val updates = mutableListOf<GridCoordinateUpdate>()
        val occupant = currentSlots.firstOrNull {
            it.row == destRow && it.column == destCol && it.content.itemKey != movingKey
        }

        updates += toGridUpdate(movingSlot.content, destRow, destCol)
        if (occupant != null) {
            updates += toGridUpdate(occupant.content, movingSlot.row, movingSlot.column)
        }
        return updates
    }

    private fun toGridUpdate(
        content: DesktopGridContent,
        row: Int,
        column: Int
    ): GridCoordinateUpdate = when (content) {
        is DesktopGridContent.Folder -> GridCoordinateUpdate(
            key = content.folder.id,
            isFolder = true,
            row = row,
            column = column
        )
        is DesktopGridContent.AppShortcut -> GridCoordinateUpdate(
            key = content.shortcut.app.packageName,
            isFolder = false,
            row = row,
            column = column
        )
    }

    fun searchSpotlight(
        query: String,
        folders: List<DesktopFolder>,
        installedApps: List<InstalledAppInfo>,
        maxApps: Int = 18
    ): SpotlightResults {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            return SpotlightResults(
                folders = folders.take(4),
                apps = installedApps.take(12)
            )
        }
        val matchedFolders = folders.filter {
            it.name.lowercase().contains(q) ||
                it.apps.any { app -> app.label.lowercase().contains(q) }
        }
        val matchedApps = installedApps.filter {
            it.label.lowercase().contains(q) ||
                it.packageName.lowercase().contains(q)
        }.take(maxApps)

        return SpotlightResults(
            folders = matchedFolders,
            apps = matchedApps
        )
    }
}
