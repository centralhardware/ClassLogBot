package me.centralhardware.znatoki.telegram.statistic.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BotLoginTest {
    private val token = "123:abc"
    private val user = TelegramUser(42, "Ann", null, null)

    @Test
    fun `signed credential validates and expires`() {
        val data = BotLogin.sign(42, 1_000, token)
        assertEquals(42L, BotLogin.validate(data, token, now = 1_000)?.id)
        assertNull(BotLogin.validate(data, "other:token", now = 1_000))
        assertNull(BotLogin.validate(data.replaceFirst("42.", "43."), token, now = 1_000))
        assertNull(BotLogin.validate(data, token, now = 1_000 + WebConfig.LOGIN_MAX_AGE_SECONDS + 1))
    }

    @Test
    fun `token is confirmed by the bot once and then handed out once`() {
        val t = BotLogin.start(now = 0)
        assertNull(BotLogin.poll(t, now = 1, botToken = token))
        assertFalse(BotLogin.confirm("unknown", user, now = 1))
        assert(BotLogin.confirm(t, user, now = 1))
        assertNotNull(BotLogin.poll(t, now = 2, botToken = token))
        assertNull(BotLogin.poll(t, now = 3, botToken = token))
    }

    @Test
    fun `expired token cannot be confirmed`() {
        val t = BotLogin.start(now = 0)
        assertFalse(BotLogin.confirm(t, user, now = 11 * 60))
    }
}
