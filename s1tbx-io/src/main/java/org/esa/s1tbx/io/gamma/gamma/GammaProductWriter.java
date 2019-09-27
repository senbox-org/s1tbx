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
package org.esa.s1tbx.io.gamma;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.s1tbx.commons.io.FileImageOutputStreamExtImpl;
import org.esa.s1tbx.io.gamma.header.GammaConstants;
import org.esa.s1tbx.io.gamma.header.HeaderDEMWriter;
import org.esa.s1tbx.io.gamma.header.HeaderDiffWriter;
import org.esa.s1tbx.io.gamma.header.HeaderWriter;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.internal.TileImpl;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.engine_utilities.datamodel.Unit;

import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * The product writer for Gamma products.
 */
public class GammaProductWriter extends AbstractProductWriter {

    private File outputDir;
    private File outputFile;
    private Product srcProduct;
    private Map<Band, ImageOutputStream> bandOutputStreams;
    private HeaderWriter headerWriter;

    public GammaProductWriter(final ProductWriterPlugIn writerPlugIn) {
        super(writerPlugIn);
    }

    /**
     * Writes the in-memory representation of a data product. This method was called by <code>writeProductNodes(product,
     * output)</code> of the AbstractProductWriter.
     *
     * @throws IllegalArgumentException if <code>output</code> type is not one of the supported output sources.
     * @throws IOException              if an I/O error occurs
     */
    @Override
    protected void writeProductNodesImpl() throws IOException {
        final Object output = getOutput();
        outputFile = null;
        if (output instanceof String) {
            outputFile = new File((String) output);
        } else if (output instanceof File) {
            outputFile = (File) output;
        }
        outputDir = outputFile.getParentFile();
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        srcProduct = getSourceProduct();
        srcProduct.setProductWriter(this);

        if (outputFile.getName().toLowerCase().contains("dem")) {
            headerWriter = new HeaderDEMWriter(this, srcProduct, outputFile);
        } else if (outputFile.getName().toLowerCase().contains("diff")) {
            headerWriter = new HeaderDiffWriter(this, srcProduct, outputFile);
        } else {
            headerWriter = new HeaderWriter(this, srcProduct, outputFile);
        }
        headerWriter.writeParFile();
    }

