package org.esa.beam.processor.radtorefl;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.Processor;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.RequestElementFactory;
import org.esa.beam.framework.processor.ui.ProcessorUI;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.math.RsMathUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;


/**
 * Class implementing a radiance-to-reflectance conversion processor
 * for MERIS Level 1b products.
 *
 * @author Ralf Quast
 * @version 1.0
 */
public class RadToReflProcessor extends Processor {

    private static final String INPUT_BAND_NAME_STUB = EnvisatConstants.MERIS_L1B_RADIANCE_1_BAND_NAME.replaceFirst(
            "_1", "");
    private static final String OUTPUT_BAND_NAME_STUB = EnvisatConstants.MERIS_L2_REFLEC_1_BAND_NAME.replaceFirst("_1",
                                                                                                                  "");

    private ArrayList<Band> inputBandList;
    private Product inputProduct;
    private Product outputProduct;
    private TiePointGrid sunZenithBand;

    private String[] inputBandNames;
    private boolean copyInputBands;

    private Logger logger;
    public static final String HELP_ID = "radtoreflScientificTool";

    public RadToReflProcessor() {
        setDefaultHelpId(HELP_ID);
    }

    @Override
    public void initProcessor() throws ProcessorException {
        inputBandList = new ArrayList<Band>();
        logger = Logger.getLogger(RadToReflConstants.LOGGER_NAME);
    }

    /**
     * Processes the current request.
     *
     * @throws ProcessorException on any failure during processing
     */
    @Override
    public void process(ProgressMonitor pm) throws ProcessorException {
        pm.beginTask("Creating output product...", inputBandList.size() + 1);
        try {
            try {
                logger.info(ProcessorConstants.LOG_MSG_START_REQUEST);
                ProcessorUtils.setProcessorLoggingHandler(RadToReflConstants.DEFAULT_LOG_PREFIX,
                                                          getRequest(), getName(), getVersion(),
                                                          getCopyrightInformation());

                readRequestParams();
                readInputProduct();

                if (isValidInputProduct(inputProduct)) {
                    readSunZenithData();
                    createInputBandList();
                } else {
                    throw new ProcessorException(RadToReflConstants.LOG_MSG_UNSUPPORTED_PRODUCT);
                }

                createOutputProduct(SubProgressMonitor.create(pm, 1));

                if (pm.isCanceled()) {
                    setCurrentStatus(ProcessorConstants.STATUS_ABORTED);
                    return;
                }

                pm.setSubTaskName(RadToReflConstants.LOG_MSG_PROC_START);

                for (Band band : inputBandList) {
                    try {
                        processBand(band, SubProgressMonitor.create(pm, 1));
                        if (pm.isCanceled()) {
                            setCurrentStatus(ProcessorConstants.STATUS_ABORTED);
                            return;
                        }
                    } catch (IOException e) {
                        logger.severe(ProcessorConstants.LOG_MSG_PROC_ERROR);
                        logger.severe(e.getMessage());
                    }
                }
            } finally {
                cleanUp();
                pm.done();
            }
        } catch (IOException e) {
            throw new ProcessorException("An I/O error occured:\n" + e.getMessage(), e);
        }

        logger.info(ProcessorConstants.LOG_MSG_FINISHED_REQUEST);
    }

    /**
     * Creates the UI for the processor.
     */
    @Override
    public ProcessorUI createUI() {
        return RadToReflUIFactory.createUI();
    }

    /**
     * Retrieves the request element factory for the Rad2Refl processor
     */
    @Override
    public RequestElementFactory getRequestElementFactory() {
        return RadToReflRequestElementFactory.getInstance();
    }

    /**
     * Retrieves the titlestring to be shown in the user interface.
     */
    @Override
    public String getUITitle() {
        return RadToReflConstants.PROCESSOR_NAME;
    }

    /**
     * Retrieves the name of the processor
     */
    @Override
    public String getName() {
        return RadToReflConstants.PROCESSOR_NAME;
    }

    /**
     * Returns the symbolic name of the processor.
     */
    @Override
    public String getSymbolicName() {
        return RadToReflConstants.PROCESSOR_SYMBOLIC_NAME;
    }

    /**
     * Retrieves a version string of the processor
     */
    @Override
    public String getVersion() {
        return RadToReflConstants.VERSION;
    }

    /**
     * Retrieves copyright information of the processor
     */
    @Override
    public String getCopyrightInformation() {
        return RadToReflConstants.COPYRIGHT;
    }

    /**
     * Writes the processor specific logging file header to the logstream currently set.
     */
    @Override
    public void logHeader() {
        logger.info(RadToReflConstants.LOG_MSG_HEADER);
        logger.info(RadToReflConstants.COPYRIGHT);
        logger.info("");
    }

    /**
     * Retrieves a progress message for the request passed in. Override this method if you need custom messaging.
     *
     * @param request
     *
     * @return the progress message for the request
     */
    @Override
    public String getProgressMessage(final Request request) {
        return RadToReflConstants.LOG_MSG_PROC_START;
    }

