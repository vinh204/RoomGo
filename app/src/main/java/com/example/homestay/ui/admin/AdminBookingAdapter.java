package com.example.homestay.ui.admin;

import android.content.res.ColorStateList;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homestay.R;
import com.example.homestay.data.model.*;
import com.example.homestay.utils.DisplayFormatter;
import com.google.android.material.button.MaterialButton;
import java.text.*;
import java.util.*;

public class AdminBookingAdapter extends RecyclerView.Adapter<AdminBookingAdapter.Holder> {
  public interface Listener {
    void onBooking(AdminBookingData booking);
  }

  private final List<AdminBookingData> items;
  private final Listener change, delete, details;

  public AdminBookingAdapter(
      List<AdminBookingData> items, Listener change, Listener delete, Listener details) {
    this.items = items;
    this.change = change;
    this.delete = delete;
    this.details = details;
  }

  public void updateBookings(List<AdminBookingData> value) {
    items.clear();
    items.addAll(value);
    notifyDataSetChanged();
  }

  public void removeBooking(String id) {
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
        LayoutInflater.from(p.getContext()).inflate(R.layout.item_admin_booking, p, false));
  }

  @Override
  public void onBindViewHolder(Holder h, int p) {
    h.bind(items.get(p));
  }

  public class Holder extends RecyclerView.ViewHolder {
    final TextView badge, id, room, user, dates, guests, price, payment, contact, meta;
    final MaterialButton statusButton;
    final ImageButton deleteButton;

    Holder(View v) {
      super(v);
      badge = v.findViewById(R.id.tv_status_badge);
      id = v.findViewById(R.id.tv_booking_id);
      room = v.findViewById(R.id.tv_room_name);
      user = v.findViewById(R.id.tv_user_name);
      dates = v.findViewById(R.id.tv_dates);
      guests = v.findViewById(R.id.tv_guest_count);
      price = v.findViewById(R.id.tv_total_price);
      payment = v.findViewById(R.id.tv_payment_method);
      contact = v.findViewById(R.id.tv_customer_contact);
      meta = v.findViewById(R.id.tv_booking_meta);
      statusButton = v.findViewById(R.id.btn_change_status);
      deleteButton = v.findViewById(R.id.btn_delete);
    }

    void bind(AdminBookingData b) {
      itemView.setOnClickListener(v -> details.onBooking(b));
      badge.setText(statusLabel(b.getStatus()));
      badge.setBackgroundTintList(ColorStateList.valueOf(statusColor(b.getStatus())));
      id.setText(DisplayFormatter.bookingCode(b.getId(), b.getCreatedAt()));
      room.setText(b.getRoom() == null ? "N/A" : b.getRoom().getName());
      user.setText(
          "Khách hàng: " + (b.getUser() == null ? "Chưa xác định" : b.getUser().getFullName()));
      dates.setText(date(b.getCheckInDate()) + " – " + date(b.getCheckOutDate()));
      guests.setText(b.getGuestCount() + " khách");
      price.setText(DisplayFormatter.vnd(b.getTotalPrice()));
      String method =
          "pay_on_site".equals(b.getPaymentMethod())
              ? "Thanh toán khi nhận phòng"
              : "qr_code".equals(b.getPaymentMethod()) ? "Thanh toán QR" : "Chưa thanh toán";
      payment.setText(method);
      contact.setText(
          (b.getUser() == null ? "Chưa có email" : b.getUser().getEmail())
              + " • "
              + (b.getUser() == null ? "Chưa có số điện thoại" : b.getUser().getPhone()));
      long nights =
          Math.max(
              1,
              com.example.homestay.domain.BookingCalculator.nights(
                  b.getCheckInDate(), b.getCheckOutDate()));
      String created =
          new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"))
              .format(new Date(b.getCreatedAt()));
      meta.setText(
          nights
              + " đêm • "
              + (b.getSlotId() == null ? "Cả phòng" : "Slot " + b.getSlotId())
              + " • Tạo lúc "
              + created);
      statusButton.setOnClickListener(v -> change.onBooking(b));
      deleteButton.setOnClickListener(v -> delete.onBooking(b));
    }
  }

  private static String date(long t) {
    return new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN")).format(new Date(t));
  }

  private static String statusLabel(String s) {
    switch (s.toLowerCase()) {
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
    switch (s.toLowerCase()) {
      case "pending":
        return 0xffff9800;
      case "confirmed":
        return 0xff4caf50;
      case "cancelled":
        return 0xfff44336;
      case "completed":
        return 0xff2196f3;
      default:
        return 0xff9e9e9e;
    }
  }
}
