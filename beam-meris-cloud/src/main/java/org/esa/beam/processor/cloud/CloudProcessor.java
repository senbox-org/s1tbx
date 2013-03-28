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
package org.esa.beam.processor.cloud;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.processor.Processor;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.ui.ProcessorUI;
import org.esa.beam.processor.cloud.internal.FrameSizeCalculator;
import org.esa.beam.processor.cloud.internal.LinebasedFrameSizeCalculator;
import org.esa.beam.processor.cloud.internal.util.PNHelper;
import org.esa.beam.util.Debug;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>CloudProcessor</code> implements all specific functionality to calculate a cloud probability.
 *
 * @deprecated since BEAM 4.11. No replacement.
 */
@Deprecated
public class CloudProcessor extends Processor {

    public static final String PROCESSOR_NAME = "Cloud Probability Processor";
    private static final String PROCESSOR_SYMBOLIC_NAME = "beam-meris-cloud";
    private static final String PROCESSOR_VERSION = "1.5.203";
    private static final String PROCESSOR_COPYRIGHT = "Copyright (C) 2004 by ESA, FUB and Brockmann Consult";

    public static final String DEFAULT_OUTPUT_DIR_NAME = "processor";
    public static final String DEFAULT_OUTPUT_FORMAT = DimapProductConstants.DIMAP_FORMAT_NAME;
    public static final String DEFAULT_OUTPUT_PRODUCT_NAME = "MER_CLOUD";

    public static final String REQUEST_TYPE = "MER_L2_CLOUD";

    private Product l1bProduct;
    private Product cloudProduct;

    private Logger _logger;

    private CloudPN cloudNode;
    private Band[] cloudNodeBands;
    private FrameSizeCalculator frameSizeCalculator;
    public static final String HELP_ID = "cloudScientificTool";

    public CloudProcessor() {
        _logger = Logger.getLogger(CloudConstants.LOGGER_NAME);
        setDefaultHelpId(HELP_ID);

    }

    /**
     * Initializes the processor. Override to perform processor specific initialization. Called by the framework after
     * the loggining is initialized.
     */
    @Override
    public void initProcessor() throws ProcessorException {
        super.initProcessor();


    }


    @Override
    public void process(ProgressMonitor pm) throws ProcessorException {
        ProcessorUtils.setProcessorLoggingHandler(CloudConstants.DEFAULT_LOG_PREFIX, getRequest(),
                                                  getName(), getVersion(), getCopyrightInformation());
        pm.beginTask("Processing cloud product...", 10);
        try {
            _logger.info(CloudConstants.LOG_MSG_START_REQUEST);

            // check the request type
            Request.checkRequestType(getRequest(), REQUEST_TYPE);

            initCloudNode();

            // create the output product
            initOutputProduct(SubProgressMonitor.create(pm, 1));

            // and process the processor
            processCloud(SubProgressMonitor.create(pm, 9));

            _logger.info(CloudConstants.LOG_MSG_SUCCESS);
        } catch (Exception e) {
            _logger.log(Level.SEVERE, CloudConstants.LOG_MSG_PROC_ERROR + e.getMessage(), e);
            throw new ProcessorException(e.getMessage(), e);
        } finally {
            pm.done();
            try {
                if (isAborted()) {
                    deleteOutputProduct();
                }
            } finally {
                closeProducts();
                _logger.info(CloudConstants.LOG_MSG_FINISHED_REQUEST);
            }
        }
    }

    /**
     * Retrieves the name of the processor
     */
    @Override
    public String getName() {
        return PROCESSOR_NAME;
    }

    /**
     * Returns the symbolic name of the processor.
     */
    @Override
    public String getSymbolicName() {
        return PROCESSOR_SYMBOLIC_NAME;
    }

    /**
     * Retrieves a version string of the processor
     */
    @Override
    public String getVersion() {
        return PROCESSOR_VERSION;
    }

    /**
     * Retrieves copyright information of the processor
     */
    @Override
    public String getCopyrightInformation() {
        return PROCESSOR_COPYRIGHT;
    }

    /**
     * Creates the UI for the processor. Override to perform processor specific
     * UI initializations.
     */
    @Override
    public ProcessorUI createUI() throws ProcessorException {
        return new CloudProcessorUI();
    }
    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private void initCloudNode() throws ProcessorException {
        installAuxdata();

        // cloud node
        final Map<String, String> cloudConfig = new HashMap<String, String>();
        cloudConfig.put(CloudPN.CONFIG_FILE_NAME, "cloud_config.txt");
//        cloudConfig.put(CloudPN.INVALID_EXPRESSION, "l1_flags.INVALID OR NOT l1_flags.LAND_OCEAN");
        cloudConfig.put(CloudPN.INVALID_EXPRESSION, "l1_flags.INVALID");
        cloudNode = new CloudPN();
        try {
            cloudNode.setUp(cloudConfig);
        } catch (IOException e) {
            throw new ProcessorException("Failed to initialise cloud source: " + e.getMessage(), e);
        }
    }

