package com.example.homestay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.homestay.data.model.CreateBookingRequest
import com.example.homestay.data.entity.Booking
import com.example.homestay.data.entity.Favorite
import com.example.homestay.data.entity.Room
import com.example.homestay.data.entity.Slot
import com.example.homestay.data.repository.BookingRepository
import com.example.homestay.data.repository.HomestayRepository
import com.example.homestay.data.repository.BookingData
import com.example.homestay.ui.adapter.BookingWithRoom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RoomViewModel(
    private val repository: HomestayRepository,
    private val bookingRepository: BookingRepository? = null
) : ViewModel() {
    val allRooms: Flow<List<Room>> = repository.getAllRooms()
    val availableRooms: Flow<List<Room>> = repository.getAvailableRooms()

    private val _searchQuery = MutableStateFlow<String>("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _checkInDate = MutableStateFlow<Long?>(null)
    val checkInDate: StateFlow<Long?> = _checkInDate.asStateFlow()

    private val _checkOutDate = MutableStateFlow<Long?>(null)
    val checkOutDate: StateFlow<Long?> = _checkOutDate.asStateFlow()

    val searchResults: Flow<List<Room>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.getAvailableRooms()
        } else {
            repository.searchRooms(query)
        }
    }

    private val _selectedRoom = MutableStateFlow<Room?>(null)
    val selectedRoom: StateFlow<Room?> = _selectedRoom.asStateFlow()

    fun getRoomById(roomId: Long): Flow<Room?> = repository.getRoomByIdFlow(roomId)

    fun getSlotsByRoomId(roomId: Long): Flow<List<Slot>> = repository.getSlotsByRoomId(roomId)
    
    fun getAvailableSlotsByRoomId(roomId: Long): Flow<List<Slot>> = repository.getAvailableSlotsByRoomId(roomId)

    fun selectRoom(room: Room) {
        _selectedRoom.value = room
    }

    fun insertRoom(room: Room) {
        viewModelScope.launch {
            repository.insertRoom(room)
        }
    }

    fun insertRooms(rooms: List<Room>) {
        viewModelScope.launch {
            repository.insertRooms(rooms)
        }
    }

    fun updateRoom(room: Room) {
        viewModelScope.launch {
            repository.updateRoom(room)
        }
    }

    fun deleteRoom(room: Room) {
        viewModelScope.launch {
            repository.deleteRoom(room)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCheckInDate(timestamp: Long) {
        _checkInDate.value = timestamp
    }

    fun setCheckOutDate(timestamp: Long) {
        _checkOutDate.value = timestamp
    }

    // Favorite operations
    fun toggleFavorite(userId: Long, roomId: Long) {
        viewModelScope.launch {
            val isFavorite = repository.isFavorite(userId, roomId)
            if (isFavorite) {
                repository.deleteFavorite(userId, roomId)
            } else {
                repository.insertFavorite(Favorite(userId = userId, roomId = roomId))
            }
        }
    }

    suspend fun isFavoriteSync(userId: Long, roomId: Long): Boolean {
        return repository.isFavorite(userId, roomId)
    }

    fun getFavoriteRooms(userId: Long): Flow<List<Room>> {
        return combine(
            repository.getFavoriteRoomIds(userId),
            repository.getAllRooms()
        ) { favoriteRoomIds, allRooms ->
            allRooms.filter { it.id in favoriteRoomIds }
        }
    }

    // Booking operations
    suspend fun insertBooking(booking: Booking): Long = repository.insertBooking(booking)
    
    /**
     * Create booking via API (MongoDB)
     * @param mongoUserId MongoDB User ID
     * @param localUserId Local User ID
     * @param localRoomId Local Room ID
     * @param mongoRoomId MongoDB Room ID (cần từ Room entity hoặc mapping)
     * @param request CreateBookingRequest
     * @return Result<BookingData>
     */
    suspend fun createBookingViaAPI(
        mongoUserId: String,
        localUserId: Long,
        localRoomId: Long,
        mongoRoomId: String,
        request: CreateBookingRequest
    ): Result<BookingData> {
        val repo = bookingRepository
        return if (repo != null) {
            repo.createBooking(
                mongoUserId,
                localUserId,
                localRoomId,
                mongoRoomId,
                request
            )
        } else {
            Result.failure(Exception("BookingRepository not initialized"))
        }
    }
    
    fun updateBooking(booking: Booking) {
        viewModelScope.launch {
            repository.updateBooking(booking)
        }
    }

    fun getBookingsByUserId(userId: Long): Flow<List<Booking>> = repository.getBookingsByUserId(userId)
    
    /**
     * Get bookings by user ID via API (MongoDB)
     * @param mongoUserId MongoDB User ID
     * @param localUserId Local User ID
     * @return Flow<List<Booking>>
     */
    fun getBookingsByUserIdViaAPI(mongoUserId: String, localUserId: Long): Flow<List<Booking>> {
        val repo = bookingRepository
        return if (repo != null) {
            repo.getBookingsByUserId(mongoUserId, localUserId)
        } else {
            repository.getBookingsByUserId(localUserId)
        }
    }
    
    fun getBookingsWithRoomInfo(userId: Long): Flow<List<BookingWithRoom>> {
        return combine(
            repository.getBookingsByUserId(userId),
            repository.getAllRooms()
        ) { bookings, allRooms ->
            // Map tất cả bookings (bao gồm cả completed) với room info
            // Sắp xếp: completed ở cuối, các status khác theo thứ tự thời gian
            bookings.map { booking ->
                val room = allRooms.find { it.id == booking.roomId }
                BookingWithRoom(booking, room)
            }.sortedWith(compareBy(
                { it.booking.status == "completed" }, // completed ở cuối
                { -it.booking.createdAt } // Các status khác: mới nhất trước
            ))
        }
    }
    
    /**
     * Get bookings with room info via API (MongoDB)
     * Bao gồm TẤT CẢ bookings của user: pending, confirmed, cancelled, và completed
     * @param mongoUserId MongoDB User ID
     * @param localUserId Local User ID
     * @return Flow<List<BookingWithRoom>> - Tất cả bookings bao gồm cả completed
     */
    fun getBookingsWithRoomInfoViaAPI(mongoUserId: String, localUserId: Long): Flow<List<BookingWithRoom>> {
        return combine(
            getBookingsByUserIdViaAPI(mongoUserId, localUserId),
            repository.getAllRooms()
        ) { bookings, allRooms ->
            // Map tất cả bookings (bao gồm cả completed) với room info
            // Sắp xếp: completed ở cuối, các status khác theo thứ tự thời gian
            bookings.map { booking ->
                val room = allRooms.find { it.id == booking.roomId }
                BookingWithRoom(booking, room)
            }.sortedWith(compareBy(
                { it.booking.status == "completed" }, // completed ở cuối
                { -it.booking.createdAt } // Các status khác: mới nhất trước
            ))
        }
    }

    suspend fun checkSlotAvailability(roomId: Long, checkInDate: Long, checkOutDate: Long): Boolean {
        val room = repository.getRoomById(roomId) ?: return false
        val overlappingCount = repository.countOverlappingBookings(roomId, checkInDate, checkOutDate)
        return overlappingCount < room.maxSlots
    }

    suspend fun getAvailableSlotCount(roomId: Long, checkInDate: Long, checkOutDate: Long): Int {
        val room = repository.getRoomById(roomId) ?: return 0
        val overlappingCount = repository.countOverlappingBookings(roomId, checkInDate, checkOutDate)
        return (room.maxSlots - overlappingCount).coerceAtLeast(0)
    }
}

class RoomViewModelFactory(
    private val repository: HomestayRepository,
    private val bookingRepository: BookingRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoomViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoomViewModel(repository, bookingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

