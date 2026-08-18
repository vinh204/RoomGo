package com.example.homestay.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.homestay.R
import com.example.homestay.data.model.AdminUserData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminUserAdapter(
    private val users: MutableList<AdminUserData>,
    private val onDeleteClick: (AdminUserData) -> Unit,
    private val onUnlockClick: (AdminUserData) -> Unit
) : RecyclerView.Adapter<AdminUserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<AdminUserData>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    fun removeUser(userId: String) {
        val index = users.indexOfFirst { it.id == userId }
        if (index != -1) {
            users.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAvatar: TextView = itemView.findViewById(R.id.tv_avatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tv_user_name)
        private val tvUserEmail: TextView = itemView.findViewById(R.id.tv_user_email)
        private val tvUserPhone: TextView = itemView.findViewById(R.id.tv_user_phone)
        private val tvLockStatus: TextView = itemView.findViewById(R.id.tv_lock_status)
        private val btnUnlock: ImageButton = itemView.findViewById(R.id.btn_unlock)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(user: AdminUserData) {
            // Avatar: Lấy chữ cái đầu
            val firstChar = user.fullName.firstOrNull()?.uppercaseChar() ?: 'U'
            tvAvatar.text = firstChar.toString()
            
            tvUserName.text = user.fullName
            tvUserEmail.text = user.email
            tvUserPhone.text = user.phone

            // Hiển thị trạng thái locked
            // Log để debug
            android.util.Log.d("AdminUserAdapter", "Binding user: ${user.email}, locked: ${user.locked}, permanent: ${user.permanent}, failedAttempts: ${user.failedLoginAttempts}, lockedUntil: ${user.lockedUntil}, secondsRemaining: ${user.secondsRemaining}")
            
            val isLocked = user.locked == true
            val isPermanent = user.permanent == true
            if (isLocked) {
                if (isPermanent) {
                    // Khóa vĩnh viễn
                    tvLockStatus.text = "🔒 Bị khóa vĩnh viễn"
                    tvLockStatus.setTextColor(0xFFD32F2F.toInt()) // Dark red
                } else {
                    // Khóa tạm thời
                    val secondsRemaining = user.secondsRemaining ?: 0
                    val minutes = secondsRemaining / 60
                    val seconds = secondsRemaining % 60
                    val timeText = if (minutes > 0) {
                        "$minutes phút ${seconds} giây"
                    } else {
                        "$seconds giây"
                    }
                    tvLockStatus.text = "🔒 Bị khóa - Còn lại: $timeText"
                    tvLockStatus.setTextColor(0xFFFF5722.toInt()) // Orange red
                }
                tvLockStatus.visibility = View.VISIBLE
                btnUnlock.visibility = View.VISIBLE
            } else if (user.failedLoginAttempts != null && user.failedLoginAttempts > 0) {
                // Hiển thị cả khi có failed attempts nhưng chưa bị khóa
                tvLockStatus.text = "⚠️ ${user.failedLoginAttempts} lần đăng nhập sai"
                tvLockStatus.setTextColor(0xFFFF9800.toInt()) // Orange
                tvLockStatus.visibility = View.VISIBLE
                btnUnlock.visibility = View.GONE
            } else {
                // Ẩn nếu không có thông tin
                tvLockStatus.visibility = View.GONE
                btnUnlock.visibility = View.GONE
            }
            
            // Luôn hiển thị nút unlock nếu có failed attempts (cho phép reset)
            if (user.failedLoginAttempts != null && user.failedLoginAttempts > 0 && !isLocked) {
                btnUnlock.visibility = View.VISIBLE
            }

            btnUnlock.setOnClickListener {
                onUnlockClick(user)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(user)
            }
        }
    }
}

