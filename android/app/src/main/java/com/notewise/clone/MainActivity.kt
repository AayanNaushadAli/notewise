package com.notewise.clone

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.notewise.clone.core.database.FolderEntity
import com.notewise.clone.core.database.NotebookEntity
import com.notewise.clone.core.database.PageEntity
import com.notewise.clone.core.sync.SyncManager
import com.notewise.clone.ui.editor.EditorScreen
import com.notewise.clone.ui.home.HomeScreen
import com.notewise.clone.ui.theme.NotewiseTheme
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val db by lazy { (application as NoteApplication).database }
    private val syncManager by lazy {
        SyncManager(this).apply {
            // Point to host machine IP on local Wi-Fi network
            serverBaseUrl = "http://192.168.0.103:3000"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NotewiseTheme {
                val notebooks by db.notebookDao().getAllNotebooks().collectAsState(initial = emptyList())
                val trashNotebooks by db.notebookDao().getTrashNotebooks().collectAsState(initial = emptyList())
                val folders by db.folderDao().getAllFolders().collectAsState(initial = emptyList())

                var activeNotebookId by remember { mutableStateOf<String?>(null) }
                var isSyncing by remember { mutableStateOf(false) }

                if (activeNotebookId == null) {
                    HomeScreen(
                        notebooks = notebooks,
                        trashNotebooks = trashNotebooks,
                        folders = folders,
                        onNotebookClick = { id -> activeNotebookId = id },
                        onCreateNotebook = { title, coverHex, template, paperHex ->
                            lifecycleScope.launch {
                                val notebook = NotebookEntity(
                                    title = title,
                                    coverColor = coverHex,
                                    pageCount = 1
                                )
                                db.notebookDao().insertNotebook(notebook)

                                // Create initial page
                                val page = PageEntity(
                                    notebookId = notebook.id,
                                    pageIndex = 0,
                                    templateType = template,
                                    backgroundColor = paperHex
                                )
                                db.pageDao().upsertPage(page)

                                activeNotebookId = notebook.id
                            }
                        },
                        onCreateFolder = { name, colorHex ->
                            lifecycleScope.launch {
                                val folder = FolderEntity(name = name, color = colorHex)
                                db.folderDao().insertFolder(folder)
                                Toast.makeText(this@MainActivity, "Folder '$name' created", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onPinNotebook = { id, isPinned ->
                            lifecycleScope.launch {
                                db.notebookDao().setPinned(id, isPinned)
                            }
                        },
                        onDuplicateNotebook = { id ->
                            lifecycleScope.launch {
                                val original = db.notebookDao().getNotebookById(id) ?: return@launch
                                val originalPages = db.pageDao().getPagesListForNotebook(id)

                                val duplicateId = UUID.randomUUID().toString()
                                val duplicateNotebook = original.copy(
                                    id = duplicateId,
                                    title = "${original.title} (Copy)",
                                    createdAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                db.notebookDao().insertNotebook(duplicateNotebook)

                                for (p in originalPages) {
                                    val dupPage = p.copy(
                                        id = UUID.randomUUID().toString(),
                                        notebookId = duplicateId,
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    db.pageDao().upsertPage(dupPage)
                                }
                                Toast.makeText(this@MainActivity, "Notebook duplicated", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteNotebook = { id ->
                            lifecycleScope.launch {
                                db.notebookDao().softDeleteNotebook(id)
                                Toast.makeText(this@MainActivity, "Moved to Trash", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRestoreNotebook = { id ->
                            lifecycleScope.launch {
                                db.notebookDao().restoreNotebook(id)
                                Toast.makeText(this@MainActivity, "Restored note", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onPermanentlyDeleteNotebook = { id ->
                            lifecycleScope.launch {
                                db.notebookDao().permanentlyDeleteNotebook(id)
                                Toast.makeText(this@MainActivity, "Deleted permanently", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSyncClick = {
                            lifecycleScope.launch {
                                isSyncing = true
                                Toast.makeText(this@MainActivity, "Syncing with Supabase Cloud...", Toast.LENGTH_SHORT).show()
                                val success = syncManager.sync()
                                isSyncing = false
                                val msg = if (success) "Cloud sync completed successfully!" else "Offline mode (Server: 192.168.0.103:3000)"
                                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        syncServerUrl = syncManager.serverBaseUrl,
                        isSyncing = isSyncing
                    )
                } else {
                    val currentNotebook = notebooks.find { it.id == activeNotebookId }
                    EditorScreen(
                        notebookId = activeNotebookId!!,
                        notebookTitle = currentNotebook?.title ?: "Note",
                        onBackClick = { activeNotebookId = null }
                    )
                }
            }
        }
    }
}
