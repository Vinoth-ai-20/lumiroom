package com.lumiroom.core.domain.model

/**
 * Maps coordinates between the 2D Room Planner (cm) and the AR Scene (meters).
 *
 * 2D Space:
 * - X axis: Left to Right
 * - Y axis: Top to Bottom (or Bottom to Top, depends on canvas, but usually Top-Down is X/Y mapped to X/Z)
 * - Units: cm
 *
 * AR Space:
 * - X axis: Right
 * - Y axis: Up (elevation)
 * - Z axis: Back (so -Z is forward)
 * - Units: meters
 *
 * The center of the 2D grid (0,0) exactly corresponds to the RoomAnchor (0,0,0) in AR space.
 */
object RoomCoordinateSystem {
    
    // Converts 2D X (cm) to AR X (meters)
    fun x2DToAr(xCm: Float): Float = xCm / 100f
    
    // Converts 2D Y (cm) to AR Z (meters)
    fun y2DToAr(yCm: Float): Float = yCm / 100f
    
    // Converts AR X (meters) to 2D X (cm)
    fun xArTo2D(xMeters: Float): Float = xMeters * 100f
    
    // Converts AR Z (meters) to 2D Y (cm)
    fun zArTo2D(zMeters: Float): Float = zMeters * 100f

    // Converts 2D scale/rotation to AR scale/rotation
    // 2D rotation is usually in degrees. AR is usually quaternions or degrees around Y axis.
    fun rotation2DToArY(rotationDegrees: Float): Float = -rotationDegrees

    fun rotationArYTo2D(rotationDegrees: Float): Float = -rotationDegrees
}
