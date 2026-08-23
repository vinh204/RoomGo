package com.example.homestay.ui.admin;

import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homestay.R;
import com.example.homestay.data.model.AdminUserData;
import java.text.*;
import java.util.*;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.Holder> {
  public interface Listener {
    void onUser(AdminUserData user);
  }

  private final List<AdminUserData> items;
  private final Listener delete, unlock, details, edit;

  public AdminUserAdapter(List<AdminUserData> i, Listener d, Listener u, Listener x, Listener e) {
    items = i;
    delete = d;
    unlock = u;
    details = x;
    edit = e;
  }

  public void updateUsers(List<AdminUserData> v) {
    items.clear();
    items.addAll(v);
    notifyDataSetChanged();
  }

  public void removeUser(String id) {
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
        LayoutInflater.from(p.getContext()).inflate(R.layout.item_admin_user, p, false));
  }

  @Override
  public void onBindViewHolder(Holder h, int p) {
    h.bind(items.get(p));
  }

  public class Holder extends RecyclerView.ViewHolder {
    final TextView avatar, name, email, phone, metrics, created, lock;
    final ImageButton unlockButton, editButton, deleteButton;

    Holder(View v) {
      super(v);
      avatar = v.findViewById(R.id.tv_avatar);
      name = v.findViewById(R.id.tv_user_name);
      email = v.findViewById(R.id.tv_user_email);
      phone = v.findViewById(R.id.tv_user_phone);
      metrics = v.findViewById(R.id.tv_user_metrics);
      created = v.findViewById(R.id.tv_user_created);
      lock = v.findViewById(R.id.tv_lock_status);
      unlockButton = v.findViewById(R.id.btn_unlock);
      editButton = v.findViewById(R.id.btn_edit);
      deleteButton = v.findViewById(R.id.btn_delete);
    }

    void bind(AdminUserData u) {
      itemView.setOnClickListener(v -> details.onUser(u));
      avatar.setText(
          u.getFullName().isEmpty()
              ? "U"
              : u.getFullName().substring(0, 1).toUpperCase(Locale.ROOT));
      name.setText(u.getFullName());
      email.setText(u.getEmail());
      phone.setText(u.getPhone());
      metrics.setText(
          u.getBookingCount()
              + " đặt chỗ • Đã chi "
              + NumberFormat.getNumberInstance(new Locale("vi", "VN"))
                  .format((long) u.getTotalSpent())
              + " đ");
      DateFormat f = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
      created.setText(
          "Tham gia: "
              + f.format(new Date(u.getCreatedAt()))
              + (u.getLastBookingAt() == null
                  ? ""
                  : " • Gần nhất: " + f.format(new Date(u.getLastBookingAt()))));
      boolean locked = Boolean.TRUE.equals(u.getLocked()),
          permanent = Boolean.TRUE.equals(u.getPermanent());
      Integer failed = u.getFailedLoginAttempts();
      if (locked) {
        if (permanent) {
          lock.setText("Bị khóa vĩnh viễn");
          lock.setTextColor(0xffd32f2f);
        } else {
          long seconds = u.getSecondsRemaining() == null ? 0 : u.getSecondsRemaining();
          lock.setText(
              "Bị khóa • Còn lại "
                  + (seconds / 60 > 0
                      ? seconds / 60 + " phút " + seconds % 60 + " giây"
                      : seconds + " giây"));
          lock.setTextColor(0xffff5722);
        }
        lock.setVisibility(View.VISIBLE);
        unlockButton.setVisibility(View.VISIBLE);
      } else if (failed != null && failed > 0) {
        lock.setText(failed + " lần đăng nhập sai");
        lock.setTextColor(0xffff9800);
        lock.setVisibility(View.VISIBLE);
        unlockButton.setVisibility(View.VISIBLE);
      } else {
        lock.setVisibility(View.GONE);
        unlockButton.setVisibility(View.GONE);
      }
      unlockButton.setOnClickListener(v -> unlock.onUser(u));
      editButton.setOnClickListener(v -> edit.onUser(u));
      deleteButton.setOnClickListener(v -> delete.onUser(u));
    }
  }
}
