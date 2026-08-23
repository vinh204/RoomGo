package com.example.homestay.domain;

public final class BookingRules {
  private BookingRules() {}

  public static String validate(
      long checkInDate,
      long checkOutDate,
      int guestCount,
      int maxGuests,
      int occupiedSlots,
      int maxSlots,
      long todayStart) {
    if (checkInDate < todayStart) return "Ngày nhận phòng không được ở trong quá khứ";
    if (checkOutDate <= checkInDate) return "Ngày trả phòng phải sau ngày nhận phòng";
    if (guestCount < 1 || guestCount > maxGuests) return "Số khách tối đa là " + maxGuests;
    if (occupiedSlots >= maxSlots) return "Phòng đã hết chỗ trong khoảng ngày đã chọn";
    return null;
  }
}
