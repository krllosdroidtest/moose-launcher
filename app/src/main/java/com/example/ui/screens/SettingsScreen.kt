package com.example.ui.screens

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.domain.model.DesktopWidgetConfig
import com.example.domain.model.ExternalWidgetConfig
import com.example.domain.model.ImportStrategy
import com.example.domain.model.ThemePreference
import com.example.domain.model.WallpaperStyle
import com.example.domain.model.WidgetType
import com.example.ui.LauncherUiState
import com.example.ui.components.localizedWidgetName

@Composable
fun SettingsScreen(
    uiState: LauncherUiState,
    pendingImportJson: String?,
    onThemeModeChange: (ThemePreference) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onWallpaperChange: (WallpaperStyle) -> Unit,
    onPickGalleryPhoto: () -> Unit,
    onShowDockLabelsChange: (Boolean) -> Unit,
    onToggleWidget: (WidgetType, Boolean) -> Unit,
    onAddExternalWidget: () -> Unit,
    onRemoveExternalWidget: (Int) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onExportToUri: (Uri) -> Unit,
    onPrepareImportFromUri: (Uri) -> Unit,
    onConfirmImport: (ImportStrategy) -> Unit,
    onDismissImportDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportToUri(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onPrepareImportFromUri(uri)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 760.dp

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_list")
        ) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .widthIn(max = 1040.dp)
                        .fillMaxWidth()
                ) {
                    if (isWide) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                AppearanceCard(
                                    uiState = uiState,
                                    onThemeModeChange = onThemeModeChange,
                                    onDynamicColorChange = onDynamicColorChange,
                                    onWallpaperChange = onWallpaperChange,
                                    onPickGalleryPhoto = onPickGalleryPhoto,
                                    onShowDockLabelsChange = onShowDockLabelsChange
                                )
                                DesktopWidgetsSettingsCard(
                                    widgets = uiState.widgets,
                                    externalWidgets = uiState.externalWidgets,
                                    onToggleWidget = onToggleWidget,
                                    onAddExternalWidget = onAddExternalWidget,
                                    onRemoveExternalWidget = onRemoveExternalWidget
                                )
                            }
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                NotificationDotsCard(
                                    isEnabled = uiState.isNotificationAccessEnabled,
                                    onOpenSettings = onOpenNotificationSettings
                                )
                                SystemLauncherCard(
                                    isDefaultHome = uiState.isDefaultHome,
                                    onOpenHomeSettings = onOpenHomeSettings
                                )
                                OfflineBackupCard(
                                    onExportClick = {
                                        exportLauncher.launch("moose-launcher-backup.json")
                                    },
                                    onImportClick = {
                                        importLauncher.launch(arrayOf("application/json", "*/*"))
                                    }
                                )
                                AboutPrivacyCard()
                            }
                        }
                    } else {
                        AppearanceCard(
                            uiState = uiState,
                            onThemeModeChange = onThemeModeChange,
                            onDynamicColorChange = onDynamicColorChange,
                            onWallpaperChange = onWallpaperChange,
                            onPickGalleryPhoto = onPickGalleryPhoto,
                            onShowDockLabelsChange = onShowDockLabelsChange
                        )
                        DesktopWidgetsSettingsCard(
                            widgets = uiState.widgets,
                            externalWidgets = uiState.externalWidgets,
                            onToggleWidget = onToggleWidget,
                            onAddExternalWidget = onAddExternalWidget,
                            onRemoveExternalWidget = onRemoveExternalWidget
                        )
                        NotificationDotsCard(
                            isEnabled = uiState.isNotificationAccessEnabled,
                            onOpenSettings = onOpenNotificationSettings
                        )
                        SystemLauncherCard(
                            isDefaultHome = uiState.isDefaultHome,
                            onOpenHomeSettings = onOpenHomeSettings
                        )
                        OfflineBackupCard(
                            onExportClick = {
                                exportLauncher.launch("moose-launcher-backup.json")
                            },
                            onImportClick = {
                                importLauncher.launch(arrayOf("application/json", "*/*"))
                            }
                        )
                        AboutPrivacyCard()
                    }
                }
            }
        }
    }

    if (pendingImportJson != null) {
        ImportStrategyDialog(
            onMerge = { onConfirmImport(ImportStrategy.MERGE) },
            onReplaceAll = { onConfirmImport(ImportStrategy.REPLACE_ALL) },
            onDismiss = onDismissImportDialog
        )
    }
}

