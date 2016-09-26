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

package org.esa.snap.core.datamodel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.TiePointGridOpImage;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.math.IndexValidator;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.core.util.math.Range;

import java.awt.image.RenderedImage;
import java.io.IOException;

/**
 * A tie-point grid contains the data for geophysical parameter in remote sensing data products. Tie-point grid are
 * two-dimensional images which hold their pixel values (samples) in a {@code float} array. <p>
 * <p>
 * Usually, tie-point grids are a sub-sampling of a data product's scene resolution.
 *
 * @author Norman Fomferra
 */
public class TiePointGrid extends RasterDataNode {

    /**
     * The discontinuity of the tie point values shall be detected automatically.
     */
    public static final int DISCONT_AUTO = -1;

    /**
     * Tie point values are assumed to have none discontinuities.
     */
    public static final int DISCONT_NONE = 0;

    /**
     * Tie point values have angles in the range -180...+180 degrees and may comprise a discontinuity at 180 (resp.
     * -180) degrees.
     */
    public static final int DISCONT_AT_180 = 180;

    /**
     * Tie point values have are angles in the range 0...+360 degrees and may comprise a discontinuity at 360 (resp. 0)
     * degrees.
     */
    public static final int DISCONT_AT_360 = 360;

    private final int gridWidth;
    private final int gridHeight;
    private final double offsetX;
    private final double offsetY;
    private final double subSamplingX;
    private final double subSamplingY;
    private int discontinuity;

    private volatile TiePointGrid sinGrid;
    private volatile TiePointGrid cosGrid;
    private volatile ProductData rasterData;


    /**
     * Constructs a new {@code TiePointGrid} with the given tie point grid properties.
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
     */
    public TiePointGrid(String name,
                        int gridWidth,
                        int gridHeight,
                        double offsetX,
                        double offsetY,
                        double subSamplingX,
                        double subSamplingY) {
        super(name, ProductData.TYPE_FLOAT32, gridWidth * gridHeight);
        Assert.argument(gridWidth >= 2, "gridWidth >= 2");
        Assert.argument(gridHeight >= 2, "gridHeight >= 2");
        Assert.argument(subSamplingX > 0.0F, "subSamplingX > 0.0");
        Assert.argument(subSamplingY > 0.0F, "subSamplingY > 0.0");

        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.subSamplingX = subSamplingX;
        this.subSamplingY = subSamplingY;
        this.discontinuity = DISCONT_NONE;
    }

    /**
     * Constructs a new {@code TiePointGrid} with the given tie point grid properties.
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
     * @param tiePoints    the tie-point data values, must be an array of the size {@code gridWidth * gridHeight}
     */
    public TiePointGrid(String name,
                        int gridWidth,
                        int gridHeight,
                        double offsetX,
                        double offsetY,
                        double subSamplingX,
                        double subSamplingY,
                        float[] tiePoints) {
        this(name, gridWidth, gridHeight, offsetX, offsetY, subSamplingX, subSamplingY, tiePoints, DISCONT_NONE);
    }

    /**
     * Constructs a new {@code TiePointGrid} with the given tie point grid properties.
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
     * @param tiePoints      the tie-point data values, must be an array of the size {@code gridWidth * gridHeight}
     * @param containsAngles if true, the {@link #getDiscontinuity() angular discontinuity} is derived from the provided tie-point data values
     */
    public TiePointGrid(String name,
                        int gridWidth,
                        int gridHeight,
                        double offsetX,
                        double offsetY,
                        double subSamplingX,
                        double subSamplingY,
                        float[] tiePoints,
                        boolean containsAngles) {
        this(name, gridWidth, gridHeight, offsetX, offsetY, subSamplingX, subSamplingY, tiePoints);
        Assert.argument(tiePoints.length == gridWidth * gridHeight, "tiePoints.length == gridWidth * gridHeight");
        if (containsAngles) {
            setDiscontinuity(getDiscontinuity(tiePoints));
        }
    }

