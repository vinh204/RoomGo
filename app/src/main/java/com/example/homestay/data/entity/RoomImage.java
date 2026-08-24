package com.example.homestay.data.entity;

import androidx.annotation.NonNull;
import androidx.room.*;

@Entity(
    tableName = "room_images",
    foreignKeys =
        @ForeignKey(
            entity = Room.class,
            parentColumns = "id",
            childColumns = "roomId",
            onDelete = ForeignKey.CASCADE),
    indices = {@Index("roomId")})
public class RoomImage {
  @PrimaryKey(autoGenerate = true) private final long id;
  private final long roomId;
  @NonNull private final String imageUri;
  private final int position;

  public RoomImage(long id, long roomId, @NonNull String imageUri, int position) {
    this.id = id;
    this.roomId = roomId;
    this.imageUri = imageUri;
    this.position = position;
  }

  public long getId() { return id; }
  public long getRoomId() { return roomId; }
  public String getImageUri() { return imageUri; }
  public int getPosition() { return position; }
}
