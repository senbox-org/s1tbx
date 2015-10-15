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
import javax.swing.JLabel;

/**
 * An "editor" which is a label component.
 */
public class LabelEditor extends AbstractParamEditor {

    private JLabel _label;

    public LabelEditor(Parameter parameter) {
        super(parameter, false);
    }

    public JLabel getLabel() {
        return _label;
    }

    /**
     * Gets the UI component used to edit the parameter's value.
     */
    public JComponent getEditorComponent() {
        return getLabel();
    }

    @Override
    protected void initUI() {
        _label = new JLabel();
        nameEditorComponent(_label);
        _label.setText(getParameter().getValueAsText());
        if (getParameter().getProperties().getDescription() != null) {
            _label.setToolTipText(getParameter().getProperties().getDescription());
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();

        String text = getParameter().getValueAsText();
        getLabel().setText(text);
        if (getLabel().isEnabled() != isEnabled()) {
            getLabel().setEnabled(isEnabled());
        }
    }
}
