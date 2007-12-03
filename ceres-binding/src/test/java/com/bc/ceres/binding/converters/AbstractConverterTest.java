package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import junit.framework.TestCase;

import java.lang.reflect.Array;

public abstract class AbstractConverterTest extends TestCase {


    protected AbstractConverterTest() {
    }

    public abstract Converter getConverter();

    public abstract void testConverter() throws ConversionException;

    protected void testValueType(Class<?> expectedType) {
        assertEquals(expectedType, getConverter().getValueType());
    }

    protected void testParseSuccess(Object expectedValue, String text) throws ConversionException {
        if (expectedValue != null && expectedValue.getClass().isArray()) {
            Object actualValue = getConverter().parse(text);
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
            assertEquals(expectedValue, getConverter().parse(text));
        }
        if (expectedValue != null) {
            assertTrue(getConverter().getValueType().isAssignableFrom(expectedValue.getClass()));
        }
    }

    protected void testParseFailed(String text) {
        try {
            getConverter().parse(text);
            fail("ConversionException expected: " + text + " should not be convertible");
        } catch (ConversionException e) {
            // OK!
        }
    }

    protected void testFormatSuccess(String expectedText, Object value) throws ConversionException {
        if (value != null) {
            assertTrue(getConverter().getValueType().isAssignableFrom(value.getClass()));
        }
        assertEquals(expectedText, getConverter().format(value));
    }

    public void assertNullCorrectlyHandled() {
        try {
            getConverter().parse(null);
            fail("NullPointerException expected");
        } catch (ConversionException e) {
            fail("ConversionException not expected");
        } catch (NullPointerException e) {
            // expected
        }
        String text = getConverter().format(null);
        // if null is supported, returned text shall not be null
        assertNotNull(text);
    }
}
