package com.example.homestay.ui.adapter;

import android.view.*;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homestay.R;
import com.example.homestay.data.entity.Review;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.Holder> {
  private List<ReviewDisplayItem> items = Collections.emptyList();

  public void submitList(List<ReviewDisplayItem> value) {
    items = value;
    notifyDataSetChanged();
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  @Override
  public Holder onCreateViewHolder(ViewGroup p, int t) {
    return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_review, p, false));
  }

  @Override
  public void onBindViewHolder(Holder h, int p) {
    h.bind(items.get(p));
  }

  static class Holder extends RecyclerView.ViewHolder {
    Holder(View v) {
      super(v);
    }

    void bind(ReviewDisplayItem item) {
      Review r = item.getReview();
      ((TextView) itemView.findViewById(R.id.tv_reviewer_name)).setText(item.getReviewerName());
      ((TextView) itemView.findViewById(R.id.tv_review_stars))
          .setText(repeat("★", r.getRating()) + repeat("☆", 5 - r.getRating()));
      ((TextView) itemView.findViewById(R.id.tv_review_comment))
          .setText(
              r.getComment().trim().isEmpty() ? "Khách không để lại nhận xét." : r.getComment());
      String edited = r.getUpdatedAt() - r.getCreatedAt() > 1000 ? "\nĐã chỉnh sửa" : "";
      ((TextView) itemView.findViewById(R.id.tv_review_date))
          .setText(
              new SimpleDateFormat("dd/MM/yyyy\nHH:mm", new Locale("vi", "VN"))
                      .format(new Date(r.getUpdatedAt()))
                  + edited);
    }

    static String repeat(String s, int n) {
      StringBuilder b = new StringBuilder();
      for (int i = 0; i < n; i++) b.append(s);
      return b.toString();
    }
  }
}
