package com.notewise.clone.ui.home

import android.graphics.Color as AndroidColor
import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notewise.clone.core.database.FolderEntity
import com.notewise.clone.core.database.NotebookEntity
import java.text.SimpleDateFormat
import java.util.*

enum class HomeTab {
    ALL_NOTES,
    FOLDERS,
    FAVORITES,
    TRASH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    notebooks: List<NotebookEntity>,
    trashNotebooks: List<NotebookEntity>,
    folders: List<FolderEntity>,
    onNotebookClick: (String) -> Unit,
    onCreateNotebook: (title: String, coverColorHex: String, template: String, paperColorHex: String) -> Unit,
    onCreateFolder: (name: String, colorHex: String) -> Unit,
    onPinNotebook: (id: String, isPinned: Boolean) -> Unit,
    onDuplicateNotebook: (id: String) -> Unit,
    onDeleteNotebook: (id: String) -> Unit,
    onRestoreNotebook: (id: String) -> Unit,
    onPermanentlyDeleteNotebook: (id: String) -> Unit,
    onSyncClick: () -> Unit,
    syncServerUrl: String,
    isSyncing: Boolean
) {
    var selectedTab by remember { mutableStateOf(HomeTab.ALL_NOTES) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateNotebookDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var selectedFolderFilter by remember { mutableStateOf<String?>(null) }

    // Filter notebooks based on tab, folder, and search query
    val displayList = remember(notebooks, trashNotebooks, selectedTab, searchQuery, selectedFolderFilter) {
        when (selectedTab) {
            HomeTab.ALL_NOTES -> {
                notebooks.filter { nb ->
                    (selectedFolderFilter == null || nb.folderId == selectedFolderFilter) &&
                            (searchQuery.isBlank() || nb.title.contains(searchQuery, ignoreCase = true))
                }
            }
            HomeTab.FAVORITES -> {
                notebooks.filter { nb ->
                    nb.isPinned && (searchQuery.isBlank() || nb.title.contains(searchQuery, ignoreCase = true))
                }
            }
            HomeTab.FOLDERS -> {
                notebooks.filter { nb ->
                    (selectedFolderFilter == null || nb.folderId == selectedFolderFilter) &&
                            (searchQuery.isBlank() || nb.title.contains(searchQuery, ignoreCase = true))
                }
            }
            HomeTab.TRASH -> {
                trashNotebooks.filter { nb ->
                    searchQuery.isBlank() || nb.title.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Notewise",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSyncClick) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = "Sync Cloud",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (selectedTab != HomeTab.TRASH) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (selectedTab == HomeTab.FOLDERS && selectedFolderFilter == null) {
                            showCreateFolderDialog = true
                        } else {
                            showCreateNotebookDialog = true
                        }
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = {
                        Text(if (selectedTab == HomeTab.FOLDERS && selectedFolderFilter == null) "New Folder" else "New Note")
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = { Text("Search notes, pages...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            // Navigation Tabs (All Notes, Folders, Favorites, Trash)
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Tab(
                    selected = selectedTab == HomeTab.ALL_NOTES,
                    onClick = {
                        selectedTab = HomeTab.ALL_NOTES
                        selectedFolderFilter = null
                    },
                    text = { Text("All Notes") }
                )
                Tab(
                    selected = selectedTab == HomeTab.FOLDERS,
                    onClick = { selectedTab = HomeTab.FOLDERS },
                    text = { Text("Folders") }
                )
                Tab(
                    selected = selectedTab == HomeTab.FAVORITES,
                    onClick = {
                        selectedTab = HomeTab.FAVORITES
                        selectedFolderFilter = null
                    },
                    text = { Text("Favorites") }
                )
                Tab(
                    selected = selectedTab == HomeTab.TRASH,
                    onClick = {
                        selectedTab = HomeTab.TRASH
                        selectedFolderFilter = null
                    },
                    text = { Text("Trash") }
                )
            }

            // Folder Filter Breadcrumb
            if (selectedFolderFilter != null) {
                val folderName = folders.find { it.id == selectedFolderFilter }?.name ?: "Folder"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    TextButton(onClick = { selectedFolderFilter = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("All Folders")
                    }
                    Text("/ $folderName", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // Folder List (if in Folders tab and no specific folder is selected)
            if (selectedTab == HomeTab.FOLDERS && selectedFolderFilter == null) {
                if (folders.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No folders yet. Tap '+' to create a folder!", color = Color.Gray)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        items(folders, key = { it.id }) { folder ->
                            val count = notebooks.count { it.folderId == folder.id }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFolderFilter = folder.id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(folder.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("$count notes", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Notebook Grid
            if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val msg = when (selectedTab) {
                        HomeTab.TRASH -> "Trash is empty."
                        HomeTab.FAVORITES -> "No favorite notes yet. Tap the star to pin notes here!"
                        else -> "No notebooks found. Tap '+' to create your first note!"
                    }
                    Text(msg, color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 155.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayList, key = { it.id }) { notebook ->
                        NotebookCard(
                            notebook = notebook,
                            isTrash = selectedTab == HomeTab.TRASH,
                            onClick = {
                                if (selectedTab != HomeTab.TRASH) {
                                    onNotebookClick(notebook.id)
                                }
                            },
                            onPinToggle = { onPinNotebook(notebook.id, !notebook.isPinned) },
                            onDuplicate = { onDuplicateNotebook(notebook.id) },
                            onDelete = { onDeleteNotebook(notebook.id) },
                            onRestore = { onRestoreNotebook(notebook.id) },
                            onPermanentDelete = { onPermanentlyDeleteNotebook(notebook.id) }
                        )
                    }
                }
            }
        }
    }

    // Create Notebook Dialog
    if (showCreateNotebookDialog) {
        CreateNotebookModal(
            onDismiss = { showCreateNotebookDialog = false },
            onConfirm = { title, coverHex, template, paperHex ->
                onCreateNotebook(title, coverHex, template, paperHex)
                showCreateNotebookDialog = false
            }
        )
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    placeholder = { Text("Folder name (e.g. Work, Study)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            onCreateFolder(folderName, "#3B82F6")
                            showCreateFolderDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun NotebookCard(
    notebook: NotebookEntity,
    isTrash: Boolean,
    onClick: () -> Unit,
    onPinToggle: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val coverColor = remember(notebook.coverColor) {
        try {
            Color(AndroidColor.parseColor(notebook.coverColor))
        } catch (e: Exception) {
            Color(0xFF1E3A8A)
        }
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dateString = remember(notebook.updatedAt) {
        dateFormat.format(Date(notebook.updatedAt))
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            // Notebook Cover representation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                coverColor,
                                coverColor.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(12.dp)
            ) {
                // Pin Star Icon
                if (notebook.isPinned && !isTrash) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Pinned",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(20.dp)
                    )
                }

                // Page count badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        "${notebook.pageCount} ${if (notebook.pageCount == 1) "page" else "pages"}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                // 3-dots Menu button
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (!isTrash) {
                            DropdownMenuItem(
                                text = { Text(if (notebook.isPinned) "Unpin" else "Pin to Top") },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onPinToggle()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onDuplicate()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Trash", color = Color.Red) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Restore") },
                                leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onRestore()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Permanently", color = Color.Red) },
                                leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    onPermanentDelete()
                                }
                            )
                        }
                    }
                }
            }

            // Notebook Info (Title and Date)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                Text(
                    text = notebook.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateString,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun CreateNotebookModal(
    onDismiss: () -> Unit,
    onConfirm: (title: String, coverColorHex: String, template: String, paperColorHex: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCoverHex by remember { mutableStateOf("#1E3A8A") }
    var selectedTemplate by remember { mutableStateOf("Ruled") }
    var selectedPaperHex by remember { mutableStateOf("#FFFFFF") }

    val coverColors = listOf(
        "#1E3A8A", // Deep Navy
        "#047857", // Forest Green
        "#B91C1C", // Crimson
        "#6D28D9", // Royal Purple
        "#D97706", // Amber
        "#334155", // Slate
        "#0F172A", // Obsidian
        "#EC4899"  // Pink
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Notebook", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notebook Title") },
                    placeholder = { Text("e.g. Biology Notes, Meeting Log") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Cover Color", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(coverColors) { hex ->
                        val isSelected = selectedCoverHex.equals(hex, ignoreCase = true)
                        val color = Color(AndroidColor.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedCoverHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Initial Template", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                val tpls = listOf("Ruled", "Grid", "Dot Grid", "Cornell Notes", "Blank")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tpls) { tpl ->
                        FilterChip(
                            selected = selectedTemplate == tpl,
                            onClick = { selectedTemplate = tpl },
                            label = { Text(tpl, fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = if (title.isBlank()) "Untitled Note" else title.trim()
                    onConfirm(finalTitle, selectedCoverHex, selectedTemplate, selectedPaperHex)
                }
            ) {
                Text("Create Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
