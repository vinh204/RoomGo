package com.example.homestay.data.dao;

import androidx.room.*;
import com.example.homestay.data.entity.Slot;
import java.util.List;

@Dao
public interface SlotDao {
  @Query("DELETE FROM slots WHERE roomId=:id")
  void deleteByRoomId(long id);

  @Query("SELECT * FROM slots WHERE roomId=:id ORDER BY slotNumber ASC")
  List<Slot> getSlotsByRoomIdNow(long id);

  @Query("SELECT * FROM slots WHERE roomId=:id AND isAvailable=1 ORDER BY slotNumber ASC")
  List<Slot> getAvailableSlotsByRoomIdNow(long id);

  @Query("SELECT * FROM slots WHERE id=:id")
  Slot getSlotById(long id);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  long insertSlot(Slot s);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertSlots(List<Slot> s);

  @Update
  void updateSlot(Slot s);

  @Delete
  void deleteSlot(Slot s);

  @Query("DELETE FROM slots")
  void deleteAllSlots();
}
