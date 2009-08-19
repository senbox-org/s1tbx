package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.swing.ComponentAdapter;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A binding for a {@link javax.swing.JComboBox} component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class ComboBoxAdapter extends ComponentAdapter implements ActionListener, ItemListener, PropertyChangeListener {

    final JComboBox comboBox;

    public ComboBoxAdapter(JComboBox comboBox) {
        this.comboBox = comboBox;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        getBinding().setPropertyValue(comboBox.getSelectedItem());
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        getBinding().setPropertyValue(comboBox.getSelectedItem());
    }

    @Override
    public void bindComponents() {
        updateComboBoxModel();
        getValueDescriptor().addPropertyChangeListener(this);
        comboBox.addActionListener(this);
        comboBox.addItemListener(this);
    }

    @Override
    public void unbindComponents() {
        getValueDescriptor().removePropertyChangeListener(this);
        comboBox.removeActionListener(this);
        comboBox.removeItemListener(this);
    }

    @Override
    public void adjustComponents() {
        Object value = getBinding().getPropertyValue();
        // Note: directly calling comboBox.setSelectedItem(value) will always fire ActionEvent
        comboBox.setSelectedItem(value);
    }

    @Override
    public JComponent[] getComponents() {
        return new JComponent[]{comboBox};
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == getValueDescriptor() && evt.getPropertyName().equals("valueSet")) {
            updateComboBoxModel();
        }
    }

    private ValueDescriptor getValueDescriptor() {
        return getBinding().getContext().getValueContainer().getDescriptor(getBinding().getPropertyName());
    }

    private void updateComboBoxModel() {
        ValueSet valueSet = getValueDescriptor().getValueSet();
        if (valueSet != null) {
            final Object oldValue = getBinding().getPropertyValue();
            final DefaultComboBoxModel model = new DefaultComboBoxModel(valueSet.getItems());
            if (!valueSet.contains(oldValue)) {
                model.addElement(oldValue);
            }
            comboBox.setModel(model);
            comboBox.setSelectedItem(oldValue);
        }
    }
}
