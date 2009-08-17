package org.esa.beam.gpf.common.reproject.ui;

import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.ui.AppContext;
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

/**
 * User: Marco
 * Date: 16.08.2009
 */
public class ReprojectionForm extends JPanel {

    private ReprojectionFormModel model;
    private ProjectedCrsSelectionFormModel crsSelectionModel;
    private GridDefinitionFormModel gridDefinitionFormModel;

    private SourceProductSelector sourceProductSelector;
    private TargetProductSelector targetProductSelector;

    public ReprojectionForm(final ReprojectionFormModel model,
                            TargetProductSelector targetProductSelector,
                            AppContext appContext) throws FactoryException {
        this.model = model;
        this.targetProductSelector = targetProductSelector;
        sourceProductSelector = new SourceProductSelector(appContext, "Source Product:");

        final List<CrsInfo> crsList = CrsInfo.generateSupportedCRSList();
        crsSelectionModel = new ProjectedCrsSelectionFormModel(new CrsInfoListModel(crsList));

        createUI();

    }

    private void createUI() throws FactoryException {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(createSourceProductPanel());
        add(targetProductSelector.createDefaultPanel());
        final ProjectedCrsSelectionForm crsSelectionForm = new ProjectedCrsSelectionForm(crsSelectionModel);
        crsSelectionForm.setBorder(BorderFactory.createTitledBorder("Target CRS"));
        add(crsSelectionForm);
        final Rectangle sourceDimension = new Rectangle(100, 200);
        final CoordinateReferenceSystem sourceCrs = DefaultGeographicCRS.WGS84;
        final CoordinateReferenceSystem crs = CRS.decode("EPSG:32632");
        final String unit = crs.getCoordinateSystem().getAxis(0).getUnit().toString();
        gridDefinitionFormModel = new GridDefinitionFormModel(sourceDimension, sourceCrs, crs, sourceDimension.width, sourceDimension.height,
                                    1, 1, unit);
        final GridDefinitionForm gridDefinitionForm = new GridDefinitionForm(gridDefinitionFormModel);
        gridDefinitionForm.setBorder(BorderFactory.createTitledBorder("Target Grid"));
        add(gridDefinitionForm);

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
