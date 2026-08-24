package com.example.homestay.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import com.example.homestay.HomestayApplication;
import com.example.homestay.R;
import com.example.homestay.data.entity.*;
import com.example.homestay.data.repository.HomestayRepository;
import com.example.homestay.ui.auth.LoginActivity;
import com.example.homestay.ui.customer.MainActivity;
import com.example.homestay.utils.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.text.*;
import java.util.*;

public class AdminDashboardActivity extends AppCompatActivity {
  private HomestayRepository repository;
  private DashboardPeriod dashboardPeriod = DashboardPeriod.MONTH;

  @Override
  protected void onCreate(Bundle s) {
    super.onCreate(s);
    setContentView(R.layout.activity_admin_dashboard);
    SystemBarUtils.keepContentBelowStatusBar(this);
    if (!isAdminLoggedIn()) {
      finish();
      return;
    }
    repository = ((HomestayApplication) getApplication()).getRepository();
    setupViews();
    AdminNavigationUtils.setup(this, R.id.admin_nav_dashboard);
    ensureAdminCustomerSession();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (repository != null) loadDashboard();
  }

  private void loadDashboard() {
    AppExecutors.io()
        .execute(
            () -> {
              List<AppNotification> adminNotifications =
                  repository.syncAdminActivityNotifications();
              SystemNotificationHelper.publishNew(this, adminNotifications);
              List<Room> rooms = repository.getAllRoomsNow();
              List<User> users = repository.getAllUsersNow();
              List<Booking> bookings = repository.getAllBookingsNow();
              runOnUiThread(
                  () -> {
                    updateNotificationBadge(adminNotifications);
                    render(rooms, users, bookings);
                  });
            });
  }

  private void render(List<Room> rooms, List<User> users, List<Booking> bookings) {
    int pending = 0, active = 0, staying = 0, completed = 0, cancelled = 0;
    double expected = 0, actual = 0, previousActual = 0;
    long now = System.currentTimeMillis();
    long periodStart = periodStart(dashboardPeriod, now);
    long previousStart = previousPeriodStart(dashboardPeriod, periodStart);
    for (Room r : rooms) if (r.isAvailable()) active++;
    for (Booking b : bookings) {
      if ("pending".equals(b.getStatus())) pending++;
      boolean inPeriod = b.getCreatedAt() >= periodStart && b.getCreatedAt() <= now;
      if (inPeriod
          && ("confirmed".equals(b.getStatus())
              || "checked_in".equals(b.getStatus())
              || "completed".equals(b.getStatus())))
        expected += b.getTotalPrice();
      if (inPeriod && "PAID".equals(b.getPaymentStatus())) actual += b.getTotalPrice();
      if (inPeriod && "completed".equals(b.getStatus())) completed++;
      if (inPeriod && "cancelled".equals(b.getStatus())) cancelled++;
      if ("checked_in".equals(b.getStatus())) staying++;
      if (b.getCreatedAt() >= previousStart
          && b.getCreatedAt() < periodStart
          && "PAID".equals(b.getPaymentStatus())) previousActual += b.getTotalPrice();
    }
    text(R.id.tv_stat_rooms, active + "\nPhòng đang mở");
    text(R.id.tv_stat_users, users.size() + "\nNgười dùng");
    text(R.id.tv_stat_pending, pending + "\nChờ duyệt");
    text(R.id.tv_stat_revenue, DisplayFormatter.vnd(expected));
    text(R.id.tv_stat_revenue_actual, DisplayFormatter.vnd(actual));
    text(R.id.tv_stat_staying, String.valueOf(staying));
    text(R.id.tv_stat_completed, String.valueOf(completed));
    text(R.id.tv_stat_cancelled, String.valueOf(cancelled));
    int change =
        previousActual == 0
            ? (actual > 0 ? 100 : 0)
            : (int) Math.round((actual - previousActual) * 100 / previousActual);
    TextView comparison = findViewById(R.id.tv_revenue_comparison);
    comparison.setText(
        (change > 0 ? "+" : "") + change + "% so với " + dashboardPeriod.previousLabel);
    comparison.setTextColor(change >= 0 ? 0xFF167A50 : 0xFFD14343);
    LinearLayout container = findViewById(R.id.recent_bookings_container);
    container.removeAllViews();
    bookings.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
    SimpleDateFormat date = new SimpleDateFormat("dd/MM HH:mm", new Locale("vi", "VN"));
    int recentCount = Math.min(5, bookings.size());
    for (int i = 0; i < recentCount; i++) {
      Booking b = bookings.get(i);
      View card = getLayoutInflater().inflate(R.layout.item_admin_recent_booking, container, false);
      ((TextView) card.findViewById(R.id.tv_recent_code))
          .setText(DisplayFormatter.bookingCode(b.getId(), b.getCreatedAt()));
      TextView status = card.findViewById(R.id.tv_recent_status);
      status.setText(statusLabel(b.getStatus()));
      status.setTextColor(statusColor(b.getStatus()));
      ((TextView) card.findViewById(R.id.tv_recent_meta))
          .setText(date.format(new Date(b.getCreatedAt())));
      ((TextView) card.findViewById(R.id.tv_recent_price))
          .setText(DisplayFormatter.vnd(b.getTotalPrice()));
      card.findViewById(R.id.recent_divider)
          .setVisibility(i == recentCount - 1 ? View.GONE : View.VISIBLE);
      card.setOnClickListener(
          view -> {
            Intent intent = new Intent(this, AdminBookingsActivity.class);
            intent.putExtra(
                AdminBookingsActivity.EXTRA_BOOKING_CODE,
                DisplayFormatter.bookingCode(b.getId(), b.getCreatedAt()));
            startActivity(intent);
            overridePendingTransition(0, 0);
          });
      container.addView(card);
    }
  }

