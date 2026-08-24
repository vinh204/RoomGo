package com.example.homestay.ui.admin;

import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.*;
import com.example.homestay.HomestayApplication;
import com.example.homestay.R;
import com.example.homestay.data.entity.*;
import com.example.homestay.data.model.*;
import com.example.homestay.data.repository.HomestayRepository;
import com.example.homestay.utils.*;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.text.*;
import java.util.*;

public class AdminBookingsActivity extends AppCompatActivity {
  public static final String EXTRA_BOOKING_CODE = "booking_code";

  private HomestayRepository repository;
  private AdminBookingAdapter adapter;
  private ProgressBar progress;
  private final List<AdminBookingData> bookings = new ArrayList<>();
  private String selectedStatus;

  @Override
  protected void onCreate(Bundle s) {
    super.onCreate(s);
    setContentView(R.layout.activity_admin_bookings);
    SystemBarUtils.keepContentBelowStatusBar(this);
    repository = ((HomestayApplication) getApplication()).getRepository();
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(false);
      getSupportActionBar().setTitle("Quản lý đặt chỗ");
    }
    setup();
    String bookingCode = getIntent().getStringExtra(EXTRA_BOOKING_CODE);
    if (bookingCode != null)
      ((TextInputEditText) findViewById(R.id.et_admin_search)).setText(bookingCode);
    AdminNavigationUtils.setup(this, R.id.admin_nav_bookings);
    load();
  }

  private void setup() {
    progress = findViewById(R.id.progress_bar);
    adapter =
        new AdminBookingAdapter(
            new ArrayList<>(), this::showStatus, this::showDelete, this::showDetails);
    RecyclerView list = findViewById(R.id.rv_bookings);
    list.setLayoutManager(new LinearLayoutManager(this));
    list.setAdapter(adapter);
    TextInputEditText search = findViewById(R.id.et_admin_search);
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

          public void onTextChanged(CharSequence s, int a, int b, int c) {
            filter();
          }

          public void afterTextChanged(Editable e) {}
        });
    ((ChipGroup) findViewById(R.id.chip_booking_status))
        .setOnCheckedStateChangeListener(
            (g, ids) -> {
              int id = ids.isEmpty() ? View.NO_ID : ids.get(0);
              selectedStatus =
                  id == R.id.chip_pending
                      ? "pending"
                      : id == R.id.chip_confirmed
                          ? "confirmed"
                          : id == R.id.chip_completed
                              ? "completed"
                              : id == R.id.chip_cancelled ? "cancelled" : null;
              filter();
            });
  }

  private void load() {
    progress.setVisibility(View.VISIBLE);
    AppExecutors.io()
        .execute(
            () -> {
              try {
                Map<Long, Room> rooms = new HashMap<>();
                for (Room r : repository.getAllRoomsNow()) rooms.put(r.getId(), r);
                Map<Long, User> users = new HashMap<>();
                for (User u : repository.getAllUsersNow()) users.put(u.getId(), u);
                List<AdminBookingData> result = new ArrayList<>();
                for (Booking b : repository.getAllBookingsNow()) {
                  User u = users.get(b.getUserId());
                  Room r = rooms.get(b.getRoomId());
                  result.add(
                      new AdminBookingData(
                          String.valueOf(b.getId()),
                          u == null
                              ? null
                              : new AdminBookingUser(
                                  String.valueOf(u.getId()),
                                  u.getEmail(),
                                  u.getFullName(),
                                  u.getPhone()),
                          r == null
                              ? null
                              : new AdminBookingRoom(
                                  String.valueOf(r.getId()), r.getName(), r.getPrice()),
                          b.getCheckInDate(),
                          b.getCheckOutDate(),
                          b.getGuestCount(),
                          b.getTotalPrice(),
                          b.getStatus(),
                          b.getPaymentMethod(),
                          b.getCreatedAt(),
                          b.getSlotId() == null ? null : String.valueOf(b.getSlotId())));
                }
                runOnUiThread(
                    () -> {
                      bookings.clear();
                      bookings.addAll(result);
                      filter();
                      progress.setVisibility(View.GONE);
                    });
              } catch (Exception e) {
                runOnUiThread(
                    () -> {
                      progress.setVisibility(View.GONE);
                      toast("Lỗi: " + e.getMessage());
                    });
              }
            });
  }

  private void filter() {
    if (adapter == null) return;
    String query =
        ((TextInputEditText) findViewById(R.id.et_admin_search)).getText() == null
            ? ""
            : ((TextInputEditText) findViewById(R.id.et_admin_search))
                .getText()
                .toString()
                .trim()
                .toLowerCase(Locale.ROOT);
    List<AdminBookingData> out = new ArrayList<>();
    for (AdminBookingData b : bookings) {
      String value = DisplayFormatter.bookingCode(b.getId(), b.getCreatedAt());
      String user =
          b.getUser() == null ? "" : b.getUser().getFullName() + " " + b.getUser().getEmail();
      String room = b.getRoom() == null ? "" : b.getRoom().getName();
      if ((query.isEmpty()
              || (user + " " + room + " " + value)
                  .toLowerCase(Locale.ROOT)
                  .contains(query.replace("#", "")))
          && (selectedStatus == null || selectedStatus.equals(b.getStatus()))) out.add(b);
    }
    adapter.updateBookings(out);
    findViewById(R.id.tv_empty).setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
  }

  private void showStatus(AdminBookingData b) {
    List<String> values = new ArrayList<>(), labels = new ArrayList<>();
    if ("pending".equalsIgnoreCase(b.getStatus())) {
      values.add("confirmed");
      labels.add("Đã xác nhận");
      values.add("cancelled");
      labels.add("Đã hủy");
    } else if ("confirmed".equalsIgnoreCase(b.getStatus())) {
      values.add("completed");
      labels.add("Hoàn thành");
      values.add("cancelled");
      labels.add("Đã hủy");
    }
    if (values.isEmpty()) {
      toast("Booking đã ở trạng thái cuối");
      return;
    }
    new MaterialAlertDialogBuilder(this)
        .setTitle("Đổi trạng thái booking")
        .setSingleChoiceItems(
            labels.toArray(new String[0]),
            -1,
            (d, w) -> {
              if ("cancelled".equals(values.get(w))) showAdminCancellation(b);
              else updateStatus(b.getId(), values.get(w));
              d.dismiss();
            })
        .setNegativeButton("Hủy", null)
        .show();
  }

  private void showAdminCancellation(AdminBookingData data) {
    TextInputEditText reason = new TextInputEditText(this);
    reason.setHint("Lý do hủy từ quản trị viên");
    reason.setText("Phòng không thể tiếp nhận khách");
    int padding = (int) (24 * getResources().getDisplayMetrics().density);
    android.widget.FrameLayout wrapper = new android.widget.FrameLayout(this);
    wrapper.setPadding(padding, 0, padding, 0);
    wrapper.addView(reason);
    new MaterialAlertDialogBuilder(this)
        .setTitle("Hủy booking")
        .setMessage("Khách hàng sẽ nhận được thông báo kèm lý do và khoản hoàn dự kiến.")
        .setView(wrapper)
        .setNegativeButton("Quay lại", null)
        .setPositiveButton(
            "Xác nhận hủy",
            (dialog, which) -> {
              String value =
                  reason.getText() == null ? "Hủy bởi quản trị viên" : reason.getText().toString().trim();
              if (value.isEmpty()) value = "Hủy bởi quản trị viên";
              cancelByAdmin(data.getId(), value);
            })
        .show();
  }

  private void cancelByAdmin(String id, String reason) {
    AppExecutors.io()
        .execute(
            () -> {
              Booking booking = repository.getBookingById(Long.parseLong(id));
              if (booking != null) {
                long remaining = booking.getCheckInDate() - System.currentTimeMillis();
                double refund =
                    remaining >= 24L * 60 * 60 * 1000
                        ? booking.getTotalPrice()
                        : remaining > 0 ? booking.getTotalPrice() * 0.5 : 0;
                repository.updateBooking(
                    booking.cancelled(
                        "Quản trị viên: " + reason, refund, System.currentTimeMillis()));
              }
              runOnUiThread(
                  () -> {
                    toast("Đã hủy booking và gửi thông báo cho khách");
                    load();
                  });
            });
  }

  private void updateStatus(String id, String status) {
    AppExecutors.io()
        .execute(
            () -> {
              Booking b = repository.getBookingById(Long.parseLong(id));
              if (b != null) repository.updateBooking(b.withStatus(status, b.getPaymentMethod()));
              runOnUiThread(
                  () -> {
                    toast("Cập nhật trạng thái thành công!");
                    load();
                  });
            });
  }

  private void showDelete(AdminBookingData b) {
    new MaterialAlertDialogBuilder(this)
        .setTitle("Xóa booking")
        .setMessage("Bạn có chắc chắn muốn xóa booking này?")
        .setPositiveButton(
            "Xóa",
            (d, w) ->
                AppExecutors.io()
                    .execute(
                        () -> {
                          Booking value = repository.getBookingById(Long.parseLong(b.getId()));
                          if (value != null) repository.deleteBooking(value);
                          runOnUiThread(
                              () -> {
                                adapter.removeBooking(b.getId());
                                toast("Xóa booking thành công!");
                              });
                        }))
        .setNegativeButton("Hủy", null)
        .show();
  }

  private void showDetails(AdminBookingData b) {
    SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
    long nights =
        Math.max(1, com.example.homestay.domain.BookingCalculator.nights(b.getCheckInDate(), b.getCheckOutDate()));
    String room = b.getRoom() == null ? "N/A" : b.getRoom().getName(),
        user = b.getUser() == null ? "N/A" : b.getUser().getFullName(),
        email = b.getUser() == null ? "N/A" : b.getUser().getEmail(),
        phone = b.getUser() == null ? "N/A" : b.getUser().getPhone();
    new MaterialAlertDialogBuilder(this)
        .setTitle("Chi tiết " + DisplayFormatter.bookingCode(b.getId(), b.getCreatedAt()))
        .setMessage(
            "Phòng: "
                + room
                + "\nKhách: "
                + user
                + "\nEmail: "
                + email
                + "\nĐiện thoại: "
                + phone
                + "\n\nNhận phòng: "
                + f.format(new Date(b.getCheckInDate()))
                + "\nTrả phòng: "
                + f.format(new Date(b.getCheckOutDate()))
                + "\nThời gian lưu trú: "
                + nights
                + " đêm\nSố khách: "
                + b.getGuestCount()
                + "\n\nTổng tiền: "
                + NumberFormat.getNumberInstance(new Locale("vi", "VN"))
                    .format((long) b.getTotalPrice())
                + " đ\nTrạng thái: "
                + b.getStatus())
        .setPositiveButton("Đóng", null)
        .show();
  }

  private void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
  }
}
