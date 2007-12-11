/*
 * $Id: SmileProcessor.java,v 1.14 2007/03/28 11:37:01 norman Exp $
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
package org.esa.beam.processor.smile;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.jexp.Term;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.Processor;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.RequestElementFactory;
import org.esa.beam.framework.processor.ui.ProcessorUI;
import org.esa.beam.util.Debug;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The Smile Correction Processor class.
 *
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision: 1.14 $ $Date: 2007/03/28 11:37:01 $
 */
public class SmileProcessor extends Processor {

    public static final String PROCESSOR_NAME = "BEAM Smile Correction Processor";
    public static final String VERSION_STRING = "1.1.2";
    public static final String COPYRIGHT_INFO = "Copyright (C) 2002-2004 by Brockmann Consult (info@brockmann-consult.de)";

    public static final double SPECTRAL_BAND_SF_FACTOR = 1.1;

    private final static String _DETECTOR_INDEX_BAND_NAME = EnvisatConstants.MERIS_L1B_BAND_NAMES[EnvisatConstants.MERIS_L1B_BAND_NAMES.length - 1];

    private Product _inputProduct;
    private Product _outputProduct;
    private SmileAuxData _auxData;
    private boolean _includeAllSpectralBands;
    private String[] _bandNamesToProcessIn;
    private Term _bitMaskTermLand;
    private Term _bitmaskTermProcess;
    private double[/*15*/][] _radianceLineCache;
    private Logger _logger;
    private static final String _processingMessage = "Generating pixels for smile corrected radiances...";
    public static final String HELP_ID = "smileScientificToolPlugIn";

    /**
     * Constructs the processor with default parameter.
     */
    public SmileProcessor() {
        _logger = Logger.getLogger(SmileConstants.LOGGER_NAME);
        setDefaultHelpId(HELP_ID);
    }

    /**
     * Processes the request actually set.
     *
     * @throws org.esa.beam.framework.processor.ProcessorException
     *          on any failure during processing
     */
    @Override
    public void process(ProgressMonitor pm) throws ProcessorException {
        try {
            _logger.info(SmileConstants.LOG_MSG_START_REQUEST);

            // initialize logging for the request
            ProcessorUtils.setProcessorLoggingHandler(SmileConstants.DEFAULT_LOG_PREFIX, getRequest(),
                                                      getName(), getVersion(), getCopyrightInformation());
            loadInputProduct();
            setProcessingParameters();
            installAuxdata();
            loadAuxdata();
            createOutputProduct();
            createBitmaskTermLand(SmileConstants.BITMASK_TERM_LAND);
            createBitmaskTermProcess(SmileConstants.BITMASK_TERM_PROCESS);
            pm.beginTask("Computing smile correction...", _includeAllSpectralBands ? 4 : 3);
            try {
                processSmileCorrection(SubProgressMonitor.create(pm, 1));
                if (pm.isCanceled()) {
                    setCurrentStatus(SmileConstants.STATUS_ABORTED);
                    return;
                }
                pm.setSubTaskName("Copying flag band data...");
                copyFlagBandData(SubProgressMonitor.create(pm, 1));
                if (pm.isCanceled()) {
                    setCurrentStatus(SmileConstants.STATUS_ABORTED);
                    return;
                }
                pm.setSubTaskName("Copying detector index band data...");
                copyDetectorIndexBandData(SubProgressMonitor.create(pm, 1));
                if (pm.isCanceled()) {
                    setCurrentStatus(SmileConstants.STATUS_ABORTED);
                    return;
                }
                if (_includeAllSpectralBands) {
                    pm.setSubTaskName("Copying other spectral band data...");
                    copyRequiredBandDataThatIsNotProcessed(SubProgressMonitor.create(pm, 1));
                    if (pm.isCanceled()) {
                        setCurrentStatus(SmileConstants.STATUS_ABORTED);
                    }
                }
            } finally {
                try {
                    if (isAborted()) {
                        removeOutputProduct();
                    }
                } finally {
                    closeProducts();
                    pm.done();
                }
            }
        } catch (IOException e) {
            _logger.severe(SmileConstants.LOG_MSG_PROC_ABORTED);
            _logger.severe(e.getMessage());
            throw new ProcessorException("An I/O error occured:\n" + e.getMessage(), e);
        } finally {
            if (getCurrentStatus() == SmileConstants.STATUS_STARTED) {
                _logger.info(SmileConstants.LOG_MSG_PROC_SUCCESS);
            }
        }
    }

