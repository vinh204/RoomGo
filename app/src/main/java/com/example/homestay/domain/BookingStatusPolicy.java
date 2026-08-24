package com.example.homestay.domain;

import java.util.Locale;

/** Quy tắc chuyển trạng thái booking dùng chung cho giao diện khách và quản trị. */
public final class BookingStatusPolicy {
  public static final String PENDING = "pending";
  public static final String CONFIRMED = "confirmed";
  public static final String CHECKED_IN = "checked_in";
  public static final String COMPLETED = "completed";
  public static final String CANCELLED = "cancelled";
  public static final String EXPIRED = "expired";

  private BookingStatusPolicy() {}

  public static boolean canTransition(String current, String next) {
    String from = normalize(current), to = normalize(next);
    if (from.equals(to)) return true;
    if (PENDING.equals(from)) return CONFIRMED.equals(to) || CANCELLED.equals(to);
    if (CONFIRMED.equals(from))
      return CHECKED_IN.equals(to) || COMPLETED.equals(to) || CANCELLED.equals(to);
    if (CHECKED_IN.equals(from)) return COMPLETED.equals(to);
    return false;
  }

  public static boolean canCancel(String status, long checkInAt, long now) {
    String value = normalize(status);
    return (PENDING.equals(value) || CONFIRMED.equals(value)) && now < checkInAt;
  }

  public static boolean canComplete(String status, long checkOutAt, long now) {
    String value = normalize(status);
    return (CONFIRMED.equals(value) || CHECKED_IN.equals(value)) && now >= checkOutAt;
  }

  public static String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }
}
