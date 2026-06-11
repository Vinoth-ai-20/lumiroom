package com.lumiroom.feature.roomplanner.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import com.lumiroom.feature.roomplanner.domain.geometry.Point2D
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomPlannerScreen(
    viewModel: RoomPlannerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToAr: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("2D Room Planner") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.undo() }, enabled = uiState.canUndo) {
                        Text("Undo")
                    }
                    TextButton(onClick = { viewModel.redo() }, enabled = uiState.canRedo) {
                        Text("Redo")
                    }
                    TextButton(onClick = { viewModel.exportToBitmap(context) }) {
                        Text("Export")
                    }
                    TextButton(onClick = { viewModel.savePlan() }) {
                        Text("Save")
                    }
                    TextButton(onClick = onNavigateToAr) {
                        Text("AR View")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.mode == InteractionMode.SELECT,
                        onClick = { viewModel.setInteractionMode(InteractionMode.SELECT) },
                        label = { Text("Select") }
                    )
                    FilterChip(
                        selected = uiState.mode == InteractionMode.DRAW_WALL,
                        onClick = { viewModel.setInteractionMode(InteractionMode.DRAW_WALL) },
                        label = { Text("Draw Wall") }
                    )
                    FilterChip(
                        selected = uiState.mode == InteractionMode.PLACE_FURNITURE,
                        onClick = { viewModel.setInteractionMode(InteractionMode.PLACE_FURNITURE) },
                        label = { Text("Furniture") }
                    )
                    FilterChip(
                        selected = uiState.mode == InteractionMode.DRAW_DOOR,
                        onClick = { viewModel.setInteractionMode(InteractionMode.DRAW_DOOR) },
                        label = { Text("Door") }
                    )
                    FilterChip(
                        selected = uiState.mode == InteractionMode.DRAW_WINDOW,
                        onClick = { viewModel.setInteractionMode(InteractionMode.DRAW_WINDOW) },
                        label = { Text("Window") }
                    )
                    FilterChip(
                        selected = uiState.mode == InteractionMode.MEASURE,
                        onClick = { viewModel.setInteractionMode(InteractionMode.MEASURE) },
                        label = { Text("Measure") }
                    )
                    FilterChip(
                        selected = uiState.mode == InteractionMode.REMOVE,
                        onClick = { viewModel.setInteractionMode(InteractionMode.REMOVE) },
                        label = { Text("Remove") }
                    )
                }
            }
        }
    ) { paddingValues ->
        
        if (uiState.deleteConfirmationId != null) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDelete() },
                title = { Text("Confirm Deletion") },
                text = { Text("Are you sure you want to delete this ${uiState.deleteConfirmationType}?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmDelete() }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDelete() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            val canvasWidthConstraint = constraints.maxWidth.toFloat()
            val canvasHeightConstraint = constraints.maxHeight.toFloat()

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(uiState.mode, uiState.selectedFurnitureId) {
                        if (uiState.mode == InteractionMode.SELECT) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val currentState = viewModel.uiState.value
                                val worldX = (down.position.x - currentState.panX) / currentState.zoom
                                val worldY = (down.position.y - currentState.panY) / currentState.zoom
                                viewModel.onCanvasPointerDown(Point2D(worldX, worldY))
                                
                                do {
                                    val event = awaitPointerEvent()
                                    val canceled = event.changes.any { it.isConsumed }
                                    if (!canceled) {
                                        val latestState = viewModel.uiState.value
                                        val isDraggingFurniture = latestState.draggedFurnitureId != null
                                        if (isDraggingFurniture) {
                                            val change = event.changes.firstOrNull()
                                            if (change != null) {
                                                val newWorldX = (change.position.x - latestState.panX) / latestState.zoom
                                                val newWorldY = (change.position.y - latestState.panY) / latestState.zoom
                                                viewModel.onCanvasPointerMove(Point2D(newWorldX, newWorldY))
                                                change.consume()
                                            }
                                        } else {
                                            val zoomChange = event.calculateZoom()
                                            val panChange = event.calculatePan()
                                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                                viewModel.updateTransform(
                                                    panX = latestState.panX + panChange.x,
                                                    panY = latestState.panY + panChange.y,
                                                    zoom = (latestState.zoom * zoomChange).coerceIn(0.1f, 5f)
                                                )
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    }
                                } while (!canceled && event.changes.any { it.pressed })
                                
                                viewModel.onCanvasPointerUp(Point2D(0f, 0f))
                            }
                        } else if (uiState.mode == InteractionMode.DRAW_WALL || 
                                   uiState.mode == InteractionMode.DRAW_DOOR || 
                                   uiState.mode == InteractionMode.DRAW_WINDOW || 
                                   uiState.mode == InteractionMode.MEASURE) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    // Inverse transform point to world coords
                                    val worldX = (offset.x - uiState.panX) / uiState.zoom
                                    val worldY = (offset.y - uiState.panY) / uiState.zoom
                                    viewModel.onCanvasPointerDown(Point2D(worldX, worldY))
                                },
                                onDrag = { change, _ ->
                                    val offset = change.position
                                    val worldX = (offset.x - uiState.panX) / uiState.zoom
                                    val worldY = (offset.y - uiState.panY) / uiState.zoom
                                    viewModel.onCanvasPointerMove(Point2D(worldX, worldY))
                                },
                                onDragEnd = {
                                    viewModel.onCanvasPointerUp(Point2D(0f, 0f)) // Actual coord doesn't matter here
                                }
                            )
                        } else if (uiState.mode == InteractionMode.PLACE_FURNITURE) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val worldX = (offset.x - uiState.panX) / uiState.zoom
                                    val worldY = (offset.y - uiState.panY) / uiState.zoom
                                    viewModel.onCanvasPointerDown(Point2D(worldX, worldY))
                                },
                                onDrag = { _, _ -> },
                                onDragEnd = { }
                            )
                        }
                    }
            ) {
                withTransform({
                    translate(uiState.panX, uiState.panY)
                    scale(uiState.zoom, uiState.zoom, pivot = Offset.Zero)
                }) {
                    // Draw Grid
                    val canvasWidth = size.width / uiState.zoom
                    val canvasHeight = size.height / uiState.zoom
                    val gridSpacing = uiState.gridSizeCm * 10f // 10px per 10cm = 1px per cm
                    
                    // Simple infinite grid visual (visible area)
                    val startX = (-uiState.panX / uiState.zoom).toInt() / gridSpacing.toInt() * gridSpacing
                    val startY = (-uiState.panY / uiState.zoom).toInt() / gridSpacing.toInt() * gridSpacing

                    for (x in generateSequence(startX) { it + gridSpacing }.takeWhile { it < startX + canvasWidth }) {
                        drawLine(Color.LightGray.copy(alpha = 0.5f), Offset(x, startY), Offset(x, startY + canvasHeight), 1f / uiState.zoom)
                    }
                    for (y in generateSequence(startY) { it + gridSpacing }.takeWhile { it < startY + canvasHeight }) {
                        drawLine(Color.LightGray.copy(alpha = 0.5f), Offset(startX, y), Offset(startX + canvasWidth, y), 1f / uiState.zoom)
                    }

                    // Draw completed walls
                    for (wall in uiState.walls) {
                        drawLine(
                            color = Color.DarkGray,
                            start = Offset(wall.segment.start.x, wall.segment.start.y),
                            end = Offset(wall.segment.end.x, wall.segment.end.y),
                            strokeWidth = wall.thicknessCm,
                            cap = StrokeCap.Round
                        )
                    }

                    // Draw completed doors
                    for (door in uiState.doors) {
                        drawLine(
                            color = Color.Cyan,
                            start = Offset(door.segment.start.x, door.segment.start.y),
                            end = Offset(door.segment.end.x, door.segment.end.y),
                            strokeWidth = door.thicknessCm,
                            cap = StrokeCap.Round
                        )
                    }

                    // Draw completed windows
                    for (window in uiState.windows) {
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(window.segment.start.x, window.segment.start.y),
                            end = Offset(window.segment.end.x, window.segment.end.y),
                            strokeWidth = window.thicknessCm,
                            cap = StrokeCap.Round
                        )
                    }

                    // Draw current drawing action
                    val startPt = uiState.currentDrawingStartPoint
                    val endPt = uiState.currentDrawingEndPoint
                    if (startPt != null && endPt != null) {
                        val color = when(uiState.mode) {
                            InteractionMode.DRAW_WALL -> Color.Blue.copy(alpha = 0.7f)
                            InteractionMode.DRAW_DOOR -> Color.Cyan.copy(alpha = 0.7f)
                            InteractionMode.DRAW_WINDOW -> Color.LightGray.copy(alpha = 0.7f)
                            InteractionMode.MEASURE -> Color.Red
                            else -> Color.Blue.copy(alpha = 0.7f)
                        }
                        drawLine(
                            color = color,
                            start = Offset(startPt.x, startPt.y),
                            end = Offset(endPt.x, endPt.y),
                            strokeWidth = if (uiState.mode == InteractionMode.MEASURE) 5f else 10f,
                            cap = StrokeCap.Round
                        )
                        if (uiState.mode == InteractionMode.MEASURE) {
                            val distanceCm = startPt.distanceTo(endPt)
                            val text = "${distanceCm.roundToInt()} cm"
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint().apply {
                                    textSize = 40f / uiState.zoom
                                    setColor(android.graphics.Color.RED)
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                                canvas.nativeCanvas.drawText(
                                    text,
                                    (startPt.x + endPt.x) / 2f,
                                    (startPt.y + endPt.y) / 2f - (20f / uiState.zoom),
                                    paint
                                )
                            }
                        }
                    }

                    // Draw Placed Furniture
                    for (f in uiState.furniture.sortedBy { it.zIndex }) {
                        withTransform({
                            translate(f.position.x, f.position.y)
                            rotate(f.rotation)
                            scale(f.scale, f.scale)
                        }) {
                            val isFocused = f.id == uiState.focusedPlacedFurnitureId
                            val fillColor = if (f.hasCollision) Color(0xFFFFCDD2) else if (isFocused) Color(0xFFE3F2FD) else Color(0xFFB0BEC5)
                            val outlineColor = if (f.hasCollision) Color.Red else Color.Blue
                            
                            drawRect(
                                color = fillColor,
                                topLeft = Offset(-f.widthCm/2, -f.depthCm/2),
                                size = androidx.compose.ui.geometry.Size(f.widthCm, f.depthCm)
                            )
                            
                            if (isFocused || f.hasCollision) {
                                // Draw bounding box outline
                                drawRect(
                                    color = outlineColor,
                                    topLeft = Offset(-f.widthCm/2, -f.depthCm/2),
                                    size = androidx.compose.ui.geometry.Size(f.widthCm, f.depthCm),
                                    style = Stroke(width = 2f / uiState.zoom)
                                )
                                // Draw corner handles
                                val handleRadius = 5f / uiState.zoom
                                drawCircle(outlineColor, radius = handleRadius, center = Offset(-f.widthCm/2, -f.depthCm/2))
                                drawCircle(outlineColor, radius = handleRadius, center = Offset(f.widthCm/2, -f.depthCm/2))
                                drawCircle(outlineColor, radius = handleRadius, center = Offset(-f.widthCm/2, f.depthCm/2))
                                drawCircle(outlineColor, radius = handleRadius, center = Offset(f.widthCm/2, f.depthCm/2))
                            }
                        }
                    }
                }
            }
            
            // Area and Perimeter Overlay
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Area: ${"%.1f".format(uiState.roomAreaSqM)} m²", style = MaterialTheme.typography.bodyMedium)
                    Text("Perimeter: ${"%.1f".format(uiState.roomPerimeterM)} m", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Viewport Controls Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallFloatingActionButton(onClick = { viewModel.fitToScreen(canvasWidthConstraint, canvasHeightConstraint) }) {
                    Text("Fit", modifier = Modifier.padding(8.dp))
                }
                SmallFloatingActionButton(onClick = { viewModel.resetZoom() }) {
                    Text("1:1", modifier = Modifier.padding(8.dp))
                }
            }
            
            AnimatedVisibility(
                visible = uiState.focusedPlacedFurnitureId != null,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { viewModel.rotateFocusedFurniture(-15f) }) { Text("⟲") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { viewModel.rotateFocusedFurniture(15f) }) { Text("⟳") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { viewModel.scaleFocusedFurniture(-0.1f) }) { Text("-") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { viewModel.scaleFocusedFurniture(0.1f) }) { Text("+") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { viewModel.bringFurnitureForward() }) { Text("↑") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { viewModel.sendFurnitureBackward() }) { Text("↓") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { viewModel.deleteFocusedItem() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
                }
            }

            // If in PLACE_FURNITURE mode and no item is selected, show catalog
            if (uiState.mode == InteractionMode.PLACE_FURNITURE && uiState.selectedFurnitureId == null) {
                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp).background(Color.White, shape = MaterialTheme.shapes.medium)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Select Furniture to Place", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        // Display simple list
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (item in catalog.take(3)) {
                                Button(onClick = { viewModel.selectFurnitureFromCatalog(item.id) }) {
                                    Text(item.name)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
