package com.lumiroom.core.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FurnitureSelectionManager @Inject constructor() {

    private val _selectedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedItemIds: StateFlow<Set<String>> = _selectedItemIds.asStateFlow()

    private val _lockedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val lockedItemIds: StateFlow<Set<String>> = _lockedItemIds.asStateFlow()

    private val _hiddenItemIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenItemIds: StateFlow<Set<String>> = _hiddenItemIds.asStateFlow()

    fun selectItem(id: String, multiSelect: Boolean = false) {
        _selectedItemIds.update { current ->
            if (multiSelect) {
                if (current.contains(id)) current - id else current + id
            } else {
                setOf(id)
            }
        }
    }

    fun clearSelection() {
        _selectedItemIds.value = emptySet()
    }

    fun toggleLock(id: String) {
        _lockedItemIds.update { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    fun toggleVisibility(id: String) {
        _hiddenItemIds.update { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    fun isLocked(id: String): Boolean = _lockedItemIds.value.contains(id)
    fun isHidden(id: String): Boolean = _hiddenItemIds.value.contains(id)
}
