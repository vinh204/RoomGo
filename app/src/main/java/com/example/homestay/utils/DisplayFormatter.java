package com.example.homestay.utils;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Central formatting rules shared by customer and administrator screens. */
public final class DisplayFormatter {
  private static final Locale VIETNAMESE = new Locale("vi", "VN");

  private DisplayFormatter() {}

  public static String number(double value) {
    return NumberFormat.getNumberInstance(VIETNAMESE).format((long) value);
  }

  public static String vnd(double value) {
    return number(value) + " đ";
  }

  public static String bookingCode(long id, long createdAt) {
    return bookingCode(Long.toString(id), createdAt);
  }

  public static String bookingCode(String id, long createdAt) {
    String date = new SimpleDateFormat("yyMMdd", Locale.ROOT).format(new Date(createdAt));
    return "#RG" + date + leftPad(id, 3);
  }

  public static String date(long timestamp) {
    return new SimpleDateFormat("dd/MM/yyyy", VIETNAMESE).format(new Date(timestamp));
  }

  private static String leftPad(String value, int length) {
    StringBuilder result = new StringBuilder(value);
    while (result.length() < length) result.insert(0, '0');
    return result.toString();
  }
}
