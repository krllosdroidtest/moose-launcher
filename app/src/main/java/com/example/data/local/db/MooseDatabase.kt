package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.domain.model.WidgetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object MooseDatabaseMigrations {
    /**
     * Reserved non-destructive migration path from v1 -> v2 preserving all folders,
     * shortcuts, widget configurations, and quick notes.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_desktop_shortcuts_surface_pos ON desktop_shortcuts(surfaceType, position)"
            )
        }
    }

    val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
}

@Database(
    entities = [
        FolderEntity::class,
        FolderAppEntity::class,
        DesktopShortcutEntity::class,
        DesktopWidgetEntity::class,
        QuickNoteEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MooseDatabase : RoomDatabase() {
    abstract fun mooseDao(): MooseDao

    companion object {
        private const val DB_NAME = "moose_desktop_v2.db"

        @Volatile
        private var INSTANCE: MooseDatabase? = null

        fun defaultWidgets(): List<DesktopWidgetEntity> = listOf(
            DesktopWidgetEntity(WidgetType.CLOCK.name, enabled = true, position = 0),
            DesktopWidgetEntity(WidgetType.SYSTEM_STATUS.name, enabled = true, position = 1),
            DesktopWidgetEntity(WidgetType.QUICK_NOTES.name, enabled = true, position = 2)
        )

        fun getInstance(context: Context): MooseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): MooseDatabase {
            return Room.databaseBuilder(
                appContext,
                MooseDatabase::class.java,
                DB_NAME
            )
                .addMigrations(*MooseDatabaseMigrations.ALL_MIGRATIONS)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(appContext).mooseDao().upsertWidgets(defaultWidgets())
                        }
                    }
                })
                .build()
        }
    }
}
