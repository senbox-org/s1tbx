/*
 * $Id: AbstractProductWriter.java,v 1.2 2006/12/08 13:48:36 marcop Exp $
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
package org.esa.beam.framework.dataio;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProgressListener;
import org.esa.beam.framework.datamodel.ProgressListenerList;
import org.esa.beam.util.Guardian;

import java.io.IOException;

/**
 * The <code>AbstractProductReader</code>  class can be used as a base class for new product writer implementations. The
 * only two methods which clients must implement are <code>writeProductNodes()</code> and <code>writeBandData</code>
 * methods.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision: 1.2 $ $Date: 2006/12/08 13:48:36 $
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
     * Is product listening enabled
     */
    private boolean _processListeningEnabled;
    /**
     * The progress listener list
     */
    private ProgressListenerList _progressListenerList;

    /**
     * Constructs a <code>EnviProductWriter</code>. Since no output destination is set, the <code>setOutput</code>
     * method must be called before data can be written.
     *
     * @param writerPlugIn the plug-in which created this writer, must not be <code>null</code>
     *
     * @throws IllegalArgumentException
     * @see #writeProductNodes
     */
    public AbstractProductWriter(ProductWriterPlugIn writerPlugIn) {
        Guardian.assertNotNull("writerPlugIn", writerPlugIn);
        _writerPlugIn = writerPlugIn;
        _processListeningEnabled = true;
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
     * Returns the progress listener list associated with this writer.
     *
     * @return the progress listener list, can be <code>null</code>
     *
     * @deprecated progress is now indicated by a {@link com.bc.ceres.core.ProgressMonitor} given as
     *             parmater to the concerning method
     */
    public ProgressListenerList getProgressListenerList() {
        return _progressListenerList;
    }

    /**
     * Enables resp. disables process listening for this reader. By default, a reader should enable progress listening.
     *
     * @param enabled enables resp. disables process listening
     */
    public void setProgressListeningEnabled(boolean enabled) {
        _processListeningEnabled = enabled;
    }

    /**
     * Returns whether process listening is enbaled for this reader.
     *
     * @return <code>true</code> if so
     */
    public boolean isProgressListeningEnabled() {
        return _processListeningEnabled;
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
     * <p/>
     * <p> Whether the band data - the actual pixel values - is written out immediately or later when pixels are
     * flushed, is up to the implementation.
     *
     * @param product the in-memory representation of the data product
     * @param output  an object representing a valid output for this writer, might be a <code>ImageOutputStream</code>
     *                or other <code>Object</code> to use for future decoding.
     *
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
     * Adds a new process listener to this product writer. Process listeners are informed while the data of the product
     * is written to the output destination. <p> A product writer shall inform all registered listeners every time a
     * non-ignorable ammount of memory (in terms of computation time) is written out.
     *
     * @param listener the new listener to be added, <code>null</code> values are ignored
     */
    public void addProgressListener(ProgressListener listener) {
        if (listener == null) {
            return;
        }
        if (_progressListenerList == null) {
            _progressListenerList = new ProgressListenerList();
        }
        _progressListenerList.addProgressListener(listener);
    }

    /**
     * Removes an existing process listener from this product writer.
     *
     * @param listener the listener to be removed, <code>null</code> values are ignored
     */
    public void removeProgressListener(ProgressListener listener) {
        if (listener == null || _progressListenerList == null) {
            return;
        }
        _progressListenerList.removeProgressListener(listener);
    }

    /**
     * Notifies all registered progress listeners the the write process started. This utility method should be used by
     * concrete product writer implementations in order to signal that the writing process started.
     *
     * @param processDescription a textual description of the process started
     * @param minProgressValue   the minimum progress value
     * @param maxProgressValue   the maximum progress value
     *
     * @return <code>true</code> if the writing process can be started, <code>false</code> indicates an error and the
     *         writer should not continue writing
     */
    protected boolean fireProcessStarted(String processDescription, int minProgressValue, int maxProgressValue) {
        if (_processListeningEnabled && _progressListenerList != null) {
            return _progressListenerList.fireProcessStarted(processDescription,
                                                            minProgressValue,
                                                            maxProgressValue);
        }
        return true;
    }

    /**
     * Notifies all registered progress listeners the the write process is in progress. This utility method should be
     * used by concrete product writer implementations in order to signal that the writing process is in progress.
     *
     * @param currentProgressValue the current progress value which should be a value in the range passed to the
     *                             <code>fireProcessStarted</code> method
     *
     * @return <code>true</code> if the process can be continued, <code>false</code> indicates an error and the writer
     *         should not continue writing
     */
    protected boolean fireProcessInProgress(int currentProgressValue) {
        if (_processListeningEnabled && _progressListenerList != null) {
            return _progressListenerList.fireProcessInProgress(currentProgressValue);
        }
        return true;
    }

    /**
     * Notifies all registered progress listeners the the write process has ended. This utility method should be used by
     * concrete product writer implementations in order to signal that the writing process is finished.
     *
     * @param success <code>true</code> if reading was successful, <code>false</code> if an error occured or the user
     *                terminated the write process.
     */
    protected void fireProcessEnded(boolean success) {
        if (_processListeningEnabled && _progressListenerList != null) {
            _progressListenerList.fireProcessEnded(success);
        }
    }

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
     *
     * @see org.esa.beam.framework.dataio.ProductWriterPlugIn#getOutputTypes()
     */
    protected boolean isInstanceOfValidOutputType(Object output) {
        Class[] outputTypes = getWriterPlugIn().getOutputTypes();
        for (int i = 0; i < outputTypes.length; i++) {
            if (outputTypes[i].isInstance(output)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns wether the given product node is to be written.
     *
     * @param node the product node
     *
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
}
