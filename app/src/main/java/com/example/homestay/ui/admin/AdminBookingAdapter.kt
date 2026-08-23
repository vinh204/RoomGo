package com.example.homestay.ui.admin

import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.homestay.R
import com.example.homestay.data.model.AdminBookingData
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminBookingAdapter(
    private val bookings: MutableList<AdminBookingData>,
    private val onChangeStatusClick: (AdminBookingData) -> Unit,
    private val onDeleteClick: (AdminBookingData) -> Unit,
    private val onDetailsClick: (AdminBookingData) -> Unit
) : RecyclerView.Adapter<AdminBookingAdapter.BookingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.bind(booking)
    }

    override fun getItemCount() = bookings.size

    fun updateBookings(newBookings: List<AdminBookingData>) {
        bookings.clear()
        bookings.addAll(newBookings)
        notifyDataSetChanged()
    }

    fun removeBooking(bookingId: String) {
        val index = bookings.indexOfFirst { it.id == bookingId }
        if (index != -1) {
            bookings.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tv_status_badge)
        private val tvBookingId: TextView = itemView.findViewById(R.id.tv_booking_id)
        private val tvRoomName: TextView = itemView.findViewById(R.id.tv_room_name)
        private val tvUserName: TextView = itemView.findViewById(R.id.tv_user_name)
        private val tvDates: TextView = itemView.findViewById(R.id.tv_dates)
        private val tvGuestCount: TextView = itemView.findViewById(R.id.tv_guest_count)
        private val tvTotalPrice: TextView = itemView.findViewById(R.id.tv_total_price)
        private val tvPaymentMethod: TextView = itemView.findViewById(R.id.tv_payment_method)
        private val tvCustomerContact: TextView = itemView.findViewById(R.id.tv_customer_contact)
        private val tvBookingMeta: TextView = itemView.findViewById(R.id.tv_booking_meta)
        private val btnChangeStatus: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btn_change_status)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(booking: AdminBookingData) {
            itemView.setOnClickListener { onDetailsClick(booking) }
            // Status badge
            tvStatusBadge.text = booking.status.uppercase()
            val color = getStatusColor(booking.status)
            tvStatusBadge.background = ColorDrawable(color)
            
            // Booking ID (short)
            tvBookingId.text = "ID: ${booking.id.takeLast(8)}"
            
            // Room name
            tvRoomName.text = booking.room?.name ?: "N/A"
            
            // User name
            tvUserName.text = booking.user?.fullName ?: "N/A"
            
            // Dates
            val checkIn = formatDate(booking.checkInDate)
            val checkOut = formatDate(booking.checkOutDate)
            tvDates.text = "$checkIn - $checkOut"
            
            // Guest count
            tvGuestCount.text = "👥 ${booking.guestCount} người"
            
            // Total price
            tvTotalPrice.text = formatPrice(booking.totalPrice)
            
            // Payment method
            val paymentMethod = when (booking.paymentMethod) {
                "pay_on_site" -> "Thanh toán khi nhận phòng"
                "qr_code" -> "Thanh toán QR"
                else -> "Chưa thanh toán"
            }
            tvPaymentMethod.text = "💳 $paymentMethod"
            tvCustomerContact.text = "✉ ${booking.user?.email ?: "N/A"}  •  ☎ ${booking.user?.phone ?: "N/A"}"
            val nights = ((booking.checkOutDate - booking.checkInDate) / 86_400_000L).coerceAtLeast(1)
            val created = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN")).format(Date(booking.createdAt))
            tvBookingMeta.text = "$nights đêm • Slot: ${booking.slotId ?: "Cả phòng"} • Tạo lúc $created"
            
            // Actions
            btnChangeStatus.setOnClickListener {
                onChangeStatusClick(booking)
            }
            
            btnDelete.setOnClickListener {
                onDeleteClick(booking)
            }
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
            return sdf.format(Date(timestamp))
        }

        private fun formatPrice(price: Double): String {
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            return "${formatter.format(price)} VNĐ"
        }

        private fun getStatusColor(status: String): Int {
            return when (status.lowercase()) {
                "pending" -> 0xFFFF9800.toInt() // Orange
                "confirmed" -> 0xFF4CAF50.toInt() // Green
                "cancelled" -> 0xFFF44336.toInt() // Red
                "completed" -> 0xFF2196F3.toInt() // Blue
                else -> 0xFF9E9E9E.toInt() // Gray
            }
        }
    }
}

