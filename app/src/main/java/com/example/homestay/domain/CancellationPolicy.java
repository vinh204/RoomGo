package com.example.homestay.domain;

/** Chính sách hoàn tiền demo dùng thống nhất cho khách và quản trị viên. */
public final class CancellationPolicy {
  public static final long FULL_REFUND_WINDOW_MS = 24L * 60 * 60 * 1000;

  private CancellationPolicy() {}

  public static double refund(double totalPrice, long checkInAt, long cancelledAt) {
    long remaining = checkInAt - cancelledAt;
    if (remaining >= FULL_REFUND_WINDOW_MS) return totalPrice;
    if (remaining > 0) return totalPrice * 0.5d;
    return 0d;
  }
}
