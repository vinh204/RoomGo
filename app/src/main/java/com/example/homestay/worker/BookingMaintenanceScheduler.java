package com.example.homestay.worker;

import android.content.Context;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

/** Lên lịch kiểm tra booking khi mở ứng dụng và định kỳ khi ứng dụng không ở màn hình. */
public final class BookingMaintenanceScheduler {
  private static final String PERIODIC_WORK = "roomgo_booking_maintenance";

  private BookingMaintenanceScheduler() {}

  public static void schedule(Context context) {
    WorkManager manager = WorkManager.getInstance(context);
    manager.enqueue(new OneTimeWorkRequest.Builder(BookingMaintenanceWorker.class).build());
    PeriodicWorkRequest periodic =
        new PeriodicWorkRequest.Builder(BookingMaintenanceWorker.class, 15, TimeUnit.MINUTES)
            .build();
    manager.enqueueUniquePeriodicWork(
        PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, periodic);
  }
}
