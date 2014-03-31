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
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.IndexValidator;
import org.esa.beam.util.math.Range;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The <code>BowtieTiePointGeoCoding</code> class is a special geo-coding for
 * MODIS Level-1B and Level-2 swath products.
 * <p/>
 * <p>It enables BEAM to transform the MODIS swaths to uniformly gridded
 * image that is geographically referenced according to user-specified
 * projection and resampling parameters.
 * Correction for oversampling between scans as a function of increasing
 * (off-nadir) scan angle is performed (correction for bow-tie effect).
 */
public class BowtieTiePointGeoCoding extends AbstractBowtieGeoCoding {

    private TiePointGrid _latGrid;
    private TiePointGrid _lonGrid;

    /**
     * Constructs geo-coding based on two given tie-point grids.
     *
     * @param latGrid the latitude grid, must not be <code>null</code>
     * @param lonGrid the longitude grid, must not be <code>null</code>
     */
    public BowtieTiePointGeoCoding(TiePointGrid latGrid, TiePointGrid lonGrid, int scanlineHeight) {
        super(scanlineHeight);
        Guardian.assertNotNull("latGrid", latGrid);
        Guardian.assertNotNull("lonGrid", lonGrid);
        if (latGrid.getRasterWidth() != lonGrid.getRasterWidth() ||
                latGrid.getRasterHeight() != lonGrid.getRasterHeight() ||
                latGrid.getOffsetX() != lonGrid.getOffsetX() ||
                latGrid.getOffsetY() != lonGrid.getOffsetY() ||
                latGrid.getSubSamplingX() != lonGrid.getSubSamplingX() ||
                latGrid.getSubSamplingY() != lonGrid.getSubSamplingY()) {
            throw new IllegalArgumentException("latGrid is not compatible with lonGrid");
        }
        _latGrid = latGrid;
        _lonGrid = lonGrid;
        setGridOwner(_lonGrid.getOwner());
        init();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BowtieTiePointGeoCoding that = (BowtieTiePointGeoCoding) o;
        if (_latGrid == null || that._latGrid == null) {
            return false;
        }
        if (!_latGrid.equals(that._latGrid)) {
            return false;
        }
        if (_lonGrid == null || that._lonGrid == null) {
            return false;
        }
        if (!_lonGrid.equals(that._lonGrid)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = _latGrid != null ? _latGrid.hashCode() : 0;
        result = 31 * result + (_lonGrid != null ? _lonGrid.hashCode() : 0);
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
        _latGrid = null;
        _lonGrid = null;
    }

    private void init() {
        _gcList = new ArrayList<GeoCoding>();
        _centerLineList = new ArrayList<PolyLine>();
        final float osX = _lonGrid.getOffsetX();
        final float osY = _lonGrid.getOffsetY();
        final float ssX = _lonGrid.getSubSamplingX();
        final float ssY = _lonGrid.getSubSamplingY();

        final float[] latFloats = (float[]) _latGrid.getDataElems();
        final float[] lonFloats = (float[]) _lonGrid.getDataElems();

        final int stripeW = _lonGrid.getRasterWidth();
        final int gcStripeSceneWidth = _lonGrid.getSceneRasterWidth();
        final int tpRasterHeight = _lonGrid.getRasterHeight();
        final int stripeH = (int) (getScanlineHeight() / ssY);

        final int gcRawWidth = stripeW * stripeH;
        for (int y = 0; y < tpRasterHeight; y += stripeH) {
            final float[] lats = new float[gcRawWidth];
            final float[] lons = new float[gcRawWidth];
            System.arraycopy(lonFloats, y * stripeW, lons, 0, gcRawWidth);
            System.arraycopy(latFloats, y * stripeW, lats, 0, gcRawWidth);

            final Range range = Range.computeRangeFloat(lats, IndexValidator.TRUE, null, ProgressMonitor.NULL);
            if (range.getMin() < -90) {
                _gcList.add(null);
                _centerLineList.add(null);
            } else {
                final ModisTiePointGrid latTPG = new ModisTiePointGrid("lat" + y, stripeW, stripeH, osX, osY, ssX, ssY, lats);
                final ModisTiePointGrid lonTPG = new ModisTiePointGrid("lon" + y, stripeW, stripeH, osX, osY, ssX, ssY, lons, true);

                final TiePointGeoCoding geoCoding = new TiePointGeoCoding(latTPG, lonTPG, getDatum());
                _cross180 = _cross180 || geoCoding.isCrossingMeridianAt180();
                _gcList.add(geoCoding);
                _centerLineList.add(createCenterPolyLine(geoCoding, gcStripeSceneWidth, getScanlineHeight()));
            }
        }
        initSmallestAndLargestValidGeocodingIndices();
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
        final String latGridName = _latGrid.getName();
        final String lonGridName = _lonGrid.getName();

        if (mustRecalculateTiePointGrids(subsetDef)) {
            try {
                recalculateTiePointGrids(srcScene, destScene, subsetDef, latGridName, lonGridName);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return createGeocoding(destScene, ((BowtieTiePointGeoCoding)srcScene.getGeoCoding()).getScanlineHeight());
    }

    private boolean recalculateTiePointGrids(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef, String latGridName, String lonGridName) throws IOException {
        // first step - remove location TP grids that have already been transferred. Their size is
        // calculated wrong in most cases
        final TiePointGrid falseTiePointGrid = destScene.getProduct().getTiePointGrid(latGridName);
        final float rightOffsetX = falseTiePointGrid.getOffsetX();
        final float falseOffsetY = falseTiePointGrid.getOffsetY();
        final float rightSubsamplingX = falseTiePointGrid.getSubSamplingX();
        final float rightSubsamplingY = falseTiePointGrid.getSubSamplingY();

        removeTiePointGrid(destScene, latGridName);
        removeTiePointGrid(destScene, lonGridName);

        final Product srcProduct = srcScene.getProduct();
        final int sceneRasterHeight = srcProduct.getSceneRasterHeight();
        final int tpRasterHeight = srcProduct.getTiePointGrid(lonGridName).getRasterHeight();

        final Rectangle region = subsetDef.getRegion();
        final int startY = calculateStartLine(getScanlineHeight(), region);
        final int stopY = calculateStopLine(getScanlineHeight(), region);
        final int extendedHeight = stopY - startY;

        float[] recalculatedLatFloats = new float[region.width * extendedHeight];
        recalculatedLatFloats = srcProduct.getTiePointGrid(latGridName).getPixels(region.x, startY, region.width, extendedHeight, recalculatedLatFloats);

        float[] recalculatedLonFloats = new float[region.width * extendedHeight];
        recalculatedLonFloats = srcProduct.getTiePointGrid(lonGridName).getPixels(region.x, startY, region.width, extendedHeight, recalculatedLonFloats);


        final int yOffsetIncrement = startY - region.y;
        final TiePointGrid correctedLatTiePointGrid = new TiePointGrid(latGridName,
                region.width,
                extendedHeight,
                rightOffsetX,
                falseOffsetY + yOffsetIncrement,
                rightSubsamplingX,
                rightSubsamplingY,
                recalculatedLatFloats
        );
        final TiePointGrid correctedLonTiePointGrid = new TiePointGrid(lonGridName,
                region.width,
                extendedHeight,
                rightOffsetX,
                falseOffsetY + yOffsetIncrement,
                rightSubsamplingX,
                rightSubsamplingY,
                recalculatedLonFloats
        );
        destScene.getProduct().addTiePointGrid(correctedLatTiePointGrid);
        destScene.getProduct().addTiePointGrid(correctedLonTiePointGrid);

        return false;
    }

    private void removeTiePointGrid(Scene destScene, String gridName) {
        final TiePointGrid tiePointGrid = destScene.getProduct().getTiePointGrid(gridName);
        if (tiePointGrid != null) {
            destScene.getProduct().removeTiePointGrid(tiePointGrid);
        }
    }

    private boolean createGeocoding(Scene destScene, int stripeHeight) {
        final String latGridName = _latGrid.getName();
        final String lonGridName = _lonGrid.getName();
        final TiePointGrid latGrid = destScene.getProduct().getTiePointGrid(latGridName);
        final TiePointGrid lonGrid = destScene.getProduct().getTiePointGrid(lonGridName);
        if (latGrid != null && lonGrid != null) {
            destScene.setGeoCoding(new BowtieTiePointGeoCoding(latGrid, lonGrid, stripeHeight));
            return true;
        }
        return false;
    }

    static boolean mustRecalculateTiePointGrids(ProductSubsetDef subsetDef) {
        if(subsetDef == null) {
            return false;
        }
        return subsetDef.getRegion() != null;
    }

}
