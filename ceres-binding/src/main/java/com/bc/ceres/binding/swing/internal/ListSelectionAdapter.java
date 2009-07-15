package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.swing.ComponentAdapter;
import com.bc.ceres.binding.swing.BindingProblem;

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
 * @since BEAM 4.2
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

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == getValueDescriptor() && evt.getPropertyName().equals("valueSet")) {
            updateListModel();
        }
    }

    private ValueDescriptor getValueDescriptor() {
        return getBinding().getContext().getValueContainer().getDescriptor(getBinding().getPropertyName());
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

    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        if (getBinding().isAdjustingComponents()) {
            return;
        }
        final ValueModel model = getBinding().getContext().getValueContainer().getModel(getBinding().getPropertyName());
        Object[] selectedValues = list.getSelectedValues();
        Object array = Array.newInstance(model.getDescriptor().getType().getComponentType(), selectedValues.length);
        for (int i = 0; i < selectedValues.length; i++) {
            Array.set(array, i, selectedValues[i]);
        }
        try {
            model.setValue(array);
            // Now model is in sync with UI
            getBinding().setProblem(null);
        } catch (ValidationException e) {
            getBinding().setProblem(new BindingProblem(getBinding(), e));
            handleError(e);
        }
    }
}
