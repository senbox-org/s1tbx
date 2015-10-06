/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.util;

import com.bc.ceres.core.Assert;

/**
 * This utility class which provides several static <code>assert</code>XXX methods which can be used to internally check
 * the arguments passed to methods.
 * <p> All functions have been implemented with extreme caution in order to provide a maximum performance.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 */
public class Guardian {
    /**
     * Checks whether the given argument value is <code>true</code>. If not, an
     * <code>IllegalArgumentException</code> is thrown with the given message text.
     * <p>This utility method is used to check arguments passed into methods:
     * <pre>
     * public void setOrigin(double[] point) {
     *     Guardian.assertTrue("point.length == 2 || point.length == 3", point.length == 2 || point.length == 3);
     *     ...
     * }
     * </pre>
     *
     * @param message   the message text
     * @param condition the condition which must be true
     * @throws IllegalArgumentException if <code>condition</code> is <code>false</code>
     */
    public static void assertTrue(String message, boolean condition) {
        Assert.argument(condition, message);
    }

    /**
     * Checks whether the given argument value is not <code>null</code>. If not, an
     * <code>IllegalArgumentException</code> is thrown with a standardized message text using the supplied parameter
     * name.
     * <p>This utility method is used to check arguments passed into methods:
     * <pre>
     * public void setBounds(Rectangle bounds) {
     *     Guardian.assertNotNull("bounds", bounds);
     *     _bounds = rect;
     * }
     * </pre>
     *
     * @param exprText  the test expression as text
     * @param exprValue the test expression result
     * @throws IllegalArgumentException if <code>exprValue</code> is <code>null</code>
     */
    public static void assertNotNull(String exprText, Object exprValue) {
        if (exprValue == null) {
            throw new IllegalArgumentException("[" + exprText + "] is null");
        }
    }

    /**
     * Checks whether the given (parameter) value string is not <code>null</code> and not empty. If not, an
     * <code>IllegalArgumentException</code> is thrown with a standardized message text using the supplied parameter
     * name.
     * <p>This utility method is used to check arguments passed into methods:
     * <pre>
     * public void setProductId(String productId) {
     *     Guardian.assertNotNullOrEmpty("productId", productId);
     *     _productId = productId;
     * }
     * </pre>
     *
     * @param exprText  the test expression as text
     * @param exprValue the test expression result
     * @throws IllegalArgumentException if <code>exprValue</code> is <code>null</code> or an empty string
     */
    public static void assertNotNullOrEmpty(String exprText, String exprValue) {
        assertNotNull(exprText, exprValue);
        assertNonEmptyString(exprText, exprValue.length());
    }

    public static void assertNotNullOrEmpty(String exprText, byte[] exprValue) {
        assertNotNull(exprText, exprValue);
        assertNonEmptyArray(exprText, exprValue.length);
    }

    public static void assertNotNullOrEmpty(String exprText, char[] exprValue) {
        assertNotNull(exprText, exprValue);
        assertNonEmptyArray(exprText, exprValue.length);
    }

    public static void assertNotNullOrEmpty(String exprText, short[] exprValue) {
        assertNotNull(exprText, exprValue);
        assertNonEmptyArray(exprText, exprValue.length);
    }

    public static void assertNotNullOrEmpty(String exprText, int[] exprValue) {
        assertNotNull(exprText, exprValue);
        assertNonEmptyArray(exprText, exprValue.length);
    }

    public static void assertNotNullOrEmpty(String exprText, float[] exprValue) {
        assertNotNull(exprText, exprValue);
        assertNonEmptyArray(exprText, exprValue.length);
    }

    public static void assertNotNullOrEmpty(String exprText, double[] exprValue) {
        assertNotNull(exprText, exprValue);
        assertNonEmptyArray(exprText, exprValue.length);
    }

    public static void assertNotNullOrEmpty(String exprText, Object[] exprValue) {
        assertNotNull(exprText, exprValue);
        assertNonEmptyArray(exprText, exprValue.length);
    }

