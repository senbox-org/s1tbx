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

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;
import com.bc.ceres.swing.binding.PropertyEditor;

import javax.swing.JComponent;
import javax.swing.JTextField;

/**
 * A value editor that uses a text field.
 * 
 * This editor does not qualify itself 
 * for any edit because it is the default editor.
 * Otherwise it would take precedence over other editors.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since Ceres 0.9
 */
public class TextFieldEditor extends PropertyEditor {

    @Override
    public JComponent createEditorComponent(PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
        JTextField textField = new JTextField();
        ComponentAdapter adapter = new TextComponentAdapter(textField);
        bindingContext.bind(propertyDescriptor.getName(), adapter);
        return textField;
    }
}
