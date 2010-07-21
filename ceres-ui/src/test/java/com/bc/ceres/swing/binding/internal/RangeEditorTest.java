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
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.internal.RangeEditor;

import javax.swing.JComponent;
import javax.swing.JSlider;

import junit.framework.TestCase;

public class RangeEditorTest extends TestCase {

    public void testIsApplicable() throws Exception {
        RangeEditor rangeEditor = new RangeEditor();
        
        PropertyDescriptor doubleDescriptor = new PropertyDescriptor("test", Double.TYPE);
        ValueRange valueRange = ValueRange.parseValueRange("[2.0,4.6]");
        doubleDescriptor.setValueRange(valueRange);
        assertFalse(rangeEditor.isValidFor(doubleDescriptor));
        
        doubleDescriptor = new PropertyDescriptor("test", Double.class);
        assertFalse(rangeEditor.isValidFor(doubleDescriptor));
        
        doubleDescriptor = new PropertyDescriptor("test", Double.TYPE);
        valueRange = ValueRange.parseValueRange("[2.0,*)");
        doubleDescriptor.setValueRange(valueRange);
        assertFalse(rangeEditor.isValidFor(doubleDescriptor));
        
        doubleDescriptor = new PropertyDescriptor("test", Double.TYPE);
        valueRange = ValueRange.parseValueRange("[*,4.6)");
        doubleDescriptor.setValueRange(valueRange);
        assertFalse(rangeEditor.isValidFor(doubleDescriptor));
        
        PropertyDescriptor booleanDescriptor = new PropertyDescriptor("test", Boolean.TYPE);
        assertFalse(rangeEditor.isValidFor(booleanDescriptor));
    }
    
    public void testCreateEditorComponent() throws Exception {
        RangeEditor rangeEditor = new RangeEditor();
        
        PropertyContainer propertyContainer = PropertyContainer.createValueBacked(V.class);
        BindingContext bindingContext = new BindingContext(propertyContainer);
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor("value");
        ValueRange valueRange = ValueRange.parseValueRange("[2.0,4.6]");
        propertyDescriptor.setValueRange(valueRange);
        assertSame(Double.TYPE, propertyDescriptor.getType());
        
        JComponent editorComponent = rangeEditor.createEditorComponent(propertyDescriptor, bindingContext);
        assertNotNull(editorComponent);
        assertSame(JSlider.class, editorComponent.getClass());
        
        JComponent[] components = bindingContext.getBinding("value").getComponents();
        assertEquals(1, components.length);
        assertSame(JSlider.class, components[0].getClass());
    }
    
    private static class V {
        double value;
    }
}
