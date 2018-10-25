/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dem.dataio.srtm1_hgt;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The product reader for SRTM HGT files.
 */
public class SRTMHGTReader extends AbstractProductReader {

    private ZipFile zipFile;
    private ImageInputStream imageInputStream = null;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public SRTMHGTReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {

        final File inputFile = ReaderUtils.getFileFromInput(getInput());
        try {
            if (inputFile == null) {
                throw new IOException("Unable to read hgt file " + getInput().toString());
            }

            final String ext = FileUtils.getExtension(inputFile);
            if (".zip".equalsIgnoreCase(ext)) {
                final ZipFile productZip = new ZipFile(inputFile, ZipFile.OPEN_READ);
                // get first hgt
                final Enumeration<? extends ZipEntry> entries = productZip.entries();
                while(entries.hasMoreElements()) {
                    final ZipEntry zipEntry = entries.nextElement();
                    final String name = zipEntry.getName().toLowerCase();
                    if(name.endsWith(".hgt")) {
                        final InputStream inputStream = productZip.getInputStream(zipEntry);
                        imageInputStream = new MemoryCacheImageInputStream(inputStream);
                        break;
                    }
                }
            } else {
                imageInputStream = new FileImageInputStream(inputFile);
                imageInputStream.setByteOrder(ByteOrder.BIG_ENDIAN);
            }

        } catch (IOException e) {
            try {
                close();
            } catch (IOException ignored) {
            }
            throw e;
        }

        final int width = SRTM1HgtElevationModelDescriptor.PIXEL_RES;
        final int height = SRTM1HgtElevationModelDescriptor.PIXEL_RES;

        final Product product = new Product(inputFile.getName(), "HGT", width, height);

        final Band band = new Band("elevation", ProductData.TYPE_INT16, width, height);
        band.setUnit(Unit.METERS);
        product.addBand(band);

        addGeoCoding(product, inputFile);

        product.setProductReader(this);
        product.setFileLocation(inputFile);
        product.setModified(false);

        return product;
    }

    private static void addGeoCoding(final Product product, final File inputFile) throws IOException {

        final SRTM1HgtFileInfo info = SRTM1HgtFileInfo.create(inputFile);
        final float northing = info.getNorthing();
        final float easting = info.getEasting();

        float[] lat = new float[]{northing+1, northing+1, northing, northing};
        float[] lon = new float[]{easting, easting+1, easting, easting+1};

        ReaderUtils.addGeoCoding(product, lat, lon);
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (imageInputStream != null) {
            imageInputStream.close();
            imageInputStream = null;
        }
        if (zipFile != null) {
            zipFile.close();
            zipFile = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        try {
            final int sourceMaxX = sourceOffsetX + sourceWidth;
            final int sourceMaxY = sourceOffsetY + sourceHeight;
            final int sourceRasterWidth = destBand.getRasterWidth() + 1;

            final int elemSize = destBuffer.getElemSize();
            int bandOffset = 0;
            int destPos = 0;

            for (int sourceY = sourceOffsetY; sourceY < sourceMaxY; sourceY += sourceStepY) {
                final long sourcePosY = sourceY * sourceRasterWidth;
                if (sourceStepX == 1) {
                    imageInputStream.seek(bandOffset + elemSize * (sourcePosY + sourceOffsetX));
                    destBuffer.readFrom(destPos, destWidth, imageInputStream);
                    destPos += destWidth;
                } else {
                    for (int sourceX = sourceOffsetX; sourceX < sourceMaxX; sourceX += sourceStepX) {
                        imageInputStream.seek(bandOffset + elemSize * (sourcePosY + sourceX));
                        destBuffer.readFrom(destPos, 1, imageInputStream);
                        destPos++;
                    }
                }
            }
        } catch (Throwable e) {
            SystemUtils.LOG.warning("Unable to read SRTM HGT: " + e.getMessage());
        }
    }
}