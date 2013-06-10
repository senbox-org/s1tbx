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
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.processor.binning.L3Constants;
import org.esa.beam.processor.binning.L3Context;
import org.esa.beam.processor.binning.algorithm.Algorithm;
import org.esa.beam.processor.binning.store.BinStoreFactory;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class SpatialBinDatabase extends AbstractBinDatabase {

    protected Product product;

    public SpatialBinDatabase(L3Context context, Product product, Logger logger) {
        this.context = context;
        this.locator = context.getLocator();
        this.product = product;
        this.logger = logger;
    }

    public void processSpatialBinning() throws ProcessorException, IOException {
        processSpatialBinning(ProgressMonitor.NULL);
    }

    public void processSpatialBinning(ProgressMonitor pm) throws ProcessorException, IOException {
        pm.beginTask("Spatial binning of product '" + product.getName() + "'...",
                     context.algorithmNeedsFinishSpatial() ? 4 : 3);
        try {
            estimateProductDimension();
            pm.worked(1);

            createStore();
            pm.worked(1);

            binSpatial(SubProgressMonitor.create(pm, 1));

            if (context.algorithmNeedsFinishSpatial()) {
                finishSpatial(SubProgressMonitor.create(pm, 1));
            }
        } finally {
            pm.done();
        }
    }

    @Override
    public void read(Point gridPoint, Bin bin) throws IOException {
        if (!locator.isValidPosition(gridPoint)) {
            return;
        }
        Point localPoint = gridToLocal(gridPoint, null);
        store.read(localPoint, bin);
    }

    protected void readLocalCoordinates(Point localPoint, Bin bin) throws IOException {
        if (!locator.isValidPosition(localToGrid(localPoint, null))) {
            return;
        }
        store.read(localPoint, bin);
    }

    @Override
    public void write(Point gridPoint, Bin bin) throws IOException {
        if (!locator.isValidPosition(gridPoint) || !bin.containsData()) {
            return;
        }
        Point localPoint = gridToLocal(gridPoint, null);
        store.write(localPoint, bin);
    }

    protected void writeLocalCoordinates(Point localPoint, Bin bin) throws IOException {
        if (!locator.isValidPosition(localToGrid(localPoint, null)) || !bin.containsData()) {
            return;
        }
        store.write(localPoint, bin);
    }

    /**
     * Converts a local (i.e. spatial grid based) row/col pair to a L3 grid based one.
     *
     * @param local the coordinates in local coordinate system
     * @param grid  the point object to be filled - can be null, then a new object is created
     */
    protected Point localToGrid(Point local, Point grid) {
        if (grid == null) {
            grid = new Point();
        }

        grid.y = local.y + rowMin;
        grid.x = local.x + colMin;

        return grid;
    }

    /**
     * Converts a L3 grid based row/col pair to a local grid based one.
     *
     * @param grid  the point object to be filled - can be null, then a new object is created
     * @param local the coordinates in local coordinate system
     */
    protected Point gridToLocal(Point grid, Point local) {
        if (local == null) {
            local = new Point();
        }

        local.y = grid.y - rowMin;
        local.x = grid.x - colMin;

        return local;
    }

    /**
     * Create a binstore that is used for the spatial aggregation.
     *
     * @throws IOException
     * @throws ProcessorException
     */
    protected void createStore() throws IOException, ProcessorException {
        logger.info(L3Constants.LOG_MSG_CREATE_BIN_DB);

        store = BinStoreFactory.getInstance().createSpatialStore(context.getDatabaseDir(),
                                                                 product.getName(), getWidth(), getHeight(),
                                                                 sumVarsPerBin());

        logger.info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    /**
     * Processes the spatial binning on the product.
     */
    protected void binSpatial(ProgressMonitor pm) throws ProcessorException, IOException {

        boolean binModified;
        Bin bin = createBin();

        GeoCoding coding = product.getGeoCoding();
        if (coding == null) {
            throw new ProcessorException("The given product: " + product.getName() + " has no geo-coding");
        }
        final L3Context.BandDefinition[] bandDefs = context.getBandDefinitions();
        final int productWidth = product.getSceneRasterWidth();
        final int productHeight = product.getSceneRasterHeight();
        final Rectangle2D dbBorders = context.getBorder();

        final GeoPos latlon = new GeoPos();
        final Point rowcol = new Point();
        final PixelPos pixelPos = new PixelPos();

        // allocate line vectors
        float[][] value = new float[bandDefs.length][productWidth];
        boolean[][] useData = new boolean[bandDefs.length][productWidth];
        Term[] bitmaskTerm = new Term[bandDefs.length];
        Band[] valueBands = new Band[bandDefs.length];

        for (int bandIndex = 0; bandIndex < bandDefs.length; bandIndex++) {
            final L3Context.BandDefinition bandDef = bandDefs[bandIndex];
            try {
                bitmaskTerm[bandIndex] = product.parseExpression(bandDef.getBitmaskExp());
            } catch (ParseException e) {
                // will not throw exception, is checked before if loadValidatedProduct() is called before
            }
            valueBands[bandIndex] = product.getBand(bandDef.getBandName());
            Arrays.fill(useData[bandIndex], true);
        }

        logger.info(L3Constants.LOG_MSG_SPATIAL_BINNING);
        pm.beginTask("Spatial binning of product '" + product.getName() + "'...", productHeight);
        try {
            // loop over scanlines
            for (int yPos = 0; yPos < productHeight; yPos++) {
                // load data for a line
                for (int bandIndex = 0; bandIndex < bandDefs.length; bandIndex++) {
                    valueBands[bandIndex].readPixels(0, yPos, productWidth, 1, value[bandIndex], ProgressMonitor.NULL);
                    if (bitmaskTerm[bandIndex] != null) {
                        product.readBitmask(0, yPos, productWidth, 1, bitmaskTerm[bandIndex], useData[bandIndex],
                                            ProgressMonitor.NULL);
                    }
                }
                // loop over line-pixels
                for (int xPos = 0; xPos < productWidth; xPos++) {
                    pixelPos.x = xPos + 0.5f;
                    pixelPos.y = yPos + 0.5f;
                    coding.getGeoPos(pixelPos, latlon);
                    if (dbBorders.contains(latlon.lon, latlon.lat)) {
                        locator.getRowCol(latlon, rowcol);
                        read(rowcol, bin);
                        binModified = false;
                        for (int bandIndex = 0; bandIndex < bandDefs.length; bandIndex++) {
                            final L3Context.BandDefinition bandDef = bandDefs[bandIndex];
                            final Algorithm algorithm = bandDef.getAlgorithm();
                            if (useData[bandIndex][xPos]) {
                                bin.setBandIndex(bandIndex);
                                algorithm.accumulateSpatial(value[bandIndex][xPos], bin);
                                binModified = true;
                            }
                        }
                        if (binModified) {
                            write(rowcol, bin);
                        }
                    }
                }

                // update progressbar
                pm.worked(1);
                if (pm.isCanceled()) {
                    break;
                }
            }
            if (!pm.isCanceled()) {
                logger.info(ProcessorConstants.LOG_MSG_SUCCESS);
            }
        } finally {
            flush();
            pm.done();
        }
    }

    /**
     * Finishes the spatial processing if algorithm needs this to be performed
     */
    protected void finishSpatial(ProgressMonitor pm) throws IOException, ProcessorException {

        final int width = getWidth();
        final int height = getHeight();
        Point rowcol = new Point();
        Bin bin = createBin();

        logger.info(L3Constants.LOG_MSG_SPATIAL_FINISH);
        pm.beginTask(L3Constants.LOG_MSG_SPATIAL_FINISH, height);
        try {
            final L3Context.BandDefinition[] bandDefs = context.getBandDefinitions();
            for (rowcol.y = 0; rowcol.y < height; rowcol.y++) {
                for (rowcol.x = 0; rowcol.x < width; rowcol.x++) {
                    readLocalCoordinates(rowcol, bin);
                    for (int bandIndex = 0; bandIndex < bandDefs.length; bandIndex++) {
                        final L3Context.BandDefinition bandDef = bandDefs[bandIndex];
                        final Algorithm algorithm = bandDef.getAlgorithm();
                        bin.setBandIndex(bandIndex);
                        algorithm.finishSpatial(bin);
                    }
                    writeLocalCoordinates(rowcol, bin);
                }

                // update progressbar
                pm.worked(1);
                if (pm.isCanceled()) {
                    break;
                }
            }
            if (!pm.isCanceled()) {
                logger.info(ProcessorConstants.LOG_MSG_SUCCESS);
            }
        } finally {
            pm.done();
        }
    }

    /**
     * Scans the product passed in for the border coordinates.
     */
    protected void estimateProductDimension() throws ProcessorException {
        final GeoPos latlon = new GeoPos();
        final Point rowcol = new Point();
        final PixelPos rowColProduct = new PixelPos();

        initializeMinMax();

        // get information from product
        GeoCoding coding = product.getGeoCoding();
        if (coding == null) {
            throw new ProcessorException("This product has no geocoding");
        }

        final int productWidth = product.getSceneRasterWidth();
        final int productHeight = product.getSceneRasterHeight();

        // loop check west
        rowColProduct.x = 0.5f;
        for (int i = 0; i < productHeight; i++) {
            rowColProduct.y = i + 0.5f;
            coding.getGeoPos(rowColProduct, latlon);
            locator.getRowCol(latlon, rowcol);
            updateBorders(rowcol);
        }

        // loop check north
        rowColProduct.y = 0.5f;
        for (int i = 0; i < productWidth; i++) {
            rowColProduct.x = i + 0.5f;
            coding.getGeoPos(rowColProduct, latlon);
            locator.getRowCol(latlon, rowcol);
            updateBorders(rowcol);
        }

        // loop check east
        rowColProduct.x = productWidth - 1 + 0.5f;
        for (int i = 0; i < productHeight; i++) {
            rowColProduct.y = i + 0.5f;
            coding.getGeoPos(rowColProduct, latlon);
            locator.getRowCol(latlon, rowcol);
            updateBorders(rowcol);
        }

        // loop check south
        rowColProduct.y = productHeight - 1 + 0.5f;
        for (int i = 0; i < productWidth; i++) {
            rowColProduct.x = i + 0.5f;
            coding.getGeoPos(rowColProduct, latlon);
            locator.getRowCol(latlon, rowcol);
            updateBorders(rowcol);
        }

        if (coding.isCrossingMeridianAt180()) {
            width = locator.getWidth() + 1;
        } else {
            width = colMax - colMin + 1;
        }
    }

    /**
     * Checks the rowcol vector passed in agains the actual minmax values and updates minmax if necessary.
     *
     * @param rowcol the point to be checked
     */
    protected void updateBorders(Point rowcol) {
        if (rowMin > rowcol.y) {
            rowMin = rowcol.y;
        }
        if (rowMax < rowcol.y) {
            rowMax = rowcol.y;
        }
        if (colMin > rowcol.x) {
            colMin = rowcol.x;
        }
        if (colMax < rowcol.x) {
            colMax = rowcol.x;
        }
    }
}