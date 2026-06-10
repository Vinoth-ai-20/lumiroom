package com.lumiroom.feature.ar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumiroom.core.common.dispatchers.Dispatcher
import com.lumiroom.core.common.dispatchers.LumiroomDispatcher
import com.lumiroom.core.common.result.LumiroomResult
import com.lumiroom.feature.ar.domain.PlaceFurnitureUseCase
import com.lumiroom.feature.ar.domain.RemoveFurnitureUseCase
import com.lumiroom.feature.ar.domain.SaveArSessionUseCase
import com.lumiroom.feature.ar.domain.TransformFurnitureUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the AR placement screen.
 *
 * This class contains NO ARCore or SceneView SDK references — those live in
 * [com.lumiroom.feature.ar.engine.LumiroomArSessionManager] which is lifecycle-scoped
 * to the Composable. The ViewModel only manages data persistence and UI state.
 *
 * Architecture note: AR gestures (hit-test results, transform matrices) are
 * passed UP from the engine as plain data, processed here, and persisted via
 * use cases. This keeps the ViewModel fully testable.
 */
import com.lumiroom.core.domain.FurnitureSelectionManager
import com.lumiroom.core.domain.RoomAnalyticsManager
import com.lumiroom.core.domain.LayoutPersistenceManager
import com.lumiroom.core.database.repository.FurnitureRepository
import com.lumiroom.feature.voice.CommandParser
import com.lumiroom.feature.voice.VoiceCommand
import com.lumiroom.feature.voice.VoiceCommandExecutor
import com.lumiroom.feature.voice.VoiceCommandManager
import com.lumiroom.feature.voice.VoiceResult
import kotlinx.coroutines.flow.combine