    /**
     * Constructs a new {@code TiePointGrid} with the given tie point grid properties.
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
     * @param tiePoints     the tie-point data values, must be an array of the size {@code gridWidth * gridHeight}
     * @param discontinuity the discontinuity mode, can be either {@link #DISCONT_NONE}, {@link #DISCONT_AUTO}, {@link #DISCONT_AT_180} or
     *                      {@link #DISCONT_AT_360}
     */
    public TiePointGrid(String name,
                        int gridWidth,
                        int gridHeight,
                        double offsetX,
                        double offsetY,
                        double subSamplingX,
                        double subSamplingY,
                        float[] tiePoints,
                        int discontinuity) {
        this(name, gridWidth, gridHeight, offsetX, offsetY, subSamplingX, subSamplingY);
        Assert.argument(tiePoints.length == gridWidth * gridHeight, "tiePoints.length == gridWidth * gridHeight");
        Assert.argument(discontinuity == DISCONT_NONE ||
                        discontinuity == DISCONT_AT_180 || discontinuity == DISCONT_AT_360,
                        "discontinuity");
        this.discontinuity = discontinuity;
        setData(ProductData.createInstance(tiePoints));
    }

    /**
     * @return The grid's width (= number of columns).
     */
    public int getGridWidth() {
        return gridWidth;
    }

    /**
     * @return The grid's height (= number of rows).
     */
    public int getGridHeight() {
        return gridHeight;
    }

    /**
     * Retrieves the x co-ordinate of the first (upper-left) tie-point in pixels.
     */
    public double getOffsetX() {
        return offsetX;
    }

    /**
     * Retrieves the y co-ordinate of the first (upper-left) tie-point in pixels.
     */
    public double getOffsetY() {
        return offsetY;
    }

    /**
     * Returns the sub-sampling in X-direction given in the pixel co-ordinates of the data product to which this
     * tie-pint grid belongs to.
     *
     * @return the sub-sampling in X-direction, never less than one.
     */
    public double getSubSamplingX() {
        return subSamplingX;
    }

    /**
     * Returns the sub-sampling in Y-direction given in the pixel co-ordinates of the data product to which this
     * tie-pint grid belongs to.
     *
     * @return the sub-sampling in Y-direction, never less than one.
     */
    public double getSubSamplingY() {
        return subSamplingY;
    }

    /**
     * @return The data array representing the single tie-points.
     */
    public float[] getTiePoints() {
        return (float[]) getGridData().getElems();
    }

    /**
     * @return The data buffer representing the single tie-points.
     */
    public ProductData getGridData() {
        if (getData() == null) {
            try {
                setData(readGridData());
            } catch (IOException e) {
                SystemUtils.LOG.severe("Unable to load TPG: " + e.getMessage());
            }
        }

        return getData();
    }

    private ProductData readGridData() throws IOException {
        ProductData productData = createCompatibleRasterData(getGridWidth(), getGridHeight());
        getProductReader().readTiePointGridRasterData(this, 0, 0, getGridWidth(), getGridHeight(), productData,
                                                      ProgressMonitor.NULL);
        return productData;
    }

    /**
     * @return The native width of the raster in pixels.
     */
    @Override
    public int getRasterWidth() {
        if (getProduct() != null) {
            return getProduct().getSceneRasterWidth();
        } else {
            return (int) Math.round((getGridWidth() - 1) * getSubSamplingX() + 1);
        }
    }

    /**
     * @return The native height of the raster in pixels.
     */
    @Override
    public int getRasterHeight() {
        if (getProduct() != null) {
            return getProduct().getSceneRasterHeight();
        } else {
            return (int) Math.round((getGridHeight() - 1) * getSubSamplingY() + 1);
        }
    }

    /**
     * Determines the angular discontinuity of the given tie point values.
     *
     * @return the angular discontinuity, will always be either {@link #DISCONT_AT_180} or
     * {@link #DISCONT_AT_360}
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
     * @return the angular discontinuity, will always be either {@link #DISCONT_NONE}, {@link #DISCONT_AUTO}, {@link #DISCONT_AT_180} or
     * {@link #DISCONT_AT_360}
     */
    public int getDiscontinuity() {
        return discontinuity;
    }

