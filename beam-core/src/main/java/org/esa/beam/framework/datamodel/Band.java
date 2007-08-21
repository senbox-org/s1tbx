/*
 * $Id: Band.java,v 1.3 2007/03/19 15:52:28 marcop Exp $
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
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.util.Guardian;

import java.awt.Rectangle;
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
 * rawSample + scalingOffset</code> is applied to the raw samples before the <code>getPixel</code> and @
 * <code>readPixel</code> methods return the actual pixel values. If the <code>log10Scaled</code> property is true then
 * the conversion is <code>pow(10, scalingFactor * rawSample + scalingOffset)</code>. The several <code>setPixel</code>
 * and <code>writePixel</code> perform the inverse operations in this case.
 *
 * @author Norman Fomferra
 * @version $Revision: 1.3 $ $Date: 2007/03/19 15:52:28 $
 * @see ProductData
 * @see #getPixels
 * @see #setPixels
 * @see #readPixels
 * @see #writePixels
 */
public class Band extends AbstractBand {

    public final static String PROPERTY_NAME_FLAG_CODING = "flagCoding";
    public final static String PROPERTY_NAME_SOLAR_FLUX = "solarFlux";


    public final static String PROPERTY_NAME_SPECTRAL_BAND_INDEX = "spectralBandIndex";
    public final static String PROPERTY_NAME_SPECTRAL_BANDWIDTH = "spectralBandwidth";
    public final static String PROPERTY_NAME_SPECTRAL_WAVELENGTH = "spectralWavelength";

    /**
     * If this band contains flag data, this is the flag coding.
     */
    private FlagCoding _flagCoding;

    private int _spectralBandIndex;
    private float _spectralWavelength;
    private float _spectralBandwidth;
    private float _solarFlux;

    /**
     * Constructs a new <code>Band</code>.
     *
     * @param name     the name of the new object
     * @param dataType the raster data type, must be one of the multiple <code>ProductData.TYPE_<i>X</i></code>
     *                 constants, with the exception of <code>ProductData.TYPE_UINT32</code>
     * @param width    the width of the raster in pixels
     * @param height   the height of the raster in pixels
     */
    public Band(String name, int dataType, int width, int height) {
        super(name, dataType, width, height);
        // By default a band is not a spectral band,
        // so spectral band index must be -1
        setSpectralBandIndex(-1);
        setModified(false);
    }

    /**
     * Gets the flag coding for this band.
     *
     * @return a non-null value if this band is a flag dataset, <code>null</code> otherwise
     */
    public FlagCoding getFlagCoding() {
        return _flagCoding;
    }

    /**
     * Sets the flag coding for this band.
     *
     * @param flagCoding a non-null value representing the flag coding
     * @throws IllegalStateException if this band does not contain integer pixels
     */
    public void setFlagCoding(FlagCoding flagCoding) {
        if (flagCoding != null) {
            if (!hasIntPixels()) {
                throw new IllegalStateException("band does not contain integer pixels");
            }
        }
        if (_flagCoding != flagCoding) {
            _flagCoding = flagCoding;
            fireProductNodeChanged(PROPERTY_NAME_FLAG_CODING);
            setModified(true);
        }
    }

    /**
     * Tests whether or not this band is a flag band (<code>getFlagCoding() != null</code>).
     *
     * @return <code>true</code> if so
     */
    public boolean isFlagBand() {
        return getFlagCoding() != null;
    }

    /**
     * Gets the (zero-based) spectral band index.
     *
     * @return the (zero-based) spectral band index or <code>-1</code> if it is unknown
     */
    public int getSpectralBandIndex() {
        return _spectralBandIndex;
    }

    /**
     * Sets the (zero-based) spectral band index.
     *
     * @param spectralBandIndex the (zero-based) spectral band index or <code>-1</code> if it is unknown
     */
    public void setSpectralBandIndex(int spectralBandIndex) {
        if (_spectralBandIndex != spectralBandIndex) {
            _spectralBandIndex = spectralBandIndex;
            fireProductNodeChanged(PROPERTY_NAME_SPECTRAL_BAND_INDEX);
            setModified(true);
        }
    }

    /**
     * Gets the spectral wavelength in <code>nm</code> (nanomater) units.
     *
     * @return the wave length in nanometers of this band, or zero if this is not a spectral band or the wave length is
     *         not known.
     */
    public float getSpectralWavelength() {
        return _spectralWavelength;
    }

