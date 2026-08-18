package com.example.homestay

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
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
            onDeleteClick = { booking -> showDeleteConfirmDialog(booking) }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
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
                        createdAt = booking.createdAt
                    )
                }
                bookings.clear()
                bookings.addAll(bookingsList)
                adapter.updateBookings(bookingsList)
            } catch (e: Exception) {
                android.util.Log.e("AdminBookings", "Error: ${e.message}", e)
                Toast.makeText(this@AdminBookingsActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun showChangeStatusDialog(booking: AdminBookingData) {
        val statuses = arrayOf("pending", "confirmed", "cancelled", "completed")
        val statusLabels = arrayOf("Chờ xác nhận", "Đã xác nhận", "Đã hủy", "Hoàn thành")
        
        val currentIndex = statuses.indexOf(booking.status.lowercase())
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Đổi trạng thái booking")
            .setSingleChoiceItems(statusLabels, currentIndex) { dialog, which ->
                val newStatus = statuses[which]
                if (newStatus != booking.status) {
                    updateBookingStatus(booking.id, newStatus)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
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
