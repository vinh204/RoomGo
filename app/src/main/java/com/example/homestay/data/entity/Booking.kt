package com.example.homestay.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookings",
    indices = [Index(value = ["roomId"]), Index(value = ["slotId"]), Index(value = ["userId"]), Index(value = ["mongoId"])]
)
data class Booking(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mongoId: String? = null, // MongoDB ObjectId - để sync với backend
    val roomId: Long,
    val mongoRoomId: String? = null, // MongoDB Room ObjectId
    val slotId: Long? = null, // Có thể null nếu đặt cả phòng
    val mongoSlotId: String? = null, // MongoDB Slot ObjectId
    val userId: Long, // User ID (local)
    val mongoUserId: String? = null, // MongoDB User ObjectId
    val checkInDate: Long, // Timestamp
    val checkOutDate: Long, // Timestamp
    val guestCount: Int = 1,
    val totalPrice: Double,
    val status: String = "pending", // "pending", "confirmed", "cancelled", "completed"
    val paymentMethod: String? = null, // "qr_code" or "pay_on_site"
    val createdAt: Long = System.currentTimeMillis()
)

