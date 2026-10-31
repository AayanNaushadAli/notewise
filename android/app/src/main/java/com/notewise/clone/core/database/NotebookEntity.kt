package com.notewise.clone.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notebooks")
data class NotebookEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val folderId: String? = null,
    val coverColor: String = "#1E3A8A",
    val coverStyle: String = "classic", // classic, gradient, minimal, texture
    val pageCount: Int = 1,
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false,
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
