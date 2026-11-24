package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopContextSheet(
    isDefaultHome: Boolean,
    isNotificationAccessEnabled: Boolean = true,
    onCreateFolder: () -> Unit,
    onManageWidgets: () -> Unit,
    onPickWallpaper: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenDefaultHomeSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("desktop_context_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = stringResource(R.string.desktop_customize_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.desktop_customize_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.action_new_folder)) },
                leadingContent = {
                    Icon(Icons.Rounded.CreateNewFolder, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onCreateFolder()
                    }
                    .testTag("desktop_menu_new_folder")
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.action_manage_widgets)) },
                leadingContent = {
                    Icon(Icons.Rounded.Widgets, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onManageWidgets()
                    }
                    .testTag("desktop_menu_widgets")
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.desktop_menu_wallpaper)) },
                leadingContent = {
                    Icon(Icons.Rounded.Wallpaper, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onPickWallpaper()
                    }
                    .testTag("desktop_menu_wallpaper")
            )

            if (!isNotificationAccessEnabled) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.desktop_menu_notifications)) },
                    leadingContent = {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onOpenNotificationSettings()
                        }
                        .testTag("desktop_menu_notifications")
                )
            }

            if (!isDefaultHome) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.desktop_set_default_action)) },
                    leadingContent = {
                        Icon(Icons.Rounded.Home, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onOpenDefaultHomeSettings()
                        }
                        .testTag("desktop_menu_default_home")
                )
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.desktop_appearance_settings)) },
                leadingContent = {
                    Icon(Icons.Rounded.Settings, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onOpenSettings()
                    }
                    .testTag("desktop_menu_settings")
            )
        }
    }
}
