package com.notewise.clone.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notewise.clone.core.database.PageEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageOverviewSheet(
    pages: List<PageEntity>,
    currentPageIndex: Int,
    onSelectPage: (Int) -> Unit,
    onAddPage: () -> Unit,
    onDuplicatePage: (Int) -> Unit,
    onDeletePage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
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
                    "Page Overview (${pages.size} pages)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Button(
                    onClick = onAddPage,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Page")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                itemsIndexed(pages) { index, page ->
                    val isCurrent = index == currentPageIndex
                    val pageBg = try {
                        Color(android.graphics.Color.parseColor(page.backgroundColor))
                    } catch (e: Exception) {
                        Color.White
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectPage(index)
                                onDismiss()
                            }
                    ) {
                        // Page thumbnail representation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(pageBg)
                                .border(
                                    width = if (isCurrent) 3.dp else 1.dp,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = if (page.backgroundColor == "#0F172A" || page.backgroundColor == "#1E293B") Color.White else Color.Black
                                )
                                Text(
                                    page.templateType,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Controls below thumbnail: duplicate, delete
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Page ${index + 1}",
                                fontSize = 12.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Row {
                                IconButton(
                                    onClick = { onDuplicatePage(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Duplicate",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.Gray
                                    )
                                }
                                if (pages.size > 1) {
                                    IconButton(
                                        onClick = { onDeletePage(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.Red.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

