package me.centralhardware.znatoki.telegram.statistic.validate;

import io.vavr.control.Either;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AmountValidatorTest {

    /**
     * Test class for AmountValidator validates that a string can be correctly parsed to an integer, 
     * and that the integer is greater than zero.
     */
    private AmountValidator validator = new AmountValidator();
  
    // Testing when a valid positive integer string is provided.
    @Test
    void testValidate_ValidPositiveIntegerString() {
        String value = "10";
        Either<String, Integer> result = validator.validate(value);
        assertTrue(result.isRight());
        assertEquals(10, result.get());
    }

    // Testing when a negative integer string is provided.
    @Test
    void testValidate_NegativeIntegerString() {
        String value = "-10";
        Either<String, Integer> result = validator.validate(value);
        assertTrue(result.isLeft());
        assertEquals("Введенное значение должно быть больше нуля", result.getLeft());
    }

    // Testing when zero string is provided.
    @Test
    void testValidate_ZeroString() {
        String value = "0";
        Either<String, Integer> result = validator.validate(value);
        assertTrue(result.isLeft());
        assertEquals("Введенное значение должно быть больше нуля", result.getLeft());
    }

    // Testing when a non-integer string is provided.
    @Test
    void testValidate_NonIntegerString() {
        String value = "Not a number";
        Either<String, Integer> result = validator.validate(value);
        assertTrue(result.isLeft());
        assertEquals("Введенное значение должно быть числом", result.getLeft());
    }

    // Testing when an empty string is provided.
    @Test
    void testValidate_EmptyString() {
        String value = "";
        Either<String, Integer> result = validator.validate(value);
        assertTrue(result.isLeft());
        assertEquals("Введенное значение должно быть числом", result.getLeft());
    }
}