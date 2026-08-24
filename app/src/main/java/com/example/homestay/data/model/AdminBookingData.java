package com.example.homestay.data.model;

import androidx.annotation.Nullable;

public final class AdminBookingData {
  private final String id, status, paymentMethod, paymentStatus, slotId;
  private final AdminBookingUser user;
  private final AdminBookingRoom room;
  private final long checkInDate, checkOutDate, createdAt;
  private final int guestCount;
  private final double totalPrice;

  public AdminBookingData(
      String id,
      @Nullable AdminBookingUser u,
      @Nullable AdminBookingRoom r,
      long in,
      long out,
      int guests,
      double total,
      String status,
      @Nullable String payment,
      String paymentStatus,
      long created,
      @Nullable String slot) {
    this.id = id;
    user = u;
    room = r;
    checkInDate = in;
    checkOutDate = out;
    guestCount = guests;
    totalPrice = total;
    this.status = status;
    paymentMethod = payment;
    this.paymentStatus = paymentStatus;
    createdAt = created;
    slotId = slot;
  }

  public String getId() {
    return id;
  }

  public AdminBookingUser getUser() {
    return user;
  }

  public AdminBookingRoom getRoom() {
    return room;
  }

  public long getCheckInDate() {
    return checkInDate;
  }

  public long getCheckOutDate() {
    return checkOutDate;
  }

  public int getGuestCount() {
    return guestCount;
  }

  public double getTotalPrice() {
    return totalPrice;
  }

  public String getStatus() {
    return status;
  }

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public String getPaymentStatus() {
    return paymentStatus;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public String getSlotId() {
    return slotId;
  }
}
