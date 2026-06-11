package com.lumiroom.feature.ar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumiroom.core.common.dispatchers.Dispatcher
import com.lumiroom.core.common.dispatchers.LumiroomDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.lumiroom.core.domain.FurnitureSelectionManager
import com.lumiroom.core.domain.RoomAnalyticsManager
import com.lumiroom.core.domain.LayoutPersistenceManager
import com.lumiroom.core.database.repository.FurnitureRepository
import com.lumiroom.feature.voice.CommandParser
import com.lumiroom.feature.voice.VoiceCommand
import com.lumiroom.feature.voice.VoiceCommandExecutor
import com.lumiroom.feature.voice.VoiceCommandManager
import com.lumiroom.feature.voice.VoiceResult
import androidx.lifecycle.SavedStateHandle
import com.lumiroom.core.database.dao.RoomPlanDao
import com.lumiroom.core.domain.SharedRoomRepository
import com.lumiroom.core.database.entity.FloorPlanItemEntity
import com.lumiroom.core.database.relation.FloorPlanItemWithFurniture
import kotlinx.coroutines.flow.combine

@HiltViewModel
class ArViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val roomPlanDao: RoomPlanDao,
    private val sharedRoomRepository: SharedRoomRepository,
    private val selectionManager: FurnitureSelectionManager,
    private val analyticsManager: RoomAnalyticsManager,
    private val layoutPersistenceManager: LayoutPersistenceManager,
    private val furnitureRepository: FurnitureRepository,
    private val voiceCommandManager: VoiceCommandManager,
    private val commandParser: CommandParser,
    @Dispatcher(LumiroomDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel(), VoiceCommandExecutor {

    private val _uiState = MutableStateFlow(ArUiState())
    val uiState: StateFlow<ArUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ArViewEvent>()
    val events: SharedFlow<ArViewEvent> = _events.asSharedFlow()

    private val undoStack = ArrayDeque<ArAction>()
    private val redoStack = ArrayDeque<ArAction>()

    init {
        // Collect selection state
        viewModelScope.launch {
            combine(
                selectionManager.selectedItemIds,
                selectionManager.lockedItemIds,
                selectionManager.hiddenItemIds
            ) { selected, locked, hidden ->
                _uiState.update { 
                    it.copy(
                        selectedItemIds = selected,
                        lockedItemIds = locked,
                        hiddenItemIds = hidden
                    )
                }
            }.collect {}
        }
        
        layoutPersistenceManager.startAutoSaveLoop()
        
        viewModelScope.launch(ioDispatcher) {
            val roomId = savedStateHandle.get<String>("roomId") ?: savedStateHandle.get<String>("planId")
            
            val activeRoomId = if (roomId == null) {
                val newPlan = com.lumiroom.core.database.entity.RoomPlanEntity(name = "Untitled AR Session")
                roomPlanDao.insertRoomPlan(newPlan)
                newPlan.id
            } else {
                roomId
            }
            
            _uiState.update { it.copy(currentRoomDesignId = activeRoomId) }
            
            try {
                roomPlanDao.getRoomPlanWithDetails(activeRoomId).collect { roomWithItems ->
                    if (roomWithItems != null) {
                        val roomName = roomWithItems.roomPlan.name
                        _uiState.update { it.copy(currentRoomName = roomName, roomAnchorId = roomWithItems.roomPlan.roomAnchorId) }
                        
                        val itemsWithFurniture = roomWithItems.items
                        val lockedIds = itemsWithFurniture.filter { it.item.isLocked }.map { it.item.id }.toSet()
                        val hiddenIds = itemsWithFurniture.filter { !it.item.isVisible }.map { it.item.id }.toSet()
                        _uiState.update { state -> 
                            state.copy(
                                placedItems = itemsWithFurniture,
                                lockedItemIds = lockedIds,
                                hiddenItemIds = hiddenIds
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ArViewModel", "Failed to load room", e)
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
    
    fun setRoomAnchor(anchorId: String) {
        viewModelScope.launch(ioDispatcher) {
            val planId = _uiState.value.currentRoomDesignId ?: return@launch
            sharedRoomRepository.updateRoomAnchor(planId, anchorId)
            _uiState.update { it.copy(roomAnchorId = anchorId) }
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

            val planId = _uiState.value.currentRoomDesignId ?: return@launch
            
            // Using hitPosY as Z elevation in 2D space, hitPosZ as Y depth
            val itemEntity = FloorPlanItemEntity(
                id = instanceId,
                planId = planId,
                furnitureId = furnitureId,
                posX = hitPosX,
                posY = hitPosZ, // Note: In AR hitPosZ is depth (Y in 2D)
                posZ = hitPosY, // hitPosY is height (Z in 2D)
                rotation = rotY, // Rough yaw conversion
                scaleX = 1f, scaleY = 1f, scaleZ = 1f,
                isLocked = false, isVisible = true
            )
            
            sharedRoomRepository.addFurniture(planId, itemEntity)
            
            val furnitureModel = furnitureRepository.getFurnitureById(furnitureId).firstOrNull()
            if (furnitureModel != null) {
                val furnitureEntity = com.lumiroom.core.database.entity.FurnitureEntity(
                    id = furnitureModel.id,
                    name = furnitureModel.name,
                    category = furnitureModel.category,
                    description = furnitureModel.description,
                    width = furnitureModel.width,
                    depth = furnitureModel.depth,
                    height = furnitureModel.height,
                    priceEstimate = furnitureModel.priceEstimate,
                    modelPath = furnitureModel.modelPath,
                    thumbnailPath = furnitureModel.thumbnailPath,
                    style = furnitureModel.style,
                    isDownloaded = furnitureModel.isDownloaded,
                    isFavorite = furnitureModel.isFavorite
                )
                val fullItem = FloorPlanItemWithFurniture(itemEntity, furnitureEntity)
                undoStack.addLast(ArAction.Place(fullItem))
                redoStack.clear()
            }

            _uiState.update { state ->
                state.copy(
                    isLoadingModel = false,
                    canUndo = true,
                    canRedo = false,
                    errorMessage = null
                )
            }
        }
    }

    // ── Selection ─────────────────────────────────────────────────────────────

    fun onItemSelected(itemId: String?, multiSelect: Boolean = false) {
        if (itemId == null) {
            selectionManager.clearSelection()
            _uiState.update { it.copy(interactionMode = InteractionMode.IDLE) }
        } else {
            selectionManager.selectItem(itemId, multiSelect)
            _uiState.update { it.copy(interactionMode = InteractionMode.IDLE) }
        }
    }
    
    fun setInteractionMode(mode: InteractionMode) {
        _uiState.update { it.copy(interactionMode = mode) }
    }
    
    fun toggleLock(itemId: String) {
        selectionManager.toggleLock(itemId)
    }
    
    fun toggleVisibility(itemId: String) {
        selectionManager.toggleVisibility(itemId)
    }

    // ── Management ────────────────────────────────────────────────────────────

    fun onRemoveSelectedItems() {
        val itemIds = _uiState.value.selectedItemIds
        if (itemIds.isEmpty()) return
        
        viewModelScope.launch(ioDispatcher) {
            val planId = _uiState.value.currentRoomDesignId ?: return@launch
            itemIds.forEach { itemId ->
                val itemToRemove = _uiState.value.placedItems.find { it.item.id == itemId }
                if (itemToRemove != null) {
                    sharedRoomRepository.removeFurniture(planId, itemId)
                    pushUndo(ArAction.Remove(itemToRemove))
                }
            }
            selectionManager.clearSelection()
        }
    }
    
    fun toggleMeasuring() {
        _uiState.update { it.copy(isMeasuring = !it.isMeasuring) }
    }

    fun onResetTransform() {
        viewModelScope.launch(ioDispatcher) {
            val selectedIds = _uiState.value.selectedItemIds
            if (selectedIds.isEmpty()) return@launch
            
            val planId = _uiState.value.currentRoomDesignId ?: return@launch
            val undoActions = mutableListOf<ArAction.Transform>()
            
            _uiState.value.placedItems.forEach { item ->
                if (item.item.id in selectedIds) {
                    val newItemEntity = item.item.copy(
                        posX = item.item.posX, // ideally reset to initial pos
                        posY = item.item.posY,
                        posZ = item.item.posZ,
                        rotation = 0f,
                        scaleX = 1f, scaleY = 1f, scaleZ = 1f
                    )
                    val newItem = item.copy(item = newItemEntity)
                    undoActions.add(ArAction.Transform(item.item.id, item, newItem))
                    sharedRoomRepository.updateFurniture(planId, newItemEntity)
                }
            }
            
            undoActions.forEach { action ->
                pushUndo(action)
            }
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
        viewModelScope.launch(ioDispatcher) {
            val oldItem = _uiState.value.placedItems.find { it.item.id == itemId } ?: return@launch
            val planId = _uiState.value.currentRoomDesignId ?: return@launch
            
            val newItemEntity = oldItem.item.copy(
                posX = posX, posY = posZ, posZ = posY,
                rotation = rotY,
                scaleX = scaleX, scaleY = scaleY, scaleZ = scaleZ
            )
            sharedRoomRepository.updateFurniture(planId, newItemEntity)
            
            val newItem = oldItem.copy(item = newItemEntity)
            pushUndo(ArAction.Transform(itemId, oldItem, newItem))
        }
    }

    // ── Undo / Redo ───────────────────────────────────────────────────────────

    private fun pushUndo(action: ArAction) {
        undoStack.addLast(action)
        if (undoStack.size > 50) undoStack.removeFirst()
        redoStack.clear()
        _uiState.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = false) }
    }

    fun onUndo() {
        val action = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(action)
        _uiState.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty()) }
        
        viewModelScope.launch(ioDispatcher) {
            val planId = _uiState.value.currentRoomDesignId ?: return@launch
            when (action) {
                is ArAction.Place -> {
                    sharedRoomRepository.removeFurniture(planId, action.item.item.id)
                }
                is ArAction.Remove -> {
                    sharedRoomRepository.addFurniture(planId, action.item.item)
                }
                is ArAction.Transform -> {
                    sharedRoomRepository.updateFurniture(planId, action.oldItem.item)
                }
            }
        }
    }

    fun onRedo() {
        val action = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(action)
        _uiState.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty()) }
        
        viewModelScope.launch(ioDispatcher) {
            val planId = _uiState.value.currentRoomDesignId ?: return@launch
            when (action) {
                is ArAction.Place -> {
                    sharedRoomRepository.addFurniture(planId, action.item.item)
                }
                is ArAction.Remove -> {
                    sharedRoomRepository.removeFurniture(planId, action.item.item.id)
                }
                is ArAction.Transform -> {
                    sharedRoomRepository.updateFurniture(planId, action.newItem.item)
                }
            }
        }
    }

    fun onSaveRoom(name: String) {
        // Auto saved
    }

    fun onSaveSuccessHandled() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
    
    fun exportInventoryCsv(): String {
        val items = _uiState.value.placedItems
        val sb = StringBuilder()
        sb.appendLine("ID,Name,Category,Price Estimate,Width,Height,Depth")
        items.forEach { item ->
            val f = item.furniture
            sb.appendLine("${f.id},\"${f.name}\",${f.category},${f.priceEstimate ?: 0.0},${f.width},${f.height},${f.depth}")
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
        when (command) {
            is VoiceCommand.Help -> _uiState.update { it.copy(showVoiceHelpDialog = true) }
            is VoiceCommand.Place -> {
                viewModelScope.launch(ioDispatcher) {
                    val item = furnitureRepository.findFirstByNameOrCategory(command.itemName)
                    if (item != null) {
                        val voiceInstanceId = java.util.UUID.randomUUID().toString()
                        onPlaneTapped(voiceInstanceId, item.id, 0f, 0f, -1f)
                        _events.emit(ArViewEvent.ShowSnackbar("Placing ${item.name}"))
                    }
                }
            }
            is VoiceCommand.Remove -> {
                val match = _uiState.value.placedItems.find { it.furniture.name.contains(command.itemName, ignoreCase = true) }
                if (match != null) {
                    viewModelScope.launch(ioDispatcher) {
                        val planId = _uiState.value.currentRoomDesignId ?: return@launch
                        sharedRoomRepository.removeFurniture(planId, match.item.id)
                        pushUndo(ArAction.Remove(match))
                    }
                }
            }
            is VoiceCommand.Deselect -> selectionManager.clearSelection()
            is VoiceCommand.Rotate -> {
                val selectedId = _uiState.value.selectedItemIds.firstOrNull()
                if (selectedId != null) {
                    viewModelScope.launch { _events.emit(ArViewEvent.RotateSelectedItem(command.degrees)) }
                }
            }
            is VoiceCommand.Scale -> {
                 viewModelScope.launch { _events.emit(ArViewEvent.ScaleSelectedItem(command.factor)) }
            }
            is VoiceCommand.DeleteSelected -> onRemoveSelectedItems()
            is VoiceCommand.Undo -> onUndo()
            is VoiceCommand.Redo -> onRedo()
            else -> {
                viewModelScope.launch { _events.emit(ArViewEvent.ShowSnackbar("Command not implemented yet.")) }
            }
        }
    }
}

/** Reversible AR actions for undo/redo history tracking. */
sealed class ArAction {
    data class Place(val item: FloorPlanItemWithFurniture) : ArAction()
    data class Remove(val item: FloorPlanItemWithFurniture) : ArAction()
    data class Transform(
        val itemId: String,
        val oldItem: FloorPlanItemWithFurniture,
        val newItem: FloorPlanItemWithFurniture
    ) : ArAction()
}

/** One-time events emitted to the UI layer. */
sealed class ArViewEvent {
    object NavigateToCatalog : ArViewEvent()
    data class ShowSnackbar(val message: String) : ArViewEvent()
    object RoomSaved : ArViewEvent()
    data class RotateSelectedItem(val degrees: Float) : ArViewEvent()
    data class ScaleSelectedItem(val factor: Float) : ArViewEvent()
    object TakeScreenshot : ArViewEvent()
    object ShareScreenshot : ArViewEvent()
}
