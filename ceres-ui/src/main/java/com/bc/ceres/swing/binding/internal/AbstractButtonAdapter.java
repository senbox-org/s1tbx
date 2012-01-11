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

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * An abstract binding for a {@link javax.swing.AbstractButton} component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class AbstractButtonAdapter extends ComponentAdapter implements ActionListener {

    private final AbstractButton button;

    public AbstractButtonAdapter(AbstractButton button) {
        super();
        this.button = button;
    }

    public AbstractButton getButton() {
        return button;
    }

    @Override
    public JComponent[] getComponents() {
        return new JComponent[]{button};
    }

    @Override
    public void bindComponents() {
        button.addActionListener(this);
    }

    @Override
    public void unbindComponents() {
        button.removeActionListener(this);
    }

    @Override
    public void adjustComponents() {
        Boolean selected = (Boolean) getBinding().getPropertyValue();
        if(selected != null)
            button.setSelected(selected);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        getBinding().setPropertyValue(getButton().isSelected());
    }
}
