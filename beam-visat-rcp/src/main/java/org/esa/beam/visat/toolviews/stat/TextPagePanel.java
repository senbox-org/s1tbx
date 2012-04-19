/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.ui.application.ToolView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A pane within the statistics window which contains text only.
 *
 * @author Marco Peters
 */
abstract class TextPagePanel extends PagePanel {

    private JTextArea textArea;
    private final String defaultText;

    protected TextPagePanel(final ToolView parentDialog, final String defaultText, String helpId, String titlePrefix) {
        super(parentDialog, helpId, titlePrefix);
        this.defaultText = defaultText;
    }

    public JTextArea getTextArea() {
        return textArea;
    }

    @Override
    protected void initComponents() {
        textArea = new JTextArea();
        textArea.setText(defaultText);
        textArea.setEditable(false);
        textArea.addMouseListener(new PopupHandler());
        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    @Override
    protected void updateComponents() {
        if (isVisible()) {
            ensureValidData();
            textArea.setText(createText());
            textArea.setCaretPosition(0);
        }
    }

    protected void ensureValidData() {
    }

    protected abstract String createText();

    @Override
    protected String getDataAsText() {
        return textArea.getText();
    }

    @Override
    protected void handlePopupCreated(final JPopupMenu popupMenu) {
        final JMenuItem menuItem = new JMenuItem("Select All");     /*I18N*/
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                textArea.selectAll();
                textArea.requestFocus();
            }
        });
        popupMenu.add(menuItem);
    }
}
