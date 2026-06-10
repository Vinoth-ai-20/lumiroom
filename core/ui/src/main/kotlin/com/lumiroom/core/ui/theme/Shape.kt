package com.lumiroom.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Lumiroom shape system.
 *
 * Uses generous rounding throughout to convey a modern, premium feel.
 * AR overlay elements use circular shapes; cards and panels use rounded rectangles.
 */
val LumiroomShapes = Shapes(
    // Used for: chips, small badges
    extraSmall = RoundedCornerShape(6.dp),

    // Used for: text fields, small buttons
    small = RoundedCornerShape(10.dp),

    // Used for: cards, bottom sheets, dialogs
    medium = RoundedCornerShape(16.dp),

    // Used for: full-width cards, panel containers
    large = RoundedCornerShape(24.dp),

    // Used for: bottom sheets, full-screen overlays
    extraLarge = RoundedCornerShape(28.dp),
)
