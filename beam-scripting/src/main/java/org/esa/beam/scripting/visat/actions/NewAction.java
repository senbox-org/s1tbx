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

package org.esa.beam.scripting.visat.actions;

import org.esa.beam.scripting.visat.ScriptConsoleForm;

import javax.script.ScriptEngineFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class NewAction extends ScriptConsoleAction {
    public static final String ID = "scriptConsole.new";

    public NewAction(ScriptConsoleForm scriptConsoleForm) {
        super(scriptConsoleForm,
              "New",
              ID,
              "/org/esa/beam/scripting/visat/icons/document-new-16.png");
    }


    public void actionPerformed(ActionEvent e) {
        ScriptEngineFactory[] scriptEngineFactories = getScriptManager().getEngineFactories();
        final Item[] items = new Item[scriptEngineFactories.length];
        for (int i = 0; i < scriptEngineFactories.length; i++) {
            ScriptEngineFactory scriptEngineFactory = scriptEngineFactories[i];
            items[i] = new Item(scriptEngineFactory.getLanguageName(), scriptEngineFactory);
        }
        Item selectedItem = null;
        if (items.length != 0) {
            selectedItem = promptForEngine(items);
        } else {
            getScriptConsoleForm().showErrorMessage("No scripting language available.");
        }
        if (selectedItem != null) {
            getScriptConsoleForm().newScript(selectedItem.scriptEngineFactory);
        }
    }

    private Item promptForEngine(Item[] items) {
        final JList list = new JList(items);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);

        final JButton infoButton = new JButton();
        infoButton.setToolTipText("Show script engine details");
        infoButton.setIcon(loadIcon("/org/esa/beam/scripting/visat/icons/help-browser-16.png"));
        infoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Item item = (Item) list.getSelectedValue();
                showEngineDetails(list, item.scriptEngineFactory);
            }
        });
        final JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(infoButton);

        JPanel titlePanel = new JPanel(new BorderLayout(16, 0));
        titlePanel.add(new JLabel("Language:"), BorderLayout.WEST);
        titlePanel.add(toolBar, BorderLayout.EAST);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(titlePanel, BorderLayout.NORTH);
        contentPanel.add(new JScrollPane(list), BorderLayout.CENTER);

        final int i = JOptionPane.showOptionDialog(getScriptConsoleForm().getContentPanel(),
                                                   contentPanel,
                                                   "Select Scripting Language",
                                                   JOptionPane.OK_CANCEL_OPTION,
                                                   JOptionPane.PLAIN_MESSAGE,
                                                   null, null, null);
        if (i == JOptionPane.OK_OPTION) {
            return (Item) list.getSelectedValue();
        }
        return null;
    }

    private void showEngineDetails(JComponent parent, ScriptEngineFactory scriptEngineFactory) {
        StringBuilder out = new StringBuilder();

        out.append(String.format("Engine name: %s\n", scriptEngineFactory.getEngineName()));
        out.append(String.format("Engine version: %s\n", scriptEngineFactory.getEngineVersion()));
        out.append(String.format("Language name: %s\n", scriptEngineFactory.getLanguageName()));
        out.append(String.format("Language version: %s\n", scriptEngineFactory.getLanguageVersion()));

        out.append("File name extension(s):");
        final List<String> extensions = scriptEngineFactory.getExtensions();
        for (String extension : extensions) {
            out.append(" ");
            out.append(extension);
        }
        out.append("\n");

        out.append("File content type(s):");
        final List<String> mimeTypes = scriptEngineFactory.getMimeTypes();
        for (String mimeType : mimeTypes) {
            out.append(" ");
            out.append(mimeType);
        }
        out.append("\n");

        out.append("Engine factory name(s): ");
        final List<String> names = scriptEngineFactory.getNames();
        for (String name : names) {
            out.append(" ");
            out.append(name);
        }
        out.append("\n");

        JOptionPane.showMessageDialog(parent, out.toString(), "Script Engine Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private static class Item {
        private final String languageName;
        private final ScriptEngineFactory scriptEngineFactory;

        public Item(String languageName, ScriptEngineFactory scriptEngineFactory) {
            this.languageName = languageName;
            this.scriptEngineFactory = scriptEngineFactory;
        }

        @Override
        public String toString() {
            return languageName;
        }
    }

}
