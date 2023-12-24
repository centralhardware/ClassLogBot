package me.centralhardware.znatoki.telegram.statistic.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class provides tests for the TelephoneUtils.java class. 
 * Specifically, it validates the functionality of the validate() method which checks 
 * if a provided string is a valid phone number according to a specific regex pattern.
 */
public class TelephoneUtilsTest {

    /**
     * This test verifies the validate() method with a valid phone number.
     */
    @Test
    void testValidateWithValidNumber() {
        String validPhoneNumber = "71234567890";
        assertTrue(TelephoneUtils.validate(validPhoneNumber));
    }

    @Test
    void testValidateWithValidNumber2() {
        String validPhoneNumber = "81234567890";
        assertTrue(TelephoneUtils.validate(validPhoneNumber));
    }

    /**
     * This test verifies the validate() method with an invalid phone number.
     */
    @Test
    void testValidateWithInvalidNumber() {
        String invalidPhoneNumber = "11234567890";
        assertFalse(TelephoneUtils.validate(invalidPhoneNumber));
    }

    /**
     * This test verifies the validate() method with a null value.
     */
    @Test
    void testValidateWithNullValue() {
        String nullPhoneNumber = null;
        assertFalse(TelephoneUtils.validate(nullPhoneNumber));
    }

    /**
     * This class is testing the TelephoneUtils class, specifically the format method.
     * The format method in TelephoneUtils is responsible for formatting the string representation
     * of a telephone number in a specific manner defined by a MaskFormatter. The tests below will
     * examine different input cases for this method and verify the correct output.
     */
    @Test
    public void format_NullOrEmpty_ReturnEmptyString() {
        String telephone = null;
        String result = TelephoneUtils.format(telephone);
        assertEquals("", result);

        telephone = "";
        result = TelephoneUtils.format(telephone);
        assertEquals("", result);
    }

    @Test
    public void format_ValidTelephone_ReturnsFormattedString() {
        String telephone = "98765432101";
        String result = TelephoneUtils.format(telephone);
        assertEquals("9-876-543-21-01", result);
    }

    @Test
    public void format_IncorrectTelephone_ReturnsEmptyString() {
        String telephone = "incorrect_phone";
        String result = TelephoneUtils.format(telephone);
        assertEquals("", result);
    }

}