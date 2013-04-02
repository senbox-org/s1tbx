/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.framework.processor;

import com.bc.ceres.core.NullProgressMonitor;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.bc.jexp.ParseException;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.processor.ui.ProcessorUI;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.ResourceInstaller;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.SystemUtils;

import javax.swing.JFrame;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * Base class for all scientific processors using the processor framework. Override this class to create new
 * processors.
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public abstract class Processor {

    private static final String _defaultUITitle = "BEAM Processor";

    /**
     * The request currently processed.
     */
    private Request _request;
    private RequestLogger _requestLogger;

    private DefaultRequestElementFactory _elementFactory;
    private JFrame _parentFrame;
    private Logger _logger;
    private int _currentState;
    private String _defaultHelpId;
    private String _defaultHelpSetPath;

    /**
     * The list of processor status listeners.
     *
     * @link aggregation
     */
    private List<ProcessorStatusListener> _processorStatusListeners;
    private File _auxdataInstallDir;
    private File _defaultAuxdataInstallDir;
    private Set<String> bandNamesToCopy = new LinkedHashSet<String>();

    /**
     * Retrieves the current request.
     */
    public final Request getRequest() {
        return _request;
    }

    /**
     * Retrieves whether the processor has the aborted flag set - or not.
     */
    public final boolean isAborted() {
        return getCurrentStatus() == ProcessorConstants.STATUS_ABORTED;
    }

    /**
     * Retrieves whether the processor has the failed flag set - or not.
     */
    public final boolean isFailed() {
        return getCurrentStatus() == ProcessorConstants.STATUS_FAILED;
    }

    /**
     * Returns the current state of this processor.
     *
     * @return the current state, normally always one of the <code>PSTATE_</code>XXX constants defined in this class.
     */
    public int getCurrentStatus() {
        return _currentState;
    }

    /**
     * Retrieves the warning messages that occurred during processing (if any). This string is shown in the
     * application dialog popping up after the processor ended processing with warnings.
     * Override to perform processor specific warning messages.
     *
     * @return the messages
     */
    public String[] getWarningMessages() {
        return new String[]{""};
    }

    /**
     * Initializes the processor. Override to perform processor specific initializations.
     *
     * @param request the request to be processed next
     *
     * @throws IllegalArgumentException when called with null argument
     */
    public void setRequest(Request request) {
        Guardian.assertNotNull("request", request);
        _request = request;
    }

    /**
     * Retrieves the parent frame containing the processor specific UI
     */
    public JFrame getParentFrame() {
        return _parentFrame;
    }

    /**
     * Retrieves the title to be shown in the user interface. Override this method to set a processor specific title
     * string.
     */
    public String getUITitle() {
        return _defaultUITitle;
    }

    /**
     * Creates the UI for the processor. Override to perform processor specific UI initializations.
     */
    public ProcessorUI createUI() throws ProcessorException {
        return null;
    }

    /**
     * Gets the name of the resource bundle to be used for the application.
     *
     * @return the resource bundle's name or null if not set
     */
    public String getResourceBundleName() {
        return null;
    }

    public String getDefaultHelpId() {
        return _defaultHelpId;
    }

    public void setDefaultHelpId(String defaultHelpId) {
        _defaultHelpId = defaultHelpId;
    }

    public String getDefaultHelpSetPath() {
        return _defaultHelpSetPath;
    }

    public void setDefaultHelpSetPath(String defaultHelpSetPath) {
        _defaultHelpSetPath = defaultHelpSetPath;
    }

    /**
     * Initializes the processor. Override to perform processor specific initialization. Called by the framework after
     * the logging is initialized.
     */
    public void initProcessor() throws ProcessorException {
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @deprecated in 4.0, override {@link #process(com.bc.ceres.core.ProgressMonitor)} instead
     */
    public void process() throws ProcessorException {
        _logger.warning("This processor uses a deprectated API.");
        _logger.warning("Should implement Processor.process(ProgressMonitor) instead.");
    }

    /**
     * Worker method. Override to perform processor specific processing
     */
    public void process(ProgressMonitor pm) throws ProcessorException {
        // need to do this, because old processors have overridden process().
        process();
    }

    /**
     * Retrieves the name of the processor
     */
    public abstract String getName();

    /**
     * Returns the symbolic name of the processor.
     */
    public String getSymbolicName() {
        return StringUtils.createValidName(getName().toLowerCase(), new char[]{'-', '.'},
                                           '-');  // todo - override in each processor!!!
    }

    /**
     * Retrieves a version string of the processor
     */
    public abstract String getVersion();

    /**
     * Retrieves copyright information of the processor
     */
    public abstract String getCopyrightInformation();

    /**
     * @deprecated in 4.0, use {@link #processRequest(Request, ProgressMonitor)}
     */
    public void processRequest(Request request) throws ProcessorException {
        processRequest(request, ProgressMonitor.NULL);
    }

    /**
     * Template method. Performa all actions needed to process one request. The following steps are run: <ul>
     * <li>setRequest(request)</li> <li>logHeader()</li> <li>logRequest()</li> <li>process()</li> </ul> Additionally the
     * method traces the processor state according to the success or failure of the processing
     */
    public void processRequest(Request request, ProgressMonitor pm) throws ProcessorException {
        setCurrentStatus(ProcessorConstants.STATUS_STARTED);
        try {
            setRequest(request);
            logHeader();
            logRequest();
            process(pm);
            if (getCurrentStatus() == ProcessorConstants.STATUS_STARTED) {
                setCurrentStatus(ProcessorConstants.STATUS_COMPLETED);
            }
        } catch (ProcessorException e) {
            setCurrentStatus(ProcessorConstants.STATUS_FAILED);
            cleanupAfterFailure();
            throw e;
        } finally {
            bandNamesToCopy.clear();
        }
    }

    /**
     * Adds a <code>ProcessorStatusListener</code> to this processor runner. The <code>ProcessorStatusListener</code> is
     * informed each time a processor in this processor runner fire an event.
     *
     * @param listener the listener to be added
     *
     * @return boolean if listener was added or not
     */
    public boolean addProcessorStatusListener(ProcessorStatusListener listener) {
        if (listener != null) {
            if (_processorStatusListeners == null) {
                _processorStatusListeners = new ArrayList<ProcessorStatusListener>();
            }
            if (!_processorStatusListeners.contains(listener)) {
                _processorStatusListeners.add(listener);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes a <code>ProcessorStatusListener</code> from this product.
     */
    public void removeProcessorStatusListener(ProcessorStatusListener listener) {
        if (listener != null && _processorStatusListeners != null) {
            _processorStatusListeners.remove(listener);
        }
    }

    /**
     * Sets the UI parent frame containing the processor specific UI.
     */
    public void setParentFrame(JFrame parent) {
        Guardian.assertNotNull("parent", parent);
        _parentFrame = parent;
    }

    /**
     * Prints a general help message when no commandline is supplied
     */
    public void printUsage() {
        ProcessorRunner.printUsage(this);
    }

    /**
     * Retrieve a message to be displayed on completion.
     */
    public String getCompletionMessage() {
        return null;
    }

    /**
     * Sets the current state of this processor. If the status changes, all registered
     * <code>ProcessorStatusListener</code> are informed about the change.
     *
     * @param state the new state, normally always one of the <code>PSTATE_</code>XXX constants defined in this class.
     */
    public void setCurrentStatus(int state) {
        int oldState = _currentState;
        if (state == oldState) {
            return;
        }
        _currentState = state;
        fireStatusChanged(oldState);
    }

    /**
     * Retrieves the request element facory for this processor. Override this method to provide a processor specific
     * element factory.
     */
    public RequestElementFactory getRequestElementFactory() {
        if (_elementFactory == null) {
            _elementFactory = DefaultRequestElementFactory.getInstance();
        }
        return _elementFactory;
    }

    /**
     * Gets the number of different progress levels during the processing. The value is used e.g. to set the number of
     * progress bars in the processors UI. By default, <code>1</code> is returned.
     *
     * @return the number progress levels
     */
    public int getProgressDepth() {
        return 1;
    }

    /**
     * Gets a progress message for the request passed in. Override this method if you need custom messaging.
     *
     * @param request
     *
     * @return a progress message, never null
     */
    public String getProgressMessage(Request request) {
        return "Processing data ...";
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Constructs a new processor with default parameters.
     */
    protected Processor() {
        _requestLogger = new RequestLogger();
        _currentState = ProcessorConstants.STATUS_UNKNOWN;
        _logger = Logger.getLogger(ProcessorConstants.PACKAGE_LOGGER_NAME);
    }


    /**
     * Dispatches a status changed message to all listeners attached.
     *
     * @param oldStatus the status before the status change
     */
    protected synchronized void fireStatusChanged(int oldStatus) {
        if (_processorStatusListeners != null && _processorStatusListeners.size() > 0) {
            ProcessorStatusEvent event = new ProcessorStatusEvent(this, oldStatus);
            for (ProcessorStatusListener listener : _processorStatusListeners) {
                fireStatusChanged(listener, event);
            }
        }
    }

    /**
     * Logs the processor specific logging file header information. Such as: <ul> <li>processor type <li>processor
     * version <li>author <li> etc. </ul>
     * <p/>
     * <p>This method is called by the processor runner initially after a logging sink has been created.
     * <p/>
     * <p>The default implementation does nothing. Override this method to perform processor specific stuff.
     */
    protected void logHeader() {
    }

    /**
     * Logs the request currently used to the logging file. This method is invoked by the processor framework each time
     * before the process() method is invoked.
     */
    protected void logRequest() {
        if (_request != null) {
            _requestLogger.logRequest(_request);
        }
    }

    /**
     * Checks that the given parameter is not null. When it is - throws a ProcessorException.
     *
     * @param param       the parameter to be checked
     * @param description a textual description of the parameter checked
     */
    protected void checkParamNotNull(Object param, String description) throws ProcessorException {
        if (param == null) {
            throw new ProcessorException("Parameter \"" + description + "\" is null!");
        }
    }

    /**
     * Called after an exception is caught during the processing to give the processor an opportuninty to perform
     * "after-crash-cleanup"
     */
    protected void cleanupAfterFailure() {
    }

    protected Product loadInputProduct(int index) throws ProcessorException,
                                                         IOException {
        final Request request = getRequest();
        int numInputProducts = request.getNumInputProducts();

        if ((index < 0) || (index >= numInputProducts)) {
            final String message = "The requested product number '" + index + "' is not contained in the processing request.";
            _logger.severe(message);
            throw new ProcessorException(message);
        }
        final ProductRef prodRef = request.getInputProductAt(index);

        if (prodRef == null) {
            _logger.severe(ProcessorConstants.LOG_MSG_NO_INPUT_IN_REQUEST);
            throw new ProcessorException(ProcessorConstants.LOG_MSG_NO_INPUT_IN_REQUEST);
        }

        final File prodFile = prodRef.getFile();
        Debug.trace("checking product file: " + prodRef.getFilePath());
        if (!prodFile.exists() || !prodFile.isFile()) {
            final String message = "Input product '" + prodRef.getFilePath() + "' does not exist";
            _logger.severe(message);
            throw new IOException(message);
        }

        final Product inputProduct = ProductIO.readProduct(prodFile);
        if (inputProduct == null) {
            final String message = "Cannot open input product '" + prodRef.getFilePath() + "'";
            _logger.severe(message);
            throw new IOException(message);
        }

        _logger.info("Loaded input product '" + prodRef.getFilePath() + "'");
        return inputProduct;
    }

    protected void copyRequestMetaData(Product outputProduct) {
        Request request = getRequest();

        MetadataElement root = outputProduct.getMetadataRoot();
        if ((root != null) && (request != null)) {
            root.addElement(request.convertToMetadata());
        }
    }

    /**
     * Copies the flags bands data from input to output product
     *
     * @deprecated since BEAM 4.6.2, use {@link #copyFlagBands(Product, Product)} instead.
     */
    @Deprecated
    protected final void copyFlagBandData(Product inputProduct, Product outputProduct, ProgressMonitor pm) throws
                                                                                                           IOException,
                                                                                                           ProcessorException {
        if (inputProduct.getFlagCodingGroup().getNodeCount() > 0) {
            // loop over bands and check if they have a flags coding attached
            for (int n = 0; n < inputProduct.getNumBands(); n++) {
                if (inputProduct.getBandAt(n).getFlagCoding() != null) {
                    copyBandData(inputProduct.getBandAt(n).getName(), inputProduct, outputProduct, pm);
                }
            }
        }
    }

    /**
     * Copies the data for the bands with the given band names from input product to output product.
     *
     * @param bandNames     the names for all the bands for which data will be copied.
     * @param inputProduct  the product that contains the source bands
     * @param outputProduct the product that contains the destination bands
     * @param pm            a monitor to inform the user about progress
     *
     * @throws IOException        if the data could not be copied because of an I/O error
     * @throws ProcessorException if the data could not be copied because any other reason
     */
    protected final void copyBandData(String[] bandNames,
                                      Product inputProduct,
                                      Product outputProduct,
                                      ProgressMonitor pm) throws IOException,
                                                                 ProcessorException {
        pm.beginTask("Copying band data ...", bandNames.length);
        try {
            for (String bandName : bandNames) {
                copyBandData(bandName, inputProduct, outputProduct, SubProgressMonitor.create(pm, 1));
                if (pm.isCanceled()) {
                    return;
                }
            }
        } finally {
            pm.done();
        }
    }

    /**
     * Copies the data for the band with the given band name from input product to output product.
     *
     * @param bandName      the name of the bands which data will be copied.
     * @param inputProduct  the product that should contain the source band.
     * @param outputProduct the product that should contain the destination band.
     * @param pm            a monitor to inform the user about progress
     *
     * @throws IOException
     * @throws ProcessorException
     */
    protected void copyBandData(String bandName,
                                Product inputProduct,
                                Product outputProduct, ProgressMonitor pm) throws ProcessorException,
                                                                                  IOException {
        final String m0 = "Unable to copy band data because ";
        final String m1 = "the sourceProduct '";
        final String m2 = "' does not contain a band named '"; /*I18N*/
        final String m3 = "'."; /*I18N*/

        Band sourceBand = inputProduct.getBand(bandName);
        assert sourceBand != null;
        if (sourceBand == null) {
            String message = m0 + m1 + inputProduct.getName() + m2 + bandName + m3;
            _logger.severe(message);
            throw new ProcessorException(message);
        }

        Band destBand = outputProduct.getBand(bandName);
        assert destBand != null;
        if (sourceBand == null) {
            String message = m0 + m1 + inputProduct.getName() + m2 + bandName + m3;
            _logger.severe(message);
            throw new ProcessorException(message);
        }

        int sourceType = sourceBand.getDataType();
        int destType = destBand.getDataType();
        if (sourceType != destType) {
            String message = m0 + "the data types for the bands do not match.";
            _logger.severe(message);
            throw new ProcessorException(message);
        }

        int width = sourceBand.getRasterWidth();
        int height = sourceBand.getRasterHeight();

        pm.beginTask("Copying data...", height * 2);
        try {
            int geophysicalDataType = sourceBand.getGeophysicalDataType();
            if (ProductData.isIntType(geophysicalDataType)) {
                int[] ints = new int[width];
                for (int line = 0; line < height; line++) {
                    sourceBand.readPixels(0, line, width, 1, ints, SubProgressMonitor.create(pm, 1));
                    destBand.writePixels(0, line, width, 1, ints, ProgressMonitor.NULL);
                    pm.worked(1);
                    if (pm.isCanceled()) {
                        return;
                    }
                }
            } else if (geophysicalDataType == ProductData.TYPE_FLOAT32) {
                float[] floats = new float[width];
                for (int line = 0; line < height; line++) {
                    sourceBand.readPixels(0, line, width, 1, floats, SubProgressMonitor.create(pm, 1));
                    destBand.writePixels(0, line, width, 1, floats, ProgressMonitor.NULL);
                    pm.worked(1);
                    if (pm.isCanceled()) {
                        return;
                    }

                }
            } else if (geophysicalDataType == ProductData.TYPE_FLOAT64) {
                double[] doubles = new double[width];
                for (int line = 0; line < height; line++) {
                    sourceBand.readPixels(0, line, width, 1, doubles, SubProgressMonitor.create(pm, 1));
                    destBand.writePixels(0, line, width, 1, doubles, ProgressMonitor.NULL);
                    pm.worked(1);
                    if (pm.isCanceled()) {
                        return;
                    }

                }
            }
        } finally {
            pm.done();
        }
    }


    /**
     * Gets all band names which shall be copied to the output product.
     *
     * @return The names of bands to be copied.
     *
     * @see #addToBandNamesToCopy(String)
     * @see #copyBand(String, Product, Product)
     * @see #copyFlagBands(Product, Product)
     */
    protected String[] getBandNamesToCopy() {
        return bandNamesToCopy.toArray(new String[bandNamesToCopy.size()]);
    }

    /**
     * Adds the band name to the internal list of band which shall be copied.
     *
     * @param bandName The name of the band.
     *
     * @see #getBandNamesToCopy()
     * @see #copyBand(String, Product, Product)
     * @see #copyFlagBands(Product, Product)
     */
    protected void addToBandNamesToCopy(String bandName) {
        bandNamesToCopy.add(bandName);
    }

    /**
     * Copies the band with the given {@code bandName} from the {@code inputProduct}
     * to the {@code outputProduct}, if the band exists in the {@code inputProduct}.
     * <p/>
     * The band is added to the copy list by calling {@link #addToBandNamesToCopy(String) addToBandNamesToCopy(bandName)}.
     *
     * @param bandName      The name of the band to be copied.
     * @param inputProduct  The input product.
     * @param outputProduct The output product.
     *
     * @see #copyBandData(String[], Product, Product, ProgressMonitor)
     * @see #addToBandNamesToCopy (String)
     * @see #getBandNamesToCopy()
     */
    protected void copyBand(String bandName, Product inputProduct, Product outputProduct) {
        if (!outputProduct.containsBand(bandName)) {
            final Band band = ProductUtils.copyBand(bandName, inputProduct, outputProduct, false);
            if (band != null) {
                addToBandNamesToCopy(bandName);
            }
        }
    }

    /**
     * Copies the {@link GeoCoding geo-coding} from the input to the output product.
     *
     * @param inputProduct  The input product.
     * @param outputProduct The output product.
     */
    protected void copyGeoCoding(Product inputProduct, Product outputProduct) {
        Set<String> bandsToCopy = getBandNamesForGeoCoding(inputProduct);
        for (String bandName : bandsToCopy) {
            copyBand(bandName, inputProduct, outputProduct);
            final Band srcBand = inputProduct.getBand(bandName);
            final Band destBand = outputProduct.getBand(bandName);
            destBand.setSourceImage(srcBand.getSourceImage());
        }
        ProductUtils.copyGeoCoding(inputProduct, outputProduct);
    }

    /**
     * Copies all flag bands together with their flagcoding from the input product
     * to the outout product. The band will be remembered in a list which can be retrieved
     * by {@link #getBandNamesToCopy()} and copying with
     * the {@link #copyBandData(String[], Product, Product, ProgressMonitor)} method.
     *
     * @param inputProduct  The input product.
     * @param outputProduct The output product.
     */
    protected void copyFlagBands(Product inputProduct, Product outputProduct) {
        ProductUtils.copyFlagBands(inputProduct, outputProduct, false);
        if (inputProduct.getFlagCodingGroup().getNodeCount() > 0) {
            // loop over bands and check if they have a flags coding attached
            for (int n = 0; n < inputProduct.getNumBands(); n++) {
                final Band band = inputProduct.getBandAt(n);
                if (band.getFlagCoding() != null) {
                    addToBandNamesToCopy(band.getName());
                }
            }
        }
    }

    private Set<String> getBandNamesForGeoCoding(Product inputProduct) {
        Set<String> bandsToCopy = new LinkedHashSet<String>();
        GeoCoding geoCoding = inputProduct.getGeoCoding();
        if (geoCoding != null && geoCoding instanceof PixelGeoCoding) {
            PixelGeoCoding pixelGeoCoding = (PixelGeoCoding) geoCoding;
            bandsToCopy.add(pixelGeoCoding.getLonBand().getName());
            bandsToCopy.add(pixelGeoCoding.getLatBand().getName());
            String validMask = pixelGeoCoding.getValidMask();
            if (validMask != null) {
                try {
                    RasterDataNode[] refRasters = BandArithmetic.getRefRasters(validMask, new Product[]{inputProduct});
                    for (RasterDataNode rasterDataNode : refRasters) {
                        bandsToCopy.add(rasterDataNode.getName());
                    }
                } catch (ParseException ignore) {
                }
            }
        }
        return bandsToCopy;
    }


    /**
     * @deprecated in 4.1, use {@link #installAuxdata(java.net.URL, String, java.io.File)} instead
     */
    protected void installAuxdata(URL sourceLocation, String relSourcePath, URL targetLocation) {
        try {
            installAuxdata(sourceLocation, relSourcePath, new File(targetLocation.toURI()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void installAuxdata() throws ProcessorException {
        File auxdataInstallDir = getAuxdataInstallDir();
        if (auxdataInstallDir == null) {
            auxdataInstallDir = getDefaultAuxdataInstallDir();
            setAuxdataInstallDir(auxdataInstallDir);
        }
        try {
            installAuxdata(ResourceInstaller.getSourceUrl(getClass()), "auxdata/", auxdataInstallDir);
        } catch (IOException e) {
            throw new ProcessorException("Failed to install auxdata into " + auxdataInstallDir, e);
        }
    }

    protected void installAuxdata(URL sourceLocation, String sourceRelPath, File auxdataInstallDir) throws IOException {
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceLocation, sourceRelPath,
                                                                          auxdataInstallDir);
        if (getParentFrame() != null) { // UI-mode?
            ProgressMonitorSwingWorker swingWorker = new ProgressMonitorSwingWorker(getParentFrame(),
                                                                                    "Installing Auxdata") {
                @Override
                protected Object doInBackground(ProgressMonitor progressMonitor) throws Exception {
                    resourceInstaller.install(".*", progressMonitor);
                    return Boolean.TRUE;
                }
            };
            swingWorker.executeWithBlocking();
            // cause former exception possibly thrown in doInBackground() to be thrown again
            try {
                swingWorker.get();
            } catch (InterruptedException e) {
                throw new IOException(e.getMessage());
            } catch (ExecutionException e) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                } else if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        } else { // command-line mode
            resourceInstaller.install(".*", new NullProgressMonitor());
        }
    }

    public File getDefaultAuxdataInstallDir() {
        if (_defaultAuxdataInstallDir == null) {
            _defaultAuxdataInstallDir = new File(SystemUtils.getApplicationDataDir(), getSymbolicName() + "/auxdata");
        }
        return _defaultAuxdataInstallDir;
    }

    public File getAuxdataInstallDir() {
        return _auxdataInstallDir;
    }

    protected void setAuxdataInstallDir(File auxdataInstallDir) {
        _auxdataInstallDir = auxdataInstallDir;
    }

    protected void setAuxdataInstallDir(String auxdataDirPropertyName, File defaultAuxdataInstallDir) {
        Guardian.assertNotNullOrEmpty("auxdataDirPropertyName", auxdataDirPropertyName);
        String auxdataDirPath = System.getProperty(auxdataDirPropertyName, defaultAuxdataInstallDir.getAbsolutePath());
        setAuxdataInstallDir(new File(auxdataDirPath));
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PROTECTED
    ///////////////////////////////////////////////////////////////////////////

    private static void fireStatusChanged(ProcessorStatusListener listener, ProcessorStatusEvent event) {
        listener.handleProcessingStateChanged(event);
        switch (event.getNewStatus()) {
            case ProcessorConstants.STATUS_STARTED:
                listener.handleProcessingStarted(event);
                break;
            case ProcessorConstants.STATUS_COMPLETED:
            case ProcessorConstants.STATUS_COMPLETED_WITH_WARNING:
                listener.handleProcessingCompleted(event);
                break;
            case ProcessorConstants.STATUS_ABORTED:
                listener.handleProcessingAborted(event);
                break;
            case ProcessorConstants.STATUS_FAILED:
                listener.handleProcessingFailed(event);
                break;
        }
    }

}
