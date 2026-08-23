package com.example.homestay

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val isAdminLoggedIn = getSharedPreferences("AdminSession", MODE_PRIVATE)
            .getBoolean("is_admin_logged_in", false)
        val destination = if (isAdminLoggedIn) {
            AdminDashboardActivity::class.java
        } else {
            MainActivity::class.java
        }
        startActivity(Intent(this, destination))
        finish()
    }
}


