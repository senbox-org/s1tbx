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
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;
import com.bc.ceres.swing.binding.PropertyEditor;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Font;

/**
 * An editor for numeric values with an unlimited range.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class NumericEditor extends PropertyEditor {

    @Override
    public boolean isValidFor(PropertyDescriptor propertyDescriptor) {
        ValueSet valueSet = propertyDescriptor.getValueSet();
        Class<?> type = propertyDescriptor.getType();
        return valueSet == null && isNumericType(type);
    }

    @Override
    public JComponent createEditorComponent(PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
        JTextField textField = new JTextField();
        textField.setHorizontalAlignment(SwingConstants.RIGHT);
        int fontSize = textField.getFont().getSize();
        textField.setFont(new Font("Courier", Font.PLAIN, fontSize));
        ComponentAdapter adapter = new TextComponentAdapter(textField);
        bindingContext.bind(propertyDescriptor.getName(), adapter);
        return textField;
    }

    protected static boolean isNumericType(Class<?> type) {
        return Byte.TYPE.equals(type)
               || Short.TYPE.equals(type)
               || Integer.TYPE.equals(type)
               || Long.TYPE.equals(type)
               || Float.TYPE.equals(type)
               || Double.TYPE.equals(type)
               || Number.class.isAssignableFrom(type);
    }
}
