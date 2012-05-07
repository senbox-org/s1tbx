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
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

public class CreateXYDisplacementBandsAction extends ExecCommand {

    public static final String DIALOG_TITLE = "Create X,Y Displacement Bands";
    private static final String DEFAULT_DISPLACEMENT_X_BAND_NAME = "displacement_x";
    private static final String DEFAULT_DISPLACEMENT_Y_BAND_NAME = "displacement_y";

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
                    final Band[] bands;
                    try {
                        bands = get();
                        product.addBand(bands[0]);
                        product.addBand(bands[1]);
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

    private static Band[] createXYDisplacementBands(final Product product, ProgressMonitor pm) {
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();

        final Band bandX = new Band(DEFAULT_DISPLACEMENT_X_BAND_NAME, ProductData.TYPE_FLOAT32, width, height);
        bandX.setSynthetic(true);
        bandX.setUnit("pixels");
        bandX.setDescription("Geo-coding X displacement");

        final Band bandY = new Band(DEFAULT_DISPLACEMENT_Y_BAND_NAME, ProductData.TYPE_FLOAT32, width, height);
        bandY.setSynthetic(true);
        bandY.setUnit("pixels");
        bandY.setDescription("Geo-coding Y displacement");

        final float[] dataX = new float[width * height];
        final float[] dataY = new float[width * height];

        bandX.setRasterData(ProductData.createInstance(dataX));
        bandY.setRasterData(ProductData.createInstance(dataY));

        pm.beginTask("Computing X,Y displacements for product '" + product.getName() + "'...", height);
        try {
            final GeoPos geoPos = new GeoPos();
            final PixelPos pixelPos1 = new PixelPos();
            final PixelPos pixelPos2 = new PixelPos();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixelPos1.setLocation(x + 0.5f, y + 0.5f);
                    product.getGeoCoding().getGeoPos(pixelPos1, geoPos);
                    product.getGeoCoding().getPixelPos(geoPos, pixelPos2);
                    dataX[y * width + x] = pixelPos2.x - pixelPos1.x;
                    dataY[y * width + x] = pixelPos2.y - pixelPos1.y;
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }

        return new Band[]{bandX, bandY};
    }

}