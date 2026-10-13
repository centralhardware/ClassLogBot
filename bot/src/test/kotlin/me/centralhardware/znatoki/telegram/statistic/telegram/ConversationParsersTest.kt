package me.centralhardware.znatoki.telegram.statistic.telegram

import me.centralhardware.telegram.conversation.Parsed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConversationParsersTest {

    @Test
    fun `parseFio accepts two or three words and collapses whitespace`() {
        assertEquals(Parsed.Ok(Triple("Иванов", "Иван", "Иванович")), parseFio("  Иванов   Иван Иванович "))
        assertEquals(Parsed.Ok(Triple("Иванов", "Иван", "")), parseFio("Иванов Иван"))
        assertTrue(parseFio("Иванов") is Parsed.Err)
        assertTrue(parseFio("a b c d") is Parsed.Err)
    }

    @Test
    fun `parseFio reports duplicates`() {
        assertTrue(parseFio("Иванов Иван", duplicateCheck = { false }) is Parsed.Err)
    }

    @Test
    fun `parsePhone and parseDateInput`() {
        assertTrue(parsePhone("79131234567") is Parsed.Ok)
        assertTrue(parsePhone("12345") is Parsed.Err)
        assertTrue(parseDateInput("01 09 2026") is Parsed.Ok)
        assertTrue(parseDateInput("2026-09-01") is Parsed.Err)
    }
}
