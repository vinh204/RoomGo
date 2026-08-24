package com.example.homestay.ui.adapter;

import android.view.*;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homestay.R;
import com.example.homestay.utils.ImageLoader;
import java.util.*;

public class RoomImagePagerAdapter extends RecyclerView.Adapter<RoomImagePagerAdapter.Holder> {
  private final List<String> images = new ArrayList<>();

  public void submitList(List<String> values) {
    images.clear();
    images.addAll(values);
    notifyDataSetChanged();
  }

  @Override public int getItemCount() { return images.size(); }

  @Override
  public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
    ImageView image = new ImageView(parent.getContext());
    image.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
    image.setContentDescription("Hình ảnh phòng");
    return new Holder(image);
  }

  @Override
  public void onBindViewHolder(Holder holder, int position) {
    ImageLoader.load(holder.image, images.get(position), R.drawable.ic_room_placeholder);
  }

  static final class Holder extends RecyclerView.ViewHolder {
    final ImageView image;
    Holder(ImageView image) {
      super(image);
      this.image = image;
    }
  }
}
