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

package org.esa.beam.binning.operator;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.binning.CompositingType;
import org.esa.beam.binning.ObservationSlice;
import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.SpatialBinner;
import org.esa.beam.binning.VariableContext;
import org.esa.beam.binning.support.PlateCarreeGrid;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.StopWatch;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.math.MathUtils;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class which performs a spatial binning of single input products.
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 */
public class SpatialProductBinner {

    /**
     * Processes a source product and generated spatial bins.
     *
     * @param product         The source product.
     * @param spatialBinner   The spatial binner to be used.
     * @param superSampling   The super-sampling rate.
     * @param addedBands      A container for the bands that are added during processing.
     * @param progressMonitor A progress monitor.
     *
     * @return The total number of observations processed.
     *
     * @throws IOException If an I/O error occurs.
     */
    public static long processProduct(Product product,
                                      SpatialBinner spatialBinner,
                                      Integer superSampling,
                                      Map<Product, List<Band>> addedBands,
                                      ProgressMonitor progressMonitor) throws IOException {
        if (product.getGeoCoding() == null) {
            throw new IllegalArgumentException("product.getGeoCoding() == null");
        }
        final VariableContext variableContext = spatialBinner.getBinningContext().getVariableContext();
        addVariablesToProduct(variableContext, product, addedBands);

        PlanetaryGrid planetaryGrid = spatialBinner.getBinningContext().getPlanetaryGrid();
        CompositingType compositingType = spatialBinner.getBinningContext().getCompositingType();
        Geometry sourceProductGeometry = null;
        final MultiLevelImage maskImage;
        if (CompositingType.MOSAICKING.equals(compositingType)) {
            addMaskToProduct(variableContext.getValidMaskExpression(), product, addedBands);
            PlateCarreeGrid plateCarreeGrid = (PlateCarreeGrid) planetaryGrid;
            sourceProductGeometry = plateCarreeGrid.computeProductGeometry(product);
            product = plateCarreeGrid.reprojectToPlateCareeGrid(product);
            maskImage = product.getBand("binning_mask").getGeophysicalImage();
        } else {
            maskImage = getMaskImage(product, variableContext.getValidMaskExpression());
        }

        final MultiLevelImage[] varImages = getVariableImages(product, variableContext);

        final Rectangle[] sliceRectangles;
        if (CompositingType.MOSAICKING.equals(compositingType)) {
            PlateCarreeGrid plateCarreeGrid = (PlateCarreeGrid) planetaryGrid;
            Dimension tileSize = product.getPreferredTileSize();
            sliceRectangles = plateCarreeGrid.getDataSliceRectangles(sourceProductGeometry, tileSize);
        } else {
            final Dimension defaultSliceDimension = getDefaultSliceDimension(product);
            sliceRectangles = computeDataSliceRectangles(maskImage, varImages, defaultSliceDimension);
        }
        final float[] superSamplingSteps = getSuperSamplingSteps(superSampling);
        long numObsTotal = 0;
        progressMonitor.beginTask("Spatially binning of " + product.getName(), sliceRectangles.length);
        for (int idx = 0; idx < sliceRectangles.length; idx++) {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            numObsTotal += processSlice(spatialBinner, progressMonitor, superSamplingSteps, maskImage, varImages,
                                        product, sliceRectangles[idx]);
            stopWatch.stopAndTrace(String.format("Processed slice %d of %d", idx, sliceRectangles.length));
        }
        spatialBinner.complete();
        return numObsTotal;
    }

    private static MultiLevelImage[] getVariableImages(Product product, VariableContext variableContext) {
        final MultiLevelImage[] varImages = new MultiLevelImage[variableContext.getVariableCount()];
        for (int i = 0; i < variableContext.getVariableCount(); i++) {
            final String nodeName = variableContext.getVariableName(i);
            final RasterDataNode node = getRasterDataNode(product, nodeName);
            final MultiLevelImage varImage = node.getGeophysicalImage();
            varImages[i] = varImage;
        }
        return varImages;
    }

    private static MultiLevelImage getMaskImage(Product product, String maskExpr) {
        MultiLevelImage maskImage = null;
        if (StringUtils.isNotNullAndNotEmpty(maskExpr)) {
            maskImage = ImageManager.getInstance().getMaskImage(maskExpr, product);
        }
        return maskImage;
    }

    private static Rectangle[] computeDataSliceRectangles(MultiLevelImage maskImage, MultiLevelImage[] varImages,
                                                          Dimension defaultSliceSize) {

        MultiLevelImage referenceImage = varImages[0];
        Rectangle[] rectangles;
        if (areTilesDirectlyUsable(maskImage, varImages, defaultSliceSize)) {
            final Point[] tileIndices = referenceImage.getTileIndices(null);
            rectangles = new Rectangle[tileIndices.length];
            for (int i = 0; i < tileIndices.length; i++) {
                Point tileIndex = tileIndices[i];
                rectangles[i] = referenceImage.getTileRect(tileIndex.x, tileIndex.y);
            }
        } else {
            int sceneHeight = referenceImage.getHeight();
            int numSlices = MathUtils.ceilInt(sceneHeight / (double) defaultSliceSize.height);
            rectangles = new Rectangle[numSlices];
            for (int i = 0; i < numSlices; i++) {
                rectangles[i] = computeCurrentSliceRectangle(defaultSliceSize, i, sceneHeight);
            }
        }
        return rectangles;
    }

