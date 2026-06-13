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


import com.lumiroom.core.database.repository.FurnitureRepository
import com.lumiroom.core.domain.SharedRoomRepository
import com.lumiroom.core.domain.RoomStateManager
import com.lumiroom.core.domain.TransformManager
import com.lumiroom.core.domain.model.*
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
import com.lumiroom.feature.voice.CommandParser
import com.lumiroom.feature.voice.VoiceCommand
import com.lumiroom.feature.voice.VoiceCommandExecutor
import com.lumiroom.feature.voice.VoiceCommandManager
import com.lumiroom.feature.voice.VoiceResult

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

enum class RoomLayer {
    STRUCTURAL,
    FURNITURE,
    LABELS
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
    val hasCollision: Boolean = false,
    val priceEstimate: Double? = null
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
    val dragOffset: Point2D? = null,
    val focusedPlacedFurnitureIds: Set<String> = emptySet(), val isMultiSelectMode: Boolean = false,
    val selectedFurnitureId: String? = null, // catalog item to place
    val deleteConfirmationId: String? = null,
    val deleteConfirmationType: String? = null,
    val visibleLayers: Set<RoomLayer> = setOf(RoomLayer.STRUCTURAL, RoomLayer.FURNITURE, RoomLayer.LABELS),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val roomAreaSqM: Float = 0f,
    val roomPerimeterM: Float = 0f,
    val snapToWalls: Boolean = true,
    val snapToGrid: Boolean = true,
    val isVoiceListening: Boolean = false,
    val voiceTranscript: String? = null,
    val errorMessage: String? = null,
    val measurementUnit: String = "m",
    val totalPriceEstimate: Double = 0.0
)

