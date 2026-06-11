package com.lumiroom.feature.roomplanner.domain.geometry

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class Point2D(val x: Float, val y: Float) {
    fun distanceTo(other: Point2D): Float {
        return hypot(other.x - x, other.y - y)
    }

    operator fun plus(other: Point2D) = Point2D(x + other.x, y + other.y)
    operator fun minus(other: Point2D) = Point2D(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Point2D(x * scalar, y * scalar)
}

data class LineSegment(val start: Point2D, val end: Point2D) {
    val length: Float
        get() = start.distanceTo(end)

    val angle: Float
        get() = atan2(end.y - start.y, end.x - start.x)

    fun getNormalVector(): Point2D {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val len = length
        if (len == 0f) return Point2D(0f, 1f)
        return Point2D(-dy / len, dx / len)
    }

    fun projectPoint(point: Point2D): Point2D {
        val l2 = length * length
        if (l2 == 0f) return start
        var t = ((point.x - start.x) * (end.x - start.x) + (point.y - start.y) * (end.y - start.y)) / l2
        t = t.coerceIn(0f, 1f)
        return Point2D(start.x + t * (end.x - start.x), start.y + t * (end.y - start.y))
    }

    fun distanceToPoint(point: Point2D): Float {
        val projection = projectPoint(point)
        return point.distanceTo(projection)
    }
}

data class BoundingBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun contains(point: Point2D): Boolean {
        return point.x in left..right && point.y in top..bottom
    }

    fun intersects(other: BoundingBox): Boolean {
        return !(left > other.right || right < other.left || top > other.bottom || bottom < other.top)
    }
}

data class RotatedRect(val center: Point2D, val width: Float, val height: Float, val rotationDegrees: Float) {
    fun getCorners(): List<Point2D> {
        val rad = Math.toRadians(rotationDegrees.toDouble()).toFloat()
        val cos = kotlin.math.cos(rad)
        val sin = kotlin.math.sin(rad)
        val hw = width / 2f
        val hh = height / 2f

        val dx1 = hw * cos - hh * sin
        val dy1 = hw * sin + hh * cos
        val dx2 = -hw * cos - hh * sin
        val dy2 = -hw * sin + hh * cos

        return listOf(
            Point2D(center.x + dx1, center.y + dy1),
            Point2D(center.x + dx2, center.y + dy2),
            Point2D(center.x - dx1, center.y - dy1),
            Point2D(center.x - dx2, center.y - dy2)
        )
    }

    fun contains(point: Point2D): Boolean {
        val rad = Math.toRadians(-rotationDegrees.toDouble()).toFloat()
        val cos = kotlin.math.cos(rad)
        val sin = kotlin.math.sin(rad)
        val dx = point.x - center.x
        val dy = point.y - center.y
        val localX = dx * cos - dy * sin
        val localY = dx * sin + dy * cos
        return kotlin.math.abs(localX) <= width / 2f && kotlin.math.abs(localY) <= height / 2f
    }

    fun intersects(other: RotatedRect): Boolean {
        val c1 = getCorners()
        val c2 = other.getCorners()
        val axes = getAxes(c1) + getAxes(c2)

        for (axis in axes) {
            val (min1, max1) = project(c1, axis)
            val (min2, max2) = project(c2, axis)
            if (max1 < min2 || max2 < min1) {
                return false // Separating axis found
            }
        }
        return true
    }

    private fun getAxes(corners: List<Point2D>): List<Point2D> {
        return listOf(
            getNormal(corners[0], corners[1]),
            getNormal(corners[1], corners[2])
        )
    }

    private fun getNormal(p1: Point2D, p2: Point2D): Point2D {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val len = hypot(dx, dy)
        return Point2D(-dy / len, dx / len)
    }

    private fun project(corners: List<Point2D>, axis: Point2D): Pair<Float, Float> {
        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        for (corner in corners) {
            val projection = corner.x * axis.x + corner.y * axis.y
            if (projection < min) min = projection
            if (projection > max) max = projection
        }
        return Pair(min, max)
    }
}

/**
 * Returns the intersection point of two line segments, or null if they do not intersect.
 */
fun getIntersection(l1: LineSegment, l2: LineSegment): Point2D? {
    val x1 = l1.start.x
    val y1 = l1.start.y
    val x2 = l1.end.x
    val y2 = l1.end.y

    val x3 = l2.start.x
    val y3 = l2.start.y
    val x4 = l2.end.x
    val y4 = l2.end.y

    val denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
    if (denom == 0f) return null // Parallel or collinear

    val t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom
    val u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denom

    if (t in 0f..1f && u in 0f..1f) {
        return Point2D(x1 + t * (x2 - x1), y1 + t * (y2 - y1))
    }
    return null
}

fun snapToGrid(value: Float, gridSize: Float): Float {
    return kotlin.math.round(value / gridSize) * gridSize
}