    private ImageOutputStream createImageOutputStream(final Band band) throws IOException {
        final ImageOutputStream out = new FileImageOutputStreamExtImpl(getValidImageFile(band));
        out.setByteOrder(ByteOrder.BIG_ENDIAN);
        return out;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void writeBandRasterData(Band sourceBand,
                                                 int sourceOffsetX, int sourceOffsetY,
                                                 int sourceWidth, int sourceHeight,
                                                 ProductData sourceBuffer,
                                                 ProgressMonitor pm) throws IOException {
        Guardian.assertNotNull("sourceBand", sourceBand);
        Guardian.assertNotNull("sourceBuffer", sourceBuffer);
        final int sourceBandWidth = sourceBand.getRasterWidth();
        final int elemSize = headerWriter.getHighestElemSize();

        final ImageOutputStream outputStream = getOrCreateImageOutputStream(sourceBand);
        pm.beginTask("Writing band '" + sourceBand.getName() + "'...", sourceHeight);
        try {
            if (isComplex(sourceBand)) {
                int numInterleaved = 2;
                final Rectangle rect = new Rectangle(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight);
                final Tile sourceTile = getSourceTile(getComplexSrcBand(sourceBand), rect);
                final ProductData qSourceBuffer = sourceTile.getRawSamples();
                int srcCnt = 0;

                if (elemSize >= 4) {
                    final float[] destBuffer = new float[sourceWidth * numInterleaved];
                    for (long y = sourceOffsetY; y < sourceOffsetY + sourceHeight; ++y) {
                        int dstCnt = 0;
                        for (int x = sourceOffsetX; x < sourceOffsetX + sourceWidth; ++x) {

                            destBuffer[dstCnt++] = sourceBuffer.getElemFloatAt(srcCnt);
                            destBuffer[dstCnt++] = qSourceBuffer.getElemFloatAt(srcCnt);
                            srcCnt++;
                        }

                        outputStream.seek(elemSize * (y * sourceBandWidth + sourceOffsetX) * numInterleaved);
                        outputStream.writeFloats(destBuffer, 0, destBuffer.length);
                    }
                } else {
                    final short[] destBuffer = new short[sourceWidth * numInterleaved];
                    for (long y = sourceOffsetY; y < sourceOffsetY + sourceHeight; ++y) {
                        int dstCnt = 0;
                        for (int x = sourceOffsetX; x < sourceOffsetX + sourceWidth; ++x) {

                            destBuffer[dstCnt++] = (short) sourceBuffer.getElemFloatAt(srcCnt);
                            destBuffer[dstCnt++] = (short) qSourceBuffer.getElemFloatAt(srcCnt);
                            srcCnt++;
                        }

                        outputStream.seek(elemSize * (y * sourceBandWidth + sourceOffsetX) * numInterleaved);
                        outputStream.writeShorts(destBuffer, 0, destBuffer.length);
                    }
                }

                //System.out.println(rect.toString());
            } else {
                //todo
                long outputPos = (long) sourceOffsetY * (long) sourceBandWidth + sourceOffsetX;
                final long max = sourceHeight * sourceWidth;
                for (int sourcePos = 0; sourcePos < max; sourcePos += sourceWidth) {
                    sourceBuffer.writeTo(sourcePos, sourceWidth, outputStream, outputPos);
                    outputPos += sourceBandWidth;
                }
            }
            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    private Band getComplexSrcBand(final Band iBand) {
        String name = iBand.getName();
        if (name.startsWith("i_")) {
            name = name.replace("i_", "q_");
        } else if (name.startsWith("q_")) {
            name = name.replace("q_", "i_");
        }
        return srcProduct.getBand(name);
    }

    private static Tile getSourceTile(RasterDataNode rasterDataNode, Rectangle region) {
        MultiLevelImage image = rasterDataNode.getSourceImage();

        Raster awtRaster = image.getData(region); // Note: copyData is NOT faster!

        return new TileImpl(rasterDataNode, awtRaster);
    }

    /**
     * Writes all data in memory to disk. After a flush operation, the writer can be closed safely
     *
     * @throws java.io.IOException on failure
     */
    public void flush() throws IOException {
        if (bandOutputStreams == null) {
            return;
        }
        for (Object o : bandOutputStreams.values()) {
            ((ImageOutputStream) o).flush();
        }
    }

    /**
     * Closes all output streams currently open.
     *
     * @throws java.io.IOException on failure
     */
    public void close() throws IOException {
        if (bandOutputStreams == null) {
            return;
        }
        for (Object o : bandOutputStreams.values()) {
            ((ImageOutputStream) o).close();
        }
        bandOutputStreams.clear();
        bandOutputStreams = null;
    }

    /**
     * Deletes the physically representation of the product from the hard disk.
     */
    public void deleteOutput() throws IOException {
        flush();
        close();
        if (outputFile != null && outputFile.exists() && outputFile.isFile()) {
            outputFile.delete();
        }
    }

    /**
     * Returns the data output stream associated with the given <code>Band</code>. If no stream exists, one is created
     * and fed into the hash map
     */
    private ImageOutputStream getOrCreateImageOutputStream(final Band band) throws IOException {
        ImageOutputStream outputStream = getImageOutputStream(band);
        if (outputStream == null) {
            outputStream = createImageOutputStream(band);
            if (bandOutputStreams == null) {
                bandOutputStreams = new HashMap<>();
            }
            bandOutputStreams.put(band, outputStream);
        }
        return outputStream;
    }

    private ImageOutputStream getImageOutputStream(final Band band) {
        if (bandOutputStreams != null) {
            return bandOutputStreams.get(band);
        }
        return null;
    }

    @Override
    public boolean shouldWrite(final ProductNode node) {
        if (node instanceof VirtualBand) {
            return false;
        }
        if (node instanceof FilterBand) {
            return false;
        }
        if (node instanceof Band) {
            Band band = (Band) node;
            if (Unit.IMAGINARY.equals(band.getUnit()))
                return false;
        }
        if (node.isModified()) {
            return true;
        }
        if (!isIncrementalMode()) {
            return true;
        }
        if (!(node instanceof Band)) {
            return true;
        }
        final File imageFile = getImageFile((Band) node);
        return !(imageFile != null && imageFile.exists());
    }

    private File getImageFile(final Band band) {
        //return new File(outputDir, createImageFilename(band));
        String filename = band.getName();
        if (!filename.contains(".")) {
            //System.out.println("baseFileName = " + headerWriter.getBaseFileName());
            filename = filename + GammaConstants.SLC_EXTENSION;
        }
        if (filename.startsWith("i_") || filename.startsWith("q_")) {
            filename = filename.substring(2);
        }
        //System.out.println("getImageFile: band = " + band.getName() + " outputDir = " + outputDir.getName() + " filename = " + filename);
        return new File(outputDir, filename);
    }

    /**
     * Returns a file associated with the given <code>Band</code>. The method ensures that the file exists and have the
     * right size. Also ensures a recreate if the file not exists or the file have a different file size. A new envi
     * header file was written every call.
     */
    private File getValidImageFile(final Band band) throws IOException {
        final File file = getImageFile(band);
        if (file.exists()) {
            if (file.length() != getImageFileSize(band)) {
                createPhysicalFile(file, getImageFileSize(band));
            }
        } else {
            createPhysicalFile(file, getImageFileSize(band));
        }
        return file;
    }

    private static long getImageFileSize(final RasterDataNode band) {
        long numInterleaved = 1;
        if (isComplex(band)) {
            numInterleaved = 2;
        }
        return (long) ProductData.getElemSize(band.getDataType()) *
                (long) band.getRasterWidth() *
                (long) band.getRasterHeight() * numInterleaved;
    }

    private static void createPhysicalFile(final File file, final long fileSize) throws IOException {
        final File parentDir = file.getParentFile();
        if (parentDir != null) {
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                throw new IOException("Unable to create folders in " + parentDir);
            }
        }
        final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        randomAccessFile.setLength(fileSize);
        randomAccessFile.close();
    }

    private static boolean isComplex(final RasterDataNode band) {
        final String unit = band.getUnit();
        return unit != null && unit.equals(Unit.REAL);
    }
}
