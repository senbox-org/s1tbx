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
import com.bc.ceres.swing.binding.BindingContext;
import junit.framework.TestCase;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.io.File;

public class FileEditorTest extends TestCase {

    public void testIsApplicable() throws Exception {
        FileEditor fileEditor = new FileEditor();
        
        PropertyDescriptor fileDescriptor = new PropertyDescriptor("test", File.class);
        assertTrue(fileEditor.isValidFor(fileDescriptor));
        
        PropertyDescriptor doubleDescriptor = new PropertyDescriptor("test", Double.TYPE);
        assertFalse(fileEditor.isValidFor(doubleDescriptor));
    }
    
    public void testCreateEditorComponent() throws Exception {
        FileEditor fileEditor = new FileEditor();
        
        PropertyContainer propertyContainer = PropertyContainer.createValueBacked(V.class);
        BindingContext bindingContext = new BindingContext(propertyContainer);
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor("file");
        assertSame(File.class, propertyDescriptor.getType());
        
        assertTrue(fileEditor.isValidFor(propertyDescriptor));
        JComponent editorComponent = fileEditor.createEditorComponent(propertyDescriptor, bindingContext);
        assertNotNull(editorComponent);
        assertSame(JPanel.class, editorComponent.getClass());
        assertEquals(2, editorComponent.getComponentCount());
        
        JComponent[] components = bindingContext.getBinding("file").getComponents();
        assertEquals(1, components.length);
        assertSame(JTextField.class, components[0].getClass());
    }
    
    private static class V {
        File file;
    }
}
