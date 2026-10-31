package com.notewise.clone.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notewise.clone.core.database.StickerEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerLibrarySheet(
    stickers: List<StickerEntity>,
    hasLassoSelection: Boolean,
    onSaveCurrentSelection: (name: String, tag: String) -> Unit,
    onSelectSticker: (StickerEntity) -> Unit,
    onDeleteSticker: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTag by remember { mutableStateOf("All") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val tags = listOf("All", "Math", "Shapes", "Chemistry", "Notes", "General")

    val filteredStickers = remember(stickers, selectedTag) {
        if (selectedTag == "All") stickers
        else stickers.filter { it.tag.equals(selectedTag, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sticker & Asset Library",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                if (hasLassoSelection) {
                    Button(
                        onClick = { showCreateDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Selection")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tags row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tags) { tag ->
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = { selectedTag = tag },
                        label = { Text(tag, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredStickers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (hasLassoSelection) "Tap 'Add Selection' to save your drawing to the library!"
                        else "No stickers in this category. Lasso-select any drawing and save it here!",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    items(filteredStickers, key = { it.id }) { sticker ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelectSticker(sticker)
                                    onDismiss()
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        sticker.name.take(2).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    sticker.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Text(
                                    sticker.tag,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var stickerName by remember { mutableStateOf("") }
        var chosenTag by remember { mutableStateOf("Math") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Save to Library") },
            text = {
                Column {
                    OutlinedTextField(
                        value = stickerName,
                        onValueChange = { stickerName = it },
                        label = { Text("Sticker Name") },
                        placeholder = { Text("e.g. Integral Formula, Benzene Ring") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Tag", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Math", "Shapes", "Chemistry", "Notes", "General")) { t ->
                            FilterChip(
                                selected = chosenTag == t,
                                onClick = { chosenTag = t },
                                label = { Text(t, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = if (stickerName.isBlank()) "Sticker" else stickerName.trim()
                        onSaveCurrentSelection(name, chosenTag)
                        showCreateDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
