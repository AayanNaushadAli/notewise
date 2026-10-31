package com.notewise.clone.ui.editor

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notewise.clone.core.template.PageTemplate

@Composable
fun TemplateSelectionDialog(
    current: PageTemplate,
    onSelect: (PageTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTemplateName by remember { mutableStateOf(current.name) }
    var selectedBgHex by remember {
        mutableStateOf(
            PageTemplate.PAPER_COLORS.find {
                try {
                    AndroidColor.parseColor(it.first) == current.backgroundColor
                } catch (e: Exception) {
                    false
                }
            }?.first ?: "#FFFFFF"
        )
    }

    val templates = listOf(
        "Ruled" to "Lined / Ruled paper",
        "Grid" to "5mm Squared Grid",
        "Dot Grid" to "Dot Matrix Grid",
        "Cornell Notes" to "Cornell study layout",
        "Music Staff" to "5-line Music Staves",
        "Weekly Planner" to "Planner & Todo Matrix",
        "Blank" to "Clean Blank Canvas"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Page Template & Paper", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Paper Color", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                // Paper Color Palette
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    items(PageTemplate.PAPER_COLORS) { (hex, name) ->
                        val isSelected = selectedBgHex.equals(hex, ignoreCase = true)
                        val color = Color(AndroidColor.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { selectedBgHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                val checkTint = if (hex == "#1E293B" || hex == "#0F172A" || hex == "#064E3B") Color.White else Color.Black
                                Icon(Icons.Default.Check, contentDescription = null, tint = checkTint, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text("Template Style", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(240.dp)
                ) {
                    items(templates) { (name, desc) ->
                        val isSelected = selectedTemplateName == name
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) borderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTemplateName = name }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    desc,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val template = PageTemplate.fromString(selectedTemplateName, selectedBgHex)
                    onSelect(template)
                }
            ) {
                Text("Apply Template")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)
