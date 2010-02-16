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
