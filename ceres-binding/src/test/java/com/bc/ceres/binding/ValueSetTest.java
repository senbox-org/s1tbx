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

import java.util.List;

import com.bc.ceres.binding.converters.IntegerConverter;

public class ValueSetTest extends TestCase {

    private List<Object> objects;

    public void testParseValueSet()  {
        ValueSet valueSet = null;
        try {
            valueSet = ValueSet.parseValueSet(new String[]{"1", "2", "3", "5", "8"}, new IntegerConverter());
        } catch (ConversionException e) {
            fail();
        }
        assertNotNull(valueSet.getItems());
        assertEquals(5, valueSet.getItems().length);
        assertEquals(1, valueSet.getItems()[0]);
        assertEquals(2, valueSet.getItems()[1]);
        assertEquals(3, valueSet.getItems()[2]);
        assertEquals(5, valueSet.getItems()[3]);
        assertEquals(8, valueSet.getItems()[4]);


        try {
            ValueSet.parseValueSet(new String[]{"1", "Foo", "3", "5", "8"}, new IntegerConverter());
            fail();
        } catch (ConversionException e) {
        }
    }

    public void testContains() {
        ValueSet valueSet = new ValueSet(new Integer[] {1, 2, 3, 5, 8});
        assertEquals(true, valueSet.contains(1));
        assertEquals(true, valueSet.contains(2));
        assertEquals(true, valueSet.contains(3));
        assertEquals(true, valueSet.contains(5));
        assertEquals(true, valueSet.contains(8));

        assertEquals(false, valueSet.contains(-1));
        assertEquals(false, valueSet.contains(0));
        assertEquals(false, valueSet.contains(4));
        assertEquals(false, valueSet.contains(9));
    }
}
