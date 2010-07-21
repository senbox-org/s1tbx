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

import javax.swing.JCheckBox;
import javax.swing.JComponent;

/**
 * An editor for boolean values using a JCheckbox.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class CheckBoxEditor extends PropertyEditor {

    @Override
    public boolean isValidFor(PropertyDescriptor propertyDescriptor) {
        Class<?> type = propertyDescriptor.getType();
        return Boolean.TYPE.equals(type) || Boolean.class.isAssignableFrom(type);
    }
    
    @Override
    public JComponent[] createComponents(PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
        JCheckBox checkBox = createCheckBoxComponent(propertyDescriptor, bindingContext);
        checkBox.setText(propertyDescriptor.getDisplayName());
        return new JComponent[] {checkBox};
    }
    
    @Override
    public JComponent createEditorComponent(PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
        return createCheckBoxComponent(propertyDescriptor, bindingContext);
    }
    
    private static JCheckBox createCheckBoxComponent(PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
        JCheckBox checkBox = new JCheckBox();
        ComponentAdapter adapter = new AbstractButtonAdapter(checkBox);
        bindingContext.bind(propertyDescriptor.getName(), adapter);
        return checkBox;
    }
}
