package com.example.homestay.data.entity;

import androidx.room.*;

@Entity(
    tableName = "favorites",
    indices =
        @Index(
            value = {"userId", "roomId"},
            unique = true))
public class Favorite {
  @PrimaryKey(autoGenerate = true)
  private final long id;

  private final long userId, roomId, createdAt;

  public Favorite(long id, long userId, long roomId, long createdAt) {
    this.id = id;
    this.userId = userId;
    this.roomId = roomId;
    this.createdAt = createdAt;
  }

  public long getId() {
    return id;
  }

  public long getUserId() {
    return userId;
  }

  public long getRoomId() {
    return roomId;
  }

  public long getCreatedAt() {
    return createdAt;
  }
}
