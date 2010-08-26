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

package org.esa.beam.gpf.operators.reproject;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ImageGeometry;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.DemSelector;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.crs.CrsForm;
import org.esa.beam.framework.ui.crs.CrsSelectionPanel;
import org.esa.beam.framework.ui.crs.CustomCrsForm;
import org.esa.beam.framework.ui.crs.OutputGeometryForm;
import org.esa.beam.framework.ui.crs.OutputGeometryFormModel;
import org.esa.beam.framework.ui.crs.PredefinedCrsForm;
import org.esa.beam.util.ProductUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marco Zuehlke
 * @author Marco Paters
 * @since BEAM 4.7
 */
class ReprojectionForm extends JTabbedPane {

    private static final String[] RESAMPLING_IDENTIFIER = {"Nearest", "Bilinear", "Bicubic"};

    private final boolean orthoMode;
    private final AppContext appContext;
    private final SourceProductSelector sourceProductSelector;
    private final TargetProductSelector targetProductSelector;
    private final Model reprojectionModel;
    private final PropertyContainer reprojectionContainer;

    private DemSelector demSelector;
    private CrsSelectionPanel crsSelectionPanel;
    
    private OutputGeometryFormModel outputGeometryModel;

    private JButton outputParamButton;
    private InfoForm infoForm;
    private CoordinateReferenceSystem crs;
    private CollocationCrsForm collocationCrsUI;


    ReprojectionForm(TargetProductSelector targetProductSelector, boolean orthorectify, AppContext appContext) {
        this.targetProductSelector = targetProductSelector;
        this.orthoMode = orthorectify;
        this.appContext = appContext;
        this.sourceProductSelector = new SourceProductSelector(appContext, "Source Product:");
        if (orthoMode) {
            this.sourceProductSelector.setProductFilter(new OrthorectifyProductFilter());
        } else {
            this.sourceProductSelector.setProductFilter(new GeoCodingProductFilter());
        }
        this.reprojectionModel = new Model();
        this.reprojectionContainer = PropertyContainer.createObjectBacked(reprojectionModel);
        createUI();
    }

    Map<String, Object> getParameterMap() {
        Map<String, Object> parameterMap = new HashMap<String, Object>(5);
        parameterMap.put("resamplingName", reprojectionModel.resamplingMethod);
        parameterMap.put("includeTiePointGrids", reprojectionModel.reprojTiePoints);
        parameterMap.put("noDataValue", reprojectionModel.noDataValue);
        if (!collocationCrsUI.getRadioButton().isSelected()) {
            parameterMap.put("crs", getSelectedCrs().toWKT());
        }
        if (orthoMode) {
            parameterMap.put("orthorectify", orthoMode);
            if (demSelector.isUsingExternalDem()) {
                parameterMap.put("elevationModelName", demSelector.getDemName());
            } else {
                parameterMap.put("elevationModelName", null);
            }
        }

        if (!reprojectionModel.preserveResolution && outputGeometryModel != null) {
            PropertyContainer container = outputGeometryModel.getPropertyContainer();
            parameterMap.put("referencePixelX", container.getValue("referencePixelX"));
            parameterMap.put("referencePixelY", container.getValue("referencePixelY"));
            parameterMap.put("easting", container.getValue("easting"));
            parameterMap.put("northing", container.getValue("northing"));
            parameterMap.put("orientation", container.getValue("orientation"));
            parameterMap.put("pixelSizeX", container.getValue("pixelSizeX"));
            parameterMap.put("pixelSizeY", container.getValue("pixelSizeY"));
            parameterMap.put("width", container.getValue("width"));
            parameterMap.put("height", container.getValue("height"));
        }
        return parameterMap;
    }

    Map<String, Product> getProductMap() {
        final Map<String, Product> productMap = new HashMap<String, Product>(5);
        productMap.put("source", getSourceProduct());
        if (collocationCrsUI.getRadioButton().isSelected()) {
            productMap.put("collocateWith", collocationCrsUI.getCollocationProduct());
        }
        return productMap;
    }

    Product getSourceProduct() {
        return sourceProductSelector.getSelectedProduct();
    }

    CoordinateReferenceSystem getSelectedCrs() {
        return crs;
    }

    void prepareShow() {
        sourceProductSelector.initProducts();
        crsSelectionPanel.prepareShow();
    }

    void prepareHide() {
        sourceProductSelector.releaseProducts();
        crsSelectionPanel.prepareHide();
        if (outputGeometryModel != null) {
            outputGeometryModel.setSourceProduct(null);
        }
    }

    String getExternalDemName() {
        if (orthoMode && demSelector.isUsingExternalDem()) {
            return demSelector.getDemName();
        }
        return null;
    }

    private void createUI() {
        addTab("I/O Parameters", createIOPanel());
        addTab("Reprojection Parameters", createParametersPanel());
    }

    private JPanel createIOPanel() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(3, 3);