    /**
     * Sets the angular discontinuity.
     *
     * @param discontinuity angular discontinuity, can be either {@link #DISCONT_NONE}, {@link #DISCONT_AUTO}, {@link #DISCONT_AT_180} or
     *                      {@link #DISCONT_AT_360}
     */
    public void setDiscontinuity(final int discontinuity) {
        if (discontinuity != DISCONT_NONE && discontinuity != DISCONT_AUTO &&
            discontinuity != DISCONT_AT_180 && discontinuity != DISCONT_AT_360) {
            throw new IllegalArgumentException("unsupported discontinuity mode");
        }
        this.discontinuity = discontinuity;
    }

    /**
     * Returns {@code true}
     *
     * @return true
     */
    @Override
    public boolean isFloatingPointType() {
        return true;
    }

    /**
     * Returns the geophysical data type of this {@code RasterDataNode}. The value returned is always one of the
     * {@code ProductData.TYPE_XXX} constants.
     *
     * @return the geophysical data type
     *
     * @see ProductData
     */
    @Override
    public int getGeophysicalDataType() {
        return ProductData.TYPE_FLOAT32;
    }


    @Override
    public void setData(ProductData data) {
        super.setData(data);
        if (getDiscontinuity() == DISCONT_AUTO) {
            setDiscontinuity(getDiscontinuity((float[]) data.getElems()));
        }
    }

    /**
     * Gets the linear interpolated raster data containing
     * {@link #getRasterWidth() rasterWidth} x {@link #getRasterHeight() rasterHeight} samples.
     *
     * @return The raster data for this tie-point grid.
     */
    @Override
    public ProductData getRasterData() {
        int width = getRasterWidth();
        int height = getRasterHeight();
        ProductData gridData = getGridData();
        // A tie-point grid's data may have the same dimensions as the requested raster data:
        // In this case we can simply return it instead of holding another one in this.rasterData.
        if (gridData.getNumElems() == width * height) {
            return gridData;
        }
        // Create a new one by interpolation.
        if (rasterData == null) {
            synchronized (this) {
                if (rasterData == null) {
                    rasterData = createCompatibleRasterData(width, height);
                    // getPixels will interpolate between tie points
                    getPixels(0, 0, width, height, (float[]) rasterData.getElems(), ProgressMonitor.NULL);
                }
            }
        }
        return rasterData;
    }

    /**
     * The method will always fail on tie-point grids as they are read-only.
     *
     * @param rasterData The raster data whose reference will be stored.
     */
    @Override
    public void setRasterData(ProductData rasterData) throws UnsupportedOperationException {
        throwPixelsAreReadOnlyException();
    }