@HiltViewModel
class RoomPlannerViewModel @Inject constructor(
    private val furnitureRepository: FurnitureRepository,
    private val roomPlanDao: RoomPlanDao,
    private val sharedRoomRepository: SharedRoomRepository,
    private val roomStateManager: RoomStateManager,
    private val transformManager: TransformManager,
    private val voiceCommandManager: VoiceCommandManager,
    private val commandParser: CommandParser,
    private val savedStateHandle: SavedStateHandle,
    private val preferencesDataSource: com.lumiroom.core.datastore.AppPreferencesDataSource
) : ViewModel(), VoiceCommandExecutor {


    private val _uiState = MutableStateFlow(RoomPlannerUiState())
    val uiState: StateFlow<RoomPlannerUiState> = _uiState

    fun undo() {
        roomStateManager.undo()
    }

    fun redo() {
        roomStateManager.redo()
    }


    private var preDragFurnitureSnapshot: List<Placed2DFurniture>? = null

    private fun updateAreaAndPerimeter(walls: List<Wall>) {
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
                roomPerimeterM = perimeter,
                roomAreaSqM = area
            ) 
        }
    }

    private fun updateRoomState(
        walls: List<Wall>? = null,
        doors: List<Door>? = null,
        windows: List<Window>? = null,
        furniture: List<Placed2DFurniture>? = null
    ) {
        roomStateManager.updateState { model ->
            var newModel = model
            if (walls != null) {
                newModel = newModel.copy(walls = walls.map { w -> RoomWall(w.id, w.segment.start.x / 100f, w.segment.start.y / 100f, w.segment.end.x / 100f, w.segment.end.y / 100f, w.thicknessCm, 250f) })
            }
            if (doors != null) {
                newModel = newModel.copy(doors = doors.map { d -> RoomDoor(d.id, d.segment.start.x / 100f, d.segment.start.y / 100f, d.segment.end.x / 100f, d.segment.end.y / 100f, d.thicknessCm, 200f, d.swingAngle) })
            }
            if (windows != null) {
                newModel = newModel.copy(windows = windows.map { w -> RoomWindow(w.id, w.segment.start.x / 100f, w.segment.start.y / 100f, w.segment.end.x / 100f, w.segment.end.y / 100f, w.thicknessCm, 120f, 80f) })
            }
            if (furniture != null) {
                val oldMap = model.furniture.associateBy { it.id }
                newModel = newModel.copy(furniture = furniture.map { f ->
                    val oldF = oldMap[f.id]
                    val catalogF = if (oldF == null) catalog.value.find { it.id == f.furnitureId } else null
                    RoomFurniture(
                        id = f.id,
                        furnitureId = f.furnitureId,
                        positionX = f.position.x / 100f,
                        positionY = f.position.y / 100f,
                        rotation = f.rotation,
                        scaleX = f.scale,
                        scaleY = f.scale,
                        widthCm = f.widthCm,
                        depthCm = f.depthCm,
                        heightCm = oldF?.heightCm ?: catalogF?.height?.times(100f) ?: 100f,
                        colorHex = f.colorHex,
                        zIndex = f.zIndex,
                        name = oldF?.name ?: catalogF?.name ?: "",
                        category = oldF?.category ?: catalogF?.category ?: "",
                        modelPath = oldF?.modelPath ?: catalogF?.modelPath ?: "",
                        thumbnailPath = oldF?.thumbnailPath ?: catalogF?.thumbnailPath,
                        priceEstimate = oldF?.priceEstimate ?: catalogF?.priceEstimate
                    )
                })
            }
            newModel
        }
        savePlan()
    }


    val catalog: StateFlow<List<Furniture>> = furnitureRepository.getAllFurniture()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentPlanId: String = savedStateHandle.get<String>("planId") ?: java.util.UUID.randomUUID().toString()


    init {
        val planId = savedStateHandle.get<String>("planId")
        if (planId != null) {
            currentPlanId = planId
        }

        viewModelScope.launch {
            android.util.Log.d("RoomPlanner", "[INIT] Coroutine started. planId=$planId currentPlanId=$currentPlanId")
            // Load saved plan if we have a planId; otherwise initialize with an empty model
            if (planId != null) {
                val dbState = sharedRoomRepository.getRoomState(planId)
                if (dbState != null) {
                    android.util.Log.d("RoomPlanner", "[INIT] Loaded plan from DB. furniture=${dbState.furniture.size}")
                    roomStateManager.initialize(dbState)
                } else {
                    android.util.Log.d("RoomPlanner", "[INIT] planId supplied but no DB data. Initializing empty.")
                    roomStateManager.initialize(RoomModel(
                        planId = currentPlanId,
                        name = "New Room",
                        walls = emptyList(),
                        doors = emptyList(),
                        windows = emptyList(),
                        furniture = emptyList()
                    ))
                }
            } else {
                android.util.Log.d("RoomPlanner", "[INIT] No planId. Initializing empty model with planId=$currentPlanId")
                roomStateManager.initialize(RoomModel(
                    planId = currentPlanId,
                    name = "New Room",
                    walls = emptyList(),
                    doors = emptyList(),
                    windows = emptyList(),
                    furniture = emptyList()
                ))
            }
            android.util.Log.d("RoomPlanner", "[INIT] roomModel.value after init: ${roomStateManager.roomModel.value?.furniture?.size} furniture items")

            // Always collect state from the manager back into _uiState
            roomStateManager.roomModel.collect { model ->
                android.util.Log.d("RoomPlanner", "[COLLECT] roomModel emitted. model=${if (model == null) "null" else "furniture=${model.furniture.size}"}")
                if (model != null) {
                    _uiState.update { state ->
                        if (state.draggedFurnitureId == null) {
                            android.util.Log.d("RoomPlanner", "[COLLECT] Updating uiState with ${model.furniture.size} furniture items")
                            state.copy(
                                walls = model.walls.map { Wall(it.id, LineSegment(Point2D(it.startX * 100f, it.startY * 100f), Point2D(it.endX * 100f, it.endY * 100f)), it.thicknessCm) },
                                doors = model.doors.map { Door(it.id, LineSegment(Point2D(it.startX * 100f, it.startY * 100f), Point2D(it.endX * 100f, it.endY * 100f)), it.thicknessCm, it.swingAngle) },
                                windows = model.windows.map { Window(it.id, LineSegment(Point2D(it.startX * 100f, it.startY * 100f), Point2D(it.endX * 100f, it.endY * 100f)), it.thicknessCm) },
                                furniture = model.furniture.map { Placed2DFurniture(it.id, it.furnitureId, Point2D(it.positionX * 100f, it.positionY * 100f), it.rotation, it.scaleX, it.widthCm, it.depthCm, it.colorHex, it.zIndex, false, it.priceEstimate) },
                                focusedPlacedFurnitureIds = model.selectionState.selectedItemIds,
                                canUndo = roomStateManager.canUndo,
                                canRedo = roomStateManager.canRedo,
                                totalPriceEstimate = model.furniture.sumOf { it.priceEstimate ?: 0.0 }
                            )
                        } else {
                            android.util.Log.d("RoomPlanner", "[COLLECT] Skipped uiState update (dragging in progress)")
                            state
                        }
                    }
                    updateAreaAndPerimeter(_uiState.value.walls)
                }
            }
        }
        
        viewModelScope.launch {
            preferencesDataSource.appPreferences.collect { prefs ->
                _uiState.update { it.copy(measurementUnit = prefs.arMeasurementUnit) }
            }
        }
    }

    fun setInteractionMode(mode: InteractionMode) {
        _uiState.update { it.copy(mode = mode, currentDrawingStartPoint = null, currentDrawingEndPoint = null, selectedFurnitureId = null, draggedFurnitureId = null) }
    }

    fun selectFurnitureFromCatalog(furnitureId: String) {
        val selectedFurniture = catalog.value.find { it.id == furnitureId }
        android.util.Log.d("RoomPlanner", "[CATALOG] Item selected: ID=$furnitureId category=${selectedFurniture?.category} width=${selectedFurniture?.width} depth=${selectedFurniture?.depth}")
        android.util.Log.d("RoomPlanner", "[CATALOG] catalog.value.size=${catalog.value.size}")
        android.util.Log.d("RoomPlanner", "[CATALOG] roomModel.value is ${if (roomStateManager.roomModel.value == null) "NULL - placement WILL FAIL" else "non-null OK"}")
        _uiState.update { it.copy(mode = InteractionMode.PLACE_FURNITURE, selectedFurnitureId = furnitureId) }
        android.util.Log.d("RoomPlanner", "[CATALOG] uiState after: mode=${_uiState.value.mode} selectedFurnitureId=${_uiState.value.selectedFurnitureId}")
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

        var snappedX = point.x
        var snappedY = point.y
        if (state.snapToGrid) {
            snappedX = kotlin.math.round(point.x / state.gridSizeCm) * state.gridSizeCm
            snappedY = kotlin.math.round(point.y / state.gridSizeCm) * state.gridSizeCm
        }
        val gridSnappedPoint = Point2D(snappedX, snappedY)

        if (state.mode == InteractionMode.DRAW_DOOR || state.mode == InteractionMode.DRAW_WINDOW) {
            // Snap to nearest wall line
            var nearestPoint: Point2D? = null
            var minDistance = snapThreshold
            for (wall in state.walls) {
                val distance = wall.segment.distanceToPoint(gridSnappedPoint)
                if (distance < minDistance) {
                    minDistance = distance
                    nearestPoint = wall.segment.projectPoint(gridSnappedPoint)
                }
            }
            if (nearestPoint != null) {
                return nearestPoint
            }
        }

        // 2. Orthogonal Snapping
        if (startPoint != null) {
            val dx = kotlin.math.abs(point.x - startPoint.x)
            val dy = kotlin.math.abs(point.y - startPoint.y)
            if (dx < snapThreshold) {
                return Point2D(startPoint.x, gridSnappedPoint.y)
            }
            if (dy < snapThreshold) {
                return Point2D(gridSnappedPoint.x, startPoint.y)
            }
        }
        
        return gridSnappedPoint
    }

    fun onCanvasPointerDown(point: Point2D) {
        val state = _uiState.value
        android.util.Log.d("RoomPlanner", "[TAP] onCanvasPointerDown: mode=${state.mode} selectedFurnitureId=${state.selectedFurnitureId} point=$point")
        android.util.Log.d("RoomPlanner", "[TAP] roomModel.value=${if (roomStateManager.roomModel.value == null) "NULL" else "furniture=${roomStateManager.roomModel.value!!.furniture.size}"}")
        if (state.mode == InteractionMode.DRAW_WALL || 
            state.mode == InteractionMode.DRAW_DOOR || 
            state.mode == InteractionMode.DRAW_WINDOW || 
            state.mode == InteractionMode.MEASURE) {
            val snapped = getSnappedPoint(point)
            _uiState.update { it.copy(currentDrawingStartPoint = snapped, currentDrawingEndPoint = snapped) }
        } else if (state.mode == InteractionMode.PLACE_FURNITURE && state.selectedFurnitureId != null) {
            android.util.Log.d("RoomPlanner", "[PLACE] Placement branch entered. Searching catalog for id=${state.selectedFurnitureId}")
            val selectedFurniture = catalog.value.find { it.id == state.selectedFurnitureId }
            android.util.Log.d("RoomPlanner", "[PLACE] selectedFurniture=${selectedFurniture?.name} width=${selectedFurniture?.width} depth=${selectedFurniture?.depth}")
            val newFurnitureId = java.util.UUID.randomUUID().toString()
            // Catalog stores dimensions in meters; canvas renders in centimeters (1px = 1cm)
            val widthCm = if (selectedFurniture != null) (selectedFurniture.width * 100f).coerceAtLeast(40f) else 100f
            val depthCm = if (selectedFurniture != null) (selectedFurniture.depth * 100f).coerceAtLeast(40f) else 100f
            val newFurniture = Placed2DFurniture(
                id = newFurnitureId,
                furnitureId = state.selectedFurnitureId,
                position = point,
                widthCm = widthCm,
                depthCm = depthCm,
                priceEstimate = selectedFurniture?.priceEstimate
            )
            val sizeBefore = state.furniture.size
            android.util.Log.d("RoomPlanner", "[PLACE] Creating furniture: id=$newFurnitureId pos=$point w=${newFurniture.widthCm} d=${newFurniture.depthCm} sizeBefore=$sizeBefore")
            updateRoomState(furniture = state.furniture + newFurniture)
            android.util.Log.d("RoomPlanner", "[PLACE] updateRoomState done. roomModel furniture=${roomStateManager.roomModel.value?.furniture?.size}")
            android.util.Log.d("RoomPlanner", "[PLACE] uiState.furniture BEFORE mode switch=${_uiState.value.furniture.size}")
            _uiState.update { it.copy(mode = InteractionMode.SELECT, selectedFurnitureId = null) }
        } else if (state.mode == InteractionMode.SELECT) {
            val touchedFurniture = state.furniture.sortedByDescending { it.zIndex }.find { f ->
                val rect = com.lumiroom.feature.roomplanner.domain.geometry.RotatedRect(f.position, f.widthCm * f.scale, f.depthCm * f.scale, f.rotation)
                rect.contains(point)
            }
            if (touchedFurniture != null) {
                preDragFurnitureSnapshot = state.furniture
                val offset = Point2D(touchedFurniture.position.x - point.x, touchedFurniture.position.y - point.y)
                _uiState.update { currentState ->
                    val newSelection = if (currentState.isMultiSelectMode) {
                        if (currentState.focusedPlacedFurnitureIds.contains(touchedFurniture.id)) {
                            currentState.focusedPlacedFurnitureIds - touchedFurniture.id
                        } else {
                            currentState.focusedPlacedFurnitureIds + touchedFurniture.id
                        }
                    } else {
                        setOf(touchedFurniture.id)
                    }
                    roomStateManager.updateStateTransient { model ->
                        model.copy(selectionState = model.selectionState.copy(selectedItemIds = newSelection))
                    }
                    currentState.copy(draggedFurnitureId = touchedFurniture.id, dragOffset = offset, focusedPlacedFurnitureIds = newSelection)
                }
            } else {
                _uiState.update { it.copy(focusedPlacedFurnitureIds = emptySet()) }
                roomStateManager.updateStateTransient { model ->
                    model.copy(selectionState = model.selectionState.copy(selectedItemIds = emptySet()))
                }
            }
        } else if (state.mode == InteractionMode.REMOVE) {
            val hitThreshold = 20f / state.zoom
            
            // Priority: Furniture -> Window -> Door -> Wall
            val touchedFurniture = state.furniture.sortedByDescending { it.zIndex }.find { f -> 
                val rect = com.lumiroom.feature.roomplanner.domain.geometry.RotatedRect(f.position, f.widthCm * f.scale, f.depthCm * f.scale, f.rotation)
                rect.contains(point)
            }
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

    private fun getSnappedFurniture(f: Placed2DFurniture, point: Point2D): Pair<Point2D, Float> {
        val state = _uiState.value
        var snappedX = point.x
        var snappedY = point.y
        var newRotation = f.rotation

        // 1. Grid Snapping
        if (state.snapToGrid) {
            snappedX = kotlin.math.round(point.x / state.gridSizeCm) * state.gridSizeCm
            snappedY = kotlin.math.round(point.y / state.gridSizeCm) * state.gridSizeCm
        }
        var currentPoint = Point2D(snappedX, snappedY)

        // 2. Wall Snapping
        if (state.snapToWalls) {
            val snapThreshold = 30f / state.zoom
            var nearestWall: Wall? = null
            var minDistance = snapThreshold
            var nearestProjected: Point2D? = null

            for (wall in state.walls) {
                val dist = wall.segment.distanceToPoint(currentPoint)
                if (dist < minDistance) {
                    minDistance = dist
                    nearestWall = wall
                    nearestProjected = wall.segment.projectPoint(currentPoint)
                }
            }

            if (nearestWall != null && nearestProjected != null) {
                val dx = nearestWall.segment.end.x - nearestWall.segment.start.x
                val dy = nearestWall.segment.end.y - nearestWall.segment.start.y
                val len = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (len > 0f) {
                    val nx = -dy / len
                    val ny = dx / len
                    val dot = (currentPoint.x - nearestProjected.x) * nx + (currentPoint.y - nearestProjected.y) * ny
                    val side = if (dot >= 0) 1f else -1f
                    
                    currentPoint = Point2D(
                        nearestProjected.x + nx * side * (f.depthCm / 2f),
                        nearestProjected.y + ny * side * (f.depthCm / 2f)
                    )
                    newRotation = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                }
            }
        }

        return Pair(currentPoint, newRotation)
    }

    private fun Placed2DFurniture.getRotatedRect(): com.lumiroom.feature.roomplanner.domain.geometry.RotatedRect {
        return com.lumiroom.feature.roomplanner.domain.geometry.RotatedRect(
            center = position,
            width = widthCm * scale,
            height = depthCm * scale,
            rotationDegrees = rotation
        )
    }

    private fun checkCollisions(furniture: List<Placed2DFurniture>): List<Placed2DFurniture> {
        val rects = furniture.associate { it.id to it.getRotatedRect() }
        return furniture.map { f ->
            val myRect = rects[f.id]!!
            val hasCol = furniture.any { other ->
                other.id != f.id && myRect.intersects(rects[other.id]!!)
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
            val offset = state.dragOffset ?: Point2D(0f, 0f)
            val effectivePoint = Point2D(point.x + offset.x, point.y + offset.y)
            _uiState.update { currentState ->
                val newFurnitureList = currentState.furniture.map { f ->
                    if (f.id == state.draggedFurnitureId) {
                        val (snappedPos, snappedRot) = getSnappedFurniture(f, effectivePoint)
                        f.copy(position = snappedPos, rotation = snappedRot)
                    } else f
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
                updateRoomState(walls = state.walls + newWall)
                _uiState.update { it.copy(currentDrawingStartPoint = null, currentDrawingEndPoint = null) }
            } else {
                _uiState.update { it.copy(currentDrawingStartPoint = null, currentDrawingEndPoint = null) }
            }
        } else if (state.mode == InteractionMode.DRAW_DOOR) {
            if (start != null && end != null && start.distanceTo(end) > 5f) {
                val newDoor = Door(java.util.UUID.randomUUID().toString(), LineSegment(start, end))
                updateRoomState(doors = state.doors + newDoor)
                _uiState.update { it.copy(currentDrawingStartPoint = null, currentDrawingEndPoint = null) }
            } else {
                _uiState.update { it.copy(currentDrawingStartPoint = null, currentDrawingEndPoint = null) }
            }
        } else if (state.mode == InteractionMode.DRAW_WINDOW) {
            if (start != null && end != null && start.distanceTo(end) > 5f) {
                val newWindow = Window(java.util.UUID.randomUUID().toString(), LineSegment(start, end))
                updateRoomState(windows = state.windows + newWindow)
                _uiState.update { it.copy(currentDrawingStartPoint = null, currentDrawingEndPoint = null) }
            } else {
                _uiState.update { it.copy(currentDrawingStartPoint = null, currentDrawingEndPoint = null) }
            }
        } else if (state.mode == InteractionMode.MEASURE) {
            _uiState.update { it.copy(currentDrawingStartPoint = null, currentDrawingEndPoint = null) }
        } else if (state.mode == InteractionMode.SELECT) {
            val snapshot = preDragFurnitureSnapshot
            if (state.draggedFurnitureId != null && snapshot != null) {
                updateRoomState(furniture = state.furniture)
            }
            preDragFurnitureSnapshot = null
            _uiState.update { it.copy(draggedFurnitureId = null, dragOffset = null) }
        }
    }


    fun savePlan(planName: String? = null) {
        val model = roomStateManager.roomModel.value ?: return
        val updatedModel = if (planName != null) model.copy(name = planName) else model
        viewModelScope.launch {
            sharedRoomRepository.saveRoomState(updatedModel)
        }
    }

    fun moveFocusedFurniture(dx: Float, dy: Float) {
        val state = _uiState.value
        val ids = state.focusedPlacedFurnitureIds
        if (ids.isEmpty()) return
        
        roomStateManager.updateState { model ->
            val newFurnitureList = model.furniture.map { f ->
                if (ids.contains(f.id)) {
                    transformManager.move(f, dx, dy, 0f)
                } else f
            }
            model.copy(furniture = newFurnitureList)
        }
    }

    fun rotateFocusedFurniture(deltaDegrees: Float) {
        val state = _uiState.value
        val ids = state.focusedPlacedFurnitureIds
        if (ids.isEmpty()) return
        
        roomStateManager.updateState { model ->
            val newFurnitureList = model.furniture.map { f ->
                if (ids.contains(f.id)) {
                    transformManager.rotate(f, deltaDegrees)
                } else f
            }
            model.copy(furniture = newFurnitureList)
        }
    }

    fun scaleFocusedFurniture(deltaScale: Float) {
        val state = _uiState.value
        val ids = state.focusedPlacedFurnitureIds
        if (ids.isEmpty()) return
        
        roomStateManager.updateState { model ->
            val newFurnitureList = model.furniture.map { f ->
                if (ids.contains(f.id)) {
                    transformManager.scale(f, deltaScale)
                } else f
            }
            model.copy(furniture = newFurnitureList)
        }
    }

    fun bringFurnitureForward() {
        val state = _uiState.value
        val ids = state.focusedPlacedFurnitureIds
        if (ids.isEmpty()) return
        val newFurniture = state.furniture.map { f ->
            if (ids.contains(f.id)) f.copy(zIndex = f.zIndex + 1) else f
        }
        updateRoomState(furniture = checkCollisions(newFurniture)
        )
    }

    fun sendFurnitureBackward() {
        val state = _uiState.value
        val ids = state.focusedPlacedFurnitureIds
        if (ids.isEmpty()) return
        val newFurniture = state.furniture.map { f ->
            if (ids.contains(f.id)) f.copy(zIndex = f.zIndex - 1) else f
        }
        updateRoomState(furniture = checkCollisions(newFurniture)
        )
    }

    fun deleteFocusedItem() {
        val state = _uiState.value
        val ids = state.focusedPlacedFurnitureIds
        if (ids.isEmpty()) return
        
        val newFurniture = state.furniture.filterNot { ids.contains(it.id) }
        updateRoomState(furniture = newFurniture)
        _uiState.update { it.copy(focusedPlacedFurnitureIds = emptySet()) }
        roomStateManager.updateStateTransient { model ->
            model.copy(selectionState = model.selectionState.copy(selectedItemIds = emptySet()))
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
        
        updateRoomState(walls = newWalls, doors = newDoors, windows = newWindows, furniture = newFurniture)
        
        _uiState.update { 
            it.copy(deleteConfirmationId = null, deleteConfirmationType = null)
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(deleteConfirmationId = null, deleteConfirmationType = null) }
    }

    fun toggleLayer(layer: RoomLayer) {
        _uiState.update { state ->
            val newLayers = if (state.visibleLayers.contains(layer)) {
                state.visibleLayers - layer
            } else {
                state.visibleLayers + layer
            }
            state.copy(visibleLayers = newLayers)
        }
    }

    fun toggleVoiceListening() {
        if (_uiState.value.isVoiceListening) {
            voiceCommandManager.stopListening()
            _uiState.update { it.copy(isVoiceListening = false) }
        } else {
            _uiState.update { it.copy(isVoiceListening = true, voiceTranscript = "Listening...") }
            viewModelScope.launch {
                voiceCommandManager.startRecognition().collect { result ->
                    when (result) {
                        is VoiceResult.Partial -> {
                            _uiState.update { it.copy(voiceTranscript = result.text) }
                        }
                        is VoiceResult.Transcript -> {
                            val transcript = result.text
                            _uiState.update { it.copy(voiceTranscript = transcript, isVoiceListening = false) }
                            val command = commandParser.parse(transcript)
                            executeCommand(command)
                        }
                        is VoiceResult.Error -> {
                            _uiState.update { it.copy(isVoiceListening = false, errorMessage = result.message, voiceTranscript = null) }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun executeCommand(command: VoiceCommand) {
        android.util.Log.d("RoomPlanner", "Executing VoiceCommand: $command")
        when (command) {
            is VoiceCommand.Undo -> undo()
            is VoiceCommand.Redo -> redo()
            is VoiceCommand.DeleteSelected -> deleteFocusedItem()
            is VoiceCommand.Remove -> {
                // If it's "remove the chair", we find the first item matching category/name
                val state = _uiState.value
                val item = state.furniture.find { it.id == command.itemName || it.furnitureId.contains(command.itemName, ignoreCase = true) }
                if (item != null) {
                    val newFurniture = state.furniture.filterNot { it.id == item.id }
                    updateRoomState(furniture = newFurniture)
                }
            }
            is VoiceCommand.RotateRelative -> rotateFocusedFurniture(command.degrees)
            is VoiceCommand.ScaleRelative -> scaleFocusedFurniture(command.delta)
            is VoiceCommand.OpenCatalog -> {
                // Not supported directly in 2D yet, but we could mock
            }
            is VoiceCommand.Place -> {
                // Note: The AR Place finds the furniture from DB by name/category and places it
                viewModelScope.launch(Dispatchers.IO) {
                    val item = furnitureRepository.findFirstByNameOrCategory(command.itemName)
                    if (item != null) {
                        // Place at center of current view
                        val state = _uiState.value
                        val cx = state.panX
                        val cy = state.panY
                        val newFurnitureId = java.util.UUID.randomUUID().toString()
                        val newFurniture = Placed2DFurniture(
                            id = newFurnitureId,
                            furnitureId = item.id,
                            position = Point2D(-cx, -cy),
                            widthCm = (item.width * 100f).coerceAtLeast(40f),
                            depthCm = (item.depth * 100f).coerceAtLeast(40f)
                        )
                        updateRoomState(furniture = state.furniture + newFurniture)
                    }
                }
            }
            else -> {
                android.util.Log.d("RoomPlanner", "VoiceCommand $command not fully handled in 2D Planner yet.")
            }
        }
    }
}
