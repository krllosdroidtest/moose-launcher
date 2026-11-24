package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.WallpaperStyle
import com.example.ui.LauncherUiState
import com.example.ui.components.AppGridTile
import com.example.ui.components.DesktopFolderTile
import com.example.ui.components.DesktopWidgetsGrid
import com.example.ui.components.LauncherMenuBar
import com.example.ui.components.ManualFloatingDock
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    uiState: LauncherUiState,
    getIconBitmap: (String) -> Bitmap?,
    onOpenSpotlight: () -> Unit,
    onCreateFolder: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onManageWidgets: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onLaunchApp: (InstalledAppInfo) -> Unit,
    onLongPressApp: (InstalledAppInfo) -> Unit,
    onSaveQuickNote: (String) -> Unit,
    onOpenDefaultHomeSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(30_000L)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        DesktopBackdrop(style = uiState.preferences.wallpaperStyle)

        Column(modifier = Modifier.fillMaxSize()) {
            LauncherMenuBar(
                onOpenSpotlight = onOpenSpotlight,
                onCreateFolder = onCreateFolder,
                onManageWidgets = onManageWidgets,
                onOpenSettings = onOpenSettings
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val isWide = maxWidth >= 600.dp

                if (isWide) {
                    val rightColumns = if (maxWidth >= 960.dp) 6 else 4
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1200.dp)
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .weight(0.38f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 124.dp)
                        ) {
                            SpotlightTriggerBar(onClick = onOpenSpotlight)

                            DesktopWidgetsGrid(
                                widgets = uiState.widgets,
                                nowMillis = nowMillis,
                                deviceStatus = uiState.deviceStatus,
                                quickNoteText = uiState.quickNoteText,
                                onSaveQuickNote = onSaveQuickNote
                            )

                            if (!uiState.isDefaultHome) {
                                DefaultHomePromptCard(onClick = onOpenDefaultHomeSettings)
                            }
                        }

                        DesktopItemsGrid(
                            uiState = uiState,
                            columns = rightColumns,
                            getIconBitmap = getIconBitmap,
                            onOpenFolder = onOpenFolder,
                            onCreateFolder = onCreateFolder,
                            onOpenLibrary = onOpenLibrary,
                            onLaunchApp = onLaunchApp,
                            onLongPressApp = onLongPressApp,
                            headerContent = null,
                            modifier = Modifier
                                .weight(0.62f)
                                .fillMaxHeight()
                        )
                    }
                } else {
                    DesktopItemsGrid(
                        uiState = uiState,
                        columns = 4,
                        getIconBitmap = getIconBitmap,
                        onOpenFolder = onOpenFolder,
                        onCreateFolder = onCreateFolder,
                        onOpenLibrary = onOpenLibrary,
                        onLaunchApp = onLaunchApp,
                        onLongPressApp = onLongPressApp,
                        headerContent = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                DesktopWidgetsGrid(
                                    widgets = uiState.widgets,
                                    nowMillis = nowMillis,
                                    deviceStatus = uiState.deviceStatus,
                                    quickNoteText = uiState.quickNoteText,
                                    onSaveQuickNote = onSaveQuickNote
                                )

                                SpotlightTriggerBar(onClick = onOpenSpotlight)

                                if (!uiState.isDefaultHome) {
                                    DefaultHomePromptCard(onClick = onOpenDefaultHomeSettings)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }

        ManualFloatingDock(
            dockApps = uiState.dockApps,
            showLabels = uiState.preferences.showDockLabels,
            getIconBitmap = getIconBitmap,
            onOpenLibrary = onOpenLibrary,
            onLaunchApp = onLaunchApp,
            onLongPressApp = onLongPressApp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}

@Composable
private fun DesktopItemsGrid(
    uiState: LauncherUiState,
    columns: Int,
    getIconBitmap: (String) -> Bitmap?,
    onOpenFolder: (String) -> Unit,
    onCreateFolder: () -> Unit,
    onOpenLibrary: () -> Unit,
    onLaunchApp: (InstalledAppInfo) -> Unit,
    onLongPressApp: (InstalledAppInfo) -> Unit,
    headerContent: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(top = 8.dp, bottom = 132.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.testTag("desktop_items_grid")
    ) {
        if (headerContent != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                headerContent()
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.desktop_items_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onCreateFolder,
                    modifier = Modifier.testTag("desktop_quick_new_folder_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CreateNewFolder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_new_folder))
                }
            }
        }

        if (uiState.folders.isEmpty() && uiState.pinnedHomeApps.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyDesktopCard(
                    onCreateFolder = onCreateFolder,
                    onOpenLibrary = onOpenLibrary
                )
            }
        } else {
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
                    showBadge = app.packageName in uiState.pinnedDockPackages
                )
            }
        }
    }
}

@Composable
private fun SpotlightTriggerBar(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("home_spotlight_bar")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.spotlight_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DefaultHomePromptCard(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.desktop_set_default_banner),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            TextButton(
                onClick = onClick,
                modifier = Modifier.testTag("home_default_launcher_button")
            ) {
                Text(stringResource(R.string.desktop_set_default_action))
            }
        }
    }
}

@Composable
private fun EmptyDesktopCard(
    onCreateFolder: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.86f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("desktop_empty_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = stringResource(R.string.desktop_empty_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.desktop_empty_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                OutlinedButton(
                    onClick = onCreateFolder,
                    modifier = Modifier.testTag("desktop_empty_create_folder_button")
                ) {
                    Text(stringResource(R.string.action_new_folder))
                }
                FilledTonalButton(
                    onClick = onOpenLibrary,
                    modifier = Modifier.testTag("desktop_empty_open_library_button")
                ) {
                    Text(stringResource(R.string.desktop_open_library))
                }
            }
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
