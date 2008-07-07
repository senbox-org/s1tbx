/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.gpf.annotations;


import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueAccessor;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.accessors.DefaultValueAccessor;
import com.bc.ceres.binding.converters.ArrayConverter;
import com.bc.ceres.binding.converters.DoubleConverter;
import com.bc.ceres.binding.validators.MultiValidator;

import junit.framework.TestCase;

public class ParameterDescriptorFactoryTest extends TestCase{
    
    private ValueContainer valueContainer;

    @Override
    public void setUp() {
        ParameterDescriptorFactory pdf = new ParameterDescriptorFactory();
        TestPojo testPojo = new TestPojo();
        valueContainer = ValueContainer.createObjectBacked(testPojo, pdf);
    }
    
    public void testPercentageField() throws Exception {
        final String PERCENTAGE = "percentage";
        ValueDescriptor valueDescriptor = valueContainer.getDescriptor(PERCENTAGE);
        assertEquals(PERCENTAGE, valueDescriptor.getName());
        assertNull(valueDescriptor.getAlias());
        assertEquals(0.0, valueDescriptor.getDefaultValue());
        assertSame(DoubleConverter.class, valueDescriptor.getConverter().getClass());
        assertEquals("", valueDescriptor.getDescription());
        assertEquals(false, valueDescriptor.getItemsInlined());
        assertEquals(PERCENTAGE, valueDescriptor.getDisplayName());
        assertEquals(null, valueDescriptor.getDomConverter());
        assertEquals(null, valueDescriptor.getFormat());
        assertEquals(null, valueDescriptor.getItemAlias());
        assertEquals("double", valueDescriptor.getType().getName());
        assertEquals("", valueDescriptor.getUnit());
        assertNull(valueDescriptor.getValidator());
        assertEquals("(0,100]", valueDescriptor.getValueRange().toString());
        assertEquals(0.0, valueDescriptor.getValueRange().getMin(), 0.000001);
        assertEquals(100.0, valueDescriptor.getValueRange().getMax(), 0.000001);
        assertEquals(null, valueDescriptor.getValueSet());
    }
    
    public void testThresholdField() throws Exception {
        final String FIELD_NAME = "threshold";
        ValueDescriptor valueDescriptor = valueContainer.getDescriptor(FIELD_NAME);
        assertEquals(FIELD_NAME, valueDescriptor.getName());
        assertNull(valueDescriptor.getAlias());
        assertEquals(0.0, valueDescriptor.getDefaultValue());
        assertSame(DoubleConverter.class, valueDescriptor.getConverter().getClass());
        assertEquals("", valueDescriptor.getDescription());
        assertEquals(false, valueDescriptor.getItemsInlined());
        assertEquals("a nice desciption", valueDescriptor.getDisplayName());
        assertEquals(null, valueDescriptor.getDomConverter());
        assertEquals(null, valueDescriptor.getFormat());
        assertEquals(null, valueDescriptor.getItemAlias());
        assertFalse(valueDescriptor.getType().isArray());
        assertEquals("", valueDescriptor.getUnit());
        assertNull(valueDescriptor.getValidator());
        assertEquals(null, valueDescriptor.getValueRange());
        assertNotNull(valueDescriptor.getValueSet());
        assertEquals(3, valueDescriptor.getValueSet().getItems().length);
        assertEquals(0.0, valueDescriptor.getValueSet().getItems()[0]);
        assertEquals(13.0, valueDescriptor.getValueSet().getItems()[1]);
        assertEquals(42.0, valueDescriptor.getValueSet().getItems()[2]);
        ValueAccessor valueAccessor = new DefaultValueAccessor();
        ValueModel valueModel = new ValueModel(valueDescriptor, valueAccessor);
        Validator validator = valueModel.getValidator();
        assertNotNull(validator);
        assertNotNull(valueModel);
        try {
            validator.validateValue(valueModel, 42.0);
        }catch (ValidationException e) {
            fail("validation failed: "+e.getMessage());
        }
    }
    
    public void testThresholdArrayField() throws Exception {
        final String FIELD_NAME = "thresholdArray";
        ValueDescriptor valueDescriptor = valueContainer.getDescriptor(FIELD_NAME);
        assertEquals(FIELD_NAME, valueDescriptor.getName());
        assertNull(valueDescriptor.getAlias());
        assertEquals(null, valueDescriptor.getDefaultValue());
        assertSame(ArrayConverter.class, valueDescriptor.getConverter().getClass());
        assertEquals("", valueDescriptor.getDescription());
        assertEquals(false, valueDescriptor.getItemsInlined());
        assertEquals(FIELD_NAME, valueDescriptor.getDisplayName());
        assertEquals(null, valueDescriptor.getDomConverter());
        assertEquals(null, valueDescriptor.getFormat());
        assertEquals(null, valueDescriptor.getItemAlias());
        Class<?> type = valueDescriptor.getType();
        assertTrue(type.isArray());
        assertEquals(Double.TYPE, type.getComponentType());
        assertEquals("", valueDescriptor.getUnit());
        assertNull(valueDescriptor.getValidator());
        assertEquals(null, valueDescriptor.getValueRange());
        assertNotNull(valueDescriptor.getValueSet());
        assertEquals(3, valueDescriptor.getValueSet().getItems().length);
        assertEquals(0.0, valueDescriptor.getValueSet().getItems()[0]);
        assertEquals(13.0, valueDescriptor.getValueSet().getItems()[1]);
        assertEquals(42.0, valueDescriptor.getValueSet().getItems()[2]);
        ValueAccessor valueAccessor = new DefaultValueAccessor();
        ValueModel valueModel = new ValueModel(valueDescriptor, valueAccessor);
        Validator validator = valueModel.getValidator();
        assertNotNull(validator);
        assertNotNull(valueModel);
        try {
            validator.validateValue(valueModel, new double[]{42.0});
        }catch (ValidationException e) {
            fail("validation failed: "+e.getMessage());
        }
    }

}
