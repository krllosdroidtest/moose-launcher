package com.example.ui

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.db.MooseDatabase
import com.example.data.local.prefs.LauncherPreferencesStore
import com.example.data.repository.LauncherRepository
import com.example.data.system.AndroidInstalledAppsProvider
import com.example.data.system.InstalledAppsProvider
import com.example.domain.engine.DesktopLayoutEngine
import com.example.domain.engine.FolderAppMembership
import com.example.domain.engine.FolderRecord
import com.example.domain.model.AppCategory
import com.example.domain.model.DesktopFolder
import com.example.domain.model.DesktopWidgetConfig
import com.example.domain.model.DeviceStatusSnapshot
import com.example.domain.model.ImportStrategy
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.LauncherPreferences
import com.example.domain.model.ThemePreference
import com.example.domain.model.WallpaperStyle
import com.example.domain.model.WidgetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LauncherUiState(
    val isLoading: Boolean = true,
    val preferences: LauncherPreferences = LauncherPreferences(),
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val folders: List<DesktopFolder> = emptyList(),
    val pinnedHomeApps: List<InstalledAppInfo> = emptyList(),
    val dockApps: List<InstalledAppInfo> = emptyList(),
    val pinnedHomePackages: Set<String> = emptySet(),
    val pinnedDockPackages: Set<String> = emptySet(),
    val widgets: List<DesktopWidgetConfig> = emptyList(),
    val quickNoteText: String = "",
    val deviceStatus: DeviceStatusSnapshot = DeviceStatusSnapshot(100, false, 0.0),
    val isDefaultHome: Boolean = false
)

sealed interface UiFeedbackEvent {
    data class Message(val text: String) : UiFeedbackEvent
}

private data class RepoDesktopBundle(
    val preferences: LauncherPreferences,
    val folders: List<FolderRecord>,
    val folderApps: List<FolderAppMembership>,
    val homePins: List<String>,
    val dockPins: List<String>,
    val widgets: List<DesktopWidgetConfig>,
    val quickNote: String
)

