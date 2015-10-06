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

package org.esa.snap.core.param;

import junit.framework.TestCase;

// @todo 1 se/se - write more tests

public class ParameterTest extends TestCase {

    private final static String _PARAMETER_NAME = "Parametername";

    public ParameterTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() {
    }

    @Override
    protected void tearDown() {
    }

    public void test_Constructors() {
        Parameter param;

        param = new Parameter(_PARAMETER_NAME);
        assertNotNull(param);
        assertEquals(_PARAMETER_NAME, param.getName());
        assertEquals(String.class, param.getValueType());
        assertNull(param.getValue());
        assertNotNull(param.getProperties());
        assertNull(param.getProperties().getValueType());

        Object value = new Integer(32);

        param = new Parameter(_PARAMETER_NAME, value);
        assertNotNull(param);
        assertEquals(_PARAMETER_NAME, param.getName());
        assertEquals(value.getClass(), param.getValueType());
        assertSame(value, param.getValue());
        assertNotNull(param.getProperties());
        assertEquals(value.getClass(), param.getProperties().getValueType());


        ParamProperties properties = new ParamProperties();
        properties.setDefaultValue(value);

        param = new Parameter(_PARAMETER_NAME, properties);
        assertNotNull(param);
        assertEquals(_PARAMETER_NAME, param.getName());
        assertEquals(value.getClass(), param.getValueType());
        assertSame(value, param.getValue());
        assertSame(properties, param.getProperties());
        assertEquals(value.getClass(), param.getProperties().getValueType());

        Object value2 = new Float(4.2);
        properties = new ParamProperties();
        properties.setDefaultValue(value2);

        param = new Parameter(_PARAMETER_NAME, value2, properties);
        assertNotNull(param);
        assertEquals(_PARAMETER_NAME, param.getName());
        assertEquals(value2.getClass(), param.getValueType());
        assertSame(value2, param.getValue());
        assertNotNull(param.getProperties());
        assertEquals(value2.getClass(), param.getProperties().getValueType());
    }
}
