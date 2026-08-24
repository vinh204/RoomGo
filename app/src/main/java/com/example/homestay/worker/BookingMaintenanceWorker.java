package com.example.homestay.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.homestay.HomestayApplication;
import com.example.homestay.data.entity.AppNotification;
import com.example.homestay.data.entity.Booking;
import com.example.homestay.data.repository.HomestayRepository;
import com.example.homestay.utils.SystemNotificationHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Tự động nhận phòng lúc 14:00, hoàn thành sau 12:00 ngày trả và phát thông báo. */
public class BookingMaintenanceWorker extends Worker {
  public BookingMaintenanceWorker(
      @NonNull Context appContext, @NonNull WorkerParameters workerParams) {
    super(appContext, workerParams);
  }

  @NonNull
  @Override
  public Result doWork() {
    try {
      HomestayApplication app = (HomestayApplication) getApplicationContext();
      HomestayRepository repository = app.getRepository();
      List<Booking> changed = repository.updateDueBookingLifecycle(System.currentTimeMillis());
      if (changed.isEmpty()) return Result.success();

      Set<Long> affectedUsers = new HashSet<>();
      for (Booking booking : changed) affectedUsers.add(booking.getUserId());
      for (long userId : affectedUsers) {
        repository.syncBookingNotifications(userId);
        SystemNotificationHelper.publishNew(
            app, repository.getCustomerNotificationsNow(userId));
      }

      List<AppNotification> adminNotifications = repository.syncAdminActivityNotifications();
      SystemNotificationHelper.publishNew(app, adminNotifications);
      return Result.success();
    } catch (RuntimeException error) {
      return Result.retry();
    }
  }
}
