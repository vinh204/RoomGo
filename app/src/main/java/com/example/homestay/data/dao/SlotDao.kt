package com.example.homestay.data.dao

import androidx.room.*
import com.example.homestay.data.entity.Slot
import kotlinx.coroutines.flow.Flow

@Dao
interface SlotDao {
    @Query("DELETE FROM slots WHERE roomId = :roomId")
    suspend fun deleteByRoomId(roomId: Long)
    @Query("SELECT * FROM slots WHERE roomId = :roomId ORDER BY slotNumber ASC")
    fun getSlotsByRoomId(roomId: Long): Flow<List<Slot>>

    @Query("SELECT * FROM slots WHERE roomId = :roomId AND isAvailable = 1 ORDER BY slotNumber ASC")
    fun getAvailableSlotsByRoomId(roomId: Long): Flow<List<Slot>>

    @Query("SELECT * FROM slots WHERE id = :slotId")
    suspend fun getSlotById(slotId: Long): Slot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: Slot): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<Slot>)

    @Update
    suspend fun updateSlot(slot: Slot)

    @Delete
    suspend fun deleteSlot(slot: Slot)

    @Query("DELETE FROM slots WHERE roomId = :roomId")
    suspend fun deleteSlotsByRoomId(roomId: Long)

    @Query("DELETE FROM slots")
    suspend fun deleteAllSlots()
}

