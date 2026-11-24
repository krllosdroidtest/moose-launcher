package com.example.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object ShortcutSurface {
    const val HOME = "HOME"
    const val DOCK = "DOCK"
}

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: Int,
    val createdAt: Long
)

@Entity(
    tableName = "folder_apps",
    primaryKeys = ["folderId", "packageName"],
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["folderId"])]
)
data class FolderAppEntity(
    val folderId: String,
    val packageName: String,
    val position: Int
)

@Entity(
    tableName = "desktop_shortcuts",
    primaryKeys = ["packageName", "surfaceType"]
)
data class DesktopShortcutEntity(
    val packageName: String,
    val surfaceType: String,
    val position: Int
)

@Entity(tableName = "desktop_widgets")
data class DesktopWidgetEntity(
    @PrimaryKey val widgetType: String,
    val enabled: Boolean,
    val position: Int
)

@Entity(tableName = "quick_notes")
data class QuickNoteEntity(
    @PrimaryKey val id: String = "default_note",
    val content: String,
    val updatedAt: Long
)
