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

package org.esa.beam.processor.cloud.internal.util;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;


/**
 * <p><i><b>IMPORTANT NOTE:</b>
 * This class not an API.
 * It is not intended to be used by clients.</i>
 * </p>
 */
public class PNHelper {

    /**
     * Copies the band data int the given region from the source bands into the destination product.
     *
     * @param sourceBands
     * @param destinationProduct
     * @param frameRect
     *
     * @throws IOException
     */
    static public void copyBandData(Band[] sourceBands, Product destinationProduct, Rectangle frameRect,
                                    ProgressMonitor pm) throws IOException {
        pm.beginTask("Copying band data...", sourceBands.length);
        try {
            for (int bandIndex = 0; bandIndex < sourceBands.length; bandIndex++) {
                Band sourceBand = sourceBands[bandIndex];
                copyBandData(sourceBand, destinationProduct, frameRect, SubProgressMonitor.create(pm, 1));
            }
        } finally {
            pm.done();
        }
    }

    /**
     * Copies the band data int the given region from the source bands into the destination product.
     *
     * @param sourceBand
     * @param destinationProduct
     * @param frameRect
     *
     * @throws IOException
     */
    static public void copyBandData(Band sourceBand, Product destinationProduct, Rectangle frameRect,
                                    ProgressMonitor pm) throws IOException {
        Band destBand = destinationProduct.getBand(sourceBand.getName());
        ProductData data = sourceBand.createCompatibleProductData(frameRect.width * frameRect.height);
        pm.beginTask("Copying data of band '" + sourceBand.getName() + "'...", 2);
        try {
            sourceBand.readRasterData(frameRect.x, frameRect.y,
                                      frameRect.width, frameRect.height,
                                      data, SubProgressMonitor.create(pm, 1));
            destBand.writeRasterData(frameRect.x, frameRect.y,
                                     frameRect.width, frameRect.height,
                                     data, SubProgressMonitor.create(pm, 1));
        } finally {
            pm.done();
        }
    }

    /**
     * Copies the tie point data, geocoding and the start and stop time.
     *
     * @param sourceProduct
     * @param destinationProduct
     */
    static public void copyMiscData(Product sourceProduct, Product destinationProduct) {
        // copy all tie point grids to output product
        ProductUtils.copyTiePointGrids(sourceProduct, destinationProduct);
        // copy geo-coding to the output product
        ProductUtils.copyGeoCoding(sourceProduct, destinationProduct);
        destinationProduct.setStartTime(sourceProduct.getStartTime());
        destinationProduct.setEndTime(sourceProduct.getEndTime());
    }

    /**
     * Creates a writer which is specified in the given productRef.
     *
     * @param productRef
     * @param outputProduct
     * @param logger
     *
     * @throws ProcessorException
     * @throws IOException
     */
    static public void initWriter(ProductRef productRef, Product outputProduct, Logger logger) throws
                                                                                               ProcessorException,
                                                                                               IOException {
        if (productRef == null) {
            throw new ProcessorException("No output product in request");
        }
        File outputFile = new File(productRef.getFilePath());
        String outputFileFormat = productRef.getFileFormat();
        if (StringUtils.isNullOrEmpty(outputFileFormat)) {
            outputFileFormat = DimapProductConstants.DIMAP_FORMAT_NAME;
            logger.warning(ProcessorConstants.LOG_MSG_NO_OUTPUT_FORMAT);
            logger.warning(ProcessorConstants.LOG_MSG_USING + outputFileFormat);
        }
        ProductWriter outputWriter = ProductIO.getProductWriter(outputFileFormat);
        if (outputWriter == null) {
            throw new ProcessorException("Invalid output product format: " + outputFileFormat);
        }
        outputProduct.setProductWriter(outputWriter);
        outputWriter.writeProductNodes(outputProduct, outputFile);
    }

}