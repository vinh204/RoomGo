package com.example.homestay

import org.junit.Test

import org.junit.Assert.*
import com.example.homestay.domain.BookingRules

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    private val today = 1_000_000L

    @Test
    fun validBookingIsAccepted() = assertNull(BookingRules.validate(today, today + 86_400_000, 2, 4, 0, 2, today))

    @Test
    fun pastCheckInIsRejected() = assertNotNull(BookingRules.validate(today - 1, today + 1, 1, 2, 0, 1, today))

    @Test
    fun checkoutBeforeCheckinIsRejected() = assertNotNull(BookingRules.validate(today, today, 1, 2, 0, 1, today))

    @Test
    fun tooManyGuestsAreRejected() = assertNotNull(BookingRules.validate(today, today + 1, 5, 4, 0, 1, today))

    @Test
    fun fullRoomIsRejected() = assertNotNull(BookingRules.validate(today, today + 1, 2, 4, 2, 2, today))
}
