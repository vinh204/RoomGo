package com.example.homestay.utils;

import java.util.Calendar;

/** Giờ nhận và trả phòng thống nhất của RoomGo. */
public final class BookingTimeUtils {
  public static final int CHECK_IN_HOUR = 14;
  public static final int CHECK_OUT_HOUR = 12;

  private BookingTimeUtils() {}

  public static long checkInMillis(int year, int month, int day) {
    return atTime(year, month, day, CHECK_IN_HOUR);
  }

  public static long checkOutMillis(int year, int month, int day) {
    return atTime(year, month, day, CHECK_OUT_HOUR);
  }

  private static long atTime(int year, int month, int day, int hour) {
    Calendar calendar = Calendar.getInstance();
    calendar.set(year, month, day, hour, 0, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTimeInMillis();
  }
}
