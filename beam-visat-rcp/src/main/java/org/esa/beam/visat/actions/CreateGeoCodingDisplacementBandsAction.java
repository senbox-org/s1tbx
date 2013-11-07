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
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import java.awt.Color;

/**
 * An action that lets users add a number of bands that can be used to assess the performance/accuracy
 * of the current geo-coding.
 */
public class CreateGeoCodingDisplacementBandsAction extends ExecCommand {

    public static final String DIALOG_TITLE = "Create Geo-Coding Displacement Bands";
    public static final float[][] OFFSETS = new float[][]{

            {0.00f, 0.00f},
            {0.25f, 0.25f},
            {0.50f, 0.50f},
            {0.75f, 0.75f},

            {0.25f, 0.75f},
            {0.75f, 0.25f},
    };

    @Override
    public void updateState(CommandEvent event) {
        final Product product = VisatApp.getApp().getSelectedProduct();
        setEnabled(product != null && product.getGeoCoding() != null && product.getGeoCoding().canGetGeoPos() && product.getGeoCoding().canGetPixelPos());
    }


    @Override
    public void actionPerformed(CommandEvent event) {
        createXYDisplacementBands();
    }

    private void createXYDisplacementBands() {
        final Product product = VisatApp.getApp().getSelectedProduct();

        final ProgressMonitorSwingWorker swingWorker = new ProgressMonitorSwingWorker<Band[], Object>(VisatApp.getApp().getMainFrame(), DIALOG_TITLE) {
            @Override
            protected Band[] doInBackground(ProgressMonitor pm) throws Exception {
                return createXYDisplacementBands(product, pm);
            }

            @Override
            public void done() {
                if (VisatApp.getApp().getPreferences().getPropertyBool(VisatApp.PROPERTY_KEY_AUTO_SHOW_NEW_BANDS, true)) {
                    try {
                        Band[] bands = get();
                        if (bands == null) {
                            return;
                        }
                        for (Band band : bands) {
                            Band oldBand = product.getBand(band.getName());
                            if (oldBand != null) {
                                product.removeBand(oldBand);
                            }
                            product.addBand(band);
                        }
                    } catch (Exception e) {
                        VisatApp.getApp().showErrorDialog(DIALOG_TITLE,
                                                          "An internal Error occurred:\n" + e.getMessage());
                        Debug.trace(e);
                    }
                }
            }
        };

        swingWorker.execute();
    }

    static final int N = 5;

    private static Band[] createXYDisplacementBands(final Product product, ProgressMonitor pm) {
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();

        ImageInfo blueToRedGrad = new ImageInfo(new ColorPaletteDef(new ColorPaletteDef.Point[]{
                new ColorPaletteDef.Point(-1.0, Color.BLUE),
                new ColorPaletteDef.Point(0.0, Color.WHITE),
                new ColorPaletteDef.Point(1.0, Color.RED),
        }));
        ImageInfo amplGrad = new ImageInfo(new ColorPaletteDef(new ColorPaletteDef.Point[]{
                new ColorPaletteDef.Point(0.0, Color.WHITE),
                new ColorPaletteDef.Point(1.0, Color.RED),
        }));
        ImageInfo phaseGrad = new ImageInfo(new ColorPaletteDef(new ColorPaletteDef.Point[]{
                new ColorPaletteDef.Point(-Math.PI, Color.WHITE),
                new ColorPaletteDef.Point(0.0, Color.BLUE),
                new ColorPaletteDef.Point(+Math.PI, Color.WHITE),
        }));

        final Band bandX = new Band("gc_displ_x", ProductData.TYPE_FLOAT32, width, height);
        configureBand(bandX, blueToRedGrad.clone(), "pixels", "Geo-coding X-displacement");

        final Band bandY = new Band("gc_displ_y", ProductData.TYPE_FLOAT32, width, height);
        configureBand(bandY, blueToRedGrad.clone(), "pixels", "Geo-coding Y-displacement");

        final Band bandAmpl = new VirtualBand("gc_displ_ampl",
                                              ProductData.TYPE_FLOAT32, width, height,
                                              "ampl(gc_displ_x, gc_displ_y)");
        configureBand(bandAmpl, amplGrad.clone(), "pixels", "Geo-coding displacement amplitude");

        final Band bandPhase = new VirtualBand("gc_displ_phase",
                                               ProductData.TYPE_FLOAT32, width, height,
                                               "phase(gc_displ_x, gc_displ_y)");
        configureBand(bandPhase, phaseGrad.clone(), "radians", "Geo-coding displacement phase");

        final float[] dataX = new float[width * height];
        final float[] dataY = new float[width * height];

        bandX.setRasterData(ProductData.createInstance(dataX));
        bandY.setRasterData(ProductData.createInstance(dataY));

        pm.beginTask("Computing geo-coding displacements for product '" + product.getName() + "'...", height);
        try {
            final GeoPos geoPos = new GeoPos();
            final PixelPos pixelPos1 = new PixelPos();
            final PixelPos pixelPos2 = new PixelPos();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    float maxX = 0;
                    float maxY = 0;
                    float valueX = 0;
                    float valueY = 0;
                    for (float[] offset : OFFSETS) {
                        pixelPos1.setLocation(x + offset[0], y + offset[1]);
                        product.getGeoCoding().getGeoPos(pixelPos1, geoPos);
                        product.getGeoCoding().getPixelPos(geoPos, pixelPos2);
                        float dx = pixelPos2.x - pixelPos1.x;
                        float dy = pixelPos2.y - pixelPos1.y;
                        if (Math.abs(dx) > maxX) {
                            maxX = Math.abs(dx);
                            valueX = dx;
                        }
                        if (Math.abs(dy) > maxY) {
                            maxY = Math.abs(dy);
                            valueY = dy;
                        }
                    }
                    dataX[y * width + x] = valueX;
                    dataY[y * width + x] = valueY;
                }
                if (pm.isCanceled()) {
                    return null;
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }

        return new Band[]{bandX, bandY, bandAmpl, bandPhase};
    }

    private static void configureBand(Band band05X, ImageInfo imageInfo, String unit, String description) {
        band05X.setUnit(unit);
        band05X.setDescription(description);
        band05X.setImageInfo(imageInfo);
        band05X.setNoDataValue(Double.NaN);
        band05X.setNoDataValueUsed(true);
    }

}