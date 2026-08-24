package com.example.homestay;

import static org.junit.Assert.assertEquals;

import com.example.homestay.data.entity.Room;
import com.example.homestay.data.entity.Slot;
import com.example.homestay.domain.BookingCalculator;
import org.junit.Test;

public class BookingCalculatorTest {
  @Test
  public void calculatesNightsAndRoomTotal() {
    long checkIn = 1_000_000L;
    long checkOut = checkIn + BookingCalculator.MILLIS_PER_DAY * 3;
    assertEquals(3, BookingCalculator.nights(checkIn, checkOut));
    assertEquals(1_500_000d, BookingCalculator.total(room(), null, checkIn, checkOut), 0d);
  }

  @Test
  public void selectedSlotOverridesRoomPrice() {
    Slot slot = new Slot(1, 1, 1, "Giường", true, 300_000d);
    assertEquals(300_000d, BookingCalculator.nightlyPrice(room(), slot), 0d);
  }

  private static Room room() {
    return new Room(
        1,
        null,
        "Room",
        "Description",
        500_000d,
        "image",
        "Location",
        "Address",
        5f,
        0,
        "WiFi",
        2,
        "Room",
        20,
        1,
        true,
        false);
  }
}
