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
package org.esa.nest.gpf;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ImageGeometry;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.crs.*;
import org.esa.beam.framework.ui.crs.projdef.AbstractCrsProvider;
import org.esa.beam.framework.ui.crs.projdef.CustomCrsPanel;
import org.esa.beam.gpf.operators.reproject.CollocationCrsForm;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.VisatApp;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.GeodeticDatum;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

/**
 * User interface for MapReProjection
 */
public class MapReProjectionOpUI extends BaseOperatorUI {

    private final JList bandList = new JList();

    private static final String[] RESAMPLING_IDENTIFIER = {"Nearest", "Bilinear", "Bicubic"};

    private AppContext appContext;
    private InfoForm infoForm;
    private CrsSelectionPanel crsSelectionPanel;
    private CoordinateReferenceSystem crs;
    private Model reprojectionModel;
    private PropertyContainer reprojectionContainer;
    private OutputGeometryFormModel outputGeometryModel;

    private CrsForm customCrsUI;
    private CrsForm predefinedCrsUI;
    private CollocationCrsForm collocationCrsUI;

    private JButton outputParamButton;
    private JCheckBox preserveResolutionCheckBox;
    private JCheckBox includeTPcheck;
    private JComboBox resampleComboBox;
    private JTextField noDataField;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        this.appContext = appContext;
        this.reprojectionModel = new Model();
        this.reprojectionContainer = PropertyContainer.createObjectBacked(reprojectionModel);

        initializeOperatorUI(operatorName, parameterMap);

        final JComponent panel = createPanel();
        initParameters();

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        OperatorUIUtils.initBandList(bandList, getBandNames());

        if(sourceProducts != null) {
            crsSelectionPanel.setReferenceProduct(sourceProducts[0]);
        }

        if(paramMap.get("resamplingName") != null)
            reprojectionModel.resamplingMethod = (String)paramMap.get("resamplingName");
        else
            reprojectionModel.resamplingMethod = (String)paramMap.get("resampling");
        resampleComboBox.setSelectedItem(reprojectionModel.resamplingMethod);
        if(paramMap.get("includeTiePointGrids") != null) {
            reprojectionModel.reprojTiePoints = (Boolean)paramMap.get("includeTiePointGrids");
            includeTPcheck.setSelected(reprojectionModel.reprojTiePoints);
        }
        if(paramMap.get("preserveResolution") != null) {
            reprojectionModel.preserveResolution = (Boolean)paramMap.get("preserveResolution");
            preserveResolutionCheckBox.setSelected(reprojectionModel.preserveResolution);
        }
        if(paramMap.get("noDataValue") != null) {
            reprojectionModel.noDataValue = (Double)paramMap.get("noDataValue");
            noDataField.setText(String.valueOf(reprojectionModel.noDataValue));
        }

        if(paramMap.get("crs") != null) {
            crs = createTargetCRS((String)paramMap.get("crs"));
            infoForm.setCrsInfoText(crs.getName().getCode(), crs.toString());
            final CustomCrsPanel customCRS = (CustomCrsPanel)customCrsUI.getCrsUI();
            final String name = crs.getName().getCode();
            String projection = name;
            String datum = "";
            if(name.contains("/")) {
                projection = name.substring(0, name.indexOf("/")-1).replace("_", " ");
                datum = name.substring(name.indexOf("/")+2, name.length());
            }

            int cnt = customCRS.projectionComboBox.getItemCount();
            for(int i = 0; i < cnt; ++i) {
                AbstractCrsProvider provider = (AbstractCrsProvider)customCRS.projectionComboBox.getItemAt(i);
                if(provider.getName().equalsIgnoreCase(projection)) {
                    customCRS.projectionComboBox.setSelectedItem(provider);
                    break;
                }
            }
            cnt = customCRS.datumComboBox.getItemCount();
            for(int i = 0; i < cnt; ++i) {
                GeodeticDatum geoDatum = (GeodeticDatum)customCRS.datumComboBox.getItemAt(i);
                if(geoDatum.getName().getCode().equalsIgnoreCase(datum)) {
                    customCRS.datumComboBox.setSelectedItem(geoDatum);
                    break;
                }
            }
        } else {
            updateCRS();
        }
        
