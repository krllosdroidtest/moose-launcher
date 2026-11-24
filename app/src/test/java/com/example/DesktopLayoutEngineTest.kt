package com.example

import com.example.domain.engine.DesktopGridContent
import com.example.domain.engine.DesktopLayoutEngine
import com.example.domain.engine.FolderAppMembership
import com.example.domain.engine.FolderRecord
import com.example.domain.engine.ShortcutRecord
import com.example.domain.model.AppCategory
import com.example.domain.model.InstalledAppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopLayoutEngineTest {

    private val sampleApps = listOf(
        InstalledAppInfo("com.mail", "Mail", AppCategory.COMMUNICATION),
        InstalledAppInfo("com.notes", "Notes", AppCategory.PRODUCTIVITY),
        InstalledAppInfo("com.music", "Music", AppCategory.MEDIA),
        InstalledAppInfo("com.camera", "Camera", AppCategory.MEDIA),
        InstalledAppInfo("com.calc", "Calculator", AppCategory.UTILITIES),
        InstalledAppInfo("com.files", "Files", AppCategory.UTILITIES),
        InstalledAppInfo("com.settings", "Settings", AppCategory.SYSTEM)
    )

    @Test
    fun `resolveManualDock preserves user order filters uninstalled and respects cap`() {
        val dock = DesktopLayoutEngine.resolveManualDock(
            installedApps = sampleApps,
            pinnedDockPackages = listOf(
                "com.music",
                "com.missing.app",
                "com.notes",
                "com.music",
                "com.mail",
                "com.camera",
                "com.calc",
                "com.files",
                "com.settings"
            ),
            maxDockApps = 5
        )

        assertEquals(
            listOf("com.music", "com.notes", "com.mail", "com.camera", "com.calc"),
            dock.map { it.packageName }
        )
    }

    @Test
    fun `resolveFolders maps ordered apps and omits uninstalled packages`() {
        val folders = listOf(
            FolderRecord("f2", "Media", position = 1),
            FolderRecord("f1", "Work", position = 0)
        )
        val memberships = listOf(
            FolderAppMembership("f1", "com.notes", position = 1),
            FolderAppMembership("f1", "com.mail", position = 0),
            FolderAppMembership("f1", "com.deleted", position = 2),
            FolderAppMembership("f2", "com.music", position = 0)
        )

        val resolved = DesktopLayoutEngine.resolveFolders(folders, memberships, sampleApps)
        assertEquals(listOf("f1", "f2"), resolved.map { it.id })
        assertEquals(listOf("com.mail", "com.notes"), resolved[0].apps.map { it.packageName })
        assertEquals(listOf("com.music"), resolved[1].apps.map { it.packageName })
    }

    @Test
    fun `assignGridSlots respects explicit coordinates and falls back on collisions`() {
        val folders = DesktopLayoutEngine.resolveFolders(
            folders = listOf(
                FolderRecord("f1", "Work", position = 0, gridRow = 1, gridColumn = 2),
                FolderRecord("f2", "Media", position = 1, gridRow = -1, gridColumn = -1)
            ),
            memberships = emptyList(),
            installedApps = sampleApps
        )
        val shortcuts = DesktopLayoutEngine.resolveDesktopShortcuts(
            installedApps = sampleApps,
            pinnedHomeRecords = listOf(
                ShortcutRecord("com.mail", position = 0, gridRow = 1, gridColumn = 2), // collides with f1
                ShortcutRecord("com.notes", position = 1, gridRow = 0, gridColumn = 3)
            )
        )

        val slots = DesktopLayoutEngine.assignGridSlots(folders, shortcuts, columns = 4)
        val byKey = slots.associateBy { it.content.itemKey }

        assertEquals(1 to 2, byKey["folder_f1"]!!.let { it.row to it.column })
        assertEquals(0 to 3, byKey["app_com.notes"]!!.let { it.row to it.column })
        // f2 and com.mail auto-fill first open cells (0,0) and (0,1)
        assertEquals(0 to 0, byKey["folder_f2"]!!.let { it.row to it.column })
        assertEquals(0 to 1, byKey["app_com.mail"]!!.let { it.row to it.column })
    }

    @Test
    fun `computeGridMoveUpdates moves to empty slot and swaps when occupied`() {
        val folders = DesktopLayoutEngine.resolveFolders(
            folders = listOf(FolderRecord("f1", "Work", 0, gridRow = 0, gridColumn = 0)),
            memberships = emptyList(),
            installedApps = sampleApps
        )
        val shortcuts = DesktopLayoutEngine.resolveDesktopShortcuts(
            installedApps = sampleApps,
            pinnedHomeRecords = listOf(ShortcutRecord("com.mail", 0, gridRow = 0, gridColumn = 1))
        )
        val slots = DesktopLayoutEngine.assignGridSlots(folders, shortcuts, columns = 4)

        // Move folder to empty (2, 3)
        val moveEmpty = DesktopLayoutEngine.computeGridMoveUpdates(
            currentSlots = slots,
            movingKey = "folder_f1",
            targetRow = 2,
            targetColumn = 3,
            columns = 4
        )
        assertEquals(1, moveEmpty.size)
        assertTrue(moveEmpty[0].isFolder)
        assertEquals(2, moveEmpty[0].row)
        assertEquals(3, moveEmpty[0].column)

        // Move folder onto occupied (0, 1) -> swaps with com.mail
        val swapUpdates = DesktopLayoutEngine.computeGridMoveUpdates(
            currentSlots = slots,
            movingKey = "folder_f1",
            targetRow = 0,
            targetColumn = 1,
            columns = 4
        )
        assertEquals(2, swapUpdates.size)
        val folderUpdate = swapUpdates.first { it.isFolder }
        val appUpdate = swapUpdates.first { !it.isFolder }
        assertEquals(0 to 1, folderUpdate.row to folderUpdate.column)
        assertEquals(0 to 0, appUpdate.row to appUpdate.column)
        assertTrue(slots.first { it.content is DesktopGridContent.AppShortcut }.content.itemKey == "app_com.mail")
    }

    @Test
    fun `searchSpotlight finds folders by folder name or member app and matches apps`() {
        val resolvedFolders = DesktopLayoutEngine.resolveFolders(
            folders = listOf(
                FolderRecord("f1", "Work", 0),
                FolderRecord("f2", "Entertainment", 1)
            ),
            memberships = listOf(
                FolderAppMembership("f1", "com.notes", 0),
                FolderAppMembership("f2", "com.music", 0)
            ),
            installedApps = sampleApps
        )

        val byAppInsideFolder = DesktopLayoutEngine.searchSpotlight("note", resolvedFolders, sampleApps)
        assertEquals(listOf("Work"), byAppInsideFolder.folders.map { it.name })
        assertEquals(listOf("com.notes"), byAppInsideFolder.apps.map { it.packageName })

        val byFolderName = DesktopLayoutEngine.searchSpotlight("entertain", resolvedFolders, sampleApps)
        assertEquals(listOf("Entertainment"), byFolderName.folders.map { it.name })
        assertTrue(byFolderName.apps.isEmpty())
    }
}
