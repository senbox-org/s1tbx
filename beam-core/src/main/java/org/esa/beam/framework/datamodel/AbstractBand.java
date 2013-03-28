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

package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.jexp.ParseException;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;

import java.io.IOException;

/**
 * The <code>AbstractBand</code> class provides a set of pixel access methods but does not provide an implementation of
 * the actual reading and writing of pixel data from or into a raster.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 */
public abstract class AbstractBand extends RasterDataNode {

    public static final String VIEW_MODE_ORTHO = "ORTHO";
    public static final String VIEW_MODE_FORWARD = "FORWARD";
    public static final String VIEW_MODE_NADIR = "NADIR";

    public AbstractBand(String name, int dataType, int width, int height) {
        super(name, dataType, width, height);
    }

    /**
     * Retrieves the range of pixels specified by the coordinates as integer array. Reads the data from disk if ot is
     * not in memory yet. If the data is loaded, just copies the data..
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be read
     * @param h      height of the pixel array to be read.
     * @param pixels integer array to be filled with data
     * @param pm     a monitor to inform the user about progress
     */
    @Override
    public void writePixels(int x, int y, int w, int h, int[] pixels, ProgressMonitor pm) throws IOException {
        Guardian.assertNotNull("pixels", pixels);
        final ProductData subRasterData = createCompatibleRasterData(w, h);
        final int n = w * h;
        // check for performance boost using native System.arraycopy
        if (!isScalingApplied() && subRasterData.getElems() instanceof int[]) {
            System.arraycopy(pixels, 0, subRasterData.getElems(), 0, n);
        } else {
            if (isScalingApplied()) {
                for (int i = 0; i < n; i++) {
                    subRasterData.setElemDoubleAt(i, scaleInverse(pixels[i]));
                }
            } else {
                for (int i = 0; i < n; i++) {
                    subRasterData.setElemIntAt(i, pixels[i]);
                }
            }
        }
        writeRasterData(x, y, w, h, subRasterData, pm);
    }


    /**
     * Retrieves the range of pixels specified by the coordinates as float array. Reads the data from disk if ot is not
     * in memory yet. If the data is loaded, just copies the data..
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be read
     * @param h      height of the pixel array to be read.
     * @param pixels float array to be filled with data
     * @param pm     a monitor to inform the user about progress
     */
    @Override
    public synchronized void writePixels(int x, int y, int w, int h, float[] pixels, ProgressMonitor pm) throws
            IOException {
        Guardian.assertNotNull("pixels", pixels);
        final ProductData subRasterData = createCompatibleRasterData(w, h);
        final int n = w * h;
        // check for performance boost using native System.arraycopy
        if (!isScalingApplied() && subRasterData.getElems() instanceof float[]) {
            System.arraycopy(pixels, 0, subRasterData.getElems(), 0, n);
        } else {
            if (isScalingApplied()) {
                for (int i = 0; i < n; i++) {
                    subRasterData.setElemDoubleAt(i, scaleInverse(pixels[i]));
                }
            } else {
                for (int i = 0; i < n; i++) {
                    subRasterData.setElemFloatAt(i, pixels[i]);
                }
            }
        }
        writeRasterData(x, y, w, h, subRasterData, pm);
    }

    /**
     * Retrieves the range of pixels specified by the coordinates as double array. Reads the data from disk if ot is not
     * in memory yet. If the data is loaded, just copies the data..
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be read
     * @param h      height of the pixel array to be read.
     * @param pixels double array to be filled with data
     * @param pm     a monitor to inform the user about progress
     */
    @Override
    public void writePixels(int x, int y, int w, int h, double[] pixels, ProgressMonitor pm) throws IOException {
        Guardian.assertNotNull("pixels", pixels);
        final ProductData subRasterData = createCompatibleRasterData(w, h);
        final int n = w * h;
        // check for performance boost using native System.arraycopy
        if (!isScalingApplied() && subRasterData.getElems() instanceof double[]) {
            System.arraycopy(pixels, 0, subRasterData.getElems(), 0, n);
        } else {
            if (isScalingApplied()) {
                for (int i = 0; i < n; i++) {
                    subRasterData.setElemDoubleAt(i, scaleInverse(pixels[i]));
                }
            } else {
                for (int i = 0; i < n; i++) {
                    subRasterData.setElemDoubleAt(i, pixels[i]);
                }
            }
        }
        writeRasterData(x, y, w, h, subRasterData, pm);
    }

