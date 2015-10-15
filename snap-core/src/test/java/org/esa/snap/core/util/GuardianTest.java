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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class GuardianTest extends TestCase {

    public GuardianTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(GuardianTest.class);
    }

    public void testAssertNotNull() {

        try {
            Object test1 = "Bibo!";
            Guardian.assertNotNull("test1", test1);
        } catch (IllegalArgumentException e) {
            fail("unexpected IllegalArgumentException");
        }

        try {
            Object test2 = null;
            Guardian.assertNotNull("test2", test2);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            testException(e, "[test2] is null");
        }

        try {
            Object test3 = "Gonzo!";
            Guardian.assertNotNull(null, test3);
        } catch (IllegalArgumentException e) {
            fail("unexpected IllegalArgumentException");
        }

        try {
            Object test4 = null;
            Guardian.assertNotNull(null, test4);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    public void testAssertGreaterThan() {
        try {
            Guardian.assertGreaterThan("x", -1, 0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            testException(e, "[x] is less than or equal to [0]");
        }
        try {
            Guardian.assertGreaterThan("x", 0, 0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            testException(e, "[x] is less than or equal to [0]");
        }
        try {
            Guardian.assertGreaterThan("x", 1, 0);
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException not Expected");
        }
    }

    public void testAssertNotNullOrEmpty() {
        try {
            Guardian.assertNotNullOrEmpty("x", "x");
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException not expected");
        }

        try {
            Guardian.assertNotNullOrEmpty("x", (String) null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            testException(e, "[x] is null");
        }

        try {
            Guardian.assertNotNullOrEmpty("s", "");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            testException(e, "[s] is an empty string");
        }
    }

    public void testAssertEquals_int() {
        int expected;
        int actual;
        try {
            expected = 3;
            actual = 3;
            Guardian.assertEquals("message", actual, expected);
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException not actual");
        }

        try {
            actual = 4;
            expected = 3;
            Guardian.assertEquals("actual", actual, expected);
            fail("IllegalArgumentException actual");
        } catch (IllegalArgumentException e) {
            testException(e, "[actual] is [4] but should be equal to [3]");
        }

        try {
            actual = 3;
            expected = 4;
            Guardian.assertEquals("actual", actual, expected);
            fail("IllegalArgumentException actual");
        } catch (IllegalArgumentException e) {
            testException(e, "[actual] is [3] but should be equal to [4]");
        }
    }

    public void testAssertWithinRange() {
        int value;
        int rangeMin;
        int rangeMax;

        // value is inside
        try {
            value = 24;
            rangeMin = 11;
            rangeMax = 321;
            Guardian.assertWithinRange("message", value, rangeMin, rangeMax);
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException not expected");
        }

        // value is on the upper limmit
        try {
            value = 427;
            rangeMin = 11;
            rangeMax = 427;
            Guardian.assertWithinRange("message", value, rangeMin, rangeMax);
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException not expected");
        }

        // value is on the lower limit
        try {
            value = 8;
            rangeMin = 8;
            rangeMax = 321;
            Guardian.assertWithinRange("message", value, rangeMin, rangeMax);
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException not expected");
        }

        // value is outside the upper limit
        try {
            value = 12537;
            rangeMin = 11;
            rangeMax = 12536;
            Guardian.assertWithinRange("value", value, rangeMin, rangeMax);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            testException(e, "[value] is [12537]  but should be in the range [11] to [12536]");
        }

        // value is outside the lower limit
        try {
            value = 122;
            rangeMin = 123;
            rangeMax = 321;
            Guardian.assertWithinRange("value", value, rangeMin, rangeMax);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            testException(e, "[value] is [122]  but should be in the range [123] to [321]");
        }
    }

    private void testException(IllegalArgumentException e, String expectedMsg) {
        assertNotNull(e.getMessage());
        assertEquals(expectedMsg, e.getMessage());
    }

}

