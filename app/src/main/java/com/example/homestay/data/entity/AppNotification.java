package com.example.homestay.data.entity;
import androidx.annotation.NonNull;import androidx.annotation.Nullable;import androidx.room.*;
@Entity(tableName="notifications",indices={@Index(value={"userId"}),@Index(value={"eventKey"},unique=true)}) public class AppNotification {
 @PrimaryKey(autoGenerate=true) final long id; final long userId; @NonNull final String eventKey; @NonNull final String title; @NonNull final String message; @NonNull final String type; final Long bookingId,roomId; final boolean isRead; final long createdAt;
 public AppNotification(long id,long userId,String eventKey,String title,String message,String type,@Nullable Long bookingId,@Nullable Long roomId,boolean isRead,long createdAt){this.id=id;this.userId=userId;this.eventKey=eventKey;this.title=title;this.message=message;this.type=type;this.bookingId=bookingId;this.roomId=roomId;this.isRead=isRead;this.createdAt=createdAt;}
 public long getId(){return id;}public long getUserId(){return userId;}public String getEventKey(){return eventKey;}public String getTitle(){return title;}public String getMessage(){return message;}public String getType(){return type;}public Long getBookingId(){return bookingId;}public Long getRoomId(){return roomId;}public boolean isRead(){return isRead;}public long getCreatedAt(){return createdAt;}
}
