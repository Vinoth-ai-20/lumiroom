package com.lumiroom.feature.roomplanner.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import com.lumiroom.feature.roomplanner.domain.geometry.Point2D
import com.lumiroom.feature.roomplanner.domain.geometry.BoundingBox
import com.lumiroom.feature.roomplanner.domain.geometry.LineSegment
import com.lumiroom.feature.roomplanner.domain.command.Command
import com.lumiroom.feature.roomplanner.domain.command.CommandManager
import com.lumiroom.core.database.repository.FurnitureRepository
import com.lumiroom.core.domain.SharedRoomRepository
import com.lumiroom.core.database.model.Furniture
import com.lumiroom.core.database.dao.RoomPlanDao
import com.lumiroom.core.database.entity.RoomPlanEntity
import com.lumiroom.core.database.entity.WallEntity
import com.lumiroom.core.database.entity.DoorEntity
import com.lumiroom.core.database.entity.WindowEntity
import com.lumiroom.core.database.entity.FloorPlanItemEntity
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore

enum class InteractionMode {
    SELECT,
    DRAW_WALL,
    DRAW_DOOR,
    DRAW_WINDOW,
    PLACE_FURNITURE,
    MOVE_FURNITURE,
    MEASURE,
    REMOVE
}

data class Wall(val id: String, val segment: LineSegment, val thicknessCm: Float = 10f)

data class Door(val id: String, val segment: LineSegment, val thicknessCm: Float = 10f, val swingAngle: Float = 90f)

data class Window(val id: String, val segment: LineSegment, val thicknessCm: Float = 10f)

data class Placed2DFurniture(
    val id: String,
    val furnitureId: String,
    val position: Point2D,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val widthCm: Float = 100f,
    val depthCm: Float = 100f,
    val colorHex: Long = 0xFF8B4513,
    val zIndex: Int = 0,
    val hasCollision: Boolean = false
)

data class RoomPlannerUiState(
    val mode: InteractionMode = InteractionMode.SELECT,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val zoom: Float = 1f,
    val gridSizeCm: Float = 10f,
    val walls: List<Wall> = emptyList(),
    val doors: List<Door> = emptyList(),
    val windows: List<Window> = emptyList(),
    val furniture: List<Placed2DFurniture> = emptyList(),
    val currentDrawingStartPoint: Point2D? = null,
    val currentDrawingEndPoint: Point2D? = null,
    val draggedFurnitureId: String? = null,
    val focusedPlacedFurnitureId: String? = null,
    val selectedFurnitureId: String? = null, // catalog item to place
    val deleteConfirmationId: String? = null,
    val deleteConfirmationType: String? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val roomAreaSqM: Float = 0f,
    val roomPerimeterM: Float = 0f
)

