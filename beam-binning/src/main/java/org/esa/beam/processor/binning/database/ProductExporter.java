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

package org.esa.beam.processor.binning.database;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapTransformFactory;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.processor.binning.L3Constants;
import org.esa.beam.processor.binning.L3Context;
import org.esa.beam.processor.binning.L3ProjectionRaster;
import org.esa.beam.processor.binning.algorithm.Algorithm;
import org.esa.beam.util.io.FileUtils;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.logging.Logger;

@Deprecated
/**
 * Export a TemporalBinDatabse into a product.
 *
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class ProductExporter {

    protected TemporalBinDatabase binDatabase;
    protected Logger logger;
    protected L3ProjectionRaster projection;
    protected float stepsPerDegree;
    protected Product outputProduct;

    public ProductExporter(TemporalBinDatabase binDatabase, Logger logger) {
        this.binDatabase = binDatabase;
        this.logger = logger;
    }

    /**
     * Sets rthe projection to be used and the gridCell size in steps per degree.
     *
     * @param projection
     * @param stepsPerDegree
     */
    public void setProjection(L3ProjectionRaster projection, float stepsPerDegree) {
        this.projection = projection;
        this.stepsPerDegree = stepsPerDegree;
    }

    /**
     * Calculates the size of the product from the content of the binDatabase.
     *
     * @param pm a monitor to inform the user about progress
     *
     * @throws ProcessorException
     * @throws IOException
     */
    public void estimateExportRegion(ProgressMonitor pm) throws ProcessorException,
                                                                IOException {
        final GeoPos upperLeft = new GeoPos();
        final GeoPos upperRight = new GeoPos();
        final GeoPos lowerRight = new GeoPos();
        final GeoPos lowerLeft = new GeoPos();

        binDatabase.scanBorders(upperLeft, upperRight, lowerRight, lowerLeft, pm);
        initProjection(upperLeft, upperRight, lowerRight, lowerLeft);
    }

    /**
     * Set the size of the product by its border.
     *
     * @param border
     *
     * @throws ProcessorException
     */
    public void setExportRegion(Rectangle2D border) throws ProcessorException {
        final GeoPos upperLeft = new GeoPos((float) border.getMaxY(), (float) border.getMinX());
        final GeoPos upperRight = new GeoPos((float) border.getMaxY(), (float) border.getMaxX());
        final GeoPos lowerRight = new GeoPos((float) border.getMinY(), (float) border.getMaxX());
        final GeoPos lowerLeft = new GeoPos((float) border.getMinY(), (float) border.getMinX());

        initProjection(upperLeft, upperRight, lowerRight, lowerLeft);
    }

    /**
     * Creates the output product
     *
     * @param outputProductRef
     * @param metadata
     */
    public void createOutputProduct(ProductRef outputProductRef,
                                    L3Context.BandDefinition[] bandDefinitions,
                                    MetadataElement[] metadata) throws
                                                                                                                 ProcessorException,
                                                                                                                 IOException {
        logger.info(L3Constants.LOG_MSG_CREATE_OUTPUT);

        final int width = projection.getWidth();
        final int height = projection.getHeight();
        final String fileName = FileUtils.getFileNameFromPath(outputProductRef.getFilePath());

        logger.info(L3Constants.LOG_MSG_OUTPUT_DIM_1 + width + L3Constants.LOG_MSG_OUTPUT_DIM_2 + height);

        outputProduct = new Product(fileName, "BEAM_L3", width, height);

        // loop over bands and variables and create the appropriate bands
        for (final L3Context.BandDefinition bandDef : bandDefinitions) {
            final Algorithm algo = bandDef.getAlgorithm();
            final String bandBaseName = bandDef.getBandName();
            for (int var = 0; var < algo.getNumberOfInterpretedVariables(); var++) {
                final String variableName = algo.getInterpretedVariableNameAt(var);
                final String bandName = bandBaseName + "_" + variableName;
                final Band band = new Band(bandName, ProductData.TYPE_FLOAT32, width, height);
                if (!variableName.equals("count")) {
                    band.setValidPixelExpression(bandBaseName + "_count > 0");
                }
                outputProduct.addBand(band);
            }
        }

        generateMapGeocoding();
        if (metadata != null) {
            addMetadata(metadata);
        }

        ProductWriter writer = ProcessorUtils.createProductWriter(outputProductRef);
        outputProduct.setProductWriter(writer);
        writer.writeProductNodes(outputProduct, outputProductRef.getFile());

        logger.info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    /**
     * Processes the actual projection and closes the written product afterwards.
     *
     * @param locator
     *
     * @return aborted true, if the process has been aborted.
     */
    public boolean outputBinDatabase(BinLocator locator, ProgressMonitor pm) throws IOException {
        logger.info(L3Constants.LOG_MSG_APPLY_PROJ);
        boolean aborted = false;
        final int width = projection.getWidth();
        final int height = projection.getHeight();
        final GeoPos geoPos = new GeoPos();
        final Point projPt = new Point();
        final Point binPt = new Point();
        final Bin bin = binDatabase.createBin();

        final int numOutputBands = outputProduct.getBandNames().length;
        final Band[] outputBands = new Band[numOutputBands];
        for (int n = 0; n < numOutputBands; n++) {
            outputBands[n] = outputProduct.getBandAt(n);
        }

        float[][] outputData = new float[numOutputBands][width];

        pm.beginTask(L3Constants.LOG_MSG_APPLY_PROJ, height);
        float[] tempBinContent = null;
        try {
            for (int line = 0; line < height; line++) {
                projPt.y = line;
                for (int col = 0; col < width; col++) {
                    projPt.x = col;

                    projection.pointToGeoPos(projPt, geoPos);
                    locator.getRowCol(geoPos, binPt);
                    binDatabase.read(binPt, bin);
                    tempBinContent = bin.save(tempBinContent);

                    for (int i = 0; i < tempBinContent.length; i++) {
                        outputData[i][col] = tempBinContent[i];
                    }
                }

                for (int n = 0; n < numOutputBands; n++) {
                    outputBands[n].writePixels(0, line, width, 1, outputData[n], ProgressMonitor.NULL);
                }

                // update progressbar
                pm.worked(1);
                if (pm.isCanceled()) {
                    logger.warning(L3Constants.LOG_MSG_PROC_CANCELED);
                    aborted = true;
                    break;
                }
            }
        } finally {
            pm.done();
        }
        logger.info(ProcessorConstants.LOG_MSG_SUCCESS);
        return aborted;
    }

    /**
     * Closes the exporter and with it the product it is writing to.
     */
    public void close() {
        outputProduct.dispose();
        outputProduct = null;
    }

    /**
     * Adds all the necessary metadata nodes to the output product.
     *
     * @param metadata An array of metadata nodes
     */
    public void addMetadata(MetadataElement[] metadata) {
        MetadataElement root = outputProduct.getMetadataRoot();
        for (int i = 0; i < metadata.length; i++) {
            MetadataElement element = metadata[i];
            root.addElement(element);
        }
    }

    /**
     * Generates a map geocoding and adds it to the output product.
     */
    protected void generateMapGeocoding() {
        final Point pt = new Point(0, 0);
        final GeoPos pos = projection.pointToGeoPos(pt, null);

        final MapProjection mapProjection = new MapProjection("Geographic Lat/Lon",
                                                              MapTransformFactory.createTransform("Identity", null));
        // TODO - (nf) pass Datum here
        final MapInfo mapInfo = new MapInfo(mapProjection, 0.5f, 0.5f, pos.lon, pos.lat,
                                            projection.getPixelSize(),
                                            projection.getPixelSize(),
                                            Datum.WGS_84);
        mapInfo.setSceneHeight(outputProduct.getSceneRasterHeight());
        mapInfo.setSceneWidth(outputProduct.getSceneRasterWidth());
        final GeoCoding mapCoding = new MapGeoCoding(mapInfo);

        outputProduct.setGeoCoding(mapCoding);
    }

    /**
     * Initializes the projection parameters needed.
     */
    private void initProjection(GeoPos upperLeft, GeoPos upperRight, GeoPos lowerRight, GeoPos lowerLeft) throws
                                                                                                          ProcessorException {
        logger.info(L3Constants.LOG_MSG_CALC_PROJ_PARAM);

        projection.init(stepsPerDegree, upperLeft, upperRight, lowerRight, lowerLeft);

        if (projection.getWidth() * projection.getHeight() == 0
            || projection.getWidth() == -1
            || projection.getHeight() == -1) {
            throw new ProcessorException("No output product is created, because it would not contain data.\n" +
                                         "Please check your processing parameters.");
        }

        logger.info(L3Constants.LOG_MSG_PROJ_BORDER);
        logger.info(L3Constants.LOG_MSG_LAT_MIN + lowerLeft.getLatString() +
                    L3Constants.LOG_MSG_LAT_MAX + upperLeft.getLatString());
        logger.info(L3Constants.LOG_MSG_LON_MIN + lowerLeft.getLonString() +
                    L3Constants.LOG_MSG_LON_MAX + lowerRight.getLonString());
        logger.info(ProcessorConstants.LOG_MSG_SUCCESS);
    }
}
