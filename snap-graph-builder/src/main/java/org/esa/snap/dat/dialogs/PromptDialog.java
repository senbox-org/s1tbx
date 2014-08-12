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
package org.esa.snap.dat.dialogs;

import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Jun 5, 2008
 * To change this template use File | Settings | File Templates.
 */
public class PromptDialog extends ModalDialog {

    private JTextComponent prompt1;
    private boolean ok = false;

    public PromptDialog(String title, String label, String defaultValue, boolean textArea) {
        super(VisatApp.getApp().getMainFrame(), title, ModalDialog.ID_OK_CANCEL, null);

        final JPanel content = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets.right = 4;
        gbc.gridy = 0;
        gbc.weightx = 0;

        gbc.insets.top = 2;
        prompt1 = addTextComponent(content, gbc, label, defaultValue, textArea);

        getJDialog().setMinimumSize(new Dimension(400, 100));

        setContent(content);
    }

    private static JTextComponent addTextComponent(final JPanel content, final GridBagConstraints gbc,
                                                   final String text, final String value, boolean isTextArea) {
        JTextComponent textComp;
        if (isTextArea) {
            final JTextArea textArea = new JTextArea(value);
            textArea.setColumns(50);
            textArea.setRows(7);
            textComp = textArea;
        } else {
            content.add(new JLabel(text), gbc);
            gbc.weightx = 1;
            textComp = new JTextField(value);
        }
        textComp.setEditable(true);
        content.add(textComp, gbc);
        gbc.gridy++;
        return textComp;
    }

    public String getValue() {
        return prompt1.getText();
    }

    protected void onOK() {
        ok = true;
        hide();
    }

    public boolean IsOK() {
        return ok;
    }

}