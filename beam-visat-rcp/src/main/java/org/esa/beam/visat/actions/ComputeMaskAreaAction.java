/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.visat.actions;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.AbstractDialog;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.Debug;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.util.math.RsMathUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;

public class ComputeMaskAreaAction extends ExecCommand {

    private static final String DIALOG_TITLE = "Compute Mask Area"; /*I18N*/

    @Override
    public void actionPerformed(CommandEvent event) {
        computeMaskArea();
    }

    @Override
    public void updateState(final CommandEvent event) {
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        boolean enabled = false;

        if (view != null) {
            final RasterDataNode raster = view.getRaster();
            if (raster != null && raster.getGeoCoding() != null) {
                Product product = raster.getProduct();
                int numMasks = product.getMaskGroup().getNodeCount();
                enabled = numMasks > 0;
            }
        }
        setEnabled(enabled);
    }

    private void computeMaskArea() {
        final String errMsgBase = "Failed to compute Mask area:\n";

        // Get selected VISAT view showing a product's band
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view == null) {
            VisatApp.getApp().showErrorDialog(DIALOG_TITLE, errMsgBase + "No view.");
            return;
        }

        // Get the current raster data node (band or tie-point grid)
        final RasterDataNode raster = view.getRaster();
        assert raster != null;

        final String[] maskNames = raster.getProduct().getMaskGroup().getNodeNames();
        String maskName;
        if (maskNames.length == 1) {
            maskName = maskNames[0];
        } else {
            JPanel panel = new JPanel();
            BoxLayout boxLayout = new BoxLayout(panel, BoxLayout.X_AXIS);
            panel.setLayout(boxLayout);
            panel.add(new JLabel("Select Mask: "));
            JComboBox maskCombo = new JComboBox(maskNames);
            panel.add(maskCombo);
            ModalDialog modalDialog = new ModalDialog(VisatApp.getApp().getApplicationWindow(), DIALOG_TITLE, panel,
                                                      ModalDialog.ID_OK_CANCEL | ModalDialog.ID_HELP, getHelpId());
            if (modalDialog.show() == AbstractDialog.ID_OK) {
                maskName = (String) maskCombo.getSelectedItem();
            } else {
                return;
            }
        }
        final Mask mask = raster.getProduct().getMaskGroup().get(maskName);

        RenderedImage maskImage = mask.getSourceImage();
        if (maskImage == null) {
            VisatApp.getApp().showErrorDialog(DIALOG_TITLE, errMsgBase + "No Mask image available.");
            return;
        }

