/*
 * $Id: RadioButtonEditor.java,v 1.2 2006/10/10 14:47:22 norman Exp $
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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComponent;
import javax.swing.JRadioButton;

import org.esa.beam.framework.param.AbstractParamEditor;
import org.esa.beam.framework.param.Parameter;

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
