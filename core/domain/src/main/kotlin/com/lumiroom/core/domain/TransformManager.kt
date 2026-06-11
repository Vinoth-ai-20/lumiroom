package com.lumiroom.core.domain
 
import com.lumiroom.core.domain.model.RoomFurniture
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sin

/**
 * Centralized manager for handling transformations (position, rotation, scale)
 * in object-local space. Resolves drift and unexpected scaling bugs by
 * ensuring proper pivot points and preserving initial transform properties.
 */
@Singleton
class TransformManager @Inject constructor() {

    fun resetTransform(furniture: RoomFurniture): RoomFurniture {
        return furniture.copy(
            positionX = furniture.initialPositionX,
            positionY = furniture.initialPositionY,
            positionZ = furniture.initialPositionZ,
            rotation = furniture.initialRotation,
            scaleX = furniture.initialScaleX,
            scaleY = furniture.initialScaleY,
            scaleZ = furniture.initialScaleZ
        )
    }

    /**
     * Moves furniture by a delta relative to its local coordinate space.
     * The rotation determines the object's local axes.
     */
    fun move(furniture: RoomFurniture, localDeltaX: Float, localDeltaY: Float, localDeltaZ: Float = 0f): RoomFurniture {
        val rad = Math.toRadians(furniture.rotation.toDouble())
        val cos = cos(rad).toFloat()
        val sin = sin(rad).toFloat()
        
        // Convert local translation to world translation
        val worldDeltaX = localDeltaX * cos - localDeltaY * sin
        val worldDeltaY = localDeltaX * sin + localDeltaY * cos
        
        return furniture.copy(
            positionX = furniture.positionX + worldDeltaX,
            positionY = furniture.positionY + worldDeltaY,
            positionZ = furniture.positionZ + localDeltaZ
        )
    }

    /**
     * Rotates furniture around its own center.
     */
    fun rotate(furniture: RoomFurniture, deltaDegrees: Float): RoomFurniture {
        return furniture.copy(
            rotation = furniture.rotation + deltaDegrees
        )
    }

    /**
     * Scales furniture uniformly relative to its own center.
     */
    fun scale(furniture: RoomFurniture, deltaScale: Float): RoomFurniture {
        val newScale = (furniture.scaleX + deltaScale).coerceIn(0.5f, 3.0f)
        return furniture.copy(
            scaleX = newScale,
            scaleY = newScale,
            scaleZ = newScale
        )
    }

    /**
     * Sets absolute scale relative to initial scale (e.g. 1.1 for 110%).
     */
    fun scaleAbsolute(furniture: RoomFurniture, absoluteScale: Float): RoomFurniture {
        val newScale = (furniture.initialScaleX * absoluteScale).coerceIn(0.5f, 3.0f)
        return furniture.copy(
            scaleX = newScale,
            scaleY = newScale,
            scaleZ = newScale
        )
    }
    
    /**
     * Sets absolute rotation
     */
    fun rotateAbsolute(furniture: RoomFurniture, absoluteDegrees: Float): RoomFurniture {
        return furniture.copy(
            rotation = absoluteDegrees
        )
    }
}
