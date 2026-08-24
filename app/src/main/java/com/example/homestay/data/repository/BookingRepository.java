package com.example.homestay.data.repository;

import com.example.homestay.data.dao.*;
import com.example.homestay.data.entity.*;
import com.example.homestay.data.model.*;
import com.example.homestay.domain.BookingRules;
import java.util.Calendar;

public final class BookingRepository {
  private final BookingDao bookingDao;
  private final RoomDao roomDao;

  public BookingRepository(BookingDao b, RoomDao r, UserDao u) {
    bookingDao = b;
    roomDao = r;
  }

  public OperationResult<BookingData> createBooking(
      String mongoUserId,
      long localUserId,
      long localRoomId,
      String mongoRoomId,
      CreateBookingRequest request) {
    Room room = roomDao.getRoomById(localRoomId);
    if (room == null) return OperationResult.failure(new Exception("Không tìm thấy phòng"));
    int occupied =
        bookingDao.countOverlappingBookings(
            localRoomId, request.getCheckInDate(), request.getCheckOutDate());
    String error =
        BookingRules.validate(
            request.getCheckInDate(),
            request.getCheckOutDate(),
            request.getGuestCount(),
            room.getMaxGuests(),
            occupied,
            room.getMaxSlots(),
            startOfToday());
    if (error != null) return OperationResult.failure(new Exception(error));
    Long slot = null;
    try {
      if (request.getSlotId() != null) slot = Long.parseLong(request.getSlotId());
    } catch (NumberFormatException ignored) {
    }
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
            request.getTotalPrice(),
            "pending",
            request.getPaymentMethod(),
            System.currentTimeMillis(),
            null,
            0,
            0);
    long id = bookingDao.insertBooking(value);
    return OperationResult.success(new BookingData(value.withId(id), String.valueOf(id)));
  }

  public OperationResult<BookingData> updateBooking(
      String mongoBookingId, long id, UpdateBookingRequest request) {
    Booking current = bookingDao.getBookingById(id);
    if (current == null) return OperationResult.failure(new Exception("Không tìm thấy booking"));
    Booking updated =
        current.withStatus(
            request.getStatus() == null ? current.getStatus() : request.getStatus(),
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

  private long startOfToday() {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTimeInMillis();
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
