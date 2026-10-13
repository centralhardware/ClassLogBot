package me.centralhardware.znatoki.telegram.statistic

import me.centralhardware.znatoki.telegram.statistic.extensions.isInSameMonthAs
import me.centralhardware.znatoki.telegram.statistic.extensions.parseDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DateTimeExtTest {

    @Test
    fun `parseDate reads dd MM yyyy`() {
        assertEquals(LocalDate.of(2026, 10, 13), "13 10 2026".parseDate().getOrThrow())
        assertTrue("2026-10-13".parseDate().isFailure)
        assertTrue(null.parseDate().isFailure)
    }

    @Test
    fun `isInSameMonthAs compares year and month`() {
        val d = LocalDateTime.of(2026, 10, 31, 23, 59, 59)
        assertTrue(d.isInSameMonthAs(LocalDateTime.of(2026, 10, 1, 0, 0)))
        assertFalse(d.isInSameMonthAs(LocalDateTime.of(2026, 11, 1, 0, 0)))
        assertFalse(d.isInSameMonthAs(LocalDateTime.of(2025, 10, 15, 12, 0)))
    }
}
