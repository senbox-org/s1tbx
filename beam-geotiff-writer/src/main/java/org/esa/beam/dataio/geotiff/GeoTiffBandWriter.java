/*
 * $Id: GeoTiffBandWriter.java,v 1.2 2006/12/08 13:48:35 marcop Exp $
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
package org.esa.beam.dataio.geotiff;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

/**
 * A band writer implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
class GeoTiffBandWriter {

    private ImageOutputStream _ios;
    private TiffIFD _ifd;
    private Product _tempProduct;

    public GeoTiffBandWriter(final TiffIFD ifd, final ImageOutputStream ios, final Product product) {
        _ifd = ifd;
        _ios = ios;
        _tempProduct = product;
    }

    public void dispose() {
        _ifd = null;
        _ios = null;
        _tempProduct = null;
    }

    /**
     * Writes raster data from the given in-memory source buffer into the data sink specified by the given source band
     * and region.
     * <p/>
     * <h3>Source band</h3> The source band is used to identify the data sink in which this method transfers the sample
     * values given in the source buffer. The method does not modify the pixel data of the given source band at all.
     * <p/>
     * <h3>Source buffer</h3> The first element of the source buffer corresponds to the given <code>sourceOffsetX</code>
     * and <code>sourceOffsetY</code> of the source region. These parameters are an offset within the band's raster data
     * and <b>not</b> an offset within the source buffer.<br> The number of elements in the buffer must be exactly be
     * <code>sourceWidth * sourceHeight</code>. The pixel values to be writte are considered to be stored in
     * line-by-line order, so the raster X co-ordinate varies faster than the Y.
     * <p/>
     * <h3>Source region</h3> The given destination region specified by the <code>sourceOffsetX</code>,
     * <code>sourceOffsetY</code>, <code>sourceWidth</code> and <code>sourceHeight</code> parameters is given in the
     * source band's raster co-ordinates. These co-ordinates are identical with the destination raster co-ordinates
     * since product writers do not support spectral or spatial subsets.
     *
     * @param sourceBand   the source band which identifies the data sink to which to write the sample values
     * @param regionData   the data buffer which provides the sample values to be written
     * @param regionX      the X-offset in the band's raster co-ordinates
     * @param regionY      the Y-offset in the band's raster co-ordinates
     * @param regionWidth  the width of region to be written given in the band's raster co-ordinates
     * @param regionHeight the height of region to be written given in the band's raster co-ordinates
     * @throws java.io.IOException      if an I/O error occurs
     * @throws IllegalArgumentException if the number of elements source buffer not equals <code>sourceWidth *
     *                                  sourceHeight</code> or the source region is out of the band's raster
     * @see org.esa.beam.framework.datamodel.Band#getRasterWidth()
     * @see org.esa.beam.framework.datamodel.Band#getRasterHeight()
     */
    public void writeBandRasterData(final Band sourceBand,
                                    final int regionX,
                                    final int regionY,
                                    final int regionWidth,
                                    final int regionHeight,
                                    final ProductData regionData,
                                    ProgressMonitor pm) throws IOException {
        if (!_tempProduct.containsBand(sourceBand.getName())) {
            throw new IllegalArgumentException("'" + sourceBand.getName() + "' is not a band of the product");
        }
        int maxElemSizeBandDataType = _ifd.getMaxElemSizeBandDataType();
        final int stripIndex = _tempProduct.getBandIndex(sourceBand.getName());
        final TiffValue[] offsetValues = _ifd.getEntry(TiffTag.STRIP_OFFSETS).getValues();
        final long stripOffset = ((TiffLong) offsetValues[stripIndex]).getValue();
        final TiffValue[] bitsPerSampleValues = _ifd.getEntry(TiffTag.BITS_PER_SAMPLE).getValues();
        final int elemSize = ((TiffShort) bitsPerSampleValues[stripIndex]).getValue() / 8;
        final int sourceWidthBytes = sourceBand.getSceneRasterWidth() * elemSize;
        final int regionOffsetXInBytes = regionX * elemSize;
        final int pixelOffset = sourceWidthBytes * regionY + regionOffsetXInBytes;
        final long startOffset = stripOffset + pixelOffset;

        pm.beginTask("Writing band '" + sourceBand.getName() + "'...", regionHeight);
        try {
            for (int y = 0; y < regionHeight; y++) {
                _ios.seek(startOffset + y * sourceWidthBytes);
                if (maxElemSizeBandDataType == ProductData.TYPE_UINT8) {
                    final byte[] data = new byte[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = (byte) regionData.getElemUIntAt(y * regionWidth + x);
                    }
                    _ios.write(data);
                } else if (maxElemSizeBandDataType == ProductData.TYPE_INT8) {
                    final byte[] data = new byte[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = (byte) regionData.getElemIntAt(y * regionWidth + x);
                    }
                    _ios.write(data);
                } else if (maxElemSizeBandDataType == ProductData.TYPE_UINT16) {
                    final short[] data = new short[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = (short) regionData.getElemUIntAt(y * regionWidth + x);
                    }
                    _ios.writeShorts(data, 0, regionWidth);
                } else if (maxElemSizeBandDataType == ProductData.TYPE_INT16) {
                    final short[] data = new short[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = (short) regionData.getElemIntAt(y * regionWidth + x);
                    }
                    _ios.writeShorts(data, 0, regionWidth);
                } else if (maxElemSizeBandDataType == ProductData.TYPE_UINT32) {
                    final int[] data = new int[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = (int) regionData.getElemUIntAt(y * regionWidth + x);
                    }
                    _ios.writeInts(data, 0, regionWidth);
                } else if (maxElemSizeBandDataType == ProductData.TYPE_INT32) {
                    final int[] data = new int[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = regionData.getElemIntAt(y * regionWidth + x);
                    }
                    _ios.writeInts(data, 0, regionWidth);
                } else if (maxElemSizeBandDataType == ProductData.TYPE_FLOAT32) {
                    final float[] data = new float[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = (float) sourceBand.scale(regionData.getElemDoubleAt(y * regionWidth + x));
                    }
                    _ios.writeFloats(data, 0, regionWidth);
                } else if (maxElemSizeBandDataType == ProductData.TYPE_FLOAT64) {
                    final double[] data = new double[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = sourceBand.scale(regionData.getElemDoubleAt(y * regionWidth + x));
                    }
                    _ios.writeDoubles(data, 0, regionWidth);
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }
}
