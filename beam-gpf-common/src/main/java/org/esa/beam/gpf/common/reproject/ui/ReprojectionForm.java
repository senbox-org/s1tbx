package org.esa.beam.gpf.common.reproject.ui;

import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.SelectionChangeListener;
import org.esa.beam.framework.ui.application.SelectionChangeEvent;
import org.esa.beam.framework.datamodel.Product;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.util.List;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValidationException;

/**
 * User: Marco
 * Date: 16.08.2009
 */
public class ReprojectionForm extends JPanel {

    private ReprojectionFormModel model;
    private ValueContainer valueContainer;

    private ProjectedCrsSelectionFormModel crsSelectionModel;
    private GridDefinitionFormModel gridDefinitionFormModel;
    private SourceProductSelector sourceProductSelector;
    private TargetProductSelector targetProductSelector;

    public ReprojectionForm(final ReprojectionFormModel model,
                            TargetProductSelector targetProductSelector,
                            AppContext appContext) throws FactoryException {
        this.model = model;
        valueContainer = ValueContainer.createObjectBacked(model);
        this.targetProductSelector = targetProductSelector;
        sourceProductSelector = new SourceProductSelector(appContext, "Source Product:");

        final List<CrsInfo> crsList = CrsInfo.generateSupportedCRSList();
        crsSelectionModel = new ProjectedCrsSelectionFormModel(new CrsInfoListModel(crsList));

        createUI();
        bindUI();
    }

    private void createUI() throws FactoryException {
        final ProjectedCrsSelectionForm crsSelectionForm = new ProjectedCrsSelectionForm(crsSelectionModel);
        crsSelectionForm.setBorder(BorderFactory.createTitledBorder("Target CRS"));

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
        add(crsSelectionForm);
        add(gridDefinitionForm);

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
        updateTargetProductName(sourceProductSelector.getSelectedProduct());
    }

    public void prepareHide() {
        sourceProductSelector.releaseProducts();
    }

    private JPanel createSourceProductPanel() {
        final JPanel panel = new JPanel(new BorderLayout(3, 3));
        sourceProductSelector.getProductNameComboBox().setPrototypeDisplayValue(
                "MER_RR__1PPBCM20030730_071000_000003972018_00321_07389_0000.N1");
        panel.add(sourceProductSelector.getProductNameComboBox(), BorderLayout.CENTER);
        panel.add(sourceProductSelector.getProductFileChooserButton(), BorderLayout.EAST);
        panel.setBorder(BorderFactory.createTitledBorder("Source Product"));
        sourceProductSelector.addSelectionChangeListener(new SelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                updateTargetProductName(sourceProductSelector.getSelectedProduct());
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
}
