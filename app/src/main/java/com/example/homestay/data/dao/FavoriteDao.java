package com.example.homestay.data.dao;

import androidx.room.*;
import com.example.homestay.data.entity.Favorite;
import java.util.List;

@Dao
public interface FavoriteDao {
  @Query("SELECT * FROM favorites WHERE userId=:u AND roomId=:r LIMIT 1")
  Favorite getFavorite(long u, long r);

  @Query("SELECT roomId FROM favorites WHERE userId=:u")
  List<Long> getFavoriteRoomIdsNow(long u);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  long insertFavorite(Favorite f);

  @Delete
  void deleteFavorite(Favorite f);

  @Query("DELETE FROM favorites WHERE userId=:u AND roomId=:r")
  void deleteFavorite(long u, long r);

  @Query("SELECT COUNT(*) FROM favorites WHERE userId=:u AND roomId=:r")
  int isFavorite(long u, long r);

  @Query("DELETE FROM favorites WHERE roomId=:id")
  void deleteByRoomId(long id);

  @Query("DELETE FROM favorites WHERE userId=:id")
  void deleteByUserId(long id);
}
