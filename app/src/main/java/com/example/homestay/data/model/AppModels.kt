package com.example.homestay.data.model

data class CreateBookingRequest(
    val roomId: String,
    val checkInDate: Long,
    val checkOutDate: Long,
    val guestCount: Int,
    val totalPrice: Double,
    val status: String = "pending",
    val paymentMethod: String? = null,
    val slotId: String? = null
)

data class UpdateBookingRequest(val status: String? = null, val paymentMethod: String? = null)

data class AdminRoomData(
    val id: String, val name: String, val description: String, val price: Double,
    val capacity: Int, val imageUrl: String, val maxSlots: Int, val createdAt: Long,
    val location: String = "", val address: String = "", val amenities: String = "",
    val roomType: String = "Homestay", val area: Int = 0, val rating: Float = 0f,
    val reviewCount: Int = 0, val isAvailable: Boolean = true, val bookingCount: Int = 0,
    val revenue: Double = 0.0
)
data class AdminUserData(
    val id: String, val email: String, val phone: String, val fullName: String,
    val createdAt: Long, val failedLoginAttempts: Int? = null, val locked: Boolean? = null,
    val permanent: Boolean? = null, val lockedUntil: Long? = null,
    val secondsRemaining: Long? = null, val bookingCount: Int = 0,
    val totalSpent: Double = 0.0, val lastBookingAt: Long? = null
)
data class AdminBookingUser(val id: String, val email: String, val fullName: String, val phone: String)
data class AdminBookingRoom(val id: String, val name: String, val price: Double)
data class AdminBookingData(val id: String, val user: AdminBookingUser?, val room: AdminBookingRoom?, val checkInDate: Long, val checkOutDate: Long, val guestCount: Int, val totalPrice: Double, val status: String, val paymentMethod: String?, val createdAt: Long, val slotId: String? = null)
