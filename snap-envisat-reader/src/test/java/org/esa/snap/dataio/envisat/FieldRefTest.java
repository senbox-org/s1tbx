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

package org.esa.snap.dataio.envisat;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FieldRefTest extends TestCase {

    public FieldRefTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(FieldRefTest.class);
    }

    public void testParseAndFormat() {

        String fieldRefStr = null;
        FieldRef fieldRef = null;

        fieldRefStr = "Tie_point_GADS.5";
        try {
            fieldRef = FieldRef.parse(fieldRefStr);
            assertEquals("Tie_point_GADS", fieldRef.getDatasetName());
            assertEquals(4, fieldRef.getFieldIndex());
            assertEquals(-1, fieldRef.getElemIndex());
        } catch (NumberFormatException e) {
            fail(e.getMessage());
        }
        assertEquals(fieldRefStr, fieldRef.format());

        fieldRefStr = "Scaling_factor_GADS.12.9";
        try {
            fieldRef = FieldRef.parse(fieldRefStr);
            assertEquals("Scaling_factor_GADS", fieldRef.getDatasetName());
            assertEquals(11, fieldRef.getFieldIndex());
            assertEquals(8, fieldRef.getElemIndex());
        } catch (NumberFormatException e) {
            fail(e.getMessage());
        }
        assertEquals(fieldRefStr, fieldRef.format());

        try {
            fieldRef = FieldRef.parse("Scaling_factor_GADS .  12 .  9  ");
            assertEquals("Scaling_factor_GADS", fieldRef.getDatasetName());
            assertEquals(11, fieldRef.getFieldIndex());
            assertEquals(8, fieldRef.getElemIndex());
            assertEquals(fieldRefStr, fieldRef.format());
        } catch (NumberFormatException e) {
            fail(e.getMessage());
        }
        assertEquals(fieldRefStr, fieldRef.format());

        fieldRefStr = "Scaling_factor.GADS.84.152";
        try {
            fieldRef = FieldRef.parse(fieldRefStr);
            assertEquals("Scaling_factor.GADS", fieldRef.getDatasetName());
            assertEquals(83, fieldRef.getFieldIndex());
            assertEquals(151, fieldRef.getElemIndex());
            assertEquals(fieldRefStr, fieldRef.format());
        } catch (NumberFormatException e) {
            fail(e.getMessage());
        }

        fieldRefStr = "Scaling_factor.GADS.84";
        try {
            fieldRef = FieldRef.parse(fieldRefStr);
            assertEquals("Scaling_factor.GADS", fieldRef.getDatasetName());
            assertEquals(83, fieldRef.getFieldIndex());
            assertEquals(-1, fieldRef.getElemIndex());
            assertEquals(fieldRefStr, fieldRef.format());
        } catch (NumberFormatException e) {
            fail(e.getMessage());
        }

        final String malformed[] = {".3.4", "x.3a", "x..", "x.0", "x.0.-1"};
        for (int i = 0; i < malformed.length; i++) {
            try {
                fieldRef = FieldRef.parse(malformed[i]);
                fail("NumberFormatException expected for '" + malformed[i] + "'");
            } catch (NumberFormatException e) {
            }
        }
    }
}
