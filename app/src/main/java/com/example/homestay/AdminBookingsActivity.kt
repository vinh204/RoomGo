package com.example.homestay

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.chip.ChipGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homestay.data.model.AdminBookingData
import com.example.homestay.data.model.AdminBookingRoom
import com.example.homestay.data.model.AdminBookingUser
import com.example.homestay.ui.admin.AdminBookingAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class AdminBookingsActivity : AppCompatActivity() {
    private val repository by lazy { (application as HomestayApplication).repository }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminBookingAdapter
    private lateinit var progressBar: ProgressBar
    private var bookings = mutableListOf<AdminBookingData>()
    private var selectedStatus: String? = null
    private var hasPaymentMethod: Boolean? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_bookings)
        
        setupToolbar()
        setupViews()
        loadBookings()
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Quản lý Bookings"
    }
    
    private fun setupViews() {
        recyclerView = findViewById(R.id.rv_bookings)
        progressBar = findViewById(R.id.progress_bar)
        
        adapter = AdminBookingAdapter(
            bookings = bookings,
            onChangeStatusClick = { booking -> showChangeStatusDialog(booking) },
            onDeleteClick = { booking -> showDeleteConfirmDialog(booking) },
            onDetailsClick = { booking -> showBookingDetails(booking) }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        findViewById<TextInputEditText>(R.id.et_admin_search).addTextChangedListener { applyBookingFilters() }
        findViewById<ChipGroup>(R.id.chip_booking_status).setOnCheckedStateChangeListener { _, ids ->
            selectedStatus = when (ids.firstOrNull()) {
                R.id.chip_pending -> "pending"
                R.id.chip_confirmed -> "confirmed"
                R.id.chip_completed -> "completed"
                R.id.chip_cancelled -> "cancelled"
                else -> null
            }
            applyBookingFilters()
        }
        findViewById<ChipGroup>(R.id.chip_payment_status).setOnCheckedStateChangeListener { _, ids ->
            hasPaymentMethod = when (ids.firstOrNull()) {
                R.id.chip_payment_set -> true; R.id.chip_payment_unset -> false; else -> null
            }
            applyBookingFilters()
        }
    }
    
    private fun loadBookings() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val localBookings = repository.getAllBookings().first()
                val localRooms = repository.getAllRooms().first().associateBy { it.id }
                val localUsers = repository.getAllUsers().first().associateBy { it.id }
                val bookingsList = localBookings.map { booking ->
                    val user = localUsers[booking.userId]
                    val room = localRooms[booking.roomId]
                    AdminBookingData(
                        id = booking.id.toString(),
                        user = user?.let { AdminBookingUser(it.id.toString(), it.email, it.fullName, it.phone) },
                        room = room?.let { AdminBookingRoom(it.id.toString(), it.name, it.price) },
                        checkInDate = booking.checkInDate, checkOutDate = booking.checkOutDate,
                        guestCount = booking.guestCount, totalPrice = booking.totalPrice,
                        status = booking.status, paymentMethod = booking.paymentMethod,
                        createdAt = booking.createdAt, slotId = booking.slotId?.toString()
                    )
                }
                bookings.clear()
                bookings.addAll(bookingsList)
                applyBookingFilters()
            } catch (e: Exception) {
                android.util.Log.e("AdminBookings", "Error: ${e.message}", e)
                Toast.makeText(this@AdminBookingsActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun applyBookingFilters() {
        if (!::adapter.isInitialized) return
        val query = findViewById<TextInputEditText>(R.id.et_admin_search).text?.toString().orEmpty().trim()
        val filtered = bookings.filter { booking ->
            val matchesText = query.isBlank() || booking.user?.fullName.orEmpty().contains(query, true) ||
                booking.user?.email.orEmpty().contains(query, true) || booking.room?.name.orEmpty().contains(query, true)
            matchesText && (selectedStatus == null || booking.status == selectedStatus) &&
                (hasPaymentMethod == null || (booking.paymentMethod != null) == hasPaymentMethod)
        }
        adapter.updateBookings(filtered)
        findViewById<View>(R.id.tv_empty).visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun showChangeStatusDialog(booking: AdminBookingData) {
        val transitions = when (booking.status.lowercase()) {
            "pending" -> listOf("confirmed" to "Đã xác nhận", "cancelled" to "Đã hủy")
            "confirmed" -> listOf("completed" to "Hoàn thành", "cancelled" to "Đã hủy")
            else -> emptyList()
        }
        if (transitions.isEmpty()) {
            Toast.makeText(this, "Booking này đã ở trạng thái cuối", Toast.LENGTH_SHORT).show()
            return
        }
        val statuses = transitions.map { it.first }.toTypedArray()
        val statusLabels = transitions.map { it.second }.toTypedArray()
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Đổi trạng thái booking")
            .setSingleChoiceItems(statusLabels, -1) { dialog, which ->
                val newStatus = statuses[which]
                if (newStatus != booking.status) {
                    updateBookingStatus(booking.id, newStatus)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showBookingDetails(booking: AdminBookingData) {
        val formatDate = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("vi", "VN"))
        val nights = ((booking.checkOutDate - booking.checkInDate) / 86_400_000L).coerceAtLeast(1)
        val price = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN")).format(booking.totalPrice.toLong())
        MaterialAlertDialogBuilder(this)
            .setTitle("Chi tiết booking #${booking.id}")
            .setMessage("Phòng: ${booking.room?.name ?: "N/A"}\nKhách: ${booking.user?.fullName ?: "N/A"}\nEmail: ${booking.user?.email ?: "N/A"}\nĐiện thoại: ${booking.user?.phone ?: "N/A"}\n\nNhận phòng: ${formatDate.format(java.util.Date(booking.checkInDate))}\nTrả phòng: ${formatDate.format(java.util.Date(booking.checkOutDate))}\nThời gian lưu trú: $nights đêm\nSố khách: ${booking.guestCount}\nSlot: ${booking.slotId ?: "Cả phòng"}\n\nTổng tiền: $price đ\nThanh toán: ${booking.paymentMethod ?: "Chưa chọn"}\nTrạng thái: ${booking.status}\nNgày tạo: ${formatDate.format(java.util.Date(booking.createdAt))}")
            .setPositiveButton("Đóng", null)
            .show()
    }
    
    private fun updateBookingStatus(bookingId: String, newStatus: String) {
        lifecycleScope.launch {
            try {
                val booking = repository.getBookingById(bookingId.toLong()) ?: return@launch
                repository.updateBooking(booking.copy(status = newStatus))
                Toast.makeText(this@AdminBookingsActivity, "Cập nhật trạng thái thành công!", Toast.LENGTH_SHORT).show()
                loadBookings()
            } catch (e: Exception) {
                Toast.makeText(this@AdminBookingsActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showDeleteConfirmDialog(booking: AdminBookingData) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Xóa booking")
            .setMessage("Bạn có chắc chắn muốn xóa booking này?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteBooking(booking.id)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun deleteBooking(bookingId: String) {
        lifecycleScope.launch {
            try {
                val booking = repository.getBookingById(bookingId.toLong()) ?: return@launch
                repository.deleteBooking(booking)
                Toast.makeText(this@AdminBookingsActivity, "Xóa booking thành công!", Toast.LENGTH_SHORT).show()
                adapter.removeBooking(bookingId)
            } catch (e: Exception) {
                Toast.makeText(this@AdminBookingsActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
