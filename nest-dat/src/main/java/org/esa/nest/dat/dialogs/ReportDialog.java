/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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

import org.esa.beam.framework.ui.ModalDialog;
import org.esa.nest.dat.DatApp;
import org.esa.nest.reports.Report;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 */
public class ReportDialog extends ModalDialog {

    public ReportDialog(final Report report) {
        super(DatApp.getApp().getMainFrame(), DatApp.getApp().getAppName()+" Report Preview",
                ModalDialog.ID_CANCEL, null);    /*I18N*/

        getJDialog().setPreferredSize(new Dimension(800, 800));

        final JLabel reportLabel = new JLabel(report.getAsHTML());

        final JPanel dialogContent = new JPanel(new BorderLayout(4,4));

        final JPanel labelPane = new JPanel(new BorderLayout());
        labelPane.add(BorderLayout.NORTH, reportLabel);
        dialogContent.add(BorderLayout.CENTER, new JScrollPane(labelPane));

        final JPanel buttonPanel = createButtonPanel();
        dialogContent.add(buttonPanel, BorderLayout.EAST);

        setContent(dialogContent);
    }

    @Override
    protected void onOther() {
        // override default behaviour by doing nothing
    }

    public static JPanel createButtonPanel() {
        final JPanel panel = new JPanel(new GridLayout(10, 1));

        final JButton pdfButton = DialogUtils.CreateButton("Export PDF", "Export PDF", null, panel);
        panel.add(pdfButton);
        pdfButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {

            }
        });
        final JButton htmlButton = DialogUtils.CreateButton("Export HTML", "Export HTML", null, panel);
        panel.add(htmlButton);
        htmlButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {


            }
        });

        return panel;
    }
}
