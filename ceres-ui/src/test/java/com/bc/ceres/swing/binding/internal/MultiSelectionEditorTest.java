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
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.internal.MultiSelectionEditor;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;

import junit.framework.TestCase;

public class MultiSelectionEditorTest extends TestCase {

    public void testIsApplicable() throws Exception {
        MultiSelectionEditor multiSelEditor = new MultiSelectionEditor();
        
        ValueSet valueSet = new ValueSet(new Double[] {Double.valueOf(42), Double.valueOf(84)});
        PropertyDescriptor doubleArrayDescriptor = new PropertyDescriptor("test", Double[].class);
        doubleArrayDescriptor.setValueSet(valueSet);
        assertTrue(multiSelEditor.isValidFor(doubleArrayDescriptor));
        
        doubleArrayDescriptor = new PropertyDescriptor("test", Double.class);
        doubleArrayDescriptor.setValueSet(valueSet);
        assertFalse(multiSelEditor.isValidFor(doubleArrayDescriptor));
        
        doubleArrayDescriptor = new PropertyDescriptor("test", Double[].class);
        assertFalse(multiSelEditor.isValidFor(doubleArrayDescriptor));
        
        PropertyDescriptor doubleDescriptor = new PropertyDescriptor("test", Double.TYPE);
        assertFalse(multiSelEditor.isValidFor(doubleDescriptor));
    }
    
    public void testCreateEditorComponent() throws Exception {
        MultiSelectionEditor multiSelEditor = new MultiSelectionEditor();
        
        PropertyContainer propertyContainer = PropertyContainer.createValueBacked(V.class);
        BindingContext bindingContext = new BindingContext(propertyContainer);
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor("value");
        ValueSet valueSet = new ValueSet(new Double[] {Double.valueOf(42), Double.valueOf(84)});
        propertyDescriptor.setValueSet(valueSet);
        assertSame(Double[].class, propertyDescriptor.getType());
        
        assertTrue(multiSelEditor.isValidFor(propertyDescriptor));
        JComponent editorComponent = multiSelEditor.createEditorComponent(propertyDescriptor, bindingContext);
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
