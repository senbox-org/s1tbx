/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.util;

import org.esa.beam.framework.ui.GridBagUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

/**
 * NEST
 * User: lveci
 * Date: Jan 8, 2009
 */
public final class DialogUtils {


    public static void enableComponents(JComponent label, JComponent field, boolean flag) {
        label.setVisible(flag);
        field.setVisible(flag);
    }

    public static void addComponent(JPanel contentPane, GridBagConstraints gbc, JLabel label, JComponent component) {
        gbc.gridx = 0;
        contentPane.add(label, gbc);
        gbc.gridx = 1;
        contentPane.add(component, gbc);
        gbc.gridx = 0;
    }

    public static JLabel addComponent(JPanel contentPane, GridBagConstraints gbc, String text, JComponent component) {
        gbc.gridx = 0;
        final JLabel label = new JLabel(text);
        contentPane.add(label, gbc);
        gbc.gridx = 1;
        contentPane.add(component, gbc);
        gbc.gridx = 0;
        return label;
    }

    public static void addInnerPanel(JPanel contentPane, GridBagConstraints gbc, JLabel label,
                                     JComponent component1, JComponent component2) {
        contentPane.add(label, gbc);

        final JPanel innerPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc2 = DialogUtils.createGridBagConstraints();
        innerPane.add(component1, gbc2);
        gbc2.gridx = 1;
        innerPane.add(component2, gbc2);

        gbc.gridx = 1;
        contentPane.add(innerPane, gbc);
    }

    public static JFormattedTextField createFormattedTextField(final NumberFormat numFormat, final Object value,
                                                               final PropertyChangeListener propListener) {
        final JFormattedTextField field = new JFormattedTextField(numFormat);
        field.setValue(value);
        field.setColumns(10);
        if (propListener != null)
            field.addPropertyChangeListener("value", propListener);

        return field;
    }

    public static void fillPanel(final JPanel panel, final GridBagConstraints gbc) {
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        panel.add(new JPanel(), gbc);
    }

    public static JButton CreateButton(final String name, final String text, final ImageIcon icon, final JPanel panel) {
        JButton button = new JButton();
        button.setName(name);
        button = new JButton();
        button.setIcon(icon);
        button.setBackground(panel.getBackground());
        button.setText(text);
        button.setActionCommand(name);
        return button;
    }

    public static class ComponentListPanel extends JPanel {

        private final JPanel labelPanel;
        private final JPanel fieldPanel;

        public ComponentListPanel() {
            final GridLayout grid = new GridLayout(0, 1);
            grid.setVgap(5);

            labelPanel = new JPanel(grid);
            fieldPanel = new JPanel(new GridLayout(0, 1));

            this.add(labelPanel, BorderLayout.CENTER);
            this.add(fieldPanel, BorderLayout.LINE_END);
        }

        public void addComponent(final String labelStr, final JComponent component) {
            labelPanel.add(new JLabel(labelStr));
            fieldPanel.add(component);
        }
    }

    public static GridBagConstraints createGridBagConstraints() {
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets.top = 1;
        gbc.insets.bottom = 1;
        gbc.insets.right = 1;
        gbc.insets.left = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        return gbc;
    }

    public static class TextAreaKeyListener implements KeyListener {
        private boolean changedByUser = false;

        public void keyPressed(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
            changedByUser = true;
        }

        public void keyTyped(KeyEvent e) {
        }

        public boolean isChangedByUser() {
            return changedByUser;
        }
    }
}
