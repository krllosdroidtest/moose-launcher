package com.example.ui.screens

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.domain.engine.DesktopGridContent
import com.example.domain.engine.DesktopLayoutEngine
import com.example.domain.model.ExternalWidgetConfig
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.WallpaperStyle
import com.example.ui.LauncherUiState
import com.example.ui.components.AppGridTile
import com.example.ui.components.DesktopFolderTile
import com.example.ui.components.DesktopWidgetsGrid
import com.example.ui.components.ManualFloatingDock
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    uiState: LauncherUiState,
    appWidgetHost: AppWidgetHost? = null,
    appWidgetManager: AppWidgetManager? = null,
    getIconBitmap: (String) -> ImageBitmap?,
    onOpenSpotlight: () -> Unit,
    onOpenDesktopMenu: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    onLaunchApp: (InstalledAppInfo) -> Unit,
    onLongPressApp: (InstalledAppInfo) -> Unit,
    onMoveGridItem: (movingKey: String, targetRow: Int, targetCol: Int, columns: Int) -> Unit,
    onSaveQuickNote: (String) -> Unit,
    onToggleExternalWidgetHeight: (ExternalWidgetConfig) -> Unit = {},
    onRemoveExternalWidget: (Int) -> Unit = {},
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
        DesktopBackdrop(
            style = uiState.preferences.wallpaperStyle,
            customWallpaperImage = uiState.customWallpaperImage
        )

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
                            externalWidgets = uiState.externalWidgets,
                            nowMillis = nowMillis,
                            deviceStatus = uiState.deviceStatus,
                            quickNoteText = uiState.quickNoteText,
                            appWidgetHost = appWidgetHost,
                            appWidgetManager = appWidgetManager,
                            onSaveQuickNote = onSaveQuickNote,
                            onToggleExternalWidgetHeight = onToggleExternalWidgetHeight,
                            onRemoveExternalWidget = onRemoveExternalWidget
                        )
                    }

                    DesktopItemsGrid(
                        uiState = uiState,
                        columns = rightColumns,
                        getIconBitmap = getIconBitmap,
                        onOpenFolder = onOpenFolder,
                        onLaunchApp = onLaunchApp,
                        onLongPressApp = onLongPressApp,
                        onMoveGridItem = onMoveGridItem,
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
                    onMoveGridItem = onMoveGridItem,
                    headerContent = {
                        DesktopWidgetsGrid(
                            widgets = uiState.widgets,
                            externalWidgets = uiState.externalWidgets,
                            nowMillis = nowMillis,
                            deviceStatus = uiState.deviceStatus,
                            quickNoteText = uiState.quickNoteText,
                            appWidgetHost = appWidgetHost,
                            appWidgetManager = appWidgetManager,
                            onSaveQuickNote = onSaveQuickNote,
                            onToggleExternalWidgetHeight = onToggleExternalWidgetHeight,
                            onRemoveExternalWidget = onRemoveExternalWidget,
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
            if (
                uiState.folders.isEmpty() &&
                uiState.pinnedHomeItems.isEmpty() &&
                uiState.dockApps.isEmpty() &&
                uiState.externalWidgets.isEmpty()
            ) {
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
                activeNotificationPackages = uiState.activeNotificationPackages,
                getIconBitmap = getIconBitmap,
                onOpenLibrary = onOpenLibrary,
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
    getIconBitmap: (String) -> ImageBitmap?,
    onOpenFolder: (String) -> Unit,
    onLaunchApp: (InstalledAppInfo) -> Unit,
    onLongPressApp: (InstalledAppInfo) -> Unit,
    onMoveGridItem: (movingKey: String, targetRow: Int, targetCol: Int, columns: Int) -> Unit,
    headerContent: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier
) {
    val slots = remember(uiState.folders, uiState.pinnedHomeItems, columns) {
        DesktopLayoutEngine.assignGridSlots(
            folders = uiState.folders,
            shortcuts = uiState.pinnedHomeItems,
            columns = columns
        )
    }

    val slotMap = remember(slots) {
        slots.associateBy { it.row to it.column }
    }

    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOrigin by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var gridWidthPx by remember { mutableIntStateOf(0) }

    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val cellHeightDp = 96.dp
    val rowSpacingDp = 10.dp
    val colSpacingDp = 8.dp
    val cellStrideY = with(density) { (cellHeightDp + rowSpacingDp).toPx() }
    val dragThresholdPx = with(density) { 18.dp.toPx() }

    val maxOccupiedRow = slots.maxOfOrNull { it.row } ?: -1
    val totalRows = if (slots.isEmpty()) 0 else (maxOccupiedRow + 2).coerceAtLeast(3)

    val hoveredTarget: Pair<Int, Int>? = if (draggedKey != null && dragOrigin != null && gridWidthPx > 0) {
        val (origRow, origCol) = dragOrigin!!
        val cellStrideX = (gridWidthPx.toFloat() / columns.coerceAtLeast(1)).coerceAtLeast(1f)
        val targetCol = (origCol + (dragOffset.x / cellStrideX).roundToInt()).coerceIn(0, columns - 1)
        val targetRow = (origRow + (dragOffset.y / cellStrideY).roundToInt()).coerceIn(0, (totalRows - 1).coerceAtLeast(0))
        targetRow to targetCol
    } else {
        null
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState(), enabled = draggedKey == null)
            .padding(top = 8.dp, bottom = 140.dp)
            .testTag("desktop_items_grid")
    ) {
        if (headerContent != null) {
            headerContent()
        }

        if (totalRows > 0) {
            Column(
                verticalArrangement = Arrangement.spacedBy(rowSpacingDp),
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { gridWidthPx = it.width }
            ) {
                for (rowIdx in 0 until totalRows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(colSpacingDp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (colIdx in 0 until columns) {
                            val slot = slotMap[rowIdx to colIdx]
                            val isDragged = slot != null && slot.content.itemKey == draggedKey
                            val isHoveredDropCell = draggedKey != null && hoveredTarget == (rowIdx to colIdx)

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(cellHeightDp)
                                    .zIndex(if (isDragged) 20f else 0f)
                                    .then(
                                        if (isHoveredDropCell && !isDragged) {
                                            Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f),
                                                    shape = RoundedCornerShape(18.dp)
                                                )
                                                .border(
                                                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                                                    RoundedCornerShape(18.dp)
                                                )
                                        } else if (draggedKey != null && slot == null) {
                                            Modifier.border(
                                                BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)
                                                ),
                                                RoundedCornerShape(18.dp)
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
                                if (slot != null) {
                                    val onTapAction: () -> Unit = {
                                        when (val c = slot.content) {
                                            is DesktopGridContent.Folder -> onOpenFolder(c.folder.id)
                                            is DesktopGridContent.AppShortcut -> onLaunchApp(c.shortcut.app)
                                        }
                                    }
                                    val onHoldOptionsAction: () -> Unit = {
                                        when (val c = slot.content) {
                                            is DesktopGridContent.Folder -> onOpenFolder(c.folder.id)
                                            is DesktopGridContent.AppShortcut -> onLongPressApp(c.shortcut.app)
                                        }
                                    }

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                if (isDragged) {
                                                    translationX = dragOffset.x
                                                    translationY = dragOffset.y
                                                    scaleX = 1.08f
                                                    scaleY = 1.08f
                                                    alpha = 0.92f
                                                }
                                            }
                                            .semantics {
                                                onClick {
                                                    onTapAction()
                                                    true
                                                }
                                                onLongClick {
                                                    onHoldOptionsAction()
                                                    true
                                                }
                                            }
                                            .pointerInput(slot.content.itemKey, rowIdx, colIdx, columns, totalRows) {
                                                val touchSlop = viewConfiguration.touchSlop
                                                val longPressMs = viewConfiguration.longPressTimeoutMillis
                                                awaitEachGesture {
                                                    val down = awaitFirstDown(requireUnconsumed = false)
                                                    var isTapCompleted = false
                                                    var isScrollAborted = false
                                                    val startTime = System.currentTimeMillis()

                                                    // Wait for long press or quick tap / scroll
                                                    while (true) {
                                                        val elapsed = System.currentTimeMillis() - startTime
                                                        val remaining = longPressMs - elapsed
                                                        if (remaining <= 0L) break

                                                        val event = withTimeoutOrNull(remaining) {
                                                            awaitPointerEvent()
                                                        } ?: break

                                                        val change = event.changes.firstOrNull { it.id == down.id }
                                                            ?: continue
                                                        if (change.changedToUpIgnoreConsumed()) {
                                                            isTapCompleted = true
                                                            change.consume()
                                                            break
                                                        }
                                                        val dist = hypot(
                                                            (change.position.x - down.position.x).toDouble(),
                                                            (change.position.y - down.position.y).toDouble()
                                                        )
                                                        if (dist > touchSlop) {
                                                            isScrollAborted = true
                                                            break
                                                        }
                                                    }

                                                    if (isTapCompleted) {
                                                        onTapAction()
                                                        return@awaitEachGesture
                                                    }
                                                    if (isScrollAborted) {
                                                        return@awaitEachGesture
                                                    }

                                                    // Long press activated: enter drag-or-options mode
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    draggedKey = slot.content.itemKey
                                                    dragOrigin = rowIdx to colIdx
                                                    dragOffset = Offset.Zero

                                                    var accumulated = Offset.Zero
                                                    var dragFinished = false
                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val change = event.changes.firstOrNull { it.id == down.id }
                                                        if (change == null || !change.pressed) {
                                                            dragFinished = true
                                                            break
                                                        }
                                                        val delta = change.positionChange()
                                                        accumulated += delta
                                                        dragOffset = accumulated
                                                        change.consume()
                                                    }

                                                    if (dragFinished) {
                                                        val movedDist = hypot(accumulated.x, accumulated.y)
                                                        val cellStrideX = (gridWidthPx.toFloat() / columns.coerceAtLeast(1)).coerceAtLeast(1f)
                                                        val dropCol = (colIdx + (accumulated.x / cellStrideX).roundToInt()).coerceIn(0, columns - 1)
                                                        val dropRow = (rowIdx + (accumulated.y / cellStrideY).roundToInt()).coerceIn(0, (totalRows - 1).coerceAtLeast(0))

                                                        draggedKey = null
                                                        dragOrigin = null
                                                        dragOffset = Offset.Zero

                                                        if (movedDist < dragThresholdPx || (dropRow == rowIdx && dropCol == colIdx)) {
                                                            onHoldOptionsAction()
                                                        } else {
                                                            onMoveGridItem(slot.content.itemKey, dropRow, dropCol, columns)
                                                        }
                                                    } else {
                                                        draggedKey = null
                                                        dragOrigin = null
                                                        dragOffset = Offset.Zero
                                                    }
                                                }
                                            }
                                    ) {
                                        if (isDragged) {
                                            Surface(
                                                shape = RoundedCornerShape(18.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
                                                tonalElevation = 6.dp
                                            ) {
                                                DesktopSlotContent(
                                                    content = slot.content,
                                                    activeNotificationPackages = uiState.activeNotificationPackages,
                                                    getIconBitmap = getIconBitmap
                                                )
                                            }
                                        } else {
                                            DesktopSlotContent(
                                                content = slot.content,
                                                activeNotificationPackages = uiState.activeNotificationPackages,
                                                getIconBitmap = getIconBitmap
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopSlotContent(
    content: DesktopGridContent,
    activeNotificationPackages: Set<String>,
    getIconBitmap: (String) -> ImageBitmap?
) {
    when (content) {
        is DesktopGridContent.Folder -> {
            val hasNotif = content.folder.apps.any { it.packageName in activeNotificationPackages }
            DesktopFolderTile(
                folder = content.folder,
                getIconBitmap = getIconBitmap,
                onClick = {},
                hasNotification = hasNotif,
                interactive = false
            )
        }
        is DesktopGridContent.AppShortcut -> {
            val app = content.shortcut.app
            AppGridTile(
                app = app,
                iconBitmap = getIconBitmap(app.packageName),
                onClick = {},
                onLongClick = {},
                hasNotification = app.packageName in activeNotificationPackages,
                interactive = false
            )
        }
    }
}

@Composable
private fun DesktopBackdrop(
    style: WallpaperStyle,
    customWallpaperImage: ImageBitmap?
) {
    when (style) {
        WallpaperStyle.CUSTOM_PHOTO -> {
            if (customWallpaperImage != null) {
                Image(
                    bitmap = customWallpaperImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                TonalBackdropGradient()
            }
        }
        WallpaperStyle.TONAL -> TonalBackdropGradient()
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

@Composable
private fun TonalBackdropGradient() {
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
