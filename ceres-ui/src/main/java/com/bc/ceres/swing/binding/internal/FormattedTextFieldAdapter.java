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

import com.bc.ceres.swing.binding.ComponentAdapter;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A binding for a {@link javax.swing.JFormattedTextField} component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class FormattedTextFieldAdapter extends ComponentAdapter implements PropertyChangeListener {

    private final JFormattedTextField textField;

    public FormattedTextFieldAdapter(JFormattedTextField textField) {
        this.textField = textField;
    }

    @Override
    public JComponent[] getComponents() {
        return new JComponent[]{textField};
    }

    @Override
    public void bindComponents() {
        textField.addPropertyChangeListener("value", this);
    }

    @Override
    public void unbindComponents() {
        textField.removePropertyChangeListener("value", this);
    }

    @Override
    public void adjustComponents() {
        Object value = getBinding().getPropertyValue();
        textField.setValue(value);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        getBinding().setPropertyValue(textField.getValue());
    }
}
