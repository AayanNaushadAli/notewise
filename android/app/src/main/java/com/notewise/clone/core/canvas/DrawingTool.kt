package com.notewise.clone.core.canvas

import android.graphics.Color

enum class PenType {
    FOUNTAIN,
    BALLPOINT,
    CALLIGRAPHY
}

enum class ShapeType {
    LINE,
    ARROW,
    RECTANGLE,
    CIRCLE,
    TRIANGLE,
    STAR
}

enum class RulerType {
    STRAIGHT_EDGE,
    TRIANGLE,
    PROTRACTOR
}

sealed class DrawingTool {
    data class Pen(
        val color: Int = Color.BLACK,
        val width: Float = 3.5f,
        val opacity: Float = 1.0f,
        val penType: PenType = PenType.FOUNTAIN,
        val pressureSensitive: Boolean = true
    ) : DrawingTool()

    data class Highlighter(
        val color: Int = Color.argb(90, 255, 235, 59),
        val width: Float = 24.0f
    ) : DrawingTool()

    data class Eraser(
        val radius: Float = 24.0f,
        val isStrokeEraser: Boolean = true,
        val autoDeselect: Boolean = false
    ) : DrawingTool()

    object Lasso : DrawingTool()

    data class Shape(
        val shapeType: ShapeType = ShapeType.RECTANGLE,
        val color: Int = Color.BLACK,
        val width: Float = 3.5f,
        val isFilled: Boolean = false
    ) : DrawingTool()

    data class Text(
        val text: String = "",
        val fontSize: Float = 20f,
        val color: Int = Color.BLACK
    ) : DrawingTool()

    object EyeDropper : DrawingTool()
}
