package com.example.ui

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
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
import com.example.data.system.MooseNotificationListenerService
import com.example.data.system.WallpaperStorage
import com.example.domain.engine.DesktopLayoutEngine
import com.example.domain.engine.FolderAppMembership
import com.example.domain.engine.FolderRecord
import com.example.domain.engine.ShortcutRecord
import com.example.domain.model.AppCategory
import com.example.domain.model.DesktopFolder
import com.example.domain.model.DesktopShortcutItem
import com.example.domain.model.DesktopWidgetConfig
import com.example.domain.model.DeviceStatusSnapshot
import com.example.domain.model.ExternalWidgetConfig
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LauncherUiState(
    val isLoading: Boolean = true,
    val preferences: LauncherPreferences = LauncherPreferences(),
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val folders: List<DesktopFolder> = emptyList(),
    val pinnedHomeItems: List<DesktopShortcutItem> = emptyList(),
    val dockApps: List<InstalledAppInfo> = emptyList(),
    val pinnedHomePackages: Set<String> = emptySet(),
    val pinnedDockPackages: Set<String> = emptySet(),
    val widgets: List<DesktopWidgetConfig> = emptyList(),
    val externalWidgets: List<ExternalWidgetConfig> = emptyList(),
    val quickNoteText: String = "",
    val deviceStatus: DeviceStatusSnapshot = DeviceStatusSnapshot(100, false, 0.0),
    val isDefaultHome: Boolean = false,
    val activeNotificationPackages: Set<String> = emptySet(),
    val isNotificationAccessEnabled: Boolean = false,
    val customWallpaperImage: ImageBitmap? = null
) {
    val pinnedHomeApps: List<InstalledAppInfo>
        get() = pinnedHomeItems.map { it.app }
}

sealed interface UiFeedbackEvent {
    data class Message(val text: String) : UiFeedbackEvent
}

private data class RepoLayoutBundle(
    val preferences: LauncherPreferences,
    val folders: List<FolderRecord>,
    val folderApps: List<FolderAppMembership>,
    val homeRecords: List<ShortcutRecord>,
    val dockPins: List<String>
)

private data class RepoWidgetsBundle(
    val widgets: List<DesktopWidgetConfig>,
    val externalWidgets: List<ExternalWidgetConfig>,
    val quickNote: String
)

private data class SystemEnvBundle(
    val installedApps: List<InstalledAppInfo>,
    val deviceStatus: DeviceStatusSnapshot,
    val isDefaultHome: Boolean,
    val isNotificationAccessEnabled: Boolean,
    val isLoadingApps: Boolean
)

