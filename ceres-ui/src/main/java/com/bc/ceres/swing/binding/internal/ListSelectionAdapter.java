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

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.ComponentAdapter;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Array;

/**
 * A binding for a selection within a {@link javax.swing.JList} component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since Ceres 0.6
 */
public class ListSelectionAdapter extends ComponentAdapter implements ListSelectionListener, PropertyChangeListener {

    private final JList list;

    public ListSelectionAdapter(JList list) {
        this.list = list;
    }

    @Override
    public JComponent[] getComponents() {
        return new JComponent[]{list};
    }

    @Override
    public void bindComponents() {
        updateListModel();
        getValueDescriptor().addAttributeChangeListener(this);
        list.addListSelectionListener(this);
    }

    @Override
    public void unbindComponents() {
        getValueDescriptor().removeAttributeChangeListener(this);
        list.removeListSelectionListener(this);
    }

    @Override
    public void adjustComponents() {
        Object array = getBinding().getPropertyValue();
        if (array != null) {
            ListModel model = list.getModel();
            int size = model.getSize();
            int[] temp = new int[size];
            int numSelectedElements = 0;
            int arrayLength = Array.getLength(array);
            for (int i = 0; i < size; i++) {
                Object element = model.getElementAt(i);
                for (int j = 0; j < arrayLength; j++) {
                    if (element.equals(Array.get(array, j))) {
                        temp[numSelectedElements++] = i;
                    }
                }
            }
            int[] indices = new int[numSelectedElements];
            System.arraycopy(temp, 0, indices, 0, numSelectedElements);
            list.setSelectedIndices(indices);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == getValueDescriptor() && evt.getPropertyName().equals("valueSet")) {
            updateListModel();
        }
    }

    private PropertyDescriptor getValueDescriptor() {
        return getBinding().getContext().getPropertySet().getDescriptor(getBinding().getPropertyName());
    }

    private void updateListModel() {
        ValueSet valueSet = getValueDescriptor().getValueSet();
        if (valueSet != null) {
            Object oldElems = getBinding().getPropertyValue();
            list.setListData(valueSet.getItems());
            if (oldElems != null) {    
                ListSelectionModel selectionModel = list.getSelectionModel();
                int arrayLength = Array.getLength(oldElems);
                ListModel listModel = list.getModel();
                for (int i = 0; i < listModel.getSize(); i++) {
                    Object element = listModel.getElementAt(i);
                    for (int j = 0; j < arrayLength; j++) {
                        if (element.equals((Array.get(oldElems, j)))) {
                            selectionModel.addSelectionInterval(i, i);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        if (getBinding().isAdjustingComponents()) {
            return;
        }
        final Property model = getBinding().getContext().getPropertySet().getProperty(getBinding().getPropertyName());
        Object[] selectedValues = list.getSelectedValues();
        Object array = Array.newInstance(model.getDescriptor().getType().getComponentType(), selectedValues.length);
        for (int i = 0; i < selectedValues.length; i++) {
            Array.set(array, i, selectedValues[i]);
        }
        try {
            model.setValue(array);
            // Now model is in sync with UI
            getBinding().clearProblem();
        } catch (ValidationException e) {
            getBinding().reportProblem(e);
        }
    }
}
