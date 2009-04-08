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

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.ComponentAdapter;
import com.bc.ceres.binding.swing.ValueEditor;

import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JTextField;

/**
 * An editor for numeric values with an unlimited range.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class NumericEditor extends ValueEditor {

    @Override
    public boolean isValidFor(ValueDescriptor valueDescriptor) {
        Class<?> type = valueDescriptor.getType();
        if (isNumericType(type)) {
            return true;
        }
        return false;
    }
    
    @Override
    public JComponent createEditorComponent(ValueDescriptor valueDescriptor, BindingContext bindingContext) {
        JTextField textField = new JTextField();
        textField.setHorizontalAlignment(JTextField.RIGHT);
        int fontSize = textField.getFont().getSize();
        textField.setFont(new Font("Courier", Font.PLAIN, fontSize));
        ComponentAdapter adapter = new TextFieldAdapter(textField);
        bindingContext.bind(valueDescriptor.getName(), adapter);
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