    /**
     * Gets an estimated raw storage size in bytes of this product node.
     *
     * @param subsetDef if not <code>null</code> the subset may limit the size returned
     * @return the size in bytes.
     */
    @Override
    public abstract long getRawStorageSize(ProductSubsetDef subsetDef);

    //////////////////////////////////////////////////////////////////////////
    // Implementation helpers

    private ProductData readSubRegionRasterData(int x, int y, int w, int h, ProgressMonitor pm) throws IOException {
        ProductData subRasterData = createCompatibleRasterData(w, h);
        readRasterData(x, y, w, h, subRasterData, pm);
        return subRasterData;
    }

    private ProductData getRasterDataSafe() {
        if (!hasRasterData()) {
            throw new IllegalStateException("raster data not loaded");
        }
        return getRasterData();
    }

    public String getViewModeId(final String bandName) {
        final String nameLC = getName().toLowerCase();
        String viewModeId = VIEW_MODE_NADIR;
        if (nameLC.indexOf("forward") >= 0 ||
                nameLC.indexOf("fward") >= 0) {
            viewModeId = VIEW_MODE_FORWARD;
        }
        return viewModeId;
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

    /////////////////////////////////////////////////////////////////////////
    // Deprecated API


    /**
     * (Re-)Computes this band's data using the given arithmetic expression.
     *
     * @param expression          the arithmetic expression string, e.g. "1 + log(radiance_5 / radiance_13)"
     * @param validMaskExpression the arithmetic expression identifying valid source pixels, e.g. "radiance_5 > 0.0 && radiance_13 > 0.0"
     * @param sourceProducts      the list of source products possibly referenced in the expression
     * @param defaultProductIndex the index of the product for which also symbols without the
     *                            product prefix <code>$<i>ref-no</i></code> are registered in the namespace
     * @param checkInvalids       if true, the method recognizes numerically invalid values (NaN, Infinity)
     * @param useInvalidValue     if true, numerically invalid values (NaN, Infinity) are set to <code>invalidValue</code>,
     *                            ignored if <code>checkInvalids = false</code>
     * @param noDataValue         the value used in place of  numerically invalid values if <code>useInvalidValue =
     *                            true</code>, ignored if  <code>checkInvalids = false</code>
     * @param pm                  a monitor to inform the user about progress
     * @return the number of invalid pixels, zero if  <code>checkInvalids = false</code>
     * @throws IOException    if an I/O error occurs
     * @throws ParseException if the expression syntax is invalid
     * @deprecated Since BEAM 4.10. Use {@link VirtualBand} or {@link org.esa.beam.jai.VirtualBandOpImage}.
     */
    @Deprecated
    public int computeBand(final String expression,
                           final String validMaskExpression,
                           final Product[] sourceProducts,
                           final int defaultProductIndex,
                           final boolean checkInvalids,
                           final boolean useInvalidValue,
                           final double noDataValue,
                           ProgressMonitor pm) throws IOException, ParseException {

        ProductData targetRasterData = getRasterData();
        if (targetRasterData == null) {
            targetRasterData = createCompatibleRasterData();
        }

        final int numInvalids = BandArithmetic.computeBand(expression,
                                                           validMaskExpression,
                                                           sourceProducts,
                                                           defaultProductIndex,
                                                           checkInvalids,
                                                           useInvalidValue,
                                                           noDataValue,
                                                           0, 0,
                                                           getRasterWidth(),
                                                           getRasterHeight(),
                                                           targetRasterData,
                                                           this,
                                                           pm);

        setRasterData(targetRasterData);
        fireProductNodeDataChanged();
        return numInvalids;
    }

    /**
     * Gets a raster data holding this band's pixel data for an entire product scene. If the data has'nt been loaded so
     * far the method returns <code>null</code>.
     * <p/>
     * <p>In opposite to the <code>getRasterData</code> method, this method returns raster data that has at least
     * <code>getBandOutputRasterWidth()*getBandOutputRasterHeight()</code> elements of the given data type to store the
     * scene's pixels.
     *
     * @return raster data covering the pixels for a complete scene
     * @see #getRasterData
     * @see org.esa.beam.framework.datamodel.RasterDataNode#getSceneRasterWidth
     * @see org.esa.beam.framework.datamodel.RasterDataNode#getSceneRasterHeight
     * @deprecated since BEAM 4.11, use {@link #getSourceImage()} instead.
     */
    @Deprecated
    @Override
    public ProductData getSceneRasterData() {
        return getRasterData();
    }

    /**
     * Gets the sample for the pixel located at (x,y) as an integer value.
     *
     * @param x The X co-ordinate of the pixel location
     * @param y The Y co-ordinate of the pixel location
     * @throws NullPointerException if this band has no raster data
     * @throws java.lang.ArrayIndexOutOfBoundsException
     *                              if the co-ordinates are not in bounds
     * @deprecated since BEAM 4.11, use {@link #getSampleInt(int, int)} instead.
     */
    @Deprecated
    @Override
    public int getPixelInt(int x, int y) {
        if (isScalingApplied()) {
            return (int) Math.round(scale(getRasterData().getElemDoubleAt(getRasterWidth() * y + x)));
        } else {
            return getRasterData().getElemIntAt(getRasterWidth() * y + x);
        }
    }

    /**
     * Gets the sample for the pixel located at (x,y) as a float value.
     *
     * @param x The X co-ordinate of the pixel location
     * @param y The Y co-ordinate of the pixel location
     * @throws NullPointerException if this band has no raster data
     * @throws java.lang.ArrayIndexOutOfBoundsException
     *                              if the co-ordinates are not in bounds
     * @deprecated since BEAM 4.11, use {@link #getSampleFloat(int, int)} instead.
     */
    @Deprecated
    @Override
    public float getPixelFloat(int x, int y) {
        if (isScalingApplied()) {
            return (float) scale(getRasterData().getElemDoubleAt(getRasterWidth() * y + x));
        } else {
            return getRasterData().getElemFloatAt(getRasterWidth() * y + x);
        }
    }

    /**
     * Gets the sample for the pixel located at (x,y) as a double value.
     *
     * @param x The X co-ordinate of the pixel location
     * @param y The Y co-ordinate of the pixel location
     * @throws NullPointerException if this band has no raster data
     * @throws java.lang.ArrayIndexOutOfBoundsException
     *                              if the co-ordinates are not in bounds
     *
     * @deprecated since BEAM 4.11, use {@link #getSampleFloat(int, int)} instead.
     */
    @Deprecated
    @Override
    public double getPixelDouble(int x, int y) {
        if (isScalingApplied()) {
            return scale(getRasterData().getElemDoubleAt(getRasterWidth() * y + x));
        } else {
            return getRasterData().getElemDoubleAt(getRasterWidth() * y + x);
        }
    }

    /**
     * Sets the pixel at the given pixel co-ordinate to the given pixel value.
     *
     * @param x          The X co-ordinate of the pixel location
     * @param y          The Y co-ordinate of the pixel location
     * @param pixelValue the new pixel value
     * @throws NullPointerException if this band has no raster data
     *
     * @deprecated since BEAM 4.11. No replacement.
     */
    @Deprecated
    @Override
    public void setPixelInt(int x, int y, int pixelValue) {
        if (isScalingApplied()) {
            getRasterData().setElemDoubleAt(getRasterWidth() * y + x, scaleInverse(pixelValue));
        } else {
            getRasterData().setElemIntAt(getRasterWidth() * y + x, pixelValue);
        }
        fireProductNodeDataChanged();
        setModified(true);
    }

    /**
     * Sets the pixel at the given pixel coordinate to the given pixel value.
     *
     * @param x          The X co-ordinate of the pixel location
     * @param y          The Y co-ordinate of the pixel location
     * @param pixelValue the new pixel value
     * @throws NullPointerException if this band has no raster data
     * @deprecated since BEAM 4.11. No replacement.
     */
    @Deprecated
    @Override
    public void setPixelFloat(int x, int y, float pixelValue) {
        if (isScalingApplied()) {
            getRasterData().setElemDoubleAt(getRasterWidth() * y + x, scaleInverse(pixelValue));
        } else {
            getRasterData().setElemFloatAt(getRasterWidth() * y + x, pixelValue);
        }
        fireProductNodeDataChanged();
        setModified(true);
    }

    /**
     * Sets the pixel value at the given pixel coordinate to the given pixel value.
     *
     * @param x          The X co-ordinate of the pixel location
     * @param y          The Y co-ordinate of the pixel location
     * @param pixelValue the new pixel value
     * @throws NullPointerException if this band has no raster data
     * @deprecated since BEAM 4.11. No replacement.
     */
    @Deprecated
    @Override
    public void setPixelDouble(int x, int y, double pixelValue) {
        if (isScalingApplied()) {
            getRasterData().setElemDoubleAt(getRasterWidth() * y + x, scaleInverse(pixelValue));
        } else {
            getRasterData().setElemDoubleAt(getRasterWidth() * y + x, pixelValue);
        }
        fireProductNodeDataChanged();
        setModified(true);
    }

    /**
     * Retrieves the band data at the given offset (x, y), width and height as integer data. If the data is already in
     * memory, it merely copies the data to the buffer provided. If not, it calls the attached product reader to
     * retrieve the data from the disk file. If the given buffer is <code>null</code> a new one was created and
     * returned.
     *
     * @param x      x offest of upper left corner
     * @param y      y offset of upper left corner
     * @param w      width of the desired data array
     * @param h      height of the desired data array
     * @param pixels array of integer pixels to be filled with data
     * @param pm     a monitor to inform the user about progress
     * @throws IllegalArgumentException if the length of the given array is less than <code>w*h</code>.
     *
     * @deprecated since BEAM 4.11. Use {@link #getSourceImage()} instead.
     */
    @Override
    @Deprecated
    public int[] readPixels(int x, int y, int w, int h, int[] pixels, ProgressMonitor pm) throws IOException {
        if (hasRasterData()) {
            pixels = getPixels(x, y, w, h, pixels, pm);
        } else {
            final ProductData rawData = readSubRegionRasterData(x, y, w, h, pm);
            final int n = w * h;
            pixels = ensureMinLengthArray(pixels, n);
            // check for performance boost using native System.arraycopy
            if (!isScalingApplied() && rawData.getElems() instanceof int[]) {
                System.arraycopy(rawData.getElems(), 0, pixels, 0, n);
            } else {
                if (isScalingApplied()) {
                    for (int i = 0; i < n; i++) {
                        pixels[i] = (int) Math.round(scale(rawData.getElemDoubleAt(i)));
                    }
                } else {
                    for (int i = 0; i < n; i++) {
                        pixels[i] = rawData.getElemIntAt(i);
                    }
                }
            }
        }
        return pixels;
    }

    /**
     * Retrieves the band data at the given offset (x, y), width and height as float data. If the data is already in
     * memory, it merely copies the data to the buffer provided. If not, it calls the attached product reader to
     * retrieve the data from the disk file. If the given buffer is <code>null</code> a new one was created and
     * returned.
     *
     * @param x      x offest of upper left corner
     * @param y      y offset of upper left corner
     * @param w      width of the desired data array
     * @param h      height of the desired data array
     * @param pixels array of float pixels to be filled with data.
     * @param pm     a monitor to inform the user about progress
     * @throws IllegalArgumentException if the length of the given array is less than <code>w*h</code>.
     *
     * @deprecated since BEAM 4.11. Use {@link #getSourceImage()} instead.
     */
    @Override
    @Deprecated
    public float[] readPixels(int x, int y, int w, int h, float[] pixels, ProgressMonitor pm) throws IOException {
        try {
            if (hasRasterData()) {
                pm.beginTask("Reading pixels...", 1);
                pixels = getPixels(x, y, w, h, pixels, SubProgressMonitor.create(pm, 1));
            } else {
                pm.beginTask("Reading pixels...", 2);
                final ProductData rawData = readSubRegionRasterData(x, y, w, h, SubProgressMonitor.create(pm, 1));
                final int n = w * h;
                pixels = ensureMinLengthArray(pixels, n);
                if (!isScalingApplied() && rawData.getElems() instanceof float[]) {
                    System.arraycopy(rawData.getElems(), 0, pixels, 0, n);
                } else {
                    if (isScalingApplied()) {
                        for (int i = 0; i < n; i++) {
                            pixels[i] = (float) scale(rawData.getElemFloatAt(i));
                        }
                    } else {
                        for (int i = 0; i < n; i++) {
                            pixels[i] = rawData.getElemFloatAt(i);
                        }
                    }
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
        return pixels;
    }

    /**
     * Retrieves the band data at the given offset (x, y), width and height as double data. If the data is already in
     * memory, it merely copies the data to the buffer provided. If not, it calls the attached product reader to
     * retrieve the data from the disk file. If the given buffer is <code>null</code> a new one was created and
     * returned.
     *
     * @param x      x offest of upper left corner
     * @param y      y offset of upper left corner
     * @param w      width of the desired data array
     * @param h      height of the desired data array
     * @param pixels array of double pixels to be filled with data
     * @param pm     a monitor to inform the user about progress
     * @throws IllegalArgumentException if the length of the given array is less than <code>w*h</code>.
     *
     * @deprecated since BEAM 4.11. Use {@link #getSourceImage()} instead.
     */
    @Override
    @Deprecated
    public double[] readPixels(int x, int y, int w, int h, double[] pixels, ProgressMonitor pm) throws IOException {
        if (hasRasterData()) {
            pixels = getPixels(x, y, w, h, pixels, pm);
        } else {
            final ProductData rawData = readSubRegionRasterData(x, y, w, h, pm);
            final int n = w * h;
            pixels = ensureMinLengthArray(pixels, n);
            if (!isScalingApplied() && rawData.getElems() instanceof double[]) {
                System.arraycopy(rawData.getElems(), 0, pixels, 0, n);
            } else {
                if (isScalingApplied()) {
                    for (int i = 0; i < n; i++) {
                        pixels[i] = scale(rawData.getElemDoubleAt(i));
                    }
                } else {
                    for (int i = 0; i < n; i++) {
                        pixels[i] = rawData.getElemDoubleAt(i);
                    }
                }
            }
        }
        return pixels;
    }

    /**
     * Retrieves the range of pixels specified by the coordinates as integer array. Throws exception when the data is
     * not read from disk yet. If the given array is <code>null</code> a new one was created and returned.
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be read
     * @param h      height of the pixel array to be read.
     * @param pixels integer array to be filled with data
     * @param pm     a monitor to inform the user about progress
     * @throws NullPointerException     if this band has no raster data
     * @throws IllegalArgumentException if the length of the given array is less than <code>w*h</code>.
     *
     * @deprecated since BEAM 4.11. Use {@link #getSourceImage()} instead.
     */
    @Override
    @Deprecated
    public int[] getPixels(int x, int y, int w, int h, int[] pixels, ProgressMonitor pm) {
        pixels = ensureMinLengthArray(pixels, w * h);
        final ProductData rasterData = getRasterDataSafe();
        final int x1 = x;
        final int y1 = y;
        final int x2 = x1 + w - 1;
        final int y2 = y1 + h - 1;
        int pos = 0;
        pm.beginTask("Retrieving pixels...", y2 - y1);
        try {
            if (isScalingApplied()) {
                for (y = y1; y <= y2; y++) {
                    final int xOffs = y * getRasterWidth();
                    for (x = x1; x <= x2; x++) {
                        pixels[pos++] = (int) Math.round(scale(rasterData.getElemDoubleAt(xOffs + x)));
                    }
                    pm.worked(1);
                }
            } else {
                for (y = y1; y <= y2; y++) {
                    final int xOffs = y * getRasterWidth();
                    for (x = x1; x <= x2; x++) {
                        pixels[pos++] = rasterData.getElemIntAt(xOffs + x);
                    }
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
        return pixels;
    }

    /**
     * Retrieves the range of pixels specified by the coordinates as float array. Throws exception when the data is not
     * read from disk yet. If the given array is <code>null</code> a new one was created and returned.
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be read
     * @param h      height of the pixel array to be read.
     * @param pixels float array to be filled with data
     * @param pm     a monitor to inform the user about progress
     * @throws NullPointerException     if this band has no raster data
     * @throws IllegalArgumentException if the length of the given array is less than <code>w*h</code>.
     *
     *
     * @deprecated since BEAM 4.11. Use {@link #getSourceImage()} instead.
     */
    @Deprecated
    @Override
    public float[] getPixels(int x, int y, int w, int h, float[] pixels, ProgressMonitor pm) {
        pixels = ensureMinLengthArray(pixels, w * h);
        final ProductData rasterData = getRasterDataSafe();
        final int x1 = x;
        final int y1 = y;
        final int x2 = x1 + w - 1;
        final int y2 = y1 + h - 1;
        int pos = 0;
        pm.beginTask("Retrieving pixels...", y2 - y1);
        try {
            if (isScalingApplied()) {
                for (y = y1; y <= y2; y++) {
                    final int xOffs = y * getRasterWidth();
                    for (x = x1; x <= x2; x++) {
                        pixels[pos++] = (float) scale(rasterData.getElemFloatAt(xOffs + x));
                    }
                    pm.worked(1);
                }
            } else {
                for (y = y1; y <= y2; y++) {
                    final int xOffs = y * getRasterWidth();
                    for (x = x1; x <= x2; x++) {
                        pixels[pos++] = rasterData.getElemFloatAt(xOffs + x);
                    }
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
        return pixels;
    }

    /**
     * Retrieves the range of pixels specified by the coordinates as double array. Throws exception when the data is not
     * read from disk yet. If the given array is <code>null</code> a new one was created and returned.
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be read
     * @param h      height of the pixel array to be read.
     * @param pixels double array to be filled with data
     * @param pm     a monitor to inform the user about progress
     * @throws NullPointerException     if this band has no raster data
     * @throws IllegalArgumentException if the length of the given array is less than <code>w*h</code>.
     * @deprecated since BEAM 4.11. Use {@link #getSourceImage()} instead.
     */
    @Deprecated
    @Override
    public double[] getPixels(int x, int y, int w, int h, double[] pixels, ProgressMonitor pm) {
        pixels = ensureMinLengthArray(pixels, w * h);
        final ProductData rasterData = getRasterDataSafe();
        final int x1 = x;
        final int y1 = y;
        final int x2 = x1 + w - 1;
        final int y2 = y1 + h - 1;
        int pos = 0;
        pm.beginTask("Retrieving pixels...", y2 - y1);
        try {
            if (isScalingApplied()) {
                for (y = y1; y <= y2; y++) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    final int xOffs = y * getRasterWidth();
                    for (x = x1; x <= x2; x++) {
                        pixels[pos++] = scale(rasterData.getElemDoubleAt(xOffs + x));
                    }
                    pm.worked(1);
                }
            } else {
                for (y = y1; y <= y2; y++) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    final int xOffs = y * getRasterWidth();
                    for (x = x1; x <= x2; x++) {
                        pixels[pos++] = rasterData.getElemDoubleAt(xOffs + x);
                    }
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
        return pixels;
    }

    /**
     * Sets a range of pixels specified by the coordinates as integer array. Copies the data to the memory buffer of
     * data at the specified location. Throws exception when the target buffer is not in memory.
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be written
     * @param h      height of the pixel array to be written.
     * @param pixels integer array to be written
     * @throws NullPointerException if this band has no raster data
     * @deprecated since BEAM 4.11. Use {@link #setSourceImage setSourceImage()} or the various {@link #writePixels readPixels()}
     *  method variants to set or write raster data.
     */
    @Deprecated
    @Override
    public void setPixels(int x, int y, int w, int h, int[] pixels) {
        Guardian.assertNotNull("pixels", pixels);
        final ProductData rasterData = getRasterData();
        final int x1 = x;
        final int y1 = y;
        final int x2 = x1 + w - 1;
        final int y2 = y1 + h - 1;
        int pos = 0;
        if (isScalingApplied()) {
            for (y = y1; y <= y2; y++) {
                final int xOffs = y * getRasterWidth();
                for (x = x1; x <= x2; x++) {
                    rasterData.setElemDoubleAt(xOffs + x, scaleInverse(pixels[pos++]));
                }
            }
        } else {
            for (y = y1; y <= y2; y++) {
                final int xOffs = y * getRasterWidth();
                for (x = x1; x <= x2; x++) {
                    rasterData.setElemIntAt(xOffs + x, pixels[pos++]);
                }
            }
        }
        fireProductNodeDataChanged();
        setModified(true);
    }

    /**
     * Sets a range of pixels specified by the coordinates as float array. Copies the data to the memory buffer of data
     * at the specified location. Throws exception when the target buffer is not in memory.
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be written
     * @param h      height of the pixel array to be written.
     * @param pixels float array to be written
     * @throws NullPointerException if this band has no raster data
     * @deprecated since BEAM 4.11. Use {@link #setSourceImage setSourceImage()} or the various {@link #writePixels readPixels()}
     *  method variants to set or write raster data.
     */
    @Deprecated
    @Override
    public void setPixels(int x, int y, int w, int h, float[] pixels) {
        Guardian.assertNotNull("pixels", pixels);
        final ProductData rasterData = getRasterData();
        final int x1 = x;
        final int y1 = y;
        final int x2 = x1 + w - 1;
        final int y2 = y1 + h - 1;
        int pos = 0;
        if (isScalingApplied()) {
            for (y = y1; y <= y2; y++) {
                final int xOffs = y * getRasterWidth();
                for (x = x1; x <= x2; x++) {
                    rasterData.setElemDoubleAt(xOffs + x, scaleInverse(pixels[pos++]));
                }
            }
        } else {
            for (y = y1; y <= y2; y++) {
                final int xOffs = y * getRasterWidth();
                for (x = x1; x <= x2; x++) {
                    rasterData.setElemFloatAt(xOffs + x, pixels[pos++]);
                }
            }
        }
        fireProductNodeDataChanged();
        setModified(true);
    }

    /**
     * Sets a range of pixels specified by the coordinates as double array. Copies the data to the memory buffer of data
     * at the specified location. Throws exception when the target buffer is not in memory.
     *
     * @param x      x offset into the band
     * @param y      y offset into the band
     * @param w      width of the pixel array to be written
     * @param h      height of the pixel array to be written.
     * @param pixels double array to be written
     * @throws NullPointerException if this band has no raster data
     *
     * @deprecated since BEAM 4.11. Use {@link #setSourceImage setSourceImage()} or the various {@link #writePixels readPixels()}
     *  method variants to set or write raster data.
     */
    @Deprecated
    @Override
    public void setPixels(int x, int y, int w, int h, double[] pixels) {
        Guardian.assertNotNull("pixels", pixels);
        final ProductData rasterData = getRasterData();
        final int x1 = x;
        final int y1 = y;
        final int x2 = x1 + w - 1;
        final int y2 = y1 + h - 1;
        int pos = 0;
        if (isScalingApplied()) {
            for (y = y1; y <= y2; y++) {
                final int xOffs = y * getRasterWidth();
                for (x = x1; x <= x2; x++) {
                    rasterData.setElemDoubleAt(xOffs + x, scaleInverse(pixels[pos++]));
                }
            }

        } else {
            for (y = y1; y <= y2; y++) {
                final int xOffs = y * getRasterWidth();
                for (x = x1; x <= x2; x++) {
                    rasterData.setElemDoubleAt(xOffs + x, pixels[pos++]);
                }
            }
        }
        fireProductNodeDataChanged();
        setModified(true);
    }

    /**
     * Ensures that raster data exists
     * @deprecated since BEAM 4.11. No replacement.
     */
    @Deprecated
    public void ensureRasterData() {
        if (!hasRasterData()) {
            setRasterData(createCompatibleRasterData());
        }
        Debug.assertNotNull(getRasterData());
    }

    /**
     * Loads the complete underlying raster data.
     * <p/>
     * <p>After this method has been called successfully, <code>hasRasterData()</code> should always return
     * <code>true</code> and <code>getRasterData()</code> should always return a valid <code>ProductData</code> instance
     * with at least <code>getRasterWidth()*getRasterHeight()</code> elements (samples).
     * <p/>
     * <p> In opposite to the <code>readRasterDataFully</code> method, <code>loadRasterData</code> will only read data
     * if this has not already been done.
     *
     * @param pm a monitor to inform the user about progress
     * @throws java.io.IOException if an I/O error occurs
     * @see #readRasterDataFully(ProgressMonitor)
     * @deprecated since BEAM 4.11. No replacement.
     */
    @Deprecated
    @Override
    public void loadRasterData(ProgressMonitor pm) throws IOException {
        if (!hasRasterData()) {
            readRasterDataFully(pm);
        }
    }

    /**
     * Un-loads the raster data for this band.
     * <p/>
     * <p>After this method has been called successfully, the <code>hasRasterData()</code> method returns
     * <code>false</code> and <code>getRasterData()</code> returns <code>null</code>.
     * <p/>
     *
     * @see #loadRasterData()
     *
     * @deprecated since BEAM 4.11. No replacement.
     */
    @Override
    @Deprecated
    public void unloadRasterData() {
        if (hasRasterData()) {
            setRasterData(null);
        }
    }

}
