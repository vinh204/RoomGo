package com.example.homestay.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.homestay.HomestayApplication;
import com.example.homestay.R;
import com.example.homestay.data.entity.User;
import com.example.homestay.ui.admin.AdminDashboardActivity;
import com.example.homestay.ui.customer.MainActivity;
import com.example.homestay.utils.AdminAuth;
import com.example.homestay.utils.AppExecutors;
import com.example.homestay.utils.SessionManager;
import com.example.homestay.utils.RateLimiter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {
  private HomestayApplication app;
  private AuthViewModel viewModel;
  private SessionManager sessionManager;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_login);
    ViewCompat.setOnApplyWindowInsetsListener(
        findViewById(android.R.id.content),
        (v, insets) -> {
          Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          v.setPadding(b.left, b.top, b.right, b.bottom);
          return insets;
        });
    app = (HomestayApplication) getApplicationContext();
    sessionManager = new SessionManager(this);
    viewModel =
        new ViewModelProvider(this, new AuthViewModel.Factory(app.getAuthRepository(), this))
            .get(AuthViewModel.class);
    if (isAdminLoggedIn()) {
      navigateAdmin();
      return;
    }
    if (sessionManager.isLoggedIn()) {
      navigateMain();
      return;
    }
    setupViews();
    viewModel
        .getLoginResult()
        .observe(
            this,
            result -> {
              if (result == null) return;
              if (result.success && result.user != null && result.mongoUserId != null) {
                clearAdminSession();
                User user = result.user;
                sessionManager.saveSession(
                    user.getId(), result.mongoUserId, user.getEmail(), user.getFullName());
                AppExecutors.io().execute(() -> app.getRepository().refreshLocalRooms());
                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                navigateMain();
              } else
                Toast.makeText(
                        this,
                        result.message == null ? "Đăng nhập thất bại" : result.message,
                        Toast.LENGTH_SHORT)
                    .show();
            });
  }

  private void setupViews() {
    TextInputEditText email = findViewById(R.id.et_email),
        password = findViewById(R.id.et_password);
    MaterialButton button = findViewById(R.id.btn_login);
    android.view.View form = findViewById(R.id.login_form_container);
    form.setAlpha(0);
    form.setTranslationY(100);
    form.animate().alpha(1).translationY(0).setDuration(800).setStartDelay(200).start();
    button.setOnClickListener(
        v -> {
          String e = text(email).trim(), p = text(password);
          if (e.isEmpty()) {
            email.setError("Vui lòng nhập email");
            return;
          }
          if (p.isEmpty()) {
            password.setError("Vui lòng nhập mật khẩu");
            return;
          }
          if (e.equalsIgnoreCase(AdminAuth.EMAIL)) {
            loginAdmin(p);
            return;
          }
          viewModel.login(e, p);
        });
    ((TextView) findViewById(R.id.tv_register_link))
        .setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
  }

  private void loginAdmin(String password) {
    RateLimiter.AttemptStatus attempt = RateLimiter.canAttemptLogin(this, AdminAuth.EMAIL);
    if (!attempt.allowed) {
      long minutes = Math.max(1, RateLimiter.getLockedMinutesRemaining(this, AdminAuth.EMAIL));
      Toast.makeText(
              this,
              "Đăng nhập tạm khóa. Vui lòng thử lại sau " + minutes + " phút.",
              Toast.LENGTH_SHORT)
          .show();
      return;
    }
    AppExecutors.io()
        .execute(
            () -> {
              User user = app.getRepository().login(AdminAuth.EMAIL, password);
              boolean customized =
                  getSharedPreferences("AdminSecurity", MODE_PRIVATE)
                      .getBoolean("admin_password_customized", false);
              if (user == null && !customized && AdminAuth.authenticate(AdminAuth.EMAIL, password)) {
                user = app.getRepository().getUserByEmail(AdminAuth.EMAIL);
                if (user != null) {
                  app.getRepository().updateUser(user.withPassword(password));
                  user = app.getRepository().getUserByEmail(AdminAuth.EMAIL);
                }
              }
              if (user == null && !customized && AdminAuth.authenticate(AdminAuth.EMAIL, password)) {
                long id =
                    app.getRepository()
                        .insertUser(
                            new User(
                                0,
                                AdminAuth.EMAIL,
                                "admin-local",
                                password,
                                "Administrator",
                                System.currentTimeMillis(),
                                false,
                                "ADMIN"));
                user = app.getRepository().getUserById(id);
              }
              User authenticatedAdmin = user != null && user.isAdmin() ? user : null;
              if (authenticatedAdmin != null) {
                RateLimiter.recordSuccess(this, AdminAuth.EMAIL);
                sessionManager.saveSession(
                    authenticatedAdmin.getId(),
                    "local-admin",
                    authenticatedAdmin.getEmail(),
                    authenticatedAdmin.getFullName());
              } else {
                RateLimiter.recordFailure(this, AdminAuth.EMAIL);
              }
              runOnUiThread(
                  () -> {
                    if (authenticatedAdmin == null) {
                      Toast.makeText(
                              this, "Email hoặc mật khẩu không đúng", Toast.LENGTH_SHORT)
                          .show();
                      return;
                    }
                    saveAdminSession();
                    navigateAdmin();
                  });
            });
  }

  private static String text(TextInputEditText field) {
    return field.getText() == null ? "" : field.getText().toString();
  }

  private boolean isAdminLoggedIn() {
    return getSharedPreferences("AdminSession", MODE_PRIVATE)
        .getBoolean("is_admin_logged_in", false);
  }

  private void saveAdminSession() {
    getSharedPreferences("AdminSession", MODE_PRIVATE)
        .edit()
        .putString("admin_id", "local-admin")
        .putString("admin_username", AdminAuth.EMAIL)
        .putString("admin_fullname", "Administrator")
        .putString("admin_role", "super_admin")
        .putBoolean("is_admin_logged_in", true)
        .apply();
  }

  private void clearAdminSession() {
    getSharedPreferences("AdminSession", MODE_PRIVATE).edit().clear().apply();
  }

  private void navigateMain() {
    Intent i = new Intent(this, MainActivity.class);
    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(i);
    finish();
  }

  private void navigateAdmin() {
    Intent i = new Intent(this, AdminDashboardActivity.class);
    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(i);
    finish();
  }
}