    /**
     * Checks if the given value is greater than the given limit. If not, an <code>IllegalArgumentException</code> is
     * thrown with a standardized message text using the supplied argument name.
     * <p>This utility method is used to check arguments passed into methods:
     * <pre>
     * public void setWeight(long weight) {
     *     Guardian.assertGreaterThan("weight", weight, 0);
     *     _weight = weight;
     * }
     * </pre>
     *
     * @param exprText  the test expression as text
     * @param exprValue the test expression result
     * @param limit     the lower limit for the expression result
     * @throws IllegalArgumentException if the <code>exprValue</code> is less than or equal to <code>limit</code>
     */
    public static void assertGreaterThan(String exprText, long exprValue, long limit) {
        if (exprValue <= limit) {
            StringBuffer sb = createExprStringBuffer(exprText)
                    .append(" is less than or equal to [")
                    .append(limit)
                    .append("]");
            throw new IllegalArgumentException(sb.toString());
        }
    }

    /**
     * Checks if the given values are equal. If not, an <code>IllegalArgumentException</code> is thrown with a
     * standardized message text using the supplied message.
     * <p>This utility method is used to check arguments passed into methods:
     * <pre>
     * public void writeDataAtRegion(int x, inty, int w, int h, byte[] data) {
     *     Guardian.assertEquals("data.length",
     *                           data.length, w * h);
     *     ...
     * }
     * </pre>
     *
     * @param exprText      the test expression as text
     * @param exprValue     the test expression result
     * @param expectedValue the expected value
     * @throws IllegalArgumentException if the <code>exprValue</code> is not equal to <code>expectedValue</code>
     */
    public static void assertEquals(String exprText, boolean exprValue, boolean expectedValue) {
        assertEquals(exprText, Boolean.valueOf(exprValue), Boolean.valueOf(expectedValue));
    }

    /**
     * Checks if the given values are equal. If not, an <code>IllegalArgumentException</code> is thrown with a
     * standardized message text using the supplied message.
     * <p>This utility method is used to check arguments passed into methods:
     * <pre>
     * public void writeDataAtRegion(int x, inty, int w, int h, byte[] data) {
     *     Guardian.assertEquals("data.length",
     *                           data.length, w * h);
     *     ...
     * }
     * </pre>
     *
     * @param exprText      the test expression as text
     * @param exprValue     the test expression result
     * @param expectedValue the expected value
     * @throws IllegalArgumentException if the <code>exprValue</code> is not equal to <code>expectedValue</code>
     */
    public static void assertEquals(String exprText, long exprValue, long expectedValue) {
        if (expectedValue != exprValue) {
            StringBuffer sb = createExprStringBuffer(exprText)
                    .append(" is [")
                    .append(exprValue)
                    .append("] but should be equal to [")
                    .append(expectedValue)
                    .append("]");
            throw new IllegalArgumentException(sb.toString());
        }
    }

    /**
     * Checks if the given objects are equal. If not, an <code>IllegalArgumentException</code> is thrown with a
     * standardized message text using the supplied message.
     * <p>This utility method is used to check arguments passed into methods:
     * <pre>
     * public NewBandDialog(final Window parent, ProductNodeList products) {
     *     Guardian.assertNotNull("products", products);
     *     Guardian.assertEquals("not the expected element type", Product.class, products.getElemType());
     *     ...
     * }
     * </pre>
     *
     * @param exprText      the test expression as text
     * @param exprValue     the test expression result
     * @param expectedValue the expected value
     * @throws IllegalArgumentException if the <code>exprValue</code> is not equal to <code>expectedValue</code>
     */
    public static void assertEquals(String exprText, Object exprValue, Object expectedValue) {
        if (!ObjectUtils.equalObjects(expectedValue, exprValue)) {
            StringBuffer sb = createExprStringBuffer(exprText)
                    .append(" is [")
                    .append(exprValue)
                    .append("] but should be equal to [")
                    .append(expectedValue)
                    .append("]");
            throw new IllegalArgumentException(sb.toString());
        }
    }

