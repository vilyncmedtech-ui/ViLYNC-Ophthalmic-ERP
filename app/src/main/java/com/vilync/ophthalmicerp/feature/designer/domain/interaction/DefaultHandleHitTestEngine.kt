package com.vilync.ophthalmicerp.feature.designer.domain.interaction

import com.vilync.ophthalmicerp.core.document.domain.geometry.Point2D
import com.vilync.ophthalmicerp.core.document.domain.geometry.MeasurementUnit
import com.vilync.ophthalmicerp.core.document.domain.geometry.Dimensions
import com.vilync.ophthalmicerp.core.document.engine.RenderLayout
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.CoordinateMapper
import com.vilync.ophthalmicerp.feature.designer.domain.geometry.GeometryTransform
import com.vilync.ophthalmicerp.feature.designer.domain.manager.DocumentManager

/**
 * Production implementation that ensures constant handle hit area regardless of zoom.
 */
class DefaultHandleHitTestEngine(
    private val documentManager: DocumentManager,
    private val handleRadiusPx: Double = 12.0,
    private val rotationOffsetPx: Double = 40.0
) : HandleHitTestEngine {

    override fun test(
        pointerWorkspacePos: Point2D,
        selectedIds: Set<String>,
        layout: RenderLayout,
        mapper: CoordinateMapper
    ): ResizeHandleResult {
        val primaryId = selectedIds.firstOrNull() ?: return ResizeHandleResult(HitType.NONE)
        
        for (pageIndex in layout.pages.indices) {
            val page = layout.pages[pageIndex]
            val obj = page.objects.find { it.id == primaryId } ?: continue
            
            val objTopLeftMm = getObjectTopLeftMm(pageIndex, obj) ?: continue
            val handles = calculateResizeHandlePositions(objTopLeftMm, obj.dimensions, obj.rotation)
            
            val pointerViewport = mapper.workspaceToViewport(pointerWorkspacePos)
            
            handles.forEach { (type, handleMm) ->
                val handleViewport = mapper.workspaceToViewport(handleMm)
                val distPx = Math.sqrt(
                    Math.pow(pointerViewport.x - handleViewport.x, 2.0) + 
                    Math.pow(pointerViewport.y - handleViewport.y, 2.0)
                )
                
                if (distPx <= handleRadiusPx) {
                    return ResizeHandleResult(
                        hitType = HitType.HANDLE,
                        handleType = type,
                        objectId = primaryId
                    )
                }
            }
        }
        
        return ResizeHandleResult(HitType.NONE)
    }

    override fun testRotation(
        pointerWorkspacePos: Point2D,
        selectedIds: Set<String>,
        layout: RenderLayout,
        mapper: CoordinateMapper
    ): RotationHandleResult {
        val primaryId = selectedIds.firstOrNull() ?: return RotationHandleResult(HitType.NONE)

        for (pageIndex in layout.pages.indices) {
            val page = layout.pages[pageIndex]
            val obj = page.objects.find { it.id == primaryId } ?: continue
            
            val objTopLeftMm = getObjectTopLeftMm(pageIndex, obj) ?: continue
            val center = GeometryTransform.calculateCenter(objTopLeftMm, obj.dimensions)
            val offsetMm = mapper.pixelsToMm(rotationOffsetPx.toFloat())
            
            val topCenter = GeometryTransform.getBoxPoint(objTopLeftMm, obj.dimensions, GeometryTransform.BoxPointType.TOP_CENTER)
            val handleRaw = Point2D(topCenter.x, topCenter.y - offsetMm, topCenter.unit)
            val rotationHandleMm = GeometryTransform.rotatePoint(handleRaw, center, obj.rotation.toDouble())
            
            val pointerViewport = mapper.workspaceToViewport(pointerWorkspacePos)
            val handleViewport = mapper.workspaceToViewport(rotationHandleMm)
            
            val distPx = Math.sqrt(
                Math.pow(pointerViewport.x - handleViewport.x, 2.0) + 
                Math.pow(pointerViewport.y - handleViewport.y, 2.0)
            )
            
            if (distPx <= handleRadiusPx) {
                return RotationHandleResult(
                    hitType = HitType.HANDLE,
                    objectId = primaryId
                )
            }
        }

        return RotationHandleResult(HitType.NONE)
    }

    private fun getObjectTopLeftMm(pageIndex: Int, obj: com.vilync.ophthalmicerp.core.document.engine.RenderObject): Point2D? {
        val pageOffset = documentManager.getPageLocation(pageIndex) ?: return null
        val layoutPage = documentManager.getLayoutPage(pageIndex) ?: return null
        
        val pageTopLeft = Point2D(
            x = pageOffset.x - layoutPage.width / 2.0,
            y = pageOffset.y - layoutPage.height / 2.0,
            unit = MeasurementUnit.MILLIMETER
        )
        
        return Point2D(
            x = pageTopLeft.x + obj.position.x,
            y = pageTopLeft.y + obj.position.y,
            unit = pageTopLeft.unit
        )
    }

    private fun calculateResizeHandlePositions(
        topLeft: Point2D, 
        dim: Dimensions, 
        rotation: Float
    ): Map<ResizeHandleType, Point2D> {
        val center = GeometryTransform.calculateCenter(topLeft, dim)
        val rot = rotation.toDouble()

        fun handle(type: GeometryTransform.BoxPointType) = 
            GeometryTransform.rotatePoint(GeometryTransform.getBoxPoint(topLeft, dim, type), center, rot)

        return mapOf(
            ResizeHandleType.TL to handle(GeometryTransform.BoxPointType.TOP_LEFT),
            ResizeHandleType.T  to handle(GeometryTransform.BoxPointType.TOP_CENTER),
            ResizeHandleType.TR to handle(GeometryTransform.BoxPointType.TOP_RIGHT),
            ResizeHandleType.R  to handle(GeometryTransform.BoxPointType.RIGHT_CENTER),
            ResizeHandleType.BR to handle(GeometryTransform.BoxPointType.BOTTOM_RIGHT),
            ResizeHandleType.B  to handle(GeometryTransform.BoxPointType.BOTTOM_CENTER),
            ResizeHandleType.BL to handle(GeometryTransform.BoxPointType.BOTTOM_LEFT),
            ResizeHandleType.L  to handle(GeometryTransform.BoxPointType.LEFT_CENTER)
        )
    }
}
