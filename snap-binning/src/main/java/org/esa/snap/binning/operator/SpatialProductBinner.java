/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning.operator;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import org.esa.snap.binning.BinningContext;
import org.esa.snap.binning.CompositingType;
import org.esa.snap.binning.MosaickingGrid;
import org.esa.snap.binning.ObservationSlice;
import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.binning.SpatialBinner;
import org.esa.snap.binning.VariableContext;
import org.esa.snap.binning.support.BinTracer;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StopWatch;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.runtime.Config;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class which performs a spatial binning of single input products.
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 */
public class SpatialProductBinner {

    private static final String PROPERTY_KEY_SLICE_HEIGHT = "snap.binning.sliceHeight";
    private static final String BINNING_MASK_NAME = "_binning_mask";

    /**
     * Processes a source product and generated spatial bins.
     *
     * @param product            The source product.
     * @param spatialBinner      The spatial binner to be used.
     * @param addedVariableBands A container for the bands that are added during processing.
     * @param progressMonitor    A progress monitor.
     * @return The total number of observations processed.
     * @throws IOException If an I/O error occurs.
     */
    public static long processProduct(Product product,
                                      SpatialBinner spatialBinner,
                                      Map<Product, List<Band>> addedVariableBands,
                                      ProgressMonitor progressMonitor) throws IOException {
        if (product.getSceneGeoCoding() == null) {
            throw new IllegalArgumentException("product.getGeoCoding() == null");
        }
        BinningContext binningContext = spatialBinner.getBinningContext();
        final VariableContext variableContext = binningContext.getVariableContext();
        addVariablesToProduct(variableContext, product, addedVariableBands);

        PlanetaryGrid planetaryGrid = binningContext.getPlanetaryGrid();
        CompositingType compositingType = binningContext.getCompositingType();
        Geometry sourceProductGeometry = null;
        final MultiLevelImage maskImage;
        if (CompositingType.MOSAICKING.equals(compositingType)) {
            addMaskToProduct(variableContext.getValidMaskExpression(), product, addedVariableBands);
            MosaickingGrid mosaickingGrid = (MosaickingGrid) planetaryGrid;
            sourceProductGeometry = computeProductGeometry(product);
            product = mosaickingGrid.reprojectToGrid(product);
            maskImage = product.getBand(BINNING_MASK_NAME).getGeophysicalImage();
        } else {
            maskImage = getMaskImage(product, variableContext.getValidMaskExpression());
        }

        final MultiLevelImage[] varImages = getVariableImages(product, variableContext);

        final Rectangle[] sliceRectangles;
        if (CompositingType.MOSAICKING.equals(compositingType)) {
            MosaickingGrid mosaickingGrid = (MosaickingGrid) planetaryGrid;
            Dimension tileSize = product.getPreferredTileSize();
            sliceRectangles = mosaickingGrid.getDataSliceRectangles(sourceProductGeometry, tileSize);
        } else {
            final Dimension defaultSliceDimension = computeDefaultSliceDimension(product);
            sliceRectangles = computeDataSliceRectangles(maskImage, varImages, defaultSliceDimension);
        }
        final float[] superSamplingSteps = getSuperSamplingSteps(binningContext.getSuperSampling());
        long numObsTotal = 0;
        String productName = product.getName();
        BinTracer binTracer = spatialBinner.getBinningContext().getBinManager().getBinTracer();
        if (binTracer != null) {
            binTracer.setProductName(productName);
        }
        progressMonitor.beginTask("Spatially binning of " + productName, sliceRectangles.length);
        final Logger logger = SystemUtils.LOG;
        for (int idx = 0; idx < sliceRectangles.length; idx++) {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            numObsTotal += processSlice(spatialBinner, progressMonitor, superSamplingSteps, maskImage, varImages,
                                        product, sliceRectangles[idx]);
            final String label = String.format("Processed slice %d of %d : ", idx + 1, sliceRectangles.length);
            stopWatch.stop();
            logger.info(label + stopWatch.getTimeDiffString());
        }
        spatialBinner.complete();
        return numObsTotal;
    }

    private static MultiLevelImage[] getVariableImages(Product product, VariableContext variableContext)
            throws OperatorException {
        final MultiLevelImage[] varImages = new MultiLevelImage[variableContext.getVariableCount()];
        for (int i = 0; i < variableContext.getVariableCount(); i++) {
            final String nodeName = variableContext.getVariableName(i);
            final RasterDataNode rasterDataNode = getRasterDataNode(product, nodeName);
            varImages[i] = ImageManager.createMaskedGeophysicalImage(rasterDataNode, Float.NaN);
        }
        if (varImages.length > 1) {
            MultiLevelImage varImage0 = varImages[0];
            for (MultiLevelImage image : varImages) {
                if (varImage0.getWidth() != image.getWidth() && varImage0.getHeight() != image.getHeight()) {
                    throw new OperatorException("Some source rasters have different size.\n" +
                                                        "Consider resampling source products to same size first.");
                }
            }
        }
        return varImages;
    }


