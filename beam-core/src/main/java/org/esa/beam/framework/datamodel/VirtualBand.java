/*
 * $Id: VirtualBand.java,v 1.4 2007/03/19 15:52:28 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jexp.*;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ObjectUtils;
import org.esa.beam.util.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

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
 * @see #getPixels
 * @see #setPixels
 * @see #readPixels
 * @see #writePixels
 */
public class VirtualBand extends Band {

    public static final String PROPERTY_NAME_CHECK_INVALIDS = "checkInvalids";
    public static final String PROPERTY_NAME_EXPRESSION = "expression";

    private String expression;
    private Map<ProductData, Term> termMap;
    private boolean checkInvalids;
    private int numInvalidPixels;


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
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(final String expression) {
        if (!ObjectUtils.equalObjects(this.expression, expression)) {
            this.expression = expression;
            disposeTermMap();
            setImageInfo(null);
            setModified(true);
            fireProductNodeChanged(PROPERTY_NAME_EXPRESSION);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateExpression(final String oldExternalName, final String newExternalName) {
        final String expression = StringUtils.replaceWord(this.expression, oldExternalName, newExternalName);
        if (!this.expression.equals(expression)) {
            this.expression = expression;
            setModified(true);
        }
        super.updateExpression(oldExternalName, newExternalName);
    }

    public boolean getCheckInvalids() {
        return checkInvalids;
    }

    public void setCheckInvalids(final boolean checkInvalids) {
        if (this.checkInvalids != checkInvalids) {
            this.checkInvalids = checkInvalids;
            fireProductNodeChanged(PROPERTY_NAME_CHECK_INVALIDS);
            setModified(true);
        }
    }

    public int getNumInvalidPixels() {
        return numInvalidPixels;
    }

    private void setNumInvalidPixels(final int numInvalidPixels) {
        this.numInvalidPixels = numInvalidPixels;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void readRasterData(final int offsetX, final int offsetY, final int width, final int height,
                               final ProductData rasterData, ProgressMonitor pm)
            throws IOException {
        Guardian.assertNotNull("rasterData", rasterData);

        final Term term;
        try {
            term = getTerm(rasterData);
        } catch (ParseException e) {
            final IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        }
        BandArithmetic.computeBand(term,
                                   offsetX, offsetY,
                                   width, height,
                                   getCheckInvalids(),
                                   isNoDataValueUsed(),
                                   getNoDataValue(),
                                   rasterData,
                                   null,
                                   pm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readRasterDataFully(ProgressMonitor pm) throws IOException {
        final Product product = getProductSafe();
        try {
            final int n = computeBand(getExpression(),
                                      new Product[]{product},
                                      getCheckInvalids(),
                                      isNoDataValueUsed(),
                                      getNoDataValue(),
                                      pm);
            setNumInvalidPixels(n);
            fireProductNodeDataChanged();
        } catch (ParseException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeRasterDataFully(ProgressMonitor pm) throws IOException {
        throw new IOException("write not supported for virtual band");
    }

    /**
     * Writes data from this product raster into the specified region of the user-supplied raster.
     * <p/>
     * <p> It is important to know that this method does not change this product raster's internal state nor does it
     * write into this product raster's internal raster.
     *
     * @param rasterData a raster data buffer receiving the pixels to be read
     * @param offsetX    the X-offset in raster co-ordinates where reading starts
     * @param offsetY    the Y-offset in raster co-ordinates where reading starts
     * @param width      the width of the raster data buffer
     * @param height     the height of the raster data buffer
     * @param pm         a monitor to inform the user about progress
     * @throws IOException              if an I/O error occurs
     * @throws IllegalArgumentException if the raster is null
     * @throws IllegalStateException    if this product raster was not added to a product so far, or if the product to
     *                                  which this product raster belongs to, has no associated product reader
     * @see ProductReader#readBandRasterData(Band, int, int, int, int, ProductData, com.bc.ceres.core.ProgressMonitor)
     */
    @Override
    public void writeRasterData(final int offsetX, final int offsetY, final int width, final int height,
                                final ProductData rasterData, ProgressMonitor pm)
            throws IOException {
        throw new IOException("write not supported for virtual band");
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
                + ProductData.getTypeString(getDataType()) + "," +
                +getRasterWidth() + "," +
                +getRasterHeight() + "]";
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
        disposeTermMap();
        super.dispose();
    }

    private synchronized Term getTerm(final ProductData pd) throws ParseException {
        if (termMap == null) {
            termMap = new WeakHashMap<ProductData, Term>(10);
        }
        Term term = termMap.get(pd);
        if (term == null) {
            term = getProductSafe().createTerm(expression, this);
            termMap.put(pd, term);
        }
        return term;
    }

    private void disposeTermMap() {
        if (termMap != null) {
            termMap.clear();
            termMap = null;
        }
    }
}


