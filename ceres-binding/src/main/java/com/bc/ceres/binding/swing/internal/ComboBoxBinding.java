package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;

import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

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
        setPropertyValue(comboBox.getSelectedItem());
    }

    @Override
    protected void adjustComponentImpl() {
        try {
            Object value = getPropertyValue();
            comboBox.setSelectedItem(value);
        } catch (Exception e) {
            handleError(e);
        }
    }

    @Override
    protected JComponent getPrimaryComponent() {
        return comboBox;
    }
}
