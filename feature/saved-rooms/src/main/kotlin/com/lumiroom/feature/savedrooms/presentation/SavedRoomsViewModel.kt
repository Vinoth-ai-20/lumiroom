package com.lumiroom.feature.savedrooms.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumiroom.core.database.entity.RoomPlanEntity
import com.lumiroom.core.domain.SharedRoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedRoomsViewModel @Inject constructor(
    private val sharedRoomRepository: SharedRoomRepository
) : ViewModel() {

    val rooms: StateFlow<List<RoomPlanEntity>> = sharedRoomRepository.getAllRooms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createRoom(name: String, roomType: String) {
        viewModelScope.launch {
            sharedRoomRepository.createRoom(name, roomType)
        }
    }

    fun renameRoom(roomId: String, newName: String) {
        viewModelScope.launch {
            sharedRoomRepository.renameRoom(roomId, newName)
        }
    }

    fun duplicateRoom(roomId: String) {
        viewModelScope.launch {
            sharedRoomRepository.duplicateRoom(roomId)
        }
    }

    fun deleteRoom(roomId: String) {
        viewModelScope.launch {
            sharedRoomRepository.deleteRoom(roomId)
        }
    }
}
