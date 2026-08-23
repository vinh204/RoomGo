package com.example.homestay

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.example.homestay.utils.SessionManager
import com.example.homestay.utils.AdminAuth
import com.example.homestay.data.entity.User

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
        observeDashboard()
        lifecycleScope.launch { ensureAdminCustomerSession() }
    }

    private fun observeDashboard() {
        val repository = (application as HomestayApplication).repository
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    repository.getAllRooms(),
                    repository.getAllUsers(),
                    repository.getAllBookings()
                ) { rooms, users, bookings -> Triple(rooms, users, bookings) }
                    .collect { (rooms, users, bookings) ->
                        findViewById<TextView>(R.id.tv_stat_rooms).text = "${rooms.size}\nPhòng"
                        findViewById<TextView>(R.id.tv_stat_users).text = "${users.size}\nNgười dùng"
                        findViewById<TextView>(R.id.tv_stat_pending).text = "${bookings.count { it.status == "pending" }}\nChờ duyệt"
                        val revenue = bookings.filter { it.status in setOf("confirmed", "completed") }.sumOf { it.totalPrice }
                        val amount = NumberFormat.getNumberInstance(Locale("vi", "VN")).format(revenue.toLong())
                        findViewById<TextView>(R.id.tv_stat_revenue).text = "Doanh thu dự kiến: $amount đ"
                        val actualRevenue = bookings.filter { it.status == "completed" }.sumOf { it.totalPrice }
                        val actualAmount = NumberFormat.getNumberInstance(Locale("vi", "VN")).format(actualRevenue.toLong())
                        findViewById<TextView>(R.id.tv_stat_revenue_actual).text = "Doanh thu thực tế: $actualAmount đ"
                        val confirmed = bookings.count { it.status == "confirmed" }
                        val completed = bookings.count { it.status == "completed" }
                        val cancelled = bookings.count { it.status == "cancelled" }
                        findViewById<TextView>(R.id.tv_booking_breakdown).text =
                            "Tổng booking: ${bookings.size}\nĐã xác nhận: $confirmed  •  Hoàn thành: $completed  •  Đã hủy: $cancelled"
                        val activeRooms = rooms.count { it.isAvailable }
                        val occupiedRoomIds = bookings.filter { it.status == "confirmed" && it.checkInDate <= System.currentTimeMillis() && it.checkOutDate >= System.currentTimeMillis() }.map { it.roomId }.distinct().size
                        val occupancy = if (activeRooms == 0) 0 else occupiedRoomIds * 100 / activeRooms
                        val monthStart = java.util.Calendar.getInstance().apply { set(java.util.Calendar.DAY_OF_MONTH, 1); set(java.util.Calendar.HOUR_OF_DAY, 0) }.timeInMillis
                        findViewById<TextView>(R.id.tv_operational_summary).text =
                            "Phòng đang mở: $activeRooms/${rooms.size}  •  Đang lưu trú: $occupiedRoomIds\nNgười dùng mới tháng này: ${users.count { it.createdAt >= monthStart }}  •  Tỷ lệ lấp đầy hiện tại: $occupancy%"
                        val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale("vi", "VN"))
                        findViewById<TextView>(R.id.tv_recent_bookings).text = bookings.sortedByDescending { it.createdAt }.take(5)
                            .joinToString("\n") { "#${it.id} • ${dateFormat.format(Date(it.createdAt))} • ${statusLabel(it.status)} • ${NumberFormat.getNumberInstance(Locale("vi", "VN")).format(it.totalPrice.toLong())} đ" }
                            .ifBlank { "Chưa có booking" }
                    }
            }
        }
    }

    private fun statusLabel(status: String) = when (status) {
        "pending" -> "Chờ duyệt"; "confirmed" -> "Đã xác nhận"
        "completed" -> "Hoàn thành"; "cancelled" -> "Đã hủy"; else -> status
    }
    
    private fun setupViews() {
        val tvAdminName = findViewById<TextView>(R.id.tv_admin_name)
        val cardManageRooms = findViewById<MaterialCardView>(R.id.card_manage_rooms)
        val cardViewUsers = findViewById<MaterialCardView>(R.id.card_view_users)
        val cardManageBookings = findViewById<MaterialCardView>(R.id.card_manage_bookings)
        val btnLogout = findViewById<MaterialButton>(R.id.btn_logout)
        val btnSwitchToGuest = findViewById<MaterialButton>(R.id.btn_switch_to_guest)
        
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

        btnSwitchToGuest.setOnClickListener {
            btnSwitchToGuest.isEnabled = false
            lifecycleScope.launch {
                ensureAdminCustomerSession()
                startActivity(Intent(this@AdminDashboardActivity, MainActivity::class.java).apply {
                    putExtra("admin_guest_preview", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
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
        SessionManager(this).clearSession()
    }

    private suspend fun ensureAdminCustomerSession() {
        val session = SessionManager(this)
        if (session.isLoggedIn()) return

        val repository = (application as HomestayApplication).repository
        val adminUser = repository.getUserByEmail(AdminAuth.EMAIL) ?: run {
            val id = repository.insertUser(
                User(
                    email = AdminAuth.EMAIL,
                    phone = "admin-local",
                    password = "AdminLocalSession@1",
                    fullName = "Administrator"
                )
            )
            repository.getUserById(id) ?: return
        }
        session.saveSession(
            userId = adminUser.id,
            mongoUserId = "local-admin",
            email = adminUser.email,
            name = adminUser.fullName
        )
    }
}

