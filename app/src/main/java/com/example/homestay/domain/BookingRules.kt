package com.example.homestay.domain

object BookingRules {
    fun validate(
        checkInDate: Long,
        checkOutDate: Long,
        guestCount: Int,
        maxGuests: Int,
        occupiedSlots: Int,
        maxSlots: Int,
        todayStart: Long
    ): String? = when {
        checkInDate < todayStart -> "Ngày nhận phòng không được ở trong quá khứ"
        checkOutDate <= checkInDate -> "Ngày trả phòng phải sau ngày nhận phòng"
        guestCount !in 1..maxGuests -> "Số khách tối đa là $maxGuests"
        occupiedSlots >= maxSlots -> "Phòng đã hết chỗ trong khoảng ngày đã chọn"
        else -> null
    }
}
