package com.example.homestay.data.dao;

import androidx.room.*;
import com.example.homestay.data.entity.AppNotification;
import java.util.List;

@Dao
public interface NotificationDao {
  @Query("SELECT * FROM notifications WHERE userId=:id ORDER BY createdAt DESC")
  List<AppNotification> getByUserNow(long id);

  @Query("SELECT * FROM notifications WHERE userId=:id AND type NOT LIKE 'admin_%' ORDER BY createdAt DESC")
  List<AppNotification> getCustomerByUserNow(long id);

  @Query("SELECT * FROM notifications WHERE userId=:id AND type LIKE 'admin_%' ORDER BY createdAt DESC")
  List<AppNotification> getAdminByUserNow(long id);

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  long insert(AppNotification n);

  @Query("UPDATE notifications SET isRead=1 WHERE id=:id")
  void markRead(long id);

  @Query("UPDATE notifications SET isRead=1 WHERE userId=:id")
  void markAllRead(long id);

  @Query("UPDATE notifications SET isRead=1 WHERE userId=:id AND type NOT LIKE 'admin_%'")
  void markAllCustomerRead(long id);

  @Query("UPDATE notifications SET isRead=1 WHERE userId=:id AND type LIKE 'admin_%'")
  void markAllAdminRead(long id);
}