    private void createMetadataElements() {
        assert _auxData != null;

        final MetadataAttribute theorLamAttr = new MetadataAttribute("theor_band_wavelen", ProductData.TYPE_FLOAT64,
                                                                     15);
        theorLamAttr.setDataElems(_auxData.getTheoreticalWavelengths());
        theorLamAttr.setDescription("Theoretical band wavelength as used by the Smile Correction.");
        theorLamAttr.setUnit("nm");

        final MetadataAttribute theorE0Attr = new MetadataAttribute("theor_sun_spec_flux", ProductData.TYPE_FLOAT64,
                                                                    15);
        theorE0Attr.setDataElems(_auxData.getTheoreticalSunSpectralFluxes());
        theorE0Attr.setDescription(
                "Sun spectral flux for theoretical band wavelength as used by the Smile Correction.");
        theorE0Attr.setUnit("LU");

        MetadataElement element = new MetadataElement("aux_data");
        element.addAttribute(theorLamAttr);
        element.addAttribute(theorE0Attr);

        _outputProduct.getMetadataRoot().addElement(element);
    }

    /**
     * Retrieves the name of the processor.
     */
    @Override
    public String getName() {
        return PROCESSOR_NAME;
    }

    /**
     * Gets the number of different progress levels during the processing. The value is used e.g. to set the number of
     * progress bars in the processors UI. By default, <code>1</code> is returned.
     *
     * @return the number progress levels
     */
    @Override
    public int getProgressDepth() {
        return 2;
    }

    /**
     * Retrieves a version string of the processor
     */
    @Override
    public String getVersion() {
        return VERSION_STRING;
    }

    /**
     * Retrieves copyright information of the processor
     */
    @Override
    public String getCopyrightInformation() {
        return COPYRIGHT_INFO;
    }

    /**
     * Called after an exception is caught during the processing to give the processor an opportuninty to perform
     * "after-crash-cleanup"
     */
    @Override
    protected void cleanupAfterFailure() {
        // removed call removeOutputProduct() to cause it makes no
        // sense because the product is already closed
        try {
            closeProducts();
        } catch (IOException e) {
            Debug.trace(e);
        }
    }

    /**
     * Creates the processor specific UI component and returns it to caller.
     */
    @Override
    public ProcessorUI createUI() throws ProcessorException {
        return new SmileProcessorUI();
    }

    /**
     * Returns the request element factory for this processor.
     */
    @Override
    public RequestElementFactory getRequestElementFactory() {
        return SmileRequestElementFactory.getInstance();
    }

    /**
     * Retrieves a progress message for the request passed in. Override this method if you need custom messaging.
     *
     * @param request
     * @return the progress message for the request
     */
    @Override
    public String getProgressMessage(final Request request) {
        return _processingMessage;
    }

