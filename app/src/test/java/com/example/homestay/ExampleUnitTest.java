package com.example.homestay;

import static org.junit.Assert.*;

import com.example.homestay.domain.BookingRules;
import org.junit.Test;

public class ExampleUnitTest {
  private final long today = 1_000_000L;

  @Test
  public void validBookingIsAccepted() {
    assertNull(BookingRules.validate(today, today + 86_400_000, 2, 4, 0, 2, today));
  }

  @Test
  public void pastCheckInIsRejected() {
    assertNotNull(BookingRules.validate(today - 1, today + 1, 1, 2, 0, 1, today));
  }

  @Test
  public void checkoutBeforeCheckinIsRejected() {
    assertNotNull(BookingRules.validate(today, today, 1, 2, 0, 1, today));
  }

  @Test
  public void tooManyGuestsAreRejected() {
    assertNotNull(BookingRules.validate(today, today + 1, 5, 4, 0, 1, today));
  }

  @Test
  public void fullRoomIsRejected() {
    assertNotNull(BookingRules.validate(today, today + 1, 2, 4, 2, 2, today));
  }
}
