package org.esa.beam.visat.actions;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.crs.CrsSelectionPanel;
import org.esa.beam.framework.ui.crs.CustomCrsForm;
import org.esa.beam.framework.ui.crs.PredefinedCrsForm;
import org.esa.beam.framework.ui.crs.ProductCrsForm;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.VisatApp;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for selection of a feature CRS in CSV import
 *
 * @author olafd
 */
public class FeatureCrsDialog extends ModalDialog {

    private CrsSelectionPanel crsSelectionPanel;
    private Product product;
    private String title;
    VisatApp visatApp;

    public FeatureCrsDialog(VisatApp visatApp, Product product, String title) {
        super(visatApp.getApplicationWindow(), title, ModalDialog.ID_OK_CANCEL_HELP, "featureCrsDialog");
        this.product = product;
        this.visatApp = visatApp;
        this.title = title;
        createUI(visatApp);
    }

    private void createUI(VisatApp visatApp) {
        final ProductCrsForm productCrsForm = new ProductCrsForm(visatApp, product);
        final CustomCrsForm customCrsForm = new CustomCrsForm(visatApp);
        final PredefinedCrsForm predefinedCrsForm = new PredefinedCrsForm(visatApp);

        crsSelectionPanel = new CrsSelectionPanel(productCrsForm, customCrsForm, predefinedCrsForm);
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setCellPadding(0, 0, new Insets(4, 10, 4, 4));
        final JPanel contentPanel = new JPanel(tableLayout);
        final JLabel label = new JLabel();
        label.setText("<html><b>" +
                              "These vector data does not define a coordinate reference system (CRS).<br/>" +
                              "Please specify a CRS so that coordinates can interpreted correctly.</b>");

        contentPanel.add(label);
        contentPanel.add(crsSelectionPanel);
        setContent(contentPanel);
    }

    public CoordinateReferenceSystem getFeatureCrs() {
        CoordinateReferenceSystem crs = null;
        try {
            crs = crsSelectionPanel.getCrs(ProductUtils.getCenterGeoPos(product));
        } catch (FactoryException e) {
            visatApp.showErrorDialog(title,
                                     "Can not create Coordinate Reference System.\n" + e.getMessage());
        }
        return crs;
    }

    @Override
    protected void onOK() {
        super.onOK();
        getParent().setVisible(true);    // todo: Visat main window disappears otherwise, find better solution
    }

    @Override
    protected void onCancel() {
        super.onCancel();
        getParent().setVisible(true);   // todo: Visat main window disappears otherwise, find better solution
    }

}
