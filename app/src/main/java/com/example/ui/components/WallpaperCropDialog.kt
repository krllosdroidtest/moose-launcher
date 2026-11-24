package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R

@Composable
fun WallpaperCropDialog(
    sourceBitmap: Bitmap,
    onApply: (
        viewportWidth: Float,
        viewportHeight: Float,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        applyToSystem: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val previewImage = remember(sourceBitmap) { sourceBitmap.asImageBitmap() }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
        val maxOffsetX = (viewportSize.width * (nextScale - 1f)) * 0.65f + viewportSize.width * 0.35f
        val maxOffsetY = (viewportSize.height * (nextScale - 1f)) * 0.65f + viewportSize.height * 0.35f
        scale = nextScale
        offset = Offset(
            x = (offset.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
            y = (offset.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxSize()
                .testTag("wallpaper_crop_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.wallpaper_crop_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.wallpaper_crop_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("wallpaper_crop_cancel")
                    ) {
                        Text(stringResource(R.string.folder_cancel))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .onSizeChanged { viewportSize = it }
                        .transformable(state = transformableState)
                ) {
                    Image(
                        bitmap = previewImage,
                        contentDescription = stringResource(R.string.wallpaper_crop_title),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                    )

                    // Subtle rule-of-thirds alignment frame
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridColor = Color.White.copy(alpha = 0.25f)
                        val thirdW = size.width / 3f
                        val thirdH = size.height / 3f
                        for (i in 1..2) {
                            drawLine(
                                color = gridColor,
                                start = Offset(thirdW * i, 0f),
                                end = Offset(thirdW * i, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, thirdH * i),
                                end = Offset(size.width, thirdH * i),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = {
                            onApply(
                                viewportSize.width.toFloat().coerceAtLeast(360f),
                                viewportSize.height.toFloat().coerceAtLeast(640f),
                                scale,
                                offset.x,
                                offset.y,
                                false
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wallpaper_apply_desktop_button")
                    ) {
                        Text(stringResource(R.string.wallpaper_apply_desktop_only))
                    }
                    Button(
                        onClick = {
                            onApply(
                                viewportSize.width.toFloat().coerceAtLeast(360f),
                                viewportSize.height.toFloat().coerceAtLeast(640f),
                                scale,
                                offset.x,
                                offset.y,
                                true
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wallpaper_apply_both_button")
                    ) {
                        Text(stringResource(R.string.wallpaper_apply_both))
                    }
                }
            }
        }
    }
}