    @Override
    public void installAuxdata() throws ProcessorException {
        setAuxdataInstallDir(CloudPN.CLOUD_AUXDATA_DIR_PROPERTY, getDefaultAuxdataInstallDir());
        super.installAuxdata();
    }


    /**
     * Creates the output product skeleton.
     */
    private void initOutputProduct(ProgressMonitor pm) throws Exception {
        l1bProduct = loadInputProduct(0);
        if (!EnvisatConstants.MERIS_L1_TYPE_PATTERN.matcher(l1bProduct.getProductType()).matches()) {
            throw new ProcessorException("Product type '" + l1bProduct.getProductType() + "' is not supported." +
                    "It must be a MERIS Level 1b product.");
        }
        cloudProduct = cloudNode.readProductNodes(l1bProduct, null);
        cloudNodeBands = cloudProduct.getBands();

        copyFlagBands(l1bProduct, cloudProduct);
//        PNHelper.copyAllBandsToProduct(l1bProduct, cloudProduct, true);
        ProductUtils.copyTiePointGrids(l1bProduct, cloudProduct);
        
        copyGeoCoding(l1bProduct, cloudProduct);
        cloudProduct.setStartTime(l1bProduct.getStartTime());
        cloudProduct.setEndTime(l1bProduct.getEndTime());
        copyRequestMetaData(cloudProduct);
        
        prepareProcessing();

        PNHelper.initWriter(getRequest().getOutputProductAt(0), cloudProduct, _logger);
        copyBandData(getBandNamesToCopy(), l1bProduct, cloudProduct, pm);
        _logger.info(CloudConstants.LOG_MSG_OUTPUT_CREATED);
    }

    private void prepareProcessing() throws Exception {
        final int width = l1bProduct.getSceneRasterWidth();
        final int height = l1bProduct.getSceneRasterHeight();

        frameSizeCalculator = new LinebasedFrameSizeCalculator(width, height);
        cloudNode.setFrameSizeCalculator(frameSizeCalculator);
        cloudNode.startProcessing();
    }

    /**
     * Performs the actual processing of the output product. Reads both input bands line
     * by line, calculates the processor and writes the result to the output band
     */
    private void processCloud(ProgressMonitor pm) throws IOException {
        final int frameCount = frameSizeCalculator.getFrameCount();

        // Notify process listeners that processing has started
        pm.beginTask("Generating Cloud product...", frameCount * 2);
        try {
            for (int frameNumber = 0; frameNumber < frameCount; frameNumber++) {
                final Rectangle frameRect = frameSizeCalculator.getFrameRect(frameNumber);
                _logger.info("processing Cloud frame: " + (frameNumber + 1) + "/" + frameCount);
                PNHelper.copyBandData(cloudNodeBands, cloudProduct, frameRect, SubProgressMonitor.create(pm, 1));
                PNHelper.copyBandData(l1bProduct.getBand(EnvisatConstants.MERIS_L1B_FLAGS_DS_NAME), cloudProduct,
                                      frameRect, SubProgressMonitor.create(pm, 1));
//                PNHelper.copyBandData(l1bProduct.getBands(), cloudProduct, frameRect);

                // Notify process listeners about processing progress and
                // check whether or not processing shall be terminated
                if (pm.isCanceled()) {
                    // Processing terminated!
                    setCurrentStatus(CloudConstants.STATUS_ABORTED);
                    // Immediately terminate now
                    return;
                }
            }
        } finally {
            pm.done();
        }
    }

    /**
     * Closes any open products.
     */
    private void closeProducts() {
        if (cloudProduct != null) {
            cloudProduct.dispose();
            cloudProduct = null;
        }
    }

    private void deleteOutputProduct() throws ProcessorException {
        if (cloudProduct != null) {
            final ProductWriter writer = cloudProduct.getProductWriter();
            if (writer != null) {
                try {
                    writer.deleteOutput();
                } catch (IOException e) {
                    _logger.warning("Failed to delete uncomplete output product: " + e.getMessage());
                    Debug.trace(e);
                    throw new ProcessorException("Failed to delete uncomplete output product.", e);
                }
            }
        }
    }


    /**
     * Called by framework after a processing failure.
     */
    @Override
    protected final void cleanupAfterFailure() {
        closeProducts();
    }
}
