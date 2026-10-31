package com.notewise.clone.ui.editor

import android.graphics.Color as AndroidColor
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notewise.clone.core.canvas.PenType
import com.notewise.clone.core.canvas.ShapeType

@Composable
fun FloatingToolbox(
    selectedTool: String,
    selectedPenType: PenType,
    selectedColor: Int,
    selectedPenWidth: Float,
    selectedPenOpacity: Float,
    selectedHighlighterWidth: Float,
    selectedHighlighterColor: Int,
    selectedEraserRadius: Float,
    isStrokeEraser: Boolean,
    selectedShapeType: ShapeType,
    isRulerEnabled: Boolean,
    hasLassoSelection: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onSelectTool: (String) -> Unit,
    onPenTypeChange: (PenType) -> Unit,
    onColorChange: (Int) -> Unit,
    onPenWidthChange: (Float) -> Unit,
    onPenOpacityChange: (Float) -> Unit,
    onHighlighterWidthChange: (Float) -> Unit,
    onHighlighterColorChange: (Int) -> Unit,
    onEraserRadiusChange: (Float) -> Unit,
    onToggleStrokeEraser: (Boolean) -> Unit,
    onShapeTypeChange: (ShapeType) -> Unit,
    onToggleRuler: () -> Unit,
    onLassoDelete: () -> Unit,
    onLassoDuplicate: () -> Unit,
    onLassoRecolor: (Int) -> Unit,
    onClearCanvas: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onResetZoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedPanel by remember { mutableStateOf<String?>(null) }

    val presetColors = listOf(
        AndroidColor.BLACK,
        AndroidColor.parseColor("#2563EB"), // Blue
        AndroidColor.parseColor("#DC2626"), // Red
        AndroidColor.parseColor("#16A34A"), // Green
        AndroidColor.parseColor("#D97706"), // Amber/Orange
        AndroidColor.parseColor("#7C3AED")  // Purple
    )

    val highlighterColors = listOf(
        AndroidColor.argb(100, 255, 235, 59),  // Pastel Yellow
        AndroidColor.argb(100, 165, 214, 167), // Pastel Green
        AndroidColor.argb(100, 144, 202, 249), // Pastel Blue
        AndroidColor.argb(100, 244, 143, 177), // Pastel Pink
        AndroidColor.argb(100, 255, 204, 128), // Pastel Orange
        AndroidColor.argb(100, 206, 147, 216)  // Pastel Purple
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Lasso Selection Action Bar
        if (hasLassoSelection) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                shadowElevation = 6.dp,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Selected", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = onLassoDuplicate, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onLassoDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                    }
                    // Quick recolor to blue / red / black
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .clickable { onLassoRecolor(AndroidColor.BLACK) }
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB))
                            .clickable { onLassoRecolor(AndroidColor.parseColor("#2563EB")) }
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626))
                            .clickable { onLassoRecolor(AndroidColor.parseColor("#DC2626")) }
                    )
                }
            }
        }

        // Sub-panels (Flyout popups for Pen, Highlighter, Eraser, Shapes)
        AnimatedVisibility(
            visible = expandedPanel != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(0.92f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (expandedPanel) {
                        "pen" -> {
                            // Pen Type selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                PenTypeChip("Fountain", selectedPenType == PenType.FOUNTAIN) {
                                    onPenTypeChange(PenType.FOUNTAIN)
                                }
                                PenTypeChip("Ballpoint", selectedPenType == PenType.BALLPOINT) {
                                    onPenTypeChange(PenType.BALLPOINT)
                                }
                                PenTypeChip("Calligraphy", selectedPenType == PenType.CALLIGRAPHY) {
                                    onPenTypeChange(PenType.CALLIGRAPHY)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Stroke width slider with preview
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Width: ${"%.1f".format(selectedPenWidth)}", fontSize = 12.sp, modifier = Modifier.width(70.dp))
                                Slider(
                                    value = selectedPenWidth,
                                    onValueChange = onPenWidthChange,
                                    valueRange = 1f..15f,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(selectedPenWidth.dp.coerceIn(4.dp, 24.dp))
                                        .clip(CircleShape)
                                        .background(Color(selectedColor))
                                )
                            }

                            // Color Palette
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(presetColors) { col ->
                                    val isSelected = selectedColor == col
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(col))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                                shape = CircleShape
                                            )
                                            .clickable { onColorChange(col) }
                                    )
                                }
                            }
                        }

                        "highlighter" -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Width: ${"%.0f".format(selectedHighlighterWidth)}", fontSize = 12.sp, modifier = Modifier.width(70.dp))
                                Slider(
                                    value = selectedHighlighterWidth,
                                    onValueChange = onHighlighterWidthChange,
                                    valueRange = 12f..50f,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(highlighterColors) { col ->
                                    val isSelected = selectedHighlighterColor == col
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(col))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                                shape = CircleShape
                                            )
                                            .clickable { onHighlighterColorChange(col) }
                                    )
                                }
                            }
                        }

                        "eraser" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isStrokeEraser,
                                        onClick = { onToggleStrokeEraser(true) }
                                    )
                                    Text("Stroke", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    RadioButton(
                                        selected = !isStrokeEraser,
                                        onClick = { onToggleStrokeEraser(false) }
                                    )
                                    Text("Pixel", fontSize = 13.sp)
                                }

                                OutlinedButton(
                                    onClick = onClearCanvas,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Clear Page", fontSize = 12.sp)
                                }
                            }
                        }

                        "shape" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ShapeIconButton(Icons.Default.Square, selectedShapeType == ShapeType.RECTANGLE) {
                                    onShapeTypeChange(ShapeType.RECTANGLE)
                                }
                                ShapeIconButton(Icons.Default.Circle, selectedShapeType == ShapeType.CIRCLE) {
                                    onShapeTypeChange(ShapeType.CIRCLE)
                                }
                                ShapeIconButton(Icons.Default.HorizontalRule, selectedShapeType == ShapeType.LINE) {
                                    onShapeTypeChange(ShapeType.LINE)
                                }
                                ShapeIconButton(Icons.AutoMirrored.Filled.ArrowForward, selectedShapeType == ShapeType.ARROW) {
                                    onShapeTypeChange(ShapeType.ARROW)
                                }
                                ShapeIconButton(Icons.Default.ChangeHistory, selectedShapeType == ShapeType.TRIANGLE) {
                                    onShapeTypeChange(ShapeType.TRIANGLE)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Main Bottom Floating Bar (Notewise Pill Design)
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            shadowElevation = 8.dp,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Undo & Redo
                IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", modifier = Modifier.size(20.dp))
                }

                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 2.dp))

                // Pen Tool
                ToolIconButton(
                    icon = Icons.Default.Edit,
                    label = "Pen",
                    isSelected = selectedTool == "pen",
                    activeColor = Color(selectedColor),
                    onClick = {
                        if (selectedTool == "pen") {
                            expandedPanel = if (expandedPanel == "pen") null else "pen"
                        } else {
                            onSelectTool("pen")
                            expandedPanel = "pen"
                        }
                    }
                )

                // Highlighter Tool
                ToolIconButton(
                    icon = Icons.Default.Brush,
                    label = "Highlighter",
                    isSelected = selectedTool == "highlighter",
                    activeColor = Color(0xFFEAB308),
                    onClick = {
                        if (selectedTool == "highlighter") {
                            expandedPanel = if (expandedPanel == "highlighter") null else "highlighter"
                        } else {
                            onSelectTool("highlighter")
                            expandedPanel = "highlighter"
                        }
                    }
                )

                // Eraser Tool
                ToolIconButton(
                    icon = Icons.Default.AutoFixHigh,
                    label = "Eraser",
                    isSelected = selectedTool == "eraser",
                    activeColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        if (selectedTool == "eraser") {
                            expandedPanel = if (expandedPanel == "eraser") null else "eraser"
                        } else {
                            onSelectTool("eraser")
                            expandedPanel = "eraser"
                        }
                    }
                )

                // Lasso Selection Tool
                ToolIconButton(
                    icon = Icons.Default.Gesture,
                    label = "Lasso",
                    isSelected = selectedTool == "lasso",
                    activeColor = Color(0xFF2563EB),
                    onClick = {
                        onSelectTool("lasso")
                        expandedPanel = null
                    }
                )

                // Shape Tool
                ToolIconButton(
                    icon = Icons.Default.Category,
                    label = "Shapes",
                    isSelected = selectedTool == "shape",
                    activeColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        if (selectedTool == "shape") {
                            expandedPanel = if (expandedPanel == "shape") null else "shape"
                        } else {
                            onSelectTool("shape")
                            expandedPanel = "shape"
                        }
                    }
                )

                // Text Tool
                ToolIconButton(
                    icon = Icons.Default.TextFields,
                    label = "Text",
                    isSelected = selectedTool == "text",
                    activeColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onSelectTool("text")
                        expandedPanel = null
                    }
                )

                // Ruler Tool
                ToolIconButton(
                    icon = Icons.Default.Straighten,
                    label = "Ruler",
                    isSelected = isRulerEnabled,
                    activeColor = Color(0xFF3B82F6),
                    onClick = onToggleRuler
                )

                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 2.dp))

                // Reset Zoom
                IconButton(onClick = onResetZoom, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.FitScreen, contentDescription = "Fit to Screen", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ToolIconButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun PenTypeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) }
    )
}

@Composable
private fun ShapeIconButton(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}
