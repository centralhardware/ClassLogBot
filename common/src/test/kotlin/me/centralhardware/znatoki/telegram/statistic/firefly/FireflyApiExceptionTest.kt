package me.centralhardware.znatoki.telegram.statistic.firefly

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FireflyApiExceptionTest {

    @Test
    fun `422 duplicate-hash response is a duplicate`() {
        val body = """{"message":"The given data was invalid.","errors":{"transactions.0.description":["Duplicate of transaction #1234."]}}"""
        assertTrue(FireflyApiException("create transaction", 422, body).isDuplicateTransaction)
    }

    @Test
    fun `other failures are not duplicates`() {
        assertFalse(FireflyApiException("create transaction", 500, "Duplicate of transaction #1").isDuplicateTransaction)
        assertFalse(FireflyApiException("create transaction", 422, """{"errors":{"amount":["required"]}}""").isDuplicateTransaction)
    }
}
