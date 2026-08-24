package com.example.homestay;

import static org.junit.Assert.*;

import com.example.homestay.domain.BookingStatusPolicy;
import org.junit.Test;

public class BookingStatusPolicyTest {
  @Test
  public void allowsOnlyExpectedTransitions() {
    assertTrue(BookingStatusPolicy.canTransition("pending", "confirmed"));
    assertTrue(BookingStatusPolicy.canTransition("pending", "cancelled"));
    assertTrue(BookingStatusPolicy.canTransition("confirmed", "completed"));
    assertTrue(BookingStatusPolicy.canTransition("confirmed", "checked_in"));
    assertTrue(BookingStatusPolicy.canTransition("checked_in", "completed"));
    assertFalse(BookingStatusPolicy.canTransition("pending", "completed"));
    assertFalse(BookingStatusPolicy.canTransition("completed", "cancelled"));
    assertFalse(BookingStatusPolicy.canTransition("cancelled", "confirmed"));
    assertFalse(BookingStatusPolicy.canTransition("expired", "confirmed"));
    assertFalse(BookingStatusPolicy.canTransition("checked_in", "cancelled"));
  }

  @Test
  public void completionRequiresCheckoutToHavePassed() {
    long now = 10_000L;
    assertFalse(BookingStatusPolicy.canComplete("confirmed", now + 1, now));
    assertTrue(BookingStatusPolicy.canComplete("confirmed", now, now));
    assertTrue(BookingStatusPolicy.canComplete("checked_in", now, now));
    assertFalse(BookingStatusPolicy.canComplete("pending", now, now));
  }

  @Test
  public void cancellationStopsAtCheckIn() {
    long now = 10_000L;
    assertTrue(BookingStatusPolicy.canCancel("pending", now + 1, now));
    assertTrue(BookingStatusPolicy.canCancel("confirmed", now + 1, now));
    assertFalse(BookingStatusPolicy.canCancel("confirmed", now, now));
    assertFalse(BookingStatusPolicy.canCancel("completed", now + 1, now));
  }
}
