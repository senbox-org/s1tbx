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
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.ComponentAdapter;
import com.bc.ceres.binding.swing.ValueEditor;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

/**
 * An editor for boolean values using a JCheckbox.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class CheckBoxEditor extends ValueEditor {

    @Override
    public boolean isValidFor(ValueDescriptor valueDescriptor) {
        Class<?> type = valueDescriptor.getType();
        return Boolean.TYPE.equals(type) || Boolean.class.isAssignableFrom(type);
    }
    
    @Override
    public JComponent[] createComponents(ValueDescriptor valueDescriptor, BindingContext bindingContext) {
        JCheckBox checkBox = createCheckBoxComponent(valueDescriptor, bindingContext);
        checkBox.setText(getDisplayName(valueDescriptor));
        return new JComponent[] {checkBox};
    }
    
    @Override
    public JComponent createEditorComponent(ValueDescriptor valueDescriptor, BindingContext bindingContext) {
        return createCheckBoxComponent(valueDescriptor, bindingContext);
    }
    
    private JCheckBox createCheckBoxComponent(ValueDescriptor valueDescriptor, BindingContext bindingContext) {
        JCheckBox checkBox = new JCheckBox();
        ComponentAdapter adapter = new AbstractButtonAdapter(checkBox);
        bindingContext.bind(valueDescriptor.getName(), adapter);
        return checkBox;
    }
}