@HiltViewModel
class RoomPlannerViewModel @Inject constructor(
    private val furnitureRepository: FurnitureRepository,
    private val roomPlanDao: RoomPlanDao,
    private val sharedRoomRepository: SharedRoomRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val commandManager = CommandManager()

    private val _uiState = MutableStateFlow(RoomPlannerUiState())
    val uiState: StateFlow<RoomPlannerUiState> = _uiState

    fun undo() {
        commandManager.undo()
        updateUndoRedoState()
    }

    fun redo() {
        commandManager.redo()
        updateUndoRedoState()
    }

    private fun updateUndoRedoState() {
        val walls = _uiState.value.walls
        val perimeter = walls.sumOf { it.segment.length.toDouble() }.toFloat() / 100f // convert cm to m
        
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        walls.forEach { w ->
            if (w.segment.start.x < minX) minX = w.segment.start.x
            if (w.segment.start.x > maxX) maxX = w.segment.start.x
            if (w.segment.start.y < minY) minY = w.segment.start.y
            if (w.segment.start.y > maxY) maxY = w.segment.start.y
            if (w.segment.end.x < minX) minX = w.segment.end.x
            if (w.segment.end.x > maxX) maxX = w.segment.end.x
            if (w.segment.end.y < minY) minY = w.segment.end.y
            if (w.segment.end.y > maxY) maxY = w.segment.end.y
        }
        
        val area = if (walls.isNotEmpty()) {
            val w = (maxX - minX) / 100f
            val h = (maxY - minY) / 100f
            w * h
        } else 0f

        _uiState.update { 
            it.copy(
                canUndo = commandManager.canUndo, 
                canRedo = commandManager.canRedo,
                roomPerimeterM = perimeter,
                roomAreaSqM = area
            ) 
        }
    }

    private var preDragFurnitureSnapshot: List<Placed2DFurniture>? = null

    private fun executeModelChange(
        oldWalls: List<Wall>, newWalls: List<Wall>,
        oldDoors: List<Door>, newDoors: List<Door>,
        oldWindows: List<Window>, newWindows: List<Window>,
        oldFurniture: List<Placed2DFurniture>, newFurniture: List<Placed2DFurniture>
    ) {
        val command = object : Command {
            override fun execute() {
                _uiState.update { 
                    it.copy(walls = newWalls, doors = newDoors, windows = newWindows, furniture = newFurniture)
                }
                savePlan()
            }
            override fun undo() {
                _uiState.update {
                    it.copy(walls = oldWalls, doors = oldDoors, windows = oldWindows, furniture = oldFurniture)
                }
                // also cancel dragging if undone while dragging (unlikely, but safe)
                _uiState.update { it.copy(draggedFurnitureId = null) }
                savePlan()
            }
        }
        commandManager.execute(command)
        updateUndoRedoState()
    }

    val catalog: StateFlow<List<Furniture>> = furnitureRepository.getAllFurniture()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentPlanId: String = savedStateHandle.get<String>("planId") ?: java.util.UUID.randomUUID().toString()

    init {
        val planId = savedStateHandle.get<String>("planId")
        if (planId != null) {
            currentPlanId = planId
            viewModelScope.launch {
                sharedRoomRepository.observeRoomState(planId).collect { planDetails ->
                    if (planDetails != null) {
                        _uiState.update { state ->
                            // When the DB emits, we only overwrite if we aren't actively dragging
                            if (state.draggedFurnitureId == null) {
                                state.copy(
                                    walls = planDetails.walls.map { Wall(it.id, LineSegment(Point2D(it.startX, it.startY), Point2D(it.endX, it.endY)), it.thicknessCm) },
                                    doors = planDetails.doors.map { Door(it.id, LineSegment(Point2D(it.startX, it.startY), Point2D(it.endX, it.endY)), it.thicknessCm, 90f) },
                                    windows = planDetails.windows.map { Window(it.id, LineSegment(Point2D(it.startX, it.startY), Point2D(it.endX, it.endY)), it.thicknessCm) },
                                    furniture = planDetails.furniture.map { Placed2DFurniture(it.id, it.furnitureId, Point2D(it.positionX, it.positionY), it.rotation, it.scaleX) }
                                )
                            } else {
                                state
                            }
                        }
                        updateUndoRedoState()
                    }
                }
            }
        }
    }

    fun setInteractionMode(mode: InteractionMode) {
        _uiState.update { it.copy(mode = mode, currentDrawingStartPoint = null, currentDrawingEndPoint = null, selectedFurnitureId = null, draggedFurnitureId = null) }
    }

    fun selectFurnitureFromCatalog(furnitureId: String) {
        _uiState.update { it.copy(mode = InteractionMode.PLACE_FURNITURE, selectedFurnitureId = furnitureId) }
    }

    fun updateTransform(panX: Float, panY: Float, zoom: Float) {
        _uiState.update { it.copy(panX = panX, panY = panY, zoom = zoom) }
    }

    fun resetZoom() {
        _uiState.update { it.copy(panX = 0f, panY = 0f, zoom = 1f) }
    }

    fun fitToScreen(screenWidth: Float, screenHeight: Float) {
        val state = _uiState.value
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        val points = mutableListOf<Point2D>()
        state.walls.forEach { points.add(it.segment.start); points.add(it.segment.end) }
        state.doors.forEach { points.add(it.segment.start); points.add(it.segment.end) }
        state.windows.forEach { points.add(it.segment.start); points.add(it.segment.end) }
        state.furniture.forEach { points.add(it.position) }

        if (points.isEmpty()) {
            resetZoom()
            return
        }

        points.forEach { p ->
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }

        val width = maxX - minX
        val height = maxY - minY

        // Add 20% padding
        val paddedWidth = width * 1.2f
        val paddedHeight = height * 1.2f

        val zoomX = if (paddedWidth > 0) screenWidth / paddedWidth else 1f
        val zoomY = if (paddedHeight > 0) screenHeight / paddedHeight else 1f
        val newZoom = minOf(zoomX, zoomY).coerceIn(0.1f, 5f)

        val centerX = minX + width / 2f
        val centerY = minY + height / 2f

        val newPanX = (screenWidth / 2f) - (centerX * newZoom)
        val newPanY = (screenHeight / 2f) - (centerY * newZoom)

        _uiState.update { it.copy(panX = newPanX, panY = newPanY, zoom = newZoom) }
    }

    private fun getSnappedPoint(point: Point2D, startPoint: Point2D? = null): Point2D {
        val state = _uiState.value
        val snapThreshold = 20f / state.zoom

        if (state.mode == InteractionMode.DRAW_DOOR || state.mode == InteractionMode.DRAW_WINDOW) {
            // Snap to nearest wall line
            var nearestPoint: Point2D? = null
            var minDistance = snapThreshold
            for (wall in state.walls) {
                val distance = wall.segment.distanceToPoint(point)
                if (distance < minDistance) {
                    minDistance = distance
                    nearestPoint = wall.segment.projectPoint(point)
                }
            }
            if (nearestPoint != null) {
                return nearestPoint
            }
        }

        if (state.mode == InteractionMode.DRAW_WALL || state.mode == InteractionMode.MEASURE) {
            // 1. Endpoint Snapping
            for (wall in state.walls) {
                if (point.distanceTo(wall.segment.start) < snapThreshold) return wall.segment.start
                if (point.distanceTo(wall.segment.end) < snapThreshold) return wall.segment.end
            }

            // 2. Orthogonal Snapping
            if (startPoint != null) {
                val dx = kotlin.math.abs(point.x - startPoint.x)
                val dy = kotlin.math.abs(point.y - startPoint.y)
                if (dx < snapThreshold) {
                    return Point2D(startPoint.x, point.y)
                }
                if (dy < snapThreshold) {
                    return Point2D(point.x, startPoint.y)
                }
            }
        }
        
        return point
    }

    fun onCanvasPointerDown(point: Point2D) {
        val state = _uiState.value
        if (state.mode == InteractionMode.DRAW_WALL || 
            state.mode == InteractionMode.DRAW_DOOR || 
            state.mode == InteractionMode.DRAW_WINDOW || 
            state.mode == InteractionMode.MEASURE) {
            val snapped = getSnappedPoint(point)
            _uiState.update { it.copy(currentDrawingStartPoint = snapped, currentDrawingEndPoint = snapped) }
        } else if (state.mode == InteractionMode.PLACE_FURNITURE && state.selectedFurnitureId != null) {
            val newFurniture = Placed2DFurniture(
                id = java.util.UUID.randomUUID().toString(),
                furnitureId = state.selectedFurnitureId,
                position = point
            )
            executeModelChange(
                oldWalls = state.walls, newWalls = state.walls,
                oldDoors = state.doors, newDoors = state.doors,
                oldWindows = state.windows, newWindows = state.windows,
                oldFurniture = state.furniture, newFurniture = state.furniture + newFurniture
            )
            _uiState.update { it.copy(mode = InteractionMode.SELECT, selectedFurnitureId = null) }
        } else if (state.mode == InteractionMode.SELECT) {
            val touchedFurniture = state.furniture.reversed().find { f ->
                point.distanceTo(f.position) < (20f / state.zoom)
            }
            if (touchedFurniture != null) {
                preDragFurnitureSnapshot = state.furniture
                _uiState.update { it.copy(draggedFurnitureId = touchedFurniture.id, focusedPlacedFurnitureId = touchedFurniture.id) }
            } else {
                _uiState.update { it.copy(focusedPlacedFurnitureId = null) }
            }
        } else if (state.mode == InteractionMode.REMOVE) {
            val hitThreshold = 20f / state.zoom
            
            // Priority: Furniture -> Window -> Door -> Wall
            val touchedFurniture = state.furniture.reversed().find { f -> point.distanceTo(f.position) < hitThreshold }
            if (touchedFurniture != null) {
                _uiState.update { it.copy(deleteConfirmationId = touchedFurniture.id, deleteConfirmationType = "Furniture") }
                return
            }

            val touchedWindow = state.windows.find { w -> w.segment.distanceToPoint(point) < hitThreshold }
            if (touchedWindow != null) {
                _uiState.update { it.copy(deleteConfirmationId = touchedWindow.id, deleteConfirmationType = "Window") }
                return
            }

            val touchedDoor = state.doors.find { d -> d.segment.distanceToPoint(point) < hitThreshold }
            if (touchedDoor != null) {
                _uiState.update { it.copy(deleteConfirmationId = touchedDoor.id, deleteConfirmationType = "Door") }
                return
            }

            val touchedWall = state.walls.find { w -> w.segment.distanceToPoint(point) < hitThreshold }
            if (touchedWall != null) {
                _uiState.update { it.copy(deleteConfirmationId = touchedWall.id, deleteConfirmationType = "Wall") }
                return
            }
        }
    }

    private fun Placed2DFurniture.getBoundingBox(): BoundingBox {
        val w = widthCm * scale / 2f
        val h = depthCm * scale / 2f
        return BoundingBox(
            left = position.x - w,
            top = position.y - h,
            right = position.x + w,
            bottom = position.y + h
        )
    }

    private fun checkCollisions(furniture: List<Placed2DFurniture>): List<Placed2DFurniture> {
        val boxes = furniture.associate { it.id to it.getBoundingBox() }
        return furniture.map { f ->
            val myBox = boxes[f.id]!!
            val hasCol = furniture.any { other ->
                other.id != f.id && myBox.intersects(boxes[other.id]!!)
            }
            f.copy(hasCollision = hasCol)
        }
    }

    fun onCanvasPointerMove(point: Point2D) {
        val state = _uiState.value
        if ((state.mode == InteractionMode.DRAW_WALL || 
             state.mode == InteractionMode.DRAW_DOOR || 
             state.mode == InteractionMode.DRAW_WINDOW || 
             state.mode == InteractionMode.MEASURE) && state.currentDrawingStartPoint != null) {
            val snapped = getSnappedPoint(point, state.currentDrawingStartPoint)
            _uiState.update { it.copy(currentDrawingEndPoint = snapped) }
        } else if (state.mode == InteractionMode.SELECT && state.draggedFurnitureId != null) {
            _uiState.update { currentState ->
                val newFurnitureList = currentState.furniture.map { f ->
                    if (f.id == state.draggedFurnitureId) f.copy(position = point) else f
                }
                currentState.copy(furniture = checkCollisions(newFurnitureList))
            }
        }
    }

    fun onCanvasPointerUp(point: Point2D) {
        val state = _uiState.value
        val start = state.currentDrawingStartPoint
        val end = state.currentDrawingEndPoint

        if (state.mode == InteractionMode.DRAW_WALL) {
            if (start != null && end != null && start.distanceTo(end) > 5f) {
                val newWall = Wall(java.util.UUID.randomUUID().toString(), LineSegment(start, end))
                executeModelChange(
                    oldWalls = state.walls, newWalls = state.walls + newWall,
                    oldDoors = state.doors, newDoors = state.doors,
                    oldWindows = state.windows, newWindows = state.windows,
                    oldFurniture = state.furniture, newFurniture = state.furniture
                )
                _uiState.update { it.copy(currentDrawingStartPoint = null, currentDrawingEndPoint = null) }
            } else {
                _uiState.update { it.copy(currentDrawingStartPoint = null, currentDrawingEndPoint = null) }
            }
        } else if (state.mode == InteractionMode.DRAW_DOOR) {
            if (start != null && end != null && start.distanceTo(end) > 5f) {
                val newDoor = Door(java.util.UUID.randomUUID().toString(), LineSegment(start, end))
                executeModelChange(
                    oldWalls = state.walls, newWalls = state.walls,
                    oldDoors = state.doors, newDoors = state.doors + newDoor,
                    oldWindows = state.windows, newWindows = state.windows,
                    oldFurniture = state.furniture, newFurniture = state.furniture
                )
                _uiState.update { it.copy(currentDrawingStartPoint = null, currentDrawingEndPoint = null) }
            } else {
                _uiState.update { it.copy(currentDrawingStartPoint = null, currentDrawingEndPoint = null) }
            }
        } else if (state.mode == InteractionMode.DRAW_WINDOW) {
            if (start != null && end != null && start.distanceTo(end) > 5f) {
                val newWindow = Window(java.util.UUID.randomUUID().toString(), LineSegment(start, end))
                executeModelChange(
                    oldWalls = state.walls, newWalls = state.walls,
                    oldDoors = state.doors, newDoors = state.doors,
                    oldWindows = state.windows, newWindows = state.windows + newWindow,
                    oldFurniture = state.furniture, newFurniture = state.furniture
                )
                _uiState.update { it.copy(currentDrawingStartPoint = null, currentDrawingEndPoint = null) }
            } else {
                _uiState.update { it.copy(currentDrawingStartPoint = null, currentDrawingEndPoint = null) }
            }
        } else if (state.mode == InteractionMode.MEASURE) {
            _uiState.update { it.copy(currentDrawingStartPoint = null, currentDrawingEndPoint = null) }
        } else if (state.mode == InteractionMode.SELECT) {
            val snapshot = preDragFurnitureSnapshot
            if (state.draggedFurnitureId != null && snapshot != null) {
                executeModelChange(
                    oldWalls = state.walls, newWalls = state.walls,
                    oldDoors = state.doors, newDoors = state.doors,
                    oldWindows = state.windows, newWindows = state.windows,
                    oldFurniture = snapshot, newFurniture = state.furniture
                )
            }
            preDragFurnitureSnapshot = null
            _uiState.update { it.copy(draggedFurnitureId = null) }
        }
    }

    fun savePlan(planName: String = "My Plan") {
        val state = _uiState.value
        val planId = currentPlanId
        val plan = RoomPlanEntity(
            id = planId,
            name = planName,
            updatedAt = System.currentTimeMillis()
        )
        val wallEntities = state.walls.map { w ->
            WallEntity(
                id = w.id,
                planId = planId,
                startX = w.segment.start.x,
                startY = w.segment.start.y,
                endX = w.segment.end.x,
                endY = w.segment.end.y,
                thicknessCm = w.thicknessCm
            )
        }
        val itemEntities = state.furniture.map { f ->
            FloorPlanItemEntity(
                id = f.id,
                planId = planId,
                furnitureId = f.furnitureId,
                posX = f.position.x,
                posY = f.position.y,
                rotation = f.rotation,
                scaleX = f.scale,
                scaleY = f.scale
            )
        }
        val doorEntities = state.doors.map { d ->
            DoorEntity(
                id = d.id,
                planId = planId,
                startX = d.segment.start.x,
                startY = d.segment.start.y,
                endX = d.segment.end.x,
                endY = d.segment.end.y,
                thicknessCm = d.thicknessCm,
                swingAngle = d.swingAngle
            )
        }
        val windowEntities = state.windows.map { w ->
            WindowEntity(
                id = w.id,
                planId = planId,
                startX = w.segment.start.x,
                startY = w.segment.start.y,
                endX = w.segment.end.x,
                endY = w.segment.end.y,
                thicknessCm = w.thicknessCm
            )
        }
        viewModelScope.launch {
            roomPlanDao.saveFullPlan(plan, wallEntities, itemEntities, doorEntities, windowEntities)
        }
    }

    fun rotateFocusedFurniture(deltaDegrees: Float) {
        val state = _uiState.value
        val id = state.focusedPlacedFurnitureId ?: return
        val newFurniture = state.furniture.map { f ->
            if (f.id == id) f.copy(rotation = f.rotation + deltaDegrees) else f
        }
        executeModelChange(
            oldWalls = state.walls, newWalls = state.walls,
            oldDoors = state.doors, newDoors = state.doors,
            oldWindows = state.windows, newWindows = state.windows,
            oldFurniture = state.furniture, newFurniture = newFurniture
        )
    }

    fun scaleFocusedFurniture(deltaScale: Float) {
        val state = _uiState.value
        val id = state.focusedPlacedFurnitureId ?: return
        val newFurniture = state.furniture.map { f ->
            if (f.id == id) f.copy(scale = (f.scale + deltaScale).coerceIn(0.5f, 3.0f)) else f
        }
        executeModelChange(
            oldWalls = state.walls, newWalls = state.walls,
            oldDoors = state.doors, newDoors = state.doors,
            oldWindows = state.windows, newWindows = state.windows,
            oldFurniture = state.furniture, newFurniture = newFurniture
        )
    }

    fun bringFurnitureForward() {
        val state = _uiState.value
        val id = state.focusedPlacedFurnitureId ?: return
        val newFurniture = state.furniture.map { f ->
            if (f.id == id) f.copy(zIndex = f.zIndex + 1) else f
        }
        executeModelChange(
            oldWalls = state.walls, newWalls = state.walls,
            oldDoors = state.doors, newDoors = state.doors,
            oldWindows = state.windows, newWindows = state.windows,
            oldFurniture = state.furniture, newFurniture = newFurniture
        )
    }

    fun sendFurnitureBackward() {
        val state = _uiState.value
        val id = state.focusedPlacedFurnitureId ?: return
        val newFurniture = state.furniture.map { f ->
            if (f.id == id) f.copy(zIndex = f.zIndex - 1) else f
        }
        executeModelChange(
            oldWalls = state.walls, newWalls = state.walls,
            oldDoors = state.doors, newDoors = state.doors,
            oldWindows = state.windows, newWindows = state.windows,
            oldFurniture = state.furniture, newFurniture = newFurniture
        )
    }

    fun deleteFocusedItem() {
        val id = _uiState.value.focusedPlacedFurnitureId ?: return
        _uiState.update { state ->
            state.copy(
                furniture = state.furniture.filterNot { it.id == id },
                focusedPlacedFurnitureId = null
            )
        }
    }

    fun exportToBitmap(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = android.graphics.Bitmap.createBitmap(1024, 1024, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                val paint = android.graphics.Paint().apply {
                    setColor(android.graphics.Color.BLUE)
                    strokeWidth = 10f
                    style = android.graphics.Paint.Style.STROKE
                }
                val state = _uiState.value
                for (wall in state.walls) {
                    canvas.drawLine(wall.segment.start.x, wall.segment.start.y, wall.segment.end.x, wall.segment.end.y, paint)
                }
                val filename = "Lumiroom_Export_${System.currentTimeMillis()}.png"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Lumiroom")
                    }
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Exported to Gallery", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Failed to save image", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setMode(newMode: InteractionMode) {
        _uiState.update { it.copy(mode = newMode) }
    }

    fun confirmDelete() {
        val state = _uiState.value
        val id = state.deleteConfirmationId ?: return
        
        val newWalls = if (state.deleteConfirmationType == "Wall") state.walls.filter { it.id != id } else state.walls
        val newDoors = if (state.deleteConfirmationType == "Door") state.doors.filter { it.id != id } else state.doors
        val newWindows = if (state.deleteConfirmationType == "Window") state.windows.filter { it.id != id } else state.windows
        val newFurniture = if (state.deleteConfirmationType == "Furniture") state.furniture.filter { it.id != id } else state.furniture
        
        executeModelChange(
            oldWalls = state.walls, newWalls = newWalls,
            oldDoors = state.doors, newDoors = newDoors,
            oldWindows = state.windows, newWindows = newWindows,
            oldFurniture = state.furniture, newFurniture = newFurniture
        )
        
        _uiState.update { 
            it.copy(deleteConfirmationId = null, deleteConfirmationType = null)
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(deleteConfirmationId = null, deleteConfirmationType = null) }
    }
}
