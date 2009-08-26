package org.esa.beam.gpf.common.reproject.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.SelectionChangeEvent;
import org.esa.beam.framework.ui.application.SelectionChangeListener;
import org.esa.beam.gpf.common.reproject.BeamGridGeometry;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Marco
 * Date: 16.08.2009
 */
public class ReprojectionForm extends JTabbedPane {

    private static final String[] RESAMPLING_IDENTIFIER = {"Nearest", "Bilinear", "Bicubic"};

    private final AppContext appContext;

    private CrsInfoListModel crsListModel;
    private SourceProductSelector sourceProductSelector;
    private TargetProductSelector targetProductSelector;
    private SourceProductSelector collocateProductSelector;
    private CrsSelectionForm crsSelectionForm;
    private JRadioButton collocateRadioButton;
    private String resamplingName;
    private Product sourceProduct;
    private JComboBox resampleComboBox;
    private JCheckBox includeTPcheck;
    private String crsCode;
    private Product collocationProduct;

    public ReprojectionForm(TargetProductSelector targetProductSelector, AppContext appContext) {
        this.appContext = appContext;
        this.targetProductSelector = targetProductSelector;
        sourceProductSelector = new SourceProductSelector(appContext, "Source Product:");
        resamplingName = "Nearest";
        crsListModel = new CrsInfoListModel(CrsInfo.generateCRSList());
        createUI();
        updateUIState();
    }

    public BeamGridGeometry getTargetGeometry() {
        if (!collocateRadioButton.isSelected()) {
            return null;
        }
        Product colloProduct = collocateProductSelector.getSelectedProduct();
        if (colloProduct == null) {
            return null;
        }
        GeoCoding geoCoding = colloProduct.getGeoCoding();
        AffineTransform i2m = (AffineTransform) geoCoding.getImageToModelTransform().clone();
        Rectangle bounds = new Rectangle(colloProduct.getSceneRasterWidth(), colloProduct.getSceneRasterHeight());
        CoordinateReferenceSystem modelCRS = geoCoding.getModelCRS();
        String modelWkt = modelCRS.toString();
        try {
            CoordinateReferenceSystem modelCrsClone = CRS.parseWKT(modelWkt);
            return new BeamGridGeometry(i2m, bounds, modelCrsClone);
        } catch (FactoryException ignored) {
            return null;
        }
    }

    public Map<String, Object> getParameterMap() {
        Map<String, Object> parameterMap = new HashMap<String, Object>(5);
        parameterMap.put("resamplingName", resamplingName);
        parameterMap.put("includeTiePointGrids", includeTPcheck.isSelected());
        if(!collocateRadioButton.isSelected()) {
            parameterMap.put("crsCode", crsCode);
        }
        return parameterMap;
    }

    public Map<String, Product> getProductMap() {
        final Map<String, Product> productMap = new HashMap<String, Product>(5);
        productMap.put("source", sourceProduct);
        if(collocateRadioButton.isSelected()) {
            productMap.put("collocate", collocationProduct);
        }
        return productMap;
    }


    private void createUI() {
        addTab("I/O Parameter", createIOTab());
        addTab("Projection Parameter", createProjectionTab());
    }

    private JPanel createProjectionTab() {

        final JPanel projPanel = new JPanel();
        projPanel.setLayout(new BoxLayout(projPanel, BoxLayout.Y_AXIS));
        projPanel.add(createProjectionPanel());
//        projPanel.add(createGridDifinitionForm());
        return projPanel;
    }

    // currently not displaying this form
//    private GridDefinitionForm createGridDifinitionForm() throws FactoryException {
//        final Rectangle sourceDimension = new Rectangle(100, 200);
//        final CoordinateReferenceSystem sourceCrs = DefaultGeographicCRS.WGS84;
//        final CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:32632");
//        final String unit = targetCrs.getCoordinateSystem().getAxis(0).getUnit().toString();
//        GridDefinitionFormModel formModel = new GridDefinitionFormModel(sourceDimension, sourceCrs, targetCrs, unit);
//        final GridDefinitionForm gridDefinitionForm = new GridDefinitionForm(formModel);
//        gridDefinitionForm.setBorder(BorderFactory.createTitledBorder("Target Grid"));
//        return gridDefinitionForm;
//    }

