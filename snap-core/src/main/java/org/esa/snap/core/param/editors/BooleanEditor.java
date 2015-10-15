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

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * An editor which uses a {@link javax.swing.JCheckBox}.
 */
public class BooleanEditor extends AbstractParamEditor {

    private JCheckBox _checkBox;

    public BooleanEditor(Parameter parameter) {
        super(parameter, false);
    }

    public JCheckBox getCheckBox() {
        return _checkBox;
    }

    /**
     * Gets the UI component used to edit the parameter's value.
     */
    public JComponent getEditorComponent() {
        return getCheckBox();
    }

    @Override
    protected void initUI() {
        // do not call super.initUI() since we don't want any labels to be created
        _checkBox = new JCheckBox();
        nameEditorComponent(_checkBox);
        if (getParameter().getProperties().getLabel() != null) {
            _checkBox.setText(getParameter().getProperties().getLabel());
        }
        if (getParameter().getProperties().getDescription() != null) {
            _checkBox.setToolTipText(getParameter().getProperties().getDescription());
        }
        _checkBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent event) {
                updateParameter();
            }
        });
    }

    @Override
    public void updateUI() {
        super.updateUI();

        boolean newValue;
        if (getParameter().getValue() instanceof Boolean) {
            newValue = (Boolean) getParameter().getValue();
        } else {
            newValue = Boolean.valueOf(getParameter().getValueAsText());
        }
        if (getCheckBox().isSelected() != newValue) {
            getCheckBox().setSelected(newValue);
        }
        if (getCheckBox().isEnabled() != isEnabled()) {
            getCheckBox().setEnabled(isEnabled());
        }
    }

    private void updateParameter() {
        boolean newValue = getCheckBox().isSelected();
        getParameter().setValue(newValue, null);
    }
}
