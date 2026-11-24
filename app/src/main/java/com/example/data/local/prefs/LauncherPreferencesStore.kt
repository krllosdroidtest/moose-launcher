package com.example.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.LauncherPreferences
import com.example.domain.model.ThemePreference
import com.example.domain.model.WallpaperStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.mooseDesktopPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "moose_desktop_prefs"
)

class LauncherPreferencesStore(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.mooseDesktopPrefsDataStore)

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val WALLPAPER_STYLE = stringPreferencesKey("wallpaper_style")
        val CUSTOM_WALLPAPER_PATH = stringPreferencesKey("custom_wallpaper_path")
        val SHOW_DOCK_LABELS = booleanPreferencesKey("show_dock_labels")
    }

    val preferences: Flow<LauncherPreferences> = dataStore.data.map { prefs ->
        LauncherPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                ?: ThemePreference.SYSTEM,
            useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: true,
            wallpaperStyle = prefs[Keys.WALLPAPER_STYLE]?.let { runCatching { WallpaperStyle.valueOf(it) }.getOrNull() }
                ?: WallpaperStyle.TONAL,
            customWallpaperPath = prefs[Keys.CUSTOM_WALLPAPER_PATH].orEmpty(),
            showDockLabels = prefs[Keys.SHOW_DOCK_LABELS] ?: false
        )
    }

    suspend fun getCurrent(): LauncherPreferences = preferences.first()

    suspend fun setThemeMode(mode: ThemePreference) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setWallpaperStyle(style: WallpaperStyle) {
        dataStore.edit { it[Keys.WALLPAPER_STYLE] = style.name }
    }

    suspend fun setCustomWallpaper(path: String) {
        dataStore.edit {
            it[Keys.CUSTOM_WALLPAPER_PATH] = path
            it[Keys.WALLPAPER_STYLE] = WallpaperStyle.CUSTOM_PHOTO.name
        }
    }

    suspend fun setShowDockLabels(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_DOCK_LABELS] = show }
    }

    suspend fun replaceAll(incoming: LauncherPreferences) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = incoming.themeMode.name
            prefs[Keys.USE_DYNAMIC_COLOR] = incoming.useDynamicColor
            prefs[Keys.WALLPAPER_STYLE] = incoming.wallpaperStyle.name
            prefs[Keys.CUSTOM_WALLPAPER_PATH] = incoming.customWallpaperPath
            prefs[Keys.SHOW_DOCK_LABELS] = incoming.showDockLabels
        }
    }
}
