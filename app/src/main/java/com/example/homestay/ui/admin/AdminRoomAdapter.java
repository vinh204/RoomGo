package com.example.homestay.ui.admin;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homestay.R;
import com.example.homestay.data.model.AdminRoomData;
import com.example.homestay.utils.ImageLoader;
import java.text.*;
import java.util.*;

public class AdminRoomAdapter extends RecyclerView.Adapter<AdminRoomAdapter.Holder> {
  public interface Listener {
    void onRoom(AdminRoomData room);
  }

  private final List<AdminRoomData> items;
  private final Listener edit, delete, details;

  public AdminRoomAdapter(List<AdminRoomData> i, Listener e, Listener d, Listener x) {
    items = i;
    edit = e;
    delete = d;
    details = x;
  }

  public void updateRooms(List<AdminRoomData> v) {
    items.clear();
    items.addAll(v);
    notifyDataSetChanged();
  }

  public void removeRoom(String id) {
    for (int i = 0; i < items.size(); i++)
      if (items.get(i).getId().equals(id)) {
        items.remove(i);
        notifyItemRemoved(i);
        break;
      }
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  @Override
  public Holder onCreateViewHolder(ViewGroup p, int t) {
    return new Holder(
        LayoutInflater.from(p.getContext()).inflate(R.layout.item_admin_room, p, false));
  }

  @Override
  public void onBindViewHolder(Holder h, int p) {
    h.bind(items.get(p));
  }

  public class Holder extends RecyclerView.ViewHolder {
    final TextView name, price, location, capacity, slots, meta, performance, status;
    final ImageView image;
    final ImageButton editButton, deleteButton;

    Holder(View v) {
      super(v);
      name = v.findViewById(R.id.tv_room_name);
      price = v.findViewById(R.id.tv_room_price);
      location = v.findViewById(R.id.tv_room_location);
      capacity = v.findViewById(R.id.tv_room_capacity);
      slots = v.findViewById(R.id.tv_room_max_slots);
      meta = v.findViewById(R.id.tv_room_meta);
      performance = v.findViewById(R.id.tv_room_performance);
      status = v.findViewById(R.id.tv_room_status);
      image = v.findViewById(R.id.iv_room_image);
      editButton = v.findViewById(R.id.btn_edit);
      deleteButton = v.findViewById(R.id.btn_delete);
    }

    void bind(AdminRoomData r) {
      itemView.setOnClickListener(v -> details.onRoom(r));
      name.setText(r.getName());
      price.setText(money(r.getPrice()) + " / đêm");
      location.setText(
          r.getRoomType()
              + " • "
              + (r.getLocation().trim().isEmpty() ? "Chưa có địa điểm" : r.getLocation()));
      capacity.setText("Sức chứa: " + r.getCapacity() + " người");
      slots.setText(
          r.getArea()
              + " m² • "
              + r.getMaxSlots()
              + " slot"
              + (r.isFeatured() ? " • ★ Nổi bật" : ""));
      status.setText(r.isAvailable() ? "Đang mở" : "Tạm ẩn");
      status.setBackgroundTintList(
          ColorStateList.valueOf(Color.parseColor(r.isAvailable() ? "#168A5B" : "#718096")));
      String reviews =
          r.getReviewCount() == 0 ? "Chưa có đánh giá" : r.getReviewCount() + " đánh giá";
      meta.setText(
          "★ "
              + String.format(Locale.getDefault(), "%.1f", r.getRating())
              + " · "
              + reviews
              + " • "
              + (r.getAmenities().trim().isEmpty() ? "Chưa có tiện nghi" : r.getAmenities()));
      performance.setText(
          r.getBookingCount() + " booking • " + money(r.getRevenue()) + " doanh thu");
      ImageLoader.load(image, r.getImageUrl(), R.drawable.ic_room_placeholder);
      editButton.setOnClickListener(v -> edit.onRoom(r));
      deleteButton.setOnClickListener(v -> delete.onRoom(r));
    }
  }

  private static String money(double v) {
    return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(v) + " đ";
  }
}
