package com.example.homestay.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.*;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.*;
import androidx.recyclerview.widget.*;
import androidx.viewpager2.widget.ViewPager2;
import com.example.homestay.*;
import com.example.homestay.data.entity.*;
import com.example.homestay.data.model.CreateBookingRequest;
import com.example.homestay.data.repository.*;
import com.example.homestay.domain.BookingCalculator;
import com.example.homestay.domain.BookingRules;
import com.example.homestay.ui.adapter.*;
import com.example.homestay.utils.DisplayFormatter;
import com.example.homestay.utils.BookingTimeUtils;
import com.example.homestay.utils.ImageLoader;
import com.example.homestay.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public class RoomDetailActivity extends AppCompatActivity {
  private HomestayApplication app;
  private HomestayRepository repository;
  private BookingRepository bookingRepository;
  private SessionManager session;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private long roomId = -1;
  private Room currentRoom;
  private SlotAdapter slotAdapter;
  private ReviewAdapter reviewAdapter;
  private RoomImagePagerAdapter imageAdapter;
  private List<ReviewDisplayItem> reviews = Collections.emptyList();
  private boolean showAllReviews;

  @Override
  protected void onCreate(Bundle s) {
    super.onCreate(s);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_room_detail);
    ViewCompat.setOnApplyWindowInsetsListener(
        findViewById(R.id.main),
        (v, i) -> {
          Insets b = i.getInsets(WindowInsetsCompat.Type.systemBars());
          v.setPadding(b.left, b.top, b.right, b.bottom);
          return i;
        });
    app = (HomestayApplication) getApplicationContext();
    repository = app.getRepository();
    bookingRepository = app.getBookingRepository();
    session = new SessionManager(this);
    roomId = getIntent().getLongExtra("room_id", -1);
    if (roomId == -1) {
      finish();
      return;
    }
    ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
    setupLists();
    setupImagePager();
    findViewById(R.id.btn_book).setOnClickListener(v -> book());
    loadData();
  }

  private void setupLists() {
    slotAdapter = new SlotAdapter();
    RecyclerView slots = findViewById(R.id.recycler_slots);
    slots.setLayoutManager(new LinearLayoutManager(this));
    slots.setAdapter(slotAdapter);
    reviewAdapter = new ReviewAdapter();
    RecyclerView list = findViewById(R.id.recycler_reviews);
    list.setLayoutManager(new LinearLayoutManager(this));
    list.setAdapter(reviewAdapter);
    findViewById(R.id.btn_show_all_reviews)
        .setOnClickListener(
            v -> {
              showAllReviews = !showAllReviews;
              renderReviews();
            });
  }

  private void setupImagePager() {
    imageAdapter = new RoomImagePagerAdapter();
    ViewPager2 pager = findViewById(R.id.pager_room_images);
    pager.setAdapter(imageAdapter);
    pager.registerOnPageChangeCallback(
        new ViewPager2.OnPageChangeCallback() {
          @Override
          public void onPageSelected(int position) {
            updateImagePosition(position, imageAdapter.getItemCount());
          }
        });
  }

  private void loadData() {
    executor.execute(
        () -> {
          Room room = repository.getRoomById(roomId);
          List<RoomImage> roomImages = repository.getRoomImages(roomId);
          List<Slot> slots = repository.getSlotsByRoomIdNow(roomId);
          List<Review> values = repository.getReviewsByRoomNow(roomId);
          Map<Long, User> users = new HashMap<>();
          for (User u : repository.getAllUsersNow()) users.put(u.getId(), u);
          List<ReviewDisplayItem> display = new ArrayList<>();
          for (Review r : values) {
            User u = users.get(r.getUserId());
            display.add(
                new ReviewDisplayItem(
                    r,
                    u == null || u.getFullName().trim().isEmpty()
                        ? "Khách RoomGo"
                        : u.getFullName()));
          }
          runOnUiThread(
              () -> {
                if (room != null) displayRoom(room, roomImages);
                slotAdapter.submitList(slots);
                reviews = display;
                renderReviews();
                double avg = 5;
                for (Review r : values) avg += r.getRating();
                if (!values.isEmpty()) avg = (avg - 5) / values.size();
                ((TextView) findViewById(R.id.tv_reviews_summary))
                    .setText(
                        values.isEmpty()
                            ? "5.0 ★ · Chưa có đánh giá"
                            : String.format(
                                Locale.getDefault(), "%.1f ★ · %d đánh giá", avg, values.size()));
                findViewById(R.id.tv_reviews_empty)
                    .setVisibility(values.isEmpty() ? View.VISIBLE : View.GONE);
              });
        });
  }

  private void renderReviews() {
    reviewAdapter.submitList(
        showAllReviews
            ? reviews
            : new ArrayList<>(reviews.subList(0, Math.min(3, reviews.size()))));
    MaterialButton b = findViewById(R.id.btn_show_all_reviews);
    b.setVisibility(reviews.size() > 3 ? View.VISIBLE : View.GONE);
    b.setText(showAllReviews ? "Thu gọn đánh giá" : "Xem tất cả " + reviews.size() + " đánh giá");
  }

  private void displayRoom(Room r, List<RoomImage> storedImages) {
    currentRoom = r;
    List<String> images = new ArrayList<>();
    for (RoomImage image : storedImages) images.add(image.getImageUri());
    if (images.isEmpty()) images.add(r.getImageUrl());
    imageAdapter.submitList(images);
    updateImagePosition(0, images.size());
    text(R.id.tv_room_name, r.getName());
    text(R.id.tv_location, r.getLocation());
    text(R.id.tv_rating, String.format(Locale.getDefault(), "%.1f", r.getRating()));
    text(
        R.id.tv_review_count,
        r.getReviewCount() == 0 ? "Chưa có đánh giá" : "(" + r.getReviewCount() + " đánh giá)");
    text(R.id.tv_room_type, r.getRoomType());
    text(R.id.tv_description, r.getDescription());
    text(R.id.tv_address, r.getAddress());
    ChipGroup group = findViewById(R.id.chip_group_amenities);
    group.removeAllViews();
    for (String raw : r.getAmenities().split(",")) {
      String value = raw.trim();
      if (value.isEmpty()) continue;
      Chip chip = new Chip(this);
      chip.setText(value);
      chip.setCheckable(false);
      chip.setClickable(false);
      chip.setChipBackgroundColor(
          ColorStateList.valueOf(ContextCompat.getColor(this, R.color.home_background)));
      chip.setTextColor(ContextCompat.getColor(this, R.color.home_primary));
      chip.setChipStrokeWidth(0);
      group.addView(chip);
    }
    text(R.id.tv_area, r.getArea() + " m²");
    text(R.id.tv_max_guests, r.getMaxGuests() + " người");
    String price = DisplayFormatter.vnd(r.getPrice()) + " / đêm";
    text(R.id.tv_price, price);
    text(R.id.tv_bottom_price, price);
    TextView status = findViewById(R.id.tv_availability_status);
    status.setText(r.isAvailable() ? "Còn phòng" : "Đã kín");
    status.setTextColor(
        getColor(
            r.isAvailable() ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
    status.setBackgroundResource(
        r.isAvailable() ? R.drawable.bg_available_chip : R.drawable.bg_unavailable_chip);
    MaterialButton book = findViewById(R.id.btn_book);
    book.setEnabled(r.isAvailable());
    book.setText(r.isAvailable() ? "Đặt ngay" : "Không còn phòng");
  }

  private void updateImagePosition(int position, int count) {
    text(R.id.tv_image_counter, (position + 1) + "/" + Math.max(1, count));
    StringBuilder dots = new StringBuilder();
    for (int i = 0; i < count; i++) {
      if (i > 0) dots.append(' ');
      dots.append(i == position ? '●' : '○');
    }
    text(R.id.tv_image_dots, dots.toString());
    findViewById(R.id.tv_image_dots).setVisibility(count > 1 ? View.VISIBLE : View.GONE);
  }

  private void book() {
    long user = session.getUserId();
    if (user == -1) {
      toast("Vui lòng đăng nhập để đặt phòng");
      startActivity(new Intent(this, LoginActivity.class));
      return;
    }
    if (currentRoom == null) {
      toast("Thông tin phòng không hợp lệ");
      return;
    }
    showBookingDialog(currentRoom, user);
  }

  private void showBookingDialog(Room room, long userId) {
    BottomSheetDialog dialog = new BottomSheetDialog(this);
    View v = getLayoutInflater().inflate(R.layout.dialog_booking, null);
    v.setBackgroundColor(Color.WHITE);
    dialog.setContentView(v);
    TextView roomName = v.findViewById(R.id.tv_room_name),
        roomPrice = v.findViewById(R.id.tv_room_price),
        inText = v.findViewById(R.id.tv_check_in_date),
        outText = v.findViewById(R.id.tv_check_out_date),
        summary = v.findViewById(R.id.tv_stay_summary),
        total = v.findViewById(R.id.tv_total_price),
        slotInfo = v.findViewById(R.id.tv_slot_info);
    TextInputEditText guests = v.findViewById(R.id.et_guest_count);
    MaterialButton minus = v.findViewById(R.id.btn_guest_minus),
        plus = v.findViewById(R.id.btn_guest_plus),
        confirm = v.findViewById(R.id.btn_confirm_booking);
    roomName.setText(room.getName());
    roomPrice.setText(DisplayFormatter.vnd(room.getPrice()) + " / đêm");
    long[] dates = {-1, -1};
    Slot[] selected = {null};
    SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM", new Locale("vi", "VN"));
    Runnable update =
        () -> {
          if (dates[0] >= 0 && dates[1] > dates[0]) {
            long nights = BookingCalculator.nights(dates[0], dates[1]);
            double pp = BookingCalculator.nightlyPrice(room, selected[0]);
            total.setText(
                DisplayFormatter.vnd(
                    BookingCalculator.total(room, selected[0], dates[0], dates[1])));
            summary.setText(nights + " đêm × " + DisplayFormatter.vnd(pp));
            executor.execute(
                () -> {
                  int available =
                      Math.max(
                          0,
                          room.getMaxSlots()
                              - repository.countOverlappingBookings(
                                  room.getId(), dates[0], dates[1]));
                  runOnUiThread(
                      () -> {
                        slotInfo.setText(available + "/" + room.getMaxSlots() + " slot");
                        slotInfo.setVisibility(View.VISIBLE);
                        slotInfo.setTextColor(
                            getColor(
                                available == 0
                                    ? android.R.color.holo_red_dark
                                    : available < Math.max(1, room.getMaxSlots() / 2)
                                        ? android.R.color.holo_orange_dark
                                        : R.color.home_primary));
                      });
                });
          } else {
            total.setText("0 đ");
            slotInfo.setVisibility(View.GONE);
          }
        };
    minus.setOnClickListener(x -> changeGuests(guests, -1, room.getMaxGuests()));
    plus.setOnClickListener(x -> changeGuests(guests, 1, room.getMaxGuests()));
    SlotSelectionAdapter selection =
        new SlotSelectionAdapter(
            slot -> {
              selected[0] = slot;
              update.run();
            });
    RecyclerView slots = v.findViewById(R.id.recycler_slot_selection);
    slots.setLayoutManager(new LinearLayoutManager(this));
    slots.setAdapter(selection);
    executor.execute(
        () -> {
          List<Slot> values = repository.getAvailableSlotsByRoomIdNow(room.getId());
          runOnUiThread(() -> selection.submitList(values));
        });
    Calendar now = Calendar.getInstance();
    v.findViewById(R.id.layout_check_in)
        .setOnClickListener(
            x -> {
              DatePickerDialog picker =
                  new DatePickerDialog(
                      this,
                      (p, y, m, d) -> {
                        Calendar c = Calendar.getInstance();
                        dates[0] = BookingTimeUtils.checkInMillis(y, m, d);
                        c.setTimeInMillis(dates[0]);
                        if (dates[1] <= dates[0]) dates[1] = -1;
                        inText.setText(df.format(c.getTime()));
                        update.run();
                      },
                      now.get(Calendar.YEAR),
                      now.get(Calendar.MONTH),
                      now.get(Calendar.DAY_OF_MONTH));
              picker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
              picker.show();
            });
    v.findViewById(R.id.layout_check_out)
        .setOnClickListener(
            x -> {
              if (dates[0] < 0) {
                toast("Vui lòng chọn ngày nhận phòng trước");
                return;
              }
              Calendar c = Calendar.getInstance();
              DatePickerDialog picker =
                  new DatePickerDialog(
                      this,
                      (p, y, m, d) -> {
                        Calendar value = Calendar.getInstance();
                        dates[1] = BookingTimeUtils.checkOutMillis(y, m, d);
                        value.setTimeInMillis(dates[1]);
                        outText.setText(df.format(value.getTime()));
                        update.run();
                      },
                      c.get(Calendar.YEAR),
                      c.get(Calendar.MONTH),
                      c.get(Calendar.DAY_OF_MONTH));
              picker.getDatePicker().setMinDate(dates[0] + BookingCalculator.MILLIS_PER_DAY);
              picker.show();
            });
    guests.addTextChangedListener(new SimpleWatcher(update));
    confirm.setOnClickListener(
        x -> confirmBooking(dialog, confirm, room, userId, guests, dates, selected));
    dialog.show();
  }

  private void confirmBooking(
      BottomSheetDialog dialog,
      MaterialButton button,
      Room room,
      long userId,
      TextInputEditText guests,
      long[] dates,
      Slot[] selected) {
    int count = parseInt(text(guests), 1);
    if (dates[0] < 0) {
      toast("Vui lòng chọn ngày nhận phòng");
      return;
    }
    String validation =
        BookingRules.validate(
            dates[0], dates[1], count, room.getMaxGuests(), 0, room.getMaxSlots(), startOfToday());
    if (validation != null) {
      toast(validation);
      return;
    }
    button.setEnabled(false);
    button.setText("Đang xử lý...");
    executor.execute(
        () -> {
          int occupied = repository.countOverlappingBookings(room.getId(), dates[0], dates[1]);
          String availabilityValidation =
              BookingRules.validate(
                  dates[0],
                  dates[1],
                  count,
                  room.getMaxGuests(),
                  occupied,
                  room.getMaxSlots(),
                  startOfToday());
          if (availabilityValidation != null) {
            runOnUiThread(
                () -> {
                  button.setEnabled(true);
                  button.setText("Xác nhận đặt phòng");
                  toast(availabilityValidation);
                });
            return;
          }
          String mongo = session.getMongoUserId();
          if (mongo == null) {
            runOnUiThread(
                () -> {
                  button.setEnabled(true);
                  toast("Vui lòng đăng nhập lại để đặt phòng");
                });
            return;
          }
          double totalPrice = BookingCalculator.total(room, selected[0], dates[0], dates[1]);
          CreateBookingRequest request =
              new CreateBookingRequest(
                  String.valueOf(room.getId()),
                  dates[0],
                  dates[1],
                  count,
                  totalPrice,
                  "pending",
                  null,
                  selected[0] == null ? null : String.valueOf(selected[0].getId()));
          OperationResult<BookingRepository.BookingData> result =
              bookingRepository.createBooking(
                  mongo, userId, room.getId(), String.valueOf(room.getId()), request);
          runOnUiThread(
              () -> {
                if (result.isSuccess()) {
                  dialog.dismiss();
                  showPaymentDialog(result.getValue().getBooking());
                } else {
                  button.setEnabled(true);
                  button.setText("Xác nhận đặt phòng");
                  toast(
                      "Không thể đặt phòng: "
                          + (result.getError() == null
                              ? "Lỗi không xác định"
                              : result.getError().getMessage()));
                }
              });
        });
  }

  private void showPaymentDialog(Booking booking) {
    BottomSheetDialog dialog = new BottomSheetDialog(this);
    View v = getLayoutInflater().inflate(R.layout.dialog_payment, null);
    v.setBackgroundColor(Color.WHITE);
    dialog.setContentView(v);
    boolean[] confirmed = {false};
    dialog.setOnDismissListener(
        x -> {
          if (!confirmed[0]) executor.execute(() -> repository.deleteBooking(booking));
        });
    TextView info = v.findViewById(R.id.tv_booking_info),
        qr = v.findViewById(R.id.tv_qr_maintenance),
        total = v.findViewById(R.id.tv_total_payment);
    MaterialButton button = v.findViewById(R.id.btn_confirm_payment);
    SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy · HH:mm", new Locale("vi", "VN"));
    if (currentRoom != null)
      info.setText(
          currentRoom.getName()
              + "\n"
              + f.format(new Date(booking.getCheckInDate()))
              + " - "
              + f.format(new Date(booking.getCheckOutDate()))
              + "\n"
              + booking.getGuestCount()
              + " người");
    total.setText(DisplayFormatter.vnd(booking.getTotalPrice()));
    RadioGroup methods = v.findViewById(R.id.rg_payment_method);
    methods.setOnCheckedChangeListener(
        (g, id) -> {
          boolean maintenance = id == R.id.rb_qr_code;
          qr.setVisibility(maintenance ? View.VISIBLE : View.GONE);
          button.setEnabled(!maintenance);
          button.setText(maintenance ? "QR đang bảo trì" : "Xác nhận thanh toán");
        });
    button.setOnClickListener(
        x -> {
          button.setEnabled(false);
          button.setText("Đang xử lý...");
          executor.execute(
              () -> {
                Booking updated = booking.withStatus("pending", "pay_on_site");
                repository.updateBooking(updated);
                runOnUiThread(
                    () -> {
                      confirmed[0] = true;
                      toast("Đặt phòng thành công! Vui lòng thanh toán khi đến nơi.");
                      dialog.dismiss();
                      showSuccess(updated);
                    });
              });
        });
    dialog.show();
  }

  private void showSuccess(Booking b) {
    String code = DisplayFormatter.bookingCode(b.getId(), b.getCreatedAt());
    new MaterialAlertDialogBuilder(this)
        .setTitle("Đặt phòng thành công")
        .setMessage(
            "Mã đặt chỗ: "
                + code
                + "\nTrạng thái: Chờ xác nhận\nPhương thức: Thanh toán khi nhận phòng")
        .setPositiveButton(
            "Xem đặt chỗ",
            (d, w) -> {
              startActivity(new Intent(this, MainActivity.class).putExtra("open_tab", "bookings"));
              finish();
            })
        .setNegativeButton("Về trang chủ", (d, w) -> finish())
        .setCancelable(false)
        .show();
  }

  private void changeGuests(TextInputEditText f, int delta, int max) {
    f.setText(String.valueOf(Math.max(1, Math.min(max, parseInt(text(f), 1) + delta))));
  }

  private static int parseInt(String s, int fallback) {
    try {
      return Integer.parseInt(s);
    } catch (Exception e) {
      return fallback;
    }
  }

  private static long startOfToday() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTimeInMillis();
  }

  private void text(int id, String s) {
    ((TextView) findViewById(id)).setText(s);
  }

  private static String text(TextInputEditText f) {
    return f.getText() == null ? "" : f.getText().toString();
  }

  private void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
  }

  @Override
  protected void onDestroy() {
    executor.shutdownNow();
    super.onDestroy();
  }

  private static class SimpleWatcher implements TextWatcher {
    final Runnable r;

    SimpleWatcher(Runnable r) {
      this.r = r;
    }

    public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

    public void onTextChanged(CharSequence s, int a, int b, int c) {}

    public void afterTextChanged(Editable e) {
      r.run();
    }
  }
}
