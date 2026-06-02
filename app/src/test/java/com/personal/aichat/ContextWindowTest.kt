package com.personal.aichat

import com.personal.aichat.domain.parseContextWindowTokensInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContextWindowTest {
  @Test
  fun parsesPlainAndGroupedTokenCounts() {
    assertEquals(128_000, parseContextWindowTokensInput("128000"))
    assertEquals(1_000_000, parseContextWindowTokensInput("1,000,000"))
    assertEquals(2_000_000, parseContextWindowTokensInput("2_000_000"))
  }

  @Test
  fun parsesKAndMSuffixes() {
    assertEquals(400_000, parseContextWindowTokensInput("400K"))
    assertEquals(128_000, parseContextWindowTokensInput("128k"))
    assertEquals(1_000_000, parseContextWindowTokensInput("1M"))
    assertEquals(1_500_000, parseContextWindowTokensInput("1.5m"))
  }

  @Test
  fun rejectsInvalidOrOverflowingValues() {
    assertNull(parseContextWindowTokensInput(""))
    assertNull(parseContextWindowTokensInput("tokens"))
    assertNull(parseContextWindowTokensInput("0"))
    assertNull(parseContextWindowTokensInput("-1"))
    assertNull(parseContextWindowTokensInput("3000M"))
  }
}
