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

package org.esa.beam.processor.binning.database;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.processor.binning.L3Constants;
import org.esa.beam.processor.binning.L3Context;
import org.esa.beam.processor.binning.algorithm.Algorithm;
import org.esa.beam.processor.binning.store.BinStoreFactory;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;


@Deprecated
/**
 * The class implements a fast flux conserving resampling based on the
 * Sutherland-Hodgman clipping algorithm.
 * <p/>
 * Consider an original image of data in some projection and a new map of an
 * overlapping region with some projection, coordinate system, etc.  Assume that
 * the flux within a given pixel in the original image is constant over the area
 * of the pixel.  The flux within each pixel in the new map should be the
 * integral of the flux in the region occupied by each pixel in the map.
 * <p/>
 * <ul>
 * <li> Find all of the corners of the pixels in resampled map and project the
 * positions of these corners into the projection of the original image. These
 * should be scaled such that the coordinates run from 0 to mx and my
 * where mx and my are the dimensions of the original image.
 * I.e., in these coordinate each pixel in the original imageis a unit square.
 * This step is done prior to call the primary polySamp methods in this class.
 * NB: the corners of the pixels are required rather than the pixel centers.
 * <p/>
 * <li>For each pixel in the resampled map, find a bounding box in the original
 * image's coordinates.  This is used to find a rectangle of candidate pixels in
 * the original image that may contribute flux to this pixel in the resampled
 * map.
 * <p/>
 * <li>For each candidate image pixel clip the resampled pixel to the image
 * pixels boundaries.
 * <p/>
 * <li>Calculate the area of the clipped region.  This is easy since the clipped
 * region is a convex polygon and we have the vertices.  Triangulating the
 * polygon allows us to calculate its area.
 * <p/>
 * <li>Add a flux to the resampled pixel equal to the area of the clipped
 * resampled pixel times the flux in the original map pixel
 * <p/>
 * <li> Repeat for all candidate original pixels
 * <p/>
 * <li> Go to the next resampling pixel.
 * </ul>
 * <p/>
 * <p/>
 * The instance methods of this class are not thread-safe, however it is
 * possible to generate a separate PolySamp object for each thread to resample
 * the same input image.
 * <p/>
 * <p/>
 * Although this class implements the SpatialBinDatabase interface it does not
 * support a spatial binning algorithm as each resampled  pixel is assumed to be
 * smaller or comparable in size to the input pixels.
 *
 * @author Tom McGlynn, NASA/GSFC, 3rd October 2002
 * @author Thomas Lankester, Infoterra Ltd., 19th May 2003
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class ClippingResampler extends SpatialBinDatabase {

    /**
     * Used for permil calculations
     */
    private static final float A_MIL = 1000;

    /**
     * Threshold used with permil values
     */
    private static final float HALF_MIL = 500;

    /**
     * Resampled image pixel size in latlon degrees
     */
    private double _rixSize;

    /**
     * Minimum longitude, in degrees, of resampling grid
     */
    private int _rixMinLon;

    /**
     * Maximum latitude, in degrees, of resampling grid
     */
    private int _rixMaxLat;

    /**
     * Maximum longitude, in degrees, of resampling grid
     */
    private int _rixMaxLon;

    /**
     * X-dimension of resampled image
     */
    private int _rixX;

    /**
     * Y-dimension of resampled image
     */
    private int _rixY;

    // Intermediate storage used by the Resampler
    private double[] psX1 = new double[12];
    private double[] psY1 = new double[12];

    /**
     * Bin for resampling.
     */
    private Bin resamplingBin;
    private static final int VALUE = 0;
    private static final int COVERAGE = 1;


    public ClippingResampler(L3Context context, Product product, Logger logger) {
        super(context, product, logger);
    }

    @Override
    public void processSpatialBinning(ProgressMonitor pm) throws ProcessorException, IOException {
        pm.beginTask("Clipping resampling of product '" + product.getName() + "'...", 4);
        try {
            estimateProductDimension();
            pm.worked(1);

            init();
            pm.worked(1);

            createStore();
            pm.worked(1);

            resample(SubProgressMonitor.create(pm, 1));
        } finally {
            pm.done();
        }
    }

    /**
     * Reads a bin at the given location using row/col of the local
     * coordinate system. The Algorithm of the BinDatabase is used
     * to accumulate the resampled value into the Bin object and then
     * to update the spatial bin statistics.
     * If more than half of the area of the resampled pixel is
     * covered by input then the area-weighted input value is used.
     *
     * @param gridRowCol row/column coordinates to read from
     * @param bin        bin to be filled (if null then a new bin is created)
     */
    @Override
    public void read(Point gridRowCol, Bin bin) throws IOException {
        // check for out of range bins
        if (!locator.isValidPosition(gridRowCol)) {
            bin.clear();
            return;
        }
        // calc bin value from area
        Point localRowCol = gridToLocal(gridRowCol, null);
        store.read(localRowCol, resamplingBin);
        final L3Context.BandDefinition[] bandDefs = context.getBandDefinitions();

        bin.clear();
        for (int i = 0; i < bandDefs.length; i++) {
            resamplingBin.setBandIndex(i);
            final short coverage = (short) resamplingBin.read(COVERAGE);
            if (coverage > HALF_MIL) {
                final L3Context.BandDefinition bandDef = bandDefs[i];
                bin.setBandIndex(i);
                Algorithm algo = bandDef.getAlgorithm();
                float resample = resamplingBin.read(VALUE);
                if (coverage < A_MIL) {
                    resample *= (A_MIL / coverage);
                }
                algo.accumulateSpatial(resample, bin);
                // finalise the bin statistics
                if (algo.needsFinishSpatial()) {
                    algo.finishSpatial(bin);
                }
            }
        }
    }

    /**
     * Create a binstore that is used for the spatial aggregation.
     *
     * @throws IOException
     * @throws ProcessorException
     */
    @Override
    protected void createStore() throws IOException, ProcessorException {
        logger.info(L3Constants.LOG_MSG_CREATE_BIN_DB);

        store = BinStoreFactory.getInstance().createSpatialStore(context.getDatabaseDir(),
                                                                 product.getName(), getWidth(), getHeight(),
                                                                 context.getNumBands() * 2);

        logger.info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    private void init() {
        // get current resampling grid locator object and algorithm
        BinLocator _locator = context.getLocator();

        // set the latlon bounding box attributes
        Rectangle2D border = context.getBorder();
        _rixMinLon = (int) border.getMinX();
        _rixMaxLat = (int) border.getMaxY();
        _rixMaxLon = (int) border.getMaxX();

        // set the resampled pixel size and grid dimensions
        final float rixels_per_degree = context.getGridCellSize();
        _rixSize = 1.0 / rixels_per_degree;
        _rixX = _locator.getWidth();
        _rixY = _locator.getHeight();

        // set up the resampling pixel (rixel) grid
        final int numBands = context.getNumBands();
        int[] numResamplingVars = new int[numBands];
        Arrays.fill(numResamplingVars, 2);
        resamplingBin = new FloatArrayBin(numResamplingVars);
    }

    private void resample(ProgressMonitor pm) throws ProcessorException, IOException {
        logger.info(L3Constants.LOG_MSG_SPATIAL_BINNING);

        final L3Context.BandDefinition[] bandDefs = context.getBandDefinitions();
        GeoCoding coding = product.getGeoCoding();
        final Rectangle2D dbBbox = context.getBorder();
        final int productWidth = product.getSceneRasterWidth();
        final int productHeight = product.getSceneRasterHeight();

        if (coding == null) {
            throw new ProcessorException("This product has no geocoding");
        }

        // allocate line vectors
        float[][] values = new float[bandDefs.length][productWidth];
        boolean[][] useData = new boolean[bandDefs.length][productWidth];
        Term[] bitmaskTerm = new Term[bandDefs.length];
        Band[] valueBands = new Band[bandDefs.length];

        float[] pixelValues = new float[bandDefs.length];
        boolean[] pixelUsage = new boolean[bandDefs.length];

        for (int bandIndex = 0; bandIndex < bandDefs.length; bandIndex++) {
            final L3Context.BandDefinition bandDef = bandDefs[bandIndex];
            try {
                bitmaskTerm[bandIndex] = product.parseExpression(bandDef.getBitmaskExp());
            } catch (ParseException e) {
                // will not throw exception, is checked before if loadValidatedProduct() is called before
            }
            valueBands[bandIndex] = product.getBand(bandDef.getBandName());
            Arrays.fill(useData[bandIndex], true);
        }

        // create lat and lon corner grids (with coords for top and bottom of each row)
        PixelPos pixelPos = new PixelPos();
        int widthPlus = productWidth + 1;
        GeoPos[][] cornerLatlon = new GeoPos[2][widthPlus];
        GeoPos[] pixCorner = new GeoPos[4];
        pixelPos.y = 0;

        final int topCorners = 0;
        final int bottomCorners = 1;

        // populate pixel top corner coordinates
        for (int col = 0; col < widthPlus; col++) {
            pixelPos.x = col;
            cornerLatlon[bottomCorners][col] = coding.getGeoPos(pixelPos, null);
        }

        pm.beginTask("Spatial binning of product '" + product.getName() + "'...", productHeight);
        try {
            // loop over scanlines
            for (int line = 0; line < productHeight; line++) {
                // load data for a line
                for (int bandIndex = 0; bandIndex < bandDefs.length; bandIndex++) {
                    valueBands[bandIndex].readPixels(0, line, productWidth, 1, values[bandIndex], ProgressMonitor.NULL);
                    if (bitmaskTerm[bandIndex] != null) {
                        product.readBitmask(0, line, productWidth, 1, bitmaskTerm[bandIndex], useData[bandIndex],
                                            ProgressMonitor.NULL);
                    }
                }

                // cycle the pixel corner row indices
                pixelPos.y = line + 1;

                final GeoPos[] lowerCorners = cornerLatlon[topCorners];
                cornerLatlon[topCorners] = cornerLatlon[bottomCorners];
                cornerLatlon[bottomCorners] = lowerCorners;

                // get the first latlon coord value for the next row
                pixelPos.x = 0;
                cornerLatlon[bottomCorners][0] = coding.getGeoPos(pixelPos, null);
                pixCorner[1] = cornerLatlon[topCorners][0];
                pixCorner[2] = cornerLatlon[bottomCorners][0];

                // loop over line-pixels
                for (int n = 0; n < productWidth; n++) {
                    final int rightCornerX = n + 1;
                    pixelPos.x = rightCornerX;
                    final GeoPos recycle = cornerLatlon[bottomCorners][rightCornerX];
                    cornerLatlon[bottomCorners][rightCornerX] = coding.getGeoPos(pixelPos, recycle);
                    pixCorner[0] = pixCorner[1];
                    pixCorner[3] = pixCorner[2];
                    pixCorner[1] = cornerLatlon[topCorners][rightCornerX];
                    pixCorner[2] = cornerLatlon[bottomCorners][rightCornerX];
                    if (dbBbox.contains(pixCorner[0].lon, pixCorner[0].lat)
                        || dbBbox.contains(pixCorner[1].lon, pixCorner[1].lat)
                        || dbBbox.contains(pixCorner[2].lon, pixCorner[2].lat)
                        || dbBbox.contains(pixCorner[3].lon, pixCorner[3].lat)) {
                        boolean validData = false;
                        for (int bandIndex = 0; bandIndex < bandDefs.length; bandIndex++) {
                            pixelValues[bandIndex] = values[bandIndex][n];
                            pixelUsage[bandIndex] = useData[bandIndex][n];
                            validData = validData || pixelUsage[bandIndex];
                        }
                        if (validData) {
                            resamplePixel(pixCorner, pixelValues, pixelUsage);
                        }
                    }
                }

                // update progressbar
                pm.worked(1);
                if (pm.isCanceled()) {
                    break;
                }
            }
            if (!pm.isCanceled()) {
                logger.info(ProcessorConstants.LOG_MSG_SUCCESS);
            }
        } finally {
            pm.done();
        }

    }

    /**
     * Resamples a single original image pixel.
     * The area weighted contribution of the pixel to each
     * overlapping rixel is calulated.
     *
     * @param imPix_loc  geoPos of the corners of the pixel [4]
     * @param imPix_val  data value of the pixel to be resampled
     * @param pixelUsage valid mask of the pixel
     */
    private void resamplePixel(GeoPos[] imPix_loc,
                               float[] imPix_val, boolean[] pixelUsage) throws IOException {
        double[] relPixX = new double[4];
        double[] relPixY = new double[4];
        double minX;
        double maxX;
        double minY;
        double maxY;
        int i, j;
        Point rowCol = new Point();

        // convert GeoPos to resampling grid coordinates
        for (i = 0; i < 4; i++) {
            // handle longitude wrap around
            if (_rixMaxLon > 170.f && imPix_loc[i].lon < -170.f) {
                imPix_loc[i].lon += 360.f;
            }
            if (_rixMinLon < -170.f && imPix_loc[i].lon > 170.f) {
                imPix_loc[i].lon -= 360.f;
            }

            relPixX[i] = (imPix_loc[i].lon - _rixMinLon) / _rixSize;
            relPixY[i] = (_rixMaxLat - imPix_loc[i].lat) / _rixSize;
        }

        //  Get the rixel grid bounding box for the pixel.
        // init the bounding box min-max coords
        minX = relPixX[0];
        maxX = minX;
        minY = relPixY[0];
        maxY = minY;

        // sort out the bounding box min-max coords
        for (int k = 1; k < 4; k++) {
            if (relPixX[k] < minX) {
                minX = relPixX[k];
            } else if (relPixX[k] > maxX) {
                maxX = relPixX[k];
            }

            if (relPixY[k] < minY) {
                minY = relPixY[k];
            } else if (relPixY[k] > maxY) {
                maxY = relPixY[k];
            }
        }

        // round the extrema of the rixel coordinates to int values
        minX = Math.floor(minX);
        maxX = Math.ceil(maxX);

        minY = Math.floor(minY);
        maxY = Math.ceil(maxY);

        //  Check if pixel position with respect to the rixel grid.
        if (maxX <= 0 || minX >= _rixX || maxY <= 0 || minY >= _rixY) {
            // off the grid, do not need to do process
            return;
        }
        // confine resampling to the rixel grid
        if (minX < getColOffset()) {
            minX = getColOffset();
        }

        if (minY < getRowOffset()) {
            minY = getRowOffset();
        }

        if (maxX > getColOffset() + getWidth()) {
            maxX = getColOffset() + getWidth();
        }

        if (maxY > getRowOffset() + getHeight()) {
            maxY = getRowOffset() + getHeight();
        }

        // Loop over the potentially overlapping rixels.
        for (i = (int) minX; i < (int) maxX; i++) {
            for (j = (int) minY; j < (int) maxY; j++) {
                // Clip the quadrilateral given by the coordinates
                // of the image pixel, to this particular resampling
                // pixel.
                final int nv = rectClip(4, relPixX, relPixY, psX1, psY1,
                                        i, j, i + 1, j + 1);

                // If there is no overlap we won't get any
                // vertices back in the clipped set.
                // each rixel has a unit size on the rixel grid
                // so area sums for all the input pixels must = 1
                if (nv > 0) {
                    // calculate the area of the clipped pixel
                    final double area = convexArea(nv, psX1, psY1);
                    rowCol.x = i - getColOffset();
                    rowCol.y = j - getRowOffset();

                    // Add the appropriate fraction of the original
                    // flux into the output pixel.
                    store.read(rowCol, resamplingBin);
                    final int numBands = context.getNumBands();
                    for (int bandIndex = 0; bandIndex < numBands; bandIndex++) {
                        if (pixelUsage[bandIndex]) {
                            resamplingBin.setBandIndex(bandIndex);
                            float sumAreaValue = resamplingBin.read(VALUE);
                            sumAreaValue += area * imPix_val[bandIndex];
                            resamplingBin.write(VALUE, sumAreaValue);

                            float sumCoverage = resamplingBin.read(COVERAGE);
                            sumCoverage += (short) (area * A_MIL + 0.5);
                            resamplingBin.write(COVERAGE, sumCoverage);
                        }
                    }
                    store.write(rowCol, resamplingBin);
                }
            }
        }
    }

    /**
     * Calculate the area of a convex polygon.
     * This function calculates the area of a convex polygon
     * by deconvolving the polygon into triangles and summing
     * the areas of the consituents.  The user provides the
     * coordinates of the vertices of the polygon in sequence
     * along the circumference (in either direction and starting
     * at any point).
     * <p/>
     * Only distinct vertices should be given.
     *
     * @param x The x coordinates of the vertices
     * @param y The y coordinates of teh vertices
     * @param n The number of vertices in the polygon.
     *
     * @return The area of the polygon.
     */
    private static double convexArea(int n, double[] x, double[] y) {
        double area = 0;

        for (int i = 1; i < n - 1; i += 1) {
            area += triangleArea(x[0], y[0], x[i], y[i], x[i + 1], y[i + 1]);
        }

        return area;
    }

    /**
     * Calculate the area of an arbitrary triangle.
     * Use the vector formula:
     * A = 1/2 sqrt(X^2 Y^2 - (X-Y)^2)
     * where X and Y are vectors describing two sides
     * of the triangle.
     *
     * @param x0 x-coordinate of first vertex
     * @param y0 y-coordinate of first vertex
     * @param x1 x-coordinate of second vertex
     * @param y1 y-coordinate of second vertex
     * @param x2 x-coordinate of third vertex
     * @param y2 y-coordinate of third vertex
     *
     * @return area of the triangle
     */
    private static double triangleArea(double x0, double y0,
                                       double x1, double y1,
                                       double x2, double y2) {

        // Convert vertices to vectors.
        double a = x0 - x1;
        double b = y0 - y1;
        double e = x0 - x2;
        double f = y0 - y2;
        double area = ((a * a + b * b) * (e * e + f * f))
                      - ((a * e + b * f) * (a * e + b * f));

        if (area <= 0) {
            return 0; // Roundoff presumably!
        } else {
            return Math.sqrt(area) / 2;
        }
    }


    /**
     * Clip a polygon to a half-plane bounded by a vertical line.
     * Just flip the input axis order to clip by a horizontal line.
     * Invert ycross cals to allow for video coord system
     * <p/>
     * This function uses pre-allocated arrays for output so that no
     * objects need be generated during a call.
     *
     * @param n   number of vertices in the polygon
     * @param x   X coordinates of the vertices
     * @param y   Y coordinates of the vertices
     * @param nx  new X coordinates
     * @param ny  new Y coordinates
     * @param val value at which clipping is to occur
     * @param dir direction for which data is to be clipped.
     *            true-> clip below val, false->clip above val.
     *
     * @return the number of new vertices.
     */
    private static int lineClip(int n,
                                double[] x,
                                double[] y,
                                double[] nx,
                                double[] ny,
                                int val,
                                boolean dir) {
        int nout = 0;

// Need to handle first segment specially
// since we don't want to duplicate vertices.
        boolean last = inPlane(x[n - 1], val, dir);

        for (int i = 0; i < n; i += 1) {
            if (last) {
                if (inPlane(x[i], val, dir)) {
                    // Both endpoints in, just add the new point
                    nx[nout] = x[i];
                    ny[nout] = y[i];
                    nout += 1;
                } else {
                    double ycross;
// Move out of the clip region, add the point we moved out
                    if (i == 0) {
                        ycross = y[0] - (y[0] - y[n - 1]) * (x[0] - val) / (x[0] - x[n - 1]);
                    } else {
                        ycross = y[i] - (y[i] - y[i - 1]) * (x[i] - val) / (x[i] - x[i - 1]);
                    }
                    nx[nout] = val;
                    ny[nout] = ycross;
                    nout += 1;
                    last = false;
                }
            } else {
                if (inPlane(x[i], val, dir)) {
// Moved into the clip region.  Add the point
// we moved in, and the end point.
                    double ycross;
                    if (i == 0) {
                        ycross = y[0] - (y[0] - y[n - 1]) * (x[0] - val) / (x[i] - x[n - 1]);
                    } else {
                        ycross = y[i] - (y[i] - y[i - 1]) * (x[i] - val) / (x[i] - x[i - 1]);
                    }
                    nx[nout] = val;
                    ny[nout] = ycross;
                    nout += 1;

                    nx[nout] = x[i];
                    ny[nout] = y[i];
                    nout += 1;
                    last = true;

                } else {
// Segment entirely clipped.
                }
            }
        }
        return nout;
    }

    /**
     * Is the test value on the on the proper side of a line.
     *
     * @param test      value to be tested
     * @param divider   critical value
     * @param direction 'true' if values greater than divider are 'in'
     *                  'false' if smaller values are 'in'.
     *
     * @return is the value on the right side of the divider?
     */
    private static boolean inPlane(double test,
                                   int divider,
                                   boolean direction) {
        if (direction) {
            return test >= divider;
        } else {
            return test <= divider;
        }
    }

// Intermediate storage used by rectClip.
// The maximum number of vertices we will get if we start with
// a convex quadrilateral is 12, but we use larger
// arrays in case this is used is some other context.

    private double[] rcX0 = new double[100];
    private double[] rcX1 = new double[100];
    private double[] rcY0 = new double[100];
    private double[] rcY1 = new double[100];

    /**
     * Clip a polygon by a non-rotated rectangle.
     * <p/>
     * This uses a simplified version of the Sutherland-Hodgeman polygon
     * clipping method.  We assume that the region to be clipped is
     * convex.  This implies that we will not need to worry about
     * the clipping breaking the input region into multiple
     * disconnected areas.
     * [Proof: Suppose the resulting region is not convex.  Then
     * there is a line between two points in the region that
     * crosses the boundary of the clipped region.  However the
     * clipped boundaries are all lines from one of the two
     * figures we are intersecting.  This would imply that
     * this line crosses one of the boundaries in the original
     * image.  Hence either the original polygon or the clipping
     * region would need to be non-convex.]
     * <p/>
     * Private arrays are used for intermediate results to minimize
     * allocation costs.
     *
     * @param n    Number of vertices in the polygon.
     * @param x    X values of vertices
     * @param y    Y values of vertices
     * @param nx   X values of clipped polygon
     * @param ny   Y values of clipped polygon
     * @param minX Minimum X-value
     * @param maxX MAximum X-value
     * @param maxY Maximum Y-value
     * @param minY Minimum Y-value
     *
     * @return Number of vertices in clipped polygon.
     */
    private int rectClip(int n, double[] x, double[] y, double[] nx, double[] ny,
                         int minX, int minY, int maxX, int maxY) {

        int nCurr;

// lineClip is called four times, once for each constraint.
// Note the inversion of order of the arguments when
// clipping vertically.

        nCurr = lineClip(n, x, y, rcX0, rcY0, minX, true);

        if (nCurr > 0) {
            nCurr = lineClip(nCurr, rcX0, rcY0, rcX1, rcY1, maxX, false);

            if (nCurr > 0) {
                nCurr = lineClip(nCurr, rcY1, rcX1, rcY0, rcX0, minY, true);

                if (nCurr > 0) {
                    nCurr = lineClip(nCurr, rcY0, rcX0, ny, nx, maxY, false);
                }
            }
        }

// We don't need to worry about how we got here.
// If nCurr == 0, then it doesn't matter that
// we haven't set nx and ny.  And if it is then we've gone
// all the way and they are already set.
        return nCurr;
    }


    /**
     * Debugging routine that prints a list of vertices.
     *
     * @param n the number of vertices in the polygon
     * @param x X coordinates
     * @param y Y coordinates
     */
    private static void printVert(int n, double[] x, double[] y, String label) {

        for (int i = 0; i < n; i += 1) {
            System.out.println(label + "   " + x[i] + "  " + y[i]);
        }
    }

}
