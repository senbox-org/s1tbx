/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.dem;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.gpf.OperatorException;

import java.io.File;
import java.io.IOException;

public class FileElevationModel implements ElevationModel, Resampling.Raster {

    private Resampling resampling;
    private Resampling.Index resamplingIndex;
    private Resampling.Raster resamplingRaster;
    private GeoCoding tileGeocoding;

    private FileElevationTile fileElevationTile;

    private int RASTER_WIDTH;
    private int RASTER_HEIGHT;
    private float noDataValue = 0;

    public FileElevationModel(final File file, final String resamplingMethodName) throws IOException {

        this(file, resamplingMethodName, null);
    }

    public FileElevationModel(final File file, final String resamplingMethodName, final Float demNoDataValue) throws IOException {

        if (resamplingMethodName.equals(DEMFactory.DELAUNAY_INTERPOLATION))
            throw new OperatorException("Delaunay interpolation for an external DEM file is currently not supported");

        init(file, ResamplingFactory.createResampling(resamplingMethodName), demNoDataValue);
    }

    public FileElevationModel(final File file, final Resampling resamplingMethod, final Float demNoDataValue) throws IOException {

        init(file, resamplingMethod, demNoDataValue);
    }

    private void init(final File file, final Resampling resamplingMethod, Float demNoDataValue) throws IOException {
        final ProductReader productReader = ProductIO.getProductReaderForFile(file);
        final Product product = productReader.readProductNodes(file, null);
        RASTER_WIDTH = product.getBandAt(0).getSceneRasterWidth();
        RASTER_HEIGHT = product.getBandAt(0).getSceneRasterHeight();
        fileElevationTile = new FileElevationTile(product);
        tileGeocoding = product.getGeoCoding();
        if(demNoDataValue == null)
            noDataValue = (float)product.getBandAt(0).getNoDataValue();
        else
            noDataValue = demNoDataValue;

        resampling = resamplingMethod;
        resamplingIndex = resampling.createIndex();
        resamplingRaster = this;
    }

    public void dispose() {
        fileElevationTile.dispose();
    }

    public ElevationModelDescriptor getDescriptor() {
        return null;
    }

    public float getNoDataValue() {
        return noDataValue;
    }

    public Resampling getResampling() {
        return resampling;
    }

    public synchronized double getElevation(final GeoPos geoPos) throws Exception {
        try {
            final PixelPos pix = tileGeocoding.getPixelPos(geoPos, null);
            if(!pix.isValid() || pix.x < 0 || pix.y < 0 || pix.x >= RASTER_WIDTH || pix.y >= RASTER_HEIGHT)
               return noDataValue;

            resampling.computeIndex(pix.x, pix.y, RASTER_WIDTH, RASTER_HEIGHT, resamplingIndex);

            final double elevation = resampling.resample(resamplingRaster, resamplingIndex);
            if (Double.isNaN(elevation)) {
                return noDataValue;
            }
            return elevation;
        } catch(Exception e) {
            throw new Exception("Problem reading DEM: "+e.getMessage());
        }
    }

    public PixelPos getIndex(final GeoPos geoPos) {
        return tileGeocoding.getPixelPos(geoPos, null);
    }

    public GeoPos getGeoPos(final PixelPos pixelPos) {
        return tileGeocoding.getGeoPos(pixelPos, null);
    }

    public float getSample(double pixelX, double pixelY) throws IOException {

        final float sample = fileElevationTile.getSample((int)pixelX, (int)pixelY);
        if (sample == noDataValue) {
            return Float.NaN;
        }
        return sample;
    }

    public int getWidth() {
        return RASTER_WIDTH;
    }

    public int getHeight() {
        return RASTER_HEIGHT;
    }

    public boolean getSamples(final int[] x, final int[] y, final double[][] samples) throws IOException {
        boolean allValid = true;
        for (int i = 0; i < y.length; i++) {
            for (int j = 0; j < x.length; j++) {
                samples[i][j] = fileElevationTile.getSample(x[j], y[i]);
                if (samples[i][j] == noDataValue) {
                    samples[i][j] = Double.NaN;
                    allValid = false;
                }
            }
        }
        return allValid;
    }
}