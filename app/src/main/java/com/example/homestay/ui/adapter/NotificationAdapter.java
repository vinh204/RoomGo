package com.example.homestay.ui.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.*;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homestay.R;
import com.example.homestay.data.entity.AppNotification;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.*;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.Holder> {
  public interface Listener {
    void onNotification(AppNotification value);
  }

  private final Listener listener;
  private List<AppNotification> items = Collections.emptyList();

  public NotificationAdapter(Listener l) {
    listener = l;
  }

  public void submitList(List<AppNotification> value) {
    items = value;
    notifyDataSetChanged();
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  @Override
  public Holder onCreateViewHolder(ViewGroup p, int t) {
    return new Holder(
        LayoutInflater.from(p.getContext()).inflate(R.layout.item_notification, p, false));
  }

  @Override
  public void onBindViewHolder(Holder h, int p) {
    h.bind(items.get(p));
  }

  class Holder extends RecyclerView.ViewHolder {
    Holder(View v) {
      super(v);
    }

    void bind(AppNotification n) {
      ((TextView) itemView.findViewById(R.id.tv_notification_title)).setText(n.getTitle());
      ((TextView) itemView.findViewById(R.id.tv_notification_message)).setText(n.getMessage());
      ((TextView) itemView.findViewById(R.id.tv_notification_time))
          .setText(
              new SimpleDateFormat("dd/MM/yyyy · HH:mm", new Locale("vi", "VN"))
                  .format(new Date(n.getCreatedAt())));
      String icon =
          "confirmed".equals(n.getType())
              ? "✓"
              : "cancelled".equals(n.getType()) ? "×" : "completed".equals(n.getType()) ? "★" : "⌛";
      ((TextView) itemView.findViewById(R.id.tv_notification_icon)).setText(icon);
      itemView
          .findViewById(R.id.view_unread_dot)
          .setVisibility(n.isRead() ? View.GONE : View.VISIBLE);
      ((MaterialCardView) itemView.findViewById(R.id.card_notification))
          .setBackgroundTintList(
              ColorStateList.valueOf(Color.parseColor(n.isRead() ? "#FFFFFF" : "#EDF5FF")));
      itemView.setOnClickListener(v -> listener.onNotification(n));
    }
  }
}