    private JPanel createIOTab() {
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

    private JPanel createProjectionPanel() {
        collocateProductSelector = new SourceProductSelector(appContext, "Product:");
        collocateProductSelector.setProductFilter(new CollocateProductFilter());
        collocateProductSelector.addSelectionChangeListener(new SelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                updateUIState();
            }
        });
        crsSelectionForm = createCrsSelectionForm();
        final ButtonGroup group = new ButtonGroup();
        collocateRadioButton = new JRadioButton("Collocate with Product");

        collocateRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateUIState();
            }
        });
        JRadioButton projectionRadioButton = new JRadioButton("Define Projection", true);
        projectionRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateUIState();
            }
        });
        
        group.add(collocateRadioButton);
        group.add(projectionRadioButton);

        resampleComboBox = new JComboBox(RESAMPLING_IDENTIFIER);
        resampleComboBox.setPrototypeDisplayValue(RESAMPLING_IDENTIFIER[0]);
        resampleComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateUIState();
            }
        });
        final JPanel settingsPanel = new JPanel(new BorderLayout(3, 3));
        final JPanel resamplePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
        resamplePanel.add(new JLabel("Resampling Method:"));
        resamplePanel.add(resampleComboBox);
        includeTPcheck = new JCheckBox("Include Tie Point Grids", true);
        settingsPanel.add(resamplePanel, BorderLayout.NORTH);
        settingsPanel.add(includeTPcheck, BorderLayout.SOUTH);

        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTablePadding(3, 3);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setCellColspan(0, 0, 2);
        tableLayout.setCellColspan(1, 0, 2);
        tableLayout.setRowWeightY(1, 1.0);
        tableLayout.setCellPadding(1, 0, new Insets(3, 15, 3, 3));
        tableLayout.setRowPadding(1, new Insets(3, 3, 15, 3));
        tableLayout.setCellColspan(2, 0, 2);
        tableLayout.setCellPadding(3, 0, new Insets(3, 15, 3, 3));
        tableLayout.setCellWeightX(3, 1, 0.0);
        tableLayout.setCellColspan(4, 0, 2);

        final JPanel panel = new JPanel(tableLayout);
        panel.setBorder(BorderFactory.createTitledBorder("Projection"));
        // row 0
        panel.add(projectionRadioButton);                                       // col 0
        // row 1
        panel.add(crsSelectionForm);
        // row 2
        panel.add(collocateRadioButton);                                        // col 0
        // row 3
        panel.add(collocateProductSelector.getProductNameComboBox());           // col 0
        panel.add(collocateProductSelector.getProductFileChooserButton());      // col 1
        // row 4
        panel.add(settingsPanel);
        return panel;
    }

    private void updateUIState() {
        final boolean collocate = collocateRadioButton.isSelected();
        collocateProductSelector.getProductNameComboBox().setEnabled(collocate);
        collocateProductSelector.getProductFileChooserButton().setEnabled(collocate);
        crsSelectionForm.setFormEnabled(!collocate);
        collocationProduct = collocate ? collocateProductSelector.getSelectedProduct() : null;

        resamplingName = resampleComboBox.getSelectedItem().toString();
    }

    private CrsSelectionForm createCrsSelectionForm() {
        final CrsSelectionForm crsForm = new CrsSelectionForm(crsListModel);
        crsForm.addPropertyChangeListener(CrsSelectionForm.PROPERTY_SELECTED_CRS_CODE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final Object selectedObject = evt.getNewValue();
                if (selectedObject instanceof String) {
                    crsCode = (String) selectedObject;
                }
            }
        });
        return crsForm;
    }

    public void prepareShow() {
        sourceProductSelector.initProducts();
        collocateProductSelector.initProducts();
        updateUIState();
    }

    public void prepareHide() {
        sourceProductSelector.releaseProducts();
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
                crsSelectionForm.setSelectedProduct(sourceProduct);
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
            final boolean sameProduct = sourceProductSelector.getSelectedProduct() == product;
            final boolean hasGeoCoding = product.getGeoCoding() != null;
            final boolean geoCodingUsable = product.getGeoCoding().canGetGeoPos() && product.getGeoCoding().canGetPixelPos();
            return !sameProduct && hasGeoCoding && geoCodingUsable;
        }
    }
}
