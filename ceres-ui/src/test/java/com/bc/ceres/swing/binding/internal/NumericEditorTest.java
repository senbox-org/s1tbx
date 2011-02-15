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
package com.bc.ceres.swing.binding.internal;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.BindingContext;
import junit.framework.TestCase;

import javax.swing.JComponent;
import javax.swing.JTextField;

public class NumericEditorTest extends TestCase {

    public void testIsApplicable() throws Exception {
        NumericEditor numericEditor = new NumericEditor();

        PropertyDescriptor doubleDescriptor = new PropertyDescriptor("test", Double.class);
        assertTrue(numericEditor.isValidFor(doubleDescriptor));

        doubleDescriptor = new PropertyDescriptor("test", Double.TYPE);
        assertTrue(numericEditor.isValidFor(doubleDescriptor));

        doubleDescriptor = new PropertyDescriptor("test", Double.TYPE);
        ValueRange valueRange = ValueRange.parseValueRange("[2.0,*)");
        doubleDescriptor.setValueRange(valueRange);
        assertTrue(numericEditor.isValidFor(doubleDescriptor));

        doubleDescriptor = new PropertyDescriptor("test", Double.TYPE);
        valueRange = ValueRange.parseValueRange("[*,4.6)");
        doubleDescriptor.setValueRange(valueRange);
        assertTrue(numericEditor.isValidFor(doubleDescriptor));

        doubleDescriptor = new PropertyDescriptor("test", Double.TYPE);
        valueRange = ValueRange.parseValueRange("[2.0,4.6]");
        doubleDescriptor.setValueRange(valueRange);
        assertTrue(numericEditor.isValidFor(doubleDescriptor));

        final PropertyDescriptor valueSetDescriptor = new PropertyDescriptor("test", Integer.TYPE);
        valueSetDescriptor.setValueSet(new ValueSet(new Integer[]{50, 150}));
        assertFalse(numericEditor.isValidFor(valueSetDescriptor));

        PropertyDescriptor booleanDescriptor = new PropertyDescriptor("test", Boolean.TYPE);
        assertFalse(numericEditor.isValidFor(booleanDescriptor));
    }

    public void testCreateEditorComponent() throws Exception {
        NumericEditor numericEditor = new NumericEditor();

        PropertyContainer propertyContainer = PropertyContainer.createValueBacked(V.class);
        BindingContext bindingContext = new BindingContext(propertyContainer);
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor("value");
        assertSame(Double.TYPE, propertyDescriptor.getType());

        assertTrue(numericEditor.isValidFor(propertyDescriptor));
        JComponent editorComponent = numericEditor.createEditorComponent(propertyDescriptor, bindingContext);
        assertNotNull(editorComponent);
        assertSame(JTextField.class, editorComponent.getClass());

        JComponent[] components = bindingContext.getBinding("value").getComponents();
        assertEquals(1, components.length);
        assertSame(JTextField.class, components[0].getClass());
    }

    private static class V {

        double value;
    }
}
