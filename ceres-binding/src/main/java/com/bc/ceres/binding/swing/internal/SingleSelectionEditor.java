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
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.ComponentAdapter;
import com.bc.ceres.binding.swing.ValueEditor;

import javax.swing.JComboBox;
import javax.swing.JComponent;

/**
 * A one-of-many selection editor using a Combobox.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class SingleSelectionEditor extends ValueEditor {

    @Override
    public boolean isValidFor(ValueDescriptor valueDescriptor) {
        ValueSet valueSet = valueDescriptor.getValueSet();
        Class<?> type = valueDescriptor.getType();
        return valueSet != null && !type.isArray();
    }
    
    @Override
    public JComponent createEditorComponent(ValueDescriptor valueDescriptor, BindingContext bindingContext) {
        JComboBox comboBox = new JComboBox();
        ComponentAdapter adapter = new ComboBoxAdapter(comboBox);
        bindingContext.bind(valueDescriptor.getName(), adapter);
        return comboBox;
    }
}
