package com.example.homestay.ui.adapter;

import android.view.*;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homestay.R;
import com.example.homestay.data.entity.Slot;
import java.util.*;

public class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.Holder> {
  private List<Slot> items = Collections.emptyList();

  public void submitList(List<Slot> value) {
    items = value;
    notifyDataSetChanged();
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  @Override
  public Holder onCreateViewHolder(ViewGroup p, int t) {
    return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_slot, p, false));
  }

  @Override
  public void onBindViewHolder(Holder h, int p) {
    h.bind(items.get(p));
  }

  public static class Holder extends RecyclerView.ViewHolder {
    final TextView name, number, status;

    Holder(View v) {
      super(v);
      name = v.findViewById(R.id.tv_slot_name);
      number = v.findViewById(R.id.tv_slot_number);
      status = v.findViewById(R.id.tv_slot_status);
    }

    void bind(Slot s) {
      name.setText(s.getSlotName());
      number.setText("Slot #" + s.getSlotNumber());
      status.setText(s.isAvailable() ? "Có sẵn" : "Đã đặt");
      status.setTextColor(
          itemView
              .getContext()
              .getColor(s.isAvailable() ? R.color.home_primary : android.R.color.darker_gray));
    }
  }
}
