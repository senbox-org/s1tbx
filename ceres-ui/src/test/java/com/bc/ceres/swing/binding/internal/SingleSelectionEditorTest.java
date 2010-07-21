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
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.internal.SingleSelectionEditor;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import junit.framework.TestCase;

public class SingleSelectionEditorTest extends TestCase {

    public void testIsApplicable() throws Exception {
        SingleSelectionEditor singleSelEditor = new SingleSelectionEditor();
        
        ValueSet valueSet = new ValueSet(new Double[] {Double.valueOf(42), Double.valueOf(84)});
        PropertyDescriptor doubleArrayDescriptor = new PropertyDescriptor("test", Double.class);
        doubleArrayDescriptor.setValueSet(valueSet);
        assertTrue(singleSelEditor.isValidFor(doubleArrayDescriptor));
        
        doubleArrayDescriptor = new PropertyDescriptor("test", Double[].class);
        doubleArrayDescriptor.setValueSet(valueSet);
        assertFalse(singleSelEditor.isValidFor(doubleArrayDescriptor));
        
        doubleArrayDescriptor = new PropertyDescriptor("test", Double.class);
        assertFalse(singleSelEditor.isValidFor(doubleArrayDescriptor));
    }
    
    public void testCreateEditorComponent() throws Exception {
        SingleSelectionEditor singleSelEditor = new SingleSelectionEditor();
        
        PropertyContainer propertyContainer = PropertyContainer.createValueBacked(V.class);
        BindingContext bindingContext = new BindingContext(propertyContainer);
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor("value");
        ValueSet valueSet = new ValueSet(new Double[] {Double.valueOf(42), Double.valueOf(84)});
        propertyDescriptor.setValueSet(valueSet);
        assertSame(Double.class, propertyDescriptor.getType());
        
        assertTrue(singleSelEditor.isValidFor(propertyDescriptor));
        JComponent editorComponent = singleSelEditor.createEditorComponent(propertyDescriptor, bindingContext);
        assertNotNull(editorComponent);
        assertSame(JComboBox.class, editorComponent.getClass());
        
        JComponent[] components = bindingContext.getBinding("value").getComponents();
        assertEquals(1, components.length);
        assertSame(JComboBox.class, components[0].getClass());
    }
    
    private static class V {
        Double value;
    }
}
