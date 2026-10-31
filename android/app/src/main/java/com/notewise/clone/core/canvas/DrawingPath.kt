package com.notewise.clone.core.canvas

import android.graphics.Path
import android.graphics.RectF
import java.util.UUID
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class DrawingPath(
    val id: String = UUID.randomUUID().toString(),
    val points: MutableList<DrawingPoint> = mutableListOf(),
    var tool: DrawingTool,
    var color: Int,
    var strokeWidth: Float,
    val createdAt: Long = System.currentTimeMillis(),
    var audioTimestampMs: Long? = null,
    var isLocked: Boolean = false
) {
    val bounds: RectF = RectF()
    private var cachedPath: Path? = null

    fun getPath(): Path {
        if (cachedPath == null) {
            cachedPath = rebuildPath()
        }
        return cachedPath!!
    }

    fun invalidateCache() {
        cachedPath = null
    }

    fun translate(dx: Float, dy: Float) {
        if (isLocked) return
        for (i in 0 until points.size) {
            val p = points[i]
            points[i] = p.copy(x = p.x + dx, y = p.y + dy)
        }
        bounds.offset(dx, dy)
        cachedPath?.offset(dx, dy)
    }

    private fun rebuildPath(): Path {
        val path = Path()
        if (points.isEmpty()) return path

        when (val t = tool) {
            is DrawingTool.Shape -> {
                if (points.size < 2) return path
                val start = points.first()
                val end = points.last()
                val left = minOf(start.x, end.x)
                val top = minOf(start.y, end.y)
                val right = maxOf(start.x, end.x)
                val bottom = maxOf(start.y, end.y)

                when (t.shapeType) {
                    ShapeType.LINE -> {
                        path.moveTo(start.x, start.y)
                        path.lineTo(end.x, end.y)
                    }
                    ShapeType.ARROW -> {
                        path.moveTo(start.x, start.y)
                        path.lineTo(end.x, end.y)
                        // Arrowhead
                        val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
                        val arrowLen = (strokeWidth * 3.5f).coerceAtLeast(24f)
                        val angle1 = angle - PI / 6
                        val angle2 = angle + PI / 6
                        path.lineTo((end.x - arrowLen * cos(angle1)).toFloat(), (end.y - arrowLen * sin(angle1)).toFloat())
                        path.moveTo(end.x, end.y)
                        path.lineTo((end.x - arrowLen * cos(angle2)).toFloat(), (end.y - arrowLen * sin(angle2)).toFloat())
                    }
                    ShapeType.RECTANGLE -> {
                        path.addRect(left, top, right, bottom, Path.Direction.CW)
                    }
                    ShapeType.CIRCLE -> {
                        path.addOval(RectF(left, top, right, bottom), Path.Direction.CW)
                    }
                    ShapeType.TRIANGLE -> {
                        val midX = (left + right) / 2
                        path.moveTo(midX, top)
                        path.lineTo(right, bottom)
                        path.lineTo(left, bottom)
                        path.close()
                    }
                    ShapeType.STAR -> {
                        val cx = (left + right) / 2
                        val cy = (top + bottom) / 2
                        val rOuter = maxOf((right - left) / 2, (bottom - top) / 2)
                        val rInner = rOuter * 0.45f
                        val pointsCount = 5
                        for (i in 0 until pointsCount * 2) {
                            val r = if (i % 2 == 0) rOuter else rInner
                            val angle = i * PI / pointsCount - PI / 2
                            val x = (cx + r * cos(angle)).toFloat()
                            val y = (cy + r * sin(angle)).toFloat()
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                    }
                }
                val pad = strokeWidth / 2 + 10f
                bounds.set(left - pad, top - pad, right + pad, bottom + pad)
                return path
            }
            is DrawingTool.Text -> {
                val start = points.first()
                bounds.set(start.x, start.y - t.fontSize, start.x + t.text.length * t.fontSize * 0.6f, start.y + 10f)
                return path
            }
            else -> {
                // Freehand Bézier curve smoothing
                path.moveTo(points[0].x, points[0].y)
                var minX = points[0].x
                var minY = points[0].y
                var maxX = points[0].x
                var maxY = points[0].y

                if (points.size == 1) {
                    path.addCircle(points[0].x, points[0].y, strokeWidth / 4, Path.Direction.CW)
                } else {
                    for (i in 1 until points.size) {
                        val pPrev = points[i - 1]
                        val pCurr = points[i]
                        val midX = (pPrev.x + pCurr.x) / 2
                        val midY = (pPrev.y + pCurr.y) / 2
                        path.quadTo(pPrev.x, pPrev.y, midX, midY)

                        minX = minOf(minX, pCurr.x)
                        minY = minOf(minY, pCurr.y)
                        maxX = maxOf(maxX, pCurr.x)
                        maxY = maxOf(maxY, pCurr.y)
                    }
                }

                val padding = strokeWidth / 2 + 12f
                bounds.set(minX - padding, minY - padding, maxX + padding, maxY + padding)
                return path
            }
        }
    }

    /**
     * Checks if an eraser point touches this stroke
     */
    fun intersects(x: Float, y: Float, radius: Float): Boolean {
        if (isLocked) return false
        if (!bounds.intersects(x - radius, y - radius, x + radius, y + radius)) {
            return false
        }
        val rSq = radius * radius
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            if (distToSegmentSquared(x, y, p1.x, p1.y, p2.x, p2.y) <= rSq) {
                return true
            }
        }
        return false
    }

    /**
     * Checks if stroke center is inside a lasso polygon
     */
    fun isInsidePolygon(polygonPoints: List<DrawingPoint>): Boolean {
        if (isLocked || points.isEmpty()) return false
        val cx = bounds.centerX()
        val cy = bounds.centerY()

        var inside = false
        var j = polygonPoints.size - 1
        for (i in polygonPoints.indices) {
            val pi = polygonPoints[i]
            val pj = polygonPoints[j]
            if ((pi.y > cy) != (pj.y > cy) &&
                (cx < (pj.x - pi.x) * (cy - pi.y) / (pj.y - pi.y) + pi.x)
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    private fun distToSegmentSquared(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val l2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)
        if (l2 == 0f) return (px - x1) * (px - x1) + (py - y1) * (py - y1)
        var t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2
        t = maxOf(0f, minOf(1f, t))
        val projX = x1 + t * (x2 - x1)
        val projY = y1 + t * (y2 - y1)
        return (px - projX) * (px - projX) + (py - projY) * (py - projY)
    }
}
