package com.example.homestay

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AdminLoginActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)
        
        setupViews()
    }
    
    private fun setupViews() {
        val etUsername = findViewById<TextInputEditText>(R.id.et_username)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)
        val btnLogin = findViewById<MaterialButton>(R.id.btn_login)
        val btnCancel = findViewById<MaterialButton>(R.id.btn_cancel)
        
        btnLogin.setOnClickListener {
            val username = etUsername.text?.toString()?.trim() ?: ""
            val password = etPassword.text?.toString() ?: ""
            
            if (username.isEmpty()) {
                etUsername.error = "Vui lòng nhập username"
                return@setOnClickListener
            }
            
            if (password.isEmpty()) {
                etPassword.error = "Vui lòng nhập password"
                return@setOnClickListener
            }
            
            // Disable button
            btnLogin.isEnabled = false
            btnLogin.text = "Đang đăng nhập..."
            
            adminLogin(username, password, btnLogin)
        }
        
        btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun adminLogin(username: String, password: String, btnLogin: MaterialButton) {
        if (username == "admin" && password == "Admin@123") {
            saveAdminSession("local-admin", "admin", "Administrator", "super_admin")
            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AdminDashboardActivity::class.java))
            finish()
        } else {
            btnLogin.isEnabled = true
            btnLogin.text = "Đăng nhập"
            Toast.makeText(this, "Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveAdminSession(id: String, username: String, fullName: String, role: String) {
        val prefs = getSharedPreferences("AdminSession", MODE_PRIVATE)
        prefs.edit().apply {
            putString("admin_id", id)
            putString("admin_username", username)
            putString("admin_fullname", fullName)
            putString("admin_role", role)
            putBoolean("is_admin_logged_in", true)
            apply()
        }
    }
}

