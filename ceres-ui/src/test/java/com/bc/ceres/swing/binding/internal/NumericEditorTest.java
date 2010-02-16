/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
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
package com.bc.ceres.swing.binding.internal;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.internal.NumericEditor;

import javax.swing.JComponent;
import javax.swing.JTextField;

import junit.framework.TestCase;

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