    private static boolean areTilesDirectlyUsable(MultiLevelImage maskImage, MultiLevelImage[] varImages,
                                                  Dimension defaultSliceSize) {
        boolean areTilesUsable = false;
        if (maskImage != null) {
            areTilesUsable = isTileSizeCompatible(maskImage, defaultSliceSize);
        }
        for (MultiLevelImage varImage : varImages) {
            areTilesUsable = areTilesUsable && isTileSizeCompatible(varImage, defaultSliceSize);
        }
        return areTilesUsable;
    }

    private static boolean isTileSizeCompatible(MultiLevelImage image, Dimension defaultSliceSize) {
        return image.getTileWidth() == defaultSliceSize.width && image.getTileHeight() == defaultSliceSize.height;
    }

    private static Rectangle computeCurrentSliceRectangle(Dimension defaultSlice, int sliceIndex, int sceneHeight) {
        int sliceY = sliceIndex * defaultSlice.height;
        int currentSliceHeight = defaultSlice.height;
        if (sliceY + defaultSlice.height > sceneHeight) {
            currentSliceHeight = sceneHeight - sliceY;
        }
        return new Rectangle(0, sliceIndex * defaultSlice.height, defaultSlice.width, currentSliceHeight);
    }

    private static long processSlice(SpatialBinner spatialBinner, ProgressMonitor progressMonitor,
                                     float[] superSamplingSteps, MultiLevelImage maskImage, MultiLevelImage[] varImages,
                                     Product product, Rectangle sliceRect) {
        final Raster maskTile = maskImage != null ? maskImage.getData(sliceRect) : null;
        final Raster[] varTiles = new Raster[varImages.length];
        for (int i = 0; i < varImages.length; i++) {
            varTiles[i] = varImages[i].getData(sliceRect);
        }

        final ObservationSlice observationSlice = new ObservationSlice(varTiles, maskTile, product,
                                                                       superSamplingSteps);
        long numObservations = spatialBinner.processObservationSlice(observationSlice);
        progressMonitor.worked(1);
        return numObservations;
    }


    private static Dimension getDefaultSliceDimension(Product product) {
        final int sliceWidth = product.getSceneRasterWidth();
        Dimension preferredTileSize = product.getPreferredTileSize();
        int sliceHeight;
        if (preferredTileSize != null) {
            sliceHeight = preferredTileSize.height;
        } else {
            sliceHeight = ImageManager.getPreferredTileSize(product).height;
        }
        return new Dimension(sliceWidth, sliceHeight);
    }

    private static void addVariablesToProduct(VariableContext variableContext, Product product,
                                              Map<Product, List<Band>> addedBands) {
        for (int i = 0; i < variableContext.getVariableCount(); i++) {
            String variableName = variableContext.getVariableName(i);
            String variableExpr = variableContext.getVariableExpression(i);
            if (variableExpr != null) {
                VirtualBand band = new VirtualBand(variableName,
                                                   ProductData.TYPE_FLOAT32,
                                                   product.getSceneRasterWidth(),
                                                   product.getSceneRasterHeight(),
                                                   variableExpr);
                band.setValidPixelExpression(variableContext.getValidMaskExpression());
                product.addBand(band);
                if (!addedBands.containsKey(product)) {
                    addedBands.put(product, new ArrayList<Band>());
                }
                addedBands.get(product).add(band);
            }
        }
    }

    private static void addMaskToProduct(String maskExpr, Product product,
                                         Map<Product, List<Band>> addedBands) {
        VirtualBand band = new VirtualBand("binning_mask",
                                           ProductData.TYPE_UINT8,
                                           product.getSceneRasterWidth(),
                                           product.getSceneRasterHeight(),
                                           StringUtils.isNotNullAndNotEmpty(maskExpr) ? maskExpr : "true");
        product.addBand(band);
        if (!addedBands.containsKey(product)) {
            addedBands.put(product, new ArrayList<Band>());
        }
        addedBands.get(product).add(band);
    }

    static float[] getSuperSamplingSteps(Integer superSampling) {
        if (superSampling == null || superSampling <= 1) {
            return new float[]{0.5f};
        } else {
            float[] samplingStep = new float[superSampling];
            for (int i = 0; i < samplingStep.length; i++) {
                samplingStep[i] = (i * 2.0F + 1.0F) / (2.0F * superSampling);
            }
            return samplingStep;
        }
    }

    private static RasterDataNode getRasterDataNode(Product product, String nodeName) {
        final RasterDataNode node = product.getRasterDataNode(nodeName);
        if (node == null) {
            throw new IllegalStateException(String.format("Can't find raster data node '%s' in product '%s'",
                                                          nodeName, product.getName()));
        }
        return node;
    }
}
