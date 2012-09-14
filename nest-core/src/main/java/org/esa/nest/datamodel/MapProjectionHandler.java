package org.esa.nest.datamodel;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.crs.CrsSelectionPanel;
import org.esa.beam.framework.ui.crs.CustomCrsForm;
import org.esa.beam.framework.ui.crs.PredefinedCrsForm;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.VisatApp;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.*;
import java.awt.*;

/**
    Helper for adding map projection components into an operator UI
 */
public class MapProjectionHandler {

    private final CrsSelectionPanel crsSelectionPanel;
    private CoordinateReferenceSystem crs;

    public MapProjectionHandler() {
        crsSelectionPanel = createCRSPanel();
    }

    private static CrsSelectionPanel createCRSPanel() {
        final VisatApp visatApp = VisatApp.getApp();
        final CustomCrsForm customCrsForm = new CustomCrsForm(visatApp);
        final PredefinedCrsForm predefinedCrsForm = new PredefinedCrsForm(visatApp);

        return new CrsSelectionPanel(customCrsForm, predefinedCrsForm);
    }

    public void initParameters(final String mapProjection, final Product[] sourceProducts) {
        crs = getCRS(mapProjection, sourceProducts);
    }

    public CoordinateReferenceSystem getCRS() {
        return crs;
    }

    public String getCRSName() {
        if(crs != null) {
            return crs.getName().getCode();
        }
        return "";
    }

    private CoordinateReferenceSystem getCRS(final String mapProjection, final Product[] sourceProducts) {
        final CoordinateReferenceSystem theCRS = parseCRS(mapProjection);
        if(theCRS == null)
            return getCRSFromDialog(sourceProducts);
        return theCRS;
    }

    static CoordinateReferenceSystem parseCRS(final String mapProjection) {
        try {
            if(mapProjection != null && !mapProjection.isEmpty())
                return CRS.parseWKT(mapProjection);
        } catch (Exception e) {
            try {
                return CRS.decode(mapProjection, true);
            } catch(Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
        return null;
    }

    private CoordinateReferenceSystem getCRSFromDialog(final Product[] sourceProducts) {
        try {
            if(sourceProducts == null || sourceProducts[0] == null)
                return crsSelectionPanel.getCrs(new GeoPos(0,0));
            return crsSelectionPanel.getCrs(ProductUtils.getCenterGeoPos(sourceProducts[0]));
        } catch(Exception e) {
            VisatApp.getApp().showErrorDialog("Unable to create coodinate reference system");
        }
        return null;
    }

    public void promptForFeatureCrs(final Product[] sourceProducts) {

        final ModalDialog dialog = new ModalDialog(VisatApp.getApp().getApplicationWindow(),
                                                   "Map Projection",
                                                   ModalDialog.ID_OK_CANCEL_HELP, "mapProjection");

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(4,4);
        tableLayout.setCellPadding(0, 0, new Insets(4, 10, 4, 4));
        final JPanel contentPanel = new JPanel(tableLayout);
        contentPanel.add(crsSelectionPanel);
        dialog.setContent(contentPanel);
        if (dialog.show() == ModalDialog.ID_OK) {
            crs = getCRSFromDialog(sourceProducts);
        }
    }

    public static CoordinateReferenceSystem getCRS(String mapProjection) throws Exception {
        try {
            if(mapProjection == null || mapProjection.isEmpty())
                mapProjection = "WGS84(DD)";
            return CRS.parseWKT(mapProjection);
        } catch (Exception e) {
            return CRS.decode(mapProjection, true);
        }
    }
}