    private static MultiLevelImage getMaskImage(Product product, String maskExpr) {
        MultiLevelImage maskImage = null;
        if (StringUtils.isNotNullAndNotEmpty(maskExpr)) {
            maskImage = product.getMaskImage(maskExpr, null);
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
        final ObservationSlice observationSlice = new ObservationSlice(varImages, maskImage, product, superSamplingSteps, sliceRect, spatialBinner.getBinningContext());
        long numObservations = spatialBinner.processObservationSlice(observationSlice);
        progressMonitor.worked(1);
        return numObservations;
    }


    private static Dimension computeDefaultSliceDimension(Product product) {
        final int sliceWidth = product.getSceneRasterWidth();
        Dimension preferredTileSize = product.getPreferredTileSize();
        int sliceHeight;
        if (preferredTileSize != null) {
            sliceHeight = preferredTileSize.height;
        } else {
            sliceHeight = ImageManager.getPreferredTileSize(product).height;
        }

        // TODO make this a parameter nf/mz 2013-11-05
        sliceHeight = Config.instance().preferences().getInt(PROPERTY_KEY_SLICE_HEIGHT, sliceHeight);
        Dimension dimension = new Dimension(sliceWidth, sliceHeight);
        String logMsg = String.format("Using slice dimension [width=%d, height=%d] in binning", dimension.width, dimension.height);
        SystemUtils.LOG.log(Level.INFO, logMsg);
        return dimension;
    }

    private static void addVariablesToProduct(VariableContext variableContext, Product product,
                                              Map<Product, List<Band>> addedVariableBands) {
        final String validMaskExpression = variableContext.getValidMaskExpression();
        for (int i = 0; i < variableContext.getVariableCount(); i++) {
            String variableName = variableContext.getVariableName(i);
            String variableExpr = variableContext.getVariableExpression(i);
            String variableValidExpr = variableContext.getVariableValidExpression(i);

            if (variableExpr != null) {
                VirtualBand band = new VirtualBand(variableName,
                                                   ProductData.TYPE_FLOAT32,
                                                   product.getSceneRasterWidth(),
                                                   product.getSceneRasterHeight(),
                                                   variableExpr);

                if (StringUtils.isNullOrEmpty(variableValidExpr)) {
                    try {
                        variableValidExpr = BandArithmetic.getValidMaskExpression(variableExpr, product, validMaskExpression);
                    } catch (ParseException e) {
                        throw new OperatorException("Failed to parse valid-mask expression " + validMaskExpression + ": " + e.getMessage(), e);
                    }
                }
                if (StringUtils.isNotNullAndNotEmpty(variableValidExpr) && !variableValidExpr.equals("true")) {
                    band.setValidPixelExpression(variableValidExpr);
                }
                product.addBand(band);
                if (!addedVariableBands.containsKey(product)) {
                    addedVariableBands.put(product, new ArrayList<Band>());
                }
                addedVariableBands.get(product).add(band);
            }
        }
    }

    private static void addMaskToProduct(String maskExpr, Product product,
                                         Map<Product, List<Band>> addedBands) {
        VirtualBand band = new VirtualBand(BINNING_MASK_NAME,
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

    // duplicate from SupsetOp find the right place for it
    private static Geometry computeProductGeometry(Product product) {
         final GeneralPath[] paths = ProductUtils.createGeoBoundaryPaths(product);
         final Polygon[] polygons = new Polygon[paths.length];
         final GeometryFactory factory = new GeometryFactory();
         for (int i = 0; i < paths.length; i++) {
             polygons[i] = convertAwtPathToJtsPolygon(paths[i], factory);
         }
         final DouglasPeuckerSimplifier peuckerSimplifier = new DouglasPeuckerSimplifier(
                 polygons.length == 1 ? polygons[0] : factory.createMultiPolygon(polygons));
         return peuckerSimplifier.getResultGeometry();
     }

     private static Polygon convertAwtPathToJtsPolygon(Path2D path, GeometryFactory factory) {
         final PathIterator pathIterator = path.getPathIterator(null);
         ArrayList<double[]> coordList = new ArrayList<>();
         int lastOpenIndex = 0;
         while (!pathIterator.isDone()) {
             final double[] coords = new double[6];
             final int segType = pathIterator.currentSegment(coords);
             if (segType == PathIterator.SEG_CLOSE) {
                 // we should only detect a single SEG_CLOSE
                 coordList.add(coordList.get(lastOpenIndex));
                 lastOpenIndex = coordList.size();
             } else {
                 coordList.add(coords);
             }
             pathIterator.next();
         }
         final Coordinate[] coordinates = new Coordinate[coordList.size()];
         for (int i1 = 0; i1 < coordinates.length; i1++) {
             final double[] coord = coordList.get(i1);
             coordinates[i1] = new Coordinate(coord[0], coord[1]);
         }

         return factory.createPolygon(factory.createLinearRing(coordinates), null);
     }
}