    /**
     * Checks if the given objects are the same instances. If not, an <code>IllegalArgumentException</code> is thrown
     * with a standardized message text using the supplied message.
     * <p>This utility method is used to check arguments passed into methods:
     * <pre>
     * public NewBandDialog(final Window parent, ProductNodeList products) {
     *     Guardian.assertNotNull("products", products);
     *     Guardian.assertEquals("not the expected element type", Product.class, products.getElemType());
     *     ...
     * }
     * </pre>
     *
     * @param exprText      the test expression as text
     * @param exprValue     the actual value
     * @param expectedValue the expected value
     * @throws IllegalArgumentException if the <code>expected</code> is not identical to <code>actual</code>
     */
    public static void assertSame(String exprText, Object exprValue, Object expectedValue) {
        if (expectedValue != exprValue) {
            StringBuffer sb = createExprStringBuffer(exprText)
                    .append(" is [")
                    .append(exprValue)
                    .append("] but should be same as [")
                    .append(expectedValue)
                    .append("]");
            throw new IllegalArgumentException(sb.toString());
        }
    }

    /**
     * Checks if the given value are in the given range. If not, an <code>IllegalArgumentException</code> is thrown with
     * a standardized message text using the supplied value name.
     * <p>This utility method is used to check arguments passed into methods:
     * <pre>
     * public void writeDataAtRegion(int x, inty, int w, int h, byte[] data) {
     *     Guardian.assertWithinRange("w", w, 0, data.length -1);
     *     ...
     * }
     * </pre>
     *
     * @param exprText  the test expression as text
     * @param exprValue the expression result
     * @param rangeMin  the range lower limit
     * @param rangeMax  the range upper limit
     * @throws IllegalArgumentException if the <code>exprValue</code> is less than <code>rangeMin</code> or greater than <code>rangeMax</code>
     */
    public static void assertWithinRange(String exprText, long exprValue, long rangeMin, long rangeMax) {
        if (exprValue < rangeMin || exprValue > rangeMax) {
            StringBuffer sb = createExprStringBuffer(exprText)
                    .append(" is [")
                    .append(exprValue)
                    .append("]  but should be in the range [")
                    .append(rangeMin)
                    .append("] to [")
                    .append(rangeMax)
                    .append("]");
            throw new IllegalArgumentException(sb.toString());
        }
    }

    /**
     * Checks if the given value are in the given range. If not, an <code>IllegalArgumentException</code> is thrown with
     * a standardized message text using the supplied value name.
     * <p>This utility method is used to check arguments passed into methods:
     * <pre>
     * public void writeDataAtRegion(int x, inty, int w, int h, byte[] data) {
     *     Guardian.assertWithinRange("w", w, 0, data.length);
     *     ...
     * }
     * </pre>
     *
     * @param exprText  the test expression as text
     * @param exprValue the expression result
     * @param rangeMin  the range lower limit
     * @param rangeMax  the range upper limit
     * @throws IllegalArgumentException if the <code>exprValue</code> is less than <code>rangeMin</code> or greater than <code>rangeMax</code>
     */
    public static void assertWithinRange(String exprText, double exprValue, double rangeMin, double rangeMax) {
        if (exprValue < rangeMin || exprValue > rangeMax) {
            StringBuffer sb = createExprStringBuffer(exprText)
                    .append(" is [")
                    .append(exprValue)
                    .append("]  but should be in the range [")
                    .append(rangeMin)
                    .append("] to [")
                    .append(rangeMax)
                    .append("]");
            throw new IllegalArgumentException(sb.toString());
        }
    }

    private static void assertNonEmptyString(String exprText, int length) {
        if (length == 0) {
            final StringBuffer sb = createExprStringBuffer(exprText).append(" is an empty string");
            throw new IllegalArgumentException(sb.toString());
        }
    }

    private static void assertNonEmptyArray(String exprText, int length) {
        if (length == 0) {
            final StringBuffer sb = createExprStringBuffer(exprText).append(" is an empty array");
            throw new IllegalArgumentException(sb.toString());
        }
    }

    private static StringBuffer createExprStringBuffer(String exprText) {
        return new StringBuffer(32).append("[").append(exprText).append("]");
    }
}
