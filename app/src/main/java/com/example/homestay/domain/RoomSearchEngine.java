package com.example.homestay.domain;

import com.example.homestay.data.entity.Booking;
import com.example.homestay.data.entity.Room;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Pure room search logic, kept outside Activity code so it can be tested independently. */
public final class RoomSearchEngine {
  private RoomSearchEngine() {}

  public static List<Room> filter(
      List<Room> rooms,
      List<Booking> bookings,
      String query,
      int guests,
      long checkIn,
      long checkOut,
      SortOrder sortOrder) {
    String key = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    List<Room> result = new ArrayList<>();
    for (Room room : rooms) {
      if (!room.isAvailable() || room.getMaxGuests() < guests) continue;
      if (!matches(room, key)) continue;
      if (hasDates(checkIn, checkOut)
          && occupiedSlots(room.getId(), bookings, checkIn, checkOut) >= room.getMaxSlots()) {
        continue;
      }
      result.add(room);
    }
    result.sort(comparator(sortOrder));
    return result;
  }

  public static Room featured(List<Room> rooms) {
    for (Room room : rooms) {
      if (room.isAvailable() && room.isFeatured()) return room;
    }
    return null;
  }

  private static boolean matches(Room room, String key) {
    if (key.isEmpty()) return true;
    return (room.getName() + " " + room.getLocation() + " " + room.getAddress())
        .toLowerCase(Locale.ROOT)
        .contains(key);
  }

  private static boolean hasDates(long checkIn, long checkOut) {
    return checkIn >= 0 && checkOut > checkIn;
  }

  private static int occupiedSlots(
      long roomId, List<Booking> bookings, long checkIn, long checkOut) {
    int occupied = 0;
    for (Booking booking : bookings) {
      if (booking.getRoomId() == roomId
          && ("pending".equals(booking.getStatus()) || "confirmed".equals(booking.getStatus()))
          && booking.getCheckInDate() < checkOut
          && booking.getCheckOutDate() > checkIn) {
        occupied++;
      }
    }
    return occupied;
  }

  private static Comparator<Room> comparator(SortOrder sortOrder) {
    if (sortOrder == SortOrder.PRICE_LOW) return Comparator.comparingDouble(Room::getPrice);
    if (sortOrder == SortOrder.PRICE_HIGH)
      return (first, second) -> Double.compare(second.getPrice(), first.getPrice());
    return (first, second) -> Float.compare(second.getRating(), first.getRating());
  }

  public enum SortOrder {
    RECOMMENDED("Đề xuất"),
    PRICE_LOW("Giá thấp nhất"),
    PRICE_HIGH("Giá cao nhất"),
    RATING("Đánh giá tốt");

    public final String label;

    SortOrder(String label) {
      this.label = label;
    }
  }
}
