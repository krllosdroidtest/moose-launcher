package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.components.AppActionSheet
import com.example.ui.components.CreateFolderDialog
import com.example.ui.components.FolderModalDialog
import com.example.ui.components.LauncherSubScreenTopBar
import com.example.ui.components.ManageWidgetsDialog
import com.example.ui.components.SpotlightSearchSheet
import com.example.ui.screens.AppLibraryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MooseLauncherTheme

object MooseRoutes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
}

@Composable
fun MooseLauncherApp(
    viewModel: LauncherViewModel,
    homePressTrigger: Int = 0
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedAppForSheet by viewModel.selectedAppForSheet.collectAsStateWithLifecycle()
    val openedFolderId by viewModel.openedFolderId.collectAsStateWithLifecycle()
    val isSpotlightOpen by viewModel.isSpotlightOpen.collectAsStateWithLifecycle()
    val showCreateFolderDialog by viewModel.showCreateFolderDialog.collectAsStateWithLifecycle()
    val showManageWidgetsDialog by viewModel.showManageWidgetsDialog.collectAsStateWithLifecycle()
    val pendingImportJson by viewModel.pendingImportJson.collectAsStateWithLifecycle()

    var pendingFolderInitialApp by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.feedbackEvents.collect { event ->
            when (event) {
                is UiFeedbackEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    MooseLauncherTheme(
        themePreference = uiState.preferences.themeMode,
        dynamicColor = uiState.preferences.useDynamicColor
    ) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route ?: MooseRoutes.HOME

        LaunchedEffect(homePressTrigger) {
            if (homePressTrigger > 0) {
                viewModel.setSpotlightOpen(false)
                viewModel.closeAppActionSheet()
                viewModel.closeFolder()
                viewModel.setCreateFolderDialogVisible(false)
                viewModel.setManageWidgetsDialogVisible(false)
                navController.popBackStack(MooseRoutes.HOME, inclusive = false)
            }
        }

        val openDestination: (String) -> Unit = { route ->
            if (currentRoute != route) {
                navController.navigate(route) {
                    launchSingleTop = true
                }
            }
        }

        val navigateBackToHome: () -> Unit = {
            if (!navController.popBackStack()) {
                navController.navigate(MooseRoutes.HOME) {
                    popUpTo(MooseRoutes.HOME) { inclusive = true }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = MooseRoutes.HOME,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(MooseRoutes.HOME) {
                    HomeScreen(
                        uiState = uiState,
                        getIconBitmap = viewModel::getAppIconBitmap,
                        onOpenSpotlight = { viewModel.setSpotlightOpen(true) },
                        onCreateFolder = {
                            pendingFolderInitialApp = null
                            viewModel.setCreateFolderDialogVisible(true)
                        },
                        onOpenFolder = viewModel::openFolder,
                        onManageWidgets = { viewModel.setManageWidgetsDialogVisible(true) },
                        onOpenLibrary = { openDestination(MooseRoutes.LIBRARY) },
                        onOpenSettings = { openDestination(MooseRoutes.SETTINGS) },
                        onLaunchApp = viewModel::launchApp,
                        onLongPressApp = viewModel::openAppActionSheet,
                        onSaveQuickNote = viewModel::saveQuickNote,
                        onOpenDefaultHomeSettings = viewModel::openDefaultHomeSettings
                    )
                }

                composable(MooseRoutes.LIBRARY) {
                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                        topBar = {
                            LauncherSubScreenTopBar(
                                title = stringResource(R.string.tab_library),
                                onBack = navigateBackToHome,
                                onSpotlightClick = { viewModel.setSpotlightOpen(true) }
                            )
                        }
                    ) { innerPadding ->
                        AppLibraryScreen(
                            uiState = uiState,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            getIconBitmap = viewModel::getAppIconBitmap,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onCategoryChange = viewModel::updateSelectedCategory,
                            onLaunchApp = viewModel::launchApp,
                            onLongPressApp = viewModel::openAppActionSheet,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }

                composable(MooseRoutes.SETTINGS) {
                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                        topBar = {
                            LauncherSubScreenTopBar(
                                title = stringResource(R.string.tab_settings),
                                onBack = navigateBackToHome
                            )
                        }
                    ) { innerPadding ->
                        SettingsScreen(
                            uiState = uiState,
                            pendingImportJson = pendingImportJson,
                            onThemeModeChange = viewModel::setThemeMode,
                            onDynamicColorChange = viewModel::setUseDynamicColor,
                            onWallpaperChange = viewModel::setWallpaperStyle,
                            onShowDockLabelsChange = viewModel::setShowDockLabels,
                            onToggleWidget = viewModel::setWidgetEnabled,
                            onOpenHomeSettings = viewModel::openDefaultHomeSettings,
                            onExportToUri = { uri ->
                                viewModel.exportBackupToUri(uri, context.contentResolver)
                            },
                            onPrepareImportFromUri = { uri ->
                                viewModel.prepareImportFromUri(uri, context.contentResolver)
                            },
                            onConfirmImport = viewModel::confirmImport,
                            onDismissImportDialog = viewModel::dismissImportDialog,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            if (isSpotlightOpen) {
                SpotlightSearchSheet(
                    query = searchQuery,
                    folders = uiState.folders,
                    installedApps = uiState.installedApps,
                    getIconBitmap = viewModel::getAppIconBitmap,
                    onQueryChange = viewModel::updateSearchQuery,
                    onOpenFolder = viewModel::openFolder,
                    onLaunchApp = viewModel::launchApp,
                    onLongPressApp = viewModel::openAppActionSheet,
                    onCreateFolder = {
                        pendingFolderInitialApp = null
                        viewModel.setCreateFolderDialogVisible(true)
                    },
                    onManageWidgets = { viewModel.setManageWidgetsDialogVisible(true) },
                    onOpenSettings = { openDestination(MooseRoutes.SETTINGS) },
                    onDismiss = { viewModel.setSpotlightOpen(false) }
                )
            }

            selectedAppForSheet?.let { app ->
                AppActionSheet(
                    app = app,
                    iconBitmap = viewModel.getAppIconBitmap(app.packageName),
                    isPinnedHome = app.packageName in uiState.pinnedHomePackages,
                    isPinnedDock = app.packageName in uiState.pinnedDockPackages,
                    folders = uiState.folders,
                    onDismiss = viewModel::closeAppActionSheet,
                    onLaunch = { viewModel.launchApp(app) },
                    onTogglePinHome = { viewModel.togglePinHome(app.packageName) },
                    onTogglePinDock = { viewModel.togglePinDock(app.packageName) },
                    onToggleFolderMembership = { folderId ->
                        viewModel.toggleAppInFolder(folderId, app.packageName)
                    },
                    onCreateFolderWithApp = {
                        pendingFolderInitialApp = app.packageName
                        viewModel.closeAppActionSheet()
                        viewModel.setCreateFolderDialogVisible(true)
                    },
                    onOpenAppInfo = { viewModel.openAppInfo(app.packageName) }
                )
            }

            if (showCreateFolderDialog) {
                CreateFolderDialog(
                    onCreate = { folderName ->
                        viewModel.createFolder(folderName, pendingFolderInitialApp)
                        pendingFolderInitialApp = null
                    },
                    onDismiss = {
                        pendingFolderInitialApp = null
                        viewModel.setCreateFolderDialogVisible(false)
                    }
                )
            }

            if (showManageWidgetsDialog) {
                ManageWidgetsDialog(
                    widgets = uiState.widgets,
                    onToggleWidget = viewModel::setWidgetEnabled,
                    onDismiss = { viewModel.setManageWidgetsDialogVisible(false) }
                )
            }

            openedFolderId?.let { folderId ->
                val folder = uiState.folders.firstOrNull { it.id == folderId }
                if (folder != null) {
                    FolderModalDialog(
                        folder = folder,
                        installedApps = uiState.installedApps,
                        getIconBitmap = viewModel::getAppIconBitmap,
                        onLaunchApp = viewModel::launchApp,
                        onLongPressApp = viewModel::openAppActionSheet,
                        onRenameFolder = { newName -> viewModel.renameFolder(folder.id, newName) },
                        onDeleteFolder = { viewModel.deleteFolder(folder.id) },
                        onToggleAppInFolder = { pkg -> viewModel.toggleAppInFolder(folder.id, pkg) },
                        onDismiss = viewModel::closeFolder
                    )
                }
            }
        }
    }
}