@Composable
fun ImportStrategyDialog(
    onMerge: () -> Unit,
    onReplaceAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_import_dialog_title)) },
        text = { Text(stringResource(R.string.settings_import_dialog_body)) },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onMerge,
                    modifier = Modifier.testTag("import_merge_button")
                ) {
                    Text(stringResource(R.string.settings_import_merge))
                }
                Button(
                    onClick = onReplaceAll,
                    modifier = Modifier.testTag("import_replace_button")
                ) {
                    Text(stringResource(R.string.settings_import_replace))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("import_cancel_button")
            ) {
                Text(stringResource(R.string.folder_cancel))
            }
        },
        modifier = Modifier.testTag("import_strategy_dialog")
    )
}

@Composable
private fun DesktopWidgetsSettingsCard(
    widgets: List<DesktopWidgetConfig>,
    externalWidgets: List<ExternalWidgetConfig>,
    onToggleWidget: (WidgetType, Boolean) -> Unit,
    onAddExternalWidget: () -> Unit,
    onRemoveExternalWidget: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_widgets_card")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Widgets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.widget_settings_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = stringResource(R.string.widget_settings_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            widgets.forEach { widget ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = localizedWidgetName(widget.type),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = widget.enabled,
                        onCheckedChange = { onToggleWidget(widget.type, it) },
                        modifier = Modifier.testTag("settings_widget_switch_${widget.type.name.lowercase()}")
                    )
                }
            }

            HorizontalDivider()

            FilledTonalButton(
                onClick = onAddExternalWidget,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_add_external_widget_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.widgets_add_external))
            }

            if (externalWidgets.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.widgets_external_header),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                externalWidgets.forEach { ext ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = ext.providerLabel.ifBlank { ext.providerPackage },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemoveExternalWidget(ext.appWidgetId) }) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(R.string.widget_external_remove),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceCard(
    uiState: LauncherUiState,
    onThemeModeChange: (ThemePreference) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onWallpaperChange: (WallpaperStyle) -> Unit,
    onPickGalleryPhoto: () -> Unit,
    onShowDockLabelsChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_appearance_title),
                style = MaterialTheme.typography.titleMedium
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.settings_theme_label),
                    style = MaterialTheme.typography.labelLarge
                )
                val options = ThemePreference.entries
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = uiState.preferences.themeMode == option,
                            onClick = { onThemeModeChange(option) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            modifier = Modifier.testTag("theme_option_${option.name.lowercase()}")
                        ) {
                            Text(
                                text = when (option) {
                                    ThemePreference.SYSTEM -> stringResource(R.string.settings_theme_system)
                                    ThemePreference.LIGHT -> stringResource(R.string.settings_theme_light)
                                    ThemePreference.DARK -> stringResource(R.string.settings_theme_dark)
                                }
                            )
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(R.string.settings_dynamic_color),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.settings_dynamic_color_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.preferences.useDynamicColor,
                        onCheckedChange = onDynamicColorChange,
                        modifier = Modifier.testTag("dynamic_color_switch")
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.settings_wallpaper_label),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    WallpaperPreviewOption(
                        style = WallpaperStyle.TONAL,
                        label = stringResource(R.string.settings_wallpaper_tonal),
                        selected = uiState.preferences.wallpaperStyle == WallpaperStyle.TONAL,
                        customWallpaperImage = null,
                        onClick = { onWallpaperChange(WallpaperStyle.TONAL) },
                        modifier = Modifier.weight(1f)
                    )
                    WallpaperPreviewOption(
                        style = WallpaperStyle.NORDIC_MIST,
                        label = stringResource(R.string.settings_wallpaper_mist),
                        selected = uiState.preferences.wallpaperStyle == WallpaperStyle.NORDIC_MIST,
                        customWallpaperImage = null,
                        onClick = { onWallpaperChange(WallpaperStyle.NORDIC_MIST) },
                        modifier = Modifier.weight(1f)
                    )
                    WallpaperPreviewOption(
                        style = WallpaperStyle.MIDNIGHT_RIDGE,
                        label = stringResource(R.string.settings_wallpaper_midnight),
                        selected = uiState.preferences.wallpaperStyle == WallpaperStyle.MIDNIGHT_RIDGE,
                        customWallpaperImage = null,
                        onClick = { onWallpaperChange(WallpaperStyle.MIDNIGHT_RIDGE) },
                        modifier = Modifier.weight(1f)
                    )
                    WallpaperPreviewOption(
                        style = WallpaperStyle.CUSTOM_PHOTO,
                        label = stringResource(R.string.settings_wallpaper_photo),
                        selected = uiState.preferences.wallpaperStyle == WallpaperStyle.CUSTOM_PHOTO,
                        customWallpaperImage = uiState.customWallpaperImage,
                        onClick = {
                            if (uiState.customWallpaperImage != null) {
                                onWallpaperChange(WallpaperStyle.CUSTOM_PHOTO)
                            } else {
                                onPickGalleryPhoto()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                FilledTonalButton(
                    onClick = onPickGalleryPhoto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_pick_gallery_wallpaper_button")
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_pick_gallery_photo))
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_dock_labels),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_dock_labels_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.preferences.showDockLabels,
                    onCheckedChange = onShowDockLabelsChange,
                    modifier = Modifier.testTag("dock_labels_switch")
                )
            }
        }
    }
}

