package com.vilync.ophthalmicerp.feature.designer.domain.geometry

import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions
import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2

/**
 * Pure mathematical component for document spatial transformations.
 * Sole authority for coordinate and rotation math.
 */
object GeometryTransform {

    /**
     * Normalizes an angle to the [0, 360) range.
     */
    fun normalizeAngle(angle: Double): Double {
        var normalized = angle % 360.0
        if (normalized < 0) normalized += 360.0
        return normalized
    }

    /**
     * Rotates a point around a pivot by a given angle in degrees (clockwise).
     */
    fun rotatePoint(point: Point2D, pivot: Point2D, angleDeg: Double): Point2D {
        val rad = Math.toRadians(angleDeg)
        val tx = point.x - pivot.x
        val ty = point.y - pivot.y
        
        return Point2D(
            x = pivot.x + tx * cos(rad) - ty * sin(rad),
            y = pivot.y + tx * sin(rad) + ty * cos(rad),
            unit = point.unit
        )
    }

    /**
     * Calculates the geometric center of an object.
     */
    fun calculateCenter(pos: Point2D, dim: Dimensions): Point2D {
        return Point2D(
            x = pos.x + dim.width / 2.0,
            y = pos.y + dim.height / 2.0,
            unit = pos.unit
        )
    }

    /**
     * Calculates absolute coordinates of logical points on a box (corners and edge centers).
     */
    fun getBoxPoint(pos: Point2D, dim: Dimensions, pointType: BoxPointType): Point2D {
        return when (pointType) {
            BoxPointType.TOP_LEFT -> Point2D(pos.x, pos.y, pos.unit)
            BoxPointType.TOP_CENTER -> Point2D(pos.x + dim.width / 2.0, pos.y, pos.unit)
            BoxPointType.TOP_RIGHT -> Point2D(pos.x + dim.width, pos.y, pos.unit)
            BoxPointType.RIGHT_CENTER -> Point2D(pos.x + dim.width, pos.y + dim.height / 2.0, pos.unit)
            BoxPointType.BOTTOM_RIGHT -> Point2D(pos.x + dim.width, pos.y + dim.height, pos.unit)
            BoxPointType.BOTTOM_CENTER -> Point2D(pos.x + dim.width / 2.0, pos.y + dim.height, pos.unit)
            BoxPointType.BOTTOM_LEFT -> Point2D(pos.x, pos.y + dim.height, pos.unit)
            BoxPointType.LEFT_CENTER -> Point2D(pos.x, pos.y + dim.height / 2.0, pos.unit)
            BoxPointType.CENTER -> calculateCenter(pos, dim)
        }
    }

    enum class BoxPointType {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT, 
        RIGHT_CENTER, BOTTOM_RIGHT, BOTTOM_CENTER, 
        BOTTOM_LEFT, LEFT_CENTER, CENTER
    }

    /**
     * Checks if a point is inside an Oriented Bounding Box (OBB).
     */
    fun isPointInBox(point: Point2D, pos: Point2D, dim: Dimensions, rotation: Float): Boolean {
        val center = calculateCenter(pos, dim)
        
        // 1. Translate point relative to center
        val tx = point.x - center.x
        val ty = point.y - center.y
        
        // 2. Rotate point back by -rotation (Inverse Transform)
        val rad = Math.toRadians(-rotation.toDouble())
        val rx = tx * cos(rad) - ty * sin(rad)
        val ry = tx * sin(rad) + ty * cos(rad)
        
        // 3. Simple AABB check in local space
        val halfW = dim.width / 2.0
        val halfH = dim.height / 2.0
        
        return rx >= -halfW && rx <= halfW && ry >= -halfH && ry <= halfH
    }

    /**
     * Calculates the angle in degrees between the positive X axis and the point relative to center.
     */
    fun calculateAngle(center: Point2D, pointer: Point2D): Double {
        val dx = pointer.x - center.x
        val dy = pointer.y - center.y
        val radians = atan2(dy, dx)
        return normalizeAngle(Math.toDegrees(radians))
    }
}
