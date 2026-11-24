package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.InstalledAppInfo

@Composable
fun AppIconArtwork(
    app: InstalledAppInfo,
    iconBitmap: Bitmap?,
    size: Dp = 52.dp,
    cornerRadius: Dp = 16.dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        shape = shape,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(size)
    ) {
        if (iconBitmap != null) {
            val imageBitmap = remember(iconBitmap) { iconBitmap.asImageBitmap() }
            Image(
                bitmap = imageBitmap,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(shape)
            )
        } else {
            val initials = remember(app.label) {
                app.label.trim().take(2).uppercase().ifEmpty { "AP" }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridTile(
    app: InstalledAppInfo,
    iconBitmap: Bitmap?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false
) {
    val haptics = LocalHapticFeedback.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .testTag("app_tile_${app.packageName}")
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            AppIconArtwork(app = app, iconBitmap = iconBitmap, size = 54.dp, cornerRadius = 16.dp)
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(9.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
