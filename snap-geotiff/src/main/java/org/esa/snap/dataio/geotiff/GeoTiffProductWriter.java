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
package org.esa.snap.dataio.geotiff;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.geotiff.internal.TiffHeader;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * A product writer implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class GeoTiffProductWriter extends AbstractProductWriter {

    private File outputFile;
    private ImageOutputStream outputStream;
    private GeoTiffBandWriter bandWriter;

    /**
     * Construct a new instance of a product writer for the given GeoTIFF product writer plug-in.
     *
     * @param writerPlugIn the given GeoTIFF product writer plug-in, must not be <code>null</code>
     */
    public GeoTiffProductWriter(final ProductWriterPlugIn writerPlugIn) {
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
        outputFile = null;
        outputStream = null;
        bandWriter = null;

        final File file;
        if (getOutput() instanceof String) {
            file = new File((String) getOutput());
        } else {
            file = (File) getOutput();
        }

        outputFile = FileUtils.ensureExtension(file, GeoTiffProductWriterPlugIn.GEOTIFF_FILE_EXTENSION[0]);
        deleteOutput();

        ensureNamingConvention();

        writeGeoTIFFProduct(new FileImageOutputStream(outputFile), getSourceProduct());
    }

    private void ensureNamingConvention() {
        if (outputFile != null) {
            getSourceProduct().setName(FileUtils.getFilenameWithoutExtension(outputFile));
        }
    }
    void writeGeoTIFFProduct(ImageOutputStream stream, final Product sourceProduct) throws IOException {
        outputStream = stream;
        final TiffHeader tiffHeader = new TiffHeader(new Product[]{sourceProduct});
        tiffHeader.write(stream);
        bandWriter = new GeoTiffBandWriter(tiffHeader.getIfdAt(0), stream, sourceProduct);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void writeBandRasterData(final Band sourceBand,
                                    final int sourceOffsetX,
                                    final int sourceOffsetY,
                                    final int sourceWidth,
                                    final int sourceHeight,
                                    final ProductData sourceBuffer,
                                    ProgressMonitor pm) throws IOException {
        bandWriter.writeBandRasterData(sourceBand,
                                        sourceOffsetX, sourceOffsetY,
                                        sourceWidth, sourceHeight,
                                        sourceBuffer, pm);
    }


    @Override
    public boolean shouldWrite(ProductNode node) {
        return Utils.shouldWriteNode(node);
    }

    /**
     * Deletes the physically representation of the given product from the hard disk.
     */
    @Override
    public void deleteOutput() {
        if (outputFile != null && outputFile.isFile()) {
            outputFile.delete();
        }
    }

    /**
     * Writes all data in memory to disk. After a flush operation, the writer can be closed safely
     *
     * @throws java.io.IOException on failure
     */
    @Override
    public void flush() throws IOException {
        if (outputStream != null) {
            outputStream.flush();
        }
    }

    /**
     * Closes all output streams currently open.
     *
     * @throws java.io.IOException on failure
     */
    @Override
    public void close() throws IOException {
        if (bandWriter != null) {
            bandWriter.dispose();
            bandWriter = null;
        }
        if (outputStream != null) {
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        }
    }
}
