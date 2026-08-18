package com.example.homestay

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class AdminDashboardActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)
        
        // Check if admin logged in
        if (!isAdminLoggedIn()) {
            finish()
            return
        }
        
        setupViews()
    }
    
    private fun setupViews() {
        val tvAdminName = findViewById<TextView>(R.id.tv_admin_name)
        val cardManageRooms = findViewById<MaterialCardView>(R.id.card_manage_rooms)
        val cardViewUsers = findViewById<MaterialCardView>(R.id.card_view_users)
        val cardManageBookings = findViewById<MaterialCardView>(R.id.card_manage_bookings)
        val btnLogout = findViewById<MaterialButton>(R.id.btn_logout)
        
        // Load admin name
        val prefs = getSharedPreferences("AdminSession", MODE_PRIVATE)
        val adminName = prefs.getString("admin_fullname", "Administrator")
        tvAdminName.text = adminName
        
        // Quản lý Phòng
        cardManageRooms.setOnClickListener {
            startActivity(Intent(this, AdminRoomsActivity::class.java))
        }
        
        // Xem Users
        cardViewUsers.setOnClickListener {
            startActivity(Intent(this, AdminUsersActivity::class.java))
        }
        
        // Quản lý Bookings
        cardManageBookings.setOnClickListener {
            startActivity(Intent(this, AdminBookingsActivity::class.java))
        }
        
        // Logout
        btnLogout.setOnClickListener {
            clearAdminSession()
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
    
    private fun isAdminLoggedIn(): Boolean {
        val prefs = getSharedPreferences("AdminSession", MODE_PRIVATE)
        return prefs.getBoolean("is_admin_logged_in", false)
    }
    
    private fun clearAdminSession() {
        val prefs = getSharedPreferences("AdminSession", MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}

