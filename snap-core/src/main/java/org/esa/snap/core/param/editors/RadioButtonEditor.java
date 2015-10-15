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
package org.esa.snap.core.param.editors;

import org.esa.snap.core.param.AbstractParamEditor;
import org.esa.snap.core.param.Parameter;

import javax.swing.JComponent;
import javax.swing.JRadioButton;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * An editor which uses a {@link javax.swing.JRadioButton}.
 */
public class RadioButtonEditor extends AbstractParamEditor {

    private JRadioButton _radioButton;

    public RadioButtonEditor(final Parameter parameter) {
        super(parameter, false);
    }

    public JRadioButton getRadioButton() {
        return _radioButton;
    }

    /**
     * Gets the UI component used to edit the parameter's value.
     */
    public JComponent getEditorComponent() {
        return getRadioButton();
    }

    @Override
    protected void initUI() {
        // do not call super.initUI() since we don't want any labels to be created
        _radioButton = new JRadioButton();
        nameEditorComponent(_radioButton);
        if (getParameter().getProperties().getLabel() != null) {
            _radioButton.setText(getParameter().getProperties().getLabel());
        }
        if (getParameter().getProperties().getDescription() != null) {
            _radioButton.setToolTipText(getParameter().getProperties().getDescription());
        }
        _radioButton.addItemListener(new ItemListener() {

            public void itemStateChanged(final ItemEvent event) {
                updateParameter();
            }
        });
    }

    @Override
    public void updateUI() {
        super.updateUI();

        final boolean newValue;
        if (getParameter().getValue() instanceof Boolean) {
            newValue = (Boolean) getParameter().getValue();
        } else {
            newValue = Boolean.valueOf(getParameter().getValueAsText());
        }
        if (getRadioButton().isSelected() != newValue) {
            getRadioButton().setSelected(newValue);
        }
        if (getRadioButton().isEnabled() != isEnabled()) {
            getRadioButton().setEnabled(isEnabled());
        }
    }

    private void updateParameter() {
        final boolean newValue = getRadioButton().isSelected();
        getParameter().setValue(newValue, null);
    }
}
