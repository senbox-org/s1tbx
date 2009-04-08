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
package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.swing.BindingContext;

import javax.swing.JComponent;
import javax.swing.JTextField;

import junit.framework.TestCase;

public class NumericEditorTest extends TestCase {

    public void testIsApplicable() throws Exception {
        NumericEditor numericEditor = new NumericEditor();
        
        ValueDescriptor doubleDescriptor = new ValueDescriptor("test", Double.class);
        assertTrue(numericEditor.isValidFor(doubleDescriptor));
        
        doubleDescriptor = new ValueDescriptor("test", Double.TYPE);
        assertTrue(numericEditor.isValidFor(doubleDescriptor));

        doubleDescriptor = new ValueDescriptor("test", Double.TYPE);
        ValueRange valueRange = ValueRange.parseValueRange("[2.0,*)");
        doubleDescriptor.setValueRange(valueRange);
        assertTrue(numericEditor.isValidFor(doubleDescriptor));
        
        doubleDescriptor = new ValueDescriptor("test", Double.TYPE);
        valueRange = ValueRange.parseValueRange("[*,4.6)");
        doubleDescriptor.setValueRange(valueRange);
        assertTrue(numericEditor.isValidFor(doubleDescriptor));
        
        doubleDescriptor = new ValueDescriptor("test", Double.TYPE);
        valueRange = ValueRange.parseValueRange("[2.0,4.6]");
        doubleDescriptor.setValueRange(valueRange);
        assertTrue(numericEditor.isValidFor(doubleDescriptor));
        
        ValueDescriptor booleanDescriptor = new ValueDescriptor("test", Boolean.TYPE);
        assertFalse(numericEditor.isValidFor(booleanDescriptor));
    }
    
    public void testCreateEditorComponent() throws Exception {
        NumericEditor numericEditor = new NumericEditor();
        
        ValueContainer valueContainer = ValueContainer.createValueBacked(V.class);
        BindingContext bindingContext = new BindingContext(valueContainer);
        ValueDescriptor valueDescriptor = valueContainer.getDescriptor("value");
        assertSame(Double.TYPE, valueDescriptor.getType());
        
        assertTrue(numericEditor.isValidFor(valueDescriptor));
        JComponent editorComponent = numericEditor.createEditorComponent(valueDescriptor, bindingContext);
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
