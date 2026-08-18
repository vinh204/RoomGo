package com.example.homestay

import android.app.Application
import com.example.homestay.data.database.HomestayDatabase
import com.example.homestay.data.repository.AuthRepository
import com.example.homestay.data.repository.BookingRepository
import com.example.homestay.data.repository.HomestayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HomestayApplication : Application() {
    val database by lazy { HomestayDatabase.getDatabase(this) }
    
    // HomestayRepository - cho rooms, bookings, favorites (local data)
    val repository by lazy {
        HomestayRepository(
            database.roomDao(),
            database.slotDao(),
            database.bookingDao(),
            database.userDao(),
            database.favoriteDao()
        )
    }
    
    // AuthRepository - cho authentication (gọi backend API)
    val authRepository by lazy {
        AuthRepository(database.userDao())
    }
    
    // BookingRepository - cho booking (gọi backend API)
    val bookingRepository by lazy {
        BookingRepository(
            database.bookingDao(),
            database.roomDao(),
            database.userDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val demoPrefs = getSharedPreferences("DemoData", MODE_PRIVATE)
            if (!demoPrefs.getBoolean("rooms_seeded", false)) {
                repository.seedLocalRoomsIfNeeded()
                demoPrefs.edit().putBoolean("rooms_seeded", true).apply()
            }
        }
        // Seed once; subsequent launches keep the user's Room data unchanged.
    }
}

