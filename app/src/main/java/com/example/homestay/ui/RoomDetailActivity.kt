package com.example.homestay.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.homestay.HomestayApplication
import com.example.homestay.R
import com.example.homestay.data.model.CreateBookingRequest
import com.example.homestay.data.entity.Booking
import com.example.homestay.data.entity.Room
import com.example.homestay.ui.adapter.SlotAdapter
import com.example.homestay.ui.adapter.SlotSelectionAdapter
import com.example.homestay.ui.viewmodel.RoomViewModel
import com.example.homestay.ui.viewmodel.RoomViewModelFactory
import com.example.homestay.utils.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RoomDetailActivity : AppCompatActivity() {
    private val application by lazy { applicationContext as HomestayApplication }
    private val viewModel: RoomViewModel by viewModels {
        RoomViewModelFactory(application.repository, application.bookingRepository)
    }

    private lateinit var slotAdapter: SlotAdapter
    private var roomId: Long = -1
    private var currentRoom: Room? = null
    private val sessionManager by lazy { SessionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_room_detail)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Lấy roomId từ intent
        roomId = intent.getLongExtra("room_id", -1)
        if (roomId == -1L) {
            finish()
            return
        }

        setupToolbar()
        setupSlotsRecyclerView()
        observeRoomData()
        setupBookButton()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupSlotsRecyclerView() {
        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_slots)
        slotAdapter = SlotAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = slotAdapter
    }

    private fun observeRoomData() {
        // Observe room data
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getRoomById(roomId).collect { room ->
                    room?.let {
                        displayRoomInfo(it)
                    }
                }
            }
        }

        // Observe slots
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getSlotsByRoomId(roomId).collect { slots ->
                    slotAdapter.submitList(slots)
                }
            }
        }
    }

    private fun displayRoomInfo(room: com.example.homestay.data.entity.Room) {
        currentRoom = room
        
        // Load image from URL using Coil
        findViewById<ImageView>(R.id.img_room).load(room.imageUrl) {
            placeholder(R.drawable.app_logo) // Placeholder while loading
            error(R.drawable.app_logo) // Error image if load fails
            crossfade(true) // Smooth transition
            listener(
                onStart = { 
                    android.util.Log.d("RoomDetail", "Loading image: ${room.imageUrl}")
                },
                onSuccess = { _, _ -> 
                    android.util.Log.d("RoomDetail", "Image loaded successfully: ${room.imageUrl}")
                },
                onError = { _, result -> 
                    android.util.Log.e("RoomDetail", "Failed to load image: ${room.imageUrl}, error: ${result.throwable.message}")
                }
            )
        }
        findViewById<TextView>(R.id.tv_room_name).text = room.name
        findViewById<TextView>(R.id.tv_location).text = room.location
        // Format rating với 1 chữ số thập phân (5.0)
        findViewById<TextView>(R.id.tv_rating).text = String.format(Locale.getDefault(), "%.1f", room.rating)
        findViewById<TextView>(R.id.tv_review_count).text = "(${room.reviewCount} đánh giá)"
        findViewById<TextView>(R.id.tv_room_type).text = room.roomType
        findViewById<TextView>(R.id.tv_description).text = room.description
        findViewById<TextView>(R.id.tv_address).text = room.address
        findViewById<TextView>(R.id.tv_amenities).text = room.amenities
        findViewById<TextView>(R.id.tv_area).text = "${room.area} m²"
        findViewById<TextView>(R.id.tv_max_guests).text = "${room.maxGuests} người"

        // Format price
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        val formattedPrice = formatter.format(room.price.toLong())
        findViewById<TextView>(R.id.tv_price).text = "$formattedPrice đ / đêm"
    }

    private fun setupBookButton() {
        findViewById<MaterialButton>(R.id.btn_book).setOnClickListener {
            // Kiểm tra user đã đăng nhập chưa
            val userId = sessionManager.getUserId()
            if (userId == -1L) {
                Toast.makeText(this, "Vui lòng đăng nhập để đặt phòng", Toast.LENGTH_SHORT).show()
                startActivity(android.content.Intent(this, com.example.homestay.LoginActivity::class.java))
                return@setOnClickListener
            }

            // Kiểm tra room có tồn tại không
            val room = currentRoom
            if (room == null) {
                Toast.makeText(this, "Thông tin phòng không hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Hiển thị dialog đặt phòng
            showBookingDialog(room, userId)
        }
    }

    private fun showBookingDialog(room: Room, userId: Long) {
        val dialog = BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_booking, null)
        dialogView.setBackgroundColor(android.graphics.Color.WHITE)
        dialog.setContentView(dialogView)

        val tvRoomName = dialogView.findViewById<TextView>(R.id.tv_room_name)
        val tvRoomPrice = dialogView.findViewById<TextView>(R.id.tv_room_price)
        val tvCheckInDate = dialogView.findViewById<TextView>(R.id.tv_check_in_date)
        val tvCheckOutDate = dialogView.findViewById<TextView>(R.id.tv_check_out_date)
        val etGuestCount = dialogView.findViewById<TextInputEditText>(R.id.et_guest_count)
        val tvTotalPrice = dialogView.findViewById<TextView>(R.id.tv_total_price)
        val tvSlotInfo = dialogView.findViewById<TextView>(R.id.tv_slot_info)
        val layoutCheckIn = dialogView.findViewById<LinearLayout>(R.id.layout_check_in)
        val layoutCheckOut = dialogView.findViewById<LinearLayout>(R.id.layout_check_out)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btn_confirm_booking)
        val recyclerSlotSelection = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_slot_selection)

        // Hiển thị thông tin phòng
        tvRoomName?.text = room.name
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        val formattedPrice = formatter.format(room.price.toLong())
        tvRoomPrice?.text = "$formattedPrice đ / đêm"

        // Setup date pickers
        val calendar = Calendar.getInstance()
        val dateFormatDisplay = SimpleDateFormat("EEE, dd MMM", Locale("vi", "VN"))
        var checkInTimestamp: Long? = null
        var checkOutTimestamp: Long? = null
        var selectedSlot: com.example.homestay.data.entity.Slot? = null

        fun updateTotalPrice() {
            if (checkInTimestamp != null && checkOutTimestamp != null && checkInTimestamp!! < checkOutTimestamp!!) {
                val days = ((checkOutTimestamp!! - checkInTimestamp!!) / (24 * 60 * 60 * 1000)).toInt()
                val guestCount = etGuestCount?.text?.toString()?.toIntOrNull() ?: 1
                
                // Tính giá dựa trên slot được chọn hoặc giá phòng
                val pricePerDay = selectedSlot?.price?.takeIf { it > 0 } ?: room.price
                val totalPrice = pricePerDay * days
                val formattedTotal = formatter.format(totalPrice.toLong())
                tvTotalPrice?.text = "$formattedTotal đ"
                
                // Kiểm tra và hiển thị slot availability
                lifecycleScope.launch {
                    try {
                        val availableSlots = viewModel.getAvailableSlotCount(
                            room.id,
                            checkInTimestamp!!,
                            checkOutTimestamp!!
                        )
                        runOnUiThread {
                            tvSlotInfo?.text = "$availableSlots/${room.maxSlots} slot"
                            tvSlotInfo?.visibility = View.VISIBLE
                            if (availableSlots == 0) {
                                tvSlotInfo?.setTextColor(getColor(android.R.color.holo_red_dark))
                            } else if (availableSlots < room.maxSlots / 2) {
                                tvSlotInfo?.setTextColor(getColor(android.R.color.holo_orange_dark))
                            } else {
                                tvSlotInfo?.setTextColor(getColor(R.color.home_primary))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("RoomDetailActivity", "Error checking slot availability: ${e.message}", e)
                    }
                }
            } else {
                tvTotalPrice?.text = "0 đ"
                tvSlotInfo?.visibility = View.GONE
            }
        }

        // Setup slot selection
        val slotSelectionAdapter = SlotSelectionAdapter { slot ->
            selectedSlot = slot
            updateTotalPrice()
        }
        recyclerSlotSelection?.layoutManager = LinearLayoutManager(this)
        recyclerSlotSelection?.adapter = slotSelectionAdapter

        // Load slots
        lifecycleScope.launch {
            viewModel.getAvailableSlotsByRoomId(room.id).collect { slots ->
                slotSelectionAdapter.submitList(slots)
            }
        }

        layoutCheckIn?.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDay)
                    }
                    checkInTimestamp = selectedDate.timeInMillis
                    tvCheckInDate?.text = dateFormatDisplay.format(selectedDate.time)
                    updateTotalPrice()
                },
                year, month, day
            ).apply {
                datePicker.minDate = System.currentTimeMillis() - 1000
                show()
            }
        }

        layoutCheckOut?.setOnClickListener {
            if (checkInTimestamp == null) {
                Toast.makeText(this, "Vui lòng chọn ngày nhận phòng trước", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDay)
                    }
                    checkOutTimestamp = selectedDate.timeInMillis
                    tvCheckOutDate?.text = dateFormatDisplay.format(selectedDate.time)
                    updateTotalPrice()
                },
                year, month, day
            ).apply {
                checkInTimestamp?.let { checkIn ->
                    datePicker.minDate = checkIn + (24 * 60 * 60 * 1000) // +1 day
                } ?: run {
                    datePicker.minDate = System.currentTimeMillis() - 1000
                }
                show()
            }
        }

        // Update total price when guest count changes
        etGuestCount?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateTotalPrice()
            }
        })

        // Confirm booking
        btnConfirm?.setOnClickListener {
            if (checkInTimestamp == null) {
                Toast.makeText(this, "Vui lòng chọn ngày nhận phòng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (checkOutTimestamp == null) {
                Toast.makeText(this, "Vui lòng chọn ngày trả phòng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (checkInTimestamp!! >= checkOutTimestamp!!) {
                Toast.makeText(this, "Ngày trả phòng phải sau ngày nhận phòng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val guestCount = etGuestCount?.text?.toString()?.toIntOrNull() ?: 1
            if (guestCount < 1) {
                Toast.makeText(this, "Số khách phải lớn hơn 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (guestCount > room.maxGuests) {
                Toast.makeText(this, "Số khách không được vượt quá ${room.maxGuests} người", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Calculate total price
            val days = ((checkOutTimestamp!! - checkInTimestamp!!) / (24 * 60 * 60 * 1000)).toInt()
            if (days <= 0) {
                Toast.makeText(this, "Số ngày không hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button to prevent double click
            btnConfirm?.isEnabled = false
            btnConfirm?.text = "Đang kiểm tra..."

            // Kiểm tra slot availability
            lifecycleScope.launch {
                try {
                    val isAvailable = viewModel.checkSlotAvailability(
                        room.id,
                        checkInTimestamp!!,
                        checkOutTimestamp!!
                    )

                    if (!isAvailable) {
                        val availableSlots = viewModel.getAvailableSlotCount(
                            room.id,
                            checkInTimestamp!!,
                            checkOutTimestamp!!
                        )
                        runOnUiThread {
                            btnConfirm?.isEnabled = true
                            btnConfirm?.text = "Xác nhận đặt phòng"
                            Toast.makeText(
                                this@RoomDetailActivity,
                                "Phòng đã hết slot trong khoảng thời gian này. Còn lại $availableSlots/${room.maxSlots} slot.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@launch
                    }

                    // Tính giá dựa trên slot được chọn hoặc giá phòng
                    val pricePerDay = selectedSlot?.price?.takeIf { it > 0 } ?: room.price
                    val totalPrice = pricePerDay * days

                    val localUserKey = sessionManager.getMongoUserId()
                    val localRoomKey = room.id.toString()
                    
                    if (localUserKey == null) {
                        runOnUiThread {
                            btnConfirm?.isEnabled = true
                            btnConfirm?.text = "Xác nhận đặt phòng"
                            Toast.makeText(
                                this@RoomDetailActivity,
                                "Vui lòng đăng nhập lại để đặt phòng",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@launch
                    }
                    
                    runOnUiThread {
                        btnConfirm?.text = "Đang xử lý..."
                    }

                    val bookingRequest = CreateBookingRequest(
                        roomId = localRoomKey,
                        checkInDate = checkInTimestamp!!,
                        checkOutDate = checkOutTimestamp!!,
                        guestCount = guestCount,
                        totalPrice = totalPrice,
                        status = "pending",
                        paymentMethod = null,
                        slotId = selectedSlot?.id?.toString()
                    )
                    
                    Log.d("RoomDetailActivity", "Creating local booking: roomId=$localRoomKey, userId=$localUserKey, totalPrice=$totalPrice")
                    val result = viewModel.createBookingViaAPI(
                        localUserKey,
                        userId,
                        room.id,
                        localRoomKey,
                        bookingRequest
                    )
                    
                    if (result.isSuccess) {
                        val bookingData = result.getOrNull()
                        if (bookingData != null) {
                            Log.d("RoomDetailActivity", "Booking created successfully: mongoId=${bookingData.mongoBookingId}, localId=${bookingData.booking.id}")
                            runOnUiThread {
                                dialog.dismiss()
                                // Show payment dialog
                                showPaymentDialog(bookingData.booking)
                            }
                        } else {
                            runOnUiThread {
                                btnConfirm?.isEnabled = true
                                btnConfirm?.text = "Xác nhận đặt phòng"
                                Toast.makeText(
                                    this@RoomDetailActivity,
                                    "Có lỗi xảy ra khi đặt phòng",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        val error = result.exceptionOrNull()
                        Log.e("RoomDetailActivity", "Error creating booking: ${error?.message}", error)
                        runOnUiThread {
                            btnConfirm?.isEnabled = true
                            btnConfirm?.text = "Xác nhận đặt phòng"
                            Toast.makeText(
                                this@RoomDetailActivity,
                                "Có lỗi xảy ra khi đặt phòng: ${error?.message ?: "Lỗi không xác định"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("RoomDetailActivity", "Error creating booking: ${e.message}", e)
                    e.printStackTrace()
                    runOnUiThread {
                        btnConfirm?.isEnabled = true
                        btnConfirm?.text = "Xác nhận đặt phòng"
                        Toast.makeText(
                            this@RoomDetailActivity,
                            "Có lỗi xảy ra khi đặt phòng: ${e.message ?: "Lỗi không xác định"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showPaymentDialog(booking: Booking) {
        val dialog = BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment, null)
        dialogView.setBackgroundColor(android.graphics.Color.WHITE)
        dialog.setContentView(dialogView)

        val tvBookingInfo = dialogView.findViewById<TextView>(R.id.tv_booking_info)
        val rgPaymentMethod = dialogView.findViewById<android.widget.RadioGroup>(R.id.rg_payment_method)
        val tvQrMaintenance = dialogView.findViewById<TextView>(R.id.tv_qr_maintenance)
        val tvTotalPayment = dialogView.findViewById<TextView>(R.id.tv_total_payment)
        val btnConfirmPayment = dialogView.findViewById<MaterialButton>(R.id.btn_confirm_payment)

        // Hiển thị thông tin booking
        val room = currentRoom
        if (room != null) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
            val checkInDate = dateFormat.format(java.util.Date(booking.checkInDate))
            val checkOutDate = dateFormat.format(java.util.Date(booking.checkOutDate))
            tvBookingInfo?.text = "${room.name}\n$checkInDate - $checkOutDate\n${booking.guestCount} người"
        }

        // Format và hiển thị tổng tiền
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        val formattedTotal = formatter.format(booking.totalPrice.toLong())
        tvTotalPayment?.text = "$formattedTotal đ"

        // Disable confirmation while QR payment is under maintenance.
        fun updatePaymentUI(paymentMethod: String) {
            val qrUnderMaintenance = paymentMethod == "qr_code"
            tvQrMaintenance?.visibility = if (qrUnderMaintenance) android.view.View.VISIBLE else android.view.View.GONE
            btnConfirmPayment?.isEnabled = !qrUnderMaintenance
            btnConfirmPayment?.text = if (qrUnderMaintenance) "QR đang bảo trì" else "Xác nhận thanh toán"
        }

        rgPaymentMethod?.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_qr_code -> updatePaymentUI("qr_code")
                R.id.rb_pay_on_site -> updatePaymentUI("pay_on_site")
            }
        }

        // Set initial UI state
        updatePaymentUI("pay_on_site")

        // Xử lý thanh toán
        btnConfirmPayment?.setOnClickListener {
            val selectedPaymentMethod = "pay_on_site"

            // Simulate payment processing
            btnConfirmPayment?.isEnabled = false
            btnConfirmPayment?.text = "Đang xử lý..."

            lifecycleScope.launch {
                try {
                    // For pay on site, status remains "pending"
                    // For other methods, simulate payment and update to "confirmed"
                    val newStatus = "pending"

                    val updatedBooking = booking.copy(
                        status = newStatus,
                        paymentMethod = selectedPaymentMethod
                    )
                    viewModel.updateBooking(updatedBooking)

                    val message = "Đặt phòng thành công! Vui lòng thanh toán khi đến nơi."

                    runOnUiThread {
                        Toast.makeText(
                            this@RoomDetailActivity,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                        dialog.dismiss()
                        finish() // Return to previous screen
                    }
                } catch (e: Exception) {
                    Log.e("RoomDetailActivity", "Error processing payment: ${e.message}", e)
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(
                            this@RoomDetailActivity,
                            "Có lỗi xảy ra khi thanh toán. Vui lòng thử lại.",
                            Toast.LENGTH_SHORT
                        ).show()
                        btnConfirmPayment?.isEnabled = true
                        btnConfirmPayment?.text = "Xác nhận thanh toán"
                    }
                }
            }
        }

        dialog.show()
    }
}