@Composable
private fun WallpaperPreviewOption(
    style: WallpaperStyle,
    label: String,
    selected: Boolean,
    customWallpaperImage: ImageBitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("wallpaper_option_${style.name.lowercase()}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(6.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                when (style) {
                    WallpaperStyle.TONAL -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                        MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            )
                    )
                    WallpaperStyle.NORDIC_MIST -> Image(
                        painter = painterResource(R.drawable.img_wallpaper_nordic_mist),
                        contentDescription = label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    WallpaperStyle.MIDNIGHT_RIDGE -> Image(
                        painter = painterResource(R.drawable.img_wallpaper_midnight_ridge),
                        contentDescription = label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    WallpaperStyle.CUSTOM_PHOTO -> {
                        if (customWallpaperImage != null) {
                            Image(
                                bitmap = customWallpaperImage,
                                contentDescription = label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoLibrary,
                                    contentDescription = label,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NotificationDotsCard(
    isEnabled: Boolean,
    onOpenSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_notifications_card")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.settings_notifications_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = stringResource(
                    if (isEnabled) R.string.settings_notifications_active
                    else R.string.settings_notifications_inactive
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("settings_notifications_button")
            ) {
                Text(stringResource(R.string.settings_open_notifications_settings))
            }
        }
    }
}

@Composable
private fun SystemLauncherCard(
    isDefaultHome: Boolean,
    onOpenHomeSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.settings_launcher_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = stringResource(
                    if (isDefaultHome) R.string.settings_default_home_status_active
                    else R.string.settings_default_home_status_inactive
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(
                onClick = onOpenHomeSettings,
                modifier = Modifier.testTag("settings_open_home_button")
            ) {
                Text(stringResource(R.string.settings_open_home_settings))
            }
        }
    }
}

@Composable
private fun OfflineBackupCard(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Backup,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.settings_backup_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = stringResource(R.string.settings_backup_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onExportClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_export_backup_button")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_export_json))
                }
                OutlinedButton(
                    onClick = onImportClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_import_backup_button")
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_import_json))
                }
            }
        }
    }
}

@Composable
private fun AboutPrivacyCard() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.settings_about_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = stringResource(
                    R.string.settings_about_version_format,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.settings_about_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
