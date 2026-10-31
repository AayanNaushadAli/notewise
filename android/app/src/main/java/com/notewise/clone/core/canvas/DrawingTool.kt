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
    TRIANGLE
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
        val color: Int = Color.argb(90, 255, 235, 59), // Translucent yellow
        val width: Float = 24.0f
    ) : DrawingTool()

    data class Eraser(
        val radius: Float = 24.0f,
        val isStrokeEraser: Boolean = true // True deletes entire stroke, False deletes segments
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
}
