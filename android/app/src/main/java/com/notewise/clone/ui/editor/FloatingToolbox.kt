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
import com.notewise.clone.core.canvas.RulerType
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
    isEraserAutoDeselect: Boolean,
    isAutoShapeEnabled: Boolean,
    isScribbleToEraseEnabled: Boolean,
    selectedShapeType: ShapeType,
    isRulerEnabled: Boolean,
    selectedRulerType: RulerType,
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
    onToggleEraserAutoDeselect: () -> Unit,
    onToggleAutoShape: () -> Unit,
    onToggleScribbleToErase: () -> Unit,
    onShapeTypeChange: (ShapeType) -> Unit,
    onToggleRuler: () -> Unit,
    onRulerTypeChange: (RulerType) -> Unit,
    onLassoDelete: () -> Unit,
    onLassoDuplicate: () -> Unit,
    onLassoRecolor: (Int) -> Unit,
    onLassoLockToggle: () -> Unit,
    onOpenStickerLibrary: () -> Unit,
    onClearCanvas: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onResetZoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedPanel by remember { mutableStateOf<String?>(null) }

    val presetColors = listOf(
        AndroidColor.BLACK,
        AndroidColor.parseColor("#2563EB"),
        AndroidColor.parseColor("#DC2626"),
        AndroidColor.parseColor("#16A34A"),
        AndroidColor.parseColor("#D97706"),
        AndroidColor.parseColor("#7C3AED")
    )

    val highlighterColors = listOf(
        AndroidColor.argb(100, 255, 235, 59),
        AndroidColor.argb(100, 165, 214, 167),
        AndroidColor.argb(100, 144, 202, 249),
        AndroidColor.argb(100, 244, 143, 177),
        AndroidColor.argb(100, 255, 204, 128),
        AndroidColor.argb(100, 206, 147, 216)
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
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onLassoLockToggle, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onOpenStickerLibrary, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = "Add to Library", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onLassoDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .clickable { onLassoRecolor(AndroidColor.BLACK) }
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB))
                            .clickable { onLassoRecolor(AndroidColor.parseColor("#2563EB")) }
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626))
                            .clickable { onLassoRecolor(AndroidColor.parseColor("#DC2626")) }
                    )
                }
            }
        }

        // Expanded Flyout Settings Panel
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
                            // Pen Types & Smart Features
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FilterChip(
                                        selected = isAutoShapeEnabled,
                                        onClick = onToggleAutoShape,
                                        label = { Text("Auto-Shape", fontSize = 11.sp) },
                                        leadingIcon = {
                                            if (isAutoShapeEnabled) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Width slider
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

                            Spacer(modifier = Modifier.height(8.dp))

                            // Colors & Scribble-erase
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(presetColors) { col ->
                                        val isSelected = selectedColor == col
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
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

                                FilterChip(
                                    selected = isScribbleToEraseEnabled,
                                    onClick = onToggleScribbleToErase,
                                    label = { Text("Scribble Erase", fontSize = 10.sp) }
                                )
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

                                FilterChip(
                                    selected = isEraserAutoDeselect,
                                    onClick = onToggleEraserAutoDeselect,
                                    label = { Text("Auto-Deselect", fontSize = 11.sp) }
                                )

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
                                ShapeIconButton(Icons.Default.Star, selectedShapeType == ShapeType.STAR) {
                                    onShapeTypeChange(ShapeType.STAR)
                                }
                            }
                        }

                        "ruler" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = selectedRulerType == RulerType.STRAIGHT_EDGE,
                                    onClick = { onRulerTypeChange(RulerType.STRAIGHT_EDGE) },
                                    label = { Text("Straight Edge", fontSize = 12.sp) }
                                )
                                FilterChip(
                                    selected = selectedRulerType == RulerType.TRIANGLE,
                                    onClick = { onRulerTypeChange(RulerType.TRIANGLE) },
                                    label = { Text("Set Square", fontSize = 12.sp) }
                                )
                                FilterChip(
                                    selected = selectedRulerType == RulerType.PROTRACTOR,
                                    onClick = { onRulerTypeChange(RulerType.PROTRACTOR) },
                                    label = { Text("Protractor", fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Floating Bar
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            shadowElevation = 8.dp,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", modifier = Modifier.size(18.dp))
                }

                VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 2.dp))

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

                ToolIconButton(
                    icon = Icons.Default.Straighten,
                    label = "Ruler",
                    isSelected = isRulerEnabled,
                    activeColor = Color(0xFF3B82F6),
                    onClick = {
                        if (!isRulerEnabled) {
                            onToggleRuler()
                            expandedPanel = "ruler"
                        } else {
                            expandedPanel = if (expandedPanel == "ruler") null else "ruler"
                        }
                    }
                )

                ToolIconButton(
                    icon = Icons.Default.Colorize,
                    label = "Eyedropper",
                    isSelected = selectedTool == "eyedropper",
                    activeColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onSelectTool("eyedropper")
                        expandedPanel = null
                    }
                )

                ToolIconButton(
                    icon = Icons.Default.CollectionsBookmark,
                    label = "Library",
                    isSelected = false,
                    activeColor = MaterialTheme.colorScheme.primary,
                    onClick = onOpenStickerLibrary
                )

                VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 2.dp))

                IconButton(onClick = onResetZoom, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.FitScreen, contentDescription = "Fit Screen", modifier = Modifier.size(18.dp))
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
            .size(38.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
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
        label = { Text(label, fontSize = 11.sp) }
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
            modifier = Modifier.size(18.dp)
        )
    }
}
