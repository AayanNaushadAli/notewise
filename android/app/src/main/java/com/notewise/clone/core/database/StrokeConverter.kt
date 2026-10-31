package com.notewise.clone.core.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.notewise.clone.core.canvas.DrawingPath
import com.notewise.clone.core.canvas.DrawingPoint
import com.notewise.clone.core.canvas.DrawingTool
import com.notewise.clone.core.canvas.PenType
import com.notewise.clone.core.canvas.ShapeType

data class SerializedPath(
    val id: String,
    val points: List<DrawingPoint>,
    val toolType: String,
    val color: Int,
    val strokeWidth: Float,
    val penType: String? = null,
    val shapeType: String? = null,
    val textContent: String? = null,
    val fontSize: Float? = null,
    val createdAt: Long,
    val audioTimestampMs: Long? = null,
    val isLocked: Boolean = false
)

class StrokeConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromStrokesList(strokes: List<DrawingPath>?): String {
        if (strokes == null) return "[]"
        val serialized = strokes.map { s ->
            val toolTypeStr: String
            var penTypeStr: String? = null
            var shapeTypeStr: String? = null
            var textContent: String? = null
            var fontSize: Float? = null

            when (val t = s.tool) {
                is DrawingTool.Pen -> {
                    toolTypeStr = "pen"
                    penTypeStr = t.penType.name
                }
                is DrawingTool.Highlighter -> {
                    toolTypeStr = "highlighter"
                }
                is DrawingTool.Eraser -> {
                    toolTypeStr = "eraser"
                }
                is DrawingTool.Lasso -> {
                    toolTypeStr = "lasso"
                }
                is DrawingTool.Shape -> {
                    toolTypeStr = "shape"
                    shapeTypeStr = t.shapeType.name
                }
                is DrawingTool.Text -> {
                    toolTypeStr = "text"
                    textContent = t.text
                    fontSize = t.fontSize
                }
                else -> {
                    toolTypeStr = "pen"
                }
            }

            SerializedPath(
                id = s.id,
                points = s.points,
                toolType = toolTypeStr,
                color = s.color,
                strokeWidth = s.strokeWidth,
                penType = penTypeStr,
                shapeType = shapeTypeStr,
                textContent = textContent,
                fontSize = fontSize,
                createdAt = s.createdAt,
                audioTimestampMs = s.audioTimestampMs,
                isLocked = s.isLocked
            )
        }
        return gson.toJson(serialized)
    }

    @TypeConverter
    fun toStrokesList(json: String?): List<DrawingPath> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<SerializedPath>>() {}.type
        val rawList: List<SerializedPath> = gson.fromJson(json, type) ?: return emptyList()

        return rawList.map { raw ->
            val tool = when (raw.toolType) {
                "highlighter" -> DrawingTool.Highlighter(color = raw.color, width = raw.strokeWidth)
                "eraser" -> DrawingTool.Eraser()
                "lasso" -> DrawingTool.Lasso
                "shape" -> {
                    val st = try {
                        ShapeType.valueOf(raw.shapeType ?: "RECTANGLE")
                    } catch (e: Exception) {
                        ShapeType.RECTANGLE
                    }
                    DrawingTool.Shape(shapeType = st, color = raw.color, width = raw.strokeWidth)
                }
                "text" -> {
                    DrawingTool.Text(text = raw.textContent ?: "", fontSize = raw.fontSize ?: 20f, color = raw.color)
                }
                else -> {
                    val pt = try {
                        PenType.valueOf(raw.penType ?: "FOUNTAIN")
                    } catch (e: Exception) {
                        PenType.FOUNTAIN
                    }
                    DrawingTool.Pen(color = raw.color, width = raw.strokeWidth, penType = pt)
                }
            }

            DrawingPath(
                id = raw.id,
                points = raw.points.toMutableList(),
                tool = tool,
                color = raw.color,
                strokeWidth = raw.strokeWidth,
                createdAt = raw.createdAt,
                audioTimestampMs = raw.audioTimestampMs,
                isLocked = raw.isLocked
            )
        }
    }
}