  private void setupViews() {
    int[] hidden = {
      R.id.card_manage_rooms,
      R.id.card_view_users,
      R.id.card_manage_bookings,
      R.id.tv_management_title
    };
    for (int id : hidden) findViewById(id).setVisibility(View.GONE);
    findViewById(R.id.btn_admin_notifications)
        .setOnClickListener(
            view -> startActivity(new Intent(this, AdminNotificationsActivity.class)));
    findViewById(R.id.card_stat_rooms)
        .setOnClickListener(view -> openAdminScreen(AdminRoomsActivity.class));
    findViewById(R.id.card_stat_users)
        .setOnClickListener(view -> openAdminScreen(AdminUsersActivity.class));
    findViewById(R.id.card_stat_pending)
        .setOnClickListener(view -> openAdminScreen(AdminBookingsActivity.class));
    findViewById(R.id.card_period_summary)
        .setOnClickListener(view -> openAdminScreen(AdminBookingsActivity.class));
    findViewById(R.id.btn_dashboard_period)
        .setOnClickListener(
            anchor -> {
              PopupMenu periods = new PopupMenu(this, anchor, Gravity.END);
              addPeriod(periods, "Hôm nay", DashboardPeriod.TODAY);
              addPeriod(periods, "7 ngày", DashboardPeriod.SEVEN_DAYS);
              addPeriod(periods, "Tháng này", DashboardPeriod.MONTH);
              periods.show();
            });
    findViewById(R.id.btn_admin_account)
        .setOnClickListener(
            anchor -> {
              // The avatar sits on the left side of the header. Aligning the popup to END
              // makes a wide menu get pushed against the screen edge, so keep its start
              // aligned with the avatar instead.
              PopupMenu menu = new PopupMenu(this, anchor, Gravity.START);
              menu.getMenu()
                  .add("Đổi mật khẩu")
                  .setOnMenuItemClickListener(
                      x -> {
                        showChangePassword();
                        return true;
                      });
              menu.getMenu()
                  .add("Xem giao diện khách")
                  .setOnMenuItemClickListener(
                      x -> {
                        switchToGuest();
                        return true;
                      });
              menu.getMenu()
                  .add("Đăng xuất")
                  .setOnMenuItemClickListener(
                      x -> {
                        logout();
                        return true;
                      });
              menu.show();
            });
  }

  private void updateNotificationBadge(List<AppNotification> notifications) {
    int unread = 0;
    for (AppNotification notification : notifications)
      if (!notification.isRead()) unread++;
    TextView badge =
        findViewById(R.id.btn_admin_notifications).findViewById(R.id.tv_notification_badge);
    badge.setVisibility(unread == 0 ? View.GONE : View.VISIBLE);
    badge.setText(unread > 99 ? "99+" : String.valueOf(unread));
  }

  private void addPeriod(PopupMenu menu, String label, DashboardPeriod period) {
    menu.getMenu()
        .add(label)
        .setOnMenuItemClickListener(
            item -> {
              dashboardPeriod = period;
              ((TextView) findViewById(R.id.btn_dashboard_period)).setText(label + " ▾");
              loadDashboard();
              return true;
            });
  }

  private void openAdminScreen(Class<? extends AppCompatActivity> target) {
    startActivity(new Intent(this, target));
    overridePendingTransition(0, 0);
  }

