package org.esa.beam.gpf.common.reproject.ui;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.SelectionChangeEvent;
import org.esa.beam.framework.ui.application.SelectionChangeListener;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Insets;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * User: Marco
 * Date: 16.08.2009
 */
public class ReprojectionForm extends JPanel {

    private final ReprojectionFormModel model;
    private final AppContext appContext;
    private final ValueContainer valueContainer;

    private ProjectedCrsSelectionFormModel crsSelectionModel;
    private GridDefinitionFormModel gridDefinitionFormModel;
    private SourceProductSelector sourceProductSelector;
    private TargetProductSelector targetProductSelector;
    private SourceProductSelector collocateProductSelector;
    private ProjectedCrsSelectionForm crsSelectionForm;
    private JRadioButton collocateRadioButton;
    private JRadioButton projectionRadioButton;

    public ReprojectionForm(final ReprojectionFormModel model,
                            TargetProductSelector targetProductSelector,
                            AppContext appContext) throws FactoryException {
        this.model = model;
        this.appContext = appContext;
        valueContainer = ValueContainer.createObjectBacked(model);
        this.targetProductSelector = targetProductSelector;
        sourceProductSelector = new SourceProductSelector(appContext, "Source Product:");

        final List<CrsInfo> crsList = CrsInfo.generateSupportedCRSList();
        crsSelectionModel = new ProjectedCrsSelectionFormModel(new CrsInfoListModel(crsList));

        createUI();
        bindUI();
        updateUIState();
    }

    private void createUI() throws FactoryException {
        final Rectangle sourceDimension = new Rectangle(100, 200);
        final CoordinateReferenceSystem sourceCrs = DefaultGeographicCRS.WGS84;
        final CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:32632");
        final String unit = targetCrs.getCoordinateSystem().getAxis(0).getUnit().toString();
        gridDefinitionFormModel = new GridDefinitionFormModel(sourceDimension, sourceCrs, targetCrs, unit);
        final GridDefinitionForm gridDefinitionForm = new GridDefinitionForm(gridDefinitionFormModel);
        gridDefinitionForm.setBorder(BorderFactory.createTitledBorder("Target Grid"));

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(createSourceProductPanel());
        add(targetProductSelector.createDefaultPanel());
        add(createProjectionPanel());
        add(gridDefinitionForm);
    }

    private JPanel createProjectionPanel() {
        collocateProductSelector = new SourceProductSelector(appContext, "Product:");
        collocateProductSelector.setProductFilter(new CollocateProductFilter());
        crsSelectionForm = createCrsSelectionForm();
        final ButtonGroup group = new ButtonGroup();
        collocateRadioButton = new JRadioButton("Collocate with Product");
        projectionRadioButton = new JRadioButton("Define Projection", true);

        collocateRadioButton.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateUIState();
            }
        });
        group.add(collocateRadioButton);
        group.add(projectionRadioButton);
        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTablePadding(3, 3);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setRowWeightY(0, 0.0);
        tableLayout.setCellColspan(0, 0, 2);
        tableLayout.setCellColspan(1, 0, 2);
        tableLayout.setRowWeightY(1, 1.0);
        tableLayout.setCellPadding(1, 0, new Insets(3, 15, 3, 3));
        tableLayout.setRowPadding(1, new Insets(3, 3, 15, 3));
        tableLayout.setRowWeightY(2, 0.0);
        tableLayout.setCellColspan(2, 0, 2);
        tableLayout.setRowWeightY(3, 0.0);
        tableLayout.setCellPadding(3, 0, new Insets(3, 15, 3, 3));
        tableLayout.setCellWeightX(3, 1, 0.0);

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
        return panel;
    }

    private void updateUIState() {
        final boolean collocate = collocateRadioButton.isSelected();
        collocateProductSelector.getProductNameComboBox().setEnabled(collocate);
        collocateProductSelector.getProductFileChooserButton().setEnabled(collocate);
        crsSelectionForm.setFormEnabled(!collocate);
    }

    private ProjectedCrsSelectionForm createCrsSelectionForm() {
        final ProjectedCrsSelectionForm crsForm = new ProjectedCrsSelectionForm(crsSelectionModel);
        crsSelectionModel.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                try {
                    valueContainer.setValue("targetCrs", evt.getNewValue());
                } catch (ValidationException e) {
                    e.printStackTrace();
                }
            }
        });
        return crsForm;
    }

    private void bindUI() {
        final BindingContext context = new BindingContext(valueContainer);
        context.bind("sourceProduct", sourceProductSelector.getProductNameComboBox());
    }

    public void prepareShow() {
        sourceProductSelector.initProducts();
        if (sourceProductSelector.getProductCount() > 0) {
            sourceProductSelector.setSelectedIndex(0);
        }
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
                updateTargetProductName(sourceProductSelector.getSelectedProduct());
                updateUIState();
            }
        });
        return panel;
    }

    private void updateTargetProductName(Product selectedProduct) {
        final TargetProductSelectorModel selectorModel = targetProductSelector.getModel();
        if (selectedProduct != null) {
            selectorModel.setProductName(selectedProduct.getName() + "_reprojected");
        } else if (selectorModel.getProductName() == null) {
            selectorModel.setProductName("reprojected");
        }
    }

    private class CollocateProductFilter implements ProductFilter {

        @Override
            public boolean accept(Product product) {
            return sourceProductSelector.getSelectedProduct() != product;
        }
    }
}
