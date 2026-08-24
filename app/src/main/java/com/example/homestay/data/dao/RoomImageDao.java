package com.example.homestay.data.dao;

import androidx.room.*;
import com.example.homestay.data.entity.RoomImage;
import java.util.List;

@Dao
public interface RoomImageDao {
  @Query("SELECT * FROM room_images WHERE roomId=:roomId ORDER BY position ASC, id ASC")
  List<RoomImage> getByRoomId(long roomId);

  @Insert
  void insertAll(List<RoomImage> images);

  @Query("DELETE FROM room_images WHERE roomId=:roomId")
  void deleteByRoomId(long roomId);
}
