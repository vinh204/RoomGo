package com.example.homestay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.example.homestay.domain.BookingRules;
import com.example.homestay.utils.DisplayFormatter;
import org.junit.Test;

public class BookingRulesTest {
  private static final long TODAY = 1_000_000L;

  @Test
  public void validBookingIsAccepted() {
    assertNull(BookingRules.validate(TODAY, TODAY + 86_400_000, 2, 4, 0, 2, TODAY));
  }

  @Test
  public void pastCheckInIsRejected() {
    assertNotNull(BookingRules.validate(TODAY - 1, TODAY + 1, 1, 2, 0, 1, TODAY));
  }

  @Test
  public void checkoutBeforeCheckInIsRejected() {
    assertNotNull(BookingRules.validate(TODAY, TODAY, 1, 2, 0, 1, TODAY));
  }

  @Test
  public void tooManyGuestsAreRejected() {
    assertNotNull(BookingRules.validate(TODAY, TODAY + 1, 5, 4, 0, 1, TODAY));
  }

  @Test
  public void fullRoomIsRejected() {
    assertNotNull(BookingRules.validate(TODAY, TODAY + 1, 2, 4, 2, 2, TODAY));
  }

  @Test
  public void bookingCodeUsesRoomGoFormat() {
    assertEquals("#RG700101003", DisplayFormatter.bookingCode(3, 0));
  }
}
