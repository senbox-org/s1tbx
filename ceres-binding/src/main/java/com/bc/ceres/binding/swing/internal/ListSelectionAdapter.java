package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.swing.ComponentAdapter;
import com.bc.ceres.binding.swing.Binding;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListModel;
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
 * @since BEAM 4.2
 */
public class ListSelectionAdapter extends ComponentAdapter implements ListSelectionListener, PropertyChangeListener {

    private final JList list;

    public ListSelectionAdapter(JList list) {
        this.list = list;
    }

    @Override
    public JComponent getPrimaryComponent() {
        return list;
    }

    @Override
    public void bindComponents() {
        updateListModel();
        getValueDescriptor().addPropertyChangeListener(this);
        list.addListSelectionListener(this);
    }

    @Override
    public void unbindComponents() {
        getValueDescriptor().removePropertyChangeListener(this);
        list.removeListSelectionListener(this);
    }

    @Override
    public void adjustComponents() {
        Object array = getBinding().getValue();
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

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == getValueDescriptor() && evt.getPropertyName().equals("valueSet")) {
            updateListModel();
        }
    }

    private ValueDescriptor getValueDescriptor() {
        return getBinding().getContext().getValueContainer().getValueDescriptor(getBinding().getName());
    }

    private void updateListModel() {
        ValueSet valueSet = getValueDescriptor().getValueSet();
        if (valueSet != null) {
            list.setListData(valueSet.getItems());
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        if (getBinding().isAdjustingComponents()) {
            return;
        }
        final ValueModel model = getBinding().getContext().getValueContainer().getModel(getBinding().getName());
        Object[] selectedValues = list.getSelectedValues();
        Object array = Array.newInstance(model.getDescriptor().getType().getComponentType(), selectedValues.length);
        for (int i = 0; i < selectedValues.length; i++) {
            Array.set(array, i, selectedValues[i]);
        }
        try {
            model.setValue(array);
        } catch (ValidationException e1) {
            getBinding().handleError(e1);
        }
    }
}
