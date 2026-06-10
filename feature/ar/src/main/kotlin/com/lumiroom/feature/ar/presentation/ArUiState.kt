package com.lumiroom.feature.ar.presentation

import com.lumiroom.core.database.entity.PlacedItemEntity
import com.lumiroom.core.database.relation.PlacedItemWithFurniture

/**
 * Immutable UI state for the AR screen.
 *
 * Emitted by [ArViewModel] as a [kotlinx.coroutines.flow.StateFlow].
 * The Compose UI collects this and renders accordingly with no business logic.
 */
data class ArUiState(

    // ── Session State ─────────────────────────────────────────────────────────
    val sessionState: ArSessionState = ArSessionState.Initializing,

    // ── Plane Detection ───────────────────────────────────────────────────────
    val planesDetected: Boolean = false,
    val planeCount: Int = 0,

    // ── Placed Items ──────────────────────────────────────────────────────────
    val placedItems: List<PlacedItemWithFurniture> = emptyList(),
    val selectedItemIds: Set<String> = emptySet(),
    val lockedItemIds: Set<String> = emptySet(),
    val hiddenItemIds: Set<String> = emptySet(),
    val isMeasuring: Boolean = false,

    // ── Visualization ─────────────────────────────────────────────────────────
    val showPlaneVisualization: Boolean = true,
    val showLabels: Boolean = true,
    
    // ── Loading / Error ───────────────────────────────────────────────────────
    val isLoadingModel: Boolean = false,
    val loadingModelName: String? = null,
    val errorMessage: String? = null,

    // ── Undo/Redo ─────────────────────────────────────────────────────────────
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,

    // ── Save ──────────────────────────────────────────────────────────────────
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,

    // ── AR Hints ─────────────────────────────────────────────────────────────
    val showScanSurfaceHint: Boolean = true,
    val showTapToPlaceHint: Boolean = false,

    // ── Voice ─────────────────────────────────────────────────────────────────
    val isVoiceListening: Boolean = false,
    val voiceTranscript: String? = null,
    val showVoiceHelpDialog: Boolean = false,

    // ── Current Room ──────────────────────────────────────────────────────────
    val currentRoomDesignId: String? = null,
    val currentRoomName: String = "Untitled Room",
    
    // ── Interaction State ─────────────────────────────────────────────────────
    val interactionMode: InteractionMode = InteractionMode.IDLE
) {
    // ── Analytics (Computed) ──────────────────────────────────────────────────
    val totalCostEstimate: Double
        get() = placedItems.filter { it.placedItem.id !in hiddenItemIds }.sumOf { it.furniture.priceEstimate ?: 0.0 }
    val totalItemCount: Int
        get() = placedItems.count { it.placedItem.id !in hiddenItemIds }
}

enum class InteractionMode {
    IDLE,
    MOVE,
    ROTATE,
    SCALE
}

/**
 * Represents the ARCore session lifecycle states, driving the scan-surface
 * hint UX and the tap-to-place availability.
 */
sealed class ArSessionState {
    /** ARCore is being initialized. Camera not yet active. */
    object Initializing : ArSessionState()

    /** Camera active, searching for planes. */
    object Scanning : ArSessionState()

    /** At least one plane detected — user can now tap to place. */
    object Ready : ArSessionState()

    /** Session paused (app backgrounded). */
    object Paused : ArSessionState()

    /** ARCore not supported or camera permission denied. */
    data class Error(val reason: String) : ArSessionState()
}
