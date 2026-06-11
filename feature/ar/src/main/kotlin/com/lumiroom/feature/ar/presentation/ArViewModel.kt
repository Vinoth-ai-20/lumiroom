package com.lumiroom.feature.ar.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumiroom.core.common.dispatchers.Dispatcher
import com.lumiroom.core.common.dispatchers.LumiroomDispatcher
import com.lumiroom.core.database.repository.FurnitureRepository
import com.lumiroom.core.domain.LayoutPersistenceManager
import com.lumiroom.core.domain.RoomAnalyticsManager
import com.lumiroom.core.domain.RoomStateManager
import com.lumiroom.core.domain.SharedRoomRepository
import com.lumiroom.core.domain.model.RoomFurniture
import com.lumiroom.core.domain.model.SelectionState
import com.lumiroom.feature.voice.CommandParser
import com.lumiroom.feature.voice.VoiceCommand
import com.lumiroom.feature.voice.VoiceCommandExecutor
import com.lumiroom.feature.voice.VoiceCommandManager
import com.lumiroom.feature.voice.VoiceResult
import com.lumiroom.feature.ar.domain.CloudAnchorManager
import com.lumiroom.core.domain.model.RoomWall
import com.lumiroom.core.domain.model.Point2DData
import com.lumiroom.core.domain.TransformManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val roomStateManager: RoomStateManager,
    private val sharedRoomRepository: SharedRoomRepository,
    private val analyticsManager: RoomAnalyticsManager,
    private val layoutPersistenceManager: LayoutPersistenceManager,
    private val furnitureRepository: FurnitureRepository,
    private val voiceCommandManager: VoiceCommandManager,
    private val commandParser: CommandParser,
    private val cloudAnchorManager: CloudAnchorManager,
    private val transformManager: TransformManager,
    @Dispatcher(LumiroomDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel(), VoiceCommandExecutor {

    private val _uiState = MutableStateFlow(ArUiState())
    val uiState: StateFlow<ArUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ArViewEvent>()
    val events: SharedFlow<ArViewEvent> = _events.asSharedFlow()

    private var lastCornerNode: Point2DData? = null

    init {
        layoutPersistenceManager.startAutoSaveLoop()
        
        viewModelScope.launch(ioDispatcher) {
            val roomId = savedStateHandle.get<String>("roomId") ?: savedStateHandle.get<String>("planId")
            val activeRoomId = roomId ?: return@launch
            
            _uiState.update { it.copy(currentRoomDesignId = activeRoomId) }
            
            // Fetch initial state from DB if not already initialized
            val dbState = sharedRoomRepository.getRoomState(activeRoomId)
            if (dbState != null) {
                roomStateManager.initialize(dbState)
            }
            
            // Observe the unified room model
            roomStateManager.roomModel.collect { model ->
                if (model != null) {
                    _uiState.update { state -> 
                        state.copy(
                            currentRoomName = model.name,
                            roomAnchorId = model.roomAnchorId,
                            anchors = model.anchors,
                            placedItems = model.furniture,
                            selectedItemIds = model.selectionState.selectedItemIds,
                            lockedItemIds = model.selectionState.lockedItemIds,
                            hiddenItemIds = model.selectionState.hiddenItemIds,
                            canUndo = roomStateManager.canUndo,
                            canRedo = roomStateManager.canRedo
                        )
                    }
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        layoutPersistenceManager.stopAutoSaveLoop()
    }

    // ── Session State ─────────────────────────────────────────────────────────

    fun onSessionInitializing() {
        _uiState.update { it.copy(sessionState = ArSessionState.Initializing) }
    }

    fun onSessionScanning() {
        _uiState.update { it.copy(sessionState = ArSessionState.Scanning, showScanSurfaceHint = true) }
    }

    fun onPlaneDetected(count: Int) {
        _uiState.update {
            it.copy(
                sessionState = ArSessionState.Ready,
                planesDetected = true,
                planeCount = count,
                showScanSurfaceHint = false,
                showTapToPlaceHint = true,
            )
        }
    }

    fun onSessionError(reason: String) {
        _uiState.update { it.copy(sessionState = ArSessionState.Error(reason)) }
    }
    
    fun setRoomAnchor(anchorId: String, pose: com.lumiroom.core.domain.model.PoseData) {
        roomStateManager.updateState { 
            val newAnchor = com.lumiroom.core.domain.model.AnchorData(id = anchorId, pose = pose)
            it.copy(
                roomAnchorId = anchorId,
                anchors = it.anchors.filter { a -> a.id != anchorId } + newAnchor
            )
        }
    }

    // ── Placement ─────────────────────────────────────────────────────────────

    fun onPlaneTapped(
        instanceId: String,
        furnitureId: String,
        hitPosX: Float, hitPosY: Float, hitPosZ: Float,
        rotX: Float = 0f, rotY: Float = 0f, rotZ: Float = 0f, rotW: Float = 1f,
    ) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoadingModel = true, showTapToPlaceHint = false) }

            val furnitureModel = furnitureRepository.getFurnitureById(furnitureId).firstOrNull()
            if (furnitureModel != null) {
                val newFurniture = RoomFurniture(
                    id = instanceId,
                    furnitureId = furnitureId,
                    positionX = hitPosX,
                    positionY = hitPosZ, // Y depth is mapped to Z
                    positionZ = hitPosY, // Height Z is mapped to Y
                    rotation = rotY,
                    scaleX = 1f, scaleY = 1f, scaleZ = 1f,
                    initialPositionX = hitPosX,
                    initialPositionY = hitPosZ,
                    initialPositionZ = hitPosY,
                    initialRotation = rotY,
                    initialScaleX = 1f, initialScaleY = 1f, initialScaleZ = 1f,
                    widthCm = furnitureModel.width * 100f,
                    depthCm = furnitureModel.depth * 100f,
                    heightCm = furnitureModel.height * 100f,
                    name = furnitureModel.name,
                    category = furnitureModel.category,
                    modelPath = furnitureModel.modelPath,
                    thumbnailPath = furnitureModel.thumbnailPath,
                    priceEstimate = furnitureModel.priceEstimate
                )
                
                roomStateManager.updateState { model ->
                    model.copy(furniture = model.furniture + newFurniture)
                }
            }

            _uiState.update { it.copy(isLoadingModel = false, errorMessage = null) }
        }
    }

    // ── Selection ─────────────────────────────────────────────────────────────

    fun onItemSelected(itemId: String?, multiSelect: Boolean = false) {
        roomStateManager.updateStateTransient { model ->
            val newSelection = if (itemId == null) {
                emptySet()
            } else if (multiSelect) {
                model.selectionState.selectedItemIds + itemId
            } else {
                setOf(itemId)
            }
            model.copy(selectionState = model.selectionState.copy(selectedItemIds = newSelection))
        }
        _uiState.update { it.copy(interactionMode = InteractionMode.IDLE) }
    }
    
    fun toggleMode(mode: InteractionMode) {
        if (_uiState.value.interactionMode == mode) {
            _uiState.update { it.copy(interactionMode = InteractionMode.IDLE) }
            lastCornerNode = null
        } else {
            _uiState.update { it.copy(interactionMode = mode) }
            lastCornerNode = null
        }
    }

    fun setInteractionMode(mode: InteractionMode) {
        _uiState.update { it.copy(interactionMode = mode) }
    }

    fun onCornerPlaced(posX: Float, posZ: Float) {
        if (_uiState.value.interactionMode != InteractionMode.DEFINE_ROOM) return
        
        val current = Point2DData(posX, posZ)
        val last = lastCornerNode
        if (last != null) {
            val newWall = RoomWall(
                id = java.util.UUID.randomUUID().toString(),
                startX = last.x,
                startY = last.y,
                endX = current.x,
                endY = current.y
            )
            roomStateManager.updateState { model ->
                model.copy(walls = model.walls + newWall)
            }
        }
        lastCornerNode = current
    }

    fun finishRoomDefinition() {
        lastCornerNode = null
        _uiState.update { it.copy(interactionMode = InteractionMode.IDLE) }
    }
    
    fun toggleLock(itemId: String) {
        roomStateManager.updateStateTransient { model ->
            val locked = model.selectionState.lockedItemIds
            val newLocked = if (locked.contains(itemId)) locked - itemId else locked + itemId
            model.copy(selectionState = model.selectionState.copy(lockedItemIds = newLocked))
        }
    }
    
    fun toggleVisibility(itemId: String) {
        roomStateManager.updateStateTransient { model ->
            val hidden = model.selectionState.hiddenItemIds
            val newHidden = if (hidden.contains(itemId)) hidden - itemId else hidden + itemId
            model.copy(selectionState = model.selectionState.copy(hiddenItemIds = newHidden))
        }
    }

    // ── Management ────────────────────────────────────────────────────────────

    fun onRemoveSelectedItems() {
        val itemIds = _uiState.value.selectedItemIds
        if (itemIds.isEmpty()) return
        
        roomStateManager.updateState { model ->
            model.copy(
                furniture = model.furniture.filter { it.id !in itemIds },
                selectionState = model.selectionState.copy(selectedItemIds = emptySet())
            )
        }
    }
    
    fun toggleMeasuring() {
        _uiState.update { it.copy(isMeasuring = !it.isMeasuring) }
    }

    fun onResetTransform() {
        val selectedIds = _uiState.value.selectedItemIds
        if (selectedIds.isEmpty()) return
        
        roomStateManager.updateState { model ->
            val newFurniture = model.furniture.map { item ->
                if (item.id in selectedIds) {
                    transformManager.resetTransform(item)
                } else {
                    item
                }
            }
            model.copy(furniture = newFurniture)
        }
    }

    fun togglePlaneVisualization() {
        _uiState.update { it.copy(showPlaneVisualization = !it.showPlaneVisualization) }
    }
    
    fun toggleLabels() {
        _uiState.update { it.copy(showLabels = !it.showLabels) }
    }

    // ── Transform ─────────────────────────────────────────────────────────────

    fun onItemTransformed(
        itemId: String,
        posX: Float, posY: Float, posZ: Float,
        rotX: Float, rotY: Float, rotZ: Float, rotW: Float,
        scaleX: Float, scaleY: Float, scaleZ: Float,
        matrixJson: String,
    ) {
        roomStateManager.updateState { model ->
            val newFurniture = model.furniture.map { item ->
                if (item.id == itemId) {
                    item.copy(
                        positionX = posX, 
                        positionY = posZ, // Map Y height back to posZ (depth map)
                        positionZ = posY, // Map Z depth back to posY (height map)
                        rotation = rotY,
                        scaleX = scaleX, scaleY = scaleY, scaleZ = scaleZ
                    )
                } else {
                    item
                }
            }
            model.copy(furniture = newFurniture)
        }
    }

    // ── Undo / Redo ───────────────────────────────────────────────────────────

    fun onUndo() {
        roomStateManager.undo()
    }

    fun onRedo() {
        roomStateManager.redo()
    }

    fun onSaveRoom(name: String) {
        viewModelScope.launch(ioDispatcher) {
            roomStateManager.roomModel.value?.let { model ->
                sharedRoomRepository.saveRoomState(model)
                _uiState.update { it.copy(saveSuccess = true) }
            }
        }
    }

    fun onSaveSuccessHandled() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
    
    fun exportInventoryCsv(): String {
        val items = _uiState.value.placedItems
        val sb = StringBuilder()
        sb.appendLine("ID,Name,Category,Price Estimate,Width,Height,Depth")
        items.forEach { f ->
            sb.appendLine("${f.furnitureId},\"${f.name}\",${f.category},${f.priceEstimate ?: 0.0},${f.widthCm},${f.heightCm},${f.depthCm}")
        }
        return sb.toString()
    }
    
    fun exportRoomSummary(): String {
        val count = _uiState.value.totalItemCount
        val cost = _uiState.value.totalCostEstimate
        val name = _uiState.value.currentRoomName
        
        return """
            Room: $name
            Total Items: $count
            Estimated Cost: ₹$cost
            
            Check out my new room design created with Lumiroom!
        """.trimIndent()
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onVoiceHelpDismissed() {
        _uiState.update { it.copy(showVoiceHelpDialog = false) }
    }

    // ── Voice Commands ────────────────────────────────────────────────────────

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

    override fun executeCommand(command: VoiceCommand) {
        android.util.Log.d("VoiceCommand", "Executing: $command")
        when (command) {
            is VoiceCommand.Help -> _uiState.update { it.copy(showVoiceHelpDialog = true) }
            is VoiceCommand.Place -> {
                viewModelScope.launch(ioDispatcher) {
                    val item = furnitureRepository.findFirstByNameOrCategory(command.itemName)
                    if (item != null) {
                        val voiceInstanceId = java.util.UUID.randomUUID().toString()
                        onPlaneTapped(voiceInstanceId, item.id, 0f, 0f, -1f)
                        _events.emit(ArViewEvent.ShowSnackbar("Placing ${item.name}"))
                    } else {
                        _events.emit(ArViewEvent.ShowSnackbar("Could not find ${command.itemName}"))
                    }
                }
            }
            is VoiceCommand.Remove -> {
                val match = _uiState.value.placedItems.find { it.name.contains(command.itemName, ignoreCase = true) }
                if (match != null) {
                    roomStateManager.updateState { model ->
                        model.copy(furniture = model.furniture.filter { it.id != match.id })
                    }
                }
            }
            is VoiceCommand.Deselect -> onItemSelected(null)
            is VoiceCommand.RotateRelative -> {
                val selectedId = _uiState.value.selectedItemIds.firstOrNull()
                if (selectedId != null) {
                    roomStateManager.updateState { model ->
                        model.copy(furniture = model.furniture.map {
                            if (it.id == selectedId) transformManager.rotate(it, command.degrees) else it
                        })
                    }
                }
            }
            is VoiceCommand.RotateAbsolute -> {
                val selectedId = _uiState.value.selectedItemIds.firstOrNull()
                if (selectedId != null) {
                    roomStateManager.updateState { model ->
                        model.copy(furniture = model.furniture.map {
                            if (it.id == selectedId) transformManager.rotateAbsolute(it, command.degrees) else it
                        })
                    }
                }
            }
            is VoiceCommand.ScaleRelative -> {
                val selectedId = _uiState.value.selectedItemIds.firstOrNull()
                if (selectedId != null) {
                    roomStateManager.updateState { model ->
                        model.copy(furniture = model.furniture.map {
                            if (it.id == selectedId) transformManager.scale(it, command.delta) else it
                        })
                    }
                }
            }
            is VoiceCommand.ScaleAbsolute -> {
                val selectedId = _uiState.value.selectedItemIds.firstOrNull()
                if (selectedId != null) {
                    roomStateManager.updateState { model ->
                        model.copy(furniture = model.furniture.map {
                            if (it.id == selectedId) transformManager.scaleAbsolute(it, command.percent) else it
                        })
                    }
                }
            }
            is VoiceCommand.Move -> {
                val selectedId = _uiState.value.selectedItemIds.firstOrNull()
                if (selectedId != null) {
                    val distance = 0.5f // 50cm per command
                    val (dx, dy) = when (command.direction) {
                        "forward" -> Pair(0f, -distance)
                        "backward" -> Pair(0f, distance)
                        "left" -> Pair(-distance, 0f)
                        "right" -> Pair(distance, 0f)
                        else -> Pair(0f, 0f)
                    }
                    roomStateManager.updateState { model ->
                        model.copy(furniture = model.furniture.map {
                            if (it.id == selectedId) transformManager.move(it, dx, dy, 0f) else it
                        })
                    }
                }
            }
            is VoiceCommand.DeleteSelected -> onRemoveSelectedItems()
            is VoiceCommand.ResetObject -> onResetTransform()
            is VoiceCommand.Undo -> onUndo()
            is VoiceCommand.Redo -> onRedo()
            else -> {
                viewModelScope.launch { _events.emit(ArViewEvent.ShowSnackbar("Command not fully implemented.")) }
            }
        }
    }
}

/** One-time events emitted to the UI layer. */
sealed class ArViewEvent {
    object NavigateToCatalog : ArViewEvent()
    data class ShowSnackbar(val message: String) : ArViewEvent()
    object RoomSaved : ArViewEvent()
    object TakeScreenshot : ArViewEvent()
    object ShareScreenshot : ArViewEvent()
}
