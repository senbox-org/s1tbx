/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package gov.nasa.gsfc.seadas.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Scene;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.IndexValidator;
import org.esa.beam.util.math.Range;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The <code>BowtiePixelGeoCoding</code> class is a special geo-coding for
 * MODIS Level-1B and Level-2 swath products.
 * <p/>
 * <p>It enables BEAM to transform the MODIS swaths to uniformly gridded
 * image that is geographically referenced according to user-specified
 * projection and resampling parameters.
 * Correction for oversampling between scans as a function of increasing
 * (off-nadir) scan angle is performed (correction for bow-tie effect).
 */
public class BowtiePixelGeoCoding extends AbstractBowtieGeoCoding {
    private Band _latBand;
    private Band _lonBand;

    /**
     * Constructs geo-coding based on two given tie-point grids.
     *
     * @param latBand       the latitude band, must not be <code>null</code>
     * @param lonBand       the longitude band, must not be <code>null</code>
     * @param scanlineHeight the number of detectors in a scan
     * @param scanlineOffsetY the Y offset into the scanline where the data starts
     */
    public BowtiePixelGeoCoding(Band latBand, Band lonBand, int scanlineHeight, int scanlineOffsetY) {
        super(scanlineHeight, scanlineOffsetY);
        Guardian.assertNotNull("latBand", latBand);
        Guardian.assertNotNull("lonBand", lonBand);
        if (latBand.getRasterWidth() != lonBand.getRasterWidth() ||
                latBand.getRasterHeight() != lonBand.getRasterHeight()) {
            throw new IllegalArgumentException("latBand is not compatible with lonBand");
        }
        _latBand = latBand;
        _lonBand = lonBand;
        setGridOwner(_lonBand.getOwner());
        try {
            init();
        } catch (IOException e) {
            throw new IllegalArgumentException("can not init geocode");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BowtiePixelGeoCoding that = (BowtiePixelGeoCoding) o;
        if (_latBand == null || that._latBand == null) {
            return false;
        }
        if (!_latBand.equals(that._latBand)) {
            return false;
        }
        if (_lonBand == null || that._lonBand == null) {
            return false;
        }
        if (!_lonBand.equals(that._lonBand)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = _latBand != null ? _latBand.hashCode() : 0;
        result = 31 * result + (_lonBand != null ? _lonBand.hashCode() : 0);
        return result;
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     */
    @Override
    public void dispose() {
        super.dispose();
        _latBand = null;
        _lonBand = null;
    }

    private void init() throws IOException {
        _gcList = new ArrayList<GeoCoding>();
        _centerLineList = new ArrayList<PolyLine>();

        _latBand.readRasterDataFully(ProgressMonitor.NULL);
        _lonBand.readRasterDataFully(ProgressMonitor.NULL);

        final float[] latFloats = (float[]) _latBand.getDataElems();
        final float[] lonFloats = (float[]) _lonBand.getDataElems();

        final int stripeW = _lonBand.getRasterWidth();
        final int rasterHeight = _lonBand.getRasterHeight();
        final int stripeH = getScanlineHeight();

        final int gcRawWidth = stripeW * stripeH;

        int firstY = 0;
        // create placeholder for first stripe if needed
        if (getScanlineOffsetY() != 0) {
            _gcList.add(null);
            _centerLineList.add(null);
            firstY = stripeH - getScanlineOffsetY();
        }

        for (int y = firstY; y + stripeH <= rasterHeight; y += stripeH) {
            final float[] lats = new float[gcRawWidth];
            final float[] lons = new float[gcRawWidth];
            System.arraycopy(lonFloats, y * stripeW, lons, 0, gcRawWidth);
            System.arraycopy(latFloats, y * stripeW, lats, 0, gcRawWidth);
            addStripeGeocode(lats, lons, y, stripeW, stripeH);
        }

        // create first and last stripe if needed
        // use the delta from the neighboring stripe to extrapolate the data
        if (getScanlineOffsetY() != 0) {

            // create first stripe
            final float[] lats = new float[gcRawWidth];
            final float[] lons = new float[gcRawWidth];
            System.arraycopy(lonFloats, 0, lons, getScanlineOffsetY() * stripeW, (stripeH - getScanlineOffsetY()) * stripeW);
            System.arraycopy(latFloats, 0, lats, getScanlineOffsetY() * stripeW, (stripeH - getScanlineOffsetY()) * stripeW);
            for (int x = 0; x < stripeW; x++) {
                int y1 = stripeH - getScanlineOffsetY(); // coord of first y in next stripe
                int y2 = y1 + stripeH - 1;         // coord of last y in next stripe
                int index1 = y1 * stripeW + x;
                int index2 = y2 * stripeW + x;
                double deltaLat = (latFloats[index2] - latFloats[index1]) / (stripeH - 1);
                double deltaLon = (lonFloats[index2] - lonFloats[index1]) / (stripeH - 1);
                double refLat = latFloats[x];
                double refLon = lonFloats[x];

                for (int y = 0; y < getScanlineOffsetY(); y++) {
                    lons[y * stripeW + x] = (float)(refLon - (deltaLon * (getScanlineOffsetY() - y)));
                    lats[y * stripeW + x] = (float)(refLat - (deltaLat * (getScanlineOffsetY() - y)));
                }
            }
            GeoCoding gc = createStripeGeocode(lats, lons, 0, stripeW, stripeH);
            if (gc != null) {
                _gcList.add(0, gc);
                _centerLineList.add(0, createCenterPolyLine(gc, stripeW, stripeH));
            }
        }

        // create last stripe
        int lastStripeH = (rasterHeight - stripeH + getScanlineOffsetY()) % stripeH;
        if (lastStripeH != 0) {

            int lastStripeY = rasterHeight - lastStripeH - 1; // y coord of first y of last stripe
            final float[] lats = new float[gcRawWidth];
            final float[] lons = new float[gcRawWidth];
            System.arraycopy(lonFloats, lastStripeY * stripeW, lons, 0, lastStripeH * stripeW);
            System.arraycopy(latFloats, lastStripeY * stripeW, lats, 0, lastStripeH * stripeW);
            for (int x = 0; x < stripeW; x++) {
                int y1 = lastStripeY - stripeH; // coord of first y in next stripe
                int y2 = lastStripeY - 1;         // coord of last y in next stripe
                int index1 = y1 * stripeW + x;
                int index2 = y2 * stripeW + x;
                float deltaLat = (latFloats[index2] - latFloats[index1]) / (stripeH - 1);
                float deltaLon = (lonFloats[index2] - lonFloats[index1]) / (stripeH - 1);
                float refLat = latFloats[lastStripeY * stripeW + x];
                float refLon = lonFloats[lastStripeY * stripeW + x];

                for (int y = lastStripeH; y < stripeH; y++) {
                    lons[y * stripeW + x] = refLon - (deltaLon * (y - lastStripeH + 1));
                    lats[y * stripeW + x] = refLat - (deltaLat * (y - lastStripeH + 1));
                }
            }
            addStripeGeocode(lats, lons, lastStripeY, stripeW, stripeH);

        }

        initSmallestAndLargestValidGeocodingIndices();
    }

    private void addStripeGeocode(float[] lats, float[] lons, int y, int stripeW, int stripeH) throws IOException {
        GeoCoding gc = createStripeGeocode(lats, lons, y, stripeW, stripeH);
        if (gc != null) {
            _gcList.add(gc);
            _centerLineList.add(createCenterPolyLine(gc, stripeW, stripeH));
        } else {
            _gcList.add(null);
            _centerLineList.add(null);
        }
    }

    private GeoCoding createStripeGeocode(float[] lats, float[] lons, int y, int stripeW, int stripeH) throws IOException {
        final Range range = Range.computeRangeFloat(lats, IndexValidator.TRUE, null, ProgressMonitor.NULL);
        if (range.getMin() < -90) {
            return null;
        } else {
            final ModisTiePointGrid latGrid = new ModisTiePointGrid("lat" + y, stripeW, stripeH, 0, 0, 1, 1, lats);
            final ModisTiePointGrid lonGrid = new ModisTiePointGrid("lon" + y, stripeW, stripeH, 0, 0, 1, 1, lons);
            final TiePointGeoCoding geoCoding = new TiePointGeoCoding(latGrid, lonGrid, getDatum());
            _cross180 = _cross180 || geoCoding.isCrossingMeridianAt180();
            return geoCoding;
        }
    }

    /**
     * Transfers the geo-coding of the {@link org.esa.beam.framework.datamodel.Scene srcScene} to the {@link org.esa.beam.framework.datamodel.Scene destScene} with respect to the given
     * {@link org.esa.beam.framework.dataio.ProductSubsetDef subsetDef}.
     *
     * @param srcScene  the source scene
     * @param destScene the destination scene
     * @param subsetDef the definition of the subset, may be <code>null</code>
     * @return true, if the geo-coding could be transferred.
     */
    @Override
    public boolean transferGeoCoding(final Scene srcScene, final Scene destScene, final ProductSubsetDef subsetDef) {

        BowtiePixelGeoCoding srcGeocoding = (BowtiePixelGeoCoding)srcScene.getGeoCoding();
        final String latBandName = _latBand.getName();
        final String lonBandName = _lonBand.getName();
        final int srcStripeOffsetY = srcGeocoding.getScanlineOffsetY();
        int destStripeOffsetY = srcStripeOffsetY;
        if(subsetDef != null) {
            Rectangle region = subsetDef.getRegion();
            if(region != null) {
                destStripeOffsetY = (srcStripeOffsetY+region.y) % getScanlineHeight();
            }
        }
        final Band latBand = destScene.getProduct().getBand(latBandName);
        final Band lonBand = destScene.getProduct().getBand(lonBandName);

        if (latBand != null && lonBand != null) {
            destScene.setGeoCoding(new BowtiePixelGeoCoding(latBand, lonBand, getScanlineHeight(), destStripeOffsetY));
            return true;
        }
        return false;
    }

}
