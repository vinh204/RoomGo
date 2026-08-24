package com.example.homestay.ui.admin;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.*;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.*;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.*;
import com.example.homestay.HomestayApplication;
import com.example.homestay.R;
import com.example.homestay.data.entity.*;
import com.example.homestay.data.model.AdminRoomData;
import com.example.homestay.data.repository.HomestayRepository;
import com.example.homestay.utils.*;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import java.text.*;
import java.util.*;

public class AdminRoomsActivity extends AppCompatActivity {
  private HomestayRepository repository;
  private AdminRoomAdapter adapter;
  private ProgressBar progress;
  private final List<AdminRoomData> rooms = new ArrayList<>();
  private List<Booking> bookings = Collections.emptyList();
  private Boolean availableFilter;
  private final List<String> selectedImageUris = new ArrayList<>();
  private Fields activeImageFields;
  private ActivityResultLauncher<String[]> imagePicker;

  @Override
  protected void onCreate(Bundle s) {
    super.onCreate(s);
    imagePicker =
        registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(),
            uris -> {
              if (uris == null || activeImageFields == null) return;
              for (Uri uri : uris) {
                if (selectedImageUris.size() >= 10) break;
                try {
                  getContentResolver()
                      .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {
                }
                String value = uri.toString();
                if (!selectedImageUris.contains(value)) selectedImageUris.add(value);
              }
              renderSelectedImages(activeImageFields);
            });
    setContentView(R.layout.activity_admin_rooms);
    SystemBarUtils.keepContentBelowStatusBar(this);
    repository = ((HomestayApplication) getApplication()).getRepository();
    Toolbar t = findViewById(R.id.toolbar);
    setSupportActionBar(t);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(false);
      getSupportActionBar().setTitle("Quản lý Phòng");
    }
    setup();
    AdminNavigationUtils.setup(this, R.id.admin_nav_rooms);
    load();
  }

  private void setup() {
    progress = findViewById(R.id.progress_bar);
    adapter =
        new AdminRoomAdapter(new ArrayList<>(), this::showEdit, this::confirmDelete, this::details);
    RecyclerView list = findViewById(R.id.rv_rooms);
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
    ChipGroup chips = findViewById(R.id.chip_room_status);
    chips.clearCheck();
    chips.check(R.id.chip_rooms_all);
    chips.setOnCheckedStateChangeListener(
        (g, ids) -> {
          int id = ids.isEmpty() ? View.NO_ID : ids.get(0);
          availableFilter =
              id == R.id.chip_rooms_active
                  ? Boolean.TRUE
                  : id == R.id.chip_rooms_hidden ? Boolean.FALSE : null;
          filter(text(search));
        });
    ((FloatingActionButton) findViewById(R.id.fab_add_room))
        .setOnClickListener(v -> showDialog(null));
  }

  private void load() {
    progress.setVisibility(View.VISIBLE);
    AppExecutors.io()
        .execute(
            () -> {
              bookings = repository.getAllBookingsNow();
              List<AdminRoomData> result = new ArrayList<>();
              for (Room room : repository.getAllRoomsNow()) result.add(toAdmin(room));
              runOnUiThread(
                  () -> {
                    rooms.clear();
                    rooms.addAll(result);
                    filter(text((TextInputEditText) findViewById(R.id.et_admin_search)));
                    progress.setVisibility(View.GONE);
                  });
            });
  }

  private AdminRoomData toAdmin(Room r) {
    int count = 0;
    double revenue = 0;
    for (Booking b : bookings)
      if (b.getRoomId() == r.getId()) {
        count++;
        if ("PAID".equals(b.getPaymentStatus())) revenue += b.getTotalPrice();
      }
    return new AdminRoomData(
        String.valueOf(r.getId()),
        r.getName(),
        r.getDescription(),
        r.getPrice(),
        r.getMaxGuests(),
        r.getImageUrl(),
        r.getMaxSlots(),
        0,
        r.getLocation(),
        r.getAddress(),
        r.getAmenities(),
        r.getRoomType(),
        r.getArea(),
        r.getRating(),
        r.getReviewCount(),
        r.isAvailable(),
        r.isFeatured(),
        count,
        revenue);
  }

  private void filter(String q) {
    if (adapter == null) return;
    String key = q.trim().toLowerCase(Locale.ROOT);
    List<AdminRoomData> out = new ArrayList<>();
    for (AdminRoomData r : rooms)
      if ((availableFilter == null || r.isAvailable() == availableFilter)
          && ((r.getName() + " " + r.getLocation() + " " + r.getRoomType() + " " + r.getAmenities())
              .toLowerCase(Locale.ROOT)
              .contains(key))) out.add(r);
    adapter.updateRooms(out);
    findViewById(R.id.tv_empty).setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
  }

  private void showEdit(AdminRoomData room) {
    showDialog(room);
  }

  private void showDialog(AdminRoomData room) {
    View view = getLayoutInflater().inflate(R.layout.dialog_add_edit_room, null);
    Fields f = new Fields(view);
    activeImageFields = f;
    selectedImageUris.clear();
    ((TextView) view.findViewById(R.id.tv_dialog_title))
        .setText(room == null ? "Thêm phòng mới" : "Chỉnh sửa phòng");
    if (room != null) {
      f.name.setText(room.getName());
      f.description.setText(room.getDescription());
      f.price.setText(String.valueOf(room.getPrice()));
      f.capacity.setText(String.valueOf(room.getCapacity()));
      f.slots.setText(String.valueOf(room.getMaxSlots()));
      f.type.setText(room.getRoomType());
      f.location.setText(room.getLocation());
      f.address.setText(room.getAddress());
      f.area.setText(String.valueOf(room.getArea()));
      f.amenities.setText(room.getAmenities());
      f.available.setChecked(room.isAvailable());
      f.featured.setChecked(room.isFeatured());
      AppExecutors.io()
          .execute(
              () -> {
                List<RoomImage> stored = repository.getRoomImages(Long.parseLong(room.getId()));
                List<String> values = new ArrayList<>();
                for (RoomImage image : stored) values.add(image.getImageUri());
                if (values.isEmpty() && !room.getImageUrl().trim().isEmpty())
                  values.add(room.getImageUrl());
                runOnUiThread(
                    () -> {
                      if (activeImageFields != f) return;
                      selectedImageUris.clear();
                      selectedImageUris.addAll(values);
                      renderSelectedImages(f);
                    });
              });
    }
    renderSelectedImages(f);
    view.findViewById(R.id.btn_select_room_image)
        .setOnClickListener(
            v -> {
              activeImageFields = f;
              imagePicker.launch(new String[] {"image/*"});
            });
    AlertDialog dialog =
        new MaterialAlertDialogBuilder(this).setView(view).setCancelable(true).create();
    dialog.setOnDismissListener(ignored -> {
      if (activeImageFields == f) activeImageFields = null;
    });
    view.findViewById(R.id.btn_cancel)
        .setOnClickListener(
            v -> {
              activeImageFields = null;
              dialog.dismiss();
            });
    view.findViewById(R.id.btn_save).setOnClickListener(v -> save(room, f, dialog));
    dialog.show();
  }

  private void save(AdminRoomData existing, Fields f, AlertDialog dialog) {
    String name = text(f.name).trim(),
        description = text(f.description).trim(),
        image = selectedImageUris.isEmpty() ? "" : selectedImageUris.get(0);
    Double price = number(text(f.price));
    Integer capacity = integer(text(f.capacity)), slots = integer(text(f.slots));
    if (selectedImageUris.isEmpty()) {
      toast("Vui lòng chọn ít nhất một ảnh phòng");
      return;
    }
    if (name.isEmpty() || price == null || capacity == null) {
      toast("Vui lòng điền đầy đủ thông tin");
      return;
    }
    if (price <= 0 || capacity < 1 || capacity > 20 || slots == null || slots < 1 || slots > 20) {
      toast("Giá phải lớn hơn 0; sức chứa và số phòng từ 1 đến 20");
      return;
    }
    int area = integer(text(f.area)) == null ? 0 : integer(text(f.area));
    AppExecutors.io()
        .execute(
            () -> {
              try {
                long roomId;
                if (existing == null) {
                  roomId =
                      repository.insertRoom(
                      new Room(
                          0,
                          null,
                          name,
                          description,
                          price,
                          image,
                          text(f.location),
                          text(f.address),
                          5f,
                          0,
                          blank(text(f.amenities), "WiFi"),
                          capacity,
                          blank(text(f.type), "Homestay"),
                          area,
                          slots,
                          f.available.isChecked(),
                          f.featured.isChecked()));
                } else {
                  roomId = Long.parseLong(existing.getId());
                  Room current = repository.getRoomById(Long.parseLong(existing.getId()));
                  int requiredQuantity = repository.requiredRoomQuantity(roomId);
                  if (slots < requiredQuantity)
                    throw new IllegalArgumentException(
                        "Số lượng phòng không thể nhỏ hơn "
                            + requiredQuantity
                            + " vì đang có booking trùng lịch");
                  if (current != null)
                    repository.updateRoom(
                        current.updated(
                            name,
                            description,
                            price,
                            image,
                            text(f.location),
                            text(f.address),
                            text(f.amenities),
                            capacity,
                            blank(text(f.type), "Homestay"),
                            area,
                            slots,
                            f.available.isChecked(),
                            f.featured.isChecked()));
                }
                repository.replaceRoomImages(roomId, new ArrayList<>(selectedImageUris));
                runOnUiThread(
                    () -> {
                      activeImageFields = null;
                      dialog.dismiss();
                      toast(
                          existing == null
                              ? "Thêm phòng thành công!"
                              : "Cập nhật phòng thành công!");
                      load();
                    });
              } catch (Exception e) {
                runOnUiThread(() -> toast("Lỗi: " + e.getMessage()));
              }
            });
  }

  private void confirmDelete(AdminRoomData room) {
    new MaterialAlertDialogBuilder(this)
        .setTitle("Xóa phòng")
        .setMessage("Bạn có chắc muốn xóa phòng \"" + room.getName() + "\"?")
        .setPositiveButton(
            "Xóa",
            (d, w) ->
                AppExecutors.io()
                    .execute(
                        () -> {
                          Room value = repository.getRoomById(Long.parseLong(room.getId()));
                          if (value != null && repository.roomHasBookings(value.getId())) {
                            runOnUiThread(() -> toast("Không thể xóa phòng đang có booking"));
                            return;
                          }
                          if (value != null) repository.deleteRoom(value);
                          runOnUiThread(
                              () -> {
                                toast("Xóa phòng thành công!");
                                load();
                              });
                        }))
        .setNegativeButton("Hủy", null)
        .show();
  }

  private void details(AdminRoomData r) {
    String price = DisplayFormatter.number(r.getPrice()),
        revenue = DisplayFormatter.number(r.getRevenue());
    new MaterialAlertDialogBuilder(this)
        .setTitle(r.getName())
        .setMessage(
            "Mã phòng: "
                + r.getId()
                + "\nLoại: "
                + r.getRoomType()
                + "\nTrạng thái: "
                + (r.isAvailable() ? "Đang mở" : "Tạm ẩn")
                + "\nGiá: "
                + price
                + " đ/đêm\nĐịa điểm: "
                + r.getLocation()
                + "\nĐịa chỉ: "
                + r.getAddress()
                + "\nDiện tích: "
                + r.getArea()
                + " m²\nSức chứa: "
                + r.getCapacity()
                + " người\nSố lượng phòng: "
                + r.getMaxSlots()
                + "\nĐánh giá: "
                + r.getRating()
                + " sao · "
                + (r.getReviewCount() == 0 ? "Chưa có đánh giá" : r.getReviewCount() + " lượt")
                + "\nTiện nghi: "
                + r.getAmenities()
                + "\n\nBooking: "
                + r.getBookingCount()
                + "\nDoanh thu thực tế: "
                + revenue
                + " đ\n\nMô tả:\n"
                + r.getDescription())
        .setNegativeButton("Đóng", null)
        .setPositiveButton("Chỉnh sửa", (d, w) -> showEdit(r))
        .show();
  }

  private void loadImage(ImageView v, String data) {
    ImageLoader.load(v, data, R.drawable.ic_room_placeholder);
  }

  private void renderSelectedImages(Fields fields) {
    fields.images.removeAllViews();
    fields.imageCount.setText(selectedImageUris.size() + "/10 ảnh");
    if (selectedImageUris.isEmpty()) {
      loadImage(fields.preview, "");
      return;
    }
    loadImage(fields.preview, selectedImageUris.get(0));
    for (int index = 0; index < selectedImageUris.size(); index++) {
      String uri = selectedImageUris.get(index);
      FrameLayout frame = new FrameLayout(this);
      LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(dp(88), dp(88));
      frameParams.setMarginEnd(dp(8));
      frame.setLayoutParams(frameParams);
      ImageView thumbnail = new ImageView(this);
      thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
      thumbnail.setBackgroundColor(getColor(R.color.home_divider));
      frame.addView(thumbnail, new FrameLayout.LayoutParams(-1, -1));
      loadImage(thumbnail, uri);
      TextView remove = new TextView(this);
      remove.setText("×");
      remove.setTextColor(Color.WHITE);
      remove.setTextSize(18);
      remove.setGravity(Gravity.CENTER);
      remove.setBackgroundResource(R.drawable.bg_image_counter);
      FrameLayout.LayoutParams removeParams =
          new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.TOP | Gravity.END);
      frame.addView(remove, removeParams);
      remove.setOnClickListener(
          view -> {
            selectedImageUris.remove(uri);
            renderSelectedImages(fields);
          });
      if (index == 0) {
        TextView cover = new TextView(this);
        cover.setText("Ảnh bìa");
        cover.setTextColor(Color.WHITE);
        cover.setTextSize(10);
        cover.setPadding(dp(6), dp(2), dp(6), dp(2));
        cover.setBackgroundResource(R.drawable.bg_image_counter);
        frame.addView(
            cover,
            new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START));
      }
      frame.setOnClickListener(
          view -> {
            selectedImageUris.remove(uri);
            selectedImageUris.add(0, uri);
            renderSelectedImages(fields);
          });
      fields.images.addView(frame);
    }
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private static String text(TextInputEditText f) {
    return f.getText() == null ? "" : f.getText().toString();
  }

  private static String blank(String s, String fallback) {
    return s.trim().isEmpty() ? fallback : s;
  }

  private static Double number(String s) {
    try {
      return Double.valueOf(s);
    } catch (Exception e) {
      return null;
    }
  }

  private static Integer integer(String s) {
    try {
      return Integer.valueOf(s);
    } catch (Exception e) {
      return null;
    }
  }

  private void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
  }

  private static final class Fields {
    final TextInputEditText name,
        description,
        price,
        capacity,
        slots,
        image,
        type,
        location,
        address,
        area,
        amenities;
    final ImageView preview;
    final LinearLayout images;
    final TextView imageCount;
    final MaterialSwitch available, featured;

    Fields(View v) {
      name = v.findViewById(R.id.et_room_name);
      description = v.findViewById(R.id.et_room_description);
      price = v.findViewById(R.id.et_room_price);
      capacity = v.findViewById(R.id.et_room_capacity);
      slots = v.findViewById(R.id.et_room_max_slots);
      image = v.findViewById(R.id.et_room_image_url);
      preview = v.findViewById(R.id.iv_room_image_preview);
      images = v.findViewById(R.id.room_images_container);
      imageCount = v.findViewById(R.id.tv_room_image_count);
      type = v.findViewById(R.id.et_room_type);
      location = v.findViewById(R.id.et_room_location);
      address = v.findViewById(R.id.et_room_address);
      area = v.findViewById(R.id.et_room_area);
      amenities = v.findViewById(R.id.et_room_amenities);
      available = v.findViewById(R.id.switch_room_available);
      featured = v.findViewById(R.id.switch_room_featured);
    }
  }
}
