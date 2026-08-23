package com.example.homestay

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.homestay.data.model.AdminRoomData
import com.example.homestay.data.entity.Room
import com.example.homestay.ui.admin.AdminRoomAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import coil.load

class AdminRoomsActivity : AppCompatActivity() {
    private val repository by lazy { (application as HomestayApplication).repository }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminRoomAdapter
    private lateinit var fabAddRoom: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private var rooms = mutableListOf<AdminRoomData>()
    private var roomAvailabilityFilter: Boolean? = null
    private var activeImageField: TextInputEditText? = null
    private var activeImagePreview: ImageView? = null
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        runCatching {
            contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activeImageField?.setText(uri.toString())
        activeImagePreview?.load(uri) {
            placeholder(R.drawable.ic_room_placeholder)
            error(R.drawable.ic_room_placeholder)
            crossfade(true)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_rooms)
        
        setupToolbar()
        setupViews()
        loadRooms()
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Quản lý Phòng"
    }
    
    private fun setupViews() {
        recyclerView = findViewById(R.id.rv_rooms)
        fabAddRoom = findViewById(R.id.fab_add_room)
        progressBar = findViewById(R.id.progress_bar)
        
        adapter = AdminRoomAdapter(
            rooms = rooms,
            onEditClick = { room -> showEditRoomDialog(room) },
            onDeleteClick = { room -> showDeleteConfirmDialog(room) },
            onDetailsClick = { room -> showRoomDetails(room) }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        findViewById<TextInputEditText>(R.id.et_admin_search).addTextChangedListener { filterRooms(it?.toString().orEmpty()) }
        findViewById<ChipGroup>(R.id.chip_room_status).setOnCheckedStateChangeListener { _, ids ->
            roomAvailabilityFilter = when (ids.firstOrNull()) {
                R.id.chip_rooms_active -> true; R.id.chip_rooms_hidden -> false; else -> null
            }
            filterRooms(findViewById<TextInputEditText>(R.id.et_admin_search).text?.toString().orEmpty())
        }
        
        fabAddRoom.setOnClickListener {
            showAddRoomDialog()
        }
    }
    
    private fun loadRooms() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                bookingsSnapshot = repository.getAllBookings().first()
                val roomsList = repository.getAllRooms().first().map { it.toAdminData() }
                rooms.clear()
                rooms.addAll(roomsList)
                filterRooms(findViewById<TextInputEditText>(R.id.et_admin_search).text?.toString().orEmpty())
            } catch (e: Exception) {
                android.util.Log.e("AdminRooms", "Error: ${e.message}", e)
                Toast.makeText(this@AdminRoomsActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun showAddRoomDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_room, null)
        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tv_dialog_title)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_room_name)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.et_room_description)
        val etPrice = dialogView.findViewById<TextInputEditText>(R.id.et_room_price)
        val etCapacity = dialogView.findViewById<TextInputEditText>(R.id.et_room_capacity)
        val etMaxSlots = dialogView.findViewById<TextInputEditText>(R.id.et_room_max_slots)
        val etImageUrl = dialogView.findViewById<TextInputEditText>(R.id.et_room_image_url)
        val imagePreview = dialogView.findViewById<ImageView>(R.id.iv_room_image_preview)
        val btnSelectImage = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_select_room_image)
        val etType = dialogView.findViewById<TextInputEditText>(R.id.et_room_type)
        val etLocation = dialogView.findViewById<TextInputEditText>(R.id.et_room_location)
        val etAddress = dialogView.findViewById<TextInputEditText>(R.id.et_room_address)
        val etArea = dialogView.findViewById<TextInputEditText>(R.id.et_room_area)
        val etAmenities = dialogView.findViewById<TextInputEditText>(R.id.et_room_amenities)
        val switchAvailable = dialogView.findViewById<MaterialSwitch>(R.id.switch_room_available)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save)
        
        tvTitle.text = "Thêm phòng mới"
        btnSelectImage.setOnClickListener {
            activeImageField = etImageUrl
            activeImagePreview = imagePreview
            selectImageLauncher.launch(arrayOf("image/*"))
        }
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val name = etName.text?.toString()?.trim() ?: ""
            val description = etDescription.text?.toString()?.trim() ?: ""
            val priceStr = etPrice.text?.toString()?.trim() ?: ""
            val capacityStr = etCapacity.text?.toString()?.trim() ?: ""
            val maxSlotsStr = etMaxSlots.text?.toString()?.trim() ?: "1"
            val imageUrl = etImageUrl.text?.toString()?.trim() ?: ""

            if (!isValidImageUrl(imageUrl)) {
                etImageUrl.error = "Ảnh không hợp lệ, vui lòng chọn lại từ thiết bị"
                return@setOnClickListener
            }
            
            if (name.isEmpty() || priceStr.isEmpty() || capacityStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val price = priceStr.toDoubleOrNull() ?: 0.0
            val capacity = capacityStr.toIntOrNull() ?: 1
            val maxSlots = maxSlotsStr.toIntOrNull() ?: 1
            if (price <= 0 || capacity !in 1..20 || maxSlots !in 1..20) {
                Toast.makeText(this, "Giá phải lớn hơn 0; sức chứa và số phòng phải từ 1 đến 20", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            createRoom(name, description, price, capacity, maxSlots, imageUrl,
                etType.text?.toString().orEmpty(), etLocation.text?.toString().orEmpty(),
                etAddress.text?.toString().orEmpty(), etArea.text?.toString()?.toIntOrNull() ?: 0,
                etAmenities.text?.toString().orEmpty(), switchAvailable.isChecked, dialog)
        }
        
        dialog.show()
    }
    
    private fun showEditRoomDialog(room: AdminRoomData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_room, null)
        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tv_dialog_title)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_room_name)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.et_room_description)
        val etPrice = dialogView.findViewById<TextInputEditText>(R.id.et_room_price)
        val etCapacity = dialogView.findViewById<TextInputEditText>(R.id.et_room_capacity)
        val etMaxSlots = dialogView.findViewById<TextInputEditText>(R.id.et_room_max_slots)
        val etImageUrl = dialogView.findViewById<TextInputEditText>(R.id.et_room_image_url)
        val imagePreview = dialogView.findViewById<ImageView>(R.id.iv_room_image_preview)
        val btnSelectImage = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_select_room_image)
        val etType = dialogView.findViewById<TextInputEditText>(R.id.et_room_type)
        val etLocation = dialogView.findViewById<TextInputEditText>(R.id.et_room_location)
        val etAddress = dialogView.findViewById<TextInputEditText>(R.id.et_room_address)
        val etArea = dialogView.findViewById<TextInputEditText>(R.id.et_room_area)
        val etAmenities = dialogView.findViewById<TextInputEditText>(R.id.et_room_amenities)
        val switchAvailable = dialogView.findViewById<MaterialSwitch>(R.id.switch_room_available)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save)
        
        tvTitle.text = "Chỉnh sửa phòng"
        etName.setText(room.name)
        etDescription.setText(room.description)
        etPrice.setText(room.price.toString())
        etCapacity.setText(room.capacity.toString())
        etMaxSlots.setText(room.maxSlots.toString())
        etImageUrl.setText(room.imageUrl)
        imagePreview.load(room.imageUrl) {
            placeholder(R.drawable.ic_room_placeholder)
            error(R.drawable.ic_room_placeholder)
        }
        btnSelectImage.setOnClickListener {
            activeImageField = etImageUrl
            activeImagePreview = imagePreview
            selectImageLauncher.launch(arrayOf("image/*"))
        }
        etType.setText(room.roomType)
        etLocation.setText(room.location)
        etAddress.setText(room.address)
        etArea.setText(room.area.toString())
        etAmenities.setText(room.amenities)
        switchAvailable.isChecked = room.isAvailable
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val name = etName.text?.toString()?.trim() ?: ""
            val description = etDescription.text?.toString()?.trim() ?: ""
            val priceStr = etPrice.text?.toString()?.trim() ?: ""
            val capacityStr = etCapacity.text?.toString()?.trim() ?: ""
            val maxSlotsStr = etMaxSlots.text?.toString()?.trim() ?: "1"
            val imageUrl = etImageUrl.text?.toString()?.trim() ?: ""

            if (!isValidImageUrl(imageUrl)) {
                etImageUrl.error = "Ảnh không hợp lệ, vui lòng chọn lại từ thiết bị"
                return@setOnClickListener
            }
            
            if (name.isEmpty() || priceStr.isEmpty() || capacityStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val price = priceStr.toDoubleOrNull() ?: 0.0
            val capacity = capacityStr.toIntOrNull() ?: 1
            val maxSlots = maxSlotsStr.toIntOrNull() ?: 1
            if (price <= 0 || capacity !in 1..20 || maxSlots !in 1..20) {
                Toast.makeText(this, "Giá phải lớn hơn 0; sức chứa và số phòng phải từ 1 đến 20", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            updateRoom(room.id, name, description, price, capacity, maxSlots, imageUrl,
                etType.text?.toString().orEmpty(), etLocation.text?.toString().orEmpty(),
                etAddress.text?.toString().orEmpty(), etArea.text?.toString()?.toIntOrNull() ?: 0,
                etAmenities.text?.toString().orEmpty(), switchAvailable.isChecked, dialog)
        }
        
        dialog.show()
    }
    
    private fun createRoom(name: String, description: String, price: Double, capacity: Int, maxSlots: Int,
        imageUrl: String, roomType: String, location: String, address: String, area: Int,
        amenities: String, isAvailable: Boolean, dialog: AlertDialog) {
        lifecycleScope.launch {
            try {
                repository.insertRoom(Room(name = name, description = description, price = price,
                    imageUrl = imageUrl, location = location, address = address,
                    amenities = amenities.ifBlank { "WiFi" }, maxGuests = capacity,
                    roomType = roomType.ifBlank { "Homestay" }, area = area,
                    maxSlots = maxSlots, isAvailable = isAvailable))
                Toast.makeText(this@AdminRoomsActivity, "Thêm phòng thành công!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                loadRooms()
            } catch (e: Exception) {
                Toast.makeText(this@AdminRoomsActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateRoom(roomId: String, name: String, description: String, price: Double, capacity: Int,
        maxSlots: Int, imageUrl: String, roomType: String, location: String, address: String,
        area: Int, amenities: String, isAvailable: Boolean, dialog: AlertDialog) {
        lifecycleScope.launch {
            try {
                val room = repository.getRoomById(roomId.toLong()) ?: return@launch
                repository.updateRoom(room.copy(name = name, description = description, price = price,
                    maxGuests = capacity, maxSlots = maxSlots, imageUrl = imageUrl,
                    roomType = roomType.ifBlank { "Homestay" }, location = location,
                    address = address, area = area, amenities = amenities,
                    isAvailable = isAvailable))
                Toast.makeText(this@AdminRoomsActivity, "Cập nhật phòng thành công!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                loadRooms()
            } catch (e: Exception) {
                Toast.makeText(this@AdminRoomsActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showDeleteConfirmDialog(room: AdminRoomData) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Xóa phòng")
            .setMessage("Bạn có chắc chắn muốn xóa phòng \"${room.name}\"?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteRoom(room.id)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun deleteRoom(roomId: String) {
        lifecycleScope.launch {
            try {
                val room = repository.getRoomById(roomId.toLong()) ?: return@launch
                if (repository.roomHasBookings(room.id)) {
                    Toast.makeText(this@AdminRoomsActivity, "Không thể xóa phòng đang có booking", Toast.LENGTH_LONG).show()
                    return@launch
                }
                repository.deleteRoom(room)
                Toast.makeText(this@AdminRoomsActivity, "Xóa phòng thành công!", Toast.LENGTH_SHORT).show()
                adapter.removeRoom(roomId)
            } catch (e: Exception) {
                Toast.makeText(this@AdminRoomsActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRoomDetails(room: AdminRoomData) {
        val price = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN")).format(room.price.toLong())
        val revenue = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN")).format(room.revenue.toLong())
        MaterialAlertDialogBuilder(this)
            .setTitle(room.name)
            .setMessage("Mã phòng: ${room.id}\nLoại: ${room.roomType}\nTrạng thái: ${if (room.isAvailable) "Đang mở" else "Tạm ẩn"}\nGiá: $price đ/đêm\nĐịa điểm: ${room.location}\nĐịa chỉ: ${room.address}\nDiện tích: ${room.area} m²\nSức chứa: ${room.capacity} người\nSố slot: ${room.maxSlots}\nĐánh giá: ${room.rating} (${room.reviewCount} lượt)\nTiện nghi: ${room.amenities}\n\nBooking: ${room.bookingCount}\nDoanh thu thực tế: $revenue đ\n\nMô tả:\n${room.description}")
            .setNegativeButton("Đóng", null)
            .setPositiveButton("Chỉnh sửa") { _, _ -> showEditRoomDialog(room) }
            .show()
    }

    private fun filterRooms(query: String) {
        val keyword = query.trim()
        val filtered = rooms.filter { (roomAvailabilityFilter == null || it.isAvailable == roomAvailabilityFilter) &&
            (it.name.contains(keyword, true) || it.location.contains(keyword, true) ||
            it.roomType.contains(keyword, true) || it.amenities.contains(keyword, true)) }
        adapter.updateRooms(filtered)
        findViewById<View>(R.id.tv_empty).visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun isValidImageUrl(value: String): Boolean {
        if (value.isBlank()) return true
        val uri = runCatching { android.net.Uri.parse(value) }.getOrNull() ?: return false
        if (uri.scheme == "content") return true
        if (uri.scheme != "https" || uri.host.isNullOrBlank()) return false
        val path = uri.path.orEmpty().lowercase()
        return !path.endsWith(".html") && !path.endsWith(".htm")
    }

    private fun Room.toAdminData(): AdminRoomData {
        val roomBookings = bookingsSnapshot.filter { it.roomId == id }
        return AdminRoomData(id = id.toString(), name = name, description = description, price = price,
            capacity = maxGuests, imageUrl = imageUrl, maxSlots = maxSlots, createdAt = 0L,
            location = location, address = address, amenities = amenities, roomType = roomType,
            area = area, rating = rating, reviewCount = reviewCount, isAvailable = isAvailable,
            bookingCount = roomBookings.size,
            revenue = roomBookings.filter { it.status == "completed" }.sumOf { it.totalPrice })
    }

    private var bookingsSnapshot = emptyList<com.example.homestay.data.entity.Booking>()
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
