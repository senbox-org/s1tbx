package org.esa.beam.gpf.common.reproject.ui;

import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.datamodel.Product;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.util.List;

/**
 * User: Marco
 * Date: 16.08.2009
 */
public class ReprojectionForm extends JPanel {
    private final ReprojectionFormModel model;
    private final TargetProductSelector targetProductSelector;

    private SourceProductSelector sourceProductSelector;
    private ProjectedCrsSelectionFormModel crsSelectionModel;

    public ReprojectionForm(final ReprojectionFormModel model,
                            TargetProductSelector targetProductSelector,
                            AppContext appContext) {
        this.model = model;
        this.targetProductSelector = targetProductSelector;
        sourceProductSelector = new SourceProductSelector(appContext, "Source Product:");

        final List<CrsInfo> crsList = CrsInfo.generateSupportedCRSList();
        crsSelectionModel = new ProjectedCrsSelectionFormModel(new CrsInfoListModel(crsList));

        createUI();

    }

    private void createUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(createSourceProductPanel());
        add(targetProductSelector.createDefaultPanel());
        final ProjectedCrsSelectionForm crsSelectionForm = new ProjectedCrsSelectionForm(crsSelectionModel);
        crsSelectionForm.setBorder(BorderFactory.createTitledBorder("Target CRS"));
        add(crsSelectionForm);
        final Product sourceProduct = sourceProductSelector.getSelectedProduct();
        final Rectangle sourceDimension = new Rectangle(sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
        final CoordinateReferenceSystem sourceCrs = sourceProduct.getGeoCoding().getBaseCRS();
        final ProjectedCRS crs = crsSelectionModel.getSelectedCrs();
        final String unit = crs.getCoordinateSystem().getAxis(0).getUnit().toString();
        new GridDefinitionFormModel(sourceDimension, sourceCrs, crs, sourceDimension.width, sourceDimension.height,
                                    1, 1, unit);
    }


    private JPanel createSourceProductPanel() {
        final JPanel panel = new JPanel(new BorderLayout(3, 3));
        sourceProductSelector.getProductNameComboBox().setPrototypeDisplayValue(
                "MER_RR__1PPBCM20030730_071000_000003972018_00321_07389_0000.N1");
        panel.add(sourceProductSelector.getProductNameComboBox(), BorderLayout.CENTER);
        panel.add(sourceProductSelector.getProductFileChooserButton(), BorderLayout.EAST);

        panel.setBorder(BorderFactory.createTitledBorder("Source Product"));
        return panel;
    }

    public void prepareShow() {
        sourceProductSelector.initProducts();
        if (sourceProductSelector.getProductCount() > 0) {
            sourceProductSelector.setSelectedIndex(0);
        }
    }

    public void prepareHide() {
        sourceProductSelector.releaseProducts();
    }

}
