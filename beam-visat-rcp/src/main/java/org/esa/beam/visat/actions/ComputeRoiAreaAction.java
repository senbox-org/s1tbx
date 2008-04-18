/*
 * $Id: ComputeRoiAreaAction.java,v 1.1 2007/04/19 10:16:12 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.actions;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.util.math.RsMathUtils;
import org.esa.beam.visat.VisatApp;

import javax.media.jai.ROI;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.io.IOException;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision: 1.1 $ $Date: 2007/04/19 10:16:12 $
 */
public class ComputeRoiAreaAction extends ExecCommand {

    private final static String DIALOG_TITLE = "Compute ROI Area"; /*I18N*/

    @Override
    public void actionPerformed(CommandEvent event) {
        computeROIArea();
    }

    @Override
    public void updateState(final CommandEvent event) {
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        boolean enabled = false;

        if (view != null) {
            final RasterDataNode raster = view.getRaster();
            if (raster != null) {
                enabled = raster.getROIDefinition() != null;
            }
        }
        setEnabled(enabled);
    }

    /**
     * Performs the actual "Export ROI Pixels" command.
     */
    private void computeROIArea() {
        final String errMsgBase = "Failed to compute ROI area:\n";

        // Get selected VISAT view showing a product's band
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view == null) {
            VisatApp.getApp().showErrorDialog(DIALOG_TITLE, errMsgBase + "No view.");
            return;
        }

        // Get the current raster data node (band or tie-point grid)
        final RasterDataNode raster = view.getRaster();
        assert raster != null;

        // Get the ROI of the displayed raster data node
        final ROI roi;
        ProgressMonitor pm = new DialogProgressMonitor(VisatApp.getApp().getMainFrame(), "Computing ROI area",
                                                       Dialog.ModalityType.APPLICATION_MODAL);
        pm.beginTask("Computing ROI area...", 2);
        double r1;
        double roiArea;
        double pixelAreaMin;
        double pixelAreaMax;
        int numPixels;
        try {
            try {
                roi = raster.createROI(SubProgressMonitor.create(pm, 1));
            } catch (IOException e) {
                VisatApp.getApp().showErrorDialog(DIALOG_TITLE,
                                                  errMsgBase + "An I/O error occured:\n" + e.getMessage());
                return;
            }
            if (roi == null) {
                VisatApp.getApp().showErrorDialog(DIALOG_TITLE, errMsgBase + "No ROI defined.");
                return;
            }

            // Get the current product's geo-coding
            final GeoCoding geoCoding = raster.getGeoCoding();
            if (geoCoding == null) {
                VisatApp.getApp().showErrorDialog(DIALOG_TITLE, errMsgBase + "Product is not geo-coded.");
                return;
            }

            final int w = raster.getSceneRasterWidth();
            final int h = raster.getSceneRasterHeight();

            final PixelPos[] pixelPoints = new PixelPos[5];
            final GeoPos[] geoPoints = new GeoPos[5];
            for (int i = 0; i < geoPoints.length; i++) {
                pixelPoints[i] = new PixelPos();
                geoPoints[i] = new GeoPos();
            }

            float deltaLon;
            float deltaLat;
            double a, b;
            r1 = RsMathUtils.MEAN_EARTH_RADIUS / 1000.0;
            double r2;
            roiArea = 0;
            double pixelArea = 0;
            pixelAreaMin = +Double.MAX_VALUE;
            pixelAreaMax = -Double.MAX_VALUE;
            numPixels = 0;

            ProgressMonitor subPm = SubProgressMonitor.create(pm, 1);
            subPm.beginTask("Computing ROI area...", h);
            try {
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        if (roi.contains(x, y)) {
                            // 0: pixel center point
                            pixelPoints[0].setLocation(x + 0.5f, y + 0.5f);
                            // 1 --> 2 : parallel (geogr. hor. line) crossing pixel center point
                            pixelPoints[1].setLocation(x + 0.0f, y + 0.5f);
                            pixelPoints[2].setLocation(x + 1.0f, y + 0.5f);
                            // 3 --> 4 : meridian (geogr. ver. line) crossing pixel center point
                            pixelPoints[3].setLocation(x + 0.5f, y + 0.0f);
                            pixelPoints[4].setLocation(x + 0.5f, y + 1.0f);

                            for (int i = 0; i < geoPoints.length; i++) {
                                geoCoding.getGeoPos(pixelPoints[i], geoPoints[i]);
                            }
                            deltaLon = Math.abs(geoPoints[2].getLon() - geoPoints[1].getLon());
                            deltaLat = Math.abs(geoPoints[4].getLat() - geoPoints[3].getLat());
                            r2 = r1 * Math.cos(geoPoints[0].getLat() * MathUtils.DTOR);
                            a = r2 * deltaLon * MathUtils.DTOR;
                            b = r1 * deltaLat * MathUtils.DTOR;
                            pixelArea = a * b;
                            pixelAreaMin = Math.min(pixelAreaMin, pixelArea);
                            pixelAreaMax = Math.max(pixelAreaMax, pixelArea);
                            roiArea += a * b;
                            numPixels++;
                        }
                    }
                    pm.worked(1);
                }
            } finally {
                subPm.done();
            }
        } finally {
            pm.done();
        }

        if (numPixels == 0) {
            VisatApp.getApp().showErrorDialog(DIALOG_TITLE, errMsgBase + "ROI is empty.");
        } else {
            showResults(roiArea, pixelAreaMin, pixelAreaMax, numPixels, r1);
        }
    }

    private void showResults(double roiArea, double pixelAreaMin, double pixelAreaMax, int numPixels,
                             double earthRadius) {
        final double roundFactor = 10000.;
        final double roiAreaR = MathUtils.round(roiArea, roundFactor);
        final double meanPixelAreaR = MathUtils.round(roiArea / numPixels, roundFactor);
        final double pixelAreaMinR = MathUtils.round(pixelAreaMin, roundFactor);
        final double pixelAreaMaxR = MathUtils.round(pixelAreaMax, roundFactor);

        final JPanel content = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets.right = 4;
        gbc.gridy = 0;
        gbc.weightx = 0;

        gbc.insets.top = 2;
        addField(content, gbc, "Number of ROI pixels: ", " " + numPixels, "");
        addField(content, gbc, "ROI area: ", "  " + roiAreaR, "km^2");
        addField(content, gbc, "Mean pixel area: ", "  " + meanPixelAreaR, "km^2");
        addField(content, gbc, "Minimum pixel area: ", "  " + pixelAreaMinR, "km^2");
        addField(content, gbc, "Maximum pixel area:  ", "  " + pixelAreaMaxR, "km^2");
        gbc.insets.top = 8;
        addField(content, gbc, "Mean earth radius:   ", "  " + earthRadius, "km");
        final ModalDialog dialog = new ModalDialog(VisatApp.getApp().getMainFrame(),
                                                   VisatApp.getApp().getAppName() + " - " + DIALOG_TITLE,
                                                   content,
                                                   ModalDialog.ID_OK | ModalDialog.ID_HELP,
                                                   getHelpId());
        dialog.show();

    }

    private static void addField(final JPanel content, final GridBagConstraints gbc,
                                 final String text, final String value,
                                 final String unit) {
        content.add(new JLabel(text), gbc);
        gbc.weightx = 1;
        content.add(createTextField(value), gbc);
        gbc.weightx = 0;
        content.add(new JLabel(unit), gbc);
        gbc.gridy++;
    }

    private static JTextField createTextField(final String value) {
        JTextField field = new JTextField(value);
        field.setEditable(false);
        field.setHorizontalAlignment(JTextField.RIGHT);
        return field;
    }

}