    /**
     * Sets the spectral wavelength in <code>nm</code> (nanomater) units.
     *
     * @param spectralWavelength the wavelength in nanometers of this band, or zero if this is not a spectral band or
     *                           the wavelength is not known.
     */
    public void setSpectralWavelength(float spectralWavelength) {
        if (_spectralWavelength != spectralWavelength) {
            _spectralWavelength = spectralWavelength;
            fireProductNodeChanged(PROPERTY_NAME_SPECTRAL_WAVELENGTH);
            setModified(true);
        }
    }

    /**
     * Gets the spectral bandwidth in <code>nm</code> (nanomater) units.
     *
     * @return the bandwidth in nanometers of this band, or zero if this is not a spectral band or the bandwidth is not
     *         known.
     */
    public float getSpectralBandwidth() {
        return _spectralBandwidth;
    }

    /**
     * Sets the spectral bandwidth in <code>nm</code> (nanomater) units.
     *
     * @param spectralBandwidth the spectral bandwidth in nanometers of this band, or zero if this is not a spectral band
     *                          or the spectral bandwidth is not known.
     */
    public void setSpectralBandwidth(float spectralBandwidth) {
        if (_spectralBandwidth != spectralBandwidth) {
            _spectralBandwidth = spectralBandwidth;
            fireProductNodeChanged(PROPERTY_NAME_SPECTRAL_BANDWIDTH);
            setModified(true);
        }
    }

    /**
     * Gets the solar flux in in <code>mW/(m^2 sr nm)</code> (milli-watts per square metre per ste-radian per nanometer)
     * units for the wavelength of this band.
     *
     * @return the solar flux for the wavelength of this band, or zero if this is not a spectral band or the solar flux
     *         is not known.
     */
    public float getSolarFlux() {
        return _solarFlux;
    }

    /**
     * Sets the solar flux in <code>mW/(m^2 sr nm)</code> (milli-watts per square metre per ste-radian per nanometer)
     * units for the wavelength of this band.
     *
     * @param solarFlux the solar flux for the wavelength of this band, or zero if this is not a spectral band or the
     *                  solar flux is not known.
     */
    public void setSolarFlux(float solarFlux) {
        if (_solarFlux != solarFlux) {
            _solarFlux = solarFlux;
            fireProductNodeChanged(PROPERTY_NAME_SOLAR_FLUX);
            setModified(true);
        }
    }

    /**
     * Reads raster data from this dataset into the user-supplied raster data buffer.
     * <p/>
     * <p>This method always directly (re-)reads this band's data from its associated data source into the given data
     * buffer.
     *
     * @param offsetX    the X-offset in the band's pixel co-ordinates where reading starts
     * @param offsetY    the Y-offset in the band's pixel co-ordinates where reading starts
     * @param width      the width of the raster data buffer
     * @param height     the height of the raster data buffer
     * @param rasterData a raster data buffer receiving the pixels to be read
     * @param pm         a monitor to inform the user about progress
     * @throws java.io.IOException      if an I/O error occurs
     * @throws IllegalArgumentException if the raster is null
     * @throws IllegalStateException    if this product raster was not added to a product so far, or if the product to which
     *                                  this product raster belongs to, has no associated product reader
     * @see org.esa.beam.framework.dataio.ProductReader#readBandRasterData(Band, int, int, int, int, ProductData, com.bc.ceres.core.ProgressMonitor) 
     */
    @Override
    public void readRasterData(int offsetX, int offsetY, int width, int height, ProductData rasterData,
                               ProgressMonitor pm)
            throws IOException {
        Guardian.assertNotNull("rasterData", rasterData);
        if (!isSynthetic()) {
            readRasterDataFromReader(offsetX, offsetY, width, height, rasterData, pm);
        } else if (hasRasterData()) {
            readRasterDataFromRasterData(offsetX, offsetY, width, height, rasterData, pm);
        } else {
            throw new IllegalStateException("no data source available");
        }
    }

    private void readRasterDataFromReader(int offsetX, int offsetY, int width, int height, ProductData rasterData,
                                          ProgressMonitor pm) throws IOException {
        Product product = getProductSafe();
        ProductReader reader = product.getProductReaderSafe();
        pm.beginTask("Reading raster data...", isMaskProductDataEnabled() ? 2 : 1);
        try {
            reader.readBandRasterData(this, offsetX, offsetY, width, height, rasterData, SubProgressMonitor.create(pm, 1));
            if (isMaskProductDataEnabled()) {
                maskProductData(offsetX, offsetY, width, height, rasterData, SubProgressMonitor.create(pm, 1));
            }
        } finally {
            pm.done();
        }
        if (rasterData == getRasterData()) {
            fireProductNodeDataChanged();
        }
    }

