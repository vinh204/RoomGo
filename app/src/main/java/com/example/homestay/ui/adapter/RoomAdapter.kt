package com.example.homestay.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.homestay.R
import com.example.homestay.data.entity.Room
import java.text.NumberFormat
import java.util.Locale

class RoomAdapter(
    private val onRoomClick: (Room) -> Unit,
    private val onFavoriteClick: ((Room, Boolean) -> Unit)? = null,
    private val getFavoriteStatus: ((Long) -> Boolean)? = null
) : ListAdapter<Room, RoomAdapter.RoomViewHolder>(RoomDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgRoom: ImageView = itemView.findViewById(R.id.img_room)
        private val btnFavorite: ImageButton = itemView.findViewById(R.id.btn_favorite)
        private val tvRoomName: TextView = itemView.findViewById(R.id.tv_room_name)
        private val tvLocation: TextView = itemView.findViewById(R.id.tv_location)
        private val tvRating: TextView = itemView.findViewById(R.id.tv_rating)
        private val tvRoomType: TextView = itemView.findViewById(R.id.tv_room_type)
        private val tvAmenitiesTag: TextView = itemView.findViewById(R.id.tv_amenities_tag)
        private val tvPrice: TextView = itemView.findViewById(R.id.tv_price)
        private val tvCapacity: TextView = itemView.findViewById(R.id.tv_capacity)

        fun bind(room: Room) {
            tvRoomName.text = room.name
            tvLocation.text = room.location
            // Format rating với 1 chữ số thập phân (5.0)
            tvRating.text = String.format(Locale.getDefault(), "%.1f (%d)", room.rating, room.reviewCount)
            
            // Room type - hiển thị nếu có
            if (room.roomType.isNotEmpty()) {
                tvRoomType.text = room.roomType
                tvRoomType.visibility = View.VISIBLE
            } else {
                tvRoomType.visibility = View.GONE
            }
            
            // Amenities tag - lấy amenity đầu tiên từ danh sách
            val amenitiesTag = getFirstAmenity(room.amenities)
            if (amenitiesTag.isNotEmpty()) {
                tvAmenitiesTag.text = amenitiesTag
                tvAmenitiesTag.visibility = View.VISIBLE
            } else {
                tvAmenitiesTag.visibility = View.GONE
            }
            
            // Format price
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            val formattedPrice = formatter.format(room.price.toLong())
            tvPrice.text = "$formattedPrice đ / đêm"
            tvCapacity.text = "Tối đa ${room.maxGuests} khách · Còn phòng"

            // Load image from URL using Coil
            imgRoom.load(room.imageUrl) {
                placeholder(R.drawable.app_logo) // Placeholder while loading
                error(R.drawable.app_logo) // Error image if load fails
                crossfade(true) // Smooth transition
                listener(
                    onStart = { 
                        android.util.Log.d("RoomAdapter", "Loading image: ${room.imageUrl}")
                    },
                    onSuccess = { _, _ -> 
                        android.util.Log.d("RoomAdapter", "Image loaded successfully: ${room.imageUrl}")
                    },
                    onError = { _, result -> 
                        android.util.Log.e("RoomAdapter", "Failed to load image: ${room.imageUrl}, error: ${result.throwable.message}")
                    }
                )
            }

            // Setup favorite button
            val favorite = getFavoriteStatus?.invoke(room.id) ?: false
            updateFavoriteButton(favorite)

            btnFavorite.setOnClickListener {
                val newFavoriteState = !favorite
                updateFavoriteButton(newFavoriteState)
                onFavoriteClick?.invoke(room, newFavoriteState)
            }

            itemView.setOnClickListener {
                onRoomClick(room)
            }
        }

        private fun updateFavoriteButton(isFavorite: Boolean) {
            if (isFavorite) {
                btnFavorite.setImageResource(android.R.drawable.btn_star_big_on)
                btnFavorite.setColorFilter(android.graphics.Color.parseColor("#FFD700")) // Gold color
            } else {
                btnFavorite.setImageResource(android.R.drawable.btn_star_big_off)
                btnFavorite.setColorFilter(android.graphics.Color.WHITE)
            }
        }
        
        private fun getFirstAmenity(amenities: String): String {
            if (amenities.isEmpty()) return ""
            // Lấy amenity đầu tiên từ chuỗi (có thể là comma-separated hoặc JSON)
            val firstAmenity = amenities.split(",").firstOrNull()?.trim() ?: ""
            // Giới hạn độ dài để tránh tag quá dài
            return if (firstAmenity.length > 15) {
                firstAmenity.take(12) + "..."
            } else {
                firstAmenity
            }
        }
    }

    class RoomDiffCallback : DiffUtil.ItemCallback<Room>() {
        override fun areItemsTheSame(oldItem: Room, newItem: Room): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Room, newItem: Room): Boolean {
            return oldItem == newItem
        }
    }
}

