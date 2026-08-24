package com.example.homestay.domain;

import com.example.homestay.data.entity.Room;
import com.example.homestay.data.entity.Slot;
import java.util.Calendar;

public final class BookingCalculator {
  public static final long MILLIS_PER_DAY = 86_400_000L;

  private BookingCalculator() {}

  public static long nights(long checkIn, long checkOut) {
    if (checkOut <= checkIn) return 0;
    Calendar in = Calendar.getInstance(), out = Calendar.getInstance();
    in.setTimeInMillis(checkIn);
    out.setTimeInMillis(checkOut);
    in.set(Calendar.HOUR_OF_DAY, 12);
    out.set(Calendar.HOUR_OF_DAY, 12);
    in.set(Calendar.MINUTE, 0);
    out.set(Calendar.MINUTE, 0);
    in.set(Calendar.SECOND, 0);
    out.set(Calendar.SECOND, 0);
    in.set(Calendar.MILLISECOND, 0);
    out.set(Calendar.MILLISECOND, 0);
    return Math.max(0, Math.round((out.getTimeInMillis() - in.getTimeInMillis()) / (double) MILLIS_PER_DAY));
  }

  public static double nightlyPrice(Room room, Slot selectedSlot) {
    if (selectedSlot != null && selectedSlot.getPrice() != null && selectedSlot.getPrice() > 0) {
      return selectedSlot.getPrice();
    }
    return room.getPrice();
  }

  public static double total(Room room, Slot selectedSlot, long checkIn, long checkOut) {
    return nightlyPrice(room, selectedSlot) * nights(checkIn, checkOut);
  }
}
