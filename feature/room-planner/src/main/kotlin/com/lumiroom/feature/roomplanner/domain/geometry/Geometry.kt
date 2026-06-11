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
