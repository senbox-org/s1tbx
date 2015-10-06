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


import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyAccessor;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import com.bc.ceres.binding.converters.ArrayConverter;
import com.bc.ceres.binding.converters.DoubleConverter;
import com.bc.ceres.binding.converters.EnumConverter;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class ParameterDescriptorFactoryTest {

    private static PropertyContainer propertyContainer;

    @BeforeClass
    public static void setUp() {
        ParameterDescriptorFactory pdf = new ParameterDescriptorFactory();
        TestPojo testPojo = new TestPojo();
        propertyContainer = PropertyContainer.createObjectBacked(testPojo, pdf);
    }

    @Test
    public void testPercentageField() throws Exception {
        final String PERCENTAGE = "percentage";
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor(PERCENTAGE);
        assertEquals(PERCENTAGE, propertyDescriptor.getName());
        assertNull(propertyDescriptor.getAlias());
        assertEquals(0.0, propertyDescriptor.getDefaultValue());
        assertSame(DoubleConverter.class, propertyDescriptor.getConverter().getClass());
        assertEquals(null, propertyDescriptor.getDescription());
        assertEquals("Percentage", propertyDescriptor.getDisplayName());
        assertEquals(null, propertyDescriptor.getDomConverter());
        assertEquals(null, propertyDescriptor.getFormat());
        assertEquals(null, propertyDescriptor.getItemAlias());
        assertEquals("double", propertyDescriptor.getType().getName());
        assertEquals(null, propertyDescriptor.getUnit());
        assertNull(propertyDescriptor.getValidator());
        assertEquals("(0,100]", propertyDescriptor.getValueRange().toString());
        assertEquals(0.0, propertyDescriptor.getValueRange().getMin(), 0.000001);
        assertEquals(100.0, propertyDescriptor.getValueRange().getMax(), 0.000001);
        assertEquals(null, propertyDescriptor.getValueSet());
    }

    @Test
    public void testThresholdField() throws Exception {
        final String FIELD_NAME = "threshold";
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor(FIELD_NAME);
        assertEquals(FIELD_NAME, propertyDescriptor.getName());
        assertNull(propertyDescriptor.getAlias());
        assertEquals(0.0, propertyDescriptor.getDefaultValue());
        assertSame(DoubleConverter.class, propertyDescriptor.getConverter().getClass());
        assertEquals(null, propertyDescriptor.getDescription());
        assertEquals("a nice desciption", propertyDescriptor.getDisplayName());
        assertEquals(null, propertyDescriptor.getDomConverter());
        assertEquals(null, propertyDescriptor.getFormat());
        assertEquals(null, propertyDescriptor.getItemAlias());
        assertFalse(propertyDescriptor.getType().isArray());
        assertEquals(null, propertyDescriptor.getUnit());
        assertNull(propertyDescriptor.getValidator());
        assertEquals(null, propertyDescriptor.getValueRange());
        assertNotNull(propertyDescriptor.getValueSet());
        assertEquals(3, propertyDescriptor.getValueSet().getItems().length);
        assertEquals(0.0, propertyDescriptor.getValueSet().getItems()[0]);
        assertEquals(13.0, propertyDescriptor.getValueSet().getItems()[1]);
        assertEquals(42.0, propertyDescriptor.getValueSet().getItems()[2]);
        PropertyAccessor propertyAccessor = new DefaultPropertyAccessor();
        Property property = new Property(propertyDescriptor, propertyAccessor);
        Validator validator = property.getValidator();
        assertNotNull(validator);
        assertNotNull(property);
        try {
            validator.validateValue(property, 42.0);
        } catch (ValidationException e) {
            fail("validation failed: " + e.getMessage());
        }
    }

    @Test
    public void testThresholdArrayField() throws Exception {
        final String fieldName = "thresholdArray";
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor(fieldName);
        assertEquals(fieldName, propertyDescriptor.getName());
        assertNull(propertyDescriptor.getAlias());
        assertEquals(null, propertyDescriptor.getDefaultValue());
        assertSame(ArrayConverter.class, propertyDescriptor.getConverter().getClass());
        assertEquals(null, propertyDescriptor.getDescription());
        assertEquals("Threshold array", propertyDescriptor.getDisplayName());
        assertEquals(null, propertyDescriptor.getDomConverter());
        assertEquals(null, propertyDescriptor.getFormat());
        assertEquals(null, propertyDescriptor.getItemAlias());
        Class<?> type = propertyDescriptor.getType();
        assertTrue(type.isArray());
        assertEquals(Double.TYPE, type.getComponentType());
        assertEquals(null, propertyDescriptor.getUnit());
        assertNull(propertyDescriptor.getValidator());
        assertEquals(null, propertyDescriptor.getValueRange());
        assertNotNull(propertyDescriptor.getValueSet());
        assertEquals(3, propertyDescriptor.getValueSet().getItems().length);
        assertEquals(0.0, propertyDescriptor.getValueSet().getItems()[0]);
        assertEquals(13.0, propertyDescriptor.getValueSet().getItems()[1]);
        assertEquals(42.0, propertyDescriptor.getValueSet().getItems()[2]);
        PropertyAccessor propertyAccessor = new DefaultPropertyAccessor();
        Property property = new Property(propertyDescriptor, propertyAccessor);
        Validator validator = property.getValidator();
        assertNotNull(validator);
        assertNotNull(property);
        try {
            validator.validateValue(property, new double[]{42.0});
        } catch (ValidationException e) {
            fail("validation failed: " + e.getMessage());
        }
    }

    @Test
    public void testThresholdFail() {
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor("threshold");
        PropertyAccessor propertyAccessor = new DefaultPropertyAccessor();
        Property property = new Property(propertyDescriptor, propertyAccessor);
        Validator validator = property.getValidator();
        try {
            validator.validateValue(property, 10.0);
            fail("validation should fail");
        } catch (ValidationException ignored) {
            // expected
        }
    }

    @Test
    public void testEnums() {
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor("aPValue");
        assertEquals("aPValue", propertyDescriptor.getName());
        assertSame(EnumConverter.class, propertyDescriptor.getConverter().getClass());

        ValueSet valueSet = propertyDescriptor.getValueSet();
        assertNotNull(valueSet);
        Object[] valueSetItems = valueSet.getItems();
        assertEquals(3, valueSetItems.length);
        assertEquals(TestPojo.TestEnum.P1, valueSetItems[0]);
        assertEquals(TestPojo.TestEnum.P2, valueSetItems[1]);
        assertEquals(TestPojo.TestEnum.P3, valueSetItems[2]);

        assertEquals(TestPojo.TestEnum.P2, propertyDescriptor.getDefaultValue());
    }
}
