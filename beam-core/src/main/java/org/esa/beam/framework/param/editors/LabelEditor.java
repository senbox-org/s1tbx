/*
 * $Id: LabelEditor.java,v 1.2 2006/10/10 14:47:22 norman Exp $
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

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.esa.beam.framework.param.AbstractParamEditor;
import org.esa.beam.framework.param.Parameter;

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

    protected void initUI() {
        _label = new JLabel();
        nameEditorComponent(_label);
        _label.setText(getParameter().getValueAsText());
        if (getParameter().getProperties().getDescription() != null) {
            _label.setToolTipText(getParameter().getProperties().getDescription());
        }
    }

    public void updateUI() {
        super.updateUI();

        String text = getParameter().getValueAsText();
        getLabel().setText(text);
        if (getLabel().isEnabled() != isEnabled()) {
            getLabel().setEnabled(isEnabled());
        }
    }
}
