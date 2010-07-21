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
