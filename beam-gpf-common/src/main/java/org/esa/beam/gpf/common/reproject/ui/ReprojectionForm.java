package org.esa.beam.gpf.common.reproject.ui;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.DemSelector;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.application.SelectionChangeEvent;
import org.esa.beam.framework.ui.application.SelectionChangeListener;
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
    private final PropertyContainer reprojectionlContainer;

    private DemSelector demSelector;
    private CrsForm crsForm;
    private PropertyContainer outputParameterContainer;

    private JButton outputParamButton;
    private InfoForm infoForm;
    private CoordinateReferenceSystem crs;
    

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
        this.reprojectionlContainer = PropertyContainer.createObjectBacked(reprojectionModel);
        createUI();
    }

    Map<String, Object> getParameterMap() {
        Map<String, Object> parameterMap = new HashMap<String, Object>(5);
        parameterMap.put("resamplingName", reprojectionModel.resamplingMethod);
        parameterMap.put("includeTiePointGrids", reprojectionModel.reprojTiePoints);
        parameterMap.put("noDataValue", reprojectionModel.noDataValue);
        try {
            if (!crsForm.isCollocate()) {
                final CoordinateReferenceSystem crs = getSelectedCrs();
                parameterMap.put("wkt", crs.toWKT());
            }
        } catch (FactoryException e) {
            throw new IllegalStateException(e);
        }
        if (orthoMode) {
            parameterMap.put("orthorectify", orthoMode);
            if (demSelector.isUsingExternalDem()) {
                parameterMap.put("elevationModelName", demSelector.getDemName());
            } else {
                parameterMap.put("elevationModelName", null);
            }
        }

        if (outputParameterContainer != null) {
            parameterMap.put("referencePixelX", outputParameterContainer.getValue("referencePixelX"));
            parameterMap.put("referencePixelY", outputParameterContainer.getValue("referencePixelY"));
            parameterMap.put("easting", outputParameterContainer.getValue("easting"));
            parameterMap.put("northing", outputParameterContainer.getValue("northing"));
            parameterMap.put("orientation", outputParameterContainer.getValue("orientation"));
            parameterMap.put("pixelSizeX", outputParameterContainer.getValue("pixelSizeX"));
            parameterMap.put("pixelSizeY", outputParameterContainer.getValue("pixelSizeY"));
            parameterMap.put("width", outputParameterContainer.getValue("width"));
            parameterMap.put("height", outputParameterContainer.getValue("height"));
        }
        return parameterMap;
    }

    Map<String, Product> getProductMap() {
        final Map<String, Product> productMap = new HashMap<String, Product>(5);
        productMap.put("source", getSourceProduct());
        if (crsForm.isCollocate()) {
            productMap.put("collocate", crsForm.getCollocationProduct());
        }
        return productMap;
    }

    Product getSourceProduct() {
        return sourceProductSelector.getSelectedProduct();
    }

    CoordinateReferenceSystem getSelectedCrs() throws FactoryException {
        return crs;
    }

    void prepareShow() {
        sourceProductSelector.initProducts();
        crsForm.prepareShow();
    }

    void prepareHide() {
        sourceProductSelector.releaseProducts();
        crsForm.prepareHide();
    }
    
    String getExternamDemName() {
        if (orthoMode && demSelector.isUsingExternalDem()) {
            return demSelector.getDemName();
        }
        return null;
    }

    private void createUI() {
        addTab("I/O Parameters", createIOPanel());
        addTab("Reprojection Parameters", createParameterPanel());
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

    private JPanel createParameterPanel() {
        final JPanel parameterPanel = new JPanel();
        final TableLayout layout = new TableLayout(1);
        layout.setTablePadding(4, 4);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableWeightX(1.0);
        parameterPanel.setLayout(layout);

        crsForm = new CrsForm(appContext, sourceProductSelector);
        parameterPanel.add(crsForm);
        if (orthoMode) {
            demSelector = new DemSelector();
            parameterPanel.add(demSelector);
        }
        parameterPanel.add(createOuputSettingsPanel());
        infoForm = new InfoForm();
        parameterPanel.add(infoForm.createUI());
        
        crsForm.addPropertyChangeListener("crs", new PropertyChangeListener() {
            
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateCRS();
            }
        });
        updateCRS();
        return parameterPanel;
    }
    
    private void updateCRS() {
        try {
            crs = crsForm.getCrs(getSourceProduct());
            if (crs != null) {
                infoForm.setCrs(crs.getName().getCode(), crs.toString());
            } else {
                infoForm.setCrs("No valid 'CoordinateReference System' selected.", null);
            }
        } catch (FactoryException e) {
            infoForm.setCrs(e.getMessage(), null);
            crs = null;
        }
        updateOutputParameterState();
        updateProductSize();
    }
    
    private void updateProductSize() {
        if (outputParameterContainer != null) {
            infoForm.setWidth((Integer)outputParameterContainer.getValue("width"));
            infoForm.setHeight((Integer)outputParameterContainer.getValue("height"));
        }
    }
    
    private class InfoForm {

        private JLabel widthLabel;
        private JLabel heightLabel;
        private JLabel centerLatLabel;
        private JLabel centerLonLabel;
        private JLabel crsLabel;
        private String wkt;
        private JButton wktButton;
        
        void setWidth(int width){
            widthLabel.setText(Integer.toString(width));
        }

        void setHeight(int height){
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
        
        void setCrs(String crsText, String wkt) {
            this.wkt = wkt;
            crsLabel.setText(crsText);
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

        final BindingContext context = new BindingContext(reprojectionlContainer);

        final JCheckBox preserveResolutionCheckBox = new JCheckBox("Preserve resolution");
        context.bind(Model.PRESERVE_RESOLUTION, preserveResolutionCheckBox);
        crsForm.addPropertyChangeListener("collocate", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final boolean collocate = (Boolean) evt.getNewValue();
                reprojectionlContainer.setValue(Model.PRESERVE_RESOLUTION,
                                                collocate || reprojectionModel.preserveResolution);
                preserveResolutionCheckBox.setEnabled(!collocate);
            }
        });
        outputSettingsPanel.add(preserveResolutionCheckBox);

        JCheckBox includeTPcheck = new JCheckBox("Reproject tie-point grids", true);
        context.bind(Model.REPROJ_TIEPOINTS, includeTPcheck);
        outputSettingsPanel.add(includeTPcheck);

        outputParamButton = new JButton("Output Parameter...");
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

        reprojectionlContainer.addPropertyChangeListener(Model.PRESERVE_RESOLUTION, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateOutputParameterState();
            }
        });

        return outputSettingsPanel;
    }
    
    private void updateOutputParameterState() {
        outputParamButton.setEnabled(!reprojectionModel.preserveResolution && (crs != null));
    }

    private JPanel createSourceProductPanel() {
        final JPanel panel = sourceProductSelector.createDefaultPanel();
        sourceProductSelector.getProductNameLabel().setText("Name:");
        sourceProductSelector.getProductNameComboBox().setPrototypeDisplayValue(
                "MER_RR__1PPBCM20030730_071000_000003972018_00321_07389_0000.N1");
        sourceProductSelector.addSelectionChangeListener(new SelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                Product sourceProduct = getSourceProduct();
                updateTargetProductName(sourceProduct);
                GeoPos centerGeoPos = null;
                if (sourceProduct != null) {
                    centerGeoPos = ProductUtils.getCenterGeoPos(sourceProduct);
                }
                infoForm.setCenterPos(centerGeoPos);
                try {
                    CoordinateReferenceSystem crs = getSelectedCrs();
                    OutputGeometryFormModel formModel = new OutputGeometryFormModel(sourceProduct, crs);
                    outputParameterContainer = formModel.getValueContainer();
                    updateProductSize();
                } catch (FactoryException ignore) {
                }
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
        public void actionPerformed(ActionEvent e) {
            try {
                final Product sourceProduct = getSourceProduct();
                if (sourceProduct == null) {
                    showWarningMessage("Please select a product to project.\n");
                    return;
                }
                final CoordinateReferenceSystem crs = crsForm.getCrs(sourceProduct);
                if (crs == null) {
                    showWarningMessage("Please specify a 'Coordinate Reference System' first.\n");
                    return;
                }
                final OutputGeometryFormModel formModel = new OutputGeometryFormModel(sourceProduct, crs);
                final OutputGeometryForm form = new OutputGeometryForm(formModel);
                final ModalDialog modalDialog = new ModalDialog(appContext.getApplicationWindow(),
                                                                "Output Parameters",
                                                                ModalDialog.ID_OK_CANCEL, null);
                modalDialog.setContent(form);
                if (modalDialog.show() == ModalDialog.ID_OK) {
                    outputParameterContainer = formModel.getValueContainer();
                    updateProductSize();
                }
            } catch (Exception fe) {
                appContext.handleError("Could not create a 'Coordinate Reference System'.\n" +
                                       fe.getMessage(), fe);
            }
        }
    }
    
    private void showWarningMessage(String message) {
        JOptionPane.showMessageDialog(getParent(), message, "Reprojection", JOptionPane.WARNING_MESSAGE);
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
            if (geoCoding == null) {
                return false;
            }
            return geoCoding.canGetGeoPos() && geoCoding.canGetPixelPos();
        }
    }
}
