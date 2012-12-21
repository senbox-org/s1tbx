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
package org.esa.nest.gpf;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.util.SystemUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Map;

/**
 * Writer OperatorUI
 */
public class TileWriterUI extends BaseOperatorUI {

    private final JComboBox numberOfTiles = new JComboBox(
            new String[] { "2","4","9","16","36","64","100","256" } );

    TargetProductSelector targetProductSelector = null;
    private static final String FILE_PARAMETER = "file";
    private AppContext appContext;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        paramMap = parameterMap;
        targetProductSelector = new TargetProductSelector();
        this.appContext = appContext;

        File saveDir = null;
        final Object value = paramMap.get(FILE_PARAMETER);
        if(value != null) {
            final File file = (File)value;
            saveDir = file.getParentFile();
        }

        if(saveDir == null) {
            final String homeDirPath = SystemUtils.getUserHomeDir().getPath();
            final String savePath = appContext.getPreferences().getPropertyString(BasicApp.PROPERTY_KEY_APP_LAST_SAVE_DIR, homeDirPath);
            saveDir = new File(savePath);
        }
        targetProductSelector.getModel().setProductDir(saveDir);
        targetProductSelector.getOpenInAppCheckBox().setText("Open in " + appContext.getApplicationName());

        initParameters();

        return createPanel();
    }

    public JPanel createPanel() {
        final JPanel subPanel1 = new JPanel(new BorderLayout(3, 3));
        subPanel1.add(targetProductSelector.getProductNameLabel(), BorderLayout.NORTH);
        subPanel1.add(targetProductSelector.getProductNameTextField(), BorderLayout.CENTER);

        final JPanel subPanel2 = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        subPanel2.add(targetProductSelector.getSaveToFileCheckBox());
        subPanel2.add(targetProductSelector.getFormatNameComboBox());

        final JPanel subPanel3 = new JPanel(new BorderLayout(3, 3));
        subPanel3.add(targetProductSelector.getProductDirLabel(), BorderLayout.NORTH);
        subPanel3.add(targetProductSelector.getProductDirTextField(), BorderLayout.CENTER);
        subPanel3.add(targetProductSelector.getProductDirChooserButton(), BorderLayout.EAST);

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);

        tableLayout.setCellPadding(0, 0, new Insets(3, 3, 3, 3));
        tableLayout.setCellPadding(1, 0, new Insets(3, 3, 3, 3));
        tableLayout.setCellPadding(2, 0, new Insets(3, 3, 3, 3));
        tableLayout.setCellPadding(3, 0, new Insets(3, 3, 3, 3));
        tableLayout.setCellPadding(4, 0, new Insets(0, 24, 3, 3));
        tableLayout.setCellPadding(5, 0, new Insets(3, 3, 3, 3));

        final JPanel panel = new JPanel(tableLayout);
        panel.setBorder(BorderFactory.createTitledBorder("Target Tile Products"));
        panel.add(new JLabel("Number of Tiles:"));
        panel.add(numberOfTiles);
        panel.add(subPanel1);
        panel.add(subPanel2);
        panel.add(subPanel3);
        panel.add(targetProductSelector.getOpenInAppCheckBox());

        return panel;
    }

    @Override
    public void initParameters() {
        assert(paramMap != null);
        String fileName = "target";
        final Object value = paramMap.get(FILE_PARAMETER);
        if(value != null) {
            final File file = (File)value;
            fileName = file.getName();
        } else if(sourceProducts != null && sourceProducts.length > 0) {
            fileName = sourceProducts[0].getName();
        }
        targetProductSelector.getProductNameTextField().setText(fileName);
        targetProductSelector.getModel().setProductName(fileName);

        String numTiles = (String)paramMap.get("numberOfTiles");
        if(numTiles == null || numTiles.isEmpty())
            numTiles = "4";
        numberOfTiles.setSelectedItem(numTiles);
    }

    @Override
    public UIValidation validateParameters() {

        final String productDir = targetProductSelector.getModel().getProductDir().getAbsolutePath();
        appContext.getPreferences().setPropertyString(BasicApp.PROPERTY_KEY_APP_LAST_SAVE_DIR, productDir);

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        if(targetProductSelector.getModel().getProductName() != null) {
            paramMap.put("file", targetProductSelector.getModel().getProductFile());
            paramMap.put("formatName", targetProductSelector.getModel().getFormatName());
            paramMap.put("numberOfTiles", numberOfTiles.getSelectedItem());
        }
    }
}