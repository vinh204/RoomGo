package com.example.homestay.data.dao

import androidx.room.*
import com.example.homestay.data.entity.Favorite
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId AND roomId = :roomId LIMIT 1")
    suspend fun getFavorite(userId: Long, roomId: Long): Favorite?

    @Query("SELECT roomId FROM favorites WHERE userId = :userId")
    fun getFavoriteRoomIds(userId: Long): Flow<List<Long>>

    @Query("SELECT * FROM favorites WHERE userId = :userId")
    fun getFavoritesByUserId(userId: Long): Flow<List<Favorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite): Long

    @Delete
    suspend fun deleteFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE userId = :userId AND roomId = :roomId")
    suspend fun deleteFavorite(userId: Long, roomId: Long)

    @Query("SELECT COUNT(*) FROM favorites WHERE userId = :userId AND roomId = :roomId")
    suspend fun isFavorite(userId: Long, roomId: Long): Int

    @Query("DELETE FROM favorites WHERE roomId = :roomId")
    suspend fun deleteByRoomId(roomId: Long)

    @Query("DELETE FROM favorites WHERE userId = :userId")
    suspend fun deleteByUserId(userId: Long)
}

