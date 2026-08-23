package com.example.homestay.data.entity;
import androidx.annotation.NonNull;import androidx.room.*;
@Entity(tableName="reviews",indices={@Index(value={"roomId"}),@Index(value={"userId"}),@Index(value={"bookingId"},unique=true)}) public class Review {
 @PrimaryKey(autoGenerate=true) final long id; final long bookingId,roomId,userId; final int rating; @NonNull final String comment; final boolean isVisible; final long createdAt,updatedAt;
 public Review(long id,long bookingId,long roomId,long userId,int rating,String comment,boolean isVisible,long createdAt,long updatedAt){this.id=id;this.bookingId=bookingId;this.roomId=roomId;this.userId=userId;this.rating=rating;this.comment=comment;this.isVisible=isVisible;this.createdAt=createdAt;this.updatedAt=updatedAt;}
 public long getId(){return id;}public long getBookingId(){return bookingId;}public long getRoomId(){return roomId;}public long getUserId(){return userId;}public int getRating(){return rating;}public String getComment(){return comment;}public boolean isVisible(){return isVisible;}public long getCreatedAt(){return createdAt;}public long getUpdatedAt(){return updatedAt;}public Review withId(long v){return new Review(v,bookingId,roomId,userId,rating,comment,isVisible,createdAt,updatedAt);}public Review edited(int r,String c,long t){return new Review(id,bookingId,roomId,userId,r,c,isVisible,createdAt,t);}
}
