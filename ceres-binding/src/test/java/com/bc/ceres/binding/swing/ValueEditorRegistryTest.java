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
package com.bc.ceres.binding.swing;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.swing.internal.CheckBoxEditor;
import com.bc.ceres.binding.swing.internal.NumericEditor;
import com.bc.ceres.binding.swing.internal.TextFieldEditor;
import junit.framework.TestCase;


public class ValueEditorRegistryTest extends TestCase {

    private ValueEditorRegistry editorRegistry;


    @Override
    protected void setUp() throws Exception {
        editorRegistry = ValueEditorRegistry.getInstance();
    }

    public void testGetValueEditor_notExistant() throws Exception {
        ValueEditor valueEditor = editorRegistry.getValueEditor("foo");
        assertNull(valueEditor);
    }

    public void testGetValueEditor_TextField() throws Exception {
        ValueEditor valueEditor = editorRegistry.getValueEditor(TextFieldEditor.class.getName());
        assertNotNull(valueEditor);
        assertSame(TextFieldEditor.class, valueEditor.getClass());
    }

    public void testFindValueEditor_Null() throws Exception {
        try {
            editorRegistry.findValueEditor(null);
            fail();
        } catch (RuntimeException ignored) {
        }
    }

    public void testFindValueEditor_UnknownEditor() throws Exception {
        ValueDescriptor descriptor = new ValueDescriptor("test", TestCase.class);
        ValueEditor valueEditor = editorRegistry.findValueEditor(descriptor);
        assertNotNull(valueEditor);
        assertSame(TextFieldEditor.class, valueEditor.getClass());
    }

    public void testFindValueEditor_SpecifiedEditor() throws Exception {
        ValueDescriptor descriptor = new ValueDescriptor("test", Double.class);
        CheckBoxEditor checkBoxEditor = new CheckBoxEditor();
        descriptor.setProperty("valueEditor", checkBoxEditor);
        ValueEditor valueEditor = editorRegistry.findValueEditor(descriptor);
        assertNotNull(valueEditor);
        assertSame(checkBoxEditor, valueEditor);
    }

    public void testFindValueEditor_MatchingEditor() throws Exception {
        ValueDescriptor descriptor = new ValueDescriptor("test", Double.class);
        ValueEditor valueEditor = editorRegistry.findValueEditor(descriptor);
        assertNotNull(valueEditor);
        assertSame(NumericEditor.class, valueEditor.getClass());
    }
}
