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
import com.bc.ceres.swing.binding.ComponentAdapter;

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
    private TextComponentAdapter textComponentAdapter;

    public ComboBoxAdapter(JComboBox comboBox) {
        this.comboBox = comboBox;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        adjustProperty();
    }

    @Override
    public void bindComponents() {
        adjustComboBoxModel();
        adjustTextComponent();
        getValueDescriptor().addAttributeChangeListener(this);
        comboBox.addPropertyChangeListener("editable", this);
        comboBox.addActionListener(this);
    }

    @Override
    public void unbindComponents() {
        comboBox.removeActionListener(this);
        comboBox.removePropertyChangeListener("editable", this);
        getValueDescriptor().removeAttributeChangeListener(this);
        unbindTextComponent();
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
        if (evt.getSource() == getValueDescriptor()
                && "valueSet".equals(evt.getPropertyName())) {
            adjustComboBoxModel();
        }
        if (evt.getSource() == comboBox
                && "editable".equals(evt.getPropertyName())) {
            adjustTextComponent();
        }
    }

    private PropertyDescriptor getValueDescriptor() {
        return getBinding().getContext().getPropertySet().getDescriptor(getBinding().getPropertyName());
    }

    private void adjustComboBoxModel() {
        ValueSet valueSet = getValueDescriptor().getValueSet();
        if (valueSet != null) {
            final Object oldValue = getBinding().getPropertyValue();
            comboBox.setModel(new DefaultComboBoxModel(valueSet.getItems()));
            comboBox.setSelectedItem(oldValue);
            adjustProperty();
        } else {
            // No else here, "valueSet == null" means the comboBox's model
            // is controlled by client.
        }
    }

    private void adjustProperty() {
        getBinding().setPropertyValue(comboBox.getSelectedItem());
    }

    private void adjustTextComponent() {
        final Component editorComponent = comboBox.getEditor().getEditorComponent();
        if (comboBox.isEditable() && editorComponent instanceof JTextComponent) {
            bindTextComponent((JTextComponent) editorComponent);
        } else {
            unbindTextComponent();
        }
    }

    private void bindTextComponent(JTextComponent editorComponent) {
        textComponentAdapter = new TextComponentAdapter(editorComponent);
        textComponentAdapter.setBinding(getBinding());
        textComponentAdapter.bindComponents();
    }

    private void unbindTextComponent() {
        if (textComponentAdapter != null) {
            textComponentAdapter.unbindComponents();
            textComponentAdapter.setBinding(null);
            textComponentAdapter = null;
        }
    }
}
