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
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.internal.CheckBoxEditor;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import junit.framework.TestCase;

public class CheckBoxEditorTest extends TestCase {

    public void testIsApplicable() throws Exception {
        CheckBoxEditor checkBoxEditor = new CheckBoxEditor();
        
        PropertyDescriptor booleanDescriptor = new PropertyDescriptor("test", Boolean.TYPE);
        assertTrue(checkBoxEditor.isValidFor(booleanDescriptor));
        
        PropertyDescriptor doubleDescriptor = new PropertyDescriptor("test", Double.TYPE);
        assertFalse(checkBoxEditor.isValidFor(doubleDescriptor));
    }
    
    public void testCreateEditorComponent() throws Exception {
        CheckBoxEditor checkBoxEditor = new CheckBoxEditor();
        
        PropertyContainer propertyContainer = PropertyContainer.createValueBacked(V.class);
        BindingContext bindingContext = new BindingContext(propertyContainer);
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor("b");
        assertSame(Boolean.TYPE, propertyDescriptor.getType());
        
        assertTrue(checkBoxEditor.isValidFor(propertyDescriptor));
        JComponent editorComponent = checkBoxEditor.createEditorComponent(propertyDescriptor, bindingContext);
        assertNotNull(editorComponent);
        assertSame(JCheckBox.class, editorComponent.getClass());
        JComponent[] components = bindingContext.getBinding("b").getComponents();
        assertEquals(1, components.length);
        assertSame(JCheckBox.class, components[0].getClass());
    }
    
    private static class V {
        boolean b;
    }
}
