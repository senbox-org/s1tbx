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

package com.bc.ceres.binding;

import junit.framework.TestCase;

public class ValueRangeTest extends TestCase {
    public void testParseFailures()  {
        try {
            ValueRange.parseValueRange(null);
            fail();
        } catch (NullPointerException e) {
        } catch (IllegalArgumentException e) {
            fail();
        }
        try {
            ValueRange.parseValueRange("");
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            ValueRange.parseValueRange("10,20");
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    public void testParse() throws ConversionException {
        ValueRange valueRange = ValueRange.parseValueRange("[10,20)");
        assertEquals(10.0, valueRange.getMin(), 1e-10);
        assertEquals(true, valueRange.hasMin());
        assertEquals(true, valueRange.isMinIncluded());
        assertEquals(20.0, valueRange.getMax(), 1e-10);
        assertEquals(true, valueRange.hasMax());
        assertEquals(false, valueRange.isMaxIncluded());

        valueRange = ValueRange.parseValueRange("(-10,20]");
        assertEquals(-10.0, valueRange.getMin(), 1e-10);
        assertEquals(false, valueRange.isMinIncluded());
        assertEquals(20.0, valueRange.getMax(), 1e-10);
        assertEquals(true, valueRange.isMaxIncluded());
        assertEquals(true, valueRange.hasMin());
        assertEquals(true, valueRange.hasMax());

        valueRange = ValueRange.parseValueRange("(*, 20]");
        assertEquals(Double.NEGATIVE_INFINITY, valueRange.getMin(), 1e-10);
        assertEquals(false, valueRange.hasMin());
        assertEquals(false, valueRange.isMinIncluded());
        assertEquals(20.0, valueRange.getMax(), 1e-10);
        assertEquals(true, valueRange.hasMax());
        assertEquals(true, valueRange.isMaxIncluded());

        valueRange = ValueRange.parseValueRange("[-10,*]");
        assertEquals(-10.0, valueRange.getMin(), 1e-10);
        assertEquals(true, valueRange.isMinIncluded());
        assertEquals(Double.POSITIVE_INFINITY, valueRange.getMax(), 1e-10);
        assertEquals(false, valueRange.hasMax());
        assertEquals(true, valueRange.isMaxIncluded());
        assertEquals(true, valueRange.hasMin());
    }

    public void testContains() {

        ValueRange valueRange = new ValueRange(-1.5, 3.2, true, false);

        assertEquals(false, valueRange.contains(-1.6));
        assertEquals(true, valueRange.contains(-1.5));
        assertEquals(true, valueRange.contains(-1.4));

        assertEquals(true, valueRange.contains(3.1));
        assertEquals(false, valueRange.contains(3.2));
        assertEquals(false, valueRange.contains(3.3));

        valueRange = new ValueRange(-1.5, 3.2, false, true);

        assertEquals(false, valueRange.contains(-1.6));
        assertEquals(false, valueRange.contains(-1.5));
        assertEquals(true, valueRange.contains(-1.4));

        assertEquals(true, valueRange.contains(3.1));
        assertEquals(true, valueRange.contains(3.2));
        assertEquals(false, valueRange.contains(3.3));

        valueRange = new ValueRange(Double.NEGATIVE_INFINITY, 3.2, false, true);

        assertEquals(true, valueRange.contains(-1.6));
        assertEquals(true, valueRange.contains(-1.5));
        assertEquals(true, valueRange.contains(-1.4));

        assertEquals(true, valueRange.contains(3.1));
        assertEquals(true, valueRange.contains(3.2));
        assertEquals(false, valueRange.contains(3.3));

        valueRange = new ValueRange(-1.5, Double.POSITIVE_INFINITY, false, true);

        assertEquals(false, valueRange.contains(-1.6));
        assertEquals(false, valueRange.contains(-1.5));
        assertEquals(true, valueRange.contains(-1.4));

        assertEquals(true, valueRange.contains(3.1));
        assertEquals(true, valueRange.contains(3.2));
        assertEquals(true, valueRange.contains(3.3));
    }

}
