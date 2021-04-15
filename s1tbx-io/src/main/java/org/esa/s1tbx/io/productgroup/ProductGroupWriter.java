/*
 * Copyright (C) 2021 SkyWatch. https://www.skywatch.com
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
package org.esa.s1tbx.io.productgroup;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.Guardian;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * The product writer for product groups.
 */
public class ProductGroupWriter extends AbstractProductWriter {

    private ProductGroupWriterOp op;

    /**
     * Construct a new instance of a product writer for the given product writer plug-in.
     *
     * @param writerPlugIn the given product writer plug-in, must not be <code>null</code>
     */
    public ProductGroupWriter(ProductWriterPlugIn writerPlugIn) {
        super(writerPlugIn);
    }

    /**
     * Writes the in-memory representation of a data product. This method was called by <code>writeProductNodes(product,
     * output)</code> of the AbstractProductWriter.
     *
     */
    @Override
    protected void writeProductNodesImpl() {
        final Object output = getOutput();
        final Product sourceProduct = getSourceProduct();

        File outputFile = null;
        if (output instanceof String) {
            outputFile = new File((String) output);
        } else if (output instanceof File) {
            outputFile = (File) output;
        }
        op = new ProductGroupWriterOp(sourceProduct, outputFile, "BEAM-DIMAP");
        op.initialize();

        sourceProduct.setProductWriter(this);
        sourceProduct.setFileLocation(op.getTargetFolder());
    }

    /**
     * {@inheritDoc}
     */
    public void writeBandRasterData(Band sourceBand,
                                    int sourceOffsetX, int sourceOffsetY,
                                    int sourceWidth, int sourceHeight,
                                    ProductData sourceBuffer,
                                    ProgressMonitor pm) throws IOException {
        Guardian.assertNotNull("sourceBand", sourceBand);
        Guardian.assertNotNull("sourceBuffer", sourceBuffer);

        op.computeBand(sourceBand, new Rectangle(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight));
    }

    /**
     * Writes all data in memory to the data sink(s) associated with this writer.
     */
    public void flush() {

    }

    /**
     * Closes all output streams currently open.
     *
     * @throws IOException on failure
     */
    public void close() throws IOException {
        if(op != null) {
            op.dispose();
        }
    }

    /**
     * Complete deletes the physical representation of the given product from the file system.
     */
    public void deleteOutput() {

    }

    /**
     * Returns whether the given product node is to be written.
     *
     * @param node the product node
     * @return <code>true</code> if so
     */
    @Override
    public boolean shouldWrite(ProductNode node) {
        return !(node instanceof VirtualBand) && super.shouldWrite(node);
    }
}
