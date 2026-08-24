package com.example.homestay;

import static org.junit.Assert.assertEquals;

import com.example.homestay.domain.CancellationPolicy;
import org.junit.Test;

public class CancellationPolicyTest {
  private static final double TOTAL = 2_000_000d;

  @Test
  public void refundsAllAtLeastTwentyFourHoursBeforeCheckIn() {
    assertEquals(
        TOTAL,
        CancellationPolicy.refund(TOTAL, CancellationPolicy.FULL_REFUND_WINDOW_MS, 0),
        0d);
  }

  @Test
  public void refundsHalfWithinTwentyFourHours() {
    assertEquals(TOTAL / 2, CancellationPolicy.refund(TOTAL, 1_000, 500), 0d);
  }

  @Test
  public void refundsNothingAfterCheckIn() {
    assertEquals(0d, CancellationPolicy.refund(TOTAL, 500, 1_000), 0d);
  }
}
