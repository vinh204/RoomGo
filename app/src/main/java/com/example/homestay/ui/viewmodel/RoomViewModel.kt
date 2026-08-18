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

    private val _guestCount = MutableStateFlow(1)
    val guestCount: StateFlow<Int> = _guestCount.asStateFlow()

    private val _sortOrder = MutableStateFlow(RoomSort.RECOMMENDED)
    val sortOrder: StateFlow<RoomSort> = _sortOrder.asStateFlow()

    private val _maxPrice = MutableStateFlow(3_000_000.0)
    val maxPrice: StateFlow<Double> = _maxPrice.asStateFlow()

    private val _roomType = MutableStateFlow("Tất cả")
    val roomType: StateFlow<String> = _roomType.asStateFlow()

    private val dateRange = combine(_checkInDate, _checkOutDate) { checkIn, checkOut ->
        checkIn to checkOut
    }

    private val searchFilters = combine(
        _searchQuery, _guestCount, _maxPrice, _roomType
    ) { query, guests, maxPrice, roomType ->
        SearchFilters(query, guests, maxPrice, roomType)
    }

    val searchResults: Flow<List<Room>> = combine(
        repository.getAvailableRooms(),
        repository.getAllBookings(),
        searchFilters,
        combine(dateRange, _sortOrder) { dates, sort -> dates to sort }
    ) { rooms, bookings, filters, options ->
        val (dates, sort) = options
        val (checkIn, checkOut) = dates
        val keyword = filters.query.trim()
        val filtered = rooms.filter { room ->
            val matchesKeyword = keyword.isBlank() ||
                room.name.contains(keyword, ignoreCase = true) ||
                room.location.contains(keyword, ignoreCase = true) ||
                room.address.contains(keyword, ignoreCase = true)
            val matchesCapacity = room.maxGuests >= filters.guests
            val matchesPrice = room.price <= filters.maxPrice
            val matchesType = when (filters.roomType) {
                "Tất cả" -> true
                "Phòng" -> room.roomType.startsWith("Phòng", ignoreCase = true)
                else -> room.roomType.equals(filters.roomType, ignoreCase = true)
            }
            val hasAvailability = if (checkIn != null && checkOut != null) {
                bookings.count { booking ->
                    booking.roomId == room.id &&
                        booking.status in setOf("confirmed", "pending") &&
                        booking.checkInDate < checkOut && booking.checkOutDate > checkIn
                } < room.maxSlots
            } else true
            matchesKeyword && matchesCapacity && matchesPrice && matchesType && hasAvailability
        }
        when (sort) {
            RoomSort.RECOMMENDED -> filtered.sortedByDescending { it.rating }
            RoomSort.PRICE_LOW -> filtered.sortedBy { it.price }
            RoomSort.PRICE_HIGH -> filtered.sortedByDescending { it.price }
            RoomSort.RATING -> filtered.sortedByDescending { it.rating }
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
        if (_checkOutDate.value?.let { it <= timestamp } == true) {
            _checkOutDate.value = null
        }
    }

    fun setCheckOutDate(timestamp: Long) {
        _checkOutDate.value = timestamp
    }

    fun setGuestCount(count: Int) {
        _guestCount.value = count.coerceIn(1, 10)
    }

    fun cycleSortOrder() {
        _sortOrder.value = when (_sortOrder.value) {
            RoomSort.RECOMMENDED -> RoomSort.PRICE_LOW
            RoomSort.PRICE_LOW -> RoomSort.PRICE_HIGH
            RoomSort.PRICE_HIGH -> RoomSort.RATING
            RoomSort.RATING -> RoomSort.RECOMMENDED
        }
    }

    fun setMaxPrice(price: Double) {
        _maxPrice.value = price.coerceIn(300_000.0, 3_000_000.0)
    }

    fun setRoomType(type: String) {
        _roomType.value = type
    }

    fun clearSearchFilters() {
        _searchQuery.value = ""
        _checkInDate.value = null
        _checkOutDate.value = null
        _guestCount.value = 1
        _sortOrder.value = RoomSort.RECOMMENDED
        _maxPrice.value = 3_000_000.0
        _roomType.value = "Tất cả"
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

    fun deleteBooking(booking: Booking) {
        viewModelScope.launch { repository.deleteBooking(booking) }
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

enum class RoomSort(val label: String) {
    RECOMMENDED("Đề xuất"),
    PRICE_LOW("Giá thấp nhất"),
    PRICE_HIGH("Giá cao nhất"),
    RATING("Đánh giá tốt")
}

private data class SearchFilters(
    val query: String,
    val guests: Int,
    val maxPrice: Double,
    val roomType: String
)

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

