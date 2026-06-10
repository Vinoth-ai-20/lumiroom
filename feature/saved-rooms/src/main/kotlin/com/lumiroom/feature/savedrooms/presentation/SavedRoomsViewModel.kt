package com.lumiroom.feature.savedrooms.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumiroom.core.database.entity.RoomDesignEntity
import com.lumiroom.core.domain.RoomPlannerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedRoomsViewModel @Inject constructor(
    private val roomPlannerManager: RoomPlannerManager
) : ViewModel() {

    val rooms: StateFlow<List<RoomDesignEntity>> = roomPlannerManager.getAllRooms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createRoom(name: String, roomType: String) {
        viewModelScope.launch {
            roomPlannerManager.createRoom(name, roomType)
        }
    }

    fun renameRoom(roomId: String, newName: String) {
        viewModelScope.launch {
            roomPlannerManager.renameRoom(roomId, newName)
        }
    }

    fun duplicateRoom(roomId: String) {
        viewModelScope.launch {
            roomPlannerManager.duplicateRoom(roomId)
        }
    }

    fun deleteRoom(roomId: String) {
        viewModelScope.launch {
            roomPlannerManager.deleteRoom(roomId)
        }
    }
}
