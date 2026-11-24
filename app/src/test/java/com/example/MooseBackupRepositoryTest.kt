package com.example

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.db.MooseDatabase
import com.example.data.local.prefs.LauncherPreferencesStore
import com.example.data.repository.LauncherRepository
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
    fun `export and import with REPLACE_ALL restores folders widgets notes shortcuts and preferences`() = runTest {
        repository.ensureSeeded()
        val workFolderId = repository.createFolder("Work", nowMillis = 2_000L)
        repository.toggleAppInFolder(workFolderId, "com.notes")
        repository.togglePinHome("com.files")
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
        assertEquals(listOf("Work"), repository.folders.first().map { it.name })
        assertEquals(listOf("com.notes"), repository.folderApps.first().map { it.packageName })
        assertEquals(listOf("com.files"), repository.pinnedHomePackages.first())
        assertEquals(listOf("com.mail"), repository.pinnedDockPackages.first())
        assertTrue(repository.widgets.first().first { it.type == WidgetType.QUICK_NOTES }.enabled)
        assertFalse(repository.widgets.first().first { it.type == WidgetType.CLOCK }.enabled)
        assertEquals("Offline checklist", repository.quickNoteText.first())
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
