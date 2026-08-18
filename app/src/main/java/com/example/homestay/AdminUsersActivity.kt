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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homestay.data.model.AdminUserData
import com.example.homestay.ui.admin.AdminUserAdapter
import com.example.homestay.utils.RateLimiter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class AdminUsersActivity : AppCompatActivity() {
    private val repository by lazy { (application as HomestayApplication).repository }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminUserAdapter
    private lateinit var progressBar: ProgressBar
    private var users = mutableListOf<AdminUserData>()
    
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
            onUnlockClick = { user -> showUnlockConfirmDialog(user) }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        findViewById<TextInputEditText>(R.id.et_admin_search).addTextChangedListener { filterUsers(it?.toString().orEmpty()) }
    }
    
    private fun loadUsers() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val usersList = repository.getAllUsers().first().map { user ->
                    AdminUserData(user.id.toString(), user.email, user.phone, user.fullName, user.createdAt)
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
            it.fullName.contains(keyword, true) || it.email.contains(keyword, true) || it.phone.contains(keyword, true)
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
