package com.example.homestay;

import static org.junit.Assert.assertEquals;

import com.example.homestay.data.entity.Booking;
import com.example.homestay.data.entity.Room;
import com.example.homestay.domain.RoomSearchEngine;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class RoomSearchEngineTest {
  @Test
  public void filtersUnavailableAndInsufficientCapacityRooms() {
    List<Room> result =
        RoomSearchEngine.filter(
            Arrays.asList(room(1, "Small", 1, true, 300), room(2, "Closed", 4, false, 200)),
            Collections.emptyList(),
            "",
            2,
            -1,
            -1,
            RoomSearchEngine.SortOrder.RECOMMENDED);
    assertEquals(0, result.size());
  }

  @Test
  public void excludesRoomWhenAllSlotsOverlap() {
    Room room = room(1, "Villa", 4, true, 500);
    Booking booking = booking(1, 100, 200, "confirmed");
    assertEquals(
        0,
        RoomSearchEngine.filter(
                Collections.singletonList(room),
                Collections.singletonList(booking),
                "",
                1,
                150,
                250,
                RoomSearchEngine.SortOrder.RECOMMENDED)
            .size());
  }

  @Test
  public void sortsByLowestPrice() {
    List<Room> result =
        RoomSearchEngine.filter(
            Arrays.asList(room(1, "Expensive", 2, true, 900), room(2, "Cheap", 2, true, 300)),
            Collections.emptyList(),
            "",
            1,
            -1,
            -1,
            RoomSearchEngine.SortOrder.PRICE_LOW);
    assertEquals("Cheap", result.get(0).getName());
  }

  private static Room room(long id, String name, int maxGuests, boolean available, double price) {
    return new Room(
        id,
        null,
        name,
        "Description",
        price,
        "image",
        "Location",
        "Address",
        5f,
        0,
        "WiFi",
        maxGuests,
        "Room",
        20,
        1,
        available,
        false);
  }

  private static Booking booking(long roomId, long checkIn, long checkOut, String status) {
    return new Booking(
        0,
        null,
        roomId,
        null,
        null,
        null,
        1,
        null,
        checkIn,
        checkOut,
        1,
        500,
        status,
        null,
        1,
        null,
        0,
        0);
  }
}
