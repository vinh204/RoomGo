package com.example.homestay

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.homestay.ui.viewmodel.AuthViewModel
import com.example.homestay.ui.viewmodel.AuthViewModelFactory
import com.example.homestay.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private val application by lazy { applicationContext as HomestayApplication }
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(application.authRepository, this)
    }
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionManager = SessionManager(this)

        // Nếu đã đăng nhập, chuyển đến MainActivity
        if (sessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        setupViews()
        observeLoginResult()
    }

    private fun setupViews() {
        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)
        val btnLogin = findViewById<MaterialButton>(R.id.btn_login)
        val tvRegisterLink = findViewById<android.widget.TextView>(R.id.tv_register_link)

        // Animation cho Form Container
        val loginForm = findViewById<android.view.View>(R.id.login_form_container)
        loginForm.alpha = 0f
        loginForm.translationY = 100f
        loginForm.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setStartDelay(200)
            .start()

        btnLogin.setOnClickListener {
            val email = etEmail.text?.toString()?.trim() ?: ""
            val password = etPassword.text?.toString() ?: ""

            if (email.isEmpty()) {
                etEmail.error = "Vui lòng nhập email"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Vui lòng nhập mật khẩu"
                return@setOnClickListener
            }

            if (email.equals(ADMIN_USERNAME, ignoreCase = true)) {
                if (password == ADMIN_PASSWORD) {
                    saveAdminSession()
                    navigateToAdminDashboard()
                } else {
                    Toast.makeText(this, "Tên đăng nhập hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }

        tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun observeLoginResult() {
        lifecycleScope.launch {
            viewModel.loginResult.collect { result ->
                result?.let {
                    if (it.success) {
                        val user = it.user
                        val mongoUserId = it.mongoUserId
                        if (user != null && mongoUserId != null) {
                            // Lưu session (bao gồm MongoDB ID để gọi API update)
                            sessionManager.saveSession(user.id, mongoUserId, user.email, user.fullName)
                            
                            // Sync rooms từ backend sau khi đăng nhập
                            val application = applicationContext as com.example.homestay.HomestayApplication
                            lifecycleScope.launch {
                                try {
                                    application.repository.syncRoomsFromAPI()
                                    android.util.Log.d("LoginActivity", "Rooms synced after login")
                                } catch (e: Exception) {
                                    android.util.Log.e("LoginActivity", "Error syncing rooms: ${e.message}", e)
                                }
                            }
                            
                            Toast.makeText(this@LoginActivity, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                            navigateToMain()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, it.message ?: "Đăng nhập thất bại", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun saveAdminSession() {
        getSharedPreferences("AdminSession", MODE_PRIVATE).edit()
            .putString("admin_id", "local-admin")
            .putString("admin_username", ADMIN_USERNAME)
            .putString("admin_fullname", "Administrator")
            .putString("admin_role", "super_admin")
            .putBoolean("is_admin_logged_in", true)
            .apply()
    }

    private fun navigateToAdminDashboard() {
        val intent = Intent(this, AdminDashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        private const val ADMIN_USERNAME = "admin"
        private const val ADMIN_PASSWORD = "Admin@123"
    }
}

