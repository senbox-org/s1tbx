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
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;

import java.awt.image.RenderedImage;
import java.io.IOException;

/**
 * A band contains the data for geophysical parameter in remote sensing data products. Bands are two-dimensional images
 * which hold their pixel values (samples) in a buffer of the type {@link ProductData}. The band class is just a
 * container for attached metadata of the band, currently: <ul> <li>the flag coding {@link FlagCoding}</li> <li>the band
 * index at which position the band is stored in the associated product</li> <li>the center wavelength of the band</li>
 * <li>the bandwidth of the band</li> <li>the solar spectral flux of the band</li> <li>the width and height of the
 * band</li> </ul> The band can contain a buffer to the real data, but this buffer must be read explicitely, to keep the
 * memory fingerprint small, the data is not read automatically.
 * <p/>
 * <p/>
 * The several <code>getPixel</code> and <code>readPixel</code> methods of this class do not necessarily return the
 * values contained in the data buffer of type {@link ProductData}. If the <code>scalingFactor</code>,
 * <code>scalingOffset</code> or <code>log10Scaled</code> are set a conversion of the form <code>scalingFactor *
 * rawSample + scalingOffset</code> is applied to the raw samples before the <code>getPixel</code> and
 * <code>readPixel</code> methods return the actual pixel values. If the <code>log10Scaled</code> property is true then
 * the conversion is <code>pow(10, scalingFactor * rawSample + scalingOffset)</code>. The several <code>setPixel</code>
 * and <code>writePixel</code> perform the inverse operations in this case.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see ProductData
 */
public class VirtualBand extends Band {

    public static final String PROPERTY_NAME_EXPRESSION = "expression";

    private String expression;


    /**
     * Constructs a new <code>Band</code>.
     *
     * @param name       the name of the new object
     * @param dataType   the raster data type, must be one of the multiple <code>ProductData.TYPE_<i>X</i></code>
     *                   constants, with the exception of <code>ProductData.TYPE_UINT32</code>
     * @param width      the width of the raster in pixels
     * @param height     the height of the raster in pixels
     * @param expression the expression code
     */
    public VirtualBand(final String name, final int dataType, final int width, final int height,
                       final String expression) {
        super(name, dataType, width, height);
        setSpectralBandIndex(-1);
        setSynthetic(true);
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(final String expression) {
        if (expression != null && !this.expression.equals(expression)) {
            this.expression = expression;
            if (isSourceImageSet()) {
                setSourceImage(null);
            }
            resetValidMask();
            setStx(null);
            setImageInfo(null);
            setModified(true);
            fireProductNodeChanged(PROPERTY_NAME_EXPRESSION);
            fireProductNodeChanged(PROPERTY_NAME_DATA);
            fireProductNodeDataChanged();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateExpression(final String oldExternalName, final String newExternalName) {
        final String updatedExpression = StringUtils.replaceWord(this.expression, oldExternalName, newExternalName);
        if (!expression.equals(updatedExpression)) {
            expression = updatedExpression;
            setModified(true);
        }
        super.updateExpression(oldExternalName, newExternalName);
    }

    @Override
    public void setPixelInt(final int x, final int y, final int pixelValue) {
        throw new IllegalStateException("set not supported for virtual band");
    }

    @Override
    public void setPixelFloat(final int x, final int y, final float pixelValue) {
        throw new IllegalStateException("set not supported for virtual band");
    }

    @Override
    public void setPixelDouble(final int x, final int y, final double pixelValue) {
        throw new IllegalStateException("set not supported for virtual band");
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
    public void writePixels(final int x, final int y, final int w, final int h, final int[] pixels,
                            ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("write not supported for virtual band");
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
    public synchronized void writePixels(final int x, final int y, final int w, final int h, final float[] pixels,
                                         ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("write not supported for virtual band");
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
    public void writePixels(final int x, final int y, final int w, final int h, final double[] pixels,
                            ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("write not supported for virtual band");
    }

    //////////////////////////////////////////////////////////////////////////
    // 'Visitor' pattern support

    /**
     * Accepts the given visitor. This method implements the well known 'Visitor' design pattern of the gang-of-four.
     * The visitor pattern allows to define new operations on the product data model without the need to add more code
     * to it. The new operation is implemented by the visitor.
     * <p/>
     * <p>The method simply calls <code>visitor.visit(this)</code>.
     *
     * @param visitor the visitor, must not be <code>null</code>
     */
    @Override
    public void acceptVisitor(final ProductVisitor visitor) {
        Guardian.assertNotNull("visitor", visitor);
        visitor.visit(this);
    }

    /**
     * Gets an estimated raw storage size in bytes of this product node.
     *
     * @param subsetDef if not <code>null</code> the subset may limit the size returned
     *
     * @return the size in bytes.
     */
    @Override
    public long getRawStorageSize(final ProductSubsetDef subsetDef) {
        return 256L;
    }

    /**
     * Creates a string defining this band object.
     */
    @Override
    public String toString() {
        return getClass().getName() + "["
               + getName() + ","
               + ProductData.getTypeString(getDataType()) + ","
               + getRasterWidth() + ","
               + getRasterHeight() + "]";
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     */
    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    protected RenderedImage createSourceImage() {
        return createVirtualSourceImage(this, getExpression());
    }

    /**
     * Non-API.
     *
     * @param raster     The raster data node.
     * @param expression The band-arithmetic expression.
     *
     * @return A multi-level image.
     */
    public static MultiLevelImage createVirtualSourceImage(final RasterDataNode raster, final String expression) {
        return VirtualBandMultiLevelImage.create(expression, raster);
    }
}