@HiltViewModel
class ArViewModel @Inject constructor(
    private val placeFurnitureUseCase: PlaceFurnitureUseCase,
    private val removeFurnitureUseCase: RemoveFurnitureUseCase,
    private val transformFurnitureUseCase: TransformFurnitureUseCase,
    private val saveArSessionUseCase: SaveArSessionUseCase,
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

    /** One-time events that should not be re-emitted on recomposition (e.g. navigation). */
    private val _events = MutableSharedFlow<ArViewEvent>()
    val events: SharedFlow<ArViewEvent> = _events.asSharedFlow()

    /** Undo history stack — list of reversible actions. */
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
        
        // Auto-save
        layoutPersistenceManager.startAutoSaveLoop()
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

    // ── Placement ─────────────────────────────────────────────────────────────

    fun onPlaneTapped(
        instanceId: String,
        furnitureId: String,
        hitPosX: Float, hitPosY: Float, hitPosZ: Float,
        rotX: Float = 0f, rotY: Float = 0f, rotZ: Float = 0f, rotW: Float = 1f,
    ) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoadingModel = true, showTapToPlaceHint = false) }

            // DEBUG: Temporarily disable collision checking as it may be causing silent placement failures
            // due to un-rendered ghost objects in the DB.
            val hasCollision = false 
            /* 
            _uiState.value.placedItems.any { item ->
                val w1 = item.furniture.width ?: 1.0f
                val d1 = item.furniture.depth ?: 1.0f
                val w2 = 1.0f
                val d2 = 1.0f
                
                com.lumiroom.core.domain.MeasurementManager().checkCollision2D(
                    item.placedItem.posX, item.placedItem.posZ, w1, d1,
                    hitPosX, hitPosZ, w2, d2
                )
            }
            */

            if (hasCollision) {
                _uiState.update { it.copy(isLoadingModel = false, errorMessage = "Cannot place here: Collision detected.") }
                return@launch
            }

            val result = placeFurnitureUseCase(
                instanceId = instanceId,
                furnitureId = furnitureId,
                roomDesignId = _uiState.value.currentRoomDesignId,
                posX = hitPosX, posY = hitPosY, posZ = hitPosZ,
                rotX = rotX, rotY = rotY, rotZ = rotZ, rotW = rotW,
            )

            when (result) {
                is LumiroomResult.Success -> {
                    val newRoomId = result.data.placedItem.roomDesignId
                    undoStack.addLast(ArAction.Place(result.data))
                    redoStack.clear()

                    _uiState.update { state ->
                        val newPlacedItems = state.placedItems + result.data
                        state.copy(
                            currentRoomDesignId = state.currentRoomDesignId ?: newRoomId,
                            placedItems = newPlacedItems,
                            isLoadingModel = false,
                            canUndo = true,
                            canRedo = false,
                            errorMessage = null
                        )
                    }
                }
                is LumiroomResult.Error -> {
                    _uiState.update { it.copy(isLoadingModel = false, errorMessage = result.message) }
                }
                else -> Unit
            }
        }
    }

    // ── Selection ─────────────────────────────────────────────────────────────

    fun onItemSelected(itemId: String?, multiSelect: Boolean = false) {
        if (itemId == null) {
            selectionManager.clearSelection()
            _uiState.update { it.copy(interactionMode = InteractionMode.IDLE, isPasteMode = false) }
        } else {
            selectionManager.selectItem(itemId, multiSelect)
            _uiState.update { it.copy(interactionMode = InteractionMode.IDLE, isPasteMode = false) }
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
            itemIds.forEach { itemId ->
                val itemToRemove = _uiState.value.placedItems.find { it.placedItem.id == itemId }
                if (itemToRemove != null) {
                    removeFurnitureUseCase(itemId)
                    pushUndo(ArAction.Remove(itemToRemove))
                }
            }
            
            _uiState.update { state ->
                val remainingItems = state.placedItems.filter { it.placedItem.id !in itemIds }
                state.copy(
                    placedItems = remainingItems
                )
            }
            selectionManager.clearSelection()
        }
    }
    
    fun onCopySelected() {
        val itemIds = _uiState.value.selectedItemIds
        if (itemIds.isEmpty()) return
        
        _uiState.update { it.copy(copiedFurnitureId = itemIds.first()) }
    }

    fun onPaste() {
        if (_uiState.value.copiedFurnitureId == null) return
        _uiState.update { it.copy(isPasteMode = true, interactionMode = InteractionMode.IDLE) }
    }
    
    fun toggleMeasuring() {
        _uiState.update { it.copy(isMeasuring = !it.isMeasuring) }
    }

    fun onResetTransform() {
        viewModelScope.launch(ioDispatcher) {
            val selectedIds = _uiState.value.selectedItemIds
            if (selectedIds.isEmpty()) return@launch
            
            val undoActions = mutableListOf<ArAction.Transform>()
            _uiState.update { state ->
                val newPlacedItems = state.placedItems.map { item ->
                    if (item.placedItem.id in selectedIds) {
                        val newItem = item.copy(
                            placedItem = item.placedItem.copy(
                                posX = item.placedItem.initPosX,
                                posY = item.placedItem.initPosY,
                                posZ = item.placedItem.initPosZ,
                                rotX = item.placedItem.initRotX,
                                rotY = item.placedItem.initRotY,
                                rotZ = item.placedItem.initRotZ,
                                rotW = item.placedItem.initRotW,
                                scaleX = item.placedItem.initScaleX,
                                scaleY = item.placedItem.initScaleY,
                                scaleZ = item.placedItem.initScaleZ
                            )
                        )
                        if (undoActions.none { it.itemId == item.placedItem.id }) {
                            undoActions.add(ArAction.Transform(item.placedItem.id, item, newItem))
                        }
                        newItem
                    } else {
                        item
                    }
                }
                state.copy(placedItems = newPlacedItems)
            }
            
            undoActions.forEach { action ->
                pushUndo(action)
                layoutPersistenceManager.queueSave(action.newItem.placedItem)
            }
        }
    }

    fun cancelPasteMode() {
        _uiState.update { it.copy(isPasteMode = false) }
    }
    
    fun onPastePlaneTapped(hitPosX: Float, hitPosY: Float, hitPosZ: Float, rotX: Float, rotY: Float, rotZ: Float, rotW: Float, newInstanceId: String) {
        val copiedId = _uiState.value.copiedFurnitureId ?: return
        
        // Immediately exit paste mode to prevent double taps
        _uiState.update { it.copy(isPasteMode = false) }
        
        viewModelScope.launch(ioDispatcher) {
            val itemToClone = _uiState.value.placedItems.find { it.placedItem.id == copiedId } ?: return@launch
            
            val result = placeFurnitureUseCase(
                instanceId = newInstanceId,
                furnitureId = itemToClone.furniture.id,
                roomDesignId = _uiState.value.currentRoomDesignId,
                posX = hitPosX, posY = hitPosY, posZ = hitPosZ,
                rotX = rotX, rotY = rotY, rotZ = rotZ, rotW = rotW,
                scaleX = itemToClone.placedItem.scaleX,
                scaleY = itemToClone.placedItem.scaleY,
                scaleZ = itemToClone.placedItem.scaleZ
            )
            
            if (result is LumiroomResult.Success) {
                pushUndo(ArAction.Place(result.data))
                _uiState.update { state ->
                    val updatedItems = state.placedItems + result.data
                    state.copy(
                        placedItems = updatedItems
                    )
                }
                selectionManager.clearSelection()
            }
            selectionManager.clearSelection()
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
        // We only care about Y rotation (yaw) for snapping in most AR interior designs.
        // But since SceneView gives Quaternions (rotX, rotY, rotZ, rotW) or Euler, we might need Euler to snap.
        // Wait, MeasurementManager currently expects degrees. Let's just save for now and snap if we implement Euler conversions.
        viewModelScope.launch(ioDispatcher) {
            val oldItem = _uiState.value.placedItems.find { it.placedItem.id == itemId } ?: return@launch
            
            transformFurnitureUseCase(
                itemId = itemId,
                posX = posX, posY = posY, posZ = posZ,
                rotX = rotX, rotY = rotY, rotZ = rotZ, rotW = rotW,
                scaleX = scaleX, scaleY = scaleY, scaleZ = scaleZ,
                matrixJson = matrixJson,
            )
            
            val newItem = oldItem.copy(
                placedItem = oldItem.placedItem.copy(
                    posX = posX, posY = posY, posZ = posZ,
                    rotX = rotX, rotY = rotY, rotZ = rotZ, rotW = rotW,
                    scaleX = scaleX, scaleY = scaleY, scaleZ = scaleZ,
                    transformMatrix = matrixJson
                )
            )
            pushUndo(ArAction.Transform(itemId, oldItem, newItem))
            
            // Push to LayoutPersistenceManager queue for auto-save
            layoutPersistenceManager.queueSave(newItem.placedItem)
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
            when (action) {
                is ArAction.Place -> {
                    // Undo Place = Remove
                    removeFurnitureUseCase(action.item.placedItem.id)
                    _uiState.update { state ->
                        val remaining = state.placedItems.filter { it.placedItem.id != action.item.placedItem.id }
                        state.copy(placedItems = remaining)
                    }
                }
                is ArAction.Remove -> {
                    // Undo Remove = Place back
                    val item = action.item
                    placeFurnitureUseCase(
                        instanceId = item.placedItem.id,
                        furnitureId = item.furniture.id,
                        roomDesignId = item.placedItem.roomDesignId,
                        posX = item.placedItem.posX, posY = item.placedItem.posY, posZ = item.placedItem.posZ,
                        rotX = item.placedItem.rotX, rotY = item.placedItem.rotY, rotZ = item.placedItem.rotZ, rotW = item.placedItem.rotW,
                        scaleX = item.placedItem.scaleX, scaleY = item.placedItem.scaleY, scaleZ = item.placedItem.scaleZ
                    )
                    _uiState.update { state ->
                        val updated = state.placedItems + item
                        state.copy(placedItems = updated)
                    }
                }
                is ArAction.Transform -> {
                    transformFurnitureUseCase(
                        itemId = action.itemId,
                        posX = action.oldItem.placedItem.posX, posY = action.oldItem.placedItem.posY, posZ = action.oldItem.placedItem.posZ,
                        rotX = action.oldItem.placedItem.rotX, rotY = action.oldItem.placedItem.rotY, rotZ = action.oldItem.placedItem.rotZ, rotW = action.oldItem.placedItem.rotW,
                        scaleX = action.oldItem.placedItem.scaleX, scaleY = action.oldItem.placedItem.scaleY, scaleZ = action.oldItem.placedItem.scaleZ,
                        matrixJson = action.oldItem.placedItem.transformMatrix
                    )
                    _uiState.update { state -> 
                        state.copy(placedItems = state.placedItems.map { if (it.placedItem.id == action.itemId) action.oldItem else it })
                    }
                }
            }
        }
    }

    fun onRedo() {
        val action = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(action)
        _uiState.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty()) }
        
        viewModelScope.launch(ioDispatcher) {
            when (action) {
                is ArAction.Place -> {
                    // Redo Place = Place back
                    val item = action.item
                    placeFurnitureUseCase(
                        instanceId = item.placedItem.id,
                        furnitureId = item.furniture.id,
                        roomDesignId = item.placedItem.roomDesignId,
                        posX = item.placedItem.posX, posY = item.placedItem.posY, posZ = item.placedItem.posZ,
                        rotX = item.placedItem.rotX, rotY = item.placedItem.rotY, rotZ = item.placedItem.rotZ, rotW = item.placedItem.rotW,
                        scaleX = item.placedItem.scaleX, scaleY = item.placedItem.scaleY, scaleZ = item.placedItem.scaleZ
                    )
                    _uiState.update { state ->
                        val updated = state.placedItems + item
                        state.copy(placedItems = updated)
                    }
                }
                is ArAction.Remove -> {
                    // Redo Remove = Remove it again
                    removeFurnitureUseCase(action.item.placedItem.id)
                    _uiState.update { state ->
                        val remaining = state.placedItems.filter { it.placedItem.id != action.item.placedItem.id }
                        state.copy(placedItems = remaining)
                    }
                }
                is ArAction.Transform -> {
                    transformFurnitureUseCase(
                        itemId = action.itemId,
                        posX = action.newItem.placedItem.posX, posY = action.newItem.placedItem.posY, posZ = action.newItem.placedItem.posZ,
                        rotX = action.newItem.placedItem.rotX, rotY = action.newItem.placedItem.rotY, rotZ = action.newItem.placedItem.rotZ, rotW = action.newItem.placedItem.rotW,
                        scaleX = action.newItem.placedItem.scaleX, scaleY = action.newItem.placedItem.scaleY, scaleZ = action.newItem.placedItem.scaleZ,
                        matrixJson = action.newItem.placedItem.transformMatrix
                    )
                    _uiState.update { state -> 
                        state.copy(placedItems = state.placedItems.map { if (it.placedItem.id == action.itemId) action.newItem else it })
                    }
                }
            }
        }
    }

    // ── Save & Export ─────────────────────────────────────────────────────────

    fun onSaveRoom(name: String) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isSaving = true) }
            val result = saveArSessionUseCase(
                name = name,
                items = _uiState.value.placedItems.map { it.placedItem },
            )
            _uiState.update {
                it.copy(
                    isSaving = false,
                    saveSuccess = result is LumiroomResult.Success,
                    errorMessage = (result as? LumiroomResult.Error)?.message,
                )
            }
        }
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
            is VoiceCommand.Help -> {
                _uiState.update { it.copy(showVoiceHelpDialog = true) }
            }
            is VoiceCommand.Place -> {
                viewModelScope.launch(ioDispatcher) {
                    val item = furnitureRepository.findFirstByNameOrCategory(command.itemName)
                    if (item != null) {
                        // Place at origin for voice command
                        val voiceInstanceId = java.util.UUID.randomUUID().toString()
                        onPlaneTapped(voiceInstanceId, item.id, 0f, 0f, -1f)
                        _events.emit(ArViewEvent.ShowSnackbar("Placing ${item.name}"))
                    } else {
                        _events.emit(ArViewEvent.ShowSnackbar("Could not find furniture matching: ${command.itemName}"))
                    }
                }
            }
            is VoiceCommand.Remove -> {
                val match = _uiState.value.placedItems.find { it.furniture.name.contains(command.itemName, ignoreCase = true) }
                if (match != null) {
                    viewModelScope.launch(ioDispatcher) {
                        removeFurnitureUseCase(match.placedItem.id)
                        pushUndo(ArAction.Remove(match))
                        _uiState.update { s -> s.copy(placedItems = s.placedItems.filter { it.placedItem.id != match.placedItem.id }) }
                    }
                } else {
                    viewModelScope.launch { _events.emit(ArViewEvent.ShowSnackbar("Cannot find item to remove")) }
                }
            }
            is VoiceCommand.Replace -> {
                // Remove selected and place new
                val selectedId = _uiState.value.selectedItemIds.firstOrNull()
                if (selectedId != null) {
                    val oldItem = _uiState.value.placedItems.find { it.placedItem.id == selectedId }
                    if (oldItem != null) {
                        viewModelScope.launch(ioDispatcher) {
                            val newItem = furnitureRepository.findFirstByNameOrCategory(command.itemName)
                            if (newItem != null) {
                                removeFurnitureUseCase(selectedId)
                                val replaceInstanceId = java.util.UUID.randomUUID().toString()
                                onPlaneTapped(
                                    replaceInstanceId,
                                    newItem.id,
                                    oldItem.placedItem.posX, oldItem.placedItem.posY, oldItem.placedItem.posZ,
                                    oldItem.placedItem.rotX, oldItem.placedItem.rotY, oldItem.placedItem.rotZ, oldItem.placedItem.rotW
                                )
                            } else {
                                _events.emit(ArViewEvent.ShowSnackbar("Cannot find replacement item"))
                            }
                        }
                    }
                }
            }
            is VoiceCommand.Select -> {
                val match = _uiState.value.placedItems.find { it.furniture.name.contains(command.itemName, ignoreCase = true) }
                if (match != null) {
                    onItemSelected(match.placedItem.id, multiSelect = false)
                }
            }
            is VoiceCommand.SelectLast -> {
                val lastItem = _uiState.value.placedItems.maxByOrNull { it.placedItem.createdAt }
                if (lastItem != null) {
                    onItemSelected(lastItem.placedItem.id, multiSelect = false)
                }
            }
            is VoiceCommand.Deselect -> {
                selectionManager.clearSelection()
            }
            is VoiceCommand.Rotate -> {
                val selectedId = _uiState.value.selectedItemIds.firstOrNull()
                if (selectedId != null) {
                    val item = _uiState.value.placedItems.find { it.placedItem.id == selectedId }
                    if (item != null) {
                        // Approximating rotation for now, ideally handled in engine
                        viewModelScope.launch { _events.emit(ArViewEvent.RotateSelectedItem(command.degrees)) }
                    }
                }
            }
            is VoiceCommand.Scale -> {
                 viewModelScope.launch { _events.emit(ArViewEvent.ScaleSelectedItem(command.factor)) }
            }
            is VoiceCommand.Move -> {
                // Needs engine support, skip for pure viewmodel
            }
            is VoiceCommand.Duplicate -> onCopySelected()
            is VoiceCommand.DeleteSelected -> onRemoveSelectedItems()
            is VoiceCommand.SaveRoom -> onSaveRoom(_uiState.value.currentRoomName)
            is VoiceCommand.LoadRoom -> { /* Implementation for later navigation */ }
            is VoiceCommand.CreateRoom -> { /* Implementation for later navigation */ }
            is VoiceCommand.DeleteRoom -> { /* Delete room from db */ }
            is VoiceCommand.OpenCatalog -> viewModelScope.launch { _events.emit(ArViewEvent.NavigateToCatalog) }
            is VoiceCommand.CloseCatalog -> { /* Navigate back */ }
            is VoiceCommand.SearchCatalog -> viewModelScope.launch { _events.emit(ArViewEvent.NavigateToCatalog) }
            is VoiceCommand.ShowPlanes -> togglePlaneVisualization()
            is VoiceCommand.HidePlanes -> togglePlaneVisualization()
            is VoiceCommand.FocusSelected -> { /* Engine command */ }
            is VoiceCommand.ResetCamera -> { /* Engine command */ }
            is VoiceCommand.TakeScreenshot -> viewModelScope.launch { _events.emit(ArViewEvent.TakeScreenshot) }
            is VoiceCommand.ShareScreenshot -> viewModelScope.launch { _events.emit(ArViewEvent.ShareScreenshot) }
            is VoiceCommand.Undo -> onUndo()
            is VoiceCommand.Redo -> onRedo()
            is VoiceCommand.ClearScene -> {
                 viewModelScope.launch { _events.emit(ArViewEvent.ShowSnackbar("Clear scene not fully implemented via voice")) }
            }
            is VoiceCommand.Unknown -> {
                 viewModelScope.launch { _events.emit(ArViewEvent.ShowSnackbar("I didn't catch that. Say 'Help' for commands.")) }
            }
        }
    }
}

/** Reversible AR actions for undo/redo history tracking. */
sealed class ArAction {
    data class Place(val item: com.lumiroom.core.database.relation.PlacedItemWithFurniture) : ArAction()
    data class Remove(val item: com.lumiroom.core.database.relation.PlacedItemWithFurniture) : ArAction()
    data class Transform(
        val itemId: String,
        val oldItem: com.lumiroom.core.database.relation.PlacedItemWithFurniture,
        val newItem: com.lumiroom.core.database.relation.PlacedItemWithFurniture
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
