package com.example.homestay;

import android.app.DatePickerDialog;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.*;
import androidx.recyclerview.widget.*;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.homestay.data.entity.*;
import com.example.homestay.data.repository.*;
import com.example.homestay.domain.RoomSearchEngine;
import com.example.homestay.domain.BookingCalculator;
import com.example.homestay.ui.RoomDetailActivity;
import com.example.homestay.ui.adapter.*;
import com.example.homestay.utils.*;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends AppCompatActivity {
  private FrameLayout content;
  private BottomNavigationView nav;
  private HomestayApplication app;
  private HomestayRepository repository;
  private AuthRepository authRepository;
  private BookingRepository bookingRepository;
  private SessionManager session;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final Set<Long> favorites = new HashSet<>();
  private boolean accountLockDialogShown;
  private long checkIn = -1, checkOut = -1;
  private int guests = 1;
  private RoomSearchEngine.SortOrder sort = RoomSearchEngine.SortOrder.RECOMMENDED;

  @Override
  protected void onCreate(Bundle s) {
    super.onCreate(s);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_main);
    content = findViewById(R.id.content_container);
    nav = findViewById(R.id.bottom_nav);
    app = (HomestayApplication) getApplicationContext();
    repository = app.getRepository();
    authRepository = app.getAuthRepository();
    bookingRepository = app.getBookingRepository();
    session = new SessionManager(this);
    requestNotificationPermission();
    ViewCompat.setOnApplyWindowInsetsListener(
        findViewById(R.id.main),
        (v, i) -> {
          Insets b = i.getInsets(WindowInsetsCompat.Type.systemBars());
          v.setPadding(b.left, b.top, b.right, 0);
          ViewGroup.LayoutParams p = nav.getLayoutParams();
          p.height = (int) (72 * getResources().getDisplayMetrics().density) + b.bottom;
          nav.setLayoutParams(p);
          nav.setPadding(0, 0, 0, b.bottom);
          return i;
        });
    setupToolbar();
    setupNav();
    executor.execute(
        () -> {
          repository.cleanupDuplicateRooms();
          repository.refreshLocalRooms();
          repository.cleanupDuplicateRooms();
        });
    if ("bookings".equals(getIntent().getStringExtra("open_tab")) && session.isLoggedIn())
      nav.setSelectedItemId(R.id.navigation_booking);
    else {
      load(R.layout.content_search);
      setupSearch();
      nav.setSelectedItemId(R.id.navigation_search);
    }
  }

  private void setupNav() {
    nav.setItemActiveIndicatorEnabled(false);
    nav.setOnItemSelectedListener(
        item -> {
          int id = item.getItemId();
          if (id == R.id.navigation_search) {
            load(R.layout.content_search);
            setupSearch();
            return true;
          }
          if (id == R.id.navigation_saved) {
            if (!session.isLoggedIn()) {
              openLogin("Vui lòng đăng nhập để xem phòng đã lưu");
              return false;
            }
            load(R.layout.content_saved);
            setupSaved();
            return true;
          }
          if (id == R.id.navigation_booking) {
            if (!session.isLoggedIn()) {
              openLogin("Vui lòng đăng nhập để xem đặt chỗ");
              return false;
            }
            load(R.layout.content_bookings);
            setupBookings();
            return true;
          }
          if (id == R.id.navigation_profile) {
            load(R.layout.content_account);
            setupAccount();
            return true;
          }
          return false;
        });
  }

  private void setupToolbar() {
    MaterialToolbar toolbar = findViewById(R.id.toolbar);
    MenuItem item = toolbar.getMenu().findItem(R.id.action_notifications);
    if (item != null && item.getActionView() != null)
      item.getActionView()
          .setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
    toolbar.setOnMenuItemClickListener(
        i -> {
          if (i.getItemId() == R.id.action_notifications) {
            startActivity(new Intent(this, NotificationsActivity.class));
            return true;
          }
          return false;
        });
    refreshBadge();
  }

  private void refreshBadge() {
    long user = session.getUserId();
    if (user == -1) return;
    executor.execute(
        () -> {
          repository.syncBookingNotifications(user);
          List<AppNotification> values = repository.getCustomerNotificationsNow(user);
          SystemNotificationHelper.publishNew(this, values);
          int unread = 0;
          for (AppNotification n : values) if (!n.isRead()) unread++;
          int count = unread;
          runOnUiThread(
              () -> {
                MenuItem item =
                    ((MaterialToolbar) findViewById(R.id.toolbar))
                        .getMenu()
                        .findItem(R.id.action_notifications);
                TextView badge =
                    item == null || item.getActionView() == null
                        ? null
                        : item.getActionView().findViewById(R.id.tv_notification_badge);
                if (badge != null) {
                  badge.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
                  badge.setText(count > 99 ? "99+" : String.valueOf(count));
                }
              });
        });
  }

  private void load(int layout) {
    content.removeAllViews();
    getLayoutInflater().inflate(layout, content, true);
  }

  private void requestNotificationPermission() {
    if (android.os.Build.VERSION.SDK_INT >= 33
        && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
          this, new String[] {Manifest.permission.POST_NOTIFICATIONS}, 1201);
    }
  }

  private void setupSearch() {
    SwipeRefreshLayout swipe = content.findViewById(R.id.swipe_refresh);
    RecyclerView list = content.findViewById(R.id.recycler_rooms),
        featured = content.findViewById(R.id.recycler_featured_rooms);
    TextInputEditText search = content.findViewById(R.id.et_search);
    TextView greeting = content.findViewById(R.id.tv_home_greeting),
        guestText = content.findViewById(R.id.tv_guest_count),
        inText = content.findViewById(R.id.tv_check_in_date),
        outText = content.findViewById(R.id.tv_check_out_date);
    String name = session.getFullName();
    greeting.setText(
        name == null || name.isEmpty() ? "Khám phá nơi lưu trú phù hợp" : "Xin chào, " + name);
    RoomAdapter adapter = roomAdapter();
    RoomAdapter featuredAdapter = roomAdapter();
    list.setLayoutManager(new LinearLayoutManager(this));
    list.setAdapter(adapter);
    featured.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    featured.setAdapter(featuredAdapter);
    Runnable reload = () -> loadRooms(adapter, featuredAdapter, search == null ? "" : text(search));
    swipe.setOnRefreshListener(
        () ->
            executor.execute(
                () -> {
                  repository.refreshLocalRooms();
                  runOnUiThread(
                      () -> {
                        reload.run();
                        swipe.setRefreshing(false);
                      });
                }));
    content.findViewById(R.id.btn_retry_rooms).setOnClickListener(v -> reload.run());
    search.addTextChangedListener(new Watcher(reload));
    guestText.setText(String.valueOf(guests));
    content
        .findViewById(R.id.btn_guest_minus)
        .setOnClickListener(
            v -> {
              guests = Math.max(1, guests - 1);
              guestText.setText(String.valueOf(guests));
              reload.run();
            });
    content
        .findViewById(R.id.btn_guest_plus)
        .setOnClickListener(
            v -> {
              guests = Math.min(10, guests + 1);
              guestText.setText(String.valueOf(guests));
              reload.run();
            });
    SimpleDateFormat df = new SimpleDateFormat("dd/MM", new Locale("vi", "VN"));
    content
        .findViewById(R.id.layout_check_in)
        .setOnClickListener(
            v ->
                pickDate(
                    false,
                    value -> {
                      checkIn = value;
                      if (checkOut <= checkIn) checkOut = -1;
                      inText.setText(df.format(new Date(value)));
                      if (checkOut < 0) outText.setText("Trả phòng");
                      reload.run();
                    }));
    content
        .findViewById(R.id.layout_check_out)
        .setOnClickListener(
            v -> {
              if (checkIn < 0) {
                toast("Vui lòng chọn ngày nhận phòng trước");
                return;
              }
              pickDate(
                  true,
                  value -> {
                    checkOut = value;
                    outText.setText(df.format(new Date(value)));
                    reload.run();
                  });
            });
    content.findViewById(R.id.btn_search).setOnClickListener(v -> reload.run());
    content.findViewById(R.id.btn_sort).setOnClickListener(v -> showSort(reload));
    content
        .findViewById(R.id.btn_clear_filters)
        .setOnClickListener(
            v -> {
              checkIn = checkOut = -1;
              guests = 1;
              sort = RoomSearchEngine.SortOrder.RECOMMENDED;
              search.setText("");
              guestText.setText("1");
              inText.setText("Nhận phòng");
              outText.setText("Trả phòng");
              reload.run();
            });
    reload.run();
  }

  private RoomAdapter roomAdapter() {
    return new RoomAdapter(
        room ->
            startActivity(
                new Intent(this, RoomDetailActivity.class).putExtra("room_id", room.getId())),
        (room, active) -> toggleFavorite(room.getId()),
        favorites::contains);
  }

  private void loadRooms(RoomAdapter adapter, RoomAdapter featured, String query) {
    content.findViewById(R.id.progress_rooms).setVisibility(View.VISIBLE);
    executor.execute(
        () -> {
          List<Room> all = repository.getAllRoomsNow();
          favorites.clear();
          if (session.getUserId() != -1)
            favorites.addAll(repository.getFavoriteRoomIdsNow(session.getUserId()));
          List<Booking> bookings = repository.getAllBookingsNow();
          List<Room> filtered =
              RoomSearchEngine.filter(all, bookings, query, guests, checkIn, checkOut, sort);
          Room special = RoomSearchEngine.featured(all);
          Room finalSpecial = special;
          runOnUiThread(
              () -> {
                adapter.submitList(filtered);
                featured.submitList(
                    finalSpecial == null
                        ? Collections.emptyList()
                        : Collections.singletonList(finalSpecial));
                content
                    .findViewById(R.id.tv_featured_title)
                    .setVisibility(finalSpecial == null ? View.GONE : View.VISIBLE);
                content
                    .findViewById(R.id.recycler_featured_rooms)
                    .setVisibility(finalSpecial == null ? View.GONE : View.VISIBLE);
                ((TextView) content.findViewById(R.id.tv_result_count))
                    .setText(filtered.size() + " phòng phù hợp");
                content
                    .findViewById(R.id.tv_empty_rooms)
                    .setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
                content.findViewById(R.id.progress_rooms).setVisibility(View.GONE);
                adapter.notifyDataSetChanged();
                featured.notifyDataSetChanged();
              });
        });
  }

  private void toggleFavorite(long roomId) {
    long user = session.getUserId();
    if (user == -1) {
      openLogin("Vui lòng đăng nhập để lưu phòng");
      return;
    }
    executor.execute(
        () -> {
          if (repository.isFavorite(user, roomId)) {
            repository.deleteFavorite(user, roomId);
            favorites.remove(roomId);
          } else {
            repository.insertFavorite(new Favorite(0, user, roomId, System.currentTimeMillis()));
            favorites.add(roomId);
          }
          runOnUiThread(
              () -> {
                if (nav.getSelectedItemId() == R.id.navigation_saved) setupSaved();
              });
        });
  }

  private void setupSaved() {
    RecyclerView list = content.findViewById(R.id.recycler_favorites);
    View empty = content.findViewById(R.id.tv_empty_favorites);
    RoomAdapter adapter = roomAdapter();
    list.setLayoutManager(new LinearLayoutManager(this));
    list.setAdapter(adapter);
    executor.execute(
        () -> {
          List<Long> ids = repository.getFavoriteRoomIdsNow(session.getUserId());
          favorites.clear();
          favorites.addAll(ids);
          List<Room> out = new ArrayList<>();
          for (Room r : repository.getAllRoomsNow()) if (ids.contains(r.getId())) out.add(r);
          runOnUiThread(
              () -> {
                adapter.submitList(out);
                empty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
              });
        });
  }

  private void setupBookings() {
    SwipeRefreshLayout swipe = content.findViewById(R.id.swipe_refresh_bookings);
    RecyclerView list = content.findViewById(R.id.recycler_bookings);
    View empty = content.findViewById(R.id.tv_empty_bookings);
    BookingAdapter adapter = new BookingAdapter(this::showBookingDetail);
    list.setLayoutManager(new LinearLayoutManager(this));
    list.setAdapter(adapter);
    Runnable reload =
        () ->
            executor.execute(
                () -> {
                  bookingRepository.syncBookingsFromAPI(
                      session.getMongoUserId(), session.getUserId());
                  Map<Long, Room> rooms = new HashMap<>();
                  for (Room r : repository.getAllRoomsNow()) rooms.put(r.getId(), r);
                  List<BookingWithRoom> out = new ArrayList<>();
                  for (Booking b : repository.getDatabaseBookingsForUser(session.getUserId()))
                    out.add(new BookingWithRoom(b, rooms.get(b.getRoomId())));
                  out.sort(
                      (a, b) -> {
                        boolean ac = "completed".equals(a.getBooking().getStatus()),
                            bc = "completed".equals(b.getBooking().getStatus());
                        if (ac != bc) return ac ? 1 : -1;
                        return Long.compare(
                            b.getBooking().getCreatedAt(), a.getBooking().getCreatedAt());
                      });
                  runOnUiThread(
                      () -> {
                        adapter.submitList(out);
                        empty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
                        swipe.setRefreshing(false);
                        refreshBadge();
                      });
                });
    swipe.setOnRefreshListener(reload::run);
    reload.run();
  }

  private void setupAccount() {
    TextView name = content.findViewById(R.id.tv_user_name),
        email = content.findViewById(R.id.tv_user_email),
        phone = content.findViewById(R.id.tv_user_phone),
        membership = content.findViewById(R.id.tv_membership);
    View phoneRow = content.findViewById(R.id.row_user_phone);
    MaterialButton edit = content.findViewById(R.id.btn_edit_profile),
        logout = content.findViewById(R.id.btn_logout),
        admin = content.findViewById(R.id.btn_back_to_admin);
    boolean preview =
        getSharedPreferences("AdminSession", MODE_PRIVATE).getBoolean("is_admin_logged_in", false);
    if (preview) {
      membership.setText("Quản trị viên");
      edit.setVisibility(View.GONE);
      admin.setVisibility(View.VISIBLE);
      executor.execute(
          () -> {
            User u = repository.getUserById(session.getUserId());
            runOnUiThread(
                () -> {
                  name.setText(u == null ? "Administrator" : u.getFullName());
                  email.setText(u == null ? AdminAuth.EMAIL : u.getEmail());
                  if (u != null && u.getPhone() != null && !u.getPhone().trim().isEmpty()) {
                    phone.setText(u.getPhone());
                    phoneRow.setVisibility(View.VISIBLE);
                  } else {
                    phoneRow.setVisibility(View.GONE);
                  }
                });
          });
    } else if (session.isLoggedIn()) {
      executor.execute(
          () -> {
            User u = repository.getUserById(session.getUserId());
            runOnUiThread(
                () -> {
                  if (u != null) {
                    name.setText(u.getFullName());
                    email.setText(u.getEmail());
                    phone.setText(u.getPhone());
                    phoneRow.setVisibility(View.VISIBLE);
                  }
                });
          });
      membership.setText("Thành viên RoomGo");
      edit.setOnClickListener(v -> showEditProfile(session.getUserId()));
      admin.setVisibility(View.GONE);
    } else {
      name.setText("Khách");
      email.setText("Đăng nhập để tiếp tục");
      phoneRow.setVisibility(View.GONE);
      membership.setText("Chưa đăng nhập");
      edit.setText("Đăng nhập");
      edit.setOnClickListener(v -> openLogin(null));
      admin.setVisibility(View.GONE);
    }
    admin.setOnClickListener(
        v -> {
          Intent i = new Intent(this, AdminDashboardActivity.class);
          i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
          startActivity(i);
          finish();
        });
    logout.setText(preview ? "Đăng xuất admin" : "Đăng xuất");
    logout.setOnClickListener(
        v -> {
          if (preview) getSharedPreferences("AdminSession", MODE_PRIVATE).edit().clear().apply();
          session.clearSession();
          Intent i = new Intent(this, LoginActivity.class);
          i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
          startActivity(i);
          finish();
        });
  }

  private void showEditProfile(long userId) {
    BottomSheetDialog dialog = new BottomSheetDialog(this);
    View v = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
    dialog.setContentView(v);
    TextInputEditText name = v.findViewById(R.id.et_full_name),
        email = v.findViewById(R.id.et_email),
        phone = v.findViewById(R.id.et_phone),
        pass = v.findViewById(R.id.et_password),
        confirm = v.findViewById(R.id.et_confirm_password);
    executor.execute(
        () -> {
          User u = repository.getUserById(userId);
          runOnUiThread(
              () -> {
                if (u != null) {
                  name.setText(u.getFullName());
                  email.setText(u.getEmail());
                  phone.setText(u.getPhone());
                }
              });
        });
    v.findViewById(R.id.btn_cancel).setOnClickListener(x -> dialog.dismiss());
    v.findViewById(R.id.btn_save)
        .setOnClickListener(
            x -> {
              String full = text(name).trim(), password = text(pass);
              if (full.length() < 2) {
                name.setError("Họ tên không hợp lệ");
                return;
              }
              if (!password.isEmpty() && !password.equals(text(confirm))) {
                confirm.setError("Mật khẩu xác nhận không khớp");
                return;
              }
              if (!password.isEmpty() && !InputValidator.isPasswordValid(password)) {
                pass.setError(InputValidator.getPasswordErrorMessage(password));
                return;
              }
              executor.execute(
                  () -> {
                    OperationResult<User> result =
                        authRepository.updateUser(
                            userId,
                            session.getMongoUserId(),
                            full,
                            password.isEmpty() ? null : password);
                    runOnUiThread(
                        () -> {
                          if (result.isSuccess()) {
                            User u = result.getValue();
                            session.saveSession(
                                u.getId(), session.getMongoUserId(), u.getEmail(), u.getFullName());
                            dialog.dismiss();
                            setupAccount();
                            toast("Đã cập nhật thông tin");
                          } else toast(result.getError().getMessage());
                        });
                  });
            });
    dialog.show();
  }

  private void showBookingDetail(BookingWithRoom item) {
    Booking b = item.getBooking();
    Room r = item.getRoom();
    BottomSheetDialog dialog = new BottomSheetDialog(this);
    View v = getLayoutInflater().inflate(R.layout.dialog_booking_detail, null);
    dialog.setContentView(v);
    String status = label(b.getStatus());
    TextView badge = v.findViewById(R.id.tv_detail_status);
    badge.setText(status);
    badge.setBackgroundTintList(ColorStateList.valueOf(statusColor(b.getStatus())));
    String code = DisplayFormatter.bookingCode(b.getId(), b.getCreatedAt());
    set(v, R.id.tv_detail_booking_code, "Mã đặt chỗ " + code);
    set(v, R.id.tv_detail_room_name, r == null ? "Phòng không còn tồn tại" : r.getName());
    set(
        v,
        R.id.tv_detail_location,
        r == null ? "Không có thông tin địa chỉ" : r.getLocation() + " · " + r.getAddress());
    SimpleDateFormat d = new SimpleDateFormat("dd/MM/yyyy · HH:mm", new Locale("vi", "VN")),
        dt = new SimpleDateFormat("dd/MM/yyyy, HH:mm", new Locale("vi", "VN"));
    set(v, R.id.tv_detail_check_in, d.format(new Date(b.getCheckInDate())));
    set(v, R.id.tv_detail_check_out, d.format(new Date(b.getCheckOutDate())));
    set(
        v,
        R.id.tv_detail_nights,
        Math.max(1, BookingCalculator.nights(b.getCheckInDate(), b.getCheckOutDate())) + " đêm");
    set(v, R.id.tv_detail_guests, "Khách lưu trú: " + b.getGuestCount() + " người · 1 phòng");
    set(
        v,
        R.id.tv_detail_payment,
        "Thanh toán: "
            + ("pay_on_site".equals(b.getPaymentMethod())
                ? "Khi nhận phòng"
                : "Chưa chọn phương thức"));
    set(v, R.id.tv_detail_created, "Thời gian đặt: " + dt.format(new Date(b.getCreatedAt())));
    set(v, R.id.tv_detail_total, DisplayFormatter.vnd(b.getTotalPrice()));
    MaterialButton room = v.findViewById(R.id.btn_detail_view_room);
    room.setEnabled(r != null);
    room.setOnClickListener(
        x -> {
          dialog.dismiss();
          startActivity(
              new Intent(this, RoomDetailActivity.class).putExtra("room_id", b.getRoomId()));
        });
    MaterialButton review = v.findViewById(R.id.btn_detail_review);
    review.setVisibility("completed".equals(b.getStatus()) ? View.VISIBLE : View.GONE);
    review.setOnClickListener(
        x -> {
          dialog.dismiss();
          showReview(item);
        });
    MaterialButton cancel = v.findViewById(R.id.btn_detail_cancel_booking);
    boolean cancellable =
        ("pending".equals(b.getStatus()) || "confirmed".equals(b.getStatus()))
            && System.currentTimeMillis() < b.getCheckInDate();
    cancel.setVisibility(cancellable ? View.VISIBLE : View.GONE);
    cancel.setOnClickListener(
        x -> {
          dialog.dismiss();
          showCancelBooking(item);
        });
    TextView cancellation = v.findViewById(R.id.tv_detail_cancellation);
    if ("cancelled".equals(b.getStatus())) {
      cancellation.setVisibility(View.VISIBLE);
      cancellation.setText(
          "Lý do hủy: "
              + (b.getCancellationReason() == null ? "Không có thông tin" : b.getCancellationReason())
              + (b.getCancelledAt() > 0
                  ? "\nThời gian hủy: " + dt.format(new Date(b.getCancelledAt()))
                  : "")
              + "\nKhoản hoàn: "
              + DisplayFormatter.vnd(b.getRefundAmount()));
    }
    v.findViewById(R.id.btn_detail_close).setOnClickListener(x -> dialog.dismiss());
    dialog.show();
  }

  private void showCancelBooking(BookingWithRoom item) {
    Booking booking = item.getBooking();
    long now = System.currentTimeMillis();
    if (!("pending".equals(booking.getStatus()) || "confirmed".equals(booking.getStatus()))
        || now >= booking.getCheckInDate()) {
      toast("Booking này không thể hủy");
      return;
    }
    long remaining = booking.getCheckInDate() - now;
    double refund = remaining >= 24L * 60 * 60 * 1000 ? booking.getTotalPrice() : booking.getTotalPrice() * 0.5;
    String[] reasons = {"Thay đổi kế hoạch", "Đặt nhầm ngày", "Tìm được phòng khác", "Lý do khác"};
    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    int padding = (int) (20 * getResources().getDisplayMetrics().density);
    content.setPadding(padding, 0, padding, 0);
    TextView summary = new TextView(this);
    summary.setText(
        (item.getRoom() == null ? "Phòng đã đặt" : item.getRoom().getName())
            + "\n"
            + DisplayFormatter.bookingCode(booking.getId(), booking.getCreatedAt())
            + "\nKhoản hoàn dự kiến: "
            + DisplayFormatter.vnd(refund));
    summary.setTextSize(15);
    summary.setTextColor(getColor(R.color.home_text));
    content.addView(summary);
    RadioGroup group = new RadioGroup(this);
    for (String reason : reasons) {
      RadioButton option = new RadioButton(this);
      option.setId(View.generateViewId());
      option.setText(reason);
      option.setTextSize(15);
      group.addView(option);
    }
    group.check(group.getChildAt(0).getId());
    content.addView(group);
    TextInputEditText other = new TextInputEditText(this);
    other.setHint("Nhập lý do khác");
    other.setVisibility(View.GONE);
    content.addView(other);
    group.setOnCheckedChangeListener(
        (g, checked) -> other.setVisibility(g.indexOfChild(g.findViewById(checked)) == 3 ? View.VISIBLE : View.GONE));
    new MaterialAlertDialogBuilder(this)
        .setTitle("Hủy đặt chỗ")
        .setView(content)
        .setNegativeButton("Quay lại", null)
        .setPositiveButton(
            "Xác nhận hủy",
            (dialog, which) -> {
              int selected = group.indexOfChild(group.findViewById(group.getCheckedRadioButtonId()));
              String reason = selected == 3 ? text(other).trim() : reasons[Math.max(0, selected)];
              if (reason.isEmpty()) reason = "Lý do khác";
              String finalReason = reason;
              executor.execute(
                  () -> {
                    Booking current = repository.getBookingById(booking.getId());
                    if (current != null)
                      repository.updateBooking(current.cancelled(finalReason, refund, System.currentTimeMillis()));
                    runOnUiThread(
                        () -> {
                          toast("Đã hủy đặt chỗ. Khoản hoàn dự kiến: " + DisplayFormatter.vnd(refund));
                          setupBookings();
                          refreshBadge();
                        });
                  });
            })
        .show();
  }

  private void showReview(BookingWithRoom item) {
    Booking b = item.getBooking();
    View v = getLayoutInflater().inflate(R.layout.dialog_submit_review, null);
    RatingBar rating = v.findViewById(R.id.rating_review);
    TextView label = v.findViewById(R.id.tv_rating_label);
    TextInputEditText comment = v.findViewById(R.id.et_review_comment);
    MaterialButton submit = v.findViewById(R.id.btn_review_submit);
    set(v, R.id.tv_review_room, item.getRoom() == null ? "Phòng đã đặt" : item.getRoom().getName());
    androidx.appcompat.app.AlertDialog dialog =
        new MaterialAlertDialogBuilder(this).setView(v).create();
    rating.setOnRatingBarChangeListener(
        (bar, value, user) -> label.setText(ratingLabel((int) value)));
    executor.execute(
        () -> {
          Review old = repository.getReviewByBooking(b.getId());
          runOnUiThread(
              () -> {
                if (old != null) {
                  rating.setRating(old.getRating());
                  comment.setText(old.getComment());
                  submit.setText("Cập nhật đánh giá");
                }
              });
        });
    v.findViewById(R.id.btn_review_cancel).setOnClickListener(x -> dialog.dismiss());
    submit.setOnClickListener(
        x -> {
          int stars = (int) rating.getRating();
          submit.setEnabled(false);
          executor.execute(
              () -> {
                try {
                  repository.submitReview(b.getId(), b.getUserId(), stars, text(comment));
                  runOnUiThread(
                      () -> {
                        toast("Đã lưu đánh giá của bạn");
                        dialog.dismiss();
                      });
                } catch (Exception e) {
                  runOnUiThread(
                      () -> {
                        submit.setEnabled(true);
                        toast(e.getMessage());
                      });
                }
              });
        });
    dialog.show();
  }

  private void showSort(Runnable reload) {
    RoomSearchEngine.SortOrder[] values = RoomSearchEngine.SortOrder.values();
    String[] labels = new String[values.length];
    int selected = 0;
    for (int i = 0; i < values.length; i++) {
      labels[i] = values[i].label;
      if (values[i] == sort) selected = i;
    }
    new MaterialAlertDialogBuilder(this)
        .setTitle("Sắp xếp phòng")
        .setSingleChoiceItems(
            labels,
            selected,
            (d, w) -> {
              sort = values[w];
              d.dismiss();
              reload.run();
            })
        .show();
  }

  private void pickDate(boolean checkout, LongConsumer action) {
    Calendar c = Calendar.getInstance();
    DatePickerDialog p =
        new DatePickerDialog(
            this,
            (v, y, m, d) -> {
              action.accept(
                  checkout
                      ? BookingTimeUtils.checkOutMillis(y, m, d)
                      : BookingTimeUtils.checkInMillis(y, m, d));
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH));
    p.getDatePicker()
        .setMinDate(checkout ? checkIn + 86400000L : System.currentTimeMillis() - 1000);
    p.show();
  }

  private void openLogin(String message) {
    if (message != null) toast(message);
    startActivity(new Intent(this, LoginActivity.class));
  }

  private static String text(TextInputEditText f) {
    return f.getText() == null ? "" : f.getText().toString();
  }

  private static void set(View v, int id, String s) {
    ((TextView) v.findViewById(id)).setText(s);
  }

  private void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
  }

  private static String label(String s) {
    switch (s) {
      case "pending":
        return "Chờ xác nhận";
      case "confirmed":
        return "Đã xác nhận";
      case "cancelled":
        return "Đã hủy";
      case "completed":
        return "Hoàn thành";
      default:
        return s;
    }
  }

  private static int statusColor(String s) {
    switch (s) {
      case "pending":
        return Color.parseColor("#F59E0B");
      case "confirmed":
        return Color.parseColor("#16A34A");
      case "cancelled":
        return Color.parseColor("#DC2626");
      case "completed":
        return Color.parseColor("#2563EB");
      default:
        return Color.GRAY;
    }
  }

  private static String ratingLabel(int n) {
    return n == 1
        ? "Rất không hài lòng"
        : n == 2 ? "Chưa tốt" : n == 3 ? "Bình thường" : n == 4 ? "Hài lòng" : "Tuyệt vời";
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (enforceAccountLock()) return;
    if (nav != null) {
      int id = nav.getSelectedItemId();
      if (id == R.id.navigation_search) {
        load(R.layout.content_search);
        setupSearch();
      } else if (id == R.id.navigation_saved && session.isLoggedIn()) {
        load(R.layout.content_saved);
        setupSaved();
      } else if (id == R.id.navigation_booking && session.isLoggedIn()) {
        load(R.layout.content_bookings);
        setupBookings();
      } else if (id == R.id.navigation_profile) {
        load(R.layout.content_account);
        setupAccount();
      }
    }
    refreshBadge();
  }

  private boolean enforceAccountLock() {
    boolean adminPreview =
        getSharedPreferences("AdminSession", MODE_PRIVATE).getBoolean("is_admin_logged_in", false);
    if (adminPreview || !session.isLoggedIn() || session.getUserEmail() == null) return false;
    RateLimiter.AttemptStatus status =
        RateLimiter.canAttemptLogin(this, session.getUserEmail());
    if (status.allowed) return false;
    if (!accountLockDialogShown) {
      accountLockDialogShown = true;
      session.clearSession();
      new MaterialAlertDialogBuilder(this)
          .setTitle("Tài khoản đã bị khóa")
          .setMessage(
              "Tài khoản của bạn đã bị quản trị viên khóa. Bạn sẽ được đưa về màn hình đăng nhập.")
          .setCancelable(false)
          .setPositiveButton(
              "Đến trang đăng nhập",
              (dialog, which) -> {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
              })
          .show();
    }
    return true;
  }

  @Override
  protected void onDestroy() {
    executor.shutdownNow();
    super.onDestroy();
  }

  private interface LongConsumer {
    void accept(long value);
  }

  private static class Watcher implements TextWatcher {
    final Runnable r;

    Watcher(Runnable r) {
      this.r = r;
    }

    public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

    public void onTextChanged(CharSequence s, int a, int b, int c) {
      r.run();
    }

    public void afterTextChanged(Editable e) {}
  }
}
