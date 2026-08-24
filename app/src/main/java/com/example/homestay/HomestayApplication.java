package com.example.homestay;

import android.app.Application;
import com.example.homestay.data.database.HomestayDatabase;
import com.example.homestay.data.repository.*;
import com.example.homestay.utils.AppExecutors;
import com.example.homestay.utils.SystemNotificationHelper;

public class HomestayApplication extends Application {
  private HomestayDatabase database;
  private HomestayRepository repository;
  private AuthRepository authRepository;
  private BookingRepository bookingRepository;

  public HomestayDatabase getDatabase() {
    if (database == null) database = HomestayDatabase.getDatabase(this);
    return database;
  }

  public HomestayRepository getRepository() {
    if (repository == null) {
      HomestayDatabase d = getDatabase();
      repository =
          new HomestayRepository(
              d.roomDao(),
              d.slotDao(),
              d.bookingDao(),
              d.userDao(),
              d.favoriteDao(),
              d.notificationDao(),
              d.reviewDao(),
              d.roomImageDao());
    }
    return repository;
  }

  public AuthRepository getAuthRepository() {
    if (authRepository == null) authRepository = new AuthRepository(getDatabase().userDao());
    return authRepository;
  }

  public BookingRepository getBookingRepository() {
    if (bookingRepository == null)
      bookingRepository =
          new BookingRepository(
              getDatabase().bookingDao(), getDatabase().roomDao(), getDatabase().userDao());
    return bookingRepository;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    SystemNotificationHelper.createChannel(this);
    AppExecutors.io()
        .execute(
            () -> {
              if (!getSharedPreferences("DemoData", MODE_PRIVATE)
                  .getBoolean("rooms_seeded", false)) {
                getRepository().seedLocalRoomsIfNeeded();
                getSharedPreferences("DemoData", MODE_PRIVATE)
                    .edit()
                    .putBoolean("rooms_seeded", true)
                    .apply();
              }
            });
  }
}
