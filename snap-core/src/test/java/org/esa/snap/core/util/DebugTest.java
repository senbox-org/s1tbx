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

public class DebugTest extends TestCase {

    private boolean _oldDebugState;

    public DebugTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        if (isDebugClassTestable()) {
            return new TestSuite(DebugTest.class);
        } else {
            return new TestCase(DebugTest.class.getName()) {

                @Override
                public void runTest() {
                    System.out.println(DebugTest.class + ": test will not be performed: Debug.DEBUG == false");
                }
            };
        }
    }

    private static boolean isDebugClassTestable() {
        boolean oldDebugState = Debug.isEnabled();
        Debug.setEnabled(true);
        boolean newDebugState = Debug.isEnabled();
        Debug.setEnabled(oldDebugState);
        return newDebugState;
    }

    @Override
    protected void setUp() {
        _oldDebugState = Debug.isEnabled();
    }

    @Override
    protected void tearDown() {
        Debug.setEnabled(_oldDebugState);
    }

    public void testAssertWidthoutMessage() {

        Debug.setEnabled(false);
        try {
            Debug.assertTrue(true);
        } catch (AssertionFailure e) {
            fail("no AssertionFailure expected");
        }

        try {
            Debug.assertTrue(false);
        } catch (AssertionFailure e) {
            fail("no AssertionFailure expected");
        }

        Debug.setEnabled(true);
        java.io.StringWriter sw = new java.io.StringWriter();
        Debug.setWriter(sw);
        try {
            Debug.assertTrue(true);
        } catch (AssertionFailure e) {
            fail("no AssertionFailure expected");
        }

        try {
            Debug.assertTrue(false);
            fail("AssertionFailure expected");
        } catch (AssertionFailure e) {
        }

        String assertionFailureMsg = sw.getBuffer().toString();
        String expectedContent = Debug.class.getName();
        assertEquals(true, assertionFailureMsg.indexOf(expectedContent) >= 0);
    }

    public void testAssertWidtMessage() {
        Debug.setEnabled(false);

        try {
            Debug.assertTrue(true, "ErrorMessage");
        } catch (AssertionFailure e) {
            fail("no AssertionFailure expected");
        }

        try {
            Debug.assertTrue(false, "ErrorMessage");
        } catch (AssertionFailure e) {
            fail("no AssertionFailure expected");
        }

        Debug.setEnabled(true);
        java.io.StringWriter sw = new java.io.StringWriter();
        Debug.setWriter(sw);
        try {
            Debug.assertTrue(true, "ErrorMessage");
        } catch (AssertionFailure e) {
            fail("no AssertionFailure expected");
        }

        try {
            Debug.assertTrue(false, "ErrorMessage");
            fail("AssertionFailure expected");
        } catch (AssertionFailure e) {
            assertEquals("ErrorMessage", e.getMessage());
        }

        String assertionFailureMsg = sw.getBuffer().toString();
        String expectedContent = Debug.class.getName();
        assertEquals(true, assertionFailureMsg.indexOf(expectedContent) >= 0);
    }

    public void testAssertNotNull() {

        Debug.setEnabled(false);
        try {
            Debug.assertNotNull(new Object());
        } catch (AssertionFailure e) {
            fail("no AssertionFailure expected");
        }

        try {
            Debug.assertNotNull(null);
        } catch (AssertionFailure e) {
            fail("no AssertionFailure expected");
        }

        Debug.setEnabled(true);
        Debug.setWriter(new java.io.StringWriter());
        try {
            Debug.assertNotNull(new Object());
        } catch (AssertionFailure e) {
            fail("no AssertionFailure expected");
        }

        try {
            Debug.assertNotNull(null);
            fail("AssertionFailure expected");
        } catch (AssertionFailure e) {
        }
    }

    public void testAssertNotNullOrEmpty() {

        Debug.setEnabled(false);
        try {
            Debug.assertNotNullOrEmpty(null);
        } catch (AssertionFailure e) {
            fail("no AssertionFailure expected");
        }

        try {
            Debug.assertNotNullOrEmpty("");
        } catch (AssertionFailure e) {
            fail("no AssertionFailure expected");
        }

        Debug.setEnabled(true);
        Debug.setWriter(new java.io.StringWriter());
        try {
            Debug.assertNotNullOrEmpty("a");
        } catch (AssertionFailure e) {
            fail("no AssertionFailure expected");
        }

        try {
            Debug.assertNotNullOrEmpty(null);
            fail("AssertionFailure expected");
        } catch (AssertionFailure e) {
        }

        try {
            Debug.assertNotNullOrEmpty("");
            fail("AssertionFailure expected");
        } catch (AssertionFailure e) {
        }
    }
}