class LauncherViewModel(
    application: Application,
    private val repository: LauncherRepository,
    private val appsProvider: InstalledAppsProvider
) : AndroidViewModel(application) {

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val _deviceStatus = MutableStateFlow(DeviceStatusSnapshot(100, false, 0.0))
    private val _isDefaultHome = MutableStateFlow(false)
    private val _isNotificationAccessEnabled = MutableStateFlow(false)
    private val _isLoadingApps = MutableStateFlow(true)
    private val _customWallpaperImage = MutableStateFlow<ImageBitmap?>(null)

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

    private val _pendingCropBitmap = MutableStateFlow<Bitmap?>(null)
    val pendingCropBitmap: StateFlow<Bitmap?> = _pendingCropBitmap.asStateFlow()

    private val _pendingImportJson = MutableStateFlow<String?>(null)
    val pendingImportJson: StateFlow<String?> = _pendingImportJson.asStateFlow()

    private val _feedbackEvents = MutableSharedFlow<UiFeedbackEvent>()
    val feedbackEvents: SharedFlow<UiFeedbackEvent> = _feedbackEvents.asSharedFlow()

    private val repoLayoutFlow = combine(
        repository.preferences,
        repository.folders,
        repository.folderApps,
        repository.pinnedHomeRecords,
        repository.pinnedDockPackages
    ) { prefs, folders, folderApps, homeRecords, dockPins ->
        RepoLayoutBundle(prefs, folders, folderApps, homeRecords, dockPins)
    }

    private val repoWidgetsFlow = combine(
        repository.widgets,
        repository.externalWidgets,
        repository.quickNoteText
    ) { widgets, externalWidgets, note ->
        RepoWidgetsBundle(widgets, externalWidgets, note)
    }

    private val systemEnvFlow = combine(
        _installedApps,
        _deviceStatus,
        _isDefaultHome,
        _isNotificationAccessEnabled,
        _isLoadingApps
    ) { apps, deviceStatus, isDefaultHome, isNotificationEnabled, isLoading ->
        SystemEnvBundle(apps, deviceStatus, isDefaultHome, isNotificationEnabled, isLoading)
    }

    private val liveOverlaysFlow = combine(
        MooseNotificationListenerService.activeNotificationPackages,
        _customWallpaperImage
    ) { activeNotifs, wallpaperBitmap ->
        activeNotifs to wallpaperBitmap
    }

    val uiState: StateFlow<LauncherUiState> = combine(
        repoLayoutFlow,
        repoWidgetsFlow,
        systemEnvFlow,
        liveOverlaysFlow
    ) { layout, widgetsBundle, env, overlays ->
        val (activeNotifications, customWallpaper) = overlays
        val folders = DesktopLayoutEngine.resolveFolders(
            folders = layout.folders,
            memberships = layout.folderApps,
            installedApps = env.installedApps
        )
        val homePinned = DesktopLayoutEngine.resolveDesktopShortcuts(
            installedApps = env.installedApps,
            pinnedHomeRecords = layout.homeRecords
        )
        val dockApps = DesktopLayoutEngine.resolveManualDock(
            installedApps = env.installedApps,
            pinnedDockPackages = layout.dockPins
        )

        LauncherUiState(
            isLoading = env.isLoadingApps,
            preferences = layout.preferences,
            installedApps = env.installedApps,
            folders = folders,
            pinnedHomeItems = homePinned,
            dockApps = dockApps,
            pinnedHomePackages = layout.homeRecords.map { it.packageName }.toSet(),
            pinnedDockPackages = layout.dockPins.toSet(),
            widgets = widgetsBundle.widgets,
            externalWidgets = widgetsBundle.externalWidgets,
            quickNoteText = widgetsBundle.quickNote,
            deviceStatus = env.deviceStatus,
            isDefaultHome = env.isDefaultHome,
            activeNotificationPackages = activeNotifications,
            isNotificationAccessEnabled = env.isNotificationAccessEnabled,
            customWallpaperImage = customWallpaper
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            reloadInstalledApps(forceRefresh = false)
            onResumeLightweight()
        }

        viewModelScope.launch {
            repository.preferences
                .map { it.wallpaperStyle to it.customWallpaperPath }
                .distinctUntilChanged()
                .collectLatest { (style, path) ->
                    _customWallpaperImage.value = if (style == WallpaperStyle.CUSTOM_PHOTO && path.isNotBlank()) {
                        WallpaperStorage.loadCachedWallpaper(path)
                    } else {
                        null
                    }
                }
        }
    }

    fun reloadInstalledApps(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            if (_installedApps.value.isEmpty()) {
                _isLoadingApps.value = true
            }
            _installedApps.value = appsProvider.loadInstalledApps(forceRefresh = forceRefresh)
            _isLoadingApps.value = false
        }
    }

    /**
     * Fast resume path: updates lightweight flags and device stats off the main thread
     * without re-querying PackageManager or flashing loading state.
     */
    fun onResumeLightweight() {
        viewModelScope.launch {
            val defaultHome = withContext(Dispatchers.IO) { appsProvider.isDefaultHomeLauncher() }
            if (_isDefaultHome.value != defaultHome) {
                _isDefaultHome.value = defaultHome
            }

            val notifEnabled = withContext(Dispatchers.IO) { appsProvider.isNotificationAccessEnabled() }
            if (_isNotificationAccessEnabled.value != notifEnabled) {
                _isNotificationAccessEnabled.value = notifEnabled
            }

            val status = appsProvider.readDeviceStatus()
            if (_deviceStatus.value != status) {
                _deviceStatus.value = status
            }
        }
    }

    fun refreshEnvironment() {
        reloadInstalledApps(forceRefresh = true)
        onResumeLightweight()
    }

    fun getAppIconBitmap(packageName: String): Bitmap? = appsProvider.getAppIconBitmap(packageName)

    fun getAppIconImageBitmap(packageName: String): ImageBitmap? =
        appsProvider.getAppIconImageBitmap(packageName)

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

    fun openNotificationAccessSettings() {
        appsProvider.openNotificationAccessSettings()
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

    fun moveDesktopGridItem(
        movingKey: String,
        targetRow: Int,
        targetColumn: Int,
        columns: Int
    ) {
        viewModelScope.launch {
            val state = uiState.value
            val slots = DesktopLayoutEngine.assignGridSlots(
                folders = state.folders,
                shortcuts = state.pinnedHomeItems,
                columns = columns
            )
            val updates = DesktopLayoutEngine.computeGridMoveUpdates(
                currentSlots = slots,
                movingKey = movingKey,
                targetRow = targetRow,
                targetColumn = targetColumn,
                columns = columns
            )
            repository.applyGridCoordinateUpdates(updates)
        }
    }

    fun setWidgetEnabled(type: WidgetType, enabled: Boolean) {
        viewModelScope.launch { repository.setWidgetEnabled(type, enabled) }
    }

    fun addExternalWidget(
        appWidgetId: Int,
        providerPackage: String,
        providerLabel: String,
        spanColumns: Int = 4,
        heightDp: Int = 180
    ) {
        viewModelScope.launch {
            repository.addExternalWidget(
                appWidgetId = appWidgetId,
                providerPackage = providerPackage,
                providerLabel = providerLabel,
                spanColumns = spanColumns,
                heightDp = heightDp
            )
        }
    }

    fun toggleExternalWidgetHeight(widget: ExternalWidgetConfig) {
        val nextHeight = if (widget.heightDp < 230) 280 else 170
        viewModelScope.launch {
            repository.updateExternalWidgetHeight(widget.appWidgetId, nextHeight)
        }
    }

    fun removeExternalWidget(appWidgetId: Int) {
        viewModelScope.launch {
            repository.removeExternalWidget(appWidgetId)
        }
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

    fun startWallpaperCrop(uri: Uri) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val bitmap = WallpaperStorage.decodePreviewBitmap(app, uri)
            if (bitmap == null) {
                _feedbackEvents.emit(
                    UiFeedbackEvent.Message(app.getString(R.string.feedback_wallpaper_error))
                )
            } else {
                _pendingCropBitmap.value = bitmap
            }
        }
    }

    fun applyCroppedWallpaper(
        viewportWidth: Float,
        viewportHeight: Float,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        applyToSystem: Boolean
    ) {
        val source = _pendingCropBitmap.value ?: return
        _pendingCropBitmap.value = null
        viewModelScope.launch {
            val app = getApplication<Application>()
            val savedPath = WallpaperStorage.cropAndSaveWallpaper(
                context = app,
                sourceBitmap = source,
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                applyToSystem = applyToSystem
            )
            if (savedPath != null) {
                _customWallpaperImage.value = WallpaperStorage.loadCachedWallpaper(savedPath)
                repository.setCustomWallpaper(savedPath)
                _feedbackEvents.emit(
                    UiFeedbackEvent.Message(app.getString(R.string.feedback_wallpaper_updated))
                )
            } else {
                _feedbackEvents.emit(
                    UiFeedbackEvent.Message(app.getString(R.string.feedback_wallpaper_error))
                )
            }
        }
    }

    fun dismissWallpaperCrop() {
        _pendingCropBitmap.value = null
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
