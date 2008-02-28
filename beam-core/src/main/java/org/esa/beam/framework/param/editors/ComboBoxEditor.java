/*
 * $Id: ComboBoxEditor.java,v 1.2 2006/10/10 14:47:22 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.param.editors;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

import org.esa.beam.framework.param.AbstractParamEditor;
import org.esa.beam.framework.param.Parameter;

/**
 * An editor which uses a {@link javax.swing.JComboBox}.
 */
public class ComboBoxEditor extends AbstractParamEditor {

    private JComboBox _comboBox;

    public ComboBoxEditor(Parameter parameter) {
        super(parameter, true);
    }

    public JComboBox getComboBox() {
        return _comboBox;
    }

    public JTextComponent getTextComponent() {
        javax.swing.ComboBoxEditor swingComboBoxEditor = _comboBox.getEditor();
        if (swingComboBoxEditor != null) {
            Component component = swingComboBoxEditor.getEditorComponent();
            if (component instanceof JTextComponent) {
                return (JTextComponent) component;
            }
        }
        return null;
    }

    /**
     * Gets the UI component used to edit the parameter's value.
     */
    public JComponent getEditorComponent() {
        return getComboBox();
    }

    protected void initUI() {

        setDefaultLabelComponent(true);

        _comboBox = new JComboBox();
        nameEditorComponent(_comboBox);

        // Configure combo box
        if (getParameter().getProperties().getDescription() != null) {
            _comboBox.setToolTipText(getParameter().getProperties().getDescription());
        }
        if (getParameter().getProperties().getValueSet() != null) {
            _comboBox.setModel(new DefaultComboBoxModel(getParameter().getProperties().getValueSet()));
        }
        _comboBox.setEnabled(!getParameter().getProperties().isReadOnly());
        _comboBox.setEditable(!getParameter().getProperties().isValueSetBound());
        JTextComponent textComp = getTextComponent();
        if (textComp != null) {
            textComp.setInputVerifier(getDefaultInputVerifier());
        }
        _comboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                updateParameter();
            }
        });
        java.awt.Font font = _comboBox.getFont();
        if (font != null) {
            _comboBox.setFont(new java.awt.Font(font.getName(),
                                                Font.PLAIN,
                                                font.getSize()));
        }
    }

    public void updateUI() {
        super.updateUI();

        JComboBox comboBox = getComboBox();
        String text = getParameter().getValueAsText();
        JTextComponent textComp = getTextComponent();
        if (text != null && !text.equals(textComp.getText())) {
            if (textComp != null) {
                if (!textComp.getText().equals(text)) {
                    textComp.setText(text);
                }
            }
            Object selItem = comboBox.getSelectedItem();
            if (selItem == null || !selItem.equals(text)) {
                comboBox.setSelectedItem(text);
            }
        }
        if (comboBox.isEnabled() != isEnabled()) {
            comboBox.setEnabled(isEnabled());
        }
    }

    public void reconfigureUI() {
        JComboBox jComboBox = getComboBox();
        ItemListener[] itemListeners = (ItemListener[]) jComboBox.getListeners(ItemListener.class);
        ActionListener[] actionListeners = (ActionListener[]) jComboBox.getListeners(ActionListener.class);
        for (int i = 0; i < itemListeners.length; i++) {
            jComboBox.removeItemListener(itemListeners[i]);
        }
        for (int i = 0; i < actionListeners.length; i++) {
            jComboBox.removeActionListener(actionListeners[i]);
        }

        jComboBox.removeAllItems();
        String[] valueSet = getParameter().getProperties().getValueSet();
        if (valueSet != null) {
            for (int i = 0; i < valueSet.length; i++) {
                String value = valueSet[i];
                if (value != null) {
                    jComboBox.addItem(value);
                }
            }
        }

//@todo 1 se/nf - IMPORTANT! must we set enabled when user sets uiEnabled (= false) in Paramerter?
        setEnabled(!getParameter().getProperties().isReadOnly());
//        jComboBox.setEnabled(!getParameter().getProperties().isReadOnly());
        jComboBox.setEditable(!getParameter().getProperties().isValueSetBound());
        jComboBox.setSelectedItem(getParameter().getValueAsText());

        for (int i = 0; i < actionListeners.length; i++) {
            jComboBox.addActionListener(actionListeners[i]);
        }
        for (int i = 0; i < itemListeners.length; i++) {
            jComboBox.addItemListener(itemListeners[i]);
        }
    }

    private void updateParameter() {
        JTextComponent textComp = getTextComponent();
        if (textComp != null && getComboBox().isEditable()) {
            setParameterValue(textComp);
        } else {
            Object newValue = getComboBox().getSelectedItem();
            getParameter().setValueAsText(newValue != null ? newValue.toString() : "",
                                          getExceptionHandler());
        }
    }
}