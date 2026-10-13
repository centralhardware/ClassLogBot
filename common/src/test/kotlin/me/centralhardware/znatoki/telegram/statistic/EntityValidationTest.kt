package me.centralhardware.znatoki.telegram.statistic

import me.centralhardware.znatoki.telegram.statistic.entity.Amount
import me.centralhardware.znatoki.telegram.statistic.entity.PhoneNumber
import me.centralhardware.znatoki.telegram.statistic.entity.SchoolClass
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EntityValidationTest {

    @Test
    fun `amount must be positive`() {
        assertTrue(Amount.validate(1))
        assertFalse(Amount.validate(0))
        assertFalse(Amount.validate(-5))
        assertFalse(Amount.validate(null))
        assertThrows<IllegalArgumentException> { Amount(0) }
    }

    @Test
    fun `school class is 1 to 11`() {
        assertTrue(SchoolClass.validate(1))
        assertTrue(SchoolClass.validate(11))
        assertFalse(SchoolClass.validate(0))
        assertFalse(SchoolClass.validate(12))
        assertFalse(SchoolClass.validate(null))
    }

    @Test
    fun `phone is 11 digits starting with 7 or 8`() {
        assertTrue(PhoneNumber.validate("79131234567"))
        assertTrue(PhoneNumber.validate("89131234567"))
        assertFalse(PhoneNumber.validate("+79131234567"))
        assertFalse(PhoneNumber.validate("9131234567"))
        assertFalse(PhoneNumber.validate("69131234567"))
    }
}