  private static long periodStart(DashboardPeriod period, long now) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(now);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    if (period == DashboardPeriod.SEVEN_DAYS) calendar.add(Calendar.DAY_OF_MONTH, -6);
    if (period == DashboardPeriod.MONTH) calendar.set(Calendar.DAY_OF_MONTH, 1);
    return calendar.getTimeInMillis();
  }

  private static long previousPeriodStart(DashboardPeriod period, long start) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(start);
    if (period == DashboardPeriod.TODAY) calendar.add(Calendar.DAY_OF_MONTH, -1);
    else if (period == DashboardPeriod.SEVEN_DAYS) calendar.add(Calendar.DAY_OF_MONTH, -7);
    else calendar.add(Calendar.MONTH, -1);
    return calendar.getTimeInMillis();
  }

  private enum DashboardPeriod {
    TODAY("hôm qua"),
    SEVEN_DAYS("7 ngày trước"),
    MONTH("tháng trước");

    final String previousLabel;

    DashboardPeriod(String previousLabel) {
      this.previousLabel = previousLabel;
    }
  }

  private void showChangePassword() {
    View content = getLayoutInflater().inflate(R.layout.dialog_admin_change_password, null);
    TextInputEditText password = content.findViewById(R.id.et_admin_new_password);
    TextInputEditText confirm = content.findViewById(R.id.et_admin_confirm_password);
    androidx.appcompat.app.AlertDialog dialog =
        new MaterialAlertDialogBuilder(this)
            .setTitle("Đổi mật khẩu admin")
            .setView(content)
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Cập nhật", null)
            .create();
    dialog.setOnShowListener(
        ignored ->
            dialog
                .getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    view -> {
                      String newPassword = fieldText(password);
                      if (!InputValidator.isPasswordValid(newPassword)) {
                        password.setError(InputValidator.getPasswordErrorMessage(newPassword));
                        return;
                      }
                      if (!newPassword.equals(fieldText(confirm))) {
                        confirm.setError("Mật khẩu xác nhận không khớp");
                        return;
                      }
                      AppExecutors.io()
                          .execute(
                              () -> {
                                User admin = repository.getUserByEmail(AdminAuth.EMAIL);
                                if (admin != null)
                                  repository.updateUser(admin.withPassword(newPassword));
                                runOnUiThread(
                                    () -> {
                                      if (admin == null) {
                                        password.setError("Không tìm thấy tài khoản admin");
                                        return;
                                      }
                                      getSharedPreferences("AdminSecurity", MODE_PRIVATE)
                                          .edit()
                                          .putBoolean("admin_password_customized", true)
                                          .apply();
                                      dialog.dismiss();
                                      Toast.makeText(
                                              this,
                                              "Đã cập nhật mật khẩu admin",
                                              Toast.LENGTH_SHORT)
                                          .show();
                                    });
                              });
                    }));
    dialog.show();
  }

  private static String fieldText(TextInputEditText field) {
    return field.getText() == null ? "" : field.getText().toString();
  }

  private void ensureAdminCustomerSession() {
    ensureAdminCustomerSession(null);
  }

  private void ensureAdminCustomerSession(Runnable onReady) {
    AppExecutors.io()
        .execute(
            () -> {
              SessionManager session = new SessionManager(this);
              if (!session.isLoggedIn()) {
                User user = repository.getUserByEmail(AdminAuth.EMAIL);
                if (user == null) {
                  long id =
                      repository.insertUser(
                          new User(
                              0,
                              AdminAuth.EMAIL,
                              "admin-local",
                              "AdminLocalSession@1",
                              "Administrator",
                              System.currentTimeMillis()));
                  user = repository.getUserById(id);
                }
                if (user != null)
                  session.saveSession(
                      user.getId(), "local-admin", user.getEmail(), user.getFullName());
              }
              if (onReady != null) runOnUiThread(onReady);
            });
  }

  private void switchToGuest() {
    ensureAdminCustomerSession(
        () -> {
          Intent intent = new Intent(this, MainActivity.class);
          intent.putExtra("admin_guest_preview", true);
          intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
          startActivity(intent);
          overridePendingTransition(0, 0);
        });
  }

  private void logout() {
    getSharedPreferences("AdminSession", MODE_PRIVATE).edit().clear().apply();
    new SessionManager(this).clearSession();
    Intent i = new Intent(this, LoginActivity.class);
    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(i);
    finish();
  }

  private boolean isAdminLoggedIn() {
    return getSharedPreferences("AdminSession", MODE_PRIVATE)
        .getBoolean("is_admin_logged_in", false);
  }

  private void text(int id, String value) {
    ((TextView) findViewById(id)).setText(value);
  }

  private static String statusLabel(String s) {
    switch (s) {
      case "pending":
        return "Chờ duyệt";
      case "confirmed":
        return "Đã xác nhận";
      case "checked_in":
        return "Đang lưu trú";
      case "completed":
        return "Hoàn thành";
      case "cancelled":
        return "Đã hủy";
      case "expired":
        return "Hết hạn";
      default:
        return s;
    }
  }

  private static int statusColor(String s) {
    switch (s) {
      case "pending":
        return 0xFFA55A00;
      case "confirmed":
        return 0xFF167A50;
      case "checked_in":
        return 0xFF0B4AA2;
      case "completed":
        return 0xFF0B4AA2;
      case "cancelled":
        return 0xFFD14343;
      case "expired":
        return 0xFF64748B;
      default:
        return 0xFF737B8C;
    }
  }
}
