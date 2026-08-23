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
      "SELECT COUNT(*) FROM bookings WHERE roomId=:roomId AND status IN ('confirmed','pending') AND"
          + " checkInDate<:outDate AND checkOutDate>:inDate")
  int countOverlappingBookings(long roomId, long inDate, long outDate);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  long insertBooking(Booking b);

  @Update
  void updateBooking(Booking b);

  @Delete
  void deleteBooking(Booking b);

  @Query("DELETE FROM bookings")
  void deleteAllBookings();
}
