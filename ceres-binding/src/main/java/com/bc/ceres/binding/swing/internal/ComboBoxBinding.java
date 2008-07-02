package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A binding for a {@link javax.swing.JComboBox} component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class ComboBoxBinding extends Binding implements ActionListener, PropertyChangeListener {

    final JComboBox comboBox;

    public ComboBoxBinding(BindingContext context, String propertyName, JComboBox comboBox) {
        super(context, propertyName);
        this.comboBox = comboBox;

        getValueDescriptor().addPropertyChangeListener(this);
        updateComboBoxModel();

        comboBox.addActionListener(this);
    }

    public void actionPerformed(ActionEvent event) {
        setValue(comboBox.getSelectedItem());
    }

    @Override
    protected void doAdjustComponents() {
        Object value = getValue();
        comboBox.setSelectedItem(value);
    }

    @Override
    public JComponent getPrimaryComponent() {
        return comboBox;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == getValueDescriptor() && evt.getPropertyName().equals("valueSet")) {
            updateComboBoxModel();
        }
    }

    private ValueDescriptor getValueDescriptor() {
        return getValueContainer().getValueDescriptor(getName());
    }

    private void updateComboBoxModel() {
        ValueSet valueSet = getValueDescriptor().getValueSet();
        if (valueSet != null) {
            final Object oldValue = getValue();
            final DefaultComboBoxModel aModel = new DefaultComboBoxModel(valueSet.getItems());
            if (!valueSet.contains(oldValue)) {
                aModel.addElement(oldValue);
            }
            comboBox.setModel(aModel);
            comboBox.setSelectedItem(oldValue);
        }
    }
}
