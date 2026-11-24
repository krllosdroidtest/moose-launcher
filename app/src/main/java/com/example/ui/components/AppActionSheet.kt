package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.DesktopFolder
import com.example.domain.model.InstalledAppInfo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppActionSheet(
    app: InstalledAppInfo,
    iconBitmap: Bitmap?,
    isPinnedHome: Boolean,
    isPinnedDock: Boolean,
    folders: List<DesktopFolder>,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onTogglePinHome: () -> Unit,
    onTogglePinDock: () -> Unit,
    onToggleFolderMembership: (String) -> Unit,
    onCreateFolderWithApp: () -> Unit,
    onOpenAppInfo: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("app_action_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AppIconArtwork(app = app, iconBitmap = iconBitmap, size = 48.dp, cornerRadius = 14.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${localizedCategoryName(app.category)} • ${app.packageName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.action_open_app)) },
                leadingContent = {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLaunch)
                    .testTag("action_open_app")
            )

            ListItem(
                headlineContent = {
                    Text(
                        stringResource(
                            if (isPinnedHome) R.string.action_unpin_home else R.string.action_pin_home
                        )
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = if (isPinnedHome) Icons.Rounded.PushPin else Icons.Rounded.Home,
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTogglePinHome)
                    .testTag("action_toggle_pin_home")
            )

            ListItem(
                headlineContent = {
                    Text(
                        stringResource(
                            if (isPinnedDock) R.string.action_unpin_dock else R.string.action_pin_dock
                        )
                    )
                },
                leadingContent = {
                    Icon(Icons.Rounded.Dock, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTogglePinDock)
                    .testTag("action_toggle_pin_dock")
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.action_app_info)) },
                leadingContent = {
                    Icon(Icons.Rounded.Info, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAppInfo)
                    .testTag("action_app_info")
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.action_add_to_folder),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                folders.forEach { folder ->
                    val isMember = folder.apps.any { it.packageName == app.packageName }
                    FilterChip(
                        selected = isMember,
                        onClick = { onToggleFolderMembership(folder.id) },
                        label = { Text(folder.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("sheet_folder_chip_${folder.id}")
                    )
                }
                AssistChip(
                    onClick = onCreateFolderWithApp,
                    label = { Text(stringResource(R.string.action_new_folder)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.CreateNewFolder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("sheet_new_folder_chip")
                )
            }
        }
    }
}