    private void readRasterDataFromRasterData(int offsetX, int offsetY, int width, int height, ProductData rasterData,
                                              ProgressMonitor pm) {
        final ProductData srcRasterData = getRasterData();
        int dstIndex;
        int srcIndex;
        int dstIndexOffset;
        int srcIndexOffset;
        pm.beginTask("Reading raster data...", height);
        try {
            for (int j = 0; j < height; j++) {
                dstIndexOffset = j * width;
                srcIndexOffset = (offsetY + j) * getSceneRasterWidth() + offsetX;
                for (int i = 0; i < width; i++) {
                    dstIndex = dstIndexOffset + i;
                    srcIndex = srcIndexOffset + i;
                    rasterData.setElemDoubleAt(dstIndex, srcRasterData.getElemDoubleAt(srcIndex));
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
        final ProductData rasterData;
        if (hasRasterData()) {
            rasterData = getRasterData();
        } else {
            rasterData = createCompatibleRasterData(getRasterWidth(), getRasterHeight());
        }

        pm.beginTask("Loading raster data", 100);

        try {
            readRasterData(0, 0, getRasterWidth(), getRasterHeight(),
                           rasterData,
                           SubProgressMonitor.create(pm, isValidMaskUsed() ? 60 : 100));
            setRasterData(rasterData);
            // todo - NaN values created in BandArithmetic.computeBand are ignored if
            // todo - although isValidMaskUsed() returns true (nf 2007-08-21)
            if (isValidMaskUsed()) {
                computeValidMask(SubProgressMonitor.create(pm, 40));
            }
        } finally {
            pm.done();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeRasterData(int offsetX, int offsetY,
                                int width, int height,
                                ProductData rasterData,
                                ProgressMonitor pm) throws IOException {
        Guardian.assertNotNull("rasterData", rasterData);
        Product product = getProductSafe();
        ProductWriter writer = product.getProductWriterSafe();
        writer.writeBandRasterData(this, offsetX, offsetY, width, height, rasterData, pm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeRasterDataFully(ProgressMonitor pm) throws IOException {
        if (hasRasterData()) {
            writeRasterData(0, 0, getRasterWidth(), getRasterHeight(), getRasterData(), pm);
        } else {
            ProductReader reader = getProductReaderSafe();
            ProductWriter writer = getProductWriterSafe();
            ProductData rasterData = createCompatibleRasterData(getRasterWidth(), 1);

            pm.beginTask("Writing raster data...", getRasterHeight() * 2);
            try {
                for (int y = 0; y < getRasterHeight(); y++) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    reader.readBandRasterData(this, 0, y, getRasterWidth(), 1, rasterData,
                                              SubProgressMonitor.create(pm, 1));
                    writer.writeBandRasterData(this, 0, y, getRasterWidth(), 1, rasterData,
                                               SubProgressMonitor.create(pm, 1));
                }
            } finally {
                pm.done();
            }
        }
    }

    /**
     * Gets an estimated raw storage size in bytes of this product node.
     *
     * @param subsetDef if not <code>null</code> the subset may limit the size returned
     * @return the size in bytes.
     */
    @Override
    public long getRawStorageSize(ProductSubsetDef subsetDef) {
        long size = 0L;
        if (isPartOfSubset(subsetDef)) {
            size += 256; // add estimated overhead of 256 bytes
            int numDataElems = getNumDataElems();
            if (subsetDef != null) {
                int width = getSceneRasterWidth();
                int height = getSceneRasterHeight();
                Rectangle region = subsetDef.getRegion();
                if (region != null) {
                    width = region.width;
                    height = region.height;
                }
                width /= 1 + subsetDef.getSubSamplingX();
                height /= 1 + subsetDef.getSubSamplingY();
                numDataElems = width * height;
            }
            size += ProductData.getElemSize(getDataType()) * numDataElems;
        }
        return size;
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
    public void acceptVisitor(ProductVisitor visitor) {
        Guardian.assertNotNull("visitor", visitor);
        visitor.visit(this);
    }

    //////////////////////////////////////////////////////////////////////////
    // Implementation helpers

    /**
     * Creates a string defining this band object.
     */
    @Override
    public String toString() {
        return getClass().getName() + "["
                + getName() + ","
                + ProductData.getTypeString(getDataType()) + "," +
                +getRasterWidth() + "," +
                +getRasterHeight() + "," +
                +getSpectralBandIndex() + "," +
                +getSpectralWavelength() + "," +
                +getSpectralBandwidth() + "," +
                +getSolarFlux() + "]";
    }

    @Override
    public void removeFromFile(ProductWriter productWriter) {
        productWriter.removeBand(this);
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
        // don't dispose _flagCoding, its only a reference
        _flagCoding = null;
        super.dispose();
    }
}


