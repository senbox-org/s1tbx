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

/**
 * A binding for a {@link javax.swing.JComboBox} component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class ComboBoxBinding extends Binding implements ActionListener {

    final JComboBox comboBox;

    public ComboBoxBinding(BindingContext context, String propertyName, JComboBox comboBox) {
        super(context, propertyName);
        this.comboBox = comboBox;

        ValueDescriptor valueDescriptor = getValueContainer().getValueDescriptor(propertyName);
        ValueSet valueSet = valueDescriptor.getValueSet();
        if (valueSet != null) {
            comboBox.setModel(new DefaultComboBoxModel(valueSet.getItems()));
        }

        comboBox.addActionListener(this);
    }

    public void actionPerformed(ActionEvent event) {
        setValue(comboBox.getSelectedItem());
    }

    @Override
    protected void adjustComponentsImpl() {
        Object value = getValue();
        comboBox.setSelectedItem(value);
    }

    @Override
    public JComponent getPrimaryComponent() {
        return comboBox;
    }
}
