package org.esa.beam.gpf.common.reproject.ui;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.DemSelector;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.application.SelectionChangeEvent;
import org.esa.beam.framework.ui.application.SelectionChangeListener;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.OperationMethod;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Marco
 * Date: 16.08.2009
 */
public class ReprojectionForm extends JTabbedPane {

    private static final String[] RESAMPLING_IDENTIFIER = {"Nearest", "Bilinear", "Bicubic"};

    private final boolean orthoMode;
    private final AppContext appContext;

    private SourceProductSelector sourceProductSelector;
    private TargetProductSelector targetProductSelector;
    private SourceProductSelector collocateProductSelector;
    private ProjectionDefinitionForm projDefPanel;
    private JComboBox resampleComboBox;
    private JCheckBox includeTPcheck;
    private Product collocationProduct;
    private ButtonModel projButtonModel;
    private ButtonModel crsButtonModel;
    private ButtonModel collocateButtonModel;
    private JPanel collocationPanel;
    private JTextField crsCodeField;
    private JPanel crsSelectionPanel;

    private Product sourceProduct;
    private CrsInfo selectedCrsInfo;
    private DemSelector demSelector;
    private ValueContainer outputParameterContainer;

    public ReprojectionForm(TargetProductSelector targetProductSelector, boolean orthorectify, AppContext appContext) {
        this.targetProductSelector = targetProductSelector;
        orthoMode = orthorectify;
        this.appContext = appContext;
        sourceProductSelector = new SourceProductSelector(appContext, "Source Product:");
        createUI();
        updateUIState();
    }

