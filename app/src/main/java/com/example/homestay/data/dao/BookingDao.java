package com.example.homestay.data.dao;

import androidx.room.*;
import com.example.homestay.data.entity.Booking;
import java.util.List;

@Dao
public interface BookingDao {
  @Query("SELECT * FROM bookings ORDER BY createdAt DESC")
  List<Booking> getAllBookingsNow();

  @Query("SELECT * FROM bookings WHERE userId=:userId ORDER BY createdAt DESC")
  List<Booking> getBookingsByUserIdNow(long userId);

  @Query("SELECT * FROM bookings WHERE id=:id")
  Booking getBookingById(long id);

  @Query("SELECT COUNT(*) FROM bookings WHERE roomId=:id")
  int countByRoomId(long id);

  @Query("SELECT COUNT(*) FROM bookings WHERE userId=:id")
  int countByUserId(long id);

  @Query("SELECT * FROM bookings WHERE mongoId=:id")
  Booking getBookingByMongoId(String id);

  @Query(
      "SELECT COUNT(*) FROM bookings WHERE roomId=:roomId AND status IN ('confirmed','pending','checked_in') AND"
          + " checkInDate<:outDate AND checkOutDate>:inDate")
  int countOverlappingBookings(long roomId, long inDate, long outDate);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  long insertBooking(Booking b);

  /** Kiểm tra số lượng và thêm booking trong cùng một transaction. */
  @Transaction
  default long insertIfCapacityAvailable(Booking booking, int maxSlots) {
    expirePendingBookings(System.currentTimeMillis());
    int occupied =
        countOverlappingBookings(
            booking.getRoomId(), booking.getCheckInDate(), booking.getCheckOutDate());
    if (occupied >= maxSlots) return -1L;
    return insertBooking(booking);
  }

  @Update
  void updateBooking(Booking b);

  @Delete
  void deleteBooking(Booking b);

  @Query("DELETE FROM bookings")
  void deleteAllBookings();

  @Query("UPDATE bookings SET status='expired' WHERE status='pending' AND expiresAt>0 AND expiresAt<=:now")
  int expirePendingBookings(long now);

  @Query("SELECT * FROM bookings WHERE (status='confirmed' AND checkInDate<=:now) OR (status='checked_in' AND checkOutDate<=:now) ORDER BY checkOutDate")
  List<Booking> getBookingsNeedingLifecycleUpdate(long now);

  @Query("UPDATE bookings SET status='checked_in' WHERE id=:bookingId AND status='confirmed' AND checkInDate<=:now AND checkOutDate>:now")
  int checkInConfirmedBookingIfDue(long bookingId, long now);

  @Query("UPDATE bookings SET status='completed' WHERE id=:bookingId AND status IN ('confirmed','checked_in') AND checkOutDate<=:now")
  int completeConfirmedBookingIfDue(long bookingId, long now);
}
