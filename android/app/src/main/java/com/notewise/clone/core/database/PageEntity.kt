package com.notewise.clone.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = NotebookEntity::class,
            parentColumns = ["id"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["notebookId", "pageIndex"])]
)
data class PageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val notebookId: String,
    val pageIndex: Int,
    val templateType: String = "Ruled",
    val backgroundColor: String = "#FFFFFF",
    val pdfBackgroundUrl: String? = null,
    val strokeJson: String? = null,
    val audioRecordPath: String? = null,
    val thumbnailPath: String? = null,
    val isDeleted: Boolean = false,
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
