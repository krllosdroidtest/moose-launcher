package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.domain.model.DesktopFolder
import com.example.domain.model.InstalledAppInfo

@Composable
fun DesktopFolderTile(
    folder: DesktopFolder,
    getIconBitmap: (String) -> ImageBitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasNotification: Boolean = false,
    interactive: Boolean = true
) {
    val baseModifier = modifier
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(18.dp))
    val finalModifier = if (interactive) baseModifier.clickable(onClick = onClick) else baseModifier

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = finalModifier
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .testTag("folder_tile_${folder.id}")
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                modifier = Modifier.size(54.dp)
            ) {
                val previewApps = folder.apps.take(4)
                if (previewApps.isEmpty()) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = folder.name,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(6.dp)
                    ) {
                        for (rowIdx in 0..1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (colIdx in 0..1) {
                                    val app = previewApps.getOrNull(rowIdx * 2 + colIdx)
                                    if (app != null) {
                                        AppIconArtwork(
                                            app = app,
                                            iconBitmap = getIconBitmap(app.packageName),
                                            size = 18.dp,
                                            cornerRadius = 5.dp
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (hasNotification) {
                NotificationBadgeDot(tagSuffix = "folder_${folder.id}")
            }
        }
        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CreateFolderDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.folder_create_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.folder_name_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("folder_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("folder_create_confirm_button")
            ) {
                Text(stringResource(R.string.folder_create_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.folder_cancel))
            }
        },
        modifier = Modifier.testTag("create_folder_dialog")
    )
}

@Composable
fun FolderModalDialog(
    folder: DesktopFolder,
    installedApps: List<InstalledAppInfo>,
    activeNotificationPackages: Set<String> = emptySet(),
    getIconBitmap: (String) -> ImageBitmap?,
    onLaunchApp: (InstalledAppInfo) -> Unit,
    onLongPressApp: (InstalledAppInfo) -> Unit,
    onRenameFolder: (String) -> Unit,
    onDeleteFolder: () -> Unit,
    onToggleAppInFolder: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    var isEditingName by remember(folder.id) { mutableStateOf(false) }
    var draftName by remember(folder.id, folder.name) { mutableStateOf(folder.name) }
    var showAppPicker by remember(folder.id) { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("folder_modal_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isEditingName) {
                        OutlinedTextField(
                            value = draftName,
                            onValueChange = { draftName = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                onRenameFolder(draftName)
                                isEditingName = false
                            }
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.folder_save_confirm))
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.folder_count_format, folder.apps.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { isEditingName = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.folder_rename_title))
                        }
                        IconButton(
                            onClick = onDeleteFolder,
                            modifier = Modifier.testTag("folder_delete_button")
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.folder_delete))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (showAppPicker) {
                    val memberSet = remember(folder.apps) { folder.apps.map { it.packageName }.toSet() }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        items(installedApps.size, key = { installedApps[it].packageName }) { idx ->
                            val app = installedApps[idx]
                            val checked = app.packageName in memberSet
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onToggleAppInFolder(app.packageName) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp)
                            ) {
                                AppIconArtwork(
                                    app = app,
                                    iconBitmap = getIconBitmap(app.packageName),
                                    size = 34.dp,
                                    cornerRadius = 10.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { onToggleAppInFolder(app.packageName) }
                                )
                            }
                        }
                    }
                } else if (folder.apps.isEmpty()) {
                    Text(
                        text = stringResource(R.string.folder_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 18.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        items(folder.apps, key = { it.packageName }) { app ->
                            AppGridTile(
                                app = app,
                                iconBitmap = getIconBitmap(app.packageName),
                                onClick = { onLaunchApp(app) },
                                onLongClick = { onLongPressApp(app) },
                                hasNotification = app.packageName in activeNotificationPackages
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { showAppPicker = !showAppPicker },
                        modifier = Modifier.testTag("folder_toggle_picker_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(
                                if (showAppPicker) R.string.folder_save_confirm else R.string.folder_add_apps
                            )
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_close))
                    }
                }
            }
        }
    }
}