        final SwingWorker<MaskAreaStatistics, Object> swingWorker = new MaskAreaSwingWorker(mask, errMsgBase);
        swingWorker.execute();
    }


    private static class MaskAreaStatistics {

        private double earthRadius;
        private double maskArea;
        private double pixelAreaMin;
        private double pixelAreaMax;
        private int numPixels;

        private MaskAreaStatistics(double earthRadius) {
            this.earthRadius = earthRadius;
            maskArea = 0.0;
            pixelAreaMax = Double.NEGATIVE_INFINITY;
            pixelAreaMin = Double.POSITIVE_INFINITY;
            numPixels = 0;
        }

        public double getEarthRadius() {
            return earthRadius;
        }

        public double getMaskArea() {
            return maskArea;
        }

        public void setMaskArea(double maskArea) {
            this.maskArea = maskArea;
        }

        public double getPixelAreaMin() {
            return pixelAreaMin;
        }

        public void setPixelAreaMin(double pixelAreaMin) {
            this.pixelAreaMin = pixelAreaMin;
        }

        public double getPixelAreaMax() {
            return pixelAreaMax;
        }

        public void setPixelAreaMax(double pixelAreaMax) {
            this.pixelAreaMax = pixelAreaMax;
        }

        public int getNumPixels() {
            return numPixels;
        }

        public void setNumPixels(int numPixels) {
            this.numPixels = numPixels;
        }
    }

    private class MaskAreaSwingWorker extends SwingWorker<MaskAreaStatistics, Object> {

        private final RasterDataNode mask;
        private final String errMsgBase;

        private MaskAreaSwingWorker(RasterDataNode mask, String errMsgBase) {
            this.mask = mask;
            this.errMsgBase = errMsgBase;
        }

        @Override
        protected MaskAreaStatistics doInBackground() throws Exception {
            ProgressMonitor pm = new DialogProgressMonitor(VisatApp.getApp().getMainFrame(), "Computing Mask area",
                                                           Dialog.ModalityType.APPLICATION_MODAL);
            return computeMaskAreaStatistics(pm);
        }

        private MaskAreaStatistics computeMaskAreaStatistics(ProgressMonitor pm) {
            final MultiLevelImage maskImage = mask.getSourceImage();

            final int minTileX = maskImage.getMinTileX();
            final int minTileY = maskImage.getMinTileY();

            final int numXTiles = maskImage.getNumXTiles();
            final int numYTiles = maskImage.getNumYTiles();

            final int w = mask.getSceneRasterWidth();
            final int h = mask.getSceneRasterHeight();
            final Rectangle imageRect = new Rectangle(0, 0, w, h);

            final PixelPos[] pixelPoints = new PixelPos[5];
            final GeoPos[] geoPoints = new GeoPos[5];
            for (int i = 0; i < geoPoints.length; i++) {
                pixelPoints[i] = new PixelPos();
                geoPoints[i] = new GeoPos();
            }

            MaskAreaStatistics areaStatistics = new MaskAreaStatistics(RsMathUtils.MEAN_EARTH_RADIUS / 1000.0);
            GeoCoding geoCoding = mask.getGeoCoding();

            pm.beginTask("Computing Mask area...", numXTiles * numYTiles);
            try {
                for (int tileX = minTileX; tileX < minTileX + numXTiles; ++tileX) {
                    for (int tileY = minTileY; tileY < minTileY + numYTiles; ++tileY) {
                        if (pm.isCanceled()) {
                            break;
                        }
                        final Rectangle tileRectangle = new Rectangle(
                                maskImage.getTileGridXOffset() + tileX * maskImage.getTileWidth(),
                                maskImage.getTileGridYOffset() + tileY * maskImage.getTileHeight(),
                                maskImage.getTileWidth(), maskImage.getTileHeight());

                        final Rectangle r = imageRect.intersection(tileRectangle);
                        if (!r.isEmpty()) {
                            Raster maskTile = maskImage.getTile(tileX, tileY);
                            for (int y = r.y; y < r.y + r.height; y++) {
                                for (int x = r.x; x < r.x + r.width; x++) {
                                    if (maskTile.getSample(x, y, 0) != 0) {
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
                                        float deltaLon = Math.abs(geoPoints[2].getLon() - geoPoints[1].getLon());
                                        float deltaLat = Math.abs(geoPoints[4].getLat() - geoPoints[3].getLat());
                                        double r2 = areaStatistics.getEarthRadius() * Math.cos(
                                                geoPoints[0].getLat() * MathUtils.DTOR);
                                        double a = r2 * deltaLon * MathUtils.DTOR;
                                        double b = areaStatistics.getEarthRadius() * deltaLat * MathUtils.DTOR;
                                        double pixelArea = a * b;
                                        areaStatistics.setPixelAreaMin(
                                                Math.min(areaStatistics.getPixelAreaMin(), pixelArea));
                                        areaStatistics.setPixelAreaMax(
                                                Math.max(areaStatistics.getPixelAreaMax(), pixelArea));
                                        areaStatistics.setMaskArea(areaStatistics.getMaskArea() + a * b);
                                        areaStatistics.setNumPixels(areaStatistics.getNumPixels() + 1);
                                    }
                                }
                            }
                        }
                        pm.worked(1);
                    }
                }
            } finally {
                pm.done();
            }
            return areaStatistics;
        }


        @Override
        public void done() {
            try {
                final MaskAreaStatistics areaStatistics = get();
                if (areaStatistics.getNumPixels() == 0) {
                    final String message = MessageFormat.format("{0}Mask is empty.", errMsgBase);
                    VisatApp.getApp().showErrorDialog(DIALOG_TITLE, message);
                } else {
                    showResults(areaStatistics);
                }
            } catch (ExecutionException e) {
                final String message = MessageFormat.format("An internal Error occurred:\n{0}", e.getMessage());
                VisatApp.getApp().showErrorDialog(DIALOG_TITLE, message);
                Debug.trace(e);
            } catch (InterruptedException e) {
                final String message = MessageFormat.format("An internal Error occurred:\n{0}", e.getMessage());
                VisatApp.getApp().showErrorDialog(DIALOG_TITLE, message);
                Debug.trace(e);
            }
        }

        private void showResults(MaskAreaStatistics areaStatistics) {
            final double roundFactor = 10000.0;
            final double maskAreaR = MathUtils.round(areaStatistics.getMaskArea(), roundFactor);
            final double meanPixelAreaR = MathUtils.round(areaStatistics.getMaskArea() / areaStatistics.getNumPixels(),
                                                          roundFactor);
            final double pixelAreaMinR = MathUtils.round(areaStatistics.getPixelAreaMin(), roundFactor);
            final double pixelAreaMaxR = MathUtils.round(areaStatistics.getPixelAreaMax(), roundFactor);

            final JPanel content = GridBagUtils.createPanel();
            final GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets.right = 4;
            gbc.gridy = 0;
            gbc.weightx = 0;

            gbc.insets.top = 2;
            addField(content, gbc, "Number of Mask pixels:", String.format("%15d", areaStatistics.getNumPixels()), "");
            addField(content, gbc, "Mask area:", String.format("%15.3f", maskAreaR), "km^2");
            addField(content, gbc, "Mean pixel area:", String.format("%15.3f", meanPixelAreaR), "km^2");
            addField(content, gbc, "Minimum pixel area:", String.format("%15.3f", pixelAreaMinR), "km^2");
            addField(content, gbc, "Maximum pixel area:", String.format("%15.3f", pixelAreaMaxR), "km^2");
            gbc.insets.top = 8;
            addField(content, gbc, "Mean earth radius:", String.format("%15.3f", areaStatistics.getEarthRadius()), "km");
            final ModalDialog dialog = new ModalDialog(VisatApp.getApp().getMainFrame(),
                                                       DIALOG_TITLE + " - " + mask.getDisplayName(),
                                                       content,
                                                       ModalDialog.ID_OK | ModalDialog.ID_HELP,
                                                       getHelpId());
            dialog.show();
        }

        private void addField(final JPanel content, final GridBagConstraints gbc,
                              final String text, final String value,
                              final String unit) {
            content.add(new JLabel(text), gbc);
            gbc.weightx = 1;
            content.add(createTextField(value), gbc);
            gbc.weightx = 0;
            content.add(new JLabel(unit), gbc);
            gbc.gridy++;
        }

        private JTextField createTextField(final String value) {
            JTextField field = new JTextField(value);
            field.setEditable(false);
            field.setHorizontalAlignment(JTextField.RIGHT);
            return field;
        }


    }
}
