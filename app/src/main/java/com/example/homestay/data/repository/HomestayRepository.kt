package com.example.homestay.data.repository

import android.util.Log
import com.example.homestay.data.dao.BookingDao
import com.example.homestay.data.dao.FavoriteDao
import com.example.homestay.data.dao.RoomDao
import com.example.homestay.data.dao.SlotDao
import com.example.homestay.data.dao.UserDao
import com.example.homestay.data.entity.Booking
import com.example.homestay.data.entity.Favorite
import com.example.homestay.data.entity.Room
import com.example.homestay.data.entity.Slot
import com.example.homestay.data.entity.User
import com.example.homestay.utils.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class HomestayRepository(
    private val roomDao: RoomDao,
    private val slotDao: SlotDao,
    private val bookingDao: BookingDao,
    private val userDao: UserDao,
    private val favoriteDao: FavoriteDao
) {
    // Room operations
    fun getAllRooms(): Flow<List<Room>> = roomDao.getAllRooms()
    fun getAvailableRooms(): Flow<List<Room>> = roomDao.getAvailableRooms()
    fun searchRoomsByName(query: String): Flow<List<Room>> = roomDao.searchRoomsByName(query)
    fun searchRooms(query: String): Flow<List<Room>> = roomDao.searchRooms(query)
    suspend fun getRoomById(roomId: Long): Room? = roomDao.getRoomById(roomId)
    fun getRoomByIdFlow(roomId: Long): Flow<Room?> = roomDao.getRoomByIdFlow(roomId)
    suspend fun insertRoom(room: Room): Long = roomDao.insertRoom(room)
    suspend fun insertRooms(rooms: List<Room>) = roomDao.insertRooms(rooms)
    suspend fun updateRoom(room: Room) = roomDao.updateRoom(room)
    suspend fun deleteRoom(room: Room) {
        favoriteDao.deleteByRoomId(room.id)
        slotDao.deleteByRoomId(room.id)
        roomDao.deleteRoom(room)
    }

    // Slot operations
    fun getSlotsByRoomId(roomId: Long): Flow<List<Slot>> = slotDao.getSlotsByRoomId(roomId)
    fun getAvailableSlotsByRoomId(roomId: Long): Flow<List<Slot>> = slotDao.getAvailableSlotsByRoomId(roomId)
    suspend fun getSlotById(slotId: Long): Slot? = slotDao.getSlotById(slotId)
    suspend fun insertSlot(slot: Slot): Long = slotDao.insertSlot(slot)
    suspend fun insertSlots(slots: List<Slot>) = slotDao.insertSlots(slots)
    suspend fun updateSlot(slot: Slot) = slotDao.updateSlot(slot)
    suspend fun deleteSlot(slot: Slot) = slotDao.deleteSlot(slot)

    // Booking operations
    fun getAllBookings(): Flow<List<Booking>> = bookingDao.getAllBookings()
    fun getBookingsByUserId(userId: Long): Flow<List<Booking>> = bookingDao.getBookingsByUserId(userId)
    fun getBookingsByRoomId(roomId: Long): Flow<List<Booking>> = bookingDao.getBookingsByRoomId(roomId)
    suspend fun getBookingById(bookingId: Long): Booking? = bookingDao.getBookingById(bookingId)
    suspend fun insertBooking(booking: Booking): Long = bookingDao.insertBooking(booking)
    suspend fun updateBooking(booking: Booking) = bookingDao.updateBooking(booking)
    suspend fun deleteBooking(booking: Booking) = bookingDao.deleteBooking(booking)
    suspend fun countOverlappingBookings(roomId: Long, checkInDate: Long, checkOutDate: Long): Int =
        bookingDao.countOverlappingBookings(roomId, checkInDate, checkOutDate)

    // User operations
    /**
     * Login với BCrypt password verification
     * Mã hóa mật khẩu
     */
    suspend fun login(email: String, password: String): User? {
        val user = userDao.getUserByEmailForLogin(email) ?: return null
        // Verify password bằng BCrypt
        if (PasswordHasher.verify(password, user.password)) {
            return user
        }
        return null
    }
    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)
    suspend fun getUserByPhone(phone: String): User? = userDao.getUserByPhone(phone)
    suspend fun getUserById(userId: Long): User? = userDao.getUserById(userId)
    fun getUserByIdFlow(userId: Long): Flow<User?> = userDao.getUserByIdFlow(userId)
    /**
     * Insert user với password đã được hash bằng BCrypt
     * Tính năng 1: Mã hóa mật khẩu
     */
    suspend fun insertUser(user: User): Long {
        // Hash password trước khi lưu
        val hashedPassword = PasswordHasher.hash(user.password)
        val userWithHashedPassword = user.copy(password = hashedPassword)
        return userDao.insertUser(userWithHashedPassword)
    }
    /**
     * Update user - hash password mới nếu được cung cấp
     * Tính năng 1: Mã hóa mật khẩu
     */
    suspend fun updateUser(user: User) {
        val existingUser = userDao.getUserById(user.id)
        if (existingUser != null) {
            // Nếu password mới được cung cấp và khác với password cũ, hash nó
            val updatedPassword = if (user.password != existingUser.password && 
                !PasswordHasher.isValidHash(user.password)) {
                PasswordHasher.hash(user.password)
            } else {
                user.password // Giữ nguyên nếu đã là hash hoặc không đổi
            }
            val updatedUser = user.copy(password = updatedPassword)
            userDao.updateUser(updatedUser)
        } else {
            userDao.updateUser(user)
        }
    }
    suspend fun deleteUser(user: User) {
        favoriteDao.deleteByUserId(user.id)
        userDao.deleteUser(user)
    }
    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    // Favorite operations
    suspend fun getFavorite(userId: Long, roomId: Long): Favorite? = favoriteDao.getFavorite(userId, roomId)
    fun getFavoriteRoomIds(userId: Long): Flow<List<Long>> = favoriteDao.getFavoriteRoomIds(userId)
    fun getFavoritesByUserId(userId: Long): Flow<List<Favorite>> = favoriteDao.getFavoritesByUserId(userId)
    suspend fun insertFavorite(favorite: Favorite): Long = favoriteDao.insertFavorite(favorite)
    suspend fun deleteFavorite(favorite: Favorite) = favoriteDao.deleteFavorite(favorite)
    suspend fun deleteFavorite(userId: Long, roomId: Long) = favoriteDao.deleteFavorite(userId, roomId)
    suspend fun isFavorite(userId: Long, roomId: Long): Boolean = favoriteDao.isFavorite(userId, roomId) > 0
    
    /**
     * Clean up duplicate rooms - xóa các rooms duplicate dựa trên mongoId
     */
    suspend fun cleanupDuplicateRooms() {
        try {
            val allRooms = getAllRooms().first()
            val roomsByMongoId = allRooms.filter { it.mongoId != null }
                .groupBy { it.mongoId }
            
            var duplicateCount = 0
            for ((mongoId, duplicateRooms) in roomsByMongoId) {
                if (duplicateRooms.size > 1) {
                    // Giữ lại room đầu tiên (id nhỏ nhất), xóa các rooms còn lại
                    val sortedRooms = duplicateRooms.sortedBy { it.id }
                    val roomsToDelete = sortedRooms.drop(1)
                    duplicateCount += roomsToDelete.size
                    for (room in roomsToDelete) {
                        deleteRoom(room)
                        Log.d("HomestayRepository", "Deleted duplicate room: id=${room.id}, mongoId=$mongoId, name=${room.name}")
                    }
                }
            }
            if (duplicateCount > 0) {
                Log.d("HomestayRepository", "Cleaned up $duplicateCount duplicate rooms")
            }
        } catch (e: Exception) {
            Log.e("HomestayRepository", "Error cleaning up duplicate rooms: ${e.message}", e)
        }
    }
    
    suspend fun syncRoomsFromAPI(): Boolean {
        return true
    }

    suspend fun roomHasBookings(roomId: Long) = bookingDao.countByRoomId(roomId) > 0
    suspend fun userHasBookings(userId: Long) = bookingDao.countByUserId(userId) > 0

    suspend fun seedLocalRoomsIfNeeded() {
        if (getAllRooms().first().isNotEmpty()) return

        insertRooms(
            listOf(
                Room(mongoId = "local-room-1", name = "Homestay Đà Lạt View", description = "Phòng ấm cúng, ban công nhìn ra đồi thông.", price = 650000.0, imageUrl = "android.resource://com.example.homestay/drawable/room_dalat", location = "Đà Lạt", address = "12 Trần Hưng Đạo, Đà Lạt", rating = 4.8f, reviewCount = 128, amenities = "WiFi, Bếp, Ban công", maxGuests = 2, roomType = "Phòng đôi", area = 28, maxSlots = 2),
                Room(mongoId = "local-room-2", name = "Nhà Gỗ Bên Hồ", description = "Căn nhà gỗ yên tĩnh phù hợp cho gia đình.", price = 1200000.0, imageUrl = "android.resource://com.example.homestay/drawable/room_dalat", location = "Bảo Lộc", address = "Hồ Nam Phương, Bảo Lộc", rating = 4.7f, reviewCount = 86, amenities = "WiFi, Bếp, Bãi đỗ xe", maxGuests = 5, roomType = "Nhà nguyên căn", area = 65, maxSlots = 1),
                Room(mongoId = "local-room-3", name = "Studio Trung Tâm", description = "Studio hiện đại, gần chợ và khu ăn uống.", price = 520000.0, imageUrl = "android.resource://com.example.homestay/drawable/room_studio", location = "Đà Nẵng", address = "45 Nguyễn Văn Linh, Đà Nẵng", rating = 4.6f, reviewCount = 74, amenities = "WiFi, Điều hòa, TV", maxGuests = 2, roomType = "Studio", area = 24, maxSlots = 3),
                Room(mongoId = "local-room-4", name = "Villa Biển Xanh", description = "Villa rộng rãi gần biển, có hồ bơi riêng.", price = 2800000.0, imageUrl = "android.resource://com.example.homestay/drawable/room_beach", location = "Vũng Tàu", address = "18 Hạ Long, Vũng Tàu", rating = 4.9f, reviewCount = 203, amenities = "Hồ bơi, WiFi, BBQ", maxGuests = 10, roomType = "Villa", area = 180, maxSlots = 1),
                Room(mongoId = "local-room-5", name = "Phòng Phố Cổ", description = "Không gian nhỏ gọn ngay trung tâm phố cổ.", price = 780000.0, imageUrl = "android.resource://com.example.homestay/drawable/room_studio", location = "Hà Nội", address = "26 Hàng Bạc, Hoàn Kiếm", rating = 4.5f, reviewCount = 156, amenities = "WiFi, Điều hòa, Máy sấy", maxGuests = 3, roomType = "Phòng gia đình", area = 32, maxSlots = 2)
            )
        )
    }
}

