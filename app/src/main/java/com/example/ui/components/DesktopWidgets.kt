package com.example.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.domain.model.DesktopWidgetConfig
import com.example.domain.model.DeviceStatusSnapshot
import com.example.domain.model.ExternalWidgetConfig
import com.example.domain.model.WidgetType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DesktopWidgetsGrid(
    widgets: List<DesktopWidgetConfig>,
    externalWidgets: List<ExternalWidgetConfig> = emptyList(),
    nowMillis: Long,
    deviceStatus: DeviceStatusSnapshot,
    quickNoteText: String,
    appWidgetHost: AppWidgetHost? = null,
    appWidgetManager: AppWidgetManager? = null,
    onSaveQuickNote: (String) -> Unit,
    onToggleExternalWidgetHeight: (ExternalWidgetConfig) -> Unit = {},
    onRemoveExternalWidget: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val enabledWidgets = widgets.filter { it.enabled }.sortedBy { it.position }
    if (enabledWidgets.isEmpty() && externalWidgets.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("desktop_widgets_section")
    ) {
        enabledWidgets.forEach { widget ->
            when (widget.type) {
                WidgetType.CLOCK -> ClockWidgetCard(nowMillis = nowMillis)
                WidgetType.SYSTEM_STATUS -> DeviceStatusWidgetCard(status = deviceStatus)
                WidgetType.QUICK_NOTES -> QuickNotesWidgetCard(
                    noteText = quickNoteText,
                    onSaveNote = onSaveQuickNote
                )
            }
        }

        externalWidgets.forEach { extWidget ->
            ExternalAppWidgetCard(
                config = extWidget,
                appWidgetHost = appWidgetHost,
                appWidgetManager = appWidgetManager,
                onToggleHeight = { onToggleExternalWidgetHeight(extWidget) },
                onRemove = { onRemoveExternalWidget(extWidget.appWidgetId) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExternalAppWidgetCard(
    config: ExternalWidgetConfig,
    appWidgetHost: AppWidgetHost?,
    appWidgetManager: AppWidgetManager?,
    onToggleHeight: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val providerInfo = remember(config.appWidgetId, appWidgetManager) {
        runCatching { appWidgetManager?.getAppWidgetInfo(config.appWidgetId) }.getOrNull()
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { showMenu = true }
            )
            .testTag("external_widget_${config.appWidgetId}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(config.heightDp.dp)
        ) {
            if (appWidgetHost != null && providerInfo != null) {
                AndroidView(
                    factory = { ctx ->
                        runCatching {
                            appWidgetHost.createView(ctx, config.appWidgetId, providerInfo).apply {
                                setAppWidget(config.appWidgetId, providerInfo)
                            }
                        }.getOrNull() ?: android.widget.FrameLayout(ctx)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = config.providerLabel.ifBlank {
                                context.getString(R.string.widget_external_fallback)
                            },
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.widget_external_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = stringResource(R.string.action_manage_widgets),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (config.heightDp < 230) R.string.widget_external_resize_taller
                                    else R.string.widget_external_resize_compact
                                )
                            )
                        },
                        leadingIcon = { Icon(Icons.Rounded.Height, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onToggleHeight()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.widget_external_remove)) },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ClockWidgetCard(
    nowMillis: Long,
    modifier: Modifier = Modifier
) {
    val timeText = remember(nowMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMillis))
    }
    val dateText = remember(nowMillis) {
        SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date(nowMillis))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("widget_clock_card")
    ) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFeatureSettings = "tnum"
            ),
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = dateText.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
        )
    }
}

@Composable
fun DeviceStatusWidgetCard(
    status: DeviceStatusSnapshot,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("widget_system_status_card")
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (status.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(R.string.widget_battery_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (status.isCharging) {
                            stringResource(R.string.widget_battery_charging, status.batteryPercent)
                        } else {
                            stringResource(R.string.widget_battery_discharging, status.batteryPercent)
                        },
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(R.string.widget_storage_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.widget_storage_gb_format, status.freeStorageGb),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}

@Composable
fun QuickNotesWidgetCard(
    noteText: String,
    onSaveNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var draft by remember(noteText) { mutableStateOf(noteText) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("widget_quick_notes_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.widget_notes_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                IconButton(
                    onClick = {
                        if (isEditing) {
                            onSaveNote(draft)
                            isEditing = false
                        } else {
                            draft = noteText
                            isEditing = true
                        }
                    },
                    modifier = Modifier.testTag("widget_notes_toggle_edit")
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Rounded.Check else Icons.Rounded.Edit,
                        contentDescription = stringResource(
                            if (isEditing) R.string.widget_notes_save else R.string.widget_notes_edit
                        ),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (isEditing) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(stringResource(R.string.widget_notes_placeholder)) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("widget_notes_input")
                )
            } else {
                Text(
                    text = noteText.ifBlank { stringResource(R.string.widget_notes_placeholder) },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (noteText.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            draft = noteText
                            isEditing = true
                        }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ManageWidgetsDialog(
    widgets: List<DesktopWidgetConfig>,
    externalWidgets: List<ExternalWidgetConfig> = emptyList(),
    onToggleWidget: (WidgetType, Boolean) -> Unit,
    onAddExternalWidget: () -> Unit = {},
    onRemoveExternalWidget: (Int) -> Unit = {},
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.widget_settings_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            modifier = Modifier.testTag("widget_switch_${widget.type.name.lowercase()}")
                        )
                    }
                }

                HorizontalDivider()

                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onAddExternalWidget()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("widgets_add_external_button")
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
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
        modifier = Modifier.testTag("manage_widgets_dialog")
    )
}
