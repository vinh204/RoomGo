package com.example.homestay.ui.adapter;
import androidx.annotation.Nullable;import com.example.homestay.data.entity.Booking;import com.example.homestay.data.entity.Room;
public final class BookingWithRoom{private final Booking booking;private final Room room;public BookingWithRoom(Booking booking,@Nullable Room room){this.booking=booking;this.room=room;}public Booking getBooking(){return booking;}@Nullable public Room getRoom(){return room;}}
