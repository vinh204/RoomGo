package com.example.homestay.data.repository

import com.example.homestay.data.dao.BookingDao
import com.example.homestay.data.dao.RoomDao
import com.example.homestay.data.dao.UserDao
import com.example.homestay.data.entity.Booking
import com.example.homestay.data.model.CreateBookingRequest
import com.example.homestay.data.model.UpdateBookingRequest
import com.example.homestay.domain.BookingRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BookingRepository(
    private val bookingDao: BookingDao,
    private val roomDao: RoomDao,
    private val userDao: UserDao
) {
    suspend fun createBooking(
        mongoUserId: String,
        localUserId: Long,
        localRoomId: Long,
        mongoRoomId: String,
        request: CreateBookingRequest
    ): Result<BookingData> = withContext(Dispatchers.IO) {
        val room = roomDao.getRoomById(localRoomId) ?: return@withContext Result.failure(Exception("Không tìm thấy phòng"))
        val occupied = bookingDao.countOverlappingBookings(localRoomId, request.checkInDate, request.checkOutDate)
        BookingRules.validate(request.checkInDate, request.checkOutDate, request.guestCount, room.maxGuests,
            occupied, room.maxSlots, startOfToday())?.let { return@withContext Result.failure(Exception(it)) }

        val booking = Booking(roomId = localRoomId, userId = localUserId, checkInDate = request.checkInDate,
            checkOutDate = request.checkOutDate, guestCount = request.guestCount, totalPrice = request.totalPrice,
            status = "pending", paymentMethod = request.paymentMethod)
        val id = bookingDao.insertBooking(booking)
        Result.success(BookingData(booking.copy(id = id), id.toString()))
    }

    fun getBookingsByUserId(mongoUserId: String, localUserId: Long): Flow<List<Booking>> = bookingDao.getBookingsByUserId(localUserId)

    suspend fun updateBooking(mongoBookingId: String, localBookingId: Long, request: UpdateBookingRequest): Result<BookingData> = withContext(Dispatchers.IO) {
        val current = bookingDao.getBookingById(localBookingId) ?: return@withContext Result.failure(Exception("Không tìm thấy booking"))
        val updated = current.copy(status = request.status ?: current.status, paymentMethod = request.paymentMethod ?: current.paymentMethod)
        bookingDao.updateBooking(updated)
        Result.success(BookingData(updated, localBookingId.toString()))
    }

    suspend fun syncBookingsFromAPI(mongoUserId: String, localUserId: Long) = true
    suspend fun countOverlappingBookings(roomId: Long, checkInDate: Long, checkOutDate: Long) =
        withContext(Dispatchers.IO) { bookingDao.countOverlappingBookings(roomId, checkInDate, checkOutDate) }

    private fun startOfToday(): Long = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
}

data class BookingData(val booking: Booking, val mongoBookingId: String)
