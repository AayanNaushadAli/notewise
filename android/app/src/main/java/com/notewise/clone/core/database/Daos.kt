package com.notewise.clone.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("UPDATE folders SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteFolder(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun permanentlyDeleteFolder(id: String)
}

@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebooks WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotebooks(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE isDeleted = 0 AND isPinned = 1 ORDER BY updatedAt DESC")
    fun getPinnedNotebooks(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE isDeleted = 0 AND folderId = :folderId ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotebooksInFolder(folderId: String): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getTrashNotebooks(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE id = :id LIMIT 1")
    suspend fun getNotebookById(id: String): NotebookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebook(notebook: NotebookEntity)

    @Update
    suspend fun updateNotebook(notebook: NotebookEntity)

    @Query("UPDATE notebooks SET isPinned = :isPinned, updatedAt = :timestamp WHERE id = :id")
    suspend fun setPinned(id: String, isPinned: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE notebooks SET folderId = :folderId, updatedAt = :timestamp WHERE id = :id")
    suspend fun moveToFolder(id: String, folderId: String?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE notebooks SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteNotebook(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE notebooks SET isDeleted = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun restoreNotebook(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM notebooks WHERE id = :id")
    suspend fun permanentlyDeleteNotebook(id: String)

    @Query("UPDATE notebooks SET pageCount = :count, updatedAt = :timestamp WHERE id = :id")
    suspend fun updatePageCount(id: String, count: Int, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM notebooks WHERE updatedAt > :since")
    suspend fun getModifiedSince(since: Long): List<NotebookEntity>
}

@Dao
interface PageDao {
    @Query("SELECT * FROM pages WHERE notebookId = :notebookId AND isDeleted = 0 ORDER BY pageIndex ASC")
    fun getPagesForNotebook(notebookId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE notebookId = :notebookId AND isDeleted = 0 ORDER BY pageIndex ASC")
    suspend fun getPagesListForNotebook(notebookId: String): List<PageEntity>

    @Query("SELECT * FROM pages WHERE notebookId = :notebookId AND pageIndex = :pageIndex AND isDeleted = 0 LIMIT 1")
    suspend fun getPage(notebookId: String, pageIndex: Int): PageEntity?

    @Query("SELECT * FROM pages WHERE id = :id LIMIT 1")
    suspend fun getPageById(id: String): PageEntity?

    @Query("SELECT COUNT(*) FROM pages WHERE notebookId = :notebookId AND isDeleted = 0")
    suspend fun getPageCount(notebookId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPage(page: PageEntity)

    @Query("UPDATE pages SET strokeJson = :strokeJson, updatedAt = :timestamp WHERE id = :pageId")
    suspend fun updateStrokes(pageId: String, strokeJson: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE pages SET templateType = :template, backgroundColor = :bgColor, updatedAt = :timestamp WHERE id = :pageId")
    suspend fun updateTemplate(pageId: String, template: String, bgColor: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE pages SET audioRecordPath = :audioPath, updatedAt = :timestamp WHERE id = :pageId")
    suspend fun updateAudioPath(pageId: String, audioPath: String?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE pages SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeletePage(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM pages WHERE id = :id")
    suspend fun permanentlyDeletePage(id: String)

    @Query("SELECT * FROM pages WHERE updatedAt > :since")
    suspend fun getModifiedSince(since: Long): List<PageEntity>
}
