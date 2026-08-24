package com.example.homestay.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.homestay.data.dao.*;
import com.example.homestay.data.entity.*;

@Database(
    entities = {
      com.example.homestay.data.entity.Room.class,
      Slot.class,
      Booking.class,
      User.class,
      Favorite.class,
      AppNotification.class,
      Review.class,
      RoomImage.class
    },
    version = 18,
    exportSchema = false)
public abstract class HomestayDatabase extends RoomDatabase {
  public abstract RoomDao roomDao();

  public abstract SlotDao slotDao();

  public abstract BookingDao bookingDao();

  public abstract UserDao userDao();

  public abstract FavoriteDao favoriteDao();

  public abstract NotificationDao notificationDao();

  public abstract ReviewDao reviewDao();

  public abstract RoomImageDao roomImageDao();

  private static volatile HomestayDatabase INSTANCE;

  public static HomestayDatabase getDatabase(Context context) {
    if (INSTANCE == null)
      synchronized (HomestayDatabase.class) {
        if (INSTANCE == null)
          INSTANCE =
              Room.databaseBuilder(
                      context.getApplicationContext(), HomestayDatabase.class, "homestay_database")
                  .addMigrations(
                      MIGRATION_7_8,
                      MIGRATION_8_9,
                      MIGRATION_9_10,
                      MIGRATION_10_11,
                      MIGRATION_11_12,
                      MIGRATION_12_13,
                      MIGRATION_13_14,
                      MIGRATION_14_15,
                      MIGRATION_15_16,
                      MIGRATION_16_17,
                      MIGRATION_17_18)
                  .build();
      }
    return INSTANCE;
  }

  static final Migration MIGRATION_7_8 =
      new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
          db.execSQL("ALTER TABLE rooms ADD COLUMN isFeatured INTEGER NOT NULL DEFAULT 0");
        }
      };
  static final Migration MIGRATION_8_9 =
      new Migration(8, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
          db.execSQL(
              "CREATE TABLE IF NOT EXISTS notifications (id INTEGER PRIMARY KEY AUTOINCREMENT NOT"
                  + " NULL, userId INTEGER NOT NULL, eventKey TEXT NOT NULL, title TEXT NOT NULL,"
                  + " message TEXT NOT NULL, type TEXT NOT NULL, bookingId INTEGER, roomId INTEGER,"
                  + " isRead INTEGER NOT NULL, createdAt INTEGER NOT NULL)");
          db.execSQL(
              "CREATE INDEX IF NOT EXISTS index_notifications_userId ON notifications(userId)");
          db.execSQL(
              "CREATE UNIQUE INDEX IF NOT EXISTS index_notifications_eventKey ON"
                  + " notifications(eventKey)");
        }
      };
  static final Migration MIGRATION_9_10 =
      new Migration(9, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
          db.execSQL("UPDATE rooms SET rating = 5.0 WHERE reviewCount = 0");
        }
      };
  static final Migration MIGRATION_10_11 =
      new Migration(10, 11) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
          db.execSQL(
              "CREATE TABLE IF NOT EXISTS reviews (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                  + " bookingId INTEGER NOT NULL, roomId INTEGER NOT NULL, userId INTEGER NOT NULL,"
                  + " rating INTEGER NOT NULL, comment TEXT NOT NULL, isVisible INTEGER NOT NULL,"
                  + " createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)");
          db.execSQL("CREATE INDEX IF NOT EXISTS index_reviews_roomId ON reviews(roomId)");
          db.execSQL("CREATE INDEX IF NOT EXISTS index_reviews_userId ON reviews(userId)");
          db.execSQL(
              "CREATE UNIQUE INDEX IF NOT EXISTS index_reviews_bookingId ON reviews(bookingId)");
        }
      };
  static final Migration MIGRATION_11_12 =
      new Migration(11, 12) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
          db.execSQL(
              "UPDATE rooms SET reviewCount = (SELECT COUNT(*) FROM reviews WHERE reviews.roomId ="
                  + " rooms.id AND reviews.isVisible = 1), rating = COALESCE((SELECT"
                  + " AVG(reviews.rating) FROM reviews WHERE reviews.roomId = rooms.id AND"
                  + " reviews.isVisible = 1), 5.0)");
        }
      };
  // The Kotlin-to-Java conversion only changed Room's generated schema identity.
  // No table or column changed, so opening v12 only needs a versioned no-op migration.
  static final Migration MIGRATION_12_13 =
      new Migration(12, 13) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {}
      };
  static final Migration MIGRATION_13_14 =
      new Migration(13, 14) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
          db.execSQL(
              "CREATE TABLE IF NOT EXISTS room_images (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                  + " roomId INTEGER NOT NULL, imageUri TEXT NOT NULL, position INTEGER NOT NULL,"
                  + " FOREIGN KEY(roomId) REFERENCES rooms(id) ON UPDATE NO ACTION ON DELETE CASCADE)");
          db.execSQL("CREATE INDEX IF NOT EXISTS index_room_images_roomId ON room_images(roomId)");
          db.execSQL(
              "INSERT INTO room_images (roomId, imageUri, position) SELECT id, imageUrl, 0 FROM rooms"
                  + " WHERE imageUrl != ''");
        }
      };
  static final Migration MIGRATION_14_15 =
      new Migration(14, 15) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
          db.execSQL("ALTER TABLE bookings ADD COLUMN cancellationReason TEXT");
          db.execSQL("ALTER TABLE bookings ADD COLUMN cancelledAt INTEGER NOT NULL DEFAULT 0");
          db.execSQL("ALTER TABLE bookings ADD COLUMN refundAmount REAL NOT NULL DEFAULT 0");
        }
      };
  static final Migration MIGRATION_15_16 =
      new Migration(15, 16) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
          // Các phiên bản cũ lưu ngày ở 00:00; chuyển sang 14:00 nhận và 12:00 trả phòng.
          db.execSQL("UPDATE bookings SET checkInDate = checkInDate + 50400000");
          db.execSQL("UPDATE bookings SET checkOutDate = checkOutDate + 43200000");
        }
      };
  static final Migration MIGRATION_16_17 =
      new Migration(16, 17) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
          db.execSQL("ALTER TABLE users ADD COLUMN locked INTEGER NOT NULL DEFAULT 0");
        }
      };
  static final Migration MIGRATION_17_18 =
      new Migration(17, 18) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
          db.execSQL("ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'CUSTOMER'");
          db.execSQL("UPDATE users SET role='ADMIN' WHERE LOWER(email)='admin@gmail.com'");
          db.execSQL("ALTER TABLE bookings ADD COLUMN paymentStatus TEXT NOT NULL DEFAULT 'UNPAID'");
          db.execSQL("ALTER TABLE bookings ADD COLUMN expiresAt INTEGER NOT NULL DEFAULT 0");
          db.execSQL(
              "UPDATE bookings SET expiresAt=createdAt+7200000 WHERE status='pending'");
          db.execSQL("UPDATE bookings SET paymentStatus='PAID' WHERE status='completed'");
          db.execSQL(
              "UPDATE bookings SET paymentStatus='REFUND_PENDING' WHERE status='cancelled' AND refundAmount>0");
        }
      };
}
