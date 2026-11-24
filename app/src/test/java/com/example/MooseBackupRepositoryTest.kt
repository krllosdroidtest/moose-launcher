package com.example

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.db.MooseDatabase
import com.example.data.local.prefs.LauncherPreferencesStore
import com.example.data.repository.LauncherRepository
import com.example.domain.engine.GridCoordinateUpdate
import com.example.domain.model.ImportStrategy
import com.example.domain.model.ThemePreference
import com.example.domain.model.WallpaperStyle
import com.example.domain.model.WidgetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MooseBackupRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var database: MooseDatabase
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var prefsStore: LauncherPreferencesStore
    private lateinit var repository: LauncherRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MooseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(scope = dataStoreScope) {
            tempFolder.newFile("test_prefs.preferences_pb")
        }
        prefsStore = LauncherPreferencesStore(dataStore)
        repository = LauncherRepository(database, prefsStore)
    }

    @After
    fun tearDown() {
        database.close()
        dataStoreScope.cancel()
    }

    @Test
    fun `export and import with REPLACE_ALL restores folders grid coords widgets notes shortcuts and preferences`() = runTest {
        repository.ensureSeeded()
        val workFolderId = repository.createFolder("Work", nowMillis = 2_000L)
        repository.toggleAppInFolder(workFolderId, "com.notes")
        repository.togglePinHome("com.files")
        repository.applyGridCoordinateUpdates(
            listOf(
                GridCoordinateUpdate(key = workFolderId, isFolder = true, row = 2, column = 3),
                GridCoordinateUpdate(key = "com.files", isFolder = false, row = 1, column = 1)
            )
        )
        repository.togglePinDock("com.mail")
        repository.setWidgetEnabled(WidgetType.QUICK_NOTES, true)
        repository.setWidgetEnabled(WidgetType.CLOCK, false)
        repository.saveQuickNote("Offline checklist", nowMillis = 5_000L)
        repository.setThemeMode(ThemePreference.DARK)
        repository.setWallpaperStyle(WallpaperStyle.MIDNIGHT_RIDGE)
        repository.setShowDockLabels(true)

        val backupJson = repository.exportBackupJson(nowMillis = 10_000L)

        // Mutate state before Replace All
        repository.deleteFolder(workFolderId)
        repository.togglePinDock("com.mail")
        repository.setWidgetEnabled(WidgetType.QUICK_NOTES, false)
        repository.setWidgetEnabled(WidgetType.CLOCK, true)
        repository.saveQuickNote("Overwritten", nowMillis = 12_000L)
        repository.setThemeMode(ThemePreference.LIGHT)
        repository.setShowDockLabels(false)

        val restoredCount = repository.importBackupJson(backupJson, ImportStrategy.REPLACE_ALL)
        assertTrue(restoredCount > 0)

        val prefs = repository.preferences.first()
        assertEquals(ThemePreference.DARK, prefs.themeMode)
        assertEquals(WallpaperStyle.MIDNIGHT_RIDGE, prefs.wallpaperStyle)
        assertTrue(prefs.showDockLabels)
        val restoredFolder = repository.folders.first().single()
        assertEquals("Work", restoredFolder.name)
        assertEquals(2, restoredFolder.gridRow)
        assertEquals(3, restoredFolder.gridColumn)
        assertEquals(listOf("com.notes"), repository.folderApps.first().map { it.packageName })
        val restoredHome = repository.pinnedHomeRecords.first().single()
        assertEquals("com.files", restoredHome.packageName)
        assertEquals(1, restoredHome.gridRow)
        assertEquals(1, restoredHome.gridColumn)
        assertEquals(listOf("com.mail"), repository.pinnedDockPackages.first())
        assertTrue(repository.widgets.first().first { it.type == WidgetType.QUICK_NOTES }.enabled)
        assertFalse(repository.widgets.first().first { it.type == WidgetType.CLOCK }.enabled)
        assertEquals("Offline checklist", repository.quickNoteText.first())
    }

    @Test
    fun `external widgets crud persists resize and removal`() = runTest {
        repository.addExternalWidget(
            appWidgetId = 77,
            providerPackage = "com.example.clock",
            providerLabel = "Analog Clock",
            spanColumns = 4,
            heightDp = 180
        )
        assertEquals(1, repository.externalWidgets.first().size)
        assertEquals(180, repository.externalWidgets.first()[0].heightDp)

        repository.updateExternalWidgetHeight(77, 280)
        assertEquals(280, repository.externalWidgets.first()[0].heightDp)

        repository.removeExternalWidget(77)
        assertTrue(repository.externalWidgets.first().isEmpty())
    }

    @Test
    fun `import with MERGE preserves existing folders pins and merges newer notes`() = runTest {
        repository.ensureSeeded()
        val workId = repository.createFolder("Work", nowMillis = 1_000L)
        repository.toggleAppInFolder(workId, "com.notes")
        repository.togglePinDock("com.mail")
        repository.saveQuickNote("From backup", nowMillis = 5_000L)

        val backupJson = repository.exportBackupJson(nowMillis = 6_000L)

        // Create distinct local state
        val mediaId = repository.createFolder("Media", nowMillis = 7_000L)
        repository.toggleAppInFolder(mediaId, "com.music")
        repository.togglePinDock("com.chat")
        repository.saveQuickNote("Local line", nowMillis = 2_000L)

        // Delete Work locally to simulate restoring missing folder on merge
        repository.deleteFolder(workId)

        repository.importBackupJson(backupJson, ImportStrategy.MERGE)

        val folderNames = repository.folders.first().map { it.name }
        assertTrue(folderNames.contains("Work"))
        assertTrue(folderNames.contains("Media"))

        val dockPins = repository.pinnedDockPackages.first()
        assertTrue(dockPins.contains("com.mail"))
        assertTrue(dockPins.contains("com.chat"))

        val note = repository.quickNoteText.first()
        assertTrue(note.contains("Local line"))
        assertTrue(note.contains("From backup"))
    }
}