    /**
     * Gets the interpolated sample for the pixel located at (x,y) as an integer value. <p>
     * <p>
     * If the pixel co-ordinates given by (x,y) are not covered by this tie-point grid, the method extrapolates.
     *
     * @param x The X co-ordinate of the pixel location
     * @param y The Y co-ordinate of the pixel location
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     */
    @Override
    public int getPixelInt(int x, int y) {
        return (int) Math.round(getPixelDouble(x, y));
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
     * Computes the interpolated sample for the pixel located at (x,y). <p>
     * <p>
     * If the pixel co-ordinates given by (x,y) are not covered by this tie-point grid, the method extrapolates.
     *
     * @param x The X co-ordinate of the pixel location, given in the pixel co-ordinates of the data product to which
     *          this tie-pint grid belongs to.
     * @param y The Y co-ordinate of the pixel location, given in the pixel co-ordinates of the data product to which
     *          this tie-pint grid belongs to.
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     */
    @Override
    public float getPixelFloat(int x, int y) {
        return (float) getPixelDouble(x + 0.5f, y + 0.5f);
    }

    /**
     * Computes the interpolated sample for the pixel located at (x,y) given as floating point co-ordinates. <p>
     * <p>
     * If the pixel co-ordinates given by (x,y) are not covered by this tie-point grid, the method extrapolates.
     *
     * @param x The X co-ordinate of the pixel location, given in the pixel co-ordinates of the data product to which
     *          this tie-pint grid belongs to.
     * @param y The Y co-ordinate of the pixel location, given in the pixel co-ordinates of the data product to which
     *          this tie-pint grid belongs to.
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     */
    public final float getPixelFloat(final float x, final float y) {
        return (float) getPixelDouble(x, y);
    }

    /**
     * Gets the interpolated sample for the pixel located at (x,y) as a double value. <p>
     * <p>
     * If the pixel co-ordinates given by (x,y) are not covered by this tie-point grid, the method extrapolates.
     *
     * @param x The X co-ordinate of the pixel location, given in the pixel co-ordinates of the data product to which
     *          this tie-pint grid belongs to.
     * @param y The Y co-ordinate of the pixel location, given in the pixel co-ordinates of the data product to which
     *          this tie-pint grid belongs to.
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     */
    @Override
    public double getPixelDouble(int x, int y) {
        return getPixelDouble(x + 0.5, y + 0.5);
    }

    /**
     * Gets the interpolated sample for the pixel located at (x,y) as a double value. <p>
     * <p>
     * If the pixel co-ordinates given by (x,y) are not covered by this tie-point grid, the method extrapolates.
     *
     * @param x The X co-ordinate of the pixel location, given in the pixel co-ordinates of the data product to which
     *          this tie-pint grid belongs to.
     * @param y The Y co-ordinate of the pixel location, given in the pixel co-ordinates of the data product to which
     *          this tie-pint grid belongs to.
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     */
    public double getPixelDouble(double x, double y) {
        if (discontinuity != DISCONT_NONE) {
            if (isDiscontNotInit()) {
                initDiscont();
            }
            final double sinAngle = sinGrid.getPixelDouble(x, y);
            final double cosAngle = cosGrid.getPixelDouble(x, y);
            final double v = MathUtils.RTOD * Math.atan2(sinAngle, cosAngle);
            if (discontinuity == DISCONT_AT_360 && v < 0.0) {
                return 360.0F + v;  // = 180 + (180 - abs(v))
            }
            return v;
        }
        double fi = (x - offsetX) / subSamplingX;
        double fj = (y - offsetY) / subSamplingY;
        final int i = MathUtils.floorAndCrop(fi, 0, getGridWidth() - 2);
        final int j = MathUtils.floorAndCrop(fj, 0, getGridHeight() - 2);
        return interpolate(fi - i, fj - j, i, j);
    }

    /**
     * This method is not implemented because pixels are read-only in tie-point grids.
     */
    @Override
    public void setPixelInt(int x, int y, int pixelValue) {
        throwPixelsAreReadOnlyException();
    }

    /**
     * This method is not implemented because pixels are read-only in tie-point grids.
     */
    @Override
    public void setPixelFloat(int x, int y, float pixelValue) {
        throwPixelsAreReadOnlyException();
    }

    /**
     * This method is not implemented because pixels are read-only in tie-point grids.
     */
    @Override
    public void setPixelDouble(int x, int y, double pixelValue) {
        throwPixelsAreReadOnlyException();
    }

    /**
     * Retrieves an array of tie point data interpolated to the product with and height as integer array. If the given
     * array is {@code null} a new one was created and returned.
     *
     * @param x      the x coordinate of the array to be read
     * @param y      the y coordinate of the array to be read
     * @param w      the width of the array to be read
     * @param h      the height of the array to be read
     * @param pixels the integer array to be filled with data
     * @param pm     a monitor to inform the user about progress
     * @throws IllegalArgumentException if the length of the given array is less than {@code w*h}.
     */
    @Override
    public int[] getPixels(int x, int y, int w, int h, int[] pixels, ProgressMonitor pm) {
        pixels = ensureMinLengthArray(pixels, w * h);
        double[] fpixels = getPixels(x, y, w, h, (double[]) null, pm);
        for (int i = 0; i < fpixels.length; i++) {
            pixels[i] = (int) Math.round(fpixels[i]);
        }
        return pixels;
    }

    /**
     * Retrieves an array of tie point data interpolated to the product width and height as float array. If the given
     * array is {@code null} a new one is created and returned.
     *
     * @param x      the x coordinate of the array to be read
     * @param y      the y coordinate of the array to be read
     * @param w      the width of the array to be read
     * @param h      the height of the array to be read
     * @param pixels the float array to be filled with data
     * @param pm     a monitor to inform the user about progress
     * @throws IllegalArgumentException if the length of the given array is less than {@code w*h}.
     */
    @Override
    public double[] getPixels(int x, int y, int w, int h, double[] pixels, ProgressMonitor pm) {
        pixels = ensureMinLengthArray(pixels, w * h);
        if (discontinuity != DISCONT_NONE) {
            if (isDiscontNotInit()) {
                initDiscont();
            }
            int i = 0;
            for (int yCoordinate = y; yCoordinate < y + h; yCoordinate++) {
                for (int xCoordinate = x; xCoordinate < x + w; xCoordinate++) {
                    pixels[i] = getPixelDouble(xCoordinate, yCoordinate);
                    i++;
                }
            }
        } else {
            final double x0 = 0.5f - offsetX;
            final double y0 = 0.5f - offsetY;
            final int x1 = x;
            final int y1 = y;
            final int x2 = x + w - 1;
            final int y2 = y + h - 1;
            final int ni = getGridWidth();
            final int nj = getGridHeight();
            int i, j;
            double fi, fj;
            double wi, wj;
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
     * array is {@code null} a new one was created and returned.
     *
     * @param x      the x coordinate of the array to be read
     * @param y      the y coordinate of the array to be read
     * @param w      the width of the array to be read
     * @param h      the height of the array to be read
     * @param pixels the double array to be filled with data
     * @throws IllegalArgumentException if the length of the given array is less than {@code w*h}.
     */
    @Override
    public float[] getPixels(int x, int y, int w, int h, float[] pixels, ProgressMonitor pm) {
        pixels = ensureMinLengthArray(pixels, w * h);
        double[] fpixels = getPixels(x, y, w, h, (double[]) null, pm);
        for (int i = 0; i < fpixels.length; i++) {
            pixels[i] = (float) fpixels[i];
        }
        return pixels;
    }

    /**
     * This method is not implemented because pixels are read-only in tie-point grids.
     */
    @Override
    public void setPixels(int x, int y, int w, int h, int[] pixels) {
        throwPixelsAreReadOnlyException();
    }

    /**
     * This method is not implemented because pixels are read-only in tie-point grids.
     */
    @Override
    public void setPixels(int x, int y, int w, int h, float[] pixels) {
        throwPixelsAreReadOnlyException();
    }

    /**
     * This method is not implemented because pixels are read-only in tie-point grids.
     */
    @Override
    public void setPixels(int x, int y, int w, int h, double[] pixels) {
        throwPixelsAreReadOnlyException();
    }

    /**
     * Retrieves an array of tie point data interpolated to the product with and height as float array. If the given
     * array is {@code null} a new one was created and returned.
     *
     * @param x      the x coordinate of the array to be read
     * @param y      the y coordinate of the array to be read
     * @param w      the width of the array to be read
     * @param h      the height of the array to be read
     * @param pixels the integer array to be filled with data
     * @throws IllegalArgumentException if the length of the given array is less than {@code w*h}.
     */
    @Override
    public int[] readPixels(int x, int y, int w, int h, int[] pixels, ProgressMonitor pm) throws IOException {
        return getPixels(x, y, w, h, pixels, pm);
    }

    /**
     * Retrieves an array of tie point data interpolated to the product with and height as float array. If the given
     * array is {@code null} a new one was created and returned. *
     *
     * @param x      the x coordinate of the array to be read
     * @param y      the y coordinate of the array to be read
     * @param w      the width of the array to be read
     * @param h      the height of the array to be read
     * @param pixels the float array to be filled with data
     * @param pm     a monitor to inform the user about progress
     * @throws IllegalArgumentException if the length of the given array is less than {@code w*h}.
     */
    @Override
    public float[] readPixels(int x, int y, int w, int h, float[] pixels, ProgressMonitor pm) throws IOException {
        return getPixels(x, y, w, h, pixels, pm);
    }

    /**
     * Retrieves an array of tie point data interpolated to the product with and height as double array. If the given
     * array is {@code null} a new one was created and returned.
     *
     * @param x      the x coordinate of the array to be read
     * @param y      the y coordinate of the array to be read
     * @param w      the width of the array to be read
     * @param h      the height of the array to be read
     * @param pixels the double array to be filled with data
     * @param pm     a monitor to inform the user about progress
     * @throws IllegalArgumentException if the length of the given array is less than {@code w*h}.
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
        throwPixelsAreReadOnlyException();
    }

    /**
     * This method is not implemented because pixels are read-only in tie-point grids.
     */
    @Override
    public void writePixels(int x, int y, int w, int h, float[] pixels, ProgressMonitor pm) throws IOException {
        throwPixelsAreReadOnlyException();
    }

    /**
     * This method is not implemented because pixels are read-only in tie-point grids.
     */
    @Override
    public void writePixels(int x, int y, int w, int h, double[] pixels, ProgressMonitor pm) throws IOException {
        throwPixelsAreReadOnlyException();
    }

    /**
     * Reads raster data from this dataset into the user-supplied raster data buffer. <p>
     * <p>
     * This method always directly (re-)reads this tie-point grid's data from its associated data source into the given data
     * buffer.
     *
     * @param offsetX    the X-offset in the raster co-ordinates where reading starts
     * @param offsetY    the Y-offset in the raster co-ordinates where reading starts
     * @param width      the width of the raster data buffer
     * @param height     the height of the raster data buffer
     * @param rasterData a raster data buffer receiving the pixels to be read
     * @param pm         a monitor to inform the user about progress
     * @throws java.io.IOException      if an I/O error occurs
     * @throws IllegalArgumentException if the raster is null
     * @throws IllegalStateException    if this product raster was not added to a product so far, or if the product to
     *                                  which this product raster belongs to, has no associated product reader
     * @see ProductReader#readBandRasterData(Band, int, int, int, int, ProductData, com.bc.ceres.core.ProgressMonitor)
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
        getGridData(); // trigger reading the grid points
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeRasterData(int offsetX, int offsetY,
                                int width, int height,
                                ProductData rasterData, ProgressMonitor pm) throws IOException {
        throwPixelsAreReadOnlyException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeRasterDataFully(ProgressMonitor pm) throws IOException {
        throwPixelsAreReadOnlyException();
    }

    @Override
    protected RenderedImage createSourceImage() {
        final MultiLevelModel model = createMultiLevelModel();
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
     * to it. The new operation is implemented by the visitor. <p>
     * <p>
     * The method simply calls {@code visitor.visit(this)}.
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
                                              this.getGridWidth(),
                                              this.getGridHeight(),
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
                                elevationAngleGrid.getGridWidth(),
                                elevationAngleGrid.getGridHeight(),
                                elevationAngleGrid.getOffsetX(),
                                elevationAngleGrid.getOffsetY(),
                                elevationAngleGrid.getSubSamplingX(),
                                elevationAngleGrid.getSubSamplingY(),
                                zenithAngles);
    }

    // ////////////////////////////////////////////////////////////////////////
    // Implementation helpers

    private static void throwPixelsAreReadOnlyException() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("pixels are read-only in tie-point grids");
    }

    private double interpolate(double wi, double wj, int i0, int j0) {
        final float[] tiePoints = getTiePoints();
        final int w = getGridWidth();
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
            double tiePoint = tiePoints[i];
            sinTiePoints[i] = (float) Math.sin(MathUtils.DTOR * tiePoint);
            cosTiePoints[i] = (float) Math.cos(MathUtils.DTOR * tiePoint);
        }
        sinGrid = new TiePointGrid(base.getName(),
                                   base.getGridWidth(),
                                   base.getGridHeight(),
                                   base.getOffsetX(),
                                   base.getOffsetY(),
                                   base.getSubSamplingX(),
                                   base.getSubSamplingY(),
                                   sinTiePoints);
        cosGrid = new TiePointGrid(base.getName(),
                                   base.getGridWidth(),
                                   base.getGridHeight(),
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
        final int srcTPGWidth = sourceTiePointGrid.getGridWidth();
        final int srcTPGHeight = sourceTiePointGrid.getGridHeight();
        final double srcTPGSubSamplingX = sourceTiePointGrid.getSubSamplingX();
        final double srcTPGSubSamplingY = sourceTiePointGrid.getSubSamplingY();
        int subsetOffsetX = 0;
        int subsetOffsetY = 0;
        int subsetStepX = 1;
        int subsetStepY = 1;
        final int srcRasterWidth = sourceTiePointGrid.getRasterWidth();
        final int srcRasterHeight = sourceTiePointGrid.getRasterHeight();
        int subsetWidth = srcRasterWidth;
        int subsetHeight = srcRasterHeight;
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

        final double newTPGSubSamplingX = srcTPGSubSamplingX / subsetStepX;
        final double newTPGSubSamplingY = srcTPGSubSamplingY / subsetStepY;
        final float pixelCenter = 0.5f;
        final double newTPGOffsetX = (sourceTiePointGrid.getOffsetX() - pixelCenter - subsetOffsetX) / subsetStepX + pixelCenter;
        final double newTPGOffsetY = (sourceTiePointGrid.getOffsetY() - pixelCenter - subsetOffsetY) / subsetStepY + pixelCenter;
        final double newOffsetX = newTPGOffsetX % newTPGSubSamplingX;
        final double newOffsetY = newTPGOffsetY % newTPGSubSamplingY;
        final double diffX = newOffsetX - newTPGOffsetX;
        final double diffY = newOffsetY - newTPGOffsetY;
        final int dataOffsetX;
        if (diffX < 0.0f) {
            dataOffsetX = 0;
        } else {
            dataOffsetX = (int) Math.round(diffX / newTPGSubSamplingX);
        }
        final int dataOffsetY;
        if (diffY < 0.0f) {
            dataOffsetY = 0;
        } else {
            dataOffsetY = (int) Math.round(diffY / newTPGSubSamplingY);
        }

        int newTPGWidth = (int) Math.ceil(subsetWidth / srcTPGSubSamplingX) + 2;
        if (dataOffsetX + newTPGWidth > srcTPGWidth) {
            newTPGWidth = srcTPGWidth - dataOffsetX;
        }
        int newTPGHeight = (int) Math.ceil(subsetHeight / srcTPGSubSamplingY) + 2;
        if (dataOffsetY + newTPGHeight > srcTPGHeight) {
            newTPGHeight = srcTPGHeight - dataOffsetY;
        }

        final float[] oldTiePoints = sourceTiePointGrid.getTiePoints();
        final float[] tiePoints = new float[newTPGWidth * newTPGHeight];
        for (int y = 0; y < newTPGHeight; y++) {
            final int srcPos = srcTPGWidth * (dataOffsetY + y) + dataOffsetX;
            System.arraycopy(oldTiePoints, srcPos, tiePoints, y * newTPGWidth, newTPGWidth);
        }

        final TiePointGrid tiePointGrid = new TiePointGrid(sourceTiePointGrid.getName(),
                                                           newTPGWidth,
                                                           newTPGHeight,
                                                           newOffsetX,
                                                           newOffsetY,
                                                           newTPGSubSamplingX,
                                                           newTPGSubSamplingY,
                                                           tiePoints);
        tiePointGrid.setUnit(sourceTiePointGrid.getUnit());
        tiePointGrid.setDescription(sourceTiePointGrid.getDescription());
        tiePointGrid.setDiscontinuity(sourceTiePointGrid.getDiscontinuity());
        return tiePointGrid;
    }
}
