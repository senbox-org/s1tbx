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
import javax.swing.JSlider;

import junit.framework.TestCase;

public class RangeEditorTest extends TestCase {

    public void testIsApplicable() throws Exception {
        RangeEditor rangeEditor = new RangeEditor();
        
        ValueDescriptor doubleDescriptor = new ValueDescriptor("test", Double.TYPE);
        ValueRange valueRange = ValueRange.parseValueRange("[2.0,4.6]");
        doubleDescriptor.setValueRange(valueRange);
        assertFalse(rangeEditor.isValidFor(doubleDescriptor));
        
        doubleDescriptor = new ValueDescriptor("test", Double.class);
        assertFalse(rangeEditor.isValidFor(doubleDescriptor));
        
        doubleDescriptor = new ValueDescriptor("test", Double.TYPE);
        valueRange = ValueRange.parseValueRange("[2.0,*)");
        doubleDescriptor.setValueRange(valueRange);
        assertFalse(rangeEditor.isValidFor(doubleDescriptor));
        
        doubleDescriptor = new ValueDescriptor("test", Double.TYPE);
        valueRange = ValueRange.parseValueRange("[*,4.6)");
        doubleDescriptor.setValueRange(valueRange);
        assertFalse(rangeEditor.isValidFor(doubleDescriptor));
        
        ValueDescriptor booleanDescriptor = new ValueDescriptor("test", Boolean.TYPE);
        assertFalse(rangeEditor.isValidFor(booleanDescriptor));
    }
    
    public void testCreateEditorComponent() throws Exception {
        RangeEditor rangeEditor = new RangeEditor();
        
        ValueContainer valueContainer = ValueContainer.createValueBacked(V.class);
        BindingContext bindingContext = new BindingContext(valueContainer);
        ValueDescriptor valueDescriptor = valueContainer.getDescriptor("value");
        ValueRange valueRange = ValueRange.parseValueRange("[2.0,4.6]");
        valueDescriptor.setValueRange(valueRange);
        assertSame(Double.TYPE, valueDescriptor.getType());
        
        JComponent editorComponent = rangeEditor.createEditorComponent(valueDescriptor, bindingContext);
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
