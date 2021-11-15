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
package org.esa.s1tbx.insar.gpf.ui;

import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Map;

/**
 * User interface for MultiReferenceOp
 */
public class MultiMasterOpUI extends BaseOperatorUI {

    private static final JFileChooser outputDirectory = new JFileChooser();


    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {
        outputDirectory.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        outputDirectory.setDialogType(0);


        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {
        if(outputDirectory.getSelectedFile() == null){
            outputDirectory.setSelectedFile(new File(System.getProperty("user.home")));

        }
    }


    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("outputFolder", outputDirectory.getSelectedFile().getAbsolutePath());
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        contentPane.add(new JLabel(" "), gbc);
        gbc.gridy++;

        final JPanel gridPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc2 = DialogUtils.createGridBagConstraints();
        gridPanel.setBorder(BorderFactory.createTitledBorder("Output parameters"));


        DialogUtils.addComponent(contentPane, gbc, "Output folder", outputDirectory);
        gbc.gridy++;

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

}
