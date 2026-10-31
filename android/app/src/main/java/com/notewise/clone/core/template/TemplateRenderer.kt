package com.notewise.clone.core.template

import android.graphics.Canvas
import android.graphics.Paint

class TemplateRenderer {
    private val linePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val marginPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val dotPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    fun render(canvas: Canvas, width: Float, height: Float, template: PageTemplate) {
        when (template) {
            is PageTemplate.Blank -> {
                // Pure blank canvas
            }
            is PageTemplate.Ruled -> {
                linePaint.color = template.lineColor
                linePaint.strokeWidth = 1.5f
                val spacing = template.lineSpacingDp * 2.5f

                var y = spacing * 1.5f
                while (y < height) {
                    canvas.drawLine(0f, y, width, y, linePaint)
                    y += spacing
                }

                // Vertical margin line
                marginPaint.color = template.marginColor
                val marginX = template.marginStartDp * 2.5f
                canvas.drawLine(marginX, 0f, marginX, height, marginPaint)
            }
            is PageTemplate.Grid -> {
                linePaint.color = template.gridColor
                linePaint.strokeWidth = 1.0f
                val size = template.gridSizeDp * 2.5f

                var x = size
                while (x < width) {
                    canvas.drawLine(x, 0f, x, height, linePaint)
                    x += size
                }
                var y = size
                while (y < height) {
                    canvas.drawLine(0f, y, width, y, linePaint)
                    y += size
                }
            }
            is PageTemplate.DotGrid -> {
                dotPaint.color = template.dotColor
                val spacing = template.dotSpacingDp * 2.5f
                val radius = template.dotRadiusDp * 1.5f

                var x = spacing
                while (x < width) {
                    var y = spacing
                    while (y < height) {
                        canvas.drawCircle(x, y, radius, dotPaint)
                        y += spacing
                    }
                    x += spacing
                }
            }
            is PageTemplate.Cornell -> {
                linePaint.color = template.lineColor
                linePaint.strokeWidth = 1.5f
                val lineSpacing = template.lineSpacingDp * 2.5f
                val headerY = template.headerHeightDp * 2.5f
                val cueX = template.cueColumnWidthDp * 2.5f
                val summaryY = height - (template.summaryHeightDp * 2.5f)

                // Header line
                canvas.drawLine(0f, headerY, width, headerY, linePaint)
                // Summary bottom divider
                canvas.drawLine(0f, summaryY, width, summaryY, linePaint)
                // Cue column vertical divider
                canvas.drawLine(cueX, headerY, cueX, summaryY, linePaint)

                // Notes lines inside main body
                var y = headerY + lineSpacing
                while (y < summaryY) {
                    canvas.drawLine(cueX, y, width, y, linePaint)
                    y += lineSpacing
                }
            }
            is PageTemplate.MusicStaff -> {
                linePaint.color = template.lineColor
                linePaint.strokeWidth = 1.2f
                val lineSpacing = template.lineSpacingDp * 2.2f
                val staffSpacing = template.staffSpacingDp * 2.5f

                var curY = 80f
                while (curY + (lineSpacing * 4) < height - 60f) {
                    // Draw 5 staff lines
                    for (i in 0 until 5) {
                        val y = curY + (i * lineSpacing)
                        canvas.drawLine(40f, y, width - 40f, y, linePaint)
                    }
                    // Staff bar ends
                    canvas.drawLine(40f, curY, 40f, curY + (4 * lineSpacing), linePaint)
                    canvas.drawLine(width - 40f, curY, width - 40f, curY + (4 * lineSpacing), linePaint)

                    curY += (lineSpacing * 4) + staffSpacing
                }
            }
            is PageTemplate.WeeklyPlanner -> {
                linePaint.color = template.lineColor
                linePaint.strokeWidth = 1.5f
                val headerY = template.headerHeightDp * 2.5f

                // Top header line
                canvas.drawLine(0f, headerY, width, headerY, linePaint)

                // 7 columns for Mon-Sun
                val colWidth = width / template.columnCount
                for (i in 1 until template.columnCount) {
                    val x = colWidth * i
                    canvas.drawLine(x, 0f, x, height, linePaint)
                }

                // Horizontal subdivisions
                val rowHeight = (height - headerY) / 6f
                for (r in 1..5) {
                    val y = headerY + (r * rowHeight)
                    canvas.drawLine(0f, y, width, y, linePaint)
                }
            }
        }
    }
}
