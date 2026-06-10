package com.lumiroom.feature.catalog.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumiroom.core.database.model.Furniture
import com.lumiroom.core.database.repository.FurnitureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FurnitureDetailViewModel @Inject constructor(
    private val repository: FurnitureRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Assuming the NavGraph passes "furnitureId"
    private val furnitureId: String = checkNotNull(savedStateHandle["furnitureId"])

    private val _uiState = MutableStateFlow<Furniture?>(null)
    val uiState: StateFlow<Furniture?> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getFurnitureById(furnitureId).collect { furniture ->
                _uiState.value = furniture
            }
        }
    }
}
