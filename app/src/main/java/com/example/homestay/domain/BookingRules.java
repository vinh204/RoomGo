package com.example.homestay.domain;

public final class BookingRules {
  public static final long MIN_ADVANCE_MS = 60L * 60 * 1000;
  private BookingRules() {}

  public static String validate(
      long checkInDate,
      long checkOutDate,
      int guestCount,
      int maxGuests,
      int occupiedSlots,
      int maxSlots,
      long now) {
    if (checkInDate < now + MIN_ADVANCE_MS)
      return "Cần đặt phòng trước giờ nhận ít nhất 1 giờ";
    if (checkOutDate <= checkInDate) return "Ngày trả phòng phải sau ngày nhận phòng";
    if (guestCount < 1 || guestCount > maxGuests) return "Số khách tối đa là " + maxGuests;
    if (occupiedSlots >= maxSlots) return "Phòng đã hết chỗ trong khoảng ngày đã chọn";
    return null;
  }
}