    /**
     * Retrieves the number of progress bars needed by the processor. Override this method if more than one progressbar
     * is needed, i.e. for multistage processes.
     *
     * @return the number of progress bars needed.
     */
    @Override
    public int getProgressDepth() {
        return 2;
    }


    /**
     * Tests if the specified product is suitable for processing.
     *
     * @param product the <code>Product</code>
     *
     * @return <code>true</code> if the specified <code>Product</code>
     *         is suitable for processing; <code>false</code> otherwise.
     */
    public static boolean isValidInputProduct(final Product product) {
        final String type = product.getProductType();

        return EnvisatConstants.MERIS_L1_TYPE_PATTERN.matcher(type).matches();
    }


    /**
     * Tests if the specified name denotes a <code>Band</code> suitable
     * for processing.
     *
     * @param name the name
     *
     * @return <code>true</code> if the specified name denotes a <code>Band</code>
     *         suitable for processing; <code>false</code> otherwise.
     */
    public static boolean isValidInputBand(final String name) {
        for (String bandName : EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES) {
            if (bandName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    ///////////////////////////////////////////////////////////////////////////
    //////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Reads the requested input <code>Product</code>.
     */
    private void readInputProduct() throws IOException,
                                           ProcessorException {
        inputProduct = loadInputProduct(0);
    }

    /**
     * Reads the sun zenith tie point data of the input <code>Product</code>.
     */
    private void readSunZenithData() throws ProcessorException {
        logger.info(RadToReflConstants.LOG_MSG_LOAD_SZA_START);

        sunZenithBand = inputProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME);
        checkParamNotNull(sunZenithBand, EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME);

        logger.fine(RadToReflConstants.LOG_MSG_LOAD_SZA_END);
    }

    /**
     * Validates the requested input bands and collects them into a list.
     */
    private void createInputBandList() throws ProcessorException {
        if (inputBandNames.length == 0) {
            throw new ProcessorException("No bands specified for processing");
        }
        inputBandList.clear();

        for (final String name : inputBandNames) {
            if (isValidInputBand(name)) {
                final Band band = inputProduct.getBand(name);

                if (band == null) {
                    logger.warning("The input product does not contain the requested band '" + name + "'");
                } else {
                    if (inputBandList.contains(band)) {
                        logger.warning("The band '" + name + "' is requested twice");
                    } else {
                        inputBandList.add(band);
                    }
                }
            } else {
                logger.warning("The requested band '" + name + "' is not a valid input band");
            }
        }
    }

    /**
     * Creates the requested output <code>Product</code>.
     */
    private void createOutputProduct(ProgressMonitor pm) throws IOException,
                                                                ProcessorException {
        final ProductRef outputProductRef = getRequest().getOutputProductAt(0);
        final String productName = getProductName(outputProductRef, ProcessorConstants.LOG_MSG_NO_OUTPUT_IN_REQUEST);
        final String productType = inputProduct.getProductType();
        final File productFile = new File(outputProductRef.getFilePath());
        final ProductWriter productWriter = ProcessorUtils.createProductWriter(outputProductRef);

        outputProduct = new Product(productName, productType,
                                    inputProduct.getSceneRasterWidth(), inputProduct.getSceneRasterHeight());
        outputProduct.setFileLocation(productFile);
        outputProduct.setProductWriter(productWriter);
        outputProduct.setStartTime(inputProduct.getStartTime());
        outputProduct.setEndTime(inputProduct.getEndTime());

        copyRequestMetaData(outputProduct);
        ProductUtils.copyTiePointGrids(inputProduct, outputProduct);
        createOutputProductBands(pm);
    }


    /**
     * Creates the <code>Bands</code> required for the output <code>Product</code>.
     *
     * @throws IOException
     * @throws ProcessorException
     */
    private void createOutputProductBands(ProgressMonitor pm) throws IOException,
                                                                     ProcessorException {
        if (copyInputBands) {
            for (final Band band : inputBandList) {
                ProductUtils.copyBand(band.getName(), inputProduct, outputProduct);
            }
        }
        for (final Band inputBand : inputBandList) {
            final Band outputBand = new Band(outputBandName(inputBand.getName()),
                                             inputBand.getGeophysicalDataType(), inputBand.getSceneRasterWidth(),
                                             inputBand.getSceneRasterHeight());
            final String inputBandDescription = inputBand.getDescription();

            if (inputBandDescription != null) {
                final String outputBandDescription =
                        inputBandDescription.replaceFirst("radiance", "reflectance");

                outputBand.setDescription(outputBandDescription);
            }
            outputBand.setUnit(EnvisatConstants.MERIS_REFLECTANCE_UNIT);
            ProductUtils.copySpectralBandProperties(inputBand, outputBand);

            outputBand.setNoDataValue(inputBand.getNoDataValue());
            outputBand.setNoDataValueUsed(inputBand.isNoDataValueUsed());
            outputBand.setValidPixelExpression(outputBand.getName() + ">= 0 AND " + outputBand.getName() + "<= 1");

            outputProduct.addBand(outputBand);
        }
        ProductUtils.copyFlagBands(inputProduct, outputProduct);

        outputProduct.getProductWriter().writeProductNodes(outputProduct,
                                                           outputProduct.getFileLocation());

        pm.beginTask("Copying band data...", inputBandList.size() + 1);
        try {
            if (copyInputBands) {
                for (Band band : inputBandList) {
                    if (pm.isCanceled()) {
                        return;
                    }
                    copyBandData(band.getName(), inputProduct, outputProduct, SubProgressMonitor.create(pm, 1));
                }
            }
            copyFlagBandData(inputProduct, outputProduct, pm);
            pm.worked(1);
        } finally {
            pm.done();
        }
    }


    /**
     * Helper Routine. Returns the name of the output <code>band</code>
     * corresponding to the given input <code>band</code> name.
     *
     * @param inputBandName the input <code>Band</code> name
     */
    private static String outputBandName(String inputBandName) {
        return inputBandName.replaceFirst(RadToReflProcessor.INPUT_BAND_NAME_STUB,
                                          RadToReflProcessor.OUTPUT_BAND_NAME_STUB);
    }


    /**
     * Processes a single input band.
     *
     * @param inputBand the input <code>Band</code>
     */
    private void processBand(Band inputBand, ProgressMonitor pm) throws IOException {
        logger.info(
                RadToReflConstants.LOG_MSG_PROC_BAND_1 + inputBand.getName() + RadToReflConstants.LOG_MSG_PROC_BAND_2);

        final Band reflectanceBand = outputProduct.getBand(outputBandName(inputBand.getName()));

        final int rasterWidth = inputBand.getSceneRasterWidth();
        final int rasterHeight = inputBand.getSceneRasterHeight();

        final float[] sza = new float[rasterWidth];
        final float[] rad = new float[rasterWidth];

        pm.beginTask(RadToReflConstants.LOG_MSG_PROC_BAND_1 + inputBand.getName() +
                     RadToReflConstants.LOG_MSG_PROC_BAND_2,
                     rasterHeight * 2);
        try {
            for (int y = 0; y < rasterHeight; y++) {
                inputBand.readPixels(0, y, rasterWidth, 1, rad, SubProgressMonitor.create(pm, 1));
                sunZenithBand.readPixels(0, y, rasterWidth, 1, sza, SubProgressMonitor.create(pm, 1));

                RsMathUtils.radianceToReflectance(rad, sza, inputBand.getSolarFlux(), rad);
                reflectanceBand.writePixels(0, y, rasterWidth, 1, rad, ProgressMonitor.NULL);

                if (pm.isCanceled()) {
                    logger.warning(ProcessorConstants.LOG_MSG_PROC_CANCELED);
                    setCurrentStatus(ProcessorConstants.STATUS_ABORTED);
                    return;
                }
            }
        } finally {
            pm.done();
        }

        logger.info(ProcessorConstants.LOG_MSG_PROC_SUCCESS);
    }

    /**
     * Assigns request parameters to corresponding fields
     */
    private void readRequestParams() {
        Parameter par;

        par = getRequest().getParameter(RadToReflConstants.INPUT_BANDS_PARAM_NAME);
        if (par != null) {
            inputBandNames = (String[]) par.getValue();
        } else {
            inputBandNames = RadToReflConstants.INPUT_BANDS_PARAM_DEFAULT;
        }

        par = getRequest().getParameter(RadToReflConstants.COPY_INPUT_BANDS_PARAM_NAME);
        if (par != null) {
            copyInputBands = (Boolean) par.getValue();
        } else {
            copyInputBands = RadToReflConstants.COPY_INPUT_BANDS_PARAM_DEFAULT;
        }
    }

    private void cleanUp() throws IOException {
        if (inputProduct != null) {
            inputProduct.dispose();
            inputProduct = null;
        }
        if (outputProduct != null) {
            if (isAborted()) {
                outputProduct.getProductWriter().deleteOutput();
            }
            outputProduct.dispose();
            outputProduct = null;
        }
    }

    @Override
    protected void cleanupAfterFailure() {
        try {
            cleanUp();
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
    }

    /**
     * Retrieves the product name for a given <code>ProductRef</code>.
     *
     * @param productRef   the <code>ProductRef</code>
     * @param errorMessage error message to be thrown with <code>ProcessorException</code>
     *
     * @throws ProcessorException
     */
    private static String getProductName(final ProductRef productRef, final String errorMessage)
            throws ProcessorException {
        if (productRef == null) {
            throw new ProcessorException(errorMessage);
        }
        File productFile = new File(productRef.getFilePath());

        return FileUtils.getFilenameWithoutExtension(productFile);
    }
}
