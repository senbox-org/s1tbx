/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.generic;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.Guardian;
import org.esa.nest.datamodel.AbstractMetadata;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;


public class GenericWriter extends AbstractProductWriter {

    private ImageOutputStream _outputStream = null;

    private String bandName = null;
    private int bandCounter = 0;
    private int numOfWriteBands = 0; // number of bands that are written (no virtual bands)

    /**
     * Construct a new instance of a product writer for the given product writer plug-in.
     *
     * @param writerPlugIn the given product writer plug-in, must not be <code>null</code>
     */
    public GenericWriter(final ProductWriterPlugIn writerPlugIn) {
        super(writerPlugIn);

    }

    /**
     * Writes the in-memory representation of a data product. This method was called by <code>writeProductNodes(product,
     * output)</code> of the AbstractProductWriter.
     *
     * @throws IllegalArgumentException if <code>output</code> type is not one of the supported output sources.
     * @throws java.io.IOException      if an I/O error occurs
     */
    @Override
    protected void writeProductNodesImpl() throws IOException {

//        _outputStream = null;
        final File file;
        if (getOutput() instanceof String) {
            file = new File((String) getOutput());
        } else {
            file = (File) getOutput();
        }

        _outputStream = new FileImageOutputStream(file);
        // Default to nativeOrder
        _outputStream.setByteOrder(ByteOrder.nativeOrder());

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(getSourceProduct());
        AbstractMetadata.saveExternalMetadata(getSourceProduct(), absRoot, file);

        // Get number of Real (not Virtual) bands
        final int numOfBands = getSourceProduct().getNumBands();
        for (int i = 0; i < numOfBands; i++) {
            if (!(getSourceProduct().getBandAt(i) instanceof VirtualBand)) {
                numOfWriteBands++;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeBandRasterData(final Band sourceBand,
                                    final int sourceOffsetX,
                                    final int sourceOffsetY,
                                    final int sourceWidth,
                                    final int sourceHeight,
                                    final ProductData sourceBuffer,
                                    ProgressMonitor pm) throws IOException {

        Guardian.assertNotNull("sourceBand", sourceBand);
        Guardian.assertNotNull("sourceBuffer", sourceBuffer);

        checkBufferSize(sourceWidth, sourceHeight, sourceBuffer);

        final int sourceBandWidth = sourceBand.getSceneRasterWidth();
        final int sourceBandHeight = sourceBand.getSceneRasterHeight();

        checkSourceRegionInsideBandRegion(sourceWidth, sourceBandWidth, sourceHeight, sourceBandHeight, sourceOffsetX, sourceOffsetY);

        // Define order number of Band that is being saved
        if (bandName == null) {
            bandName = sourceBand.getName();
        } else if (!bandName.equals(sourceBand.getName())) {
            bandName = sourceBand.getName();
            bandCounter++;
        }

        // Write all source NOT VIRTUAL bands in BSQ : Band Sequential Format
        long outputPos = sourceOffsetY * (numOfWriteBands * sourceBandWidth) + sourceOffsetX + (bandCounter * sourceBandWidth);
        pm.beginTask("Writing band '" + sourceBand.getName() + "'...", sourceHeight);
        try {
            final long max = sourceHeight * sourceWidth;
            final int size = sourceBuffer.getElemSize();
            for (int sourcePos = 0; sourcePos < max; sourcePos += sourceWidth) {
                sourceBuffer.writeTo(sourcePos, sourceWidth, size, _outputStream, outputPos);
                outputPos += (numOfWriteBands * sourceBandWidth);
            }
            pm.worked(1);
        } finally {
            pm.done();
        }

    }

    private static void checkSourceRegionInsideBandRegion(int sourceWidth, final int sourceBandWidth, int sourceHeight,
                                                          final int sourceBandHeight, int sourceOffsetX,
                                                          int sourceOffsetY) {
        Guardian.assertWithinRange("sourceWidth", sourceWidth, 1, sourceBandWidth);
        Guardian.assertWithinRange("sourceHeight", sourceHeight, 1, sourceBandHeight);
        Guardian.assertWithinRange("sourceOffsetX", sourceOffsetX, 0, sourceBandWidth - sourceWidth);
        Guardian.assertWithinRange("sourceOffsetY", sourceOffsetY, 0, sourceBandHeight - sourceHeight);
    }


    // from BEAM EnviProductWriter
    private static void checkBufferSize(int sourceWidth, int sourceHeight, ProductData sourceBuffer) {
        final int expectedBufferSize = (sourceWidth * sourceHeight);
        final int actualBufferSize = sourceBuffer.getNumElems();
        Guardian.assertEquals("sourceWidth * sourceHeight", actualBufferSize, expectedBufferSize);  /*I18N*/
    }


    /**
     * Deletes the physically representation of the given product from the hard disk.
     */
    public void deleteOutput() {

    }

    /**
     * Writes all data in memory to disk. After a flush operation, the writer can be closed safely
     *
     * @throws java.io.IOException on failure
     */
    public void flush() throws IOException {
        if (_outputStream != null) {
            _outputStream.flush();
        }
    }

    /**
     * Closes all output streams currently open.
     *
     * @throws java.io.IOException on failure
     */
    public void close() throws IOException {
        if (_outputStream != null) {
            _outputStream.flush();
            _outputStream.close();
            _outputStream = null;
        }
    }

    /**
     * Returns wether the given product node is to be written.
     *
     * @param node the product node
     *
     * @return <code>true</code> if so
     */
    @Override
    public boolean shouldWrite(ProductNode node) {
        return !(node instanceof VirtualBand) && super.shouldWrite(node);
    }
}