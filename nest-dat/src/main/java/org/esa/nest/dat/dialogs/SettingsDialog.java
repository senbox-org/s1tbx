/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.dialogs;

import com.jidesoft.icons.IconsFactory;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.JideSplitPane;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.util.DialogUtils;
import org.esa.nest.util.Settings;
import org.jdom.Attribute;
import org.jdom.Element;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Display the Settings
 */
public class SettingsDialog extends ModelessDialog {

    private SettingsTree settingsTree;
    private DefaultMutableTreeNode rootNode;

    private final JLabel editLabel = new JLabel("Value:");
    private final JTextField editField = new JTextField("");
    private Element selectedElement = null;

    public SettingsDialog(String title) {
        super(VisatApp.getApp().getMainFrame(), title, ModalDialog.ID_OK_CANCEL, null);

        final JScrollPane scrollPane = new JideScrollPane(createTree());
        scrollPane.setPreferredSize(new Dimension(320, 480));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.setViewportBorder(null);

        final JideSplitPane splitPane = new JideSplitPane(JideSplitPane.HORIZONTAL_SPLIT);
        splitPane.addPane(scrollPane);
        splitPane.addPane(createEditPanel());

        editLabel.setMinimumSize(new Dimension(500, 20));
        editField.addKeyListener(new SettingsKeyListener());
        editField.setMargin(new Insets(2, 5, 2, 5));
        editField.setMaximumSize(new Dimension(500, 20));
        
        setContent(splitPane);
    }

    private JPanel createEditPanel() {
        final JPanel editPanel = new JPanel();
        editPanel.setPreferredSize(new Dimension(500, 480));
        editPanel.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();
        gbc.ipady = 5;
        
        gbc.gridy = 20;
        gbc.gridy++;
        gbc.gridx = 0;
        editPanel.add(editLabel, gbc);
        gbc.gridy++;
        gbc.gridy++;
        gbc.gridx = 0;
        editPanel.add(editField, gbc);
        gbc.gridy++;

        DialogUtils.fillPanel(editPanel, gbc);

        return editPanel;
    }

    private SettingsTree createTree() {
        rootNode = new DefaultMutableTreeNode("");
        settingsTree = new SettingsTree(false);//rootNode);
        settingsTree.populateTree(Settings.instance().getSettingsRootXML(), rootNode);
        settingsTree.setRootVisible(false);
        settingsTree.setShowsRootHandles(true);
        settingsTree.addMouseListener(new PTMouseListener());

        final DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) settingsTree.getCellRenderer();
        renderer.setLeafIcon(IconsFactory.getImageIcon(BasicApp.class, "/org/esa/beam/resources/images/icons/RsBandAsSwath16.gif"));
        renderer.setClosedIcon(IconsFactory.getImageIcon(BasicApp.class, "/org/esa/beam/resources/images/icons/RsGroupClosed16.gif"));
        renderer.setOpenIcon(IconsFactory.getImageIcon(BasicApp.class, "/org/esa/beam/resources/images/icons/RsGroupOpen16.gif"));
        return settingsTree;
    }

    private class PTMouseListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent event) {
            int selRow = settingsTree.getRowForLocation(event.getX(), event.getY());
            if (selRow >= 0) {
                final TreePath selPath = settingsTree.getPathForLocation(event.getX(), event.getY());
                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();

                final Object o = node.getUserObject();
                if (o instanceof Element) {
                    selectSetting((Element)o);
                }
            }
        }
    }

    private void selectSetting(Element elem) {
        selectedElement = elem;
        final Attribute label = elem.getAttribute(Settings.LABEL);
        String labelText = elem.getName();
        if (label != null) {
            labelText = label.getValue();
        }
        editLabel.setText("  "+labelText+": ");

        final Attribute elemValue = elem.getAttribute(Settings.VALUE);
        if (elemValue != null) {
            editField.setText(elemValue.getValue());
        } else {
            editField.setText("");
        }

        editField.setColumns(80);
    }

    @Override
    protected void onOK() {

        Settings.instance().Save();
        // reload settings
        Settings.instance().Load();

        hide();
    }

    private class SettingsKeyListener implements KeyListener {
        /**
         * Invoked when a key has been typed.
         * See the class description for {@link java.awt.event.KeyEvent} for a definition of
         * a key typed event.
         */
        public void keyTyped(KeyEvent e) {
            selectedElement.getAttribute(Settings.VALUE).setValue(editField.getText());
        }

        /**
         * Invoked when a key has been pressed.
         * See the class description for {@link KeyEvent} for a definition of
         * a key pressed event.
         */
        public void keyPressed(KeyEvent e) {
        }

        /**
         * Invoked when a key has been released.
         * See the class description for {@link KeyEvent} for a definition of
         * a key released event.
         */
        public void keyReleased(KeyEvent e) {
            selectedElement.getAttribute(Settings.VALUE).setValue(editField.getText());
        }
    }
}