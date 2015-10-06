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
package org.esa.snap.core.dataio;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.util.Guardian;

import java.io.IOException;

/**
 * The <code>AbstractProductWriter</code> class can be used as a base class for new product writer implementations.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 * @see #writeProductNodes
 */
public abstract class AbstractProductWriter implements ProductWriter {

    /**
     * The destination directory to write to
     */
    private final ProductWriterPlugIn _writerPlugIn;
    /**
     * The source product to be written.
     */
    private Product _sourceProduct;
    /**
     * The output object
     */
    private Object _output;

    /**
     * Constructs a <code>ProductWriter</code>. Since no output destination is set, the <code>setOutput</code>
     * method must be called before data can be written.
     *
     * @param writerPlugIn the plug-in which created this writer, must not be <code>null</code>
     * @throws IllegalArgumentException
     * @see #writeProductNodes
     */
    public AbstractProductWriter(ProductWriterPlugIn writerPlugIn) {
        Guardian.assertNotNull("writerPlugIn", writerPlugIn);
        _writerPlugIn = writerPlugIn;
    }

    /**
     * Returns the plug-in which created this product writer.
     *
     * @return the product writer plug-in, should never be <code>null</code>
     */
    public ProductWriterPlugIn getWriterPlugIn() {
        return _writerPlugIn;
    }

    /**
     * Returns the source product to be written or <code>null</code> if the <code>writeProductNodes</code> has not be
     * called so far.
     */
    protected Product getSourceProduct() {
        return _sourceProduct;
    }

    /**
     * Retrives the current output destination object. Thie return value might be <code>null</code> if the
     * <code>setOutput</code> method has not been called so far.
     *
     * @return the output
     */
    public Object getOutput() {
        return _output;
    }

    /**
     * Writes the in-memory representation of a data product.
     * <p> Whether the band data - the actual pixel values - is written out immediately or later when pixels are
     * flushed, is up to the implementation.
     *
     * @param product the in-memory representation of the data product
     * @param output  an object representing a valid output for this writer, might be a <code>ImageOutputStream</code>
     *                or other <code>Object</code> to use for future decoding.
     * @throws IllegalArgumentException if <code>product</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>output</code> is <code>null</code> or it's type is none of the
     *                                  supported output types.
     * @throws IOException              if an I/O error occurs
     */
    public void writeProductNodes(Product product, Object output) throws IOException {
        Guardian.assertNotNull("product", product);
        Guardian.assertNotNull("output", output);
        if (!isInstanceOfValidOutputType(output)) {
            throw new IllegalArgumentException("invalid output type");
        }
        _sourceProduct = product;
        _output = output;
        writeProductNodesImpl();
        _sourceProduct.setModified(false);
    }

    /**
     * Writes the in-memory representation of a data product. This method was called by <code>writeProductNodes(product,
     * output)</code> of the AbstractProductWriter.
     *
     * @throws IllegalArgumentException if <code>output</code> type is not one of the supported output sources.
     * @throws IOException              if an I/O error occurs
     */
    protected abstract void writeProductNodesImpl() throws IOException;

    /**
     * Utility method which ensures that an output is assigned to this writer.
     *
     * @throws IllegalStateException if no output was set (output is <code>null</code>).
     */
    protected void checkOutput() throws IllegalStateException {
        if (_output == null) {
            throw new IllegalStateException("output not set");
        }
    }

    /**
     * Checks if the given object is an instance of one of the valid output types for this product writer.
     *
     * @return <code>true</code> if so
     * @see ProductWriterPlugIn#getOutputTypes()
     */
    protected boolean isInstanceOfValidOutputType(Object output) {
        Class[] outputTypes = getWriterPlugIn().getOutputTypes();
        for (Class outputType : outputTypes) {
            if (outputType.isInstance(output)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns wether the given product node is to be written.
     *
     * @param node the product node
     * @return <code>true</code> if so
     */
    public boolean shouldWrite(ProductNode node) {
        return true;
    }

    /**
     * Enables resp. disables incremental writing of this product writer. By default, a reader should enable progress
     * listening.
     *
     * @param enabled enables or disables progress listening.
     */
    public void setIncrementalMode(boolean enabled) {
    }

    /**
     * Returns whether this product writer writes only modified product nodes.
     *
     * @return <code>true</code> if so
     */
    public boolean isIncrementalMode() {
        return false;
    }

    /**
     * Overwrite this method to physicaly delete a <code>Band</code> from the writer's output file.
     */
    public void removeBand(Band band) {
    }

    /**
    * Sets selectable product format for writers which handle multiple formats.
    *
    * @param formatName The name of the file format.
    */
    public void setFormatName(final String formatName) {}
}
