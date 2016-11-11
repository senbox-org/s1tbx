/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.sar.gpf.ui.geometric;

import com.bc.ceres.swing.TableLayout;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.crs.CrsSelectionPanel;
import org.esa.snap.ui.crs.CustomCrsForm;
import org.esa.snap.ui.crs.PredefinedCrsForm;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JPanel;
import java.awt.Insets;

/**
 * Helper for adding map projection components into an operator UI
 */
public class MapProjectionHandler {

    private final CrsSelectionPanel crsSelectionPanel;
    private CoordinateReferenceSystem crs;

    public MapProjectionHandler() {
        crsSelectionPanel = createCRSPanel();
    }

    private static CrsSelectionPanel createCRSPanel() {
        final AppContext appContext = SnapApp.getDefault().getAppContext();
        final CustomCrsForm customCrsForm = new CustomCrsForm(appContext);
        final PredefinedCrsForm predefinedCrsForm = new PredefinedCrsForm(appContext);

        return new CrsSelectionPanel(customCrsForm, predefinedCrsForm);
    }

    public void initParameters(final String mapProjection, final Product[] sourceProducts) {
        crs = getCRS(mapProjection, sourceProducts);
    }

    public CoordinateReferenceSystem getCRS() {
        return crs;
    }

    public String getCRSName() {
        if (crs != null) {
            return crs.getName().getCode();
        }
        return "";
    }

    private CoordinateReferenceSystem getCRS(final String mapProjection, final Product[] sourceProducts) {
        final CoordinateReferenceSystem theCRS = parseCRS(mapProjection, sourceProducts);
        if (theCRS == null)
            return getCRSFromDialog(sourceProducts);
        return theCRS;
    }

    static CoordinateReferenceSystem parseCRS(String mapProjection, final Product[] sourceProducts) {
        try {
            if (mapProjection != null && !mapProjection.isEmpty())
                return CRS.parseWKT(mapProjection);
        } catch (Exception e) {
            try {
                // prefix with EPSG, if there are only numbers
                if (mapProjection.matches("[0-9]*")) {
                    mapProjection = "EPSG:" + mapProjection;
                }
                // append center coordinates for AUTO code
                if (mapProjection.matches("AUTO:[0-9]*")) {
                    final GeoPos centerGeoPos;
                    if (sourceProducts == null || sourceProducts[0] == null)
                        centerGeoPos = new GeoPos(0, 0);
                    else
                        centerGeoPos = ProductUtils.getCenterGeoPos(sourceProducts[0]);
                    mapProjection = String.format("%s,%s,%s", mapProjection, centerGeoPos.lon, centerGeoPos.lat);
                }
                // force longitude==x-axis and latitude==y-axis
                return CRS.decode(mapProjection, true);
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
        return null;
    }

    private CoordinateReferenceSystem getCRSFromDialog(final Product[] sourceProducts) {
        try {
            if (sourceProducts == null || sourceProducts[0] == null)
                return crsSelectionPanel.getCrs(new GeoPos(0, 0));
            return crsSelectionPanel.getCrs(ProductUtils.getCenterGeoPos(sourceProducts[0]));
        } catch (Exception e) {
            Dialogs.showError("Unable to create coodinate reference system");
        }
        return null;
    }

    public void promptForFeatureCrs(final Product[] sourceProducts) {

        final ModalDialog dialog = new ModalDialog(null,
                "Map Projection",
                ModalDialog.ID_OK_CANCEL_HELP, "mapProjection");

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setCellPadding(0, 0, new Insets(4, 10, 4, 4));
        final JPanel contentPanel = new JPanel(tableLayout);
        contentPanel.add(crsSelectionPanel);
        dialog.setContent(contentPanel);
        if (dialog.show() == ModalDialog.ID_OK) {
            crs = getCRSFromDialog(sourceProducts);
        }
    }
}
