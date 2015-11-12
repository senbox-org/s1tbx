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
package org.esa.snap.dataio.geotiff;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.geotiff.internal.TiffIFD;
import org.esa.snap.dataio.geotiff.internal.TiffLong;
import org.esa.snap.dataio.geotiff.internal.TiffShort;
import org.esa.snap.dataio.geotiff.internal.TiffTag;
import org.esa.snap.dataio.geotiff.internal.TiffValue;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A band writer implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
class GeoTiffBandWriter {

    private ImageOutputStream ios;
    private TiffIFD ifd;
    private Product tempProduct;
    private ArrayList<Band> bandsList;

    GeoTiffBandWriter(final TiffIFD ifd, final ImageOutputStream ios, final Product product) {
        this.ifd = ifd;
        this.ios = ios;
        tempProduct = product;
        final Band[] bands = tempProduct.getBands();
        bandsList = new ArrayList<Band>(bands.length);
        for (Band band : bands) {
            if(Utils.shouldWriteNode(band)) {
                bandsList.add(band);
            }
        }
    }

    public void dispose() {
        ifd = null;
        ios = null;
        tempProduct = null;
    }

    /**
     * Writes raster data from the given in-memory source buffer into the data sink specified by the given source band
     * and region.
     * <p>
     * <h3>Source band</h3> The source band is used to identify the data sink in which this method transfers the sample
     * values given in the source buffer. The method does not modify the pixel data of the given source band at all.
     * <p>
     * <h3>Source buffer</h3> The first element of the source buffer corresponds to the given <code>sourceOffsetX</code>
     * and <code>sourceOffsetY</code> of the source region. These parameters are an offset within the band's raster data
     * and <b>not</b> an offset within the source buffer.<br> The number of elements in the buffer must be exactly be
     * <code>sourceWidth * sourceHeight</code>. The pixel values to be writte are considered to be stored in
     * line-by-line order, so the raster X co-ordinate varies faster than the Y.
     * <p>
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
     * @see Band#getRasterWidth()
     * @see Band#getRasterHeight()
     */
    public void writeBandRasterData(final Band sourceBand,
                                    final int regionX,
                                    final int regionY,
                                    final int regionWidth,
                                    final int regionHeight,
                                    final ProductData regionData,
                                    ProgressMonitor pm) throws IOException {
        if (!tempProduct.containsBand(sourceBand.getName())) {
            throw new IllegalArgumentException("'" + sourceBand.getName() + "' is not a band of the product");
        }
        final int bandDataType = ifd.getBandDataType();
        final int stripIndex = getStripIndex(sourceBand);
        final TiffValue[] offsetValues = ifd.getEntry(TiffTag.STRIP_OFFSETS).getValues();
        final long stripOffset = ((TiffLong) offsetValues[stripIndex]).getValue();
        final TiffValue[] bitsPerSampleValues = ifd.getEntry(TiffTag.BITS_PER_SAMPLE).getValues();
        final long elemSize = ((TiffShort) bitsPerSampleValues[stripIndex]).getValue() / 8;
        final long sourceWidthBytes = sourceBand.getRasterWidth() * elemSize;
        final long regionOffsetXInBytes = regionX * elemSize;
        final long pixelOffset = sourceWidthBytes * regionY + regionOffsetXInBytes;
        final long startOffset = stripOffset + pixelOffset;

        pm.beginTask("Writing band '" + sourceBand.getName() + "'...", regionHeight);
        try {
            for (int y = 0; y < regionHeight; y++) {
                ios.seek(startOffset + y * sourceWidthBytes);
                final int stride = y * regionWidth;
                if (bandDataType == ProductData.TYPE_UINT8) {
                    final byte[] data = new byte[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = (byte) regionData.getElemUIntAt(stride + x);
                    }
                    ios.write(data);
                } else if (bandDataType == ProductData.TYPE_INT8) {
                    final byte[] data = new byte[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = (byte) regionData.getElemIntAt(stride + x);
                    }
                    ios.write(data);
                } else if (bandDataType == ProductData.TYPE_UINT16) {
                    final short[] data = new short[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = (short) regionData.getElemUIntAt(stride + x);
                    }
                    ios.writeShorts(data, 0, regionWidth);
                } else if (bandDataType == ProductData.TYPE_INT16) {
                    final short[] data = new short[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = (short) regionData.getElemIntAt(stride + x);
                    }
                    ios.writeShorts(data, 0, regionWidth);
                } else if (bandDataType == ProductData.TYPE_UINT32) {
                    final int[] data = new int[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = (int) regionData.getElemUIntAt(stride + x);
                    }
                    ios.writeInts(data, 0, regionWidth);
                } else if (bandDataType == ProductData.TYPE_INT32) {
                    final int[] data = new int[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = regionData.getElemIntAt(stride + x);
                    }
                    ios.writeInts(data, 0, regionWidth);
                } else if (bandDataType == ProductData.TYPE_FLOAT32) {
                    final float[] data = new float[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = regionData.getElemFloatAt(stride + x);
                    }
                    ios.writeFloats(data, 0, regionWidth);
                } else if (bandDataType == ProductData.TYPE_FLOAT64) {
                    final double[] data = new double[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = regionData.getElemDoubleAt(stride + x);
                    }
                    ios.writeDoubles(data, 0, regionWidth);
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private int getStripIndex(Band sourceBand) {
        return bandsList.indexOf(sourceBand);
//        return tempProduct.getBandIndex(sourceBand.getName());
    }
}
