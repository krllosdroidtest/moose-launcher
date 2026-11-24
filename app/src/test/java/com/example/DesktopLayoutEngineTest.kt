package com.example

import com.example.domain.engine.DesktopLayoutEngine
import com.example.domain.engine.FolderAppMembership
import com.example.domain.engine.FolderRecord
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
