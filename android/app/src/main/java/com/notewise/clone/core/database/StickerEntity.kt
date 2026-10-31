package com.notewise.clone.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "stickers")
data class StickerEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tag: String = "General",
    val strokeJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface StickerDao {
    @Query("SELECT * FROM stickers ORDER BY createdAt DESC")
    fun getAllStickers(): Flow<List<StickerEntity>>

    @Query("SELECT * FROM stickers WHERE tag = :tag ORDER BY createdAt DESC")
    fun getStickersByTag(tag: String): Flow<List<StickerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSticker(sticker: StickerEntity)

    @Query("DELETE FROM stickers WHERE id = :id")
    suspend fun deleteSticker(id: String)
}

