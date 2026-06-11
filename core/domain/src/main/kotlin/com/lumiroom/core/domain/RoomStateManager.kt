package com.lumiroom.core.domain

import com.lumiroom.core.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomStateManager @Inject constructor(
    private val repository: SharedRoomRepository
) {
    private val _roomModel = MutableStateFlow<RoomModel?>(null)
    val roomModel: StateFlow<RoomModel?> = _roomModel.asStateFlow()

    private val undoStack = ArrayDeque<RoomModel>()
    private val redoStack = ArrayDeque<RoomModel>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun initialize(model: RoomModel) {
        if (_roomModel.value?.planId != model.planId) {
            _roomModel.value = model
            undoStack.clear()
            redoStack.clear()
        }
    }

    /**
     * Pushes current state to undo stack, clears redo stack, and updates state.
     */
    fun updateState(mutator: (RoomModel) -> RoomModel) {
        val currentState = _roomModel.value ?: return
        val newState = mutator(currentState)
        
        // Push to undo history only if meaningful geometry/furniture changed, 
        // to avoid storing state for simple camera panning or selection
        if (stateChangedSignificantly(currentState, newState)) {
            undoStack.addLast(currentState)
            if (undoStack.size > 50) {
                undoStack.removeFirst()
            }
            redoStack.clear()
        }
        
        _roomModel.value = newState
    }
    
    /**
     * Updates state without affecting history (for camera, selection, drag)
     */
    fun updateStateTransient(mutator: (RoomModel) -> RoomModel) {
        val currentState = _roomModel.value ?: return
        _roomModel.value = mutator(currentState)
    }

    private fun stateChangedSignificantly(old: RoomModel, new: RoomModel): Boolean {
        return old.walls != new.walls || 
               old.doors != new.doors || 
               old.windows != new.windows || 
               old.furniture != new.furniture ||
               old.planeBoundaries != new.planeBoundaries
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val currentState = _roomModel.value ?: return
        val previousState = undoStack.removeLast()
        
        // Push current to redo
        redoStack.addLast(currentState)
        
        // Keep transient state like camera & selection from current, apply geometry from previous
        _roomModel.value = previousState.copy(
            cameraState = currentState.cameraState,
            selectionState = currentState.selectionState
        )
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val currentState = _roomModel.value ?: return
        val nextState = redoStack.removeLast()
        
        // Push current to undo
        undoStack.addLast(currentState)
        
        // Restore next state
        _roomModel.value = nextState.copy(
            cameraState = currentState.cameraState,
            selectionState = currentState.selectionState
        )
    }
}
