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

package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.TiePointGridOpImage;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.IndexValidator;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.util.math.Range;

import java.awt.image.RenderedImage;
import java.io.IOException;

/**
 * A tie-point grid contains the data for geophysical parameter in remote sensing data products. Tie-point grid are
 * two-dimensional images which hold their pixel values (samples) in a <code>float</code> array. <p/>
 * <p/>
 * Usually, tie-point grids are a sub-sampling of a data product's scene resolution.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class TiePointGrid extends RasterDataNode {

    /**
     * Tie point values are assumed to have none discontinuities.
     */
    public static int DISCONT_NONE = 0;

    /**
     * Tie point values have angles in the range -180...+180 degrees and may comprise a discontinuity at 180 (resp.
     * -180) degrees.
     */
    public static int DISCONT_AT_180 = 180;

    /**
     * Tie point values have are angles in the range 0...+360 degrees and may comprise a discontinuity at 360 (resp. 0)
     * degrees.
     */
    public static int DISCONT_AT_360 = 360;

    private final float offsetX;
    private final float offsetY;
    private final float subSamplingX;
    private final float subSamplingY;

    private int discontinuity;
    private volatile TiePointGrid sinGrid;
    private volatile TiePointGrid cosGrid;

    /**
     * Constructs a new <code>TiePointGrid</code> with the given tie point grid properties.
     *
     * @param name         the name of the new object
     * @param gridWidth    the width of the tie-point grid in pixels
     * @param gridHeight   the height of the tie-point grid in pixels
     * @param offsetX      the X co-ordinate of the first (upper-left) tie-point in pixels
     * @param offsetY      the Y co-ordinate of the first (upper-left) tie-point in pixels
     * @param subSamplingX the sub-sampling in X-direction given in the pixel co-ordinates of the data product to which
     *                     this tie-pint grid belongs to. Must not be less than one.
     * @param subSamplingY the sub-sampling in X-direction given in the pixel co-ordinates of the data product to which
     *                     this tie-pint grid belongs to. Must not be less than one.
     * @param tiePoints    the tie-point data values, must be an array of the size <code>gridWidth * gridHeight</code>
     */
    public TiePointGrid(String name,
                        int gridWidth,
                        int gridHeight,
                        float offsetX,
                        float offsetY,
                        float subSamplingX,
                        float subSamplingY,
                        float[] tiePoints) {
        this(name, gridWidth, gridHeight, offsetX, offsetY, subSamplingX, subSamplingY, tiePoints, DISCONT_NONE);
    }

    /**
     * Constructs a new <code>TiePointGrid</code> with the given tie point grid properties.
     *
     * @param name          the name of the new object
     * @param gridWidth     the width of the tie-point grid in pixels
     * @param gridHeight    the height of the tie-point grid in pixels
     * @param offsetX       the X co-ordinate of the first (upper-left) tie-point in pixels
     * @param offsetY       the Y co-ordinate of the first (upper-left) tie-point in pixels
     * @param subSamplingX  the sub-sampling in X-direction given in the pixel co-ordinates of the data product to which
     *                      this tie-pint grid belongs to. Must not be less than one.
     * @param subSamplingY  the sub-sampling in X-direction given in the pixel co-ordinates of the data product to which
     *                      this tie-pint grid belongs to. Must not be less than one.
     * @param tiePoints     the tie-point data values, must be an array of the size <code>gridWidth * gridHeight</code>
     * @param discontinuity the discontinuity mode, can be either {@link #DISCONT_NONE} or {@link #DISCONT_AT_180}
     *                      {@link #DISCONT_AT_360}
     */
    public TiePointGrid(String name,
                        int gridWidth,
                        int gridHeight,
                        float offsetX,
                        float offsetY,
                        float subSamplingX,
                        float subSamplingY,
                        float[] tiePoints,
                        int discontinuity) {
        super(name, ProductData.TYPE_FLOAT32, gridWidth, gridHeight);
        Guardian.assertNotNull("tiePoints", tiePoints);

        if (discontinuity != DISCONT_NONE && discontinuity != DISCONT_AT_180 && discontinuity != DISCONT_AT_360) {
            throw new IllegalArgumentException("unsupported discontinuity mode");
        }
        this.discontinuity = discontinuity;

        if (tiePoints.length != gridWidth * gridHeight) {
            throw new IllegalArgumentException("data array size does not match 'gridWidth' x 'gridHeight'");
        }
        if (subSamplingX <= 0.0F) {
            throw new IllegalArgumentException("'subSamplingX' is less or equal zero");
        }
        if (subSamplingY <= 0.0F) {
            throw new IllegalArgumentException("'subSamplingY' is less or equal zero");
        }
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.subSamplingX = subSamplingX;
        this.subSamplingY = subSamplingY;

        setData(ProductData.createInstance(tiePoints));
    }

    /**
     * Constructs a new <code>TiePointGrid</code> with the given tie point grid properties.
     *
     * @param name           the name of the new object
     * @param gridWidth      the width of the tie-point grid in pixels
     * @param gridHeight     the height of the tie-point grid in pixels
     * @param offsetX        the X co-ordinate of the first (upper-left) tie-point in pixels
     * @param offsetY        the Y co-ordinate of the first (upper-left) tie-point in pixels
     * @param subSamplingX   the sub-sampling in X-direction given in the pixel co-ordinates of the data product to which
     *                       this tie-pint grid belongs to. Must not be less than one.
     * @param subSamplingY   the sub-sampling in X-direction given in the pixel co-ordinates of the data product to which
     *                       this tie-pint grid belongs to. Must not be less than one.
     * @param tiePoints      the tie-point data values, must be an array of the size <code>gridWidth * gridHeight</code>
     * @param containsAngles if true, the {@link #getDiscontinuity() angular discontinuity} is derived from the provided tie-point data values
     */
    public TiePointGrid(String name,
                        int gridWidth,
                        int gridHeight,
                        float offsetX,
                        float offsetY,
                        float subSamplingX,
                        float subSamplingY,
                        float[] tiePoints,
                        boolean containsAngles) {
        this(name, gridWidth, gridHeight, offsetX, offsetY, subSamplingX, subSamplingY, tiePoints,
             containsAngles ? getDiscontinuity(tiePoints) : DISCONT_NONE);
    }

    /**
     * Determines the angular discontinuity of the given tie point values.
     *
     * @return the angular discontinuity, will always be either {@link #DISCONT_AT_180} or
     *         {@link #DISCONT_AT_360}
     */
    public static int getDiscontinuity(float[] tiePoints) {
        final Range range = Range.computeRangeFloat(tiePoints, IndexValidator.TRUE, null, ProgressMonitor.NULL);
        if (range.getMax() > 180.0) {
            return DISCONT_AT_360;
        } else {
            return DISCONT_AT_180;
        }
    }

    /**
     * Gets the angular discontinuity.
     *
     * @return the angular discontinuity, will always be either {@link #DISCONT_NONE} or {@link #DISCONT_AT_180} or
     *         {@link #DISCONT_AT_360}
     */
    public int getDiscontinuity() {
        return discontinuity;
    }

    /**
     * Sets the angular discontinuity.
     *
     * @param discontinuity angular discontinuity, can be either {@link #DISCONT_NONE} or {@link #DISCONT_AT_180} or
     *                      {@link #DISCONT_AT_360}
     */
    public void setDiscontinuity(final int discontinuity) {
        if (discontinuity != DISCONT_NONE && discontinuity != DISCONT_AT_180 && discontinuity != DISCONT_AT_360) {
            throw new IllegalArgumentException("unsupported discontinuity mode");
        }
        this.discontinuity = discontinuity;
    }

    /**
     * Returns <code>true</code>
     *
     * @return true
     */
    @Override
    public boolean isFloatingPointType() {
        return true;
    }

    /**
     * Returns the geophysical data type of this <code>RasterDataNode</code>. The value retuned is always one of the
     * <code>ProductData.TYPE_XXX</code> constants.
     *
     * @return the geophysical data type
     *
     * @see ProductData
     */
    @Override
    public int getGeophysicalDataType() {
        return ProductData.TYPE_FLOAT32;
    }

    /**
     * Gets a raster data holding this tie-point's interpolated pixel data for an entire product scene. <p/>
     * <p/>
     * In opposite to the <code>getRasterData</code> method, this method returns raster data that has at least
     * <code>getBandOutputRasterWidth()*getBandOutputRasterHeight()</code> elements of the given data type to store
     * the scene's pixels.
     *
     * @return raster data covering the pixels for a complete scene
     *
     * @see #getRasterData
     * @see #getRasterWidth
     * @see #getRasterHeight
     * @see #getSceneRasterWidth
     * @see #getSceneRasterHeight
     */
    @Override
    public ProductData getSceneRasterData() {
        int width = getSceneRasterWidth();
        int height = getSceneRasterHeight();
        ProductData data = createCompatibleRasterData(width, height);
        final float[] elems = (float[]) data.getElems();
        // getPixels will interpolate between tie points
        getPixels(0, 0, width, height, elems, ProgressMonitor.NULL);
        return data;
    }

    /**
     * Returns the width in pixels of the scene represented by this tie-point grid. The value returned is
     * <code>(getRasterWidth() - 1) * getSubSamplingX() + 1</code>
     *
     * @return the scene width in pixels
     */
    @Override
    public int getSceneRasterWidth() {
        if (getProduct() != null) {
            return getProduct().getSceneRasterWidth();
        } else {
            return Math.round((getRasterWidth() - 1) * getSubSamplingX() + 1);
        }
    }

    /**
     * Returns the height in pixels of the scene represented by this tie-point grid. The value returned is
     * <code>(getRasterHeight() - 1) * getSubSamplingY() + 1</code>
     *
     * @return the scene height in pixels
     */
    @Override
    public int getSceneRasterHeight() {
        if (getProduct() != null) {
            return getProduct().getSceneRasterHeight();
        } else {
            return Math.round((getRasterHeight() - 1) * getSubSamplingY() + 1);
        }
    }

    /**
     * Retrieves the x co-ordinate of the first (upper-left) tie-point in pixels.
     */
    public float getOffsetX() {
        return offsetX;
    }

    /**
     * Retrieves the y co-ordinate of the first (upper-left) tie-point in pixels.
     */
    public float getOffsetY() {
        return offsetY;
    }

    /**
     * Returns the sub-sampling in X-direction given in the pixel co-ordinates of the data product to which this
     * tie-pint grid belongs to.
     *
     * @return the sub-sampling in X-direction, never less than one.
     */
    public float getSubSamplingX() {
        return subSamplingX;
    }

    /**
     * Returns the sub-sampling in Y-direction given in the pixel co-ordinates of the data product to which this
     * tie-pint grid belongs to.
     *
     * @return the sub-sampling in Y-direction, never less than one.
     */
    public float getSubSamplingY() {
        return subSamplingY;
    }

    /**
     * Gets the data array holding this band's pixel samples.
     *
     * @return the data array for this band, or <code>null</code> if no data has been loaded
     *
     * @see ProductData#getElems
     */
    public float[] getTiePoints() {
        return (float[]) getRasterData().getElems();
    }

    /**
     * Gets the interpolated sample for the pixel located at (x,y) as an integer value. <p/>
     * <p/>
     * If the pixel co-odinates given by (x,y) are not covered by this tie-point grid, the method extrapolates.
     *
     * @param x The X co-ordinate of the pixel location
     * @param y The Y co-ordinate of the pixel location
     *
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     */
    @Override
    public int getPixelInt(int x, int y) {
        return Math.round(getPixelFloat(x, y));
    }

    @Override
    public void dispose() {
        if (cosGrid != null) {
            cosGrid.dispose();
            cosGrid = null;
        }
        if (sinGrid != null) {
            sinGrid.dispose();
            sinGrid = null;
        }
        super.dispose();
    }

    /**
     * Computes the interpolated sample for the pixel located at (x,y). <p/>
     * <p/>
     * If the pixel co-odinates given by (x,y) are not covered by this tie-point grid, the method extrapolates.
     *
     * @param x The X co-ordinate of the pixel location, given in the pixel co-ordinates of the data product to which
     *          this tie-pint grid belongs to.
     * @param y The Y co-ordinate of the pixel location, given in the pixel co-ordinates of the data product to which
     *          this tie-pint grid belongs to.
     *
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     */
    @Override
    public float getPixelFloat(int x, int y) {
        return getPixelFloat(x + 0.5f, y + 0.5f);
    }

    /**
     * Computes the interpolated sample for the pixel located at (x,y) given as floating point co-ordinates. <p/>
     * <p/>
     * If the pixel co-odinates given by (x,y) are not covered by this tie-point grid, the method extrapolates.
     *
     * @param x The X co-ordinate of the pixel location, given in the pixel co-ordinates of the data product to which
     *          this tie-pint grid belongs to.
     * @param y The Y co-ordinate of the pixel location, given in the pixel co-ordinates of the data product to which
     *          this tie-pint grid belongs to.
     *
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     */
    public float getPixelFloat(float x, float y) {
        if (discontinuity != DISCONT_NONE) {
            if (isDiscontNotInit()) {
                initDiscont();
            }
            final float sinAngle = sinGrid.getPixelFloat(x, y);
            final float cosAngle = cosGrid.getPixelFloat(x, y);
            final float v = (float) (MathUtils.RTOD * Math.atan2(sinAngle, cosAngle));
            if (discontinuity == DISCONT_AT_360 && v < 0.0) {
                return 360.0F + v;  // = 180 + (180 - abs(v))
            }
            return v;
        }
        float fi = (x - offsetX) / subSamplingX;
        float fj = (y - offsetY) / subSamplingY;
        final int i = MathUtils.floorAndCrop(fi, 0, getRasterWidth() - 2);
        final int j = MathUtils.floorAndCrop(fj, 0, getRasterHeight() - 2);
        return interpolate(fi - i, fj - j, i, j);
    }

    /**
     * Gets the interpolated sample for the pixel located at (x,y) as a double value. <p/>
     * <p/>
     * If the pixel co-ordinates given by (x,y) are not covered by this tie-point grid, the method extrapolates.
     *
     * @param x The X co-ordinate of the pixel location, given in the pixel co-ordinates of the data product to which
     *          this tie-pint grid belongs to.
     * @param y The Y co-ordinate of the pixel location, given in the pixel co-ordinates of the data product to which
     *          this tie-pint grid belongs to.
     *
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     */
    @Override
    public double getPixelDouble(int x, int y) {
        return getPixelFloat(x, y);
    }

    /**
     * This method is not implemented because pixels are read-only in tie-point grids.
     */
    @Override
    public void setPixelInt(int x, int y, int pixelValue) {
        raisePixelsAreReadOnlyError();
    }

    /**
     * This method is not implemented because pixels are read-only in tie-point grids.
     */
    @Override
    public void setPixelFloat(int x, int y, float pixelValue) {
        raisePixelsAreReadOnlyError();
    }

    /**
     * This method is not implemented because pixels are read-only in tie-point grids.
     */
    @Override
    public void setPixelDouble(int x, int y, double pixelValue) {
        raisePixelsAreReadOnlyError();
    }

    /**
     * Retrieves an array of tie point data interpolated to the product with and height as integer array. If the given
     * array is <code>null</code> a new one was created and returned.
     *
     * @param x      the x coordinate of the array to be read
     * @param y      the y coordinate of the array to be read
     * @param w      the width of the array to be read
     * @param h      the height of the array to be read
     * @param pixels the integer array to be filled with data
     * @param pm     a monitor to inform the user about progress
     *
     * @throws IllegalArgumentException if the length of the given array is less than <code>w*h</code>.
     */
    @Override
    public int[] getPixels(int x, int y, int w, int h, int[] pixels, ProgressMonitor pm) {
        pixels = ensureMinLengthArray(pixels, w * h);
        float[] fpixels = getPixels(x, y, w, h, (float[]) null, pm);
        for (int i = 0; i < fpixels.length; i++) {
            pixels[i] = Math.round(fpixels[i]);
        }
        return pixels;
    }

    /**
     * Retrieves an array of tie point data interpolated to the product width and height as float array. If the given
     * array is <code>null</code> a new one is created and returned.
     *
     * @param x      the x coordinate of the array to be read
     * @param y      the y coordinate of the array to be read
     * @param w      the width of the array to be read
     * @param h      the height of the array to be read
     * @param pixels the float array to be filled with data
     * @param pm     a monitor to inform the user about progress
     *
     * @throws IllegalArgumentException if the length of the given array is less than <code>w*h</code>.
     */
    @Override
    public float[] getPixels(int x, int y, int w, int h, float[] pixels, ProgressMonitor pm) {
        pixels = ensureMinLengthArray(pixels, w * h);
        if (discontinuity != DISCONT_NONE) {
            if (isDiscontNotInit()) {
                initDiscont();
            }
            int i = 0;
            for (int yCoordinate = y; yCoordinate < y + h; yCoordinate++) {
                for (int xCoordinate = x; xCoordinate < x + w; xCoordinate++) {
                    pixels[i] = getPixelFloat(xCoordinate, yCoordinate);
                    i++;
                }
            }
        } else {
            final float x0 = 0.5f - offsetX;
            final float y0 = 0.5f - offsetY;
            final int x1 = x;
            final int y1 = y;
            final int x2 = x + w - 1;
            final int y2 = y + h - 1;
            final int ni = getRasterWidth();
            final int nj = getRasterHeight();
            int i, j;
            float fi, fj;
            float wi, wj;
            int pos = 0;
            for (y = y1; y <= y2; y++) {
                fj = (y + y0) / subSamplingY;
                j = MathUtils.floorAndCrop(fj, 0, nj - 2);
                wj = fj - j;
                for (x = x1; x <= x2; x++) {
                    fi = (x + x0) / subSamplingX;
                    i = MathUtils.floorAndCrop(fi, 0, ni - 2);
                    wi = fi - i;
                    pixels[pos++] = interpolate(wi, wj, i, j);
                }
            }
        }
        return pixels;
    }

    /**
     * Retrieves an array of tie point data interpolated to the product with and height as double array. If the given
     * array is <code>null</code> a new one was created and returned.
     *
     * @param x      the x coordinate of the array to be read
     * @param y      the y coordinate of the array to be read
     * @param w      the width of the array to be read
     * @param h      the height of the array to be read
     * @param pixels the double array to be filled with data
     *
     * @throws IllegalArgumentException if the length of the given array is less than <code>w*h</code>.
     */
    @Override
    public double[] getPixels(int x, int y, int w, int h, double[] pixels, ProgressMonitor pm) {
        pixels = ensureMinLengthArray(pixels, w * h);
        float[] fpixels = getPixels(x, y, w, h, (float[]) null, pm);
        for (int i = 0; i < fpixels.length; i++) {
            pixels[i] = fpixels[i];
        }
        return pixels;
    }

    /**
     * This method is not implemented because pixels are read-only in tie-point grids.
     */
    @Override
    public void setPixels(int x, int y, int w, int h, int[] pixels) {
        raisePixelsAreReadOnlyError();
    }

    /**
     * This method is not implemented because pixels are read-only in tie-point grids.
     */
    @Override
    public void setPixels(int x, int y, int w, int h, float[] pixels) {
        raisePixelsAreReadOnlyError();
    }

    /**
     * This method is not implemented because pixels are read-only in tie-point grids.
     */
    @Override
    public void setPixels(int x, int y, int w, int h, double[] pixels) {
        raisePixelsAreReadOnlyError();
    }

    /**
     * Retrieves an array of tie point data interpolated to the product with and height as float array. If the given
     * array is <code>null</code> a new one was created and returned.
     *
     * @param x      the x coordinate of the array to be read
     * @param y      the y coordinate of the array to be read
     * @param w      the width of the array to be read
     * @param h      the height of the array to be read
     * @param pixels the integer array to be filled with data
     *
     * @throws IllegalArgumentException if the length of the given array is less than <code>w*h</code>.
     */
    @Override
    public int[] readPixels(int x, int y, int w, int h, int[] pixels, ProgressMonitor pm) throws IOException {
        return getPixels(x, y, w, h, pixels, pm);
    }

    /**
     * Retrieves an array of tie point data interpolated to the product with and height as float array. If the given
     * array is <code>null</code> a new one was created and returned. *
     *
     * @param x      the x coordinate of the array to be read
     * @param y      the y coordinate of the array to be read
     * @param w      the width of the array to be read
     * @param h      the height of the array to be read
     * @param pixels the float array to be filled with data
     * @param pm     a monitor to inform the user about progress
     *
     * @throws IllegalArgumentException if the length of the given array is less than <code>w*h</code>.
     */
    @Override
    public float[] readPixels(int x, int y, int w, int h, float[] pixels, ProgressMonitor pm) throws IOException {
        return getPixels(x, y, w, h, pixels, pm);
    }

    /**
     * Retrieves an array of tie point data interpolated to the product with and height as double array. If the given
     * array is <code>null</code> a new one was created and returned.
     *
     * @param x      the x coordinate of the array to be read
     * @param y      the y coordinate of the array to be read
     * @param w      the width of the array to be read
     * @param h      the height of the array to be read
     * @param pixels the double array to be filled with data
     * @param pm     a monitor to inform the user about progress
     *
     * @throws IllegalArgumentException if the length of the given array is less than <code>w*h</code>.
     */
    @Override
    public double[] readPixels(int x, int y, int w, int h, double[] pixels, ProgressMonitor pm) throws IOException {
        return getPixels(x, y, w, h, pixels, pm);
    }
    
    /**
     * This method is not implemented because pixels are read-only in tie-point grids.
     */
    @Override
    public void writePixels(int x, int y, int w, int h, int[] pixels, ProgressMonitor pm) throws IOException {
        raisePixelsAreReadOnlyError();
    }

    /**
     * This method is not implemented because pixels are read-only in tie-point grids.
     */
    @Override
    public void writePixels(int x, int y, int w, int h, float[] pixels, ProgressMonitor pm) throws IOException {
        raisePixelsAreReadOnlyError();
    }

    /**
     * This method is not implemented because pixels are read-only in tie-point grids.
     */
    @Override
    public void writePixels(int x, int y, int w, int h, double[] pixels, ProgressMonitor pm) throws IOException {
        raisePixelsAreReadOnlyError();
    }

    /**
     * Reads raster data from this dataset into the user-supplied raster data buffer. <p/>
     * <p/>
     * This method always directly (re-)reads this band's data from its associated data source into the given data
     * buffer.
     *
     * @param offsetX    the X-offset in the raster co-ordinates where reading starts
     * @param offsetY    the Y-offset in the raster co-ordinates where reading starts
     * @param width      the width of the raster data buffer
     * @param height     the height of the raster data buffer
     * @param rasterData a raster data buffer receiving the pixels to be read
     * @param pm         a monitor to inform the user about progress
     *
     * @throws java.io.IOException      if an I/O error occurs
     * @throws IllegalArgumentException if the raster is null
     * @throws IllegalStateException    if this product raster was not added to a product so far, or if the product to
     *                                  which this product raster belongs to, has no associated product reader
     * @see org.esa.beam.framework.dataio.ProductReader#readBandRasterData(Band, int, int, int, int, ProductData, com.bc.ceres.core.ProgressMonitor) 
     */
    @Override
    public void readRasterData(int offsetX, int offsetY, int width, int height, ProductData rasterData,
                               ProgressMonitor pm) throws IOException {
        final ProductData src = getRasterData();
        int iSrc;
        int iDest = 0;
        pm.beginTask("Reading raster data...", height);
        try {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    iSrc = (offsetY + y) * width + (offsetX + x);
                    rasterData.setElemDoubleAt(iDest, src.getElemDoubleAt(iSrc));
                    iDest++;
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readRasterDataFully(ProgressMonitor pm) throws IOException {
        // ok, raster data is already loaded in tie-point grids
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeRasterData(int offsetX, int offsetY,
                                int width, int height,
                                ProductData rasterData, ProgressMonitor pm) throws IOException {
        raisePixelsAreReadOnlyError();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeRasterDataFully(ProgressMonitor pm) throws IOException {
        raisePixelsAreReadOnlyError();
    }
    
    @Override
    protected RenderedImage createSourceImage() {
        final MultiLevelModel model = ImageManager.getMultiLevelModel(this);
        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(model) {

            @Override
            public RenderedImage createImage(int level) {
                return new TiePointGridOpImage(TiePointGrid.this,
                                               ResolutionLevel.create(getModel(), level));
            }
        });
    }

    // ////////////////////////////////////////////////////////////////////////
    // 'Visitor' pattern support

    /**
     * Accepts the given visitor. This method implements the well known 'Visitor' design pattern of the gang-of-four.
     * The visitor pattern allows to define new operations on the product data model without the need to add more code
     * to it. The new operation is implemented by the visitor. <p/>
     * <p/>
     * The method simply calls <code>visitor.visit(this)</code>.
     *
     * @param visitor the visitor
     */
    @Override
    public void acceptVisitor(ProductVisitor visitor) {
        Guardian.assertNotNull("visitor", visitor);
        visitor.visit(this);
    }

    public TiePointGrid cloneTiePointGrid() {
        final float[] srcTiePoints = this.getTiePoints();
        final float[] destTiePoints = new float[srcTiePoints.length];
        System.arraycopy(srcTiePoints, 0, destTiePoints, 0, srcTiePoints.length);
        TiePointGrid clone = new TiePointGrid(this.getName(),
                                              this.getRasterWidth(),
                                              this.getRasterHeight(),
                                              this.getOffsetX(),
                                              this.getOffsetY(),
                                              this.getSubSamplingX(),
                                              this.getSubSamplingY(),
                                              destTiePoints,
                                              this.getDiscontinuity());
        clone.setUnit(getUnit());
        clone.setDescription(getDescription());
        return clone;

    }

    // ////////////////////////////////////////////////////////////////////////
    // Public static helpers

    public static TiePointGrid createZenithFromElevationAngleTiePointGrid(TiePointGrid elevationAngleGrid) {
        final float[] elevationAngles = elevationAngleGrid.getTiePoints();
        final float[] zenithAngles = new float[elevationAngles.length];
        for (int i = 0; i < zenithAngles.length; i++) {
            zenithAngles[i] = 90.0f - elevationAngles[i];
        }
        return new TiePointGrid(elevationAngleGrid.getName(),
                                elevationAngleGrid.getRasterWidth(),
                                elevationAngleGrid.getRasterHeight(),
                                elevationAngleGrid.getOffsetX(),
                                elevationAngleGrid.getOffsetY(),
                                elevationAngleGrid.getSubSamplingX(),
                                elevationAngleGrid.getSubSamplingY(),
                                zenithAngles);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Implementation helpers

    private static void raisePixelsAreReadOnlyError() {
        throw new IllegalStateException("pixels are read-only in tie-point grids");
    }

    private float interpolate(float wi, float wj, int i0, int j0) {
        final float[] tiePoints = getTiePoints();
        final int w = getRasterWidth();
        final int i1 = i0 + 1;
        final int j1 = j0 + 1;
        return MathUtils.interpolate2D(wi,
                                       wj,
                                       tiePoints[i0 + j0 * w],
                                       tiePoints[i1 + j0 * w],
                                       tiePoints[i0 + j1 * w],
                                       tiePoints[i1 + j1 * w]);

    }

    private boolean isDiscontNotInit() {
        return sinGrid == null || cosGrid == null;
    }

    private void initDiscont() {
        TiePointGrid base = this;
        final float[] tiePoints = base.getTiePoints();
        final float[] sinTiePoints = new float[tiePoints.length];
        final float[] cosTiePoints = new float[tiePoints.length];
        for (int i = 0; i < tiePoints.length; i++) {
            float tiePoint = tiePoints[i];
            sinTiePoints[i] = (float) Math.sin(MathUtils.DTOR * tiePoint);
            cosTiePoints[i] = (float) Math.cos(MathUtils.DTOR * tiePoint);
        }
        sinGrid = new TiePointGrid(base.getName(),
                                    base.getRasterWidth(),
                                    base.getRasterHeight(),
                                    base.getOffsetX(),
                                    base.getOffsetY(),
                                    base.getSubSamplingX(),
                                    base.getSubSamplingY(),
                                    sinTiePoints);
        cosGrid = new TiePointGrid(base.getName(),
                                    base.getRasterWidth(),
                                    base.getRasterHeight(),
                                    base.getOffsetX(),
                                    base.getOffsetY(),
                                    base.getSubSamplingX(),
                                    base.getSubSamplingY(),
                                    cosTiePoints);
    }

    protected static int[] ensureMinLengthArray(int[] array, int length) {
        if (array == null) {
            return new int[length];
        }
        if (array.length < length) {
            throw new IllegalArgumentException("The length of the given array is less than " + length);
        }
        return array;
    }

    protected static float[] ensureMinLengthArray(float[] array, int length) {
        if (array == null) {
            return new float[length];
        }
        if (array.length < length) {
            throw new IllegalArgumentException("The length of the given array is less than " + length);
        }
        return array;
    }

    protected static double[] ensureMinLengthArray(double[] array, int length) {
        if (array == null) {
            return new double[length];
        }
        if (array.length < length) {
            throw new IllegalArgumentException("The length of the given array is less than " + length);
        }
        return array;
    }


    public static TiePointGrid createSubset(TiePointGrid sourceTiePointGrid, ProductSubsetDef subsetDef) {
        final int srcTPGRasterWidth = sourceTiePointGrid.getRasterWidth();
        final int srcTPGRasterHeight = sourceTiePointGrid.getRasterHeight();
        final float srcTPGSubSamplingX = sourceTiePointGrid.getSubSamplingX();
        final float srcTPGSubSamplingY = sourceTiePointGrid.getSubSamplingY();
        int subsetOffsetX = 0;
        int subsetOffsetY = 0;
        int subsetStepX = 1;
        int subsetStepY = 1;
        final int srcSceneRasterWidth = sourceTiePointGrid.getSceneRasterWidth();
        final int srcSceneRasterHeight = sourceTiePointGrid.getSceneRasterHeight();
        int subsetWidth = srcSceneRasterWidth;
        int subsetHeight = srcSceneRasterHeight;
        if (subsetDef != null) {
            subsetStepX = subsetDef.getSubSamplingX();
            subsetStepY = subsetDef.getSubSamplingY();
            if (subsetDef.getRegion() != null) {
                subsetOffsetX = subsetDef.getRegion().x;
                subsetOffsetY = subsetDef.getRegion().y;
                subsetWidth = subsetDef.getRegion().width;
                subsetHeight = subsetDef.getRegion().height;
            }
        }

        final float newTPGSubSamplingX = srcTPGSubSamplingX / subsetStepX;
        final float newTPGSubSamplingY = srcTPGSubSamplingY / subsetStepY;
        final float pixelCenter = 0.5f;
        final float newTPGOffsetX = (sourceTiePointGrid.getOffsetX() - pixelCenter - subsetOffsetX) / subsetStepX + pixelCenter;
        final float newTPGOffsetY = (sourceTiePointGrid.getOffsetY() - pixelCenter - subsetOffsetY) / subsetStepY + pixelCenter;
        final float newOffsetX = newTPGOffsetX % newTPGSubSamplingX;
        final float newOffsetY = newTPGOffsetY % newTPGSubSamplingY;
        final float diffX = newOffsetX - newTPGOffsetX;
        final float diffY = newOffsetY - newTPGOffsetY;
        final int dataOffsetX;
        if (diffX < 0.0f) {
            dataOffsetX = 0;
        } else {
            dataOffsetX = Math.round(diffX / newTPGSubSamplingX);
        }
        final int dataOffsetY;
        if (diffY < 0.0f) {
            dataOffsetY = 0;
        } else {
            dataOffsetY = Math.round(diffY / newTPGSubSamplingY);
        }

        int newTPGWidth = (int) Math.ceil(subsetWidth / srcTPGSubSamplingX) + 2;
        if (dataOffsetX + newTPGWidth > srcTPGRasterWidth) {
            newTPGWidth = srcTPGRasterWidth - dataOffsetX;
        }
        int newTPGHeight = (int) Math.ceil(subsetHeight / srcTPGSubSamplingY) + 2;
        if (dataOffsetY + newTPGHeight > srcTPGRasterHeight) {
            newTPGHeight = srcTPGRasterHeight - dataOffsetY;
        }

        final float[] oldTiePoints = sourceTiePointGrid.getTiePoints();
        final float[] tiePoints = new float[newTPGWidth * newTPGHeight];
        for (int y = 0; y < newTPGHeight; y++) {
            final int srcPos = srcTPGRasterWidth * (dataOffsetY + y) + dataOffsetX;
            System.arraycopy(oldTiePoints, srcPos, tiePoints, y * newTPGWidth, newTPGWidth);
        }

        final TiePointGrid tiePointGrid = new TiePointGrid(sourceTiePointGrid.getName(),
                                                           newTPGWidth,
                                                           newTPGHeight,
                                                           newOffsetX,
                                                           newOffsetY,
                                                           newTPGSubSamplingX,
                                                           newTPGSubSamplingY,
                                                           tiePoints,
                                                           sourceTiePointGrid.getDiscontinuity());
        tiePointGrid.setUnit(sourceTiePointGrid.getUnit());
        tiePointGrid.setDescription(sourceTiePointGrid.getDescription());
        return tiePointGrid;
    }
}
