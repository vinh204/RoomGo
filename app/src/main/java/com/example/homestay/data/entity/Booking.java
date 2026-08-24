package com.example.homestay.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.*;

@Entity(
    tableName = "bookings",
    indices = {
      @Index(value = {"roomId"}),
      @Index(value = {"slotId"}),
      @Index(value = {"userId"}),
      @Index(value = {"mongoId"})
    })
public class Booking {
  @PrimaryKey(autoGenerate = true)
  final long id;

  final String mongoId;
  final long roomId;
  final String mongoRoomId;
  final Long slotId;
  final String mongoSlotId;
  final long userId;
  final String mongoUserId;
  final long checkInDate, checkOutDate;
  final int guestCount;
  final double totalPrice;
  @NonNull final String status;
  final String paymentMethod;
  final long createdAt;
  final String cancellationReason;
  final long cancelledAt;
  final double refundAmount;
  @NonNull final String paymentStatus;
  final long expiresAt;

  @Ignore
  public Booking(
      long id,
      @Nullable String mongoId,
      long roomId,
      @Nullable String mongoRoomId,
      @Nullable Long slotId,
      @Nullable String mongoSlotId,
      long userId,
      @Nullable String mongoUserId,
      long checkInDate,
      long checkOutDate,
      int guestCount,
      double totalPrice,
      String status,
      @Nullable String paymentMethod,
      long createdAt,
      @Nullable String cancellationReason,
      long cancelledAt,
      double refundAmount) {
    this(
        id,
        mongoId,
        roomId,
        mongoRoomId,
        slotId,
        mongoSlotId,
        userId,
        mongoUserId,
        checkInDate,
        checkOutDate,
        guestCount,
        totalPrice,
        status,
        paymentMethod,
        createdAt,
        cancellationReason,
        cancelledAt,
        refundAmount,
        "UNPAID",
        0);
  }

  public Booking(
      long id,
      @Nullable String mongoId,
      long roomId,
      @Nullable String mongoRoomId,
      @Nullable Long slotId,
      @Nullable String mongoSlotId,
      long userId,
      @Nullable String mongoUserId,
      long checkInDate,
      long checkOutDate,
      int guestCount,
      double totalPrice,
      String status,
      @Nullable String paymentMethod,
      long createdAt,
      @Nullable String cancellationReason,
      long cancelledAt,
      double refundAmount,
      @NonNull String paymentStatus,
      long expiresAt) {
    this.id = id;
    this.mongoId = mongoId;
    this.roomId = roomId;
    this.mongoRoomId = mongoRoomId;
    this.slotId = slotId;
    this.mongoSlotId = mongoSlotId;
    this.userId = userId;
    this.mongoUserId = mongoUserId;
    this.checkInDate = checkInDate;
    this.checkOutDate = checkOutDate;
    this.guestCount = guestCount;
    this.totalPrice = totalPrice;
    this.status = status;
    this.paymentMethod = paymentMethod;
    this.createdAt = createdAt;
    this.cancellationReason = cancellationReason;
    this.cancelledAt = cancelledAt;
    this.refundAmount = refundAmount;
    this.paymentStatus = paymentStatus;
    this.expiresAt = expiresAt;
  }

  public long getId() {
    return id;
  }

  public String getMongoId() {
    return mongoId;
  }

  public long getRoomId() {
    return roomId;
  }

  public String getMongoRoomId() {
    return mongoRoomId;
  }

  public Long getSlotId() {
    return slotId;
  }

  public String getMongoSlotId() {
    return mongoSlotId;
  }

  public long getUserId() {
    return userId;
  }

  public String getMongoUserId() {
    return mongoUserId;
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

  public long getCreatedAt() {
    return createdAt;
  }

  public String getCancellationReason() {
    return cancellationReason;
  }

  public long getCancelledAt() {
    return cancelledAt;
  }

  public double getRefundAmount() {
    return refundAmount;
  }

  public String getPaymentStatus() {
    return paymentStatus;
  }

  public long getExpiresAt() {
    return expiresAt;
  }

  public Booking withId(long v) {
    return new Booking(
        v,
        mongoId,
        roomId,
        mongoRoomId,
        slotId,
        mongoSlotId,
        userId,
        mongoUserId,
        checkInDate,
        checkOutDate,
        guestCount,
        totalPrice,
        status,
        paymentMethod,
        createdAt,
        cancellationReason,
        cancelledAt,
        refundAmount,
        paymentStatus,
        expiresAt);
  }

  public Booking withStatus(String s, @Nullable String p) {
    return new Booking(
        id,
        mongoId,
        roomId,
        mongoRoomId,
        slotId,
        mongoSlotId,
        userId,
        mongoUserId,
        checkInDate,
        checkOutDate,
        guestCount,
        totalPrice,
        s,
        p,
        createdAt,
        cancellationReason,
        cancelledAt,
        refundAmount,
        paymentStatus,
        expiresAt);
  }

  public Booking cancelled(String reason, double refund, long time) {
    return new Booking(
        id,
        mongoId,
        roomId,
        mongoRoomId,
        slotId,
        mongoSlotId,
        userId,
        mongoUserId,
        checkInDate,
        checkOutDate,
        guestCount,
        totalPrice,
        "cancelled",
        paymentMethod,
        createdAt,
        reason,
        time,
        refund,
        refund > 0 ? "REFUND_PENDING" : paymentStatus,
        expiresAt);
  }

  public Booking withPaymentStatus(String value) {
    return new Booking(
        id, mongoId, roomId, mongoRoomId, slotId, mongoSlotId, userId, mongoUserId,
        checkInDate, checkOutDate, guestCount, totalPrice, status, paymentMethod, createdAt,
        cancellationReason, cancelledAt, refundAmount, value, expiresAt);
  }
}
