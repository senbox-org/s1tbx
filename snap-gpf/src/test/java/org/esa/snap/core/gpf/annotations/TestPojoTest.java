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

package org.esa.snap.core.gpf.annotations;

import junit.framework.TestCase;

import java.lang.reflect.Field;

public class TestPojoTest extends TestCase {
    private Class<? extends TestPojo> testPojoClass;

    @Override
    public void setUp() {
        TestPojo testPojo = new TestPojo();
        testPojoClass = testPojo.getClass();
    }

    public void testTargetProductAnnotation() throws NoSuchFieldException {
        Field vapourField = testPojoClass.getDeclaredField("vapour");
        TargetProduct tpa = vapourField.getAnnotation(TargetProduct.class);
        assertNotNull(tpa);
    }

    public void testSourceProductAnnotation() throws NoSuchFieldException {
        Field brrField = testPojoClass.getDeclaredField("brr");
        SourceProduct spa = brrField.getAnnotation(SourceProduct.class);
        assertNotNull(spa);
        assertEquals(true, spa.optional());
        assertEquals("MERIS_BRR", spa.type());
        assertEquals(2, spa.bands().length);
        assertEquals("radiance_2", spa.bands()[0]);
        assertEquals("radiance_5", spa.bands()[1]);
    }

    public void testParameterAnnotation() throws NoSuchFieldException {
        Field percentage = testPojoClass.getDeclaredField("percentage");
        Parameter pa = percentage.getAnnotation(Parameter.class);
        assertNotNull(pa);
        assertEquals("(0, 100]", pa.interval());
    }

    public void testTargetPropertyAnnotation() throws NoSuchFieldException {
        Field propertyField = testPojoClass.getDeclaredField("property");
        TargetProperty tpa = propertyField.getAnnotation(TargetProperty.class);
        assertNotNull(tpa);
        assertEquals("a test property", tpa.description());
        assertEquals("bert", tpa.alias());
    }
}
