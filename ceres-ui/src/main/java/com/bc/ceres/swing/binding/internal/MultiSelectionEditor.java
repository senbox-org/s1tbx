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
import javax.swing.JList;
import javax.swing.JScrollPane;

/**
 * A many-of-many selection editor using a JList.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class MultiSelectionEditor extends PropertyEditor {

    @Override
    public boolean isValidFor(PropertyDescriptor propertyDescriptor) {
        ValueSet valueSet = propertyDescriptor.getValueSet();
        Class<?> type = propertyDescriptor.getType();
        return valueSet != null && type.isArray();
    }
    
    @Override
    public JComponent createEditorComponent(PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
        final JList list = new JList();
        ComponentAdapter adapter = new ListSelectionAdapter(list);
        bindingContext.bind(propertyDescriptor.getName(), adapter);
        return new JScrollPane(list);
    }
}
