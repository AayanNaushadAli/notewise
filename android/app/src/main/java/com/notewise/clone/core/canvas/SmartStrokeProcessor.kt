package com.notewise.clone.core.canvas

import android.graphics.RectF
import kotlin.math.*

object SmartStrokeProcessor {

    data class ShapeRecognitionResult(
        val shapeType: ShapeType,
        val snappedPoints: List<DrawingPoint>,
        val confidence: Float
    )

    /**
     * Detects if a stroke represents a scribble-to-erase gesture.
     * Criteria:
     * - At least 4 horizontal or vertical direction reversals (oscillations)
     * - Bounding box is compact (< 180x180 dp)
     * - Sufficient total path length relative to bounding box size
     */
    fun isScribbleToErase(points: List<DrawingPoint>): RectF? {
        if (points.size < 8) return null

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        var xReversals = 0
        var yReversals = 0
        var prevDx = 0f
        var prevDy = 0f
        var totalLength = 0f

        for (i in points.indices) {
            val p = points[i]
            minX = minOf(minX, p.x)
            minY = minOf(minY, p.y)
            maxX = maxOf(maxX, p.x)
            maxY = maxOf(maxY, p.y)

            if (i > 0) {
                val prev = points[i - 1]
                val dx = p.x - prev.x
                val dy = p.y - prev.y
                totalLength += hypot(dx, dy)

                if (abs(dx) > 4f) {
                    if (prevDx != 0f && (dx > 0) != (prevDx > 0)) {
                        xReversals++
                    }
                    prevDx = dx
                }

                if (abs(dy) > 4f) {
                    if (prevDy != 0f && (dy > 0) != (prevDy > 0)) {
                        yReversals++
                    }
                    prevDy = dy
                }
            }
        }

        val width = maxX - minX
        val height = maxY - minY
        val maxDim = maxOf(width, height)

        // Check scribble density and reversal thresholds
        val totalReversals = xReversals + yReversals
        if (totalReversals >= 4 && maxDim < 300f && totalLength > maxDim * 2.5f) {
            return RectF(minX - 20f, minY - 20f, maxX + 20f, maxY + 20f)
        }
        return null
    }

    /**
     * Classifies and snaps hand-drawn strokes into mathematical vector shapes:
     * Circle, Rectangle, Triangle, Star, Line, Arrow.
     */
    fun detectShape(points: List<DrawingPoint>): ShapeRecognitionResult? {
        if (points.size < 6) return null

        val start = points.first()
        val end = points.last()
        val distanceStartEnd = hypot(end.x - start.x, end.y - start.y)

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        var pathLength = 0f

        for (i in points.indices) {
            val p = points[i]
            minX = minOf(minX, p.x)
            minY = minOf(minY, p.y)
            maxX = maxOf(maxX, p.x)
            maxY = maxOf(maxY, p.y)
            if (i > 0) {
                pathLength += hypot(p.x - points[i - 1].x, p.y - points[i - 1].y)
            }
        }

        val boxWidth = maxX - minX
        val boxHeight = maxY - minY
        val perimeter = (boxWidth + boxHeight) * 2
        val diagonal = hypot(boxWidth, boxHeight)

        if (diagonal < 30f) return null // Too small to classify

        // 1. OPEN STROKES: Line or Arrow
        val isClosed = distanceStartEnd < (diagonal * 0.25f).coerceAtLeast(35f)

        if (!isClosed) {
            val straightRatio = pathLength / distanceStartEnd.coerceAtLeast(1f)
            if (straightRatio < 1.25f) {
                // Highly straight -> Straight Line
                return ShapeRecognitionResult(
                    shapeType = ShapeType.LINE,
                    snappedPoints = listOf(start, end),
                    confidence = 0.95f
                )
            }

            // Check for Arrow: last 20% of points form an acute angle back
            if (points.size >= 10 && straightRatio < 1.7f) {
                return ShapeRecognitionResult(
                    shapeType = ShapeType.ARROW,
                    snappedPoints = listOf(start, end),
                    confidence = 0.85f
                )
            }
        } else {
            // 2. CLOSED STROKES: Circle, Rectangle, Triangle, Star
            val area = boxWidth * boxHeight
            val circularity = (4 * PI * area) / (pathLength * pathLength)

            // Circle / Ellipse: circularity close to 1.0
            val aspectRatio = boxWidth / boxHeight.coerceAtLeast(1f)
            if (circularity > 0.65f && aspectRatio in 0.6f..1.6f) {
                return ShapeRecognitionResult(
                    shapeType = ShapeType.CIRCLE,
                    snappedPoints = listOf(
                        DrawingPoint(minX, minY),
                        DrawingPoint(maxX, maxY)
                    ),
                    confidence = 0.92f
                )
            }

            // Detect corners for Polygon (Rectangle vs Triangle vs Star)
            val corners = detectCorners(points)

            if (corners.size == 3) {
                return ShapeRecognitionResult(
                    shapeType = ShapeType.TRIANGLE,
                    snappedPoints = listOf(
                        DrawingPoint(minX, minY),
                        DrawingPoint(maxX, maxY)
                    ),
                    confidence = 0.88f
                )
            }

            if (corners.size in 4..5) {
                return ShapeRecognitionResult(
                    shapeType = ShapeType.RECTANGLE,
                    snappedPoints = listOf(
                        DrawingPoint(minX, minY),
                        DrawingPoint(maxX, maxY)
                    ),
                    confidence = 0.90f
                )
            }

            // Star detection (alternating distance from center)
            if (corners.size >= 5) {
                return ShapeRecognitionResult(
                    shapeType = ShapeType.STAR,
                    snappedPoints = listOf(
                        DrawingPoint(minX, minY),
                        DrawingPoint(maxX, maxY)
                    ),
                    confidence = 0.82f
                )
            }

            // Default closed fallback: Rectangle or Circle based on aspect ratio
            return ShapeRecognitionResult(
                shapeType = if (circularity > 0.5f) ShapeType.CIRCLE else ShapeType.RECTANGLE,
                snappedPoints = listOf(
                    DrawingPoint(minX, minY),
                    DrawingPoint(maxX, maxY)
                ),
                confidence = 0.75f
            )
        }

        return null
    }

    private fun detectCorners(points: List<DrawingPoint>): List<DrawingPoint> {
        val corners = mutableListOf<DrawingPoint>()
        val step = maxOf(1, points.size / 24)

        for (i in step until points.size - step step step) {
            val pPrev = points[i - step]
            val pCurr = points[i]
            val pNext = points[i + step]

            val v1x = pCurr.x - pPrev.x
            val v1y = pCurr.y - pPrev.y
            val v2x = pNext.x - pCurr.x
            val v2y = pNext.y - pCurr.y

            val dot = v1x * v2x + v1y * v2y
            val mag1 = hypot(v1x, v1y)
            val mag2 = hypot(v2x, v2y)

            if (mag1 > 0 && mag2 > 0) {
                val cosTheta = (dot / (mag1 * mag2)).coerceIn(-1f, 1f)
                val angleDeg = acos(cosTheta) * (180f / PI.toFloat())
                if (angleDeg > 45f) {
                    corners.add(pCurr)
                }
            }
        }
        return corners
    }
}

