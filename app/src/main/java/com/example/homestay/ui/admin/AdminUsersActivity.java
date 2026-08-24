package com.example.homestay.ui.admin;

import android.os.Bundle;
import android.text.*;
import android.util.Patterns;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.*;
import com.example.homestay.HomestayApplication;
import com.example.homestay.R;
import com.example.homestay.data.entity.*;
import com.example.homestay.data.model.AdminUserData;
import com.example.homestay.data.repository.HomestayRepository;
import com.example.homestay.utils.*;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.text.*;
import java.util.*;

public class AdminUsersActivity extends AppCompatActivity {
  private HomestayRepository repository;
  private AdminUserAdapter adapter;
  private ProgressBar progress;
  private final List<AdminUserData> users = new ArrayList<>();
  private Boolean lockedFilter;

  @Override
  protected void onCreate(Bundle s) {
    super.onCreate(s);
    setContentView(R.layout.activity_admin_users);
    SystemBarUtils.keepContentBelowStatusBar(this);
    repository = ((HomestayApplication) getApplication()).getRepository();
    Toolbar t = findViewById(R.id.toolbar);
    setSupportActionBar(t);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(false);
      getSupportActionBar().setTitle("Quản lý người dùng");
    }
    setup();
    AdminNavigationUtils.setup(this, R.id.admin_nav_users);
    load();
  }

  private void setup() {
    progress = findViewById(R.id.progress_bar);
    adapter =
        new AdminUserAdapter(
            new ArrayList<>(), this::confirmDelete, this::details, this::edit);
    RecyclerView list = findViewById(R.id.rv_users);
    list.setLayoutManager(new LinearLayoutManager(this));
    list.setAdapter(adapter);
    TextInputEditText search = findViewById(R.id.et_admin_search);
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

          public void onTextChanged(CharSequence s, int a, int b, int c) {
            filter(s.toString());
          }

          public void afterTextChanged(Editable e) {}
        });
    ((ChipGroup) findViewById(R.id.chip_user_status))
        .setOnCheckedStateChangeListener(
            (g, ids) -> {
              int id = ids.isEmpty() ? View.NO_ID : ids.get(0);
              lockedFilter =
                  id == R.id.chip_users_active
                      ? Boolean.FALSE
                      : id == R.id.chip_users_locked ? Boolean.TRUE : null;
              filter(text(search));
            });
  }

  private void load() {
    progress.setVisibility(View.VISIBLE);
    AppExecutors.io()
        .execute(
            () -> {
              List<Booking> all = repository.getAllBookingsNow();
              List<AdminUserData> result = new ArrayList<>();
              for (User u : repository.getAllUsersNow()) {
                int count = 0;
                double spent = 0;
                Long last = null;
                for (Booking b : all)
                  if (b.getUserId() == u.getId()) {
                    count++;
                    if ("completed".equals(b.getStatus())) spent += b.getTotalPrice();
                    if (last == null || b.getCreatedAt() > last) last = b.getCreatedAt();
                  }
                RateLimiter.AttemptStatus can = RateLimiter.canAttemptLogin(this, u.getEmail());
                int failed = RateLimiter.getFailedAttempts(this, u.getEmail());
                long seconds = RateLimiter.getLockedSecondsRemaining(this, u.getEmail());
                result.add(
                    new AdminUserData(
                        String.valueOf(u.getId()),
                        u.getEmail(),
                        u.getPhone(),
                        u.getFullName(),
                        u.getCreatedAt(),
                        failed,
                        !can.allowed,
                        !can.allowed && seconds > 365L * 24 * 3600,
                        can.lockedUntil,
                        seconds,
                        count,
                        spent,
                        last));
              }
              runOnUiThread(
                  () -> {
                    users.clear();
                    users.addAll(result);
                    filter(text((TextInputEditText) findViewById(R.id.et_admin_search)));
                    progress.setVisibility(View.GONE);
                  });
            });
  }

  private void filter(String q) {
    if (adapter == null) return;
    String key = q.trim().toLowerCase(Locale.ROOT);
    List<AdminUserData> out = new ArrayList<>();
    for (AdminUserData u : users)
      if ((lockedFilter == null || Boolean.TRUE.equals(u.getLocked()) == lockedFilter)
          && ((u.getFullName() + " " + u.getEmail() + " " + u.getPhone())
              .toLowerCase(Locale.ROOT)
              .contains(key))) out.add(u);
    adapter.updateUsers(out);
    findViewById(R.id.tv_empty).setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
  }

  private void details(AdminUserData u) {
    AppExecutors.io()
        .execute(
            () -> {
              Map<Long, Room> rooms = new HashMap<>();
              for (Room r : repository.getAllRoomsNow()) rooms.put(r.getId(), r);
              List<Booking> history = new ArrayList<>();
              for (Booking b : repository.getAllBookingsNow())
                if (String.valueOf(b.getUserId()).equals(u.getId())) history.add(b);
              history.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
              SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
              StringBuilder h = new StringBuilder();
              for (int i = 0; i < Math.min(10, history.size()); i++) {
                Booking b = history.get(i);
                Room room = rooms.get(b.getRoomId());
                h.append("• ")
                    .append(room == null ? "Phòng #" + b.getRoomId() : room.getName())
                    .append(" — ")
                    .append(date.format(new Date(b.getCheckInDate())))
                    .append(" — ")
                    .append(b.getStatus())
                    .append('\n');
              }
              if (h.length() == 0) h.append("Chưa có lịch sử đặt phòng");
              String spent =
                  NumberFormat.getNumberInstance(new Locale("vi", "VN"))
                      .format((long) u.getTotalSpent());
              runOnUiThread(
                  () ->
                      new MaterialAlertDialogBuilder(this)
                          .setTitle(u.getFullName())
                          .setMessage(
                              "Mã người dùng: "
                                  + u.getId()
                                  + "\nEmail: "
                                  + u.getEmail()
                                  + "\nĐiện thoại: "
                                  + u.getPhone()
                                  + "\nNgày tham gia: "
                                  + date.format(new Date(u.getCreatedAt()))
                                  + "\nTrạng thái: "
                                  + (Boolean.TRUE.equals(u.getLocked()) ? "Bị khóa" : "Hoạt động")
                                  + "\nTổng booking: "
                                  + u.getBookingCount()
                                  + "\nTổng đã chi: "
                                  + spent
                                  + " đ\n\nLịch sử gần đây:\n"
                                  + h)
                          .setPositiveButton("Đóng", null)
                          .show());
            });
  }

  private void edit(AdminUserData data) {
    if (data.getEmail().equalsIgnoreCase(AdminAuth.EMAIL)) {
      toast("Không thể chỉnh sửa tài khoản quản trị hệ thống");
      return;
    }
    View v = getLayoutInflater().inflate(R.layout.dialog_admin_edit_user, null);
    TextInputEditText name = v.findViewById(R.id.et_admin_user_name),
        email = v.findViewById(R.id.et_admin_user_email),
        phone = v.findViewById(R.id.et_admin_user_phone),
        pass = v.findViewById(R.id.et_admin_user_password);
    MaterialCheckBox locked = v.findViewById(R.id.cb_admin_user_locked);
    name.setText(data.getFullName());
    email.setText(data.getEmail());
    phone.setText(data.getPhone());
    locked.setChecked(Boolean.TRUE.equals(data.getLocked()));
    androidx.appcompat.app.AlertDialog dialog =
        new MaterialAlertDialogBuilder(this).setView(v).create();
    v.findViewById(R.id.btn_cancel).setOnClickListener(x -> dialog.dismiss());
    v.findViewById(R.id.btn_save)
        .setOnClickListener(
            x -> {
              String n = text(name).trim(),
                  e = text(email).trim().toLowerCase(Locale.ROOT),
                  p = text(phone).trim(),
                  pw = text(pass);
              if (n.length() < 2) {
                name.setError("Họ tên phải có ít nhất 2 ký tự");
                return;
              }
              if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
                email.setError("Email không hợp lệ");
                return;
              }
              if (!p.matches("^[0-9+]{8,15}$")) {
                phone.setError("Số điện thoại không hợp lệ");
                return;
              }
              if (!pw.isEmpty() && pw.length() < 8) {
                pass.setError("Mật khẩu phải có ít nhất 8 ký tự");
                return;
              }
              AppExecutors.io()
                  .execute(
                      () -> {
                        try {
                          User current = repository.getUserById(Long.parseLong(data.getId()));
                          User dupeEmail = repository.getUserByEmail(e),
                              dupePhone = repository.getUserByPhone(p);
                          if (dupeEmail != null && dupeEmail.getId() != current.getId()) {
                            runOnUiThread(() -> email.setError("Email đã được sử dụng"));
                            return;
                          }
                          if (dupePhone != null && dupePhone.getId() != current.getId()) {
                            runOnUiThread(() -> phone.setError("Số điện thoại đã được sử dụng"));
                            return;
                          }
                          repository.updateUser(
                              current.updated(n, e, p, pw.isEmpty() ? current.getPassword() : pw));
                          if (!data.getEmail().equalsIgnoreCase(e))
                            RateLimiter.reset(this, data.getEmail());
                          boolean wasLocked = Boolean.TRUE.equals(data.getLocked());
                          if (locked.isChecked()) {
                            RateLimiter.lock(this, e);
                            if (!wasLocked)
                              repository.insertNotification(
                                  new AppNotification(
                                      0,
                                      current.getId(),
                                      "account_locked_" + System.currentTimeMillis(),
                                      "Tài khoản đã bị khóa",
                                      "Tài khoản RoomGo của bạn đã bị quản trị viên khóa.",
                                      "account",
                                      null,
                                      null,
                                      false,
                                      System.currentTimeMillis()));
                          } else {
                            RateLimiter.reset(this, e);
                          }
                          runOnUiThread(
                              () -> {
                                dialog.dismiss();
                                toast("Cập nhật người dùng thành công");
                                load();
                              });
                        } catch (Exception ex) {
                          runOnUiThread(() -> toast("Không thể cập nhật: " + ex.getMessage()));
                        }
                      });
            });
    dialog.show();
  }

  private void confirmDelete(AdminUserData u) {
    new MaterialAlertDialogBuilder(this)
        .setTitle("Xóa user")
        .setMessage("Bạn có chắc muốn xóa \"" + u.getFullName() + "\"?")
        .setPositiveButton(
            "Xóa",
            (d, w) ->
                AppExecutors.io()
                    .execute(
                        () -> {
                          User value = repository.getUserById(Long.parseLong(u.getId()));
                          if (value != null && repository.userHasBookings(value.getId())) {
                            runOnUiThread(() -> toast("Không thể xóa người dùng đang có booking"));
                            return;
                          }
                          if (value != null) repository.deleteUser(value);
                          runOnUiThread(
                              () -> {
                                adapter.removeUser(u.getId());
                                toast("Xóa user thành công!");
                              });
                        }))
        .setNegativeButton("Hủy", null)
        .show();
  }

  private static String text(TextInputEditText f) {
    return f.getText() == null ? "" : f.getText().toString();
  }

  private void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
  }
}