        final JPanel ioPanel = new JPanel(tableLayout);
        ioPanel.add(createSourceProductPanel());
        ioPanel.add(targetProductSelector.createDefaultPanel());
        ioPanel.add(tableLayout.createVerticalSpacer());
        return ioPanel;
    }

    private JPanel createParametersPanel() {
        final JPanel parameterPanel = new JPanel();
        final TableLayout layout = new TableLayout(1);
        layout.setTablePadding(4, 4);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableWeightX(1.0);
        parameterPanel.setLayout(layout);
        CrsForm customCrsUI = new CustomCrsForm(appContext);
        CrsForm predefinedCrsUI = new PredefinedCrsForm(appContext);
        collocationCrsUI = new CollocationCrsForm(appContext);
        CrsForm[] crsForms = new CrsForm[]{customCrsUI, predefinedCrsUI, collocationCrsUI};
        crsSelectionPanel = new CrsSelectionPanel(crsForms);
        sourceProductSelector.addSelectionChangeListener(new AbstractSelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                final Product product = (Product) event.getSelection().getSelectedValue();
                crsSelectionPanel.setReferenceProduct(product);
            }
        });

        parameterPanel.add(crsSelectionPanel);
        if (orthoMode) {
            demSelector = new DemSelector();
            parameterPanel.add(demSelector);
        }
        parameterPanel.add(createOuputSettingsPanel());
        infoForm = new InfoForm();
        parameterPanel.add(infoForm.createUI());

        crsSelectionPanel.addPropertyChangeListener("crs", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateCRS();
            }
        });
        updateCRS();
        return parameterPanel;
    }

    private void updateCRS() {
        final Product sourceProduct = getSourceProduct();
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
            outputGeometryModel.setTargetCrs(crs);
        }
        updateOutputParameterState();
    }

    private void updateProductSize() {
        int width = 0;
        int height = 0;
        final Product sourceProduct = getSourceProduct();
        if (sourceProduct != null && crs != null) {
            if (!reprojectionModel.preserveResolution && outputGeometryModel != null) {
                PropertyContainer container = outputGeometryModel.getPropertyContainer();
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

                @Override
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

        final JCheckBox preserveResolutionCheckBox = new JCheckBox("Preserve resolution");
        context.bind(Model.PRESERVE_RESOLUTION, preserveResolutionCheckBox);
        collocationCrsUI.getCrsUI().addPropertyChangeListener("collocate", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final boolean collocate = (Boolean) evt.getNewValue();
                reprojectionContainer.setValue(Model.PRESERVE_RESOLUTION,
                                                collocate || reprojectionModel.preserveResolution);
                preserveResolutionCheckBox.setEnabled(!collocate);
            }
        });
        outputSettingsPanel.add(preserveResolutionCheckBox);

        JCheckBox includeTPcheck = new JCheckBox("Reproject tie-point grids", true);
        context.bind(Model.REPROJ_TIEPOINTS, includeTPcheck);
        outputSettingsPanel.add(includeTPcheck);

        outputParamButton = new JButton("Output Parameters...");
        outputParamButton.setEnabled(!reprojectionModel.preserveResolution);
        outputParamButton.addActionListener(new OutputParamActionListener());
        outputSettingsPanel.add(outputParamButton);

        outputSettingsPanel.add(new JLabel("No-data value:"));
        final JTextField noDataField = new JTextField();

        outputSettingsPanel.add(noDataField);
        context.bind(Model.NO_DATA_VALUE, noDataField);
        outputSettingsPanel.add(new JPanel());

        outputSettingsPanel.add(new JLabel("Resampling method:"));
        JComboBox resampleComboBox = new JComboBox(RESAMPLING_IDENTIFIER);
        resampleComboBox.setPrototypeDisplayValue(RESAMPLING_IDENTIFIER[0]);
        context.bind(Model.RESAMPLING_METHOD, resampleComboBox);
        outputSettingsPanel.add(resampleComboBox);

        reprojectionContainer.addPropertyChangeListener(Model.PRESERVE_RESOLUTION, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateOutputParameterState();
            }
        });

        return outputSettingsPanel;
    }

    private void updateOutputParameterState() {
        outputParamButton.setEnabled(!reprojectionModel.preserveResolution && (crs != null));
        updateProductSize();        
    }

    private JPanel createSourceProductPanel() {
        final JPanel panel = sourceProductSelector.createDefaultPanel();
        sourceProductSelector.getProductNameLabel().setText("Name:");
        sourceProductSelector.getProductNameComboBox().setPrototypeDisplayValue(
                "MER_RR__1PPBCM20030730_071000_000003972018_00321_07389_0000.N1");
        sourceProductSelector.addSelectionChangeListener(new AbstractSelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                final Product sourceProduct = getSourceProduct();
                updateTargetProductName(sourceProduct);
                GeoPos centerGeoPos = null;
                if (sourceProduct != null) {
                    centerGeoPos = ProductUtils.getCenterGeoPos(sourceProduct);
                }
                infoForm.setCenterPos(centerGeoPos);
                if (outputGeometryModel != null) {
                    outputGeometryModel.setSourceProduct(sourceProduct);
                }
                updateCRS();
            }
        });
        return panel;
    }

    private void updateTargetProductName(Product selectedProduct) {
        final TargetProductSelectorModel selectorModel = targetProductSelector.getModel();
        if (selectedProduct != null) {
            final String productName = MessageFormat.format("{0}_reprojected", selectedProduct.getName());
            selectorModel.setProductName(productName);
        } else if (selectorModel.getProductName() == null) {
            selectorModel.setProductName("reprojected");
        }
    }

    private class OutputParamActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            try {
                final Product sourceProduct = getSourceProduct();
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
        JOptionPane.showMessageDialog(getParent(), message, "Reprojection", JOptionPane.WARNING_MESSAGE);
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

    private static class OrthorectifyProductFilter implements ProductFilter {

        @Override
        public boolean accept(Product product) {
            return product.canBeOrthorectified();
        }
    }

    private static class GeoCodingProductFilter implements ProductFilter {

        @Override
        public boolean accept(Product product) {
            final GeoCoding geoCoding = product.getGeoCoding();
            return geoCoding != null && geoCoding.canGetGeoPos() && geoCoding.canGetPixelPos();
        }
    }
}
