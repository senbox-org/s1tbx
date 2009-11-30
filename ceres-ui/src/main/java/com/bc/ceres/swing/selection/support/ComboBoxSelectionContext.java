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