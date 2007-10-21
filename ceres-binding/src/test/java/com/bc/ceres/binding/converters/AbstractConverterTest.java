package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import junit.framework.TestCase;

import java.lang.reflect.Array;

public abstract class AbstractConverterTest extends TestCase {

    private com.bc.ceres.binding.Converter converter;

    protected AbstractConverterTest(com.bc.ceres.binding.Converter converter) {
        this.converter = converter;
    }

    public abstract void testConverter() throws ConversionException;

    protected void testValueType(Class<?> expectedType) {
        assertEquals(expectedType, converter.getValueType());
    }

    protected void testParseSuccess(Object expectedValue, String text) throws ConversionException {
        if (expectedValue != null && expectedValue.getClass().isArray()) {
            Object actualValue = converter.parse(text);
            assertTrue(actualValue.getClass().isArray());
            int expectedLength = Array.getLength(expectedValue);
            int actualLength = Array.getLength(actualValue);
            assertEquals(expectedLength, actualLength);
            for (int i = 0; i < expectedLength; i++) {
                Object expectedElem = Array.get(expectedValue, i);
                Object actualElem = Array.get(actualValue, i);
                assertEquals("index=" + i, expectedElem, actualElem);
            }
        } else {
            assertEquals(expectedValue, converter.parse(text));
        }
        if (expectedValue != null) {
            assertTrue(converter.getValueType().isAssignableFrom(expectedValue.getClass()));
        }
    }

    protected void testParseFailed(String text) {
        try {
            converter.parse(text);
            fail("ConversionException expected: " + text + " should not be convertible");
        } catch (ConversionException e) {
            // OK!
        }
    }

    protected void testFormatSuccess(String expectedText, Object value) throws ConversionException {
        if (value != null) {
            assertTrue(converter.getValueType().isAssignableFrom(value.getClass()));
        }
        assertEquals(expectedText, converter.format(value));
    }

    public void assertNullCorrectlyHandled() {
        try {
            converter.parse(null);
            fail("NullPointerException expected");
        } catch (ConversionException e) {
            fail("ConversionException not expected");
        } catch (NullPointerException e) {
            // expected
        }
        String text = converter.format(null);
        // if null is supported, returned text shall not be null
        assertNotNull(text);
    }
}
