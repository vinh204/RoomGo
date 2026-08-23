package com.example.homestay.ui.adapter;

import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homestay.R;
import com.example.homestay.data.entity.Room;
import com.example.homestay.utils.ImageLoader;
import java.text.*;
import java.util.*;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.Holder> {
  public interface RoomClick {
    void onRoom(Room room);
  }

  public interface FavoriteClick {
    void onFavorite(Room room, boolean active);
  }

  public interface FavoriteStatus {
    boolean isFavorite(long roomId);
  }

  private final RoomClick roomClick;
  private final FavoriteClick favoriteClick;
  private final FavoriteStatus favoriteStatus;
  private List<Room> items = Collections.emptyList();

  public RoomAdapter(RoomClick c, FavoriteClick f, FavoriteStatus s) {
    roomClick = c;
    favoriteClick = f;
    favoriteStatus = s;
  }

  public void submitList(List<Room> v) {
    items = v;
    notifyDataSetChanged();
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  @Override
  public Holder onCreateViewHolder(ViewGroup p, int t) {
    return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_room, p, false));
  }

  @Override
  public void onBindViewHolder(Holder h, int p) {
    h.bind(items.get(p));
  }

  public class Holder extends RecyclerView.ViewHolder {
    final ImageView image;
    final ImageButton favorite;
    final TextView name, location, rating, reviews, type, tag1, tag2, price, capacity;

    Holder(View v) {
      super(v);
      image = v.findViewById(R.id.img_room);
      favorite = v.findViewById(R.id.btn_favorite);
      name = v.findViewById(R.id.tv_room_name);
      location = v.findViewById(R.id.tv_location);
      rating = v.findViewById(R.id.tv_rating);
      reviews = v.findViewById(R.id.tv_review_summary);
      type = v.findViewById(R.id.tv_room_type);
      tag1 = v.findViewById(R.id.tv_amenities_tag);
      tag2 = v.findViewById(R.id.tv_amenities_tag_second);
      price = v.findViewById(R.id.tv_price);
      capacity = v.findViewById(R.id.tv_capacity);
    }

    void bind(Room r) {
      name.setText(r.getName());
      location.setText(r.getLocation());
      rating.setText(String.format(Locale.getDefault(), "%.1f", r.getRating()));
      reviews.setText(
          r.getReviewCount() == 0 ? "· Chưa có đánh giá" : "(" + r.getReviewCount() + ")");
      type.setText(r.getRoomType());
      type.setVisibility(r.getRoomType().isEmpty() ? View.GONE : View.VISIBLE);
      List<String> tags = tags(r.getAmenities());
      tag(tag1, tags.size() > 0 ? tags.get(0) : null);
      tag(tag2, tags.size() > 1 ? tags.get(1) : null);
      price.setText(
          NumberFormat.getNumberInstance(new Locale("vi", "VN")).format((long) r.getPrice())
              + " đ / đêm");
      capacity.setText("Tối đa " + r.getMaxGuests() + " khách · Còn phòng");
      int fallback = R.drawable.room_dalat;
      String data = r.getImageUrl().trim().isEmpty() ? null : r.getImageUrl();
      ImageLoader.load(image, data, fallback);
      boolean active = favoriteStatus != null && favoriteStatus.isFavorite(r.getId());
      setFavorite(active);
      favorite.setOnClickListener(
          v -> {
            boolean next = favoriteStatus == null || !favoriteStatus.isFavorite(r.getId());
            setFavorite(next);
            if (favoriteClick != null) favoriteClick.onFavorite(r, next);
          });
      itemView.setOnClickListener(v -> roomClick.onRoom(r));
    }

    void setFavorite(boolean active) {
      favorite.setImageResource(
          active ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
      favorite.setColorFilter(active ? Color.parseColor("#FFD700") : Color.WHITE);
    }

    void tag(TextView v, String s) {
      v.setText(s == null ? "" : s);
      v.setVisibility(s == null || s.isEmpty() ? View.GONE : View.VISIBLE);
    }
  }

  private static List<String> tags(String text) {
    List<String> out = new ArrayList<>();
    for (String raw : text.split(",")) {
      String s = raw.trim();
      if (!s.isEmpty()) {
        out.add(s.length() > 15 ? s.substring(0, 12) + "..." : s);
        if (out.size() == 2) break;
      }
    }
    return out;
  }
}