    public void installAuxdata() throws ProcessorException {
        setAuxdataInstallDir(SmileConstants.AUXDATA_DIR_PROPERTY, getDefaultAuxdataInstallDir());
        super.installAuxdata();
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////


    private void removeOutputProduct() throws ProcessorException {
        if (_outputProduct != null) {
            final ProductWriter productWriter = _outputProduct.getProductWriter();
            if (productWriter != null) {
                try {
                    productWriter.deleteOutput();
                } catch (IOException e) {
                    _logger.warning("Failed to delete uncomplete output product: " + e.getMessage());
                    Debug.trace(e);
                    throw new ProcessorException("Failed to delete uncomplete output product.", e);
                }
            }
        }
    }


    private void setProcessingParameters() throws ProcessorException {
        final Request request = getRequest();

        // check that we have the correct request type
        // -------------------------------------------
        Request.checkRequestType(request, SmileConstants.REQUEST_TYPE);

        // when this parameter is set to true, the output product contains all spectral bands,
        // whether they have been smile corrected or not. When set too false, the output product
        // contains only processed specral bands.
        _includeAllSpectralBands = true;
        Parameter includeAllParam = request.getParameter(SmileConstants.PARAM_NAME_OUTPUT_INCLUDE_ALL_SPECTRAL_BANDS);
        if (includeAllParam != null) {
            _includeAllSpectralBands = Boolean.valueOf(includeAllParam.getValueAsText());
            _logger.info(SmileConstants.LOG_MSG_INCLUDE_ALL_BANDS);
        }

        _bandNamesToProcessIn = new String[0];
        Parameter bandsToProcessParam = request.getParameter(SmileConstants.PARAM_NAME_BANDS_TO_PROCESS);
        if (bandsToProcessParam != null) {
            _bandNamesToProcessIn = (String[]) bandsToProcessParam.getValue();
            _logger.info(SmileConstants.LOG_MSG_PROCESS_BANDS);
            for (String a_bandNamesToProcessIn : _bandNamesToProcessIn) {
                _logger.info("...... '" + a_bandNamesToProcessIn + "'");
            }
        } else {
            _logger.warning(SmileConstants.LOG_MSG_NO_BANDS);
        }
    }

    /**
     * Loads the first input product in the processing request.
     *
     * @throws ProcessorException
     * @throws IOException
     */
    private void loadInputProduct() throws ProcessorException,
            IOException {
        _inputProduct = loadInputProduct(0);
        if (!_inputProduct.containsBand(_DETECTOR_INDEX_BAND_NAME)) {
            throw new ProcessorException(SmileConstants.LOG_MSG_WRONG_PRODUCT); /*I18N*/
        }
    }

    /**
     * Creates the output product specified in the processing request.
     *
     * @throws ProcessorException
     * @throws IOException
     */
    private void createOutputProduct() throws ProcessorException,
            IOException {
        ProductRef prod;
        ProductWriter writer;

        // take only the first output product. There might be more but we will ignore
        // these in this processor.
        prod = getRequest().getOutputProductAt(0);
        if (prod == null) {
            throw new ProcessorException(ProcessorConstants.LOG_MSG_NO_OUTPUT);
        }

        String productName = FileUtils.getFilenameWithoutExtension(new File(prod.getFilePath()));
        if (productName == null || productName.length() == 0) {
            throw new ProcessorException(SmileConstants.LOG_MSG_NO_OUTPUT_NAME);
        }

        String productType = _inputProduct.getProductType();
        if (productType == null) {
            throw new ProcessorException(SmileConstants.LOG_MSG_NO_INPUT_TYPE);
        }
        // @todo 1 nf/he - make sure its not dangerous to copy source product type
        // productType += "_SMILE";
        int sceneWidth = _inputProduct.getSceneRasterWidth();
        int sceneHeight = _inputProduct.getSceneRasterHeight();

        writer = ProcessorUtils.createProductWriter(prod);

        _outputProduct = new Product(productName, productType, sceneWidth, sceneHeight);
        _outputProduct.setProductWriter(writer);

        String[] bandNames;
        if (_includeAllSpectralBands) {
            bandNames = EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES;
        } else {
            bandNames = _bandNamesToProcessIn;
        }
        for (String bandName : bandNames) {
            ProductUtils.copyBand(bandName, _inputProduct, _outputProduct);

            final Band band = _outputProduct.getBand(bandName);
            final double sfOld = band.getScalingFactor();
            final double sfNew = sfOld * SPECTRAL_BAND_SF_FACTOR;
            band.setScalingFactor(sfNew);
        }

        ProductUtils.copyTiePointGrids(_inputProduct, _outputProduct);
        ProductUtils.copyGeoCoding(_inputProduct, _outputProduct);
        copyRequestMetaData(_outputProduct);
        ProductUtils.copyFlagBands(_inputProduct, _outputProduct);
        ProductUtils.copyBand(_DETECTOR_INDEX_BAND_NAME, _inputProduct, _outputProduct);

        createMetadataElements();

        // initialize the disk represenation
        // ---------------------------------
        writer.writeProductNodes(_outputProduct, new File(prod.getFilePath()));
    }

    private void copyDetectorIndexBandData(ProgressMonitor pm) throws ProcessorException,
            IOException {
        _logger.info(SmileConstants.LOG_MSG_COPY_DETECTOR_BAND);
        copyBandData(_DETECTOR_INDEX_BAND_NAME, _inputProduct, _outputProduct, pm);
        _logger.info(SmileConstants.LOG_MSG_SUCCESS);
    }

    private void copyFlagBandData(ProgressMonitor pm) throws IOException,
            ProcessorException {
        _logger.info(SmileConstants.LOG_MSG_COPY_FLAG_BAND);
        copyFlagBandData(_inputProduct, _outputProduct, pm);
        _logger.info(SmileConstants.LOG_MSG_SUCCESS);
    }

    private void copyRequiredBandDataThatIsNotProcessed(ProgressMonitor pm) throws ProcessorException,
            IOException {
        _logger.info(SmileConstants.LOG_MSG_COPY_UNPROCESSED_BANDS);

        if (_includeAllSpectralBands) {
            for (String bandName : EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES) {
                if (!StringUtils.contains(_bandNamesToProcessIn, bandName)) {
                    copyBandData(bandName, _inputProduct, _outputProduct, pm);
                }
                if (isAborted()) {
                    break;
                }
            }
        }

        _logger.info(SmileConstants.LOG_MSG_SUCCESS);
    }

    /**
     * Creates a the bitmask term for land bitmask from the bitmask expression passed in.
     */
    private void createBitmaskTermLand(String expression) throws ProcessorException {
        assert expression != null;
        assert expression.trim().length() > 0;

        // something is set - try to create a valid expression
        _bitMaskTermLand = ProcessorUtils.createTerm(expression, _inputProduct);
        _logger.info("Using land pixel bitmask: '" + expression + "'");
    }

    /**
     * Creates a the bitmask term for land bitmask from the bitmask expression passed in.
     */
    private void createBitmaskTermProcess(String expression) throws ProcessorException {
        assert expression != null;
        assert expression.trim().length() > 0;

        // something is set - try to create a valid expression
        _bitmaskTermProcess = ProcessorUtils.createTerm(expression, _inputProduct);
        _logger.info("Using pixel validation bitmask: '" + expression + "'");
    }

    private void processSmileCorrection(ProgressMonitor pm) throws IOException {
        final int width = _inputProduct.getSceneRasterWidth();
        final int height = _inputProduct.getSceneRasterHeight();

        // @todo 3 he/he - move this code block to another method
        assert _bandNamesToProcessIn != null;
        final Set<Integer> bandIndexSet = new HashSet<Integer>();
        for (final String bandNameToProcess : _bandNamesToProcessIn) {
            final int index = _inputProduct.getBandIndex(bandNameToProcess);
            if (index >= 0) {
                bandIndexSet.add(index);
            }
        }
        int[] _bandIndexesToProcessIn = intSetToSortedIntArray(bandIndexSet);
        int[] _bandIndexesToProcessOut = new int[_bandIndexesToProcessIn.length];
        for (int i = 0; i < _bandIndexesToProcessOut.length; i++) {
            final String bandName = _inputProduct.getBandAt(_bandIndexesToProcessIn[i]).getName();
            _bandIndexesToProcessOut[i] = _outputProduct.getBandIndex(bandName);
        }

        final int numBandsToProcess = _bandIndexesToProcessIn.length;

        pm.beginTask(_processingMessage, height);
        try {
            if (numBandsToProcess == 0) {
                return;
            }

            final Band detectorIndexBand = _inputProduct.getBand(EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME);
            assert detectorIndexBand != null;

            final int[] bandIndexesRequired = computeRequiredBandIndexes(_bandIndexesToProcessIn);

            // Variables providing values for a entire scan line
            initRadianceLineCache(bandIndexesRequired, width);
            final boolean[/*w*/] landPixelFlagLine = new boolean[width];
            final boolean[/*w*/] validPixelFlagLine = new boolean[width];
            final int[/*w*/] detectorIndexLine = new int[width];

            final double[][/*15*/] detectorWLsTable = _auxData.getDetectorWavelengths();
            final double[][/*15*/] detectorE0sTable = _auxData.getDetectorSunSpectralFluxes();

            // Variables providing values for a single pixel
            boolean[/*15*/] shouldCorrect;
            int[/*15*/] indexes1;
            int[/*15*/] indexes2;
            final double[/*15*/] theoretWLs = _auxData.getTheoreticalWavelengths();
            final double[/*15*/] theoretE0s = _auxData.getTheoreticalSunSpectralFluxes();
            double[/*15*/] detectorWLs;
            double[/*15*/] detectorE0s;
            final double[/*15*/] radiances = new double[15];
            final double[/*15*/] corrRadiances = new double[15];

            int detectorIndex;
            boolean detectorIndexValid;
            boolean correctionPossible;
            int bandIndex;

            for (int row = 0; row < height; row++) {
                readRadianceLines(bandIndexesRequired, row, pm);
                _inputProduct.readBitmask(0, row, landPixelFlagLine.length, 1, _bitMaskTermLand, landPixelFlagLine,
                                          ProgressMonitor.NULL);
                _inputProduct.readBitmask(0, row, validPixelFlagLine.length, 1, _bitmaskTermProcess,
                                          validPixelFlagLine, ProgressMonitor.NULL);
                detectorIndexBand.readPixels(0, row, detectorIndexLine.length, 1, detectorIndexLine,
                                             ProgressMonitor.NULL);

                for (int col = 0; col < width; col++) {
                    detectorIndex = detectorIndexLine[col];
                    detectorIndexValid = (detectorIndex >= 0 && detectorIndex < detectorWLsTable.length);
                    correctionPossible = validPixelFlagLine[col] && detectorIndexValid;
                    if (correctionPossible) {
                        for (int aBandIndexesRequired : bandIndexesRequired) {
                            bandIndex = aBandIndexesRequired;
                            radiances[bandIndex] = getRadianceLine(bandIndex)[col];
                        }

                        detectorWLs = detectorWLsTable[detectorIndex];
                        detectorE0s = detectorE0sTable[detectorIndex];

                        if (landPixelFlagLine[col]) {
                            shouldCorrect = _auxData.getRadCorrFlagsLand();
                            indexes1 = _auxData.getLowerBandIndexesLand();
                            indexes2 = _auxData.getUpperBandIndexesLand();
                        } else {
                            shouldCorrect = _auxData.getRadCorrFlagsWater();
                            indexes1 = _auxData.getLowerBandIndexesWater();
                            indexes2 = _auxData.getUpperBandIndexesWater();
                        }

                        SmileCorrectionAlgorithm.computeSmileCorrectedRadiances(_bandIndexesToProcessIn,
                                                                                shouldCorrect,
                                                                                indexes1,
                                                                                indexes2,
                                                                                radiances,
                                                                                theoretWLs,
                                                                                theoretE0s,
                                                                                detectorWLs,
                                                                                detectorE0s,
                                                                                corrRadiances);

                        for (int a_bandIndexesToProcessIn : _bandIndexesToProcessIn) {
                            bandIndex = a_bandIndexesToProcessIn;
                            getRadianceLine(bandIndex)[col] = corrRadiances[bandIndex];
                        }
                    }
                }

                // write to output product
                for (int i = 0; i < _bandIndexesToProcessIn.length; i++) {
                    final double[] radianceLine = getRadianceLine(_bandIndexesToProcessIn[i]);
                    final Band outputBand = _outputProduct.getBandAt(_bandIndexesToProcessOut[i]);
                    outputBand.writePixels(0, row, radianceLine.length, 1, radianceLine, ProgressMonitor.NULL);
                }

                pm.worked(1);
                if (pm.isCanceled()) {
                    setCurrentStatus(SmileConstants.STATUS_ABORTED);
                    return;
                }
            }
        } finally {
            pm.done();
        }
    }

    private void closeProducts() throws IOException {
        if (_inputProduct != null) {
            _inputProduct.closeProductReader();
        }
        if (_outputProduct != null) {
            _outputProduct.closeProductWriter();
        }
    }

    private int[] computeRequiredBandIndexes(final int[] bandIndexesToProcess) {
        final boolean[] landShouldCorrect = _auxData.getRadCorrFlagsLand();
        final int[] landIndexes1 = _auxData.getLowerBandIndexesLand();
        final int[] landIndexes2 = _auxData.getUpperBandIndexesLand();
        final boolean[] waterShouldCorrect = _auxData.getRadCorrFlagsWater();
        final int[] waterIndexes1 = _auxData.getLowerBandIndexesWater();
        final int[] waterIndexes2 = _auxData.getUpperBandIndexesWater();

        final Set<Integer> bandIndexSet = new HashSet<Integer>();

        for (final int bandIndex : bandIndexesToProcess) {
            bandIndexSet.add(bandIndex);
            if (landShouldCorrect[bandIndex]) {
                bandIndexSet.add(landIndexes1[bandIndex]);
                bandIndexSet.add(landIndexes2[bandIndex]);
            }
            if (waterShouldCorrect[bandIndex]) {
                bandIndexSet.add(waterIndexes1[bandIndex]);
                bandIndexSet.add(waterIndexes2[bandIndex]);
            }
        }
        return intSetToSortedIntArray(bandIndexSet);
    }

    private void readRadianceLines(final int[] bandIndexes, final int lineIndex, ProgressMonitor pm) throws
            IOException {
        assert _inputProduct != null;
        assert _radianceLineCache != null;
        assert bandIndexes != null;
        assert lineIndex >= 0;

        pm.beginTask("Reading radiance lines...", bandIndexes.length);
        try {
            for (final int bandIndex : bandIndexes) {
                double[] radianceLine = getRadianceLine(bandIndex);
                _inputProduct.getBandAt(bandIndex).readPixels(0, lineIndex, radianceLine.length, 1, radianceLine,
                                                              SubProgressMonitor.create(pm, 1));
            }
        } finally {
            pm.done();
        }
    }

    // **********************
    // Aux Data   >>>>>>>>>>>
    // **********************

    private void loadAuxdata() throws ProcessorException,
            IOException {
        assert _inputProduct != null;
        final String productType = _inputProduct.getProductType();
        if (productType.startsWith("MER_F")) {
            _auxData = SmileAuxData.loadFRAuxData(getAuxdataInstallDir());
        } else if (productType.startsWith("MER_R")) {
            _auxData = SmileAuxData.loadRRAuxData(getAuxdataInstallDir());
        } else {
            throw new ProcessorException("No auxillary data found for input product of type '"
                    + _inputProduct.getProductType() + "'"); /*I18N*/
        }
    }

    // **************************
    // Line cache   >>>>>>>>>>>
    // **************************

    private void initRadianceLineCache(final int[] bandsIndexes, final int width) {
        _radianceLineCache = new double[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS][];
        for (int neededBandsIndex : bandsIndexes) {
            _radianceLineCache[neededBandsIndex] = new double[width];
        }
    }

    private double[] getRadianceLine(final int index) {
        return _radianceLineCache[index];
    }

    // ***************************
    // Line listener   >>>>>>>>>>>
    // ***************************
//    private Processor.LineListener getLineListenerInstance() {
//        if (_lineListener == null) {
//            _lineListener = new Processor.LineListener() {
//                public boolean lineWritten() {
//                    _inProgress = fireProcessInProgress(++_currentProgressValue);
//                    return !_inProgress;
//                }
//
//                public boolean linesWritten(int numLines) {
//                    _inProgress = fireProcessInProgress(_currentProgressValue + numLines);
//                    return !_inProgress;
//                }
//            };
//        }
//        return _lineListener;
//    }

    private static int[] intSetToSortedIntArray(final Set<Integer> set) {
        int[] bandIndexes = new int[set.size()];
        Integer[] a = new Integer[bandIndexes.length];
        a = set.toArray(a);
        for (int i = 0; i < a.length; i++) {
            bandIndexes[i] = a[i];
        }
        Arrays.sort(bandIndexes);
        return bandIndexes;
    }
}
