package com.example.homestay.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import java.util.concurrent.TimeUnit;

public final class RateLimiter {
  private static final String PREFS = "RateLimiterPrefs",
      FAILED = "failed_attempts_",
      LOCKED = "locked_until_",
      LAST = "last_attempt_",
      COUNT = "lock_count_";
  private static final int MAX_ATTEMPTS = 5;
  private static final long LOCK_SECONDS = 100L * 365 * 24 * 60 * 60;

  private RateLimiter() {}

  public static AttemptStatus canAttemptLogin(Context context, String id) {
    SharedPreferences prefs = prefs(context);
    long until = prefs.getLong(LOCKED + id, 0);
    if (until > 0 && System.currentTimeMillis() >= until) {
      prefs.edit().remove(LOCKED + id).apply();
      return new AttemptStatus(true, null);
    }
    return until > System.currentTimeMillis()
        ? new AttemptStatus(false, until)
        : new AttemptStatus(true, null);
  }

  public static void recordSuccess(Context context, String id) {
    reset(context, id);
  }

  public static FailureStatus recordFailure(Context context, String id) {
    SharedPreferences p = prefs(context);
    long now = System.currentTimeMillis();
    long until = p.getLong(LOCKED + id, 0);
    if (until > now) return new FailureStatus(0, until);
    int attempts = until > 0 ? 0 : p.getInt(FAILED + id, 0);
    attempts++;
    SharedPreferences.Editor editor =
        p.edit().putInt(FAILED + id, attempts).putLong(LAST + id, now);
    if (attempts >= MAX_ATTEMPTS) {
      long newUntil = now + LOCK_SECONDS * 1000;
      editor.putLong(LOCKED + id, newUntil).apply();
      return new FailureStatus(0, newUntil);
    }
    editor.apply();
    return new FailureStatus(MAX_ATTEMPTS - attempts, null);
  }

  public static int getRemainingAttempts(Context context, String id) {
    return Math.max(0, MAX_ATTEMPTS - getFailedAttempts(context, id));
  }

  public static long getLockedSecondsRemaining(Context context, String id) {
    long remaining = prefs(context).getLong(LOCKED + id, 0) - System.currentTimeMillis();
    return remaining > 0 ? TimeUnit.MILLISECONDS.toSeconds(remaining) + 1 : 0;
  }

  public static long getLockedMinutesRemaining(Context context, String id) {
    long s = getLockedSecondsRemaining(context, id);
    return s / 60 + (s % 60 > 0 ? 1 : 0);
  }

  public static void syncFromBackend(
      Context context,
      String id,
      @Nullable Integer attempts,
      @Nullable Object lockedUntil,
      @Nullable Integer remaining) {
    SharedPreferences.Editor editor = prefs(context).edit();
    if (attempts != null) editor.putInt(FAILED + id, attempts);
    if (lockedUntil != null) {
      long value = 0;
      try {
        if (lockedUntil instanceof String) {
          java.text.SimpleDateFormat format =
              new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", java.util.Locale.US);
          java.util.Date parsed = format.parse((String) lockedUntil);
          value = parsed == null ? 0 : parsed.getTime();
        } else value = ((Number) lockedUntil).longValue();
      } catch (Exception ignored) {
      }
      if (value > 0) editor.putLong(LOCKED + id, value);
      else editor.remove(LOCKED + id);
    }
    editor.apply();
  }

  public static int getFailedAttempts(Context context, String id) {
    return prefs(context).getInt(FAILED + id, 0);
  }

  public static void reset(Context context, String id) {
    prefs(context)
        .edit()
        .remove(FAILED + id)
        .remove(LOCKED + id)
        .remove(LAST + id)
        .remove(COUNT + id)
        .apply();
  }

  public static void lock(Context context, String id) {
    long lockedUntil = System.currentTimeMillis() + LOCK_SECONDS * 1000;
    prefs(context)
        .edit()
        .putInt(FAILED + id, MAX_ATTEMPTS)
        .putLong(LOCKED + id, lockedUntil)
        .apply();
  }

  private static SharedPreferences prefs(Context context) {
    return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }

  public static final class AttemptStatus {
    public final boolean allowed;
    public final Long lockedUntil;

    AttemptStatus(boolean a, Long l) {
      allowed = a;
      lockedUntil = l;
    }
  }

  public static final class FailureStatus {
    public final int remainingAttempts;
    public final Long lockedUntil;

    FailureStatus(int r, Long l) {
      remainingAttempts = r;
      lockedUntil = l;
    }
  }
}