    public Map<String, Object> getParameterMap() {
        Map<String, Object> parameterMap = new HashMap<String, Object>(5);
        parameterMap.put("resamplingName", resampleComboBox.getSelectedItem().toString());
        parameterMap.put("includeTiePointGrids", includeTPcheck.isSelected());
        try {
            if (crsButtonModel.isSelected()) {
                final CoordinateReferenceSystem crs = selectedCrsInfo.getCrs(sourceProduct);
                parameterMap.put("wkt", crs.toWKT());
            }
            if (projButtonModel.isSelected()) {
                parameterMap.put("wkt", projDefPanel.getProcjetedCRS().toWKT());
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
            parameterMap.put("noDataValue", outputParameterContainer.getValue("noData"));
        }
        return parameterMap;
    }

    private CoordinateReferenceSystem getTargetCrs() throws FactoryException {
            if (crsButtonModel.isSelected() && selectedCrsInfo != null) {
                return selectedCrsInfo.getCrs(sourceProduct);
            }
            if (projButtonModel.isSelected()) {
                return projDefPanel.getProcjetedCRS();
            }
            if (collocateButtonModel.isSelected() && collocationProduct != null) {
                return collocationProduct.getGeoCoding().getModelCRS();
            }
        return null;
    }

    public Map<String, Product> getProductMap() {
        final Map<String, Product> productMap = new HashMap<String, Product>(5);
        productMap.put("source", sourceProduct);
        if (collocateButtonModel.isSelected()) {
            productMap.put("collocate", collocationProduct);
        }
        return productMap;
    }

    public void prepareShow() {
        sourceProductSelector.initProducts();
        collocateProductSelector.initProducts();
        updateUIState();
    }

    public void prepareHide() {
        sourceProductSelector.releaseProducts();
    }

    private void createUI() {
        addTab("I/O Parameter", createIOPanel());
        addTab("Projection Parameter", createParameterPanel());
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

        parameterPanel.add(createProjectionPanel());
        if (orthoMode) {
            parameterPanel.add(createOrthorectifyPanel());
        }
        parameterPanel.add(createOuputSettingsPanel());
        return parameterPanel;
    }

    private JPanel createProjectionPanel() {
        collocationPanel = createCollocationPanel();
        crsSelectionPanel = createCrsSelectionPanel();
        projDefPanel = createProjectionDefinitionPanel();
        JRadioButton projectionRadioButton = new JRadioButton("Transformation", true);
        projectionRadioButton.addActionListener(new UpdateStateListener());
        JRadioButton crsRadioButton = new JRadioButton("CRS");
        crsRadioButton.addActionListener(new UpdateStateListener());
        JRadioButton collocateRadioButton = new JRadioButton("Collocate");
        collocateRadioButton.addActionListener(new UpdateStateListener());
        projButtonModel = projectionRadioButton.getModel();
        crsButtonModel = crsRadioButton.getModel();
        collocateButtonModel = collocateRadioButton.getModel();

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(projectionRadioButton);
        buttonGroup.add(crsRadioButton);
        buttonGroup.add(collocateRadioButton);

        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setCellColspan(0,0,2);
        tableLayout.setCellColspan(1,0,2);
        tableLayout.setCellWeightX(2,0,0.0);
        tableLayout.setCellWeightX(3,0,0.0);

        final JPanel projectionPanel = new JPanel(tableLayout);
        projectionPanel.setBorder(BorderFactory.createTitledBorder("Projection"));
        projectionPanel.add(projectionRadioButton);
        projectionPanel.add(projDefPanel);
        projectionPanel.add(crsRadioButton);
        projectionPanel.add(crsSelectionPanel);
        projectionPanel.add(collocateRadioButton);
        projectionPanel.add(collocationPanel);
        return projectionPanel;
    }

    private JPanel createOrthorectifyPanel() {
        demSelector = new DemSelector(new ParamChangeListener() {
            @Override
            public void parameterValueChanged(ParamChangeEvent event) {
                updateUIState();
            }
        });
        return demSelector;
    }

    private JPanel createOuputSettingsPanel() {
        // todo - not working well yet (mp, 21102009)
        final JButton outputParamBtn = new JButton("Output Parameter");
        outputParamBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (sourceProduct == null) {
                        appContext.handleError("Please select a product to project.\n", new NullPointerException());
                        return;
                    }
                    final CoordinateReferenceSystem crs = getTargetCrs();
                    if (crs == null) {
                        appContext.handleError("Please specify a target CRS first.\n", new NullPointerException());
                        return;
                    }
                    final OutputSizeFormModel formModel = new OutputSizeFormModel(sourceProduct, crs);
                    final OutputSizeForm form = new OutputSizeForm(formModel);
                    final ModalDialog modalDialog = new ModalDialog(appContext.getApplicationWindow(),
                                                                    "Output Parameters",
                                                                    ModalDialog.ID_OK_CANCEL, null);
                    modalDialog.setContent(form);
                    if (modalDialog.show() == ModalDialog.ID_OK) {
                        outputParameterContainer = formModel.getValueContainer();
                    }
                } catch (Exception fe) {
                    appContext.handleError("Could not create target CRS.\n" +
                                           fe.getMessage(), fe);
                }
            }
        });
        final JPanel resamplePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
        resamplePanel.add(new JLabel("Resampling Method:"));
        resampleComboBox = new JComboBox(RESAMPLING_IDENTIFIER);
        resampleComboBox.setPrototypeDisplayValue(RESAMPLING_IDENTIFIER[0]);
        resamplePanel.add(resampleComboBox);
        includeTPcheck = new JCheckBox("Include Tie-Point Grids", true);

        final TableLayout tableLayout = new TableLayout(3);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(8, 4);
        tableLayout.setTableWeightX(1.0);

        final JPanel outputSettingsPanel = new JPanel(tableLayout);
        outputSettingsPanel.setBorder(BorderFactory.createTitledBorder("Output Settings"));
        outputSettingsPanel.add(outputParamBtn);
        outputSettingsPanel.add(resamplePanel);
        outputSettingsPanel.add(includeTPcheck);
        return outputSettingsPanel;
    }


    private JPanel createCollocationPanel() {
        collocateProductSelector = new SourceProductSelector(appContext, "Product:");
        collocateProductSelector.setProductFilter(new CollocateProductFilter());
        collocateProductSelector.addSelectionChangeListener(new SelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                updateUIState();
            }
        });

        final JPanel panel = new JPanel(new BorderLayout(2, 2));
        panel.add(collocateProductSelector.getProductNameComboBox(), BorderLayout.CENTER);
        panel.add(collocateProductSelector.getProductFileChooserButton(), BorderLayout.EAST);
        panel.addPropertyChangeListener("enabled", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                collocateProductSelector.getProductNameComboBox().setEnabled(panel.isEnabled());
                collocateProductSelector.getProductFileChooserButton().setEnabled(panel.isEnabled());
            }
        });
        return panel;

    }

    private JPanel createCrsSelectionPanel() {
        final TableLayout tableLayout = new TableLayout(2);
        final JPanel panel = new JPanel(tableLayout);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setColumnWeightX(0, 1.0);
        tableLayout.setColumnWeightX(1, 0.0);

        crsCodeField = new JTextField();
        crsCodeField.setEditable(false);
        final JButton crsButton = new JButton("Select CRS");
        crsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final CrsSelectionForm crsForm = new CrsSelectionForm(new CrsInfoListModel(CrsInfo.generateCRSList()));
                final ModalDialog dialog = new ModalDialog(appContext.getApplicationWindow(), "Select CRS", crsForm,
                                                           ModalDialog.ID_OK_CANCEL, null);
                if (dialog.show() == ModalDialog.ID_OK) {
                    selectedCrsInfo = crsForm.getSelectedCrsInfo();
                    crsCodeField.setText(selectedCrsInfo.toString());
                }
            }
        });
        panel.add(crsCodeField);
        panel.add(crsButton);
        panel.addPropertyChangeListener("enabled", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                crsCodeField.setEnabled((Boolean) evt.getNewValue());
                crsButton.setEnabled((Boolean) evt.getNewValue());
            }
        });
        return panel;
    }

    private ProjectionDefinitionForm createProjectionDefinitionPanel() {
        final List<OperationMethod> methods = ProjectionDefinitionForm.createProjectionMethodList();
        final List<GeodeticDatum> datums = ProjectionDefinitionForm.createDatumList();
        return new ProjectionDefinitionForm(appContext.getApplicationWindow(), methods, datums);
    }

    private void updateUIState() {
        boolean collocationEnabled = (collocateProductSelector.getProductCount()>0);
        collocateButtonModel.setEnabled(collocationEnabled);
        final boolean collocate = collocateButtonModel.isSelected();
        collocateProductSelector.getProductNameComboBox().setEnabled(collocate);
        collocateProductSelector.getProductFileChooserButton().setEnabled(collocate);
        collocationProduct = collocate ? collocateProductSelector.getSelectedProduct() : null;
        projDefPanel.setEnabled(projButtonModel.isSelected());
        crsSelectionPanel.setEnabled(crsButtonModel.isSelected());
        collocationPanel.setEnabled(collocateButtonModel.isSelected());
    }


    private JPanel createSourceProductPanel() {
        final JPanel panel = sourceProductSelector.createDefaultPanel();
        sourceProductSelector.getProductNameLabel().setText("Name:");
        sourceProductSelector.getProductNameComboBox().setPrototypeDisplayValue(
                "MER_RR__1PPBCM20030730_071000_000003972018_00321_07389_0000.N1");
        sourceProductSelector.addSelectionChangeListener(new SelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                sourceProduct = sourceProductSelector.getSelectedProduct();
                updateTargetProductName(sourceProduct);
                updateUIState();
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

    private class CollocateProductFilter implements ProductFilter {

        @Override
        public boolean accept(Product product) {
            if (product == null) {
                return false;
            }
            final boolean sameProduct = sourceProductSelector.getSelectedProduct() == product;
            final GeoCoding geoCoding = product.getGeoCoding();
            final boolean hasGeoCoding = geoCoding != null;
            final boolean geoCodingUsable = hasGeoCoding && geoCoding.canGetGeoPos() && geoCoding.canGetPixelPos();
            return !sameProduct && geoCodingUsable;
        }
    }

    private class UpdateStateListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            updateUIState();
        }
    }
}
