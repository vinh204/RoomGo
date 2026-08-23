package com.example.homestay

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.chip.ChipGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homestay.data.model.AdminUserData
import com.example.homestay.ui.admin.AdminUserAdapter
import com.example.homestay.utils.RateLimiter
import com.example.homestay.utils.AdminAuth
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class AdminUsersActivity : AppCompatActivity() {
    private val repository by lazy { (application as HomestayApplication).repository }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminUserAdapter
    private lateinit var progressBar: ProgressBar
    private var users = mutableListOf<AdminUserData>()
    private var lockedFilter: Boolean? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_users)
        
        setupToolbar()
        setupViews()
        loadUsers()
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Danh sách Users"
    }
    
    private fun setupViews() {
        recyclerView = findViewById(R.id.rv_users)
        progressBar = findViewById(R.id.progress_bar)
        
        adapter = AdminUserAdapter(
            users = users,
            onDeleteClick = { user -> showDeleteConfirmDialog(user) },
            onUnlockClick = { user -> showUnlockConfirmDialog(user) },
            onDetailsClick = { user -> showUserDetails(user) },
            onEditClick = { user -> showEditUserDialog(user) }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        findViewById<TextInputEditText>(R.id.et_admin_search).addTextChangedListener { filterUsers(it?.toString().orEmpty()) }
        findViewById<ChipGroup>(R.id.chip_user_status).setOnCheckedStateChangeListener { _, ids ->
            lockedFilter = when (ids.firstOrNull()) {
                R.id.chip_users_active -> false; R.id.chip_users_locked -> true; else -> null
            }
            filterUsers(findViewById<TextInputEditText>(R.id.et_admin_search).text?.toString().orEmpty())
        }
    }
    
    private fun loadUsers() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val allBookings = repository.getAllBookings().first()
                val usersList = repository.getAllUsers().first().map { user ->
                    val userBookings = allBookings.filter { it.userId == user.id }
                    val canLogin = RateLimiter.canAttemptLogin(this@AdminUsersActivity, user.email)
                    val failed = RateLimiter.getFailedAttempts(this@AdminUsersActivity, user.email)
                    AdminUserData(user.id.toString(), user.email, user.phone, user.fullName, user.createdAt,
                        failedLoginAttempts = failed, locked = !canLogin.first,
                        permanent = !canLogin.first && RateLimiter.getLockedSecondsRemaining(this@AdminUsersActivity, user.email) > 365L * 24 * 3600,
                        lockedUntil = canLogin.second,
                        secondsRemaining = RateLimiter.getLockedSecondsRemaining(this@AdminUsersActivity, user.email),
                        bookingCount = userBookings.size,
                        totalSpent = userBookings.filter { it.status == "completed" }.sumOf { it.totalPrice },
                        lastBookingAt = userBookings.maxOfOrNull { it.createdAt })
                }
                users.clear()
                users.addAll(usersList)
                filterUsers(findViewById<TextInputEditText>(R.id.et_admin_search).text?.toString().orEmpty())
            } catch (e: Exception) {
                android.util.Log.e("AdminUsers", "Error: ${e.message}", e)
                Toast.makeText(this@AdminUsersActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun filterUsers(query: String) {
        val keyword = query.trim()
        val filtered = users.filter {
            (lockedFilter == null || it.locked == lockedFilter) &&
                (it.fullName.contains(keyword, true) || it.email.contains(keyword, true) || it.phone.contains(keyword, true))
        }
        adapter.updateUsers(filtered)
        findViewById<View>(R.id.tv_empty).visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun showDeleteConfirmDialog(user: AdminUserData) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Xóa user")
            .setMessage("Bạn có chắc chắn muốn xóa user \"${user.fullName}\" (${user.email})?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteUser(user.id)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showUserDetails(user: AdminUserData) {
        lifecycleScope.launch {
            val bookings = repository.getAllBookings().first().filter { it.userId.toString() == user.id }
            val rooms = repository.getAllRooms().first().associateBy { it.id }
            val date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("vi", "VN"))
            val history = bookings.sortedByDescending { it.createdAt }.take(10).joinToString("\n") {
                "• ${rooms[it.roomId]?.name ?: "Phòng #${it.roomId}"} — ${date.format(java.util.Date(it.checkInDate))} — ${it.status}"
            }.ifBlank { "Chưa có lịch sử đặt phòng" }
            val spent = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN")).format(user.totalSpent.toLong())
            MaterialAlertDialogBuilder(this@AdminUsersActivity)
                .setTitle(user.fullName)
                .setMessage("Mã người dùng: ${user.id}\nEmail: ${user.email}\nĐiện thoại: ${user.phone}\nNgày tham gia: ${date.format(java.util.Date(user.createdAt))}\nTrạng thái: ${if (user.locked == true) "Bị khóa" else "Hoạt động"}\nTổng booking: ${user.bookingCount}\nTổng đã chi: $spent đ\n\nLịch sử gần đây:\n$history")
                .setPositiveButton("Đóng", null).show()
        }
    }

    private fun showEditUserDialog(userData: AdminUserData) {
        if (userData.email.equals(AdminAuth.EMAIL, ignoreCase = true)) {
            Toast.makeText(this, "Không thể chỉnh sửa tài khoản quản trị hệ thống", Toast.LENGTH_SHORT).show()
            return
        }

        val view = layoutInflater.inflate(R.layout.dialog_admin_edit_user, null)
        val etName = view.findViewById<TextInputEditText>(R.id.et_admin_user_name)
        val etEmail = view.findViewById<TextInputEditText>(R.id.et_admin_user_email)
        val etPhone = view.findViewById<TextInputEditText>(R.id.et_admin_user_phone)
        val etPassword = view.findViewById<TextInputEditText>(R.id.et_admin_user_password)
        val btnCancel = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel)
        val btnSave = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save)
        etName.setText(userData.fullName)
        etEmail.setText(userData.email)
        etPhone.setText(userData.phone)

        val dialog = MaterialAlertDialogBuilder(this).setView(view).create()
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val name = etName.text?.toString()?.trim().orEmpty()
            val email = etEmail.text?.toString()?.trim()?.lowercase().orEmpty()
            val phone = etPhone.text?.toString()?.trim().orEmpty()
            val newPassword = etPassword.text?.toString().orEmpty()

            when {
                name.length < 2 -> etName.error = "Họ tên phải có ít nhất 2 ký tự"
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> etEmail.error = "Email không hợp lệ"
                !phone.matches(Regex("^[0-9+]{8,15}$")) -> etPhone.error = "Số điện thoại không hợp lệ"
                newPassword.isNotBlank() && newPassword.length < 8 -> etPassword.error = "Mật khẩu phải có ít nhất 8 ký tự"
                else -> lifecycleScope.launch {
                    try {
                        val current = repository.getUserById(userData.id.toLong()) ?: return@launch
                        val duplicateEmail = repository.getUserByEmail(email)?.takeIf { it.id != current.id }
                        val duplicatePhone = repository.getUserByPhone(phone)?.takeIf { it.id != current.id }
                        when {
                            duplicateEmail != null -> etEmail.error = "Email đã được sử dụng"
                            duplicatePhone != null -> etPhone.error = "Số điện thoại đã được sử dụng"
                            else -> {
                                repository.updateUser(current.copy(
                                    fullName = name, email = email, phone = phone,
                                    password = newPassword.ifBlank { current.password }
                                ))
                                RateLimiter.reset(this@AdminUsersActivity, userData.email)
                                Toast.makeText(this@AdminUsersActivity, "Cập nhật người dùng thành công", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                                loadUsers()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@AdminUsersActivity, "Không thể cập nhật: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }
    
    private fun showUnlockConfirmDialog(user: AdminUserData) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Mở khóa tài khoản")
            .setMessage("Bạn có chắc chắn muốn mở khóa tài khoản \"${user.fullName}\" (${user.email})?")
            .setPositiveButton("Mở khóa") { _, _ ->
                unlockUser(user.id)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun unlockUser(userId: String) {
        // Tìm user để lấy email trước khi unlock
        val user = users.find { it.id == userId }
        val userEmail = user?.email
        
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            userEmail?.let { RateLimiter.reset(this@AdminUsersActivity, it) }
            Toast.makeText(this@AdminUsersActivity, "Mở khóa tài khoản thành công!", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            loadUsers()
        }
    }

    private fun deleteUser(userId: String) {
        lifecycleScope.launch {
            try {
                val user = repository.getUserById(userId.toLong()) ?: return@launch
                if (repository.userHasBookings(user.id)) {
                    Toast.makeText(this@AdminUsersActivity, "Không thể xóa người dùng đang có booking", Toast.LENGTH_LONG).show()
                    return@launch
                }
                repository.deleteUser(user)
                Toast.makeText(this@AdminUsersActivity, "Xóa user thành công!", Toast.LENGTH_SHORT).show()
                adapter.removeUser(userId)
            } catch (e: Exception) {
                Toast.makeText(this@AdminUsersActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
