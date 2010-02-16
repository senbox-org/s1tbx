package com.bc.ceres.core;

import junit.framework.TestCase;

public class  AssertTest extends TestCase {
    public void testState() {
        try {
            Assert.state(true);
        } catch (Exception e) {
            fail("Exception not expected");
        }

        try {
            Assert.state(false);
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            assertEquals("Assert.state(false) called", e.getMessage());
        }

        try {
            Assert.state(false, "Uups!");
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            assertEquals("Uups!", e.getMessage());
        }
    }

    public void testNotNull() {
        try {
            Assert.notNull("");
        } catch (Exception e) {
            fail("Exception not expected");
        }

        try {
            Assert.notNull(null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            assertEquals("Assert.notNull(null) called", e.getMessage());
        }

        try {
            Assert.notNull(null, "Uups!");
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            assertEquals("Uups!", e.getMessage());
        }
    }

    public void testLegalArgument() {
        try {
            Assert.argument(true);
        } catch (Exception e) {
            fail("Exception not expected");
        }

        try {
            Assert.argument(false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertEquals("Assert.argument(false) called", e.getMessage());
        }

        try {
            Assert.argument(false, "Uups!");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertEquals("Uups!", e.getMessage());
        }
    }
}
