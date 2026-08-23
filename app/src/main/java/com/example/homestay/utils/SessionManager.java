package com.example.homestay.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

public final class SessionManager {
  private static final String PREFS_NAME = "HomestaySession";
  private static final String KEY_USER_ID = "user_id", KEY_MONGO_USER_ID = "mongo_user_id";
  private static final String KEY_IS_LOGGED_IN = "is_logged_in",
      KEY_USER_EMAIL = "user_email",
      KEY_USER_NAME = "user_name";
  private final SharedPreferences prefs;

  public SessionManager(Context context) {
    prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }

  public void saveSession(long userId, @Nullable String mongoUserId, String email, String name) {
    prefs
        .edit()
        .putLong(KEY_USER_ID, userId)
        .putString(KEY_MONGO_USER_ID, mongoUserId)
        .putBoolean(KEY_IS_LOGGED_IN, true)
        .putString(KEY_USER_EMAIL, email)
        .putString(KEY_USER_NAME, name)
        .apply();
  }

  public long getUserId() {
    return prefs.getLong(KEY_USER_ID, -1L);
  }

  @Nullable
  public String getMongoUserId() {
    return prefs.getString(KEY_MONGO_USER_ID, null);
  }

  @Nullable
  public String getUserEmail() {
    return prefs.getString(KEY_USER_EMAIL, null);
  }

  @Nullable
  public String getUserName() {
    return prefs.getString(KEY_USER_NAME, null);
  }

  @Nullable
  public String getFullName() {
    return getUserName();
  }

  public boolean isLoggedIn() {
    return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getUserId() != -1L;
  }

  public void clearSession() {
    prefs.edit().clear().apply();
  }
}
