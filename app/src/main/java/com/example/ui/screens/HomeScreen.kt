package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.WallpaperStyle
import com.example.ui.LauncherUiState
import com.example.ui.components.AppGridTile
import com.example.ui.components.DesktopFolderTile
import com.example.ui.components.DesktopWidgetsGrid
import com.example.ui.components.ManualFloatingDock
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    uiState: LauncherUiState,
    getIconBitmap: (String) -> Bitmap?,
    onOpenSpotlight: () -> Unit,
    onOpenDesktopMenu: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    onLaunchApp: (InstalledAppInfo) -> Unit,
    onLongPressApp: (InstalledAppInfo) -> Unit,
    onSaveQuickNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(30_000L)
        }
    }

    val haptics = LocalHapticFeedback.current
    val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }

    val launcherSwipeConnection = remember(swipeThresholdPx, onOpenLibrary, onOpenSpotlight) {
        object : NestedScrollConnection {
            private var overscrollY = 0f
            private var triggered = false

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.UserInput || triggered) {
                    return Offset.Zero
                }
                overscrollY += available.y
                if (overscrollY <= -swipeThresholdPx) {
                    triggered = true
                    overscrollY = 0f
                    onOpenLibrary()
                } else if (overscrollY >= swipeThresholdPx) {
                    triggered = true
                    overscrollY = 0f
                    onOpenSpotlight()
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                overscrollY = 0f
                triggered = false
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(launcherSwipeConnection)
            .pointerInput(onOpenDesktopMenu) {
                detectTapGestures(
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenDesktopMenu()
                    }
                )
            }
            .semantics {
                onLongClick {
                    onOpenDesktopMenu()
                    true
                }
            }
            .testTag("desktop_surface")
    ) {
        DesktopBackdrop(style = uiState.preferences.wallpaperStyle)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            val isWide = maxWidth >= 600.dp

            if (isWide) {
                val rightColumns = if (maxWidth >= 960.dp) 6 else 4
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 1200.dp)
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 28.dp, vertical = 20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .weight(0.36f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 124.dp)
                    ) {
                        DesktopWidgetsGrid(
                            widgets = uiState.widgets,
                            nowMillis = nowMillis,
                            deviceStatus = uiState.deviceStatus,
                            quickNoteText = uiState.quickNoteText,
                            onSaveQuickNote = onSaveQuickNote
                        )
                    }

                    DesktopItemsGrid(
                        uiState = uiState,
                        columns = rightColumns,
                        getIconBitmap = getIconBitmap,
                        onOpenFolder = onOpenFolder,
                        onLaunchApp = onLaunchApp,
                        onLongPressApp = onLongPressApp,
                        headerContent = null,
                        modifier = Modifier
                            .weight(0.64f)
                            .fillMaxHeight()
                    )
                }
            } else {
                DesktopItemsGrid(
                    uiState = uiState,
                    columns = 4,
                    getIconBitmap = getIconBitmap,
                    onOpenFolder = onOpenFolder,
                    onLaunchApp = onLaunchApp,
                    onLongPressApp = onLongPressApp,
                    headerContent = {
                        DesktopWidgetsGrid(
                            widgets = uiState.widgets,
                            nowMillis = nowMillis,
                            deviceStatus = uiState.deviceStatus,
                            quickNoteText = uiState.quickNoteText,
                            onSaveQuickNote = onSaveQuickNote,
                            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (uiState.folders.isEmpty() && uiState.pinnedHomeApps.isEmpty() && uiState.dockApps.isEmpty()) {
                Text(
                    text = stringResource(R.string.desktop_hint_minimal),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("desktop_minimal_hint")
                )
            }

            ManualFloatingDock(
                dockApps = uiState.dockApps,
                showLabels = uiState.preferences.showDockLabels,
                getIconBitmap = getIconBitmap,
                onOpenLibrary = onOpenLibrary,
                onOpenSpotlight = onOpenSpotlight,
                onLaunchApp = onLaunchApp,
                onLongPressApp = onLongPressApp
            )
        }
    }
}

@Composable
private fun DesktopItemsGrid(
    uiState: LauncherUiState,
    columns: Int,
    getIconBitmap: (String) -> Bitmap?,
    onOpenFolder: (String) -> Unit,
    onLaunchApp: (InstalledAppInfo) -> Unit,
    onLongPressApp: (InstalledAppInfo) -> Unit,
    headerContent: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.testTag("desktop_items_grid")
    ) {
        if (headerContent != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                headerContent()
            }
        }

        items(uiState.folders, key = { "folder_${it.id}" }) { folder ->
            DesktopFolderTile(
                folder = folder,
                getIconBitmap = getIconBitmap,
                onClick = { onOpenFolder(folder.id) }
            )
        }

        items(uiState.pinnedHomeApps, key = { "app_${it.packageName}" }) { app ->
            AppGridTile(
                app = app,
                iconBitmap = getIconBitmap(app.packageName),
                onClick = { onLaunchApp(app) },
                onLongClick = { onLongPressApp(app) },
                showBadge = false
            )
        }
    }
}

@Composable
private fun DesktopBackdrop(style: WallpaperStyle) {
    when (style) {
        WallpaperStyle.TONAL -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
                            )
                        )
                    )
            )
        }
        WallpaperStyle.NORDIC_MIST -> {
            Image(
                painter = painterResource(id = R.drawable.img_wallpaper_nordic_mist),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.55f)
            )
        }
        WallpaperStyle.MIDNIGHT_RIDGE -> {
            Image(
                painter = painterResource(id = R.drawable.img_wallpaper_midnight_ridge),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.60f)
            )
        }
    }
}
