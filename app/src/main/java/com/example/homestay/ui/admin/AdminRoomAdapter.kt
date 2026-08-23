package com.example.homestay.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.homestay.R
import com.example.homestay.data.model.AdminRoomData
import java.text.NumberFormat
import java.util.Locale

class AdminRoomAdapter(
    private val rooms: MutableList<AdminRoomData>,
    private val onEditClick: (AdminRoomData) -> Unit,
    private val onDeleteClick: (AdminRoomData) -> Unit,
    private val onDetailsClick: (AdminRoomData) -> Unit
) : RecyclerView.Adapter<AdminRoomAdapter.RoomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]
        holder.bind(room)
    }

    override fun getItemCount() = rooms.size

    fun updateRooms(newRooms: List<AdminRoomData>) {
        rooms.clear()
        rooms.addAll(newRooms)
        notifyDataSetChanged()
    }

    fun removeRoom(roomId: String) {
        val index = rooms.indexOfFirst { it.id == roomId }
        if (index != -1) {
            rooms.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRoomName: TextView = itemView.findViewById(R.id.tv_room_name)
        private val tvRoomPrice: TextView = itemView.findViewById(R.id.tv_room_price)
        private val tvRoomLocation: TextView = itemView.findViewById(R.id.tv_room_location)
        private val tvRoomCapacity: TextView = itemView.findViewById(R.id.tv_room_capacity)
        private val tvRoomMaxSlots: TextView = itemView.findViewById(R.id.tv_room_max_slots)
        private val tvRoomMeta: TextView = itemView.findViewById(R.id.tv_room_meta)
        private val tvRoomPerformance: TextView = itemView.findViewById(R.id.tv_room_performance)
        private val ivRoomImage: ImageView = itemView.findViewById(R.id.iv_room_image)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(room: AdminRoomData) {
            itemView.setOnClickListener { onDetailsClick(room) }
            tvRoomName.text = room.name
            tvRoomPrice.text = formatPrice(room.price)
            tvRoomLocation.text = "${room.roomType} • ${room.location.ifBlank { "Chưa có địa điểm" }}"
            tvRoomCapacity.text = "Sức chứa: ${room.capacity} người"
            tvRoomMaxSlots.text = "${room.area} m² • ${room.maxSlots} slot • ${if (room.isAvailable) "Đang mở" else "Tạm ẩn"}"
            tvRoomMeta.text = "★ ${String.format(java.util.Locale.getDefault(), "%.1f", room.rating)} (${room.reviewCount}) • ${room.amenities.ifBlank { "Chưa có tiện nghi" }}"
            tvRoomPerformance.text = "${room.bookingCount} booking • ${formatPrice(room.revenue)} doanh thu"

            // Load image from URL using Coil
            if (room.imageUrl.isNotEmpty()) {
                ivRoomImage.load(room.imageUrl) {
                    placeholder(R.drawable.ic_room_placeholder)
                    error(R.drawable.ic_room_placeholder)
                    crossfade(true) // Smooth transition
                }
            } else {
                // Nếu không có imageUrl, hiển thị placeholder
                ivRoomImage.setImageResource(R.drawable.ic_room_placeholder)
            }

            btnEdit.setOnClickListener {
                onEditClick(room)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(room)
            }
        }

        private fun formatPrice(price: Double): String {
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            return "${formatter.format(price)} VNĐ/đêm"
        }
    }
}

