package com.lumiroom.core.domain

import com.lumiroom.core.database.dao.PlacedItemDao
import com.lumiroom.core.database.entity.PlacedItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LayoutPersistenceManager @Inject constructor(
    private val placedItemDao: PlacedItemDao
) {
    private var autoSaveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // A queue of items that need to be saved
    private val pendingSaves = mutableMapOf<String, PlacedItemEntity>()

    fun startAutoSaveLoop() {
        if (autoSaveJob?.isActive == true) return

        autoSaveJob = scope.launch {
            while (true) {
                delay(30_000) // 30 seconds
                executeSave()
            }
        }
    }

    fun stopAutoSaveLoop() {
        autoSaveJob?.cancel()
        autoSaveJob = null
        scope.launch { executeSave() } // flush on stop
    }

    fun queueSave(item: PlacedItemEntity) {
        synchronized(pendingSaves) {
            pendingSaves[item.id] = item
        }
    }

    suspend fun manualSave() {
        executeSave()
    }

    private suspend fun executeSave() {
        val itemsToSave = synchronized(pendingSaves) {
            val copy = pendingSaves.values.toList()
            pendingSaves.clear()
            copy
        }

        if (itemsToSave.isNotEmpty()) {
            placedItemDao.insertAll(itemsToSave)
        }
    }
}
