package com.example.homestay

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val isAdminLoggedIn = getSharedPreferences("AdminSession", MODE_PRIVATE)
                .getBoolean("is_admin_logged_in", false)
            val destination = if (isAdminLoggedIn) {
                AdminDashboardActivity::class.java
            } else {
                MainActivity::class.java
            }
            startActivity(Intent(this, destination))
            finish()
        }, 1000)
    }
}


