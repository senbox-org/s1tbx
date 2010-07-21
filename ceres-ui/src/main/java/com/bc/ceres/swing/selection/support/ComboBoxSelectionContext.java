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

package com.bc.ceres.swing.selection.support;

import com.bc.ceres.swing.selection.AbstractSelectionContext;
import com.bc.ceres.swing.selection.Selection;

import javax.swing.JComboBox;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * A selection provider that wraps a {@link javax.swing.JList}.
 * Elements contained in {@link com.bc.ceres.swing.selection.Selection}s handled by this provider
 * represent currently selected list objects.
 */
public class ComboBoxSelectionContext extends AbstractSelectionContext {

    private final ItemListener listSelectionListener;
    private JComboBox comboBox;

    public ComboBoxSelectionContext(final JComboBox comboBox) {
        listSelectionListener = new ComboBoxSelectionHandler();
        this.comboBox = comboBox;
        this.comboBox.addItemListener(listSelectionListener);
    }

    @Override
    public Selection getSelection() {
        if (comboBox.getSelectedIndex() == -1) {
            return DefaultSelection.EMPTY;
        }
        return new DefaultSelection<Object>(comboBox.getSelectedItem());
    }

    @Override
    public void setSelection(Selection selection) {
        comboBox.setSelectedItem(selection.getSelectedValue());
    }

    public JComboBox getComboBox() {
        return comboBox;
    }

    public void setComboBox(JComboBox comboBox) {
        if (comboBox != this.comboBox) {
            this.comboBox.removeItemListener(listSelectionListener);
            this.comboBox = comboBox;
            this.comboBox.addItemListener(listSelectionListener);
            fireSelectionChange(getSelection());
        }
    }

    private class ComboBoxSelectionHandler implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            fireSelectionChange(getSelection());
        }
    }
}