package com.lumiroom.core.domain

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

@Singleton
class MeasurementManager @Inject constructor() {

    fun calculateDistance3D(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
        return sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2) + (z2 - z1).pow(2))
    }

    fun calculateDistance2D(x1: Float, z1: Float, x2: Float, z2: Float): Float {
        return sqrt((x2 - x1).pow(2) + (z2 - z1).pow(2))
    }

    fun checkCollision2D(
        x1: Float, z1: Float, width1: Float, depth1: Float,
        x2: Float, z2: Float, width2: Float, depth2: Float
    ): Boolean {
        // Simple AABB collision on XZ plane
        val xOverlap = (x1 - width1/2) < (x2 + width2/2) && (x1 + width1/2) > (x2 - width2/2)
        val zOverlap = (z1 - depth1/2) < (z2 + depth2/2) && (z1 + depth1/2) > (z2 - depth2/2)
        return xOverlap && zOverlap
    }

    fun snapRotation(currentRotationDegrees: Float, snapInterval: Float = 15f): Float {
        val mod = currentRotationDegrees % snapInterval
        val half = snapInterval / 2f
        return if (mod > half) {
            currentRotationDegrees + (snapInterval - mod)
        } else {
            currentRotationDegrees - mod
        }
    }
}
