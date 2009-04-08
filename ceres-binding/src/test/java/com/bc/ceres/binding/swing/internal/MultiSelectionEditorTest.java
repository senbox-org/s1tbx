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
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.swing.BindingContext;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;

import junit.framework.TestCase;

public class MultiSelectionEditorTest extends TestCase {

    public void testIsApplicable() throws Exception {
        MultiSelectionEditor multiSelEditor = new MultiSelectionEditor();
        
        ValueSet valueSet = new ValueSet(new Double[] {Double.valueOf(42), Double.valueOf(84)});
        ValueDescriptor doubleArrayDescriptor = new ValueDescriptor("test", Double[].class);
        doubleArrayDescriptor.setValueSet(valueSet);
        assertTrue(multiSelEditor.isValidFor(doubleArrayDescriptor));
        
        doubleArrayDescriptor = new ValueDescriptor("test", Double.class);
        doubleArrayDescriptor.setValueSet(valueSet);
        assertFalse(multiSelEditor.isValidFor(doubleArrayDescriptor));
        
        doubleArrayDescriptor = new ValueDescriptor("test", Double[].class);
        assertFalse(multiSelEditor.isValidFor(doubleArrayDescriptor));
        
        ValueDescriptor doubleDescriptor = new ValueDescriptor("test", Double.TYPE);
        assertFalse(multiSelEditor.isValidFor(doubleDescriptor));
    }
    
    public void testCreateEditorComponent() throws Exception {
        MultiSelectionEditor multiSelEditor = new MultiSelectionEditor();
        
        ValueContainer valueContainer = ValueContainer.createValueBacked(V.class);
        BindingContext bindingContext = new BindingContext(valueContainer);
        ValueDescriptor valueDescriptor = valueContainer.getDescriptor("value");
        ValueSet valueSet = new ValueSet(new Double[] {Double.valueOf(42), Double.valueOf(84)});
        valueDescriptor.setValueSet(valueSet);
        assertSame(Double[].class, valueDescriptor.getType());
        
        assertTrue(multiSelEditor.isValidFor(valueDescriptor));
        JComponent editorComponent = multiSelEditor.createEditorComponent(valueDescriptor, bindingContext);
        assertNotNull(editorComponent);
        assertSame(JScrollPane.class, editorComponent.getClass());
        
        JComponent[] components = bindingContext.getBinding("value").getComponents();
        assertEquals(1, components.length);
        assertSame(JList.class, components[0].getClass());
    }
    
    private static class V {
        Double[] value;
    }
}
