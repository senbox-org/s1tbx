/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.insar.gpf.ui;

import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;

/**
 * User interface for StaMPS Export
 */
public class StampsExportOpUI extends BaseOperatorUI {

    private final JTextField targetFolder = new JTextField("");
    private final JButton targetFolderBrowseButton = new JButton("...");

    public static final String STAMPS_TARGET_DIR_KEY = "s1tbx.stampsTargetFolder";

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);

        final JComponent panel = createPanel();
        initParameters();

        targetFolderBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final File file = Dialogs.requestFileForSave("Target Folder", true, null, null, null, null, STAMPS_TARGET_DIR_KEY);
                if(file != null) {
                    targetFolder.setText(file.getAbsolutePath());
                }
            }
        });

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        final File targetFolderFile = (File) paramMap.get("targetFolder");
        if(targetFolderFile != null) {
            targetFolder.setText(targetFolderFile.getAbsolutePath());
        } else {
            targetFolder.setText(SnapApp.getDefault().getPreferences().get(STAMPS_TARGET_DIR_KEY, ""));
        }
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        final String extFileStr = targetFolder.getText();
        if(!extFileStr.isEmpty()) {
            paramMap.put("targetFolder", new File(extFileStr));
        }
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Target Folder", targetFolder);
        gbc.gridx = 2;
        contentPane.add(targetFolderBrowseButton, gbc);
        gbc.gridy++;

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }
}
