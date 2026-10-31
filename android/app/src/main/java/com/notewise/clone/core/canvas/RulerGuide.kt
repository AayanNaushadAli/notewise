package com.notewise.clone.core.canvas

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.*

class RulerGuide {
    var isEnabled: Boolean = false
    var rulerType: RulerType = RulerType.STRAIGHT_EDGE

    // Transform
    var centerX: Float = 400f
    var centerY: Float = 600f
    var angleDeg: Float = 0f
    var lengthDp: Float = 500f
    var widthDp: Float = 100f

    private val bodyPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.argb(45, 59, 130, 246) // Semi-transparent blue glass
    }

    private val borderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.argb(180, 37, 99, 235)
        strokeWidth = 3f
    }

    private val tickPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.argb(200, 30, 58, 138)
        strokeWidth = 2f
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.argb(220, 30, 58, 138)
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    fun render(canvas: Canvas) {
        if (!isEnabled) return

        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.rotate(angleDeg)

        when (rulerType) {
            RulerType.STRAIGHT_EDGE -> renderStraightEdge(canvas)
            RulerType.TRIANGLE -> renderTriangle(canvas)
            RulerType.PROTRACTOR -> renderProtractor(canvas)
        }

        canvas.restore()
    }

    private fun renderStraightEdge(canvas: Canvas) {
        val halfL = lengthDp / 2
        val halfW = widthDp / 2
        val rect = RectF(-halfL, -halfW, halfL, halfW)

        canvas.drawRoundRect(rect, 16f, 16f, bodyPaint)
        canvas.drawRoundRect(rect, 16f, 16f, borderPaint)

        // Draw ticks along top edge
        val tickSpacing = 20f
        var x = -halfL + 20f
        var cm = 0
        while (x <= halfL - 20f) {
            val isMajor = (cm % 5 == 0)
            val tickLen = if (isMajor) 30f else 15f
            canvas.drawLine(x, -halfW, x, -halfW + tickLen, tickPaint)

            if (isMajor && cm > 0) {
                canvas.drawText("${cm / 5}", x, -halfW + 55f, textPaint)
            }
            cm++
            x += tickSpacing
        }

        // Center degree badge
        val angleStr = "${angleDeg.toInt() % 360}°"
        canvas.drawCircle(0f, 0f, 28f, borderPaint)
        canvas.drawText(angleStr, 0f, 8f, textPaint)
    }

    private fun renderTriangle(canvas: Canvas) {
        val size = lengthDp * 0.7f
        val path = Path().apply {
            moveTo(-size / 2, size / 3)
            lineTo(size / 2, size / 3)
            lineTo(-size / 2, -size * 2 / 3)
            close()
        }
        canvas.drawPath(path, bodyPaint)
        canvas.drawPath(path, borderPaint)

        // 90 and 45 degree markings
        canvas.drawText("90°", -size / 2 + 50f, size / 3 - 30f, textPaint)
        canvas.drawText("45°", size / 2 - 60f, size / 3 - 30f, textPaint)
    }

    private fun renderProtractor(canvas: Canvas) {
        val radius = lengthDp * 0.4f
        val rect = RectF(-radius, -radius, radius, radius)

        // Half circle
        val path = Path().apply {
            arcTo(rect, 180f, 180f)
            close()
        }
        canvas.drawPath(path, bodyPaint)
        canvas.drawPath(path, borderPaint)

        // Degree ticks every 10 degrees
        for (deg in 0..180 step 10) {
            val rad = Math.toRadians(deg.toDouble())
            val x1 = (radius * cos(rad)).toFloat()
            val y1 = (-radius * sin(rad)).toFloat()
            val tickLen = if (deg % 30 == 0) 25f else 14f
            val x2 = ((radius - tickLen) * cos(rad)).toFloat()
            val y2 = (-(radius - tickLen) * sin(rad)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, tickPaint)

            if (deg % 30 == 0) {
                val tx = ((radius - 45f) * cos(rad)).toFloat()
                val ty = (-(radius - 45f) * sin(rad)).toFloat()
                canvas.drawText("$deg°", tx, ty + 6f, textPaint)
            }
        }
        canvas.drawCircle(0f, 0f, 12f, tickPaint)
    }

    /**
     * Snap point to nearest ruler edge if close (< 35px)
     */
    fun snapToEdge(x: Float, y: Float): Pair<Float, Float>? {
        if (!isEnabled) return null

        val rad = Math.toRadians(-angleDeg.toDouble())
        val dx = x - centerX
        val dy = y - centerY
        val localX = (dx * cos(rad) - dy * sin(rad)).toFloat()
        val localY = (dx * sin(rad) + dy * cos(rad)).toFloat()

        when (rulerType) {
            RulerType.STRAIGHT_EDGE -> {
                val halfL = lengthDp / 2
                val topEdgeY = -widthDp / 2
                if (localX in -halfL..halfL && abs(localY - topEdgeY) < 35f) {
                    // Snap to top edge
                    val snappedLocalX = localX
                    val snappedLocalY = topEdgeY
                    val invRad = Math.toRadians(angleDeg.toDouble())
                    val worldX = centerX + (snappedLocalX * cos(invRad) - snappedLocalY * sin(invRad)).toFloat()
                    val worldY = centerY + (snappedLocalX * sin(invRad) + snappedLocalY * cos(invRad)).toFloat()
                    return worldX to worldY
                }
            }
            else -> {}
        }
        return null
    }
}

