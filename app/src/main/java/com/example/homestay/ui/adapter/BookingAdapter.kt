package com.example.homestay.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.homestay.R
import com.example.homestay.data.entity.Booking
import com.example.homestay.data.entity.Room
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

data class BookingWithRoom(
    val booking: Booking,
    val room: Room?
)

class BookingAdapter(
    private val onBookingClick: (BookingWithRoom) -> Unit
) : ListAdapter<BookingWithRoom, BookingAdapter.BookingViewHolder>(BookingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBookingId: TextView = itemView.findViewById(R.id.tv_booking_id)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val tvRoomName: TextView = itemView.findViewById(R.id.tv_room_name)
        private val tvDates: TextView = itemView.findViewById(R.id.tv_dates)
        private val tvGuests: TextView = itemView.findViewById(R.id.tv_guests)
        private val tvTotalPrice: TextView = itemView.findViewById(R.id.tv_total_price)

        fun bind(bookingWithRoom: BookingWithRoom) {
            val booking = bookingWithRoom.booking
            val room = bookingWithRoom.room

            // Booking ID
            tvBookingId.text = "Booking #HV${booking.id.toString().padStart(8, '0')}"

            // Status
            val statusText = when (booking.status) {
                "pending" -> "Chờ xác nhận"
                "confirmed" -> "Đã xác nhận"
                "cancelled" -> "Đã hủy"
                "completed" -> "Hoàn thành"
                else -> booking.status
            }
            tvStatus.text = statusText

            // Status background color
            val statusColor = when (booking.status) {
                "pending" -> "#FF9800" // Orange
                "confirmed" -> "#4CAF50" // Green
                "cancelled" -> "#F44336" // Red
                "completed" -> "#2196F3" // Blue
                else -> "#757575" // Grey
            }
            tvStatus.setBackgroundColor(Color.parseColor(statusColor))

            // Room name
            tvRoomName.text = room?.name ?: "Phòng không tồn tại"

            // Dates
            val dateFormat = SimpleDateFormat("dd/MM", Locale("vi", "VN"))
            val checkInDate = dateFormat.format(java.util.Date(booking.checkInDate))
            val checkOutDate = dateFormat.format(java.util.Date(booking.checkOutDate))
            tvDates.text = "Nhận phòng: $checkInDate · Trả phòng: $checkOutDate"

            // Guests
            tvGuests.text = "${booking.guestCount} người lớn · 1 phòng"

            // Total price (màu đỏ)
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            val formattedPrice = formatter.format(booking.totalPrice.toLong())
            tvTotalPrice.text = "$formattedPrice đ"

            itemView.setOnClickListener {
                onBookingClick(bookingWithRoom)
            }
        }
    }

    class BookingDiffCallback : DiffUtil.ItemCallback<BookingWithRoom>() {
        override fun areItemsTheSame(oldItem: BookingWithRoom, newItem: BookingWithRoom): Boolean {
            return oldItem.booking.id == newItem.booking.id
        }

        override fun areContentsTheSame(oldItem: BookingWithRoom, newItem: BookingWithRoom): Boolean {
            return oldItem == newItem
        }
    }
}