class LauncherViewModel(
    application: Application,
    private val repository: LauncherRepository,
    private val appsProvider: InstalledAppsProvider
) : AndroidViewModel(application) {

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val _deviceStatus = MutableStateFlow(DeviceStatusSnapshot(100, false, 0.0))
    private val _isDefaultHome = MutableStateFlow(false)
    private val _isLoadingApps = MutableStateFlow(true)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(AppCategory.ALL)
    val selectedCategory: StateFlow<AppCategory> = _selectedCategory.asStateFlow()

    private val _selectedAppForSheet = MutableStateFlow<InstalledAppInfo?>(null)
    val selectedAppForSheet: StateFlow<InstalledAppInfo?> = _selectedAppForSheet.asStateFlow()

    private val _openedFolderId = MutableStateFlow<String?>(null)
    val openedFolderId: StateFlow<String?> = _openedFolderId.asStateFlow()

    private val _isSpotlightOpen = MutableStateFlow(false)
    val isSpotlightOpen: StateFlow<Boolean> = _isSpotlightOpen.asStateFlow()

    private val _showCreateFolderDialog = MutableStateFlow(false)
    val showCreateFolderDialog: StateFlow<Boolean> = _showCreateFolderDialog.asStateFlow()

    private val _showManageWidgetsDialog = MutableStateFlow(false)
    val showManageWidgetsDialog: StateFlow<Boolean> = _showManageWidgetsDialog.asStateFlow()

    private val _showDesktopMenu = MutableStateFlow(false)
    val showDesktopMenu: StateFlow<Boolean> = _showDesktopMenu.asStateFlow()

    private val _pendingImportJson = MutableStateFlow<String?>(null)
    val pendingImportJson: StateFlow<String?> = _pendingImportJson.asStateFlow()

    private val _feedbackEvents = MutableSharedFlow<UiFeedbackEvent>()
    val feedbackEvents: SharedFlow<UiFeedbackEvent> = _feedbackEvents.asSharedFlow()

    private val repoFirstPart = combine(
        repository.preferences,
        repository.folders,
        repository.folderApps,
        repository.pinnedHomePackages,
        repository.pinnedDockPackages
    ) { prefs, folders, folderApps, homePins, dockPins ->
        RepoDesktopBundle(prefs, folders, folderApps, homePins, dockPins, emptyList(), "")
    }

    private val repoBundleFlow = combine(
        repoFirstPart,
        repository.widgets,
        repository.quickNoteText
    ) { bundle, widgets, note ->
        bundle.copy(widgets = widgets, quickNote = note)
    }

    val uiState: StateFlow<LauncherUiState> = combine(
        repoBundleFlow,
        _installedApps,
        _deviceStatus,
        _isDefaultHome,
        _isLoadingApps
    ) { bundle, apps, deviceStatus, isDefaultHome, isLoading ->
        val folders = DesktopLayoutEngine.resolveFolders(bundle.folders, bundle.folderApps, apps)
        val homePinned = DesktopLayoutEngine.resolveDesktopShortcuts(apps, bundle.homePins)
        val dockApps = DesktopLayoutEngine.resolveManualDock(apps, bundle.dockPins)

        LauncherUiState(
            isLoading = isLoading,
            preferences = bundle.preferences,
            installedApps = apps,
            folders = folders,
            pinnedHomeApps = homePinned,
            dockApps = dockApps,
            pinnedHomePackages = bundle.homePins.toSet(),
            pinnedDockPackages = bundle.dockPins.toSet(),
            widgets = bundle.widgets,
            quickNoteText = bundle.quickNote,
            deviceStatus = deviceStatus,
            isDefaultHome = isDefaultHome
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            refreshEnvironment()
        }
    }

    fun refreshEnvironment() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            _installedApps.value = appsProvider.loadInstalledApps()
            _deviceStatus.value = appsProvider.readDeviceStatus()
            _isDefaultHome.value = appsProvider.isDefaultHomeLauncher()
            _isLoadingApps.value = false
        }
    }

    fun getAppIconBitmap(packageName: String): Bitmap? = appsProvider.getAppIconBitmap(packageName)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedCategory(category: AppCategory) {
        _selectedCategory.value = category
    }

    fun openAppActionSheet(app: InstalledAppInfo) {
        _selectedAppForSheet.value = app
    }

    fun closeAppActionSheet() {
        _selectedAppForSheet.value = null
    }

    fun openFolder(folderId: String) {
        _openedFolderId.value = folderId
        _isSpotlightOpen.value = false
    }

    fun closeFolder() {
        _openedFolderId.value = null
    }

    fun setSpotlightOpen(open: Boolean) {
        _isSpotlightOpen.value = open
        if (!open) _searchQuery.value = ""
    }

    fun setCreateFolderDialogVisible(visible: Boolean) {
        _showCreateFolderDialog.value = visible
    }

    fun setManageWidgetsDialogVisible(visible: Boolean) {
        _showManageWidgetsDialog.value = visible
    }

    fun setDesktopMenuVisible(visible: Boolean) {
        _showDesktopMenu.value = visible
    }

    fun launchApp(app: InstalledAppInfo) {
        viewModelScope.launch {
            val launched = appsProvider.launchApp(app.packageName)
            if (launched) {
                _selectedAppForSheet.value = null
                _isSpotlightOpen.value = false
                _openedFolderId.value = null
            } else {
                val msg = getApplication<Application>().getString(
                    R.string.feedback_app_launch_error,
                    app.label
                )
                _feedbackEvents.emit(UiFeedbackEvent.Message(msg))
            }
        }
    }

    fun openAppInfo(packageName: String) {
        appsProvider.openAppDetailsSettings(packageName)
        _selectedAppForSheet.value = null
    }

    fun openDefaultHomeSettings() {
        appsProvider.openDefaultHomeSettings()
    }

    fun createFolder(name: String, initialAppPackage: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val folderId = repository.createFolder(trimmed)
            if (initialAppPackage != null) {
                repository.toggleAppInFolder(folderId, initialAppPackage)
            }
            _showCreateFolderDialog.value = false
        }
    }

    fun renameFolder(folderId: String, newName: String) {
        viewModelScope.launch { repository.renameFolder(folderId, newName) }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            if (_openedFolderId.value == folderId) {
                _openedFolderId.value = null
            }
            repository.deleteFolder(folderId)
        }
    }

    fun toggleAppInFolder(folderId: String, packageName: String) {
        viewModelScope.launch { repository.toggleAppInFolder(folderId, packageName) }
    }

    fun togglePinHome(packageName: String) {
        viewModelScope.launch { repository.togglePinHome(packageName) }
    }

    fun togglePinDock(packageName: String) {
        viewModelScope.launch { repository.togglePinDock(packageName) }
    }

    fun setWidgetEnabled(type: WidgetType, enabled: Boolean) {
        viewModelScope.launch { repository.setWidgetEnabled(type, enabled) }
    }

    fun saveQuickNote(content: String) {
        viewModelScope.launch { repository.saveQuickNote(content) }
    }

    fun setThemeMode(mode: ThemePreference) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setUseDynamicColor(enabled) }
    }

    fun setWallpaperStyle(style: WallpaperStyle) {
        viewModelScope.launch { repository.setWallpaperStyle(style) }
    }

    fun setShowDockLabels(show: Boolean) {
        viewModelScope.launch { repository.setShowDockLabels(show) }
    }

    fun exportBackupToUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    val json = repository.exportBackupJson()
                    contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                        out.flush()
                    } ?: error("Null output stream")
                }.isSuccess
            }
            val msgRes = if (success) R.string.feedback_export_success else R.string.feedback_export_error
            _feedbackEvents.emit(UiFeedbackEvent.Message(app.getString(msgRes)))
        }
    }

    fun prepareImportFromUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val rawJson = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    }
                }.getOrNull()
            }
            if (rawJson.isNullOrBlank()) {
                _feedbackEvents.emit(
                    UiFeedbackEvent.Message(app.getString(R.string.feedback_import_error))
                )
            } else {
                _pendingImportJson.value = rawJson
            }
        }
    }

    fun confirmImport(strategy: ImportStrategy) {
        val rawJson = _pendingImportJson.value ?: return
        _pendingImportJson.value = null
        viewModelScope.launch {
            val app = getApplication<Application>()
            runCatching {
                repository.importBackupJson(rawJson, strategy)
            }.fold(
                onSuccess = { count ->
                    val msg = when (strategy) {
                        ImportStrategy.MERGE -> app.getString(R.string.feedback_import_merge_success, count)
                        ImportStrategy.REPLACE_ALL -> app.getString(R.string.feedback_import_replace_success, count)
                    }
                    _feedbackEvents.emit(UiFeedbackEvent.Message(msg))
                },
                onFailure = {
                    _feedbackEvents.emit(
                        UiFeedbackEvent.Message(app.getString(R.string.feedback_import_error))
                    )
                }
            )
        }
    }

    fun dismissImportDialog() {
        _pendingImportJson.value = null
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = MooseDatabase.getInstance(application)
            val prefsStore = LauncherPreferencesStore(application)
            val repository = LauncherRepository(db, prefsStore)
            val appsProvider = AndroidInstalledAppsProvider(application)
            return LauncherViewModel(application, repository, appsProvider) as T
        }
    }
}
