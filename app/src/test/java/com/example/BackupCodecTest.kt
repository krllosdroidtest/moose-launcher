package com.example

import com.example.data.backup.BackupCodec
import com.example.data.backup.BackupFolderAppDto
import com.example.data.backup.BackupFolderDto
import com.example.data.backup.BackupPreferencesDto
import com.example.data.backup.BackupQuickNoteDto
import com.example.data.backup.BackupShortcutDto
import com.example.data.backup.BackupWidgetDto
import com.example.data.backup.MooseBackupPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupCodecTest {

    @Test
    fun `encode and decode round trip preserves all desktop backup sections`() {
        val payload = MooseBackupPayload(
            schemaVersion = 1,
            exportedAt = 1_732_450_000L,
            preferences = BackupPreferencesDto(
                themeMode = "DARK",
                useDynamicColor = false,
                wallpaperStyle = "MIDNIGHT_RIDGE",
                showDockLabels = true
            ),
            folders = listOf(
                BackupFolderDto("folder_work", "Work", 0, 1_732_440_000L),
                BackupFolderDto("folder_media", "Media", 1, 1_732_441_000L)
            ),
            folderApps = listOf(
                BackupFolderAppDto("folder_work", "com.notes", 0),
                BackupFolderAppDto("folder_media", "com.music", 0)
            ),
            shortcuts = listOf(
                BackupShortcutDto("com.mail", "DOCK", 0),
                BackupShortcutDto("com.files", "HOME", 0)
            ),
            widgets = listOf(
                BackupWidgetDto("CLOCK", true, 0),
                BackupWidgetDto("SYSTEM_STATUS", true, 1),
                BackupWidgetDto("QUICK_NOTES", false, 2)
            ),
            quickNote = BackupQuickNoteDto("Buy milk", 1_732_448_000L)
        )

        val json = BackupCodec.encode(payload)
        val decoded = BackupCodec.decode(json)

        assertEquals(payload, decoded)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decode rejects blank json`() {
        BackupCodec.decode("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decode rejects unsupported future schema version`() {
        val invalidJson = """
            {
              "schemaVersion": 999,
              "appName": "Moose Launcher",
              "exportedAt": 100,
              "preferences": {
                "themeMode": "SYSTEM",
                "useDynamicColor": true,
                "wallpaperStyle": "TONAL",
                "showDockLabels": false
              },
              "folders": [],
              "folderApps": [],
              "shortcuts": [],
              "widgets": []
            }
        """.trimIndent()
        BackupCodec.decode(invalidJson)
    }
}
