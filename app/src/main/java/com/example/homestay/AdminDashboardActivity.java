package com.example.homestay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import com.example.homestay.data.entity.*;
import com.example.homestay.data.repository.HomestayRepository;
import com.example.homestay.utils.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.Executors;

public class AdminDashboardActivity extends AppCompatActivity {
  private HomestayRepository repository;

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
    Executors.newSingleThreadExecutor()
        .execute(
            () -> {
              List<Room> rooms = repository.getAllRoomsNow();
              List<User> users = repository.getAllUsersNow();
              List<Booking> bookings = repository.getAllBookingsNow();
              runOnUiThread(() -> render(rooms, users, bookings));
            });
  }

  private void render(List<Room> rooms, List<User> users, List<Booking> bookings) {
    int pending = 0, confirmed = 0, completed = 0, cancelled = 0, active = 0;
    double expected = 0, actual = 0;
    long now = System.currentTimeMillis();
    Set<Long> occupied = new HashSet<>();
    for (Room r : rooms) if (r.isAvailable()) active++;
    for (Booking b : bookings) {
      switch (b.getStatus()) {
        case "pending":
          pending++;
          break;
        case "confirmed":
          confirmed++;
          break;
        case "completed":
          completed++;
          break;
        case "cancelled":
          cancelled++;
          break;
      }
      if ("confirmed".equals(b.getStatus()) || "completed".equals(b.getStatus()))
        expected += b.getTotalPrice();
      if ("completed".equals(b.getStatus())) actual += b.getTotalPrice();
      if ("confirmed".equals(b.getStatus())
          && b.getCheckInDate() <= now
          && b.getCheckOutDate() >= now) occupied.add(b.getRoomId());
    }
    text(R.id.tv_stat_rooms, rooms.size() + "\nPhòng");
    text(R.id.tv_stat_users, users.size() + "\nNgười dùng");
    text(R.id.tv_stat_pending, pending + "\nChờ duyệt");
    text(R.id.tv_stat_revenue, "Doanh thu dự kiến: " + money(expected) + " đ");
    text(R.id.tv_stat_revenue_actual, "Doanh thu thực tế: " + money(actual) + " đ");
    text(R.id.tv_booking_breakdown, bookings.size() + "\nTổng đặt chỗ");
    text(R.id.tv_booking_completed, completed + "\nHoàn thành");
    text(R.id.tv_booking_cancelled, cancelled + "\nĐã hủy");
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.DAY_OF_MONTH, 1);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    int fresh = 0;
    for (User u : users) if (u.getCreatedAt() >= cal.getTimeInMillis()) fresh++;
    int occupancy = active == 0 ? 0 : occupied.size() * 100 / active;
    text(
        R.id.tv_operational_summary,
        "Phòng đang mở     "
            + active
            + "/"
            + rooms.size()
            + "        Đang lưu trú     "
            + occupied.size()
            + "\nNgười dùng mới     "
            + fresh
            + "        Lấp đầy hiện tại     "
            + occupancy
            + "%");
    LinearLayout container = findViewById(R.id.recent_bookings_container);
    container.removeAllViews();
    bookings.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
    SimpleDateFormat date = new SimpleDateFormat("dd/MM HH:mm", new Locale("vi", "VN")),
        codeDate = new SimpleDateFormat("yyMMdd", Locale.getDefault());
    for (int i = 0; i < Math.min(3, bookings.size()); i++) {
      Booking b = bookings.get(i);
      View card = getLayoutInflater().inflate(R.layout.item_admin_recent_booking, container, false);
      ((TextView) card.findViewById(R.id.tv_recent_code))
          .setText(
              "#RG"
                  + codeDate.format(new Date(b.getCreatedAt()))
                  + String.format(Locale.ROOT, "%03d", b.getId()));
      TextView status = card.findViewById(R.id.tv_recent_status);
      status.setText(statusLabel(b.getStatus()));
      status.setTextColor(statusColor(b.getStatus()));
      ((TextView) card.findViewById(R.id.tv_recent_meta))
          .setText(date.format(new Date(b.getCreatedAt())));
      ((TextView) card.findViewById(R.id.tv_recent_price)).setText(money(b.getTotalPrice()) + " đ");
      container.addView(card);
    }
  }

  private void setupViews() {
    TextView name = findViewById(R.id.tv_admin_name);
    name.setText(
        getSharedPreferences("AdminSession", MODE_PRIVATE)
            .getString("admin_fullname", "Administrator"));
    int[] hidden = {
      R.id.card_manage_rooms,
      R.id.card_view_users,
      R.id.card_manage_bookings,
      R.id.btn_logout,
      R.id.btn_switch_to_guest,
      R.id.tv_management_title
    };
    for (int id : hidden) findViewById(id).setVisibility(View.GONE);
    findViewById(R.id.btn_admin_account)
        .setOnClickListener(
            anchor -> {
              PopupMenu menu = new PopupMenu(this, anchor);
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

  private void ensureAdminCustomerSession() {
    Executors.newSingleThreadExecutor()
        .execute(
            () -> {
              SessionManager session = new SessionManager(this);
              if (session.isLoggedIn()) return;
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
            });
  }

  private void switchToGuest() {
    ensureAdminCustomerSession();
    Intent i = new Intent(this, MainActivity.class);
    i.putExtra("admin_guest_preview", true);
    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(i);
    finish();
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

  private static String money(double v) {
    return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format((long) v);
  }

  private static String statusLabel(String s) {
    switch (s) {
      case "pending":
        return "Chờ duyệt";
      case "confirmed":
        return "Đã xác nhận";
      case "completed":
        return "Hoàn thành";
      case "cancelled":
        return "Đã hủy";
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
      case "completed":
        return 0xFF0B4AA2;
      case "cancelled":
        return 0xFFD14343;
      default:
        return 0xFF737B8C;
    }
  }
}
