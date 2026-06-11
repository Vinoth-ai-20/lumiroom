package com.lumiroom.feature.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumiroom.core.database.model.Furniture
import com.lumiroom.core.database.repository.FurnitureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.lumiroom.core.domain.SharedRoomRepository

data class CatalogUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedCategory: String? = null,
    val searchQuery: String = "",
)

@OptIn(FlowPreview::class)
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val repository: FurnitureRepository,
    private val sharedRoomRepository: SharedRoomRepository
) : ViewModel() {

    suspend fun createRoom(name: String, roomType: String): String {
        return sharedRoomRepository.createRoom(name, roomType)
    }

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    
    // Derived state for the catalog grid
    val furniture: StateFlow<List<Furniture>> = combine(
        _selectedCategory,
        _searchQuery.debounce(300L) // Debounce search
    ) { category, query ->
        Pair(category, query)
    }.flatMapLatest { (category, query) ->
        val safeQuery = query.takeIf { it.isNotBlank() }
        if (category == "Favorites") {
            repository.getFilteredFurniture(null, safeQuery).map { list -> list.filter { it.isFavorite } }
        } else {
            repository.getFilteredFurniture(category, safeQuery)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun toggleFavorite(furnitureId: String) {
        viewModelScope.launch {
            val current = furniture.value.find { it.id == furnitureId } ?: return@launch
            repository.toggleFavorite(furnitureId, !current.isFavorite)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onCategorySelected(category: String?) {
        val newCategory = if (category == "All" || _selectedCategory.value == category) null else category
        _selectedCategory.value = newCategory
        _uiState.update { it.copy(selectedCategory = newCategory) }
    }
}
