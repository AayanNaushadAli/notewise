package com.notewise.clone.core.template

import android.graphics.Color

sealed class PageTemplate(
    val name: String,
    open val backgroundColor: Int = Color.WHITE
) {
    data class Blank(
        override val backgroundColor: Int = Color.WHITE
    ) : PageTemplate("Blank", backgroundColor)

    data class Ruled(
        val lineSpacingDp: Float = 32f,
        val lineColor: Int = Color.argb(40, 0, 0, 0),
        val marginColor: Int = Color.argb(60, 239, 68, 68), // Red margin line
        val marginStartDp: Float = 56f,
        override val backgroundColor: Int = Color.WHITE
    ) : PageTemplate("Ruled", backgroundColor)

    data class Grid(
        val gridSizeDp: Float = 24f,
        val gridColor: Int = Color.argb(35, 0, 0, 0),
        override val backgroundColor: Int = Color.WHITE
    ) : PageTemplate("Grid", backgroundColor)

    data class DotGrid(
        val dotSpacingDp: Float = 24f,
        val dotRadiusDp: Float = 1.5f,
        val dotColor: Int = Color.argb(50, 0, 0, 0),
        override val backgroundColor: Int = Color.WHITE
    ) : PageTemplate("Dot Grid", backgroundColor)

    data class Cornell(
        val lineSpacingDp: Float = 32f,
        val headerHeightDp: Float = 80f,
        val cueColumnWidthDp: Float = 140f,
        val summaryHeightDp: Float = 120f,
        val lineColor: Int = Color.argb(40, 0, 0, 0),
        override val backgroundColor: Int = Color.WHITE
    ) : PageTemplate("Cornell Notes", backgroundColor)

    data class MusicStaff(
        val lineSpacingDp: Float = 10f,
        val staffSpacingDp: Float = 50f,
        val lineColor: Int = Color.argb(60, 0, 0, 0),
        override val backgroundColor: Int = Color.WHITE
    ) : PageTemplate("Music Staff", backgroundColor)

    data class WeeklyPlanner(
        val headerHeightDp: Float = 50f,
        val columnCount: Int = 7,
        val lineColor: Int = Color.argb(45, 0, 0, 0),
        override val backgroundColor: Int = Color.WHITE
    ) : PageTemplate("Weekly Planner", backgroundColor)

    companion object {
        val PAPER_COLORS = listOf(
            "#FFFFFF" to "Clean White",
            "#FBF7EE" to "Ivory Cream",
            "#FFFBEB" to "Yellow Pad",
            "#1E293B" to "Slate Gray",
            "#0F172A" to "Pitch Black",
            "#064E3B" to "Greenboard"
        )

        fun fromString(templateName: String, bgColorHex: String = "#FFFFFF"): PageTemplate {
            val bg = try {
                Color.parseColor(bgColorHex)
            } catch (e: Exception) {
                Color.WHITE
            }
            val isDark = isDarkColor(bg)
            val lineCol = if (isDark) Color.argb(60, 255, 255, 255) else Color.argb(40, 0, 0, 0)
            val dotCol = if (isDark) Color.argb(70, 255, 255, 255) else Color.argb(50, 0, 0, 0)

            return when (templateName) {
                "Grid" -> Grid(gridColor = lineCol, backgroundColor = bg)
                "Dot Grid", "DotGrid" -> DotGrid(dotColor = dotCol, backgroundColor = bg)
                "Cornell Notes", "Cornell" -> Cornell(lineColor = lineCol, backgroundColor = bg)
                "Music Staff", "Music" -> MusicStaff(lineColor = lineCol, backgroundColor = bg)
                "Weekly Planner", "Planner" -> WeeklyPlanner(lineColor = lineCol, backgroundColor = bg)
                "Blank" -> Blank(backgroundColor = bg)
                else -> Ruled(lineColor = lineCol, backgroundColor = bg)
            }
        }

        private fun isDarkColor(color: Int): Boolean {
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
            return luminance < 128
        }
    }
}
