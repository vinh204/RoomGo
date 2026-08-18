package com.example.homestay.data.dao

import androidx.room.*
import com.example.homestay.data.entity.Booking
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY createdAt DESC")
    fun getAllBookings(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE userId = :userId ORDER BY createdAt DESC")
    fun getBookingsByUserId(userId: Long): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE roomId = :roomId ORDER BY createdAt DESC")
    fun getBookingsByRoomId(roomId: Long): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE id = :bookingId")
    suspend fun getBookingById(bookingId: Long): Booking?

    @Query("SELECT COUNT(*) FROM bookings WHERE roomId = :roomId")
    suspend fun countByRoomId(roomId: Long): Int

    @Query("SELECT COUNT(*) FROM bookings WHERE userId = :userId")
    suspend fun countByUserId(userId: Long): Int

    @Query("SELECT * FROM bookings WHERE mongoId = :mongoId")
    suspend fun getBookingByMongoId(mongoId: String): Booking?

    // Đếm số booking overlapping với khoảng thời gian checkIn-checkOut
    // Chỉ đếm booking có status = "confirmed" hoặc "pending" (không đếm cancelled, completed)
    @Query("""
        SELECT COUNT(*) FROM bookings 
        WHERE roomId = :roomId 
        AND status IN ('confirmed', 'pending')
        AND checkInDate < :checkOutDate 
        AND checkOutDate > :checkInDate
    """)
    suspend fun countOverlappingBookings(
        roomId: Long,
        checkInDate: Long,
        checkOutDate: Long
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking): Long

    @Update
    suspend fun updateBooking(booking: Booking)

    @Delete
    suspend fun deleteBooking(booking: Booking)

    @Query("DELETE FROM bookings")
    suspend fun deleteAllBookings()
}

