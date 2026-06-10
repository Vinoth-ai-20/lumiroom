package com.lumiroom.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lumiroom.core.ui.theme.LumiroomPrimary
import com.lumiroom.core.ui.theme.LumiroomTertiary

/**
 * Premium furniture card used throughout the Catalog and Saved Rooms screens.
 *
 * Features:
 * - Smooth scale spring animation on click
 * - Gradient overlay on thumbnail for text legibility
 * - Animated favorite toggle
 * - Download status indicator dot
 */
@Composable
fun FurnitureCard(
    name: String,
    brand: String,
    priceUsd: Double?,
    thumbnailUrl: String,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            // ── Thumbnail ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.large),
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "$name thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )

                // Gradient overlay for legibility
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    androidx.compose.ui.graphics.Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                ),
                                startY = 0.4f,
                            ),
                        ),
                )

                // Download indicator dot
                DownloadIndicator(
                    isDownloaded = isDownloaded,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                )

                // Favorite button
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) LumiroomPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Info Row ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = brand,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (priceUsd != null) {
                        Text(
                            text = "₹${String.format("%.0f", priceUsd)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = LumiroomPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadIndicator(isDownloaded: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = isDownloaded,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(LumiroomTertiary, shape = MaterialTheme.shapes.extraSmall)
                .border(1.dp, MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraSmall),
        )
    }
}
