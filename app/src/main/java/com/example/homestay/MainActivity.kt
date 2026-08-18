package com.example.homestay

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import android.app.DatePickerDialog
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.homestay.ui.RoomDetailActivity
import com.example.homestay.ui.adapter.BookingAdapter
import com.example.homestay.ui.adapter.RoomAdapter
import com.example.homestay.ui.viewmodel.RoomViewModel
import com.example.homestay.ui.viewmodel.RoomViewModelFactory
import com.example.homestay.utils.InputValidator
import com.example.homestay.utils.PasswordHasher
import com.example.homestay.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private val contentContainer: FrameLayout by lazy { findViewById(R.id.content_container) }
    private val bottomNav: BottomNavigationView by lazy { findViewById(R.id.bottom_nav) }
    
    private val application by lazy { applicationContext as HomestayApplication }
    private val repository by lazy { application.repository }
    private val authRepository by lazy { application.authRepository }
    private val bookingRepository by lazy { application.bookingRepository }
    private val viewModel: RoomViewModel by viewModels {
        RoomViewModelFactory(repository, bookingRepository)
    }
    
    private var roomAdapter: RoomAdapter? = null
    private var favoriteAdapter: RoomAdapter? = null
    private var bookingAdapter: BookingAdapter? = null
    private var searchResultsJob: Job? = null
    private var isObservingFavorites = false
    private var isObservingBookings = false
    private var currentUserId: Long = -1L
    private var favoriteStatusMap = mutableMapOf<Long, Boolean>()
    private val sessionManager by lazy { SessionManager(this) }
    
    // Để tránh sync quá thường xuyên, lưu thời gian sync cuối cùng
    private var lastRoomsSyncTime: Long = 0
    private var lastBookingsSyncTime: Long = 0
    private val SYNC_COOLDOWN_MS = 5000L // Chỉ sync tối đa mỗi 5 giây

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)

            // Keep the navigation background flush with the screen bottom while
            // placing its icons safely above the gesture/navigation area.
            val baseHeight = (72 * resources.displayMetrics.density).toInt()
            bottomNav.layoutParams = bottomNav.layoutParams.apply {
                height = baseHeight + systemBars.bottom
            }
            bottomNav.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // Initialize currentUserId
        currentUserId = sessionManager.getUserId()

        // Sync rooms from backend API
        syncRoomsFromBackend()

        setupBottomNav()
        loadContent(R.layout.content_search)
        bottomNav.selectedItemId = R.id.navigation_search
    }
    
    private fun syncRoomsFromBackend() {
        lifecycleScope.launch {
            try {
                // Clean up duplicate rooms trước khi sync
                repository.cleanupDuplicateRooms()
                
                val success = repository.syncRoomsFromAPI()
                if (success) {
                    android.util.Log.d("MainActivity", "Rooms synced successfully from backend")
                    
                    // Clean up lại sau khi sync để đảm bảo không có duplicate
                    repository.cleanupDuplicateRooms()
                } else {
                    android.util.Log.w("MainActivity", "Failed to sync rooms from backend")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error syncing rooms: ${e.message}", e)
            }
        }
    }
    
    /**
     * Sync rooms mà không hiển thị toast notification
     * Dùng khi sync tự động (khi chuyển tab, quay lại app)
     * Có debounce để tránh sync quá thường xuyên
     */
    private fun syncRoomsFromBackendSilent() {
        val currentTime = System.currentTimeMillis()
        // Chỉ sync nếu đã qua cooldown period
        if (currentTime - lastRoomsSyncTime < SYNC_COOLDOWN_MS) {
            android.util.Log.d("MainActivity", "Skip rooms sync - too soon since last sync")
            return
        }
        
        lastRoomsSyncTime = currentTime
        lifecycleScope.launch {
            try {
                // Clean up duplicate rooms trước khi sync
                repository.cleanupDuplicateRooms()
                
                val success = repository.syncRoomsFromAPI()
                if (success) {
                    android.util.Log.d("MainActivity", "Rooms synced silently from backend")
                    
                    // Clean up lại sau khi sync để đảm bảo không có duplicate
                    repository.cleanupDuplicateRooms()
                } else {
                    android.util.Log.w("MainActivity", "Failed to sync rooms from backend (silent)")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error syncing rooms (silent): ${e.message}", e)
            }
        }
    }
    
    private fun syncRoomsFromBackendWithRefresh(swipeRefresh: SwipeRefreshLayout) {
        lifecycleScope.launch {
            try {
                // Clean up duplicate rooms trước khi sync
                repository.cleanupDuplicateRooms()
                
                val success = repository.syncRoomsFromAPI()
                if (success) {
                    android.util.Log.d("MainActivity", "Rooms synced successfully from backend (pull-to-refresh)")
                    
                    // Clean up lại sau khi sync để đảm bảo không có duplicate
                    repository.cleanupDuplicateRooms()
                    
                    // Hiển thị thông báo thành công
                    Toast.makeText(this@MainActivity, "Đã cập nhật danh sách phòng", Toast.LENGTH_SHORT).show()
                } else {
                    android.util.Log.w("MainActivity", "Failed to sync rooms from backend (pull-to-refresh)")
                    Toast.makeText(this@MainActivity, "Không thể cập nhật danh sách phòng", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error syncing rooms: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                // Tắt refresh indicator
                swipeRefresh.isRefreshing = false
            }
        }
    }
    
    /**
     * Sync bookings từ backend API mà không hiển thị toast notification
     * Dùng khi sync tự động (khi chuyển tab, quay lại app)
     */
    private fun syncBookingsFromBackendSilent() {
        val currentTime = System.currentTimeMillis()
        // Chỉ sync nếu đã qua cooldown period
        if (currentTime - lastBookingsSyncTime < SYNC_COOLDOWN_MS) {
            android.util.Log.d("MainActivity", "Skip bookings sync - too soon since last sync")
            return
        }
        
        lastBookingsSyncTime = currentTime
        lifecycleScope.launch {
            try {
                val userId = sessionManager.getUserId()
                val mongoUserId = sessionManager.getMongoUserId()
                
                if (userId != -1L && mongoUserId != null) {
                    val success = bookingRepository?.syncBookingsFromAPI(mongoUserId, userId) ?: false
                    if (success) {
                        android.util.Log.d("MainActivity", "Bookings synced silently from backend")
                    } else {
                        android.util.Log.w("MainActivity", "Failed to sync bookings from backend (silent)")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error syncing bookings (silent): ${e.message}", e)
            }
        }
    }
    
    /**
     * Sync bookings từ backend API với refresh indicator
     * Dùng khi user kéo xuống để refresh (pull-to-refresh)
     */
    private fun syncBookingsFromBackendWithRefresh(swipeRefresh: SwipeRefreshLayout) {
        lifecycleScope.launch {
            try {
                val userId = sessionManager.getUserId()
                val mongoUserId = sessionManager.getMongoUserId()
                
                if (userId != -1L && mongoUserId != null) {
                    val success = bookingRepository?.syncBookingsFromAPI(mongoUserId, userId) ?: false
                    if (success) {
                        android.util.Log.d("MainActivity", "Bookings synced successfully from backend (pull-to-refresh)")
                        
                        // Hiển thị thông báo thành công
                        Toast.makeText(this@MainActivity, "Đã cập nhật danh sách đặt chỗ", Toast.LENGTH_SHORT).show()
                    } else {
                        android.util.Log.w("MainActivity", "Failed to sync bookings from backend (pull-to-refresh)")
                        Toast.makeText(this@MainActivity, "Không thể cập nhật danh sách đặt chỗ", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Vui lòng đăng nhập để xem đặt chỗ", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error syncing bookings: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                // Tắt refresh indicator
                swipeRefresh.isRefreshing = false
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        // Sync rooms khi activity được hiển thị, đặc biệt khi quay lại từ RoomDetailActivity
        // Chỉ sync nếu đang ở tab search để tránh sync không cần thiết
        when (bottomNav.selectedItemId) {
            R.id.navigation_search -> {
                syncRoomsFromBackendSilent()
            }
            R.id.navigation_booking -> {
                syncBookingsFromBackendSilent()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Sync rooms/bookings khi resume (để đảm bảo luôn có dữ liệu mới nhất từ admin)
        when (bottomNav.selectedItemId) {
            R.id.navigation_search -> {
                syncRoomsFromBackendSilent()
            }
            R.id.navigation_booking -> {
                syncBookingsFromBackendSilent()
            }
        }
        
        // Kiểm tra nếu user đã đổi (ví dụ: đăng nhập lại từ LoginActivity)
        val newUserId = sessionManager.getUserId()
        if (currentUserId != newUserId) {
            // Reset adapters và observers khi user đổi
            currentUserId = newUserId
            favoriteStatusMap.clear()
            roomAdapter = null
            favoriteAdapter = null
            bookingAdapter = null
            searchResultsJob?.cancel()
            isObservingFavorites = false
            isObservingBookings = false
            
            // Reload content nếu đang ở tab search, saved, booking hoặc profile
            when (bottomNav.selectedItemId) {
                R.id.navigation_search -> {
                    loadContent(R.layout.content_search)
                    setupSearchContent()
                    // Sync rooms khi quay lại tab search để đảm bảo đồng bộ với admin
                    syncRoomsFromBackend()
                }
                R.id.navigation_saved -> {
                    if (!sessionManager.isLoggedIn()) {
                        openLogin("Vui lòng đăng nhập để xem phòng đã lưu")
                    } else {
                        loadContent(R.layout.content_saved)
                        setupSavedContent()
                    }
                }
                R.id.navigation_booking -> {
                    if (!sessionManager.isLoggedIn()) {
                        openLogin("Vui lòng đăng nhập để xem đặt chỗ")
                    } else {
                        loadContent(R.layout.content_bookings)
                        setupBookingsContent()
                    }
                }
                R.id.navigation_profile -> {
                    loadContent(R.layout.content_account)
                    setupAccountContent()
                }
            }
        } else {
            // Nếu user không đổi, vẫn reload bookings và account khi quay lại
            when (bottomNav.selectedItemId) {
                R.id.navigation_search -> {
                    // Sync rooms khi quay lại tab search (ví dụ: từ RoomDetailActivity)
                    syncRoomsFromBackendSilent()
                }
                R.id.navigation_booking -> {
                    setupBookingsContent()
                    // Sync bookings khi quay lại tab bookings để đảm bảo đồng bộ với admin
                    syncBookingsFromBackendSilent()
                }
                R.id.navigation_profile -> {
                    setupAccountContent()
                }
            }
        }
    }

    private fun setupBottomNav() {
        bottomNav.isItemActiveIndicatorEnabled = false
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_search -> {
                    loadContent(R.layout.content_search)
                    setupSearchContent()
                    // Sync rooms khi chuyển sang tab search để đảm bảo đồng bộ với admin
                    // Sync ngay lập tức để user thấy thay đổi từ admin
                    syncRoomsFromBackendSilent()
                    true
                }
                R.id.navigation_saved -> {
                    if (!sessionManager.isLoggedIn()) {
                        openLogin("Vui lòng đăng nhập để xem phòng đã lưu")
                        false
                    } else {
                        loadContent(R.layout.content_saved)
                        setupSavedContent()
                        true
                    }
                }
                R.id.navigation_booking -> {
                    if (!sessionManager.isLoggedIn()) {
                        openLogin("Vui lòng đăng nhập để xem đặt chỗ")
                        false
                    } else {
                        loadContent(R.layout.content_bookings)
                        setupBookingsContent()
                        true
                    }
                }
                R.id.navigation_profile -> {
                    loadContent(R.layout.content_account)
                    setupAccountContent()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadContent(@LayoutRes layoutId: Int) {
        contentContainer.removeAllViews()
        layoutInflater.inflate(layoutId, contentContainer, true)
        
        // Setup content sau khi load
        if (layoutId == R.layout.content_search) {
            setupSearchContent()
        }
    }

    private fun setupSearchContent() {
        val swipeRefresh = contentContainer.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        val recyclerView = contentContainer.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_rooms)
        val tvGreeting = contentContainer.findViewById<TextView>(R.id.tv_home_greeting)
        val tvResultCount = contentContainer.findViewById<TextView>(R.id.tv_result_count)
        val emptyView = contentContainer.findViewById<TextView>(R.id.tv_empty_rooms)

        tvGreeting?.text = sessionManager.getUserName()
            ?.takeIf { sessionManager.isLoggedIn() && it.isNotBlank() }
            ?.let { "Xin chào, $it" }
            ?: "Xin chào, Khách tham quan"
        
        // Setup SwipeRefreshLayout
        swipeRefresh?.setOnRefreshListener {
            syncRoomsFromBackendWithRefresh(swipeRefresh)
        }
        
        // Sync rooms mỗi khi setup search content để đảm bảo có dữ liệu mới nhất từ admin
        syncRoomsFromBackendSilent()
        
        recyclerView?.let { rv ->
            val userId = sessionManager.getUserId()
            
            // Nếu userId thay đổi, reset adapter và favoriteStatusMap
            if (currentUserId != userId) {
                currentUserId = userId
                favoriteStatusMap.clear()
                roomAdapter = null
                searchResultsJob?.cancel()
            }
            
            // Tạo adapter nếu chưa có hoặc đã reset
            if (roomAdapter == null) {
                roomAdapter = RoomAdapter(
                    onRoomClick = { room ->
                        // Navigate to room detail
                        val intent = Intent(this@MainActivity, RoomDetailActivity::class.java)
                        intent.putExtra("room_id", room.id)
                        startActivity(intent)
                    },
                    onFavoriteClick = { room, _ ->
                        if (userId != -1L) {
                            viewModel.toggleFavorite(userId, room.id)
                        } else {
                            openLogin("Vui lòng đăng nhập để lưu phòng yêu thích")
                        }
                    },
                    getFavoriteStatus = { roomId ->
                        favoriteStatusMap[roomId] ?: false
                    }
                )
                
                // Observe favorite room IDs với userId hiện tại
                if (userId != -1L) {
                    lifecycleScope.launch {
                        repeatOnLifecycle(Lifecycle.State.STARTED) {
                            repository.getFavoriteRoomIds(userId).collect { favoriteRoomIds: List<Long> ->
                                favoriteStatusMap.clear()
                                favoriteRoomIds.forEach { roomId: Long ->
                                    favoriteStatusMap[roomId] = true
                                }
                                // Notify adapter để update UI
                                roomAdapter?.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
            
            rv.layoutManager = LinearLayoutManager(this)
            rv.adapter = roomAdapter

            // Setup search
            setupSearch()

            searchResultsJob?.cancel()
            searchResultsJob = lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.searchResults.collect { rooms ->
                        Log.d("MainActivity", "Received ${rooms.size} rooms from database")
                        roomAdapter?.submitList(rooms)
                        tvResultCount?.text = "${rooms.size} phòng phù hợp"
                        emptyView?.visibility = if (rooms.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                        rv.visibility = if (rooms.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
                    }
                }
            }
        }
    }

    private fun setupSearch() {
        val etSearch = contentContainer.findViewById<TextInputEditText>(R.id.et_search)
        val tvCheckInDate = contentContainer.findViewById<TextView>(R.id.tv_check_in_date)
        val tvCheckOutDate = contentContainer.findViewById<TextView>(R.id.tv_check_out_date)
        val layoutCheckIn = contentContainer.findViewById<LinearLayout>(R.id.layout_check_in)
        val layoutCheckOut = contentContainer.findViewById<LinearLayout>(R.id.layout_check_out)
        val btnSearch = contentContainer.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_search)
        val btnGuestMinus = contentContainer.findViewById<MaterialButton>(R.id.btn_guest_minus)
        val btnGuestPlus = contentContainer.findViewById<MaterialButton>(R.id.btn_guest_plus)
        val tvGuestCount = contentContainer.findViewById<TextView>(R.id.tv_guest_count)
        val btnSort = contentContainer.findViewById<MaterialButton>(R.id.btn_sort)
        val btnClearFilters = contentContainer.findViewById<MaterialButton>(R.id.btn_clear_filters)

        // Setup search text watcher
        etSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
        })

        fun updateGuestCount(delta: Int) {
            viewModel.setGuestCount(viewModel.guestCount.value + delta)
        }
        btnGuestMinus?.setOnClickListener { updateGuestCount(-1) }
        btnGuestPlus?.setOnClickListener { updateGuestCount(1) }
        btnSort?.setOnClickListener { viewModel.cycleSortOrder() }
        btnClearFilters?.setOnClickListener {
            viewModel.clearSearchFilters()
            etSearch?.setText("")
            tvCheckInDate?.text = "Chọn ngày"
            tvCheckOutDate?.text = "Chọn ngày"
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.guestCount.collect { count -> tvGuestCount?.text = count.toString() }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sortOrder.collect { sort -> btnSort?.text = sort.label }
            }
        }

        // Setup date pickers
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
        val dateFormatDisplay = SimpleDateFormat("EEE, dd MMM", Locale("vi", "VN"))

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
                    val timestamp = selectedDate.timeInMillis
                    viewModel.setCheckInDate(timestamp)
                    tvCheckInDate?.text = dateFormatDisplay.format(selectedDate.time)
                    if (viewModel.checkOutDate.value?.let { it <= timestamp } == true) {
                        tvCheckOutDate?.text = "Chọn ngày"
                    }
                },
                year, month, day
            ).apply {
                datePicker.minDate = System.currentTimeMillis() - 1000
                show()
            }
        }

        layoutCheckOut?.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDay)
                    }
                    val timestamp = selectedDate.timeInMillis
                    viewModel.setCheckOutDate(timestamp)
                    tvCheckOutDate?.text = dateFormatDisplay.format(selectedDate.time)
                },
                year, month, day
            ).apply {
                // Check-out date phải sau check-in date
                viewModel.checkInDate.value?.let { checkInTimestamp ->
                    datePicker.minDate = checkInTimestamp + (24 * 60 * 60 * 1000) // +1 day
                } ?: run {
                    datePicker.minDate = System.currentTimeMillis() - 1000
                }
                show()
            }
        }

        // Observe date changes to update check-out min date
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.checkInDate.collect { checkInTimestamp ->
                    checkInTimestamp?.let {
                        tvCheckInDate?.text = dateFormatDisplay.format(it)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.checkOutDate.collect { checkOutTimestamp ->
                    checkOutTimestamp?.let {
                        tvCheckOutDate?.text = dateFormatDisplay.format(it)
                    }
                }
            }
        }

        btnSearch?.setOnClickListener {
            val checkIn = viewModel.checkInDate.value
            val checkOut = viewModel.checkOutDate.value
            if ((checkIn == null) != (checkOut == null)) {
                Toast.makeText(this, "Vui lòng chọn đủ ngày nhận và trả phòng", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.setSearchQuery(etSearch?.text?.toString().orEmpty())
                contentContainer.findViewById<android.view.View>(R.id.tv_room_section_title)
                    ?.requestFocus()
            }
        }
    }

    private fun setupSavedContent() {
        val recyclerView = contentContainer.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_favorites)
        val emptyView = contentContainer.findViewById<android.view.View>(R.id.tv_empty_favorites)
        recyclerView?.let { rv ->
            val userId = sessionManager.getUserId()
            
            if (userId == -1L) {
                // User chưa đăng nhập
                favoriteAdapter?.submitList(emptyList())
                return
            }

            // Nếu userId thay đổi, reset adapter
            if (currentUserId != userId) {
                currentUserId = userId
                favoriteAdapter = null
                isObservingFavorites = false
            }

            // Tạo adapter nếu chưa có hoặc đã reset
            if (favoriteAdapter == null) {
                favoriteAdapter = RoomAdapter(
                    onRoomClick = { room ->
                        // Navigate to room detail
                        val intent = Intent(this@MainActivity, RoomDetailActivity::class.java)
                        intent.putExtra("room_id", room.id)
                        startActivity(intent)
                    },
                    onFavoriteClick = { room, _ ->
                        // Remove from favorites
                        viewModel.toggleFavorite(userId, room.id)
                    },
                    getFavoriteStatus = { _ -> true } // Tất cả đều là favorite trong tab này
                )
            }

            rv.layoutManager = LinearLayoutManager(this)
            rv.adapter = favoriteAdapter

            // Restart observation với userId mới nếu chưa observe hoặc userId đã đổi
            if (!isObservingFavorites || currentUserId != userId) {
                isObservingFavorites = true
                lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        viewModel.getFavoriteRooms(userId).collect { rooms ->
                            favoriteAdapter?.submitList(rooms)
                            emptyView?.visibility = if (rooms.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                            rv.visibility = if (rooms.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun setupBookingsContent() {
        val swipeRefresh = contentContainer.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_bookings)
        val recyclerView = contentContainer.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_bookings)
        val emptyView = contentContainer.findViewById<android.view.View>(R.id.tv_empty_bookings)
        
        // Setup SwipeRefreshLayout
        swipeRefresh?.setOnRefreshListener {
            syncBookingsFromBackendWithRefresh(swipeRefresh)
        }
        
        recyclerView?.let { rv ->
            val userId = sessionManager.getUserId()
            
            if (userId == -1L) {
                // User chưa đăng nhập
                bookingAdapter?.submitList(emptyList())
                return
            }

            // Sync bookings mỗi khi setup bookings content để đảm bảo có dữ liệu mới nhất từ admin
            syncBookingsFromBackendSilent()

            // Nếu userId thay đổi, reset adapter
            if (currentUserId != userId) {
                currentUserId = userId
                bookingAdapter = null
                isObservingBookings = false
            }

            // Tạo adapter nếu chưa có hoặc đã reset
            if (bookingAdapter == null) {
                bookingAdapter = BookingAdapter { booking ->
                    // Navigate to booking detail hoặc room detail
                    val intent = Intent(this@MainActivity, RoomDetailActivity::class.java)
                    intent.putExtra("room_id", booking.roomId)
                    startActivity(intent)
                }
            }

            rv.layoutManager = LinearLayoutManager(this)
            rv.adapter = bookingAdapter

            // Restart observation với userId mới nếu chưa observe hoặc userId đã đổi
            if (!isObservingBookings || currentUserId != userId) {
                isObservingBookings = true
                lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        // Lấy mongoUserId từ SessionManager
                        val mongoUserId = sessionManager.getMongoUserId()
                        if (mongoUserId != null) {
                            // Dùng API để load bookings
                            viewModel.getBookingsWithRoomInfoViaAPI(mongoUserId, userId).collect { bookings ->
                                bookingAdapter?.submitList(bookings)
                                emptyView?.visibility = if (bookings.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                                rv.visibility = if (bookings.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
                            }
                        } else {
                            // Fallback: dùng local bookings
                            viewModel.getBookingsWithRoomInfo(userId).collect { bookings ->
                                bookingAdapter?.submitList(bookings)
                                emptyView?.visibility = if (bookings.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                                rv.visibility = if (bookings.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupAccountContent() {
        val tvUserName = contentContainer.findViewById<TextView>(R.id.tv_user_name)
        val tvUserEmail = contentContainer.findViewById<TextView>(R.id.tv_user_email)
        val tvUserPhone = contentContainer.findViewById<TextView>(R.id.tv_user_phone)
        val rowUserPhone = contentContainer.findViewById<android.view.View>(R.id.row_user_phone)
        val tvMembership = contentContainer.findViewById<TextView>(R.id.tv_membership)
        val btnEditProfile = contentContainer.findViewById<MaterialButton>(R.id.btn_edit_profile)
        val btnLogout = contentContainer.findViewById<MaterialButton>(R.id.btn_logout)

        val userId = sessionManager.getUserId()
        val mongoUserId = sessionManager.getMongoUserId()
        
        if (userId != -1L) {
            lifecycleScope.launch {
                // Nếu có MongoDB ID, sync dữ liệu mới nhất từ backend
                if (mongoUserId != null) {
                    val syncResult = authRepository.syncUserFromBackend(userId, mongoUserId)
                    if (syncResult.isSuccess) {
                        val user = syncResult.getOrNull()
                        runOnUiThread {
                            tvUserName?.text = user?.fullName
                            tvUserEmail?.text = user?.email
                            tvUserPhone?.text = user?.phone
                            rowUserPhone?.visibility = android.view.View.VISIBLE
                        }
                        return@launch
                    }
                    // Nếu sync fail, fallback sang Room DB
                }
                
                // Fallback: Load từ Room DB (local cache)
                repository.getUserById(userId)?.let { user ->
                    runOnUiThread {
                        tvUserName?.text = user.fullName
                        tvUserEmail?.text = user.email
                        tvUserPhone?.text = user.phone
                        rowUserPhone?.visibility = android.view.View.VISIBLE
                    }
                } ?: run {
                    // Nếu không tìm thấy user, dùng thông tin từ session
                    runOnUiThread {
                        tvUserName?.text = sessionManager.getUserName() ?: "Người dùng"
                        tvUserEmail?.text = sessionManager.getUserEmail() ?: ""
                        rowUserPhone?.visibility = android.view.View.GONE
                    }
                }
            }
        } else {
            tvUserName?.text = "Khách tham quan"
            tvUserEmail?.text = "Chưa đăng nhập"
            rowUserPhone?.visibility = android.view.View.GONE
            tvMembership?.text = "Chưa có"
            btnEditProfile?.visibility = android.view.View.GONE
            btnLogout?.text = "Đăng nhập"
        }

        // Xử lý chỉnh sửa thông tin
        btnEditProfile?.setOnClickListener {
            if (userId != -1L) {
                showEditProfileDialog(userId)
            } else {
                Toast.makeText(this, "Vui lòng đăng nhập để chỉnh sửa thông tin", Toast.LENGTH_SHORT).show()
            }
        }

        // Xử lý đăng xuất
        btnLogout?.setOnClickListener {
            if (sessionManager.isLoggedIn()) {
                sessionManager.clearSession()
            }
            openLogin(null)
        }
    }

    private fun openLogin(message: String?) {
        message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun showEditProfileDialog(userId: Long) {
        val dialog = BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        dialogView.setBackgroundColor(android.graphics.Color.WHITE)
        dialog.setContentView(dialogView)

        val etFullName = dialogView.findViewById<TextInputEditText>(R.id.et_full_name)
        val etEmail = dialogView.findViewById<TextInputEditText>(R.id.et_email)
        val etPhone = dialogView.findViewById<TextInputEditText>(R.id.et_phone)
        val etPassword = dialogView.findViewById<TextInputEditText>(R.id.et_password)
        val etConfirmPassword = dialogView.findViewById<TextInputEditText>(R.id.et_confirm_password)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btn_save)

        // Load current user info
        lifecycleScope.launch {
            repository.getUserById(userId)?.let { user ->
                runOnUiThread {
                    etFullName?.setText(user.fullName)
                    etEmail?.setText(user.email)
                    etPhone?.setText(user.phone)
                }
            }
        }

        btnCancel?.setOnClickListener {
            dialog.dismiss()
        }

        btnSave?.setOnClickListener {
            val fullName = etFullName?.text?.toString()?.trim() ?: ""
            // Email và phone KHÔNG cho phép sửa nữa
            val newPassword = etPassword?.text?.toString()?.trim() ?: ""
            val confirmPassword = etConfirmPassword?.text?.toString()?.trim() ?: ""

            // Validation - CHỈ validate fullName và password
            // Validate Full Name
            if (fullName.isEmpty()) {
                etFullName?.error = "Vui lòng nhập họ và tên"
                return@setOnClickListener
            }
            if (!InputValidator.validateFullName(fullName)) {
                etFullName?.error = "Họ và tên không hợp lệ (2-50 ký tự, chỉ chữ cái)"
                return@setOnClickListener
            }

            // Validate Password mới (nếu có nhập) - Tính năng 2: Validation mạnh mẽ
            if (newPassword.isNotEmpty()) {
                // Kiểm tra độ dài tối thiểu (8 ký tự)
                if (newPassword.length < 8) {
                    etPassword?.error = "Mật khẩu phải có ít nhất 8 ký tự"
                    return@setOnClickListener
                }
                
                // Kiểm tra độ mạnh mật khẩu và yêu cầu tối thiểu
                val (isValid, strength) = InputValidator.validatePassword(newPassword)
                
                // Kiểm tra mật khẩu có đáp ứng yêu cầu tối thiểu không
                // Yêu cầu: 8+ ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt
                if (!InputValidator.isPasswordValid(newPassword)) {
                    val errorMessage = InputValidator.getPasswordErrorMessage(newPassword)
                    etPassword?.error = errorMessage
                    return@setOnClickListener
                }
                
                // Kiểm tra độ mạnh mật khẩu (không chấp nhận mật khẩu yếu)
                if (!isValid || strength == InputValidator.PasswordStrength.WEAK) {
                    val errorMessage = InputValidator.getPasswordErrorMessage(newPassword)
                    etPassword?.error = errorMessage
                    return@setOnClickListener
                }
                
                // Kiểm tra xác nhận mật khẩu
                if (confirmPassword.isEmpty()) {
                    etConfirmPassword?.error = "Vui lòng xác nhận mật khẩu mới"
                    return@setOnClickListener
                }
                
                if (newPassword != confirmPassword) {
                    etConfirmPassword?.error = "Mật khẩu xác nhận không khớp"
                    return@setOnClickListener
                }
            } else if (confirmPassword.isNotEmpty()) {
                // Nếu không nhập mật khẩu mới nhưng có xác nhận
                etConfirmPassword?.error = "Vui lòng nhập mật khẩu mới trước"
                return@setOnClickListener
            }

            // Disable button
            btnSave?.isEnabled = false
            btnSave?.text = "Đang lưu..."

            lifecycleScope.launch {
                try {
                    // Lấy MongoDB User ID từ session
                    var mongoUserId = sessionManager.getMongoUserId()
                    
                    // Nếu không có MongoDB ID (user đăng nhập trước update) → Yêu cầu đăng nhập lại
                    if (mongoUserId == null) {
                        runOnUiThread {
                            btnSave?.isEnabled = true
                            btnSave?.text = "Lưu"
                            
                            // Hiển thị dialog yêu cầu đăng nhập lại
                            android.app.AlertDialog.Builder(this@MainActivity)
                                .setTitle("Cần đăng nhập lại")
                                .setMessage("Để cập nhật thông tin, vui lòng đăng xuất và đăng nhập lại.\n\nLý do: Hệ thống đã được cập nhật và cần xác thực lại tài khoản.")
                                .setPositiveButton("Đăng xuất ngay") { _, _ ->
                                    // Đăng xuất và chuyển về LoginActivity
                                    sessionManager.clearSession()
                                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                                .setNegativeButton("Hủy", null)
                                .show()
                        }
                        return@launch
                    }
                    
                    // Gọi backend API để update user (CHỈ fullName và password)
                    val result = authRepository.updateUser(
                        localUserId = userId,
                        mongoUserId = mongoUserId,
                        fullName = fullName,
                        newPassword = if (newPassword.isNotEmpty()) newPassword else null
                    )
                    
                    if (result.isSuccess) {
                        val updatedUser = result.getOrNull()
                        
                        // Cập nhật session với tên mới
                        if (updatedUser != null) {
                            sessionManager.saveSession(
                                updatedUser.id,
                                mongoUserId,
                                updatedUser.email,
                                updatedUser.fullName
                            )
                        }
                        
                        runOnUiThread {
                            dialog.dismiss()
                            Toast.makeText(this@MainActivity, "Đã cập nhật thông tin thành công", Toast.LENGTH_SHORT).show()
                            // Reload account content
                            setupAccountContent()
                        }
                    } else {
                        val errorMsg = result.exceptionOrNull()?.message ?: "Cập nhật thất bại"
                        runOnUiThread {
                            btnSave?.isEnabled = true
                            btnSave?.text = "Lưu"
                            Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error updating user: ${e.message}", e)
                    runOnUiThread {
                        btnSave?.isEnabled = true
                        btnSave?.text = "Lưu"
                        Toast.makeText(this@MainActivity, "Có lỗi xảy ra: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }
}
