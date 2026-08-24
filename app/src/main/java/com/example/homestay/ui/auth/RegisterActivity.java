package com.example.homestay.ui.auth;

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
import com.example.homestay.utils.InputValidator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {
  private AuthViewModel viewModel;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_register);
    ViewCompat.setOnApplyWindowInsetsListener(
        findViewById(android.R.id.content),
        (v, i) -> {
          Insets b = i.getInsets(WindowInsetsCompat.Type.systemBars());
          v.setPadding(b.left, b.top, b.right, b.bottom);
          return i;
        });
    HomestayApplication app = (HomestayApplication) getApplicationContext();
    viewModel =
        new ViewModelProvider(this, new AuthViewModel.Factory(app.getAuthRepository(), this))
            .get(AuthViewModel.class);
    setup();
    viewModel
        .getRegisterResult()
        .observe(
            this,
            r -> {
              if (r == null) return;
              Toast.makeText(
                      this,
                      r.success
                          ? "Đăng ký thành công! Vui lòng đăng nhập."
                          : (r.message == null ? "Đăng ký thất bại" : r.message),
                      Toast.LENGTH_SHORT)
                  .show();
              if (r.success) finish();
            });
  }

  private void setup() {
    TextInputEditText name = findViewById(R.id.et_full_name),
        email = findViewById(R.id.et_email),
        phone = findViewById(R.id.et_phone),
        pass = findViewById(R.id.et_password),
        confirm = findViewById(R.id.et_confirm_password);
    ((MaterialButton) findViewById(R.id.btn_register))
        .setOnClickListener(
            v -> {
              String n = text(name).trim(),
                  e = text(email).trim(),
                  p = text(phone).trim(),
                  pw = text(pass),
                  c = text(confirm);
              if (n.isEmpty() || !InputValidator.validateFullName(n)) {
                name.setError("Họ và tên không hợp lệ (2-50 ký tự)");
                return;
              }
              if (!InputValidator.validateEmail(e)) {
                email.setError("Email không hợp lệ");
                return;
              }
              if (!InputValidator.validatePhoneNumber(p)) {
                phone.setError("Số điện thoại không hợp lệ");
                return;
              }
              if (pw.isEmpty()) {
                pass.setError("Vui lòng nhập mật khẩu");
                return;
              }
              if (!pw.equals(c)) {
                confirm.setError("Mật khẩu xác nhận không khớp");
                return;
              }
              viewModel.register(n, e, p, pw);
            });
    ((TextView) findViewById(R.id.tv_login_link)).setOnClickListener(v -> finish());
  }

  private static String text(TextInputEditText f) {
    return f.getText() == null ? "" : f.getText().toString();
  }
}