        if(sourceProducts != null && crs != null && outputGeometryModel == null) {
            outputGeometryModel = new OutputGeometryFormModel(sourceProducts[0], crs);
        }

        if (outputGeometryModel != null) {
            final PropertySet container = outputGeometryModel.getPropertySet();
            if(paramMap.get("referencePixelX") != null)
                container.setValue("referencePixelX", paramMap.get("referencePixelX"));
            if(paramMap.get("referencePixelY") != null)
                container.setValue("referencePixelY", paramMap.get("referencePixelY"));
            if(paramMap.get("easting") != null)
                container.setValue("easting", paramMap.get("easting"));
            if(paramMap.get("northing") != null)
                container.setValue("northing", paramMap.get("northing"));
            if(paramMap.get("orientation") != null)
                container.setValue("orientation", paramMap.get("orientation"));
            if(paramMap.get("pixelSizeX") != null)
                container.setValue("pixelSizeX", paramMap.get("pixelSizeX"));
            if(paramMap.get("pixelSizeY") != null)
                container.setValue("pixelSizeY", paramMap.get("pixelSizeY"));
            if(paramMap.get("width") != null)
                container.setValue("width", paramMap.get("width"));
            if(paramMap.get("height") != null)
                container.setValue("height", paramMap.get("height"));
        }
        updateOutputParameterState();
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateBandList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);

        paramMap.put("resamplingName", reprojectionModel.resamplingMethod);
        paramMap.put("includeTiePointGrids", reprojectionModel.reprojTiePoints);
        paramMap.put("preserveResolution", reprojectionModel.preserveResolution);
        paramMap.put("noDataValue", reprojectionModel.noDataValue);

        updateCRS();
        if (crs != null && !collocationCrsUI.getRadioButton().isSelected()) {
            paramMap.put("crs", crs.toWKT());
        }
        
        if (!reprojectionModel.preserveResolution && outputGeometryModel != null) {
            final PropertySet container = outputGeometryModel.getPropertySet();
            paramMap.put("referencePixelX", container.getValue("referencePixelX"));
            paramMap.put("referencePixelY", container.getValue("referencePixelY"));
            paramMap.put("easting", container.getValue("easting"));
            paramMap.put("northing", container.getValue("northing"));
            paramMap.put("orientation", container.getValue("orientation"));
            paramMap.put("pixelSizeX", container.getValue("pixelSizeX"));
            paramMap.put("pixelSizeY", container.getValue("pixelSizeY"));
            paramMap.put("width", container.getValue("width"));
            paramMap.put("height", container.getValue("height"));
        }
    }

    private CoordinateReferenceSystem createTargetCRS(String crsStr) throws OperatorException {
        try {
            //if (wktFile != null) {
            //    return CRS.parseWKT(FileUtils.readText(wktFile));
            //}
            if (crsStr != null) {
                try {
                    return CRS.parseWKT(crsStr);
                } catch (FactoryException ignored) {
                    // prefix with EPSG, if there are only numbers
                    if (crsStr.matches("[0-9]*")) {
                        crsStr = "EPSG:" + crsStr;
                    }
                    // append center coordinates for AUTO code
                    if (crsStr.matches("AUTO:[0-9]*") && sourceProducts != null) {
                        final GeoPos centerGeoPos = ProductUtils.getCenterGeoPos(sourceProducts[0]);
                        crsStr = String.format("%s,%s,%s", crsStr, centerGeoPos.lon, centerGeoPos.lat);
                    }
                    // force longitude==x-axis and latitude==y-axis
                    return CRS.decode(crsStr, true);
                }
            }
        } catch (FactoryException e) {
            throw new OperatorException(String.format("Target CRS could not be created: %s", e.getMessage()), e);
        }
        throw new OperatorException("Target CRS could not be created.");
    }

    private JComponent createPanel() {

        final JPanel parameterPanel = new JPanel();
        final TableLayout layout = new TableLayout(1);
        layout.setTablePadding(4, 4);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableWeightX(1.0);
        parameterPanel.setLayout(layout);
        customCrsUI = new CustomCrsForm(appContext);
        predefinedCrsUI = new PredefinedCrsForm(appContext);
        collocationCrsUI = new CollocationCrsForm(appContext);
        CrsForm[] crsForms = new CrsForm[]{customCrsUI, predefinedCrsUI, collocationCrsUI};
        crsSelectionPanel = new CrsSelectionPanel(crsForms);

        parameterPanel.add(crsSelectionPanel);

        parameterPanel.add(createOuputSettingsPanel());
        infoForm = new InfoForm();
        parameterPanel.add(infoForm.createUI());

        crsSelectionPanel.addPropertyChangeListener("crs", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateCRS();
            }
        });

        return parameterPanel;
    }

    private void updateCRS() {
        if(sourceProducts != null) {

            final Product sourceProduct = sourceProducts[0];
            try {
                if (sourceProduct != null) {
                    crs = crsSelectionPanel.getCrs(ProductUtils.getCenterGeoPos(sourceProduct));
                    if (crs != null) {
                        infoForm.setCrsInfoText(crs.getName().getCode(), crs.toString());
                    } else {
                        infoForm.setCrsErrorText("No valid 'Coordinate Reference System' selected.");
                    }
                } else {
                    infoForm.setCrsErrorText("No source product selected.");
                    crs = null;
                }
            } catch (FactoryException e) {
                infoForm.setCrsErrorText(e.getMessage());
                crs = null;
            }
            if (outputGeometryModel != null) {
                outputGeometryModel.setSourceProduct(sourceProduct);
                outputGeometryModel.setTargetCrs(crs);
            }
        }
        updateOutputParameterState();
    }

    private void updateOutputParameterState() {
        outputParamButton.setEnabled(!reprojectionModel.preserveResolution);
        updateProductSize();
    }

    private void updateProductSize() {
        int width = 0;
        int height = 0;
        if(sourceProducts == null) return;
        final Product sourceProduct = sourceProducts[0];
        if (sourceProduct != null && crs != null) {
            if (!reprojectionModel.preserveResolution && outputGeometryModel != null) {
                PropertySet container = outputGeometryModel.getPropertySet();
                width = (Integer) container.getValue("width");
                height = (Integer) container.getValue("height");
            } else {
                ImageGeometry iGeometry;
                final Product collocationProduct = collocationCrsUI.getCollocationProduct();
                if(collocationCrsUI.getRadioButton().isSelected() && collocationProduct != null) {
                    iGeometry = ImageGeometry.createCollocationTargetGeometry(sourceProduct, collocationProduct);
                }else {
                    iGeometry = ImageGeometry.createTargetGeometry(sourceProduct, crs,
                                                                   null, null, null, null,
                                                                   null, null, null, null,
                                                                   null);

                }
                Rectangle imageRect = iGeometry.getImageRect();
                width = imageRect.width;
                height = imageRect.height;
            }
        }
        infoForm.setWidth(width);
        infoForm.setHeight(height);
    }

    private class InfoForm {

        private JLabel widthLabel;
        private JLabel heightLabel;
        private JLabel centerLatLabel;
        private JLabel centerLonLabel;
        private JLabel crsLabel;
        private String wkt;
        private JButton wktButton;

        void setWidth(int width) {
            widthLabel.setText(Integer.toString(width));
        }

        void setHeight(int height) {
            heightLabel.setText(Integer.toString(height));
        }

        void setCenterPos(GeoPos geoPos) {
            if (geoPos != null) {
                centerLatLabel.setText(geoPos.getLatString());
                centerLonLabel.setText(geoPos.getLonString());
            } else {
                centerLatLabel.setText("");
                centerLonLabel.setText("");
            }
        }

        void setCrsErrorText(String infoText) {
            setCrsInfoText("<html><b>"+infoText+"</b>", null);
        }

        void setCrsInfoText(String infoText, String wkt) {
            this.wkt = wkt;
            crsLabel.setText(infoText);
            boolean hasWKT = (wkt != null);
            wktButton.setEnabled(hasWKT);
        }

        JPanel createUI() {
            widthLabel = new JLabel();
            heightLabel = new JLabel();
            centerLatLabel = new JLabel();
            centerLonLabel = new JLabel();
            crsLabel = new JLabel();

            final TableLayout tableLayout = new TableLayout(5);
            tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
            tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
            tableLayout.setTablePadding(4, 4);
            tableLayout.setColumnWeightX(0, 0.0);
            tableLayout.setColumnWeightX(1, 0.0);
            tableLayout.setColumnWeightX(2, 1.0);
            tableLayout.setColumnWeightX(3, 0.0);
            tableLayout.setColumnWeightX(4, 1.0);
            tableLayout.setCellColspan(2, 1, 3);
            tableLayout.setCellPadding(0, 3, new Insets(4, 24, 4, 20));
            tableLayout.setCellPadding(1, 3, new Insets(4, 24, 4, 20));


            final JPanel panel = new JPanel(tableLayout);
            panel.setBorder(BorderFactory.createTitledBorder("Output Information"));
            panel.add(new JLabel("Scene Width:"));
            panel.add(widthLabel);
            panel.add(new JLabel("pixel"));
            panel.add(new JLabel("Center Longitude:"));
            panel.add(centerLonLabel);

            panel.add(new JLabel("Scene Height:"));
            panel.add(heightLabel);
            panel.add(new JLabel("pixel"));
            panel.add(new JLabel("Center Latitude:"));
            panel.add(centerLatLabel);

            panel.add(new JLabel("CRS:"));
            panel.add(crsLabel);
            wktButton = new JButton("Show WKT");
            wktButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    JTextArea wktArea = new JTextArea(30, 40);
                    wktArea.setEditable(false);
                    wktArea.setText(wkt);
                    final JScrollPane scrollPane = new JScrollPane(wktArea);
                    final ModalDialog dialog = new ModalDialog(appContext.getApplicationWindow(),
                                                               "Coordinate reference system as well known text",
                                                               scrollPane,
                                                               ModalDialog.ID_OK, null);
                    dialog.show();
                }
            });
            wktButton.setEnabled(false);
            panel.add(wktButton);
            return panel;
        }
    }

    private JPanel createOuputSettingsPanel() {
        final TableLayout tableLayout = new TableLayout(3);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setColumnFill(0, TableLayout.Fill.NONE);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setColumnPadding(0, new Insets(4, 4, 4, 20));
        tableLayout.setColumnWeightX(0, 0.0);
        tableLayout.setColumnWeightX(1, 0.0);
        tableLayout.setColumnWeightX(2, 1.0);
        tableLayout.setCellColspan(0, 1, 2);
        tableLayout.setCellPadding(1, 0, new Insets(4, 24, 4, 20));

        final JPanel outputSettingsPanel = new JPanel(tableLayout);
        outputSettingsPanel.setBorder(BorderFactory.createTitledBorder("Output Settings"));

        final BindingContext context = new BindingContext(reprojectionContainer);

        preserveResolutionCheckBox = new JCheckBox("Preserve resolution");
        context.bind(Model.PRESERVE_RESOLUTION, preserveResolutionCheckBox);
        collocationCrsUI.getCrsUI().addPropertyChangeListener("collocate", new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                final boolean collocate = (Boolean) evt.getNewValue();
                reprojectionContainer.setValue(Model.PRESERVE_RESOLUTION,
                                                collocate || reprojectionModel.preserveResolution);
                preserveResolutionCheckBox.setEnabled(!collocate);
            }
        });
        outputSettingsPanel.add(preserveResolutionCheckBox);

        includeTPcheck = new JCheckBox("Reproject tie-point grids", true);
        context.bind(Model.REPROJ_TIEPOINTS, includeTPcheck);
        outputSettingsPanel.add(includeTPcheck);

        outputParamButton = new JButton("Output Parameters...");
        outputParamButton.setEnabled(!reprojectionModel.preserveResolution);
        outputParamButton.addActionListener(new OutputParamActionListener());
        outputSettingsPanel.add(outputParamButton);

        outputSettingsPanel.add(new JLabel("No-data value:"));
        noDataField = new JTextField();

        outputSettingsPanel.add(noDataField);
        context.bind(Model.NO_DATA_VALUE, noDataField);
        outputSettingsPanel.add(new JPanel());

        outputSettingsPanel.add(new JLabel("Resampling method:"));
        resampleComboBox = new JComboBox(RESAMPLING_IDENTIFIER);
        resampleComboBox.setPrototypeDisplayValue(RESAMPLING_IDENTIFIER[0]);
        context.bind(Model.RESAMPLING_METHOD, resampleComboBox);
        outputSettingsPanel.add(resampleComboBox);

        reprojectionContainer.addPropertyChangeListener(Model.PRESERVE_RESOLUTION, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                updateOutputParameterState();
            }
        });

        return outputSettingsPanel;
    }

    private static class Model {

        private static final String PRESERVE_RESOLUTION = "preserveResolution";
        private static final String REPROJ_TIEPOINTS = "reprojTiePoints";
        private static final String NO_DATA_VALUE = "noDataValue";
        private static final String RESAMPLING_METHOD = "resamplingMethod";

        private boolean preserveResolution = true;
        private boolean reprojTiePoints = true;
        private double noDataValue = Double.NaN;
        private String resamplingMethod = RESAMPLING_IDENTIFIER[0];
    }

    private class OutputParamActionListener implements ActionListener {

        public void actionPerformed(ActionEvent event) {
            try {
                if(sourceProducts == null) return;
                final Product sourceProduct = sourceProducts[0];
                if (sourceProduct == null) {
                    showWarningMessage("Please select a product to reproject.\n");
                    return;
                }
                if (crs == null) {
                    showWarningMessage("Please specify a 'Coordinate Reference System' first.\n");
                    return;
                }
                OutputGeometryFormModel workCopy;
                if (outputGeometryModel != null) {
                    workCopy = new OutputGeometryFormModel(outputGeometryModel);
                } else {
                    final Product collocationProduct = collocationCrsUI.getCollocationProduct();
                    if(collocationCrsUI.getRadioButton().isSelected() && collocationProduct != null) {
                        workCopy = new OutputGeometryFormModel(sourceProduct, collocationProduct);
                    }else {
                        workCopy = new OutputGeometryFormModel(sourceProduct, crs);
                    }
                }
                final OutputGeometryForm form = new OutputGeometryForm(workCopy);
                final ModalDialog outputParametersDialog = new OutputParametersDialog(appContext.getApplicationWindow(),
                        sourceProduct, workCopy);
                outputParametersDialog.setContent(form);
                if (outputParametersDialog.show() == ModalDialog.ID_OK) {
                    outputGeometryModel = workCopy;
                    updateProductSize();
                }
            } catch (Exception e) {
                appContext.handleError("Could not create a 'Coordinate Reference System'.\n" +
                                       e.getMessage(), e);
            }
        }

    }

    private void showWarningMessage(String message) {
        JOptionPane.showMessageDialog(VisatApp.getApp().getMainFrame(), message,
                "Reprojection", JOptionPane.WARNING_MESSAGE);
    }

    private class OutputParametersDialog extends ModalDialog {

        private static final String TITLE = "Output Parameters";

        private final Product sourceProduct;
        private final OutputGeometryFormModel outputGeometryFormModel;

        public OutputParametersDialog(Window parent, Product sourceProduct, OutputGeometryFormModel outputGeometryFormModel) {
            super(parent, TITLE, ModalDialog.ID_OK_CANCEL | ModalDialog.ID_RESET, null);
            this.sourceProduct = sourceProduct;
            this.outputGeometryFormModel = outputGeometryFormModel;
        }

        @Override
        protected void onReset() {
            final Product collocationProduct = collocationCrsUI.getCollocationProduct();
            ImageGeometry imageGeometry;
            if(collocationCrsUI.getRadioButton().isSelected() && collocationProduct != null) {
                imageGeometry = ImageGeometry.createCollocationTargetGeometry(sourceProduct, collocationProduct);
            }else {
                imageGeometry = ImageGeometry.createTargetGeometry(sourceProduct, crs,
                null, null, null, null,
                null, null, null, null, null);
            }
            outputGeometryFormModel.resetToDefaults(imageGeometry);
        }
    }
}