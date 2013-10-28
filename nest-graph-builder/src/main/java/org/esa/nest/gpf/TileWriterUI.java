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
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Map;

/**
 * Writer OperatorUI
 */
public class TileWriterUI extends BaseOperatorUI {

    private final JLabel divisionByLabel = new JLabel("Division By:     ");
    private final JComboBox divisionBy = new JComboBox(new String[] { "Tiles","Pixels" } );

    private final JLabel numberOfTilesLabel = new JLabel("Number of Tiles:     ");
    private final JComboBox numberOfTiles = new JComboBox(
            new String[] { "2","4","9","16","36","64","100","256" } );

    private final JLabel pixelSizeLabel = new JLabel("Pixel Size:     ");
    private final JTextField pixelSize = new JTextField("");

    private final TargetProductSelector targetProductSelector = new TargetProductSelector();
    private static final String FILE_PARAMETER = "file";
    private AppContext appContext;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        this.appContext = appContext;

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        divisionBy.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                updateDivisionBy();
            }
        });

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

        return panel;
    }

    public JPanel createPanel() {
        final JPanel contentPane = new JPanel(new BorderLayout(2, 2));

        final JPanel optionsPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 1;
        DialogUtils.addComponent(optionsPane, gbc, divisionByLabel, divisionBy);
        gbc.gridy++;
        DialogUtils.addComponent(optionsPane, gbc, numberOfTilesLabel, numberOfTiles);
        DialogUtils.addComponent(optionsPane, gbc, pixelSizeLabel, pixelSize);

        contentPane.add(optionsPane, BorderLayout.CENTER);

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

        final JPanel targetPanel = new JPanel(tableLayout);
        targetPanel.setBorder(BorderFactory.createTitledBorder("Target Tile Products"));
        targetPanel.add(subPanel1);
        targetPanel.add(subPanel2);
        targetPanel.add(subPanel3);
        targetPanel.add(targetProductSelector.getOpenInAppCheckBox());
        contentPane.add(targetPanel, BorderLayout.SOUTH);

        updateDivisionBy();

        return contentPane;
    }

    @Override
    public void initParameters() {
        assert(paramMap != null);

        divisionBy.setSelectedItem(paramMap.get("divisionBy"));

        String numTiles = (String)paramMap.get("numberOfTiles");
        if(numTiles == null || numTiles.isEmpty())
            numTiles = "4";
        numberOfTiles.setSelectedItem(numTiles);

        pixelSize.setText(String.valueOf(paramMap.get("pixelSize")));

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
            paramMap.put("divisionBy", divisionBy.getSelectedItem());
            paramMap.put("numberOfTiles", numberOfTiles.getSelectedItem());
            paramMap.put("pixelSize", Integer.parseInt(pixelSize.getText()));

            paramMap.put("file", targetProductSelector.getModel().getProductFile());
            paramMap.put("formatName", targetProductSelector.getModel().getFormatName());
        }
    }

    private void updateDivisionBy() {

        final String item = (String)divisionBy.getSelectedItem();
        if(item.equals("Pixels")) {
            numberOfTiles.setVisible(false);
            numberOfTilesLabel.setVisible(false);
            pixelSize.setVisible(true);
            pixelSizeLabel.setVisible(true);
        } else {
            numberOfTiles.setVisible(true);
            numberOfTilesLabel.setVisible(true);
            pixelSize.setVisible(false);
            pixelSizeLabel.setVisible(false);
        }
    }
}