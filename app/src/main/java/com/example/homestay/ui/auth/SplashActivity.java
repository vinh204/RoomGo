package com.example.homestay.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import com.example.homestay.ui.admin.AdminDashboardActivity;
import com.example.homestay.ui.customer.MainActivity;

public class SplashActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle state) {
    SplashScreen.Companion.installSplashScreen(this);
    super.onCreate(state);
    boolean admin =
        getSharedPreferences("AdminSession", MODE_PRIVATE).getBoolean("is_admin_logged_in", false);
    startActivity(new Intent(this, admin ? AdminDashboardActivity.class : MainActivity.class));
    finish();
  }
}
