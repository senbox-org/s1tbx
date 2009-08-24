package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.swing.ComponentAdapter;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import java.awt.Component;
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
public class ComboBoxAdapter extends ComponentAdapter implements ActionListener, PropertyChangeListener {

    private final JComboBox comboBox;
    private final EditableChangeListener listener;
    private TextComponentAdapter textComponentAdapter;

    public ComboBoxAdapter(JComboBox comboBox) {
        this.comboBox = comboBox;
        listener = new EditableChangeListener();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        getBinding().setPropertyValue(comboBox.getSelectedItem());
    }

    @Override
    public void bindComponents() {
        updateComboBoxModel();
        getValueDescriptor().addPropertyChangeListener(this);
        comboBox.addActionListener(this);
        comboBox.addPropertyChangeListener("editable", listener);
        updateEditable();
    }

    @Override
    public void unbindComponents() {
        getValueDescriptor().removePropertyChangeListener(this);
        comboBox.removeActionListener(this);
        comboBox.removePropertyChangeListener("editable", listener);
    }

    @Override
    public void adjustComponents() {
        Object value = getBinding().getPropertyValue();
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

    private void updateEditable() {
        final Component editorComponent = comboBox.getEditor().getEditorComponent();
        if (comboBox.isEditable() && editorComponent instanceof JTextComponent) {
            textComponentAdapter = new TextComponentAdapter((JTextComponent) editorComponent);
            textComponentAdapter.setBinding(getBinding());
            textComponentAdapter.bindComponents();
        } else if (textComponentAdapter != null) {
            textComponentAdapter.unbindComponents();
            textComponentAdapter = null;
        }
    }

    private class EditableChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            updateEditable();
        }
    }
}
