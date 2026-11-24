package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.DesktopWidgetConfig
import com.example.domain.model.DeviceStatusSnapshot
import com.example.domain.model.WidgetType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DesktopWidgetsGrid(
    widgets: List<DesktopWidgetConfig>,
    nowMillis: Long,
    deviceStatus: DeviceStatusSnapshot,
    quickNoteText: String,
    onSaveQuickNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val enabledWidgets = widgets.filter { it.enabled }.sortedBy { it.position }
    if (enabledWidgets.isEmpty()) return

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

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.84f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("widget_clock_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateText.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
    onToggleWidget: (WidgetType, Boolean) -> Unit,
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
