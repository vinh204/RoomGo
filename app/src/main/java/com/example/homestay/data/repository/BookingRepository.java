package com.example.homestay.data.repository;

import com.example.homestay.data.dao.*;
import com.example.homestay.data.entity.*;
import com.example.homestay.data.model.*;
import com.example.homestay.domain.BookingCalculator;
import com.example.homestay.domain.BookingRules;
import com.example.homestay.domain.BookingStatusPolicy;
import java.util.Calendar;

public final class BookingRepository {
  private static final long PENDING_HOLD_MS = 2L * 60 * 60 * 1000;
  private final BookingDao bookingDao;
  private final RoomDao roomDao;
  private final SlotDao slotDao;

  public BookingRepository(BookingDao b, RoomDao r, SlotDao s) {
    bookingDao = b;
    roomDao = r;
    slotDao = s;
  }

  public OperationResult<BookingData> createBooking(
      String mongoUserId,
      long localUserId,
      long localRoomId,
      String mongoRoomId,
      CreateBookingRequest request) {
    Room room = roomDao.getRoomById(localRoomId);
    if (room == null) return OperationResult.failure(new Exception("Không tìm thấy phòng"));
    if (!room.isAvailable())
      return OperationResult.failure(new Exception("Phòng đang tạm ngưng nhận đặt chỗ"));
    String error =
        BookingRules.validate(
            request.getCheckInDate(),
            request.getCheckOutDate(),
            request.getGuestCount(),
            room.getMaxGuests(),
            0,
            room.getMaxSlots(),
            System.currentTimeMillis());
    if (error != null) return OperationResult.failure(new Exception(error));
    Long slot = null;
    try {
      if (request.getSlotId() != null) slot = Long.parseLong(request.getSlotId());
    } catch (NumberFormatException ignored) {
    }
    Slot selectedSlot = slot == null ? null : slotDao.getSlotById(slot);
    if (selectedSlot != null && selectedSlot.getRoomId() != localRoomId)
      return OperationResult.failure(new Exception("Lựa chọn phòng không hợp lệ"));
    if (selectedSlot != null && !selectedSlot.isAvailable())
      return OperationResult.failure(new Exception("Lựa chọn phòng hiện không còn hoạt động"));
    double totalPrice =
        BookingCalculator.total(
            room, selectedSlot, request.getCheckInDate(), request.getCheckOutDate());
    if (totalPrice <= 0)
      return OperationResult.failure(new Exception("Không thể tính tổng tiền đặt phòng"));
    if (Math.abs(totalPrice - request.getTotalPrice()) >= 1d)
      return OperationResult.failure(
          new Exception("Giá phòng vừa thay đổi. Vui lòng mở lại thông tin đặt phòng"));
    long createdAt = System.currentTimeMillis();
    Booking value =
        new Booking(
            0,
            null,
            localRoomId,
            mongoRoomId,
            slot,
            null,
            localUserId,
            mongoUserId,
            request.getCheckInDate(),
            request.getCheckOutDate(),
            request.getGuestCount(),
            totalPrice,
            "pending",
            request.getPaymentMethod(),
            createdAt,
            null,
            0,
            0,
            "UNPAID",
            createdAt + PENDING_HOLD_MS);
    long id = bookingDao.insertIfCapacityAvailable(value, room.getMaxSlots());
    if (id < 0)
      return OperationResult.failure(new Exception("Phòng đã hết chỗ trong khoảng ngày đã chọn"));
    return OperationResult.success(new BookingData(value.withId(id), String.valueOf(id)));
  }

  public OperationResult<BookingData> updateBooking(
      String mongoBookingId, long id, UpdateBookingRequest request) {
    bookingDao.expirePendingBookings(System.currentTimeMillis());
    Booking current = bookingDao.getBookingById(id);
    if (current == null) return OperationResult.failure(new Exception("Không tìm thấy booking"));
    String nextStatus = request.getStatus() == null ? current.getStatus() : request.getStatus();
    if (!BookingStatusPolicy.canTransition(current.getStatus(), nextStatus))
      return OperationResult.failure(new Exception("Chuyển trạng thái booking không hợp lệ"));
    Booking updated =
        current.withStatus(
            nextStatus,
            request.getPaymentMethod() == null
                ? current.getPaymentMethod()
                : request.getPaymentMethod());
    bookingDao.updateBooking(updated);
    return OperationResult.success(new BookingData(updated, String.valueOf(id)));
  }

  public boolean syncBookingsFromAPI(String mongoUserId, long localUserId) {
    return true;
  }

  public int countOverlappingBookings(long room, long in, long out) {
    return bookingDao.countOverlappingBookings(room, in, out);
  }

  public static final class BookingData {
    private final Booking booking;
    private final String mongoBookingId;

    public BookingData(Booking b, String id) {
      booking = b;
      mongoBookingId = id;
    }

    public Booking getBooking() {
      return booking;
    }

    public String getMongoBookingId() {
      return mongoBookingId;
    }
  }
}
