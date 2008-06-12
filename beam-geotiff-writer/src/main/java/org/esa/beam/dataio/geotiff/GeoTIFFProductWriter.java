/*
 * $Id: GeoTIFFProductWriter.java,v 1.2 2006/12/08 13:48:35 marcop Exp $
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
import org.esa.beam.framework.dataio.AbstractProductWriter;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.util.io.FileUtils;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A product writer implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class GeoTIFFProductWriter extends AbstractProductWriter {

    private File _outputFile;
    private ImageOutputStream _outputStream;
    private GeoTiffBandWriter _bandWriter;

    /**
     * Construct a new instance of a product writer for the given GeoTIFF product writer plug-in.
     *
     * @param writerPlugIn the given GeoTIFF product writer plug-in, must not be <code>null</code>
     */
    public GeoTIFFProductWriter(final ProductWriterPlugIn writerPlugIn) {
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
        _outputFile = null;
        _outputStream = null;
        _bandWriter = null;

        final File file;
        if (getOutput() instanceof String) {
            file = new File((String) getOutput());
        } else {
            file = (File) getOutput();
        }

        _outputFile = FileUtils.ensureExtension(file, GeoTIFFProductWriterPlugIn.GEOTIFF_FILE_EXTENSION);
        deleteOutput();
        _outputStream = new FileImageOutputStream(_outputFile);

        final Product tempProduct = createWritableProduct();
        final TiffHeader tiffHeader = new TiffHeader(new Product[]{tempProduct});
        tiffHeader.write(_outputStream);
        _bandWriter = new GeoTiffBandWriter(tiffHeader.getIfdAt(0), _outputStream, tempProduct);
    }

    private Product createWritableProduct() throws IOException {
        final Product sourceProduct = getSourceProduct();
        final ArrayList<String> nodeNames = new ArrayList<String>();
        for (int i = 0; i < sourceProduct.getNumBands(); i++) {
            final Band band = sourceProduct.getBandAt(i);
            if (shouldWrite(band)) {
                nodeNames.add(band.getName());
            }
        }
        final GeoCoding sourceGeoCoding = sourceProduct.getGeoCoding();
        if (sourceGeoCoding instanceof TiePointGeoCoding) {
            final TiePointGeoCoding geoCoding = (TiePointGeoCoding) sourceGeoCoding;
            nodeNames.add(geoCoding.getLatGrid().getName());
            nodeNames.add(geoCoding.getLonGrid().getName());
        }
        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setNodeNames(nodeNames.toArray(new String[nodeNames.size()]));
        subsetDef.setIgnoreMetadata(false);
        return sourceProduct.createSubset(subsetDef, "temp", "");
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

        _bandWriter.writeBandRasterData(sourceBand,
                                        sourceOffsetX, sourceOffsetY,
                                        sourceWidth, sourceHeight,
                                        sourceBuffer, pm);
    }

    /**
     * Deletes the physically representation of the given product from the hard disk.
     */
    public void deleteOutput() {
        if (_outputFile != null && _outputFile.isFile()) {
            _outputFile.delete();
        }
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
        if (_bandWriter != null) {
            _bandWriter.dispose();
            _bandWriter = null;
        }
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
        if (node instanceof VirtualBand) {
            return false;
        }
        return super.shouldWrite(node);
    }
}
