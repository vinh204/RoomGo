package com.example.homestay.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.*;

@Entity(tableName = "slots", indices = @Index(value = {"roomId"}))
public class Slot {
  @PrimaryKey(autoGenerate = true)
  final long id;

  final long roomId;
  final int slotNumber;
  @NonNull final String slotName;
  final boolean isAvailable;
  final Double price;

  public Slot(
      long id,
      long roomId,
      int slotNumber,
      String slotName,
      boolean isAvailable,
      @Nullable Double price) {
    this.id = id;
    this.roomId = roomId;
    this.slotNumber = slotNumber;
    this.slotName = slotName;
    this.isAvailable = isAvailable;
    this.price = price;
  }

  public long getId() {
    return id;
  }

  public long getRoomId() {
    return roomId;
  }

  public int getSlotNumber() {
    return slotNumber;
  }

  public String getSlotName() {
    return slotName;
  }

  public boolean isAvailable() {
    return isAvailable;
  }

  public Double getPrice() {
    return price;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Slot && ((Slot) o).id == id && ((Slot) o).isAvailable == isAvailable;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(id);
  }
}
