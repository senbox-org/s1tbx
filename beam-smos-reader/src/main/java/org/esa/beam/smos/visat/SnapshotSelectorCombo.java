/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
package org.esa.beam.smos.visat;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class SnapshotSelectorCombo {
    private final JComboBox comboBox;
    private final SnapshotSelector snapshotSelector;
    private SnapshotSelectorComboModel model;

    SnapshotSelectorCombo(final SnapshotSelectorComboModel model) {
        this.model = model;

        comboBox = new JComboBox(model.getComboBoxModel());
        snapshotSelector = new SnapshotSelector(model.getSelectedSnapshotSelectorModel());

        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSnapshotIdSelector();
            }
        });
        comboBox.setRenderer(new ComboBoxRenderer());
    }

    public JComboBox getComboBox() {
        return comboBox;
    }

    void setModel(SnapshotSelectorComboModel model) {
        if (this.model != model) {
            this.model = model;
            comboBox.setModel(model.getComboBoxModel());
            snapshotSelector.setModel(model.getSelectedSnapshotSelectorModel());
        }
    }

    private void updateSnapshotIdSelector() {
        snapshotSelector.setModel(this.model.getSelectedSnapshotSelectorModel());
    }

    private static class ComboBoxRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            final Component component = super.getListCellRendererComponent(list, value, index, isSelected,
                                                                           cellHasFocus);
            if (component instanceof JLabel) {
                final JLabel label = (JLabel) component;

                switch (index) {
                    case 0:
                        label.setText("Any");
                        break;
                    case 1:
                        label.setText("X");
                        break;
                    case 2:
                        label.setText("Y");
                        break;
                    case 3:
                        label.setText("XY");
                }
            }

            return component;
        }
    }
}
