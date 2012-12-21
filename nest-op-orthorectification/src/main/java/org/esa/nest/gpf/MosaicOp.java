/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.math.MathUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.MapProjectionHandler;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.util.Constants;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

/**
 * The Mosaic operator.
 */
@OperatorMetadata(alias = "Mosaic",
        category = "Geometry",
        description = "Mosaics two or more products based on their geo-codings.")
public class MosaicOp extends Operator {

    @SourceProducts
    private Product[] sourceProduct;
    @TargetProduct
    private Product targetProduct =     null;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames = null;

    @Parameter(valueSet = {ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
            ResamplingFactory.BILINEAR_INTERPOLATION_NAME, ResamplingFactory.CUBIC_CONVOLUTION_NAME},
            defaultValue = ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
            description = "The method to be used when resampling the slave grid onto the master grid.",
            label = "Resampling Type")
    private String resamplingMethod = ResamplingFactory.NEAREST_NEIGHBOUR_NAME;

    @Parameter(defaultValue = "true", description = "Average the overlapping areas", label = "Average Overlap")
    private boolean average = true;
    @Parameter(defaultValue = "true", description = "Normalize by Mean", label = "Normalize by Mean")
    private boolean normalizeByMean = true;
    @Parameter(defaultValue = "false", description = "Gradient Domain Mosaic", label = "Gradient Domain Mosaic")
    private boolean gradientDomainMosaic = false;

    @Parameter(defaultValue = "0", description = "Pixel Size (m)", label = "Pixel Size (m)")
    private double pixelSize = 0;
    @Parameter(defaultValue = "0", description = "Target width", label = "Scene Width (pixels)")
    private int sceneWidth = 0;
    @Parameter(defaultValue = "0", description = "Target height", label = "Scene Height (pixels)")
    private int sceneHeight = 0;
    @Parameter(defaultValue = "20", description = "Feather amount around source image", label = "Feature (pixels)")
    private int feather = 0;
    @Parameter(defaultValue = "5000", description = "Maximum number of iterations", label = "Maximum Iterations")
    private int maxIterations = 5000;
    @Parameter(defaultValue = "1e-4", description = "Convergence threshold for Relaxed Gauss-Seidel method",
            label = "Convergence Threshold")
    private double convergenceThreshold = 1e-4;

    private final OperatorUtils.SceneProperties scnProp = new OperatorUtils.SceneProperties();
    private final Map<Integer, Band> bandIndexSet = new HashMap<Integer, Band>(20);
    private final Map<Product, Rectangle> srcRectMap = new HashMap<Product, Rectangle>(10);
    private Product[] selectedProducts = null;

    private boolean outputGradientBand = false;


    @Override
    public void initialize() throws OperatorException {
        try {

            if (gradientDomainMosaic && resamplingMethod.contains(ResamplingFactory.NEAREST_NEIGHBOUR_NAME)) {
                throw new OperatorException("Nearest neighbour resampling method produces poor result with gradient" +
                        " domain mosaic, please select other method");
            }

            GeoCoding srcGeocoding = null;
            for (final Product prod : sourceProduct) {
                if (prod.getGeoCoding() == null) {
                    throw new OperatorException(
                            MessageFormat.format("Product ''{0}'' has no geo-coding.", prod.getName()));
                }
                if(srcGeocoding == null) {
                    srcGeocoding = prod.getGeoCoding();
                }
                // todo check that all source products have same geocoding

            }

            getSourceBands();

            OperatorUtils.computeImageGeoBoundary(selectedProducts, scnProp);

            if (sceneWidth == 0 || sceneHeight == 0) {

                final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct[0]);

                if(pixelSize == 0 && absRoot != null) {
                    final double rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
                    final double azimuthSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.azimuth_spacing);
                    pixelSize = Math.min(rangeSpacing, azimuthSpacing);
                }
                OperatorUtils.getSceneDimensions(pixelSize, scnProp);

                sceneWidth = scnProp.sceneWidth;
                sceneHeight = scnProp.sceneHeight;
                final double ratio = sceneWidth / (double)sceneHeight;
                long dim = (long) sceneWidth * (long) sceneHeight;
                while (sceneWidth > 0 && sceneHeight > 0 && dim > Integer.MAX_VALUE) {
                    sceneWidth -= 1000;
                    sceneHeight = (int)(sceneWidth / ratio);
                    dim = (long) sceneWidth * (long) sceneHeight;
                }
            }

            targetProduct = new Product("mosaic", "mosaic", sceneWidth, sceneHeight);
            targetProduct.setGeoCoding(createCRSGeoCoding(srcGeocoding));

            // add all bands
            for(Map.Entry<Integer, Band> integerBandEntry : bandIndexSet.entrySet()) {
                final Band srcBand = integerBandEntry.getValue();

                int targetBandDataType;
                if (gradientDomainMosaic) {
                    targetBandDataType = ProductData.TYPE_FLOAT32;
                } else {
                    targetBandDataType = srcBand.getDataType();
                }
                final Band targetBand = new Band(srcBand.getName(), targetBandDataType, sceneWidth, sceneHeight);
                targetBand.setUnit(srcBand.getUnit());
                targetBand.setDescription(srcBand.getDescription());
                targetBand.setNoDataValue(srcBand.getNoDataValue());
                targetBand.setNoDataValueUsed(true);
                targetProduct.addBand(targetBand);

                if (gradientDomainMosaic && outputGradientBand) { // add gradient band
                    final String targetBandName = srcBand.getName() + "_gradient";
                    final Band gradientBand = new Band(targetBandName, ProductData.TYPE_FLOAT32, sceneWidth, sceneHeight);
                    targetProduct.addBand(gradientBand);
                }
            }

            // transfer index coding
            if(sourceProduct[0].getIndexCodingGroup().getNodeCount() > 0 &&
                    sourceProduct[0].getIndexCodingGroup().get(0) != null) {
                try {
                    OperatorUtils.copyIndexCodings(sourceProduct[0], targetProduct);
                } catch(Exception e) {
                    if(!resamplingMethod.equals(Resampling.NEAREST_NEIGHBOUR)) {
                        throw new OperatorException("Use Nearest Neighbour with Classificaitons: "+e.getMessage());
                    }
                }
            }

            for (Product srcProduct : selectedProducts) {
                final Rectangle srcRect = getSrcRect(targetProduct.getGeoCoding(),
                        scnProp.srcCornerLatitudeMap.get(srcProduct),
                        scnProp.srcCornerLongitudeMap.get(srcProduct));
                srcRectMap.put(srcProduct, srcRect);
            }

            updateTargetProductMetadata();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private CrsGeoCoding createCRSGeoCoding(GeoCoding srcGeocoding) throws Exception {
        final CoordinateReferenceSystem srcCRS = srcGeocoding.getMapCRS();
        final CoordinateReferenceSystem targetCRS = MapProjectionHandler.getCRS(srcCRS.toWKT());
        final double pixelSpacingInDegree = pixelSize / Constants.semiMajorAxis * MathUtils.RTOD;

        double pixelSizeX = pixelSize;
        double pixelSizeY = pixelSize;
        if (targetCRS.getName().getCode().equals("WGS84(DD)")) {
            pixelSizeX = pixelSpacingInDegree;
            pixelSizeY = pixelSpacingInDegree;
        }

        final Rectangle2D bounds = new Rectangle2D.Double();
        bounds.setFrameFromDiagonal(scnProp.lonMin, scnProp.latMin, scnProp.lonMax, scnProp.latMax);
        final ReferencedEnvelope boundsEnvelope = new ReferencedEnvelope(bounds, DefaultGeographicCRS.WGS84);
        final ReferencedEnvelope targetEnvelope = boundsEnvelope.transform(targetCRS, true);
        return new CrsGeoCoding(targetCRS,
                sceneWidth,
                sceneHeight,
                targetEnvelope.getMinimum(0),
                targetEnvelope.getMaximum(1),
                pixelSizeX, pixelSizeY);
    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {
        MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if(absRoot == null) {
            absRoot = AbstractMetadata.addAbstractedMetadataHeader(targetProduct.getMetadataRoot());
        }
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing, pixelSize);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing, pixelSize);
    }

    private Band[] getSourceBands() throws OperatorException {
        final List<Band> bandList = new ArrayList<Band>(20);
        final Set<Product> selectedProductSet = new HashSet<Product>(sourceProduct.length);

        if (sourceBandNames.length == 0) {
            for (final Product srcProduct : sourceProduct) {
                for (final Band band : srcProduct.getBands()) {
                    if (band instanceof VirtualBand)
                        continue;
                    bandList.add(band);
                    bandIndexSet.put(srcProduct.getBandIndex(band.getName()), band);
                }
                selectedProductSet.add(srcProduct);
            }
        } else {

            for (final String name : sourceBandNames) {
                final String bandName = getBandName(name);
                final String productName = getProductName(name, sourceProduct[0].getName());

                final Product srcProduct = getProduct(productName);
                final Band band = srcProduct.getBand(bandName);
                final String bandUnit = band.getUnit();
                if (bandUnit != null) {
                    if (bandUnit.contains(Unit.IMAGINARY) || bandUnit.contains(Unit.REAL)) {
                        throw new OperatorException("Real and imaginary bands not handled");
                    }
                }
                bandList.add(band);
                bandIndexSet.put(srcProduct.getBandIndex(band.getName()), band);
                selectedProductSet.add(srcProduct);
            }
        }

        selectedProducts = selectedProductSet.toArray(new Product[selectedProductSet.size()]);
        return bandList.toArray(new Band[bandList.size()]);
    }

    private Product getProduct(final String productName) {
        for (Product prod : sourceProduct) {
            if (prod.getName().equals(productName)) {
                return prod;
            }
        }
        return null;
    }

    private static String getBandName(final String name) {
        if (name.contains("::"))
            return name.substring(0, name.indexOf("::"));
        return name;
    }

    private static String getProductName(final String name, final String defaultName) {
        if (name.contains("::"))
            return name.substring(name.indexOf("::") + 2, name.length());
        return defaultName;
    }

    private static Rectangle getSrcRect(final GeoCoding destGeoCoding,
                                        final double[] lats, final double[] lons) {

        double srcLatMin = 90.0;
        double srcLatMax = -90.0;
        double srcLonMin = 180.0;
        double srcLonMax = -180.0;

        for (double lat : lats) {
            if (lat < srcLatMin) {
                srcLatMin = lat;
            }
            if (lat > srcLatMax) {
                srcLatMax = lat;
            }
        }

        for (double lon : lons) {
            if (lon < srcLonMin) {
                srcLonMin = lon;
            }
            if (lon > srcLonMax) {
                srcLonMax = lon;
            }
        }

        final GeoPos geoPos = new GeoPos();
        final PixelPos[] pixelPos = new PixelPos[4];
        geoPos.setLocation((float) srcLatMin, (float) srcLonMin);
        pixelPos[0] = destGeoCoding.getPixelPos(geoPos, null);
        geoPos.setLocation((float) srcLatMin, (float) srcLonMax);
        pixelPos[1] = destGeoCoding.getPixelPos(geoPos, null);
        geoPos.setLocation((float) srcLatMax, (float) srcLonMax);
        pixelPos[2] = destGeoCoding.getPixelPos(geoPos, null);
        geoPos.setLocation((float) srcLatMax, (float) srcLonMin);
        pixelPos[3] = destGeoCoding.getPixelPos(geoPos, null);

        return getBoundingBox(pixelPos, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, 4);
    }

    private static Rectangle getBoundingBox(final PixelPos[] pixelPositions,
                                            final int minOffsetX, final int minOffsetY,
                                            final int maxWidth, final int maxHeight, final int margin) {
        int minX = Integer.MAX_VALUE;
        int maxX = -Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = -Integer.MAX_VALUE;

        for (final PixelPos pixelsPos : pixelPositions) {
            if (pixelsPos != null) {
                final int x = (int) Math.floor(pixelsPos.getX());
                final int y = (int) Math.floor(pixelsPos.getY());

                if (x < minX) {
                    minX = x;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (y < minY) {
                    minY = y;
                }
                if (y > maxY) {
                    maxY = y;
                }
            }
        }
        if (minX > maxX || minY > maxY) {
            return null;
        }

        minX = Math.max(minX - margin, minOffsetX);
        maxX = Math.min(maxX + margin, maxWidth - 1);
        minY = Math.max(minY - margin, minOffsetY);
        maxY = Math.min(maxY + margin, maxHeight - 1);

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        try {
            final List<Product> validProducts = new ArrayList<Product>(sourceProduct.length);

            for (final Product srcProduct : selectedProducts) {
                final Rectangle srcRect = srcRectMap.get(srcProduct);
                if (srcRect == null || !srcRect.intersects(targetRectangle)) {
                    continue;
                }
                validProducts.add(srcProduct);
            }

            if (validProducts.isEmpty()) {
                return;
            }

            final GeoPos geoPos = new GeoPos();
            final PixelPos pixelPos = new PixelPos();
            final int minX = targetRectangle.x;
            final int minY = targetRectangle.y;
            final int maxX = targetRectangle.x + targetRectangle.width - 1;
            final int maxY = targetRectangle.y + targetRectangle.height - 1;

            final TileGeoreferencing tileGeoRef = new TileGeoreferencing(targetProduct, minX, minY, maxX-minX+1, maxY-minY+1);

            final List<PixelPos[]> srcPixelCoords = new ArrayList<PixelPos[]>(validProducts.size());
            final int numPixelPos = targetRectangle.width * targetRectangle.height;
            for (Product validProduct : validProducts) {
                srcPixelCoords.add(new PixelPos[numPixelPos]);
            }

            int coordIndex = 0;
            int prodIndex;
            for (int y = minY; y <= maxY; ++y) {
                for (int x = minX; x <= maxX; ++x) {
                    tileGeoRef.getGeoPos(x, y, geoPos);

                    prodIndex = 0;
                    for (final Product srcProduct : validProducts) {
                        srcProduct.getGeoCoding().getPixelPos(geoPos, pixelPos);

                        if (pixelPos.x >= feather && pixelPos.y >= feather &&
                                pixelPos.x < srcProduct.getSceneRasterWidth()-feather &&
                                pixelPos.y < srcProduct.getSceneRasterHeight()-feather) {

                            srcPixelCoords.get(prodIndex)[coordIndex] = new PixelPos(pixelPos.x, pixelPos.y);
                        } else {
                            srcPixelCoords.get(prodIndex)[coordIndex] = null;
                        }
                        ++prodIndex;
                    }
                    ++coordIndex;
                }
            }

            final Resampling resampling = ResamplingFactory.createResampling(resamplingMethod);

            if (gradientDomainMosaic) {
                performGradientDomainMosaic(targetTiles, targetRectangle, srcPixelCoords, validProducts, resampling, pm);
                return;
            }

            final List<SourceData> validSourceData = new ArrayList<SourceData>(validProducts.size());
            for(final Map.Entry<Band, Tile> bandTileEntry : targetTiles.entrySet()) {
                final String trgBandName = bandTileEntry.getKey().getName();
                validSourceData.clear();

                prodIndex = 0;
                for (final Product srcProduct : validProducts) {
                    final Band srcBand = srcProduct.getBand(trgBandName);
                    if(srcBand == null) {
                        continue;
                    }

                    final PixelPos[] pixPos = srcPixelCoords.get(prodIndex);

                    final Rectangle sourceRectangle = getBoundingBox(
                            pixPos, feather, feather,
                            srcProduct.getSceneRasterWidth()-feather,
                            srcProduct.getSceneRasterHeight()-feather, 4);

                    if (sourceRectangle != null) {
                        double min = 0, max = 0, mean = 0, std = 0;
                        if(normalizeByMean) {                  // get stat values
                            try {
                                final Stx stats = srcBand.getStx(true, ProgressMonitor.NULL);
                                mean = stats.getMean();
                                min = stats.getMin();
                                max = stats.getMax();
                                std = stats.getStandardDeviation();
                            } catch (Throwable e) {
                                //OperatorUtils.catchOperatorException(getId(), e);
                                normalizeByMean = false; // temporary disable
                            }
                        }

                        final Tile srcTile = getSourceTile(srcBand, sourceRectangle);
                        if(srcTile != null) {
                            validSourceData.add(new SourceData(srcTile, pixPos, resampling, min, max, mean, std));
                        }
                    }
                    ++prodIndex;
                }

                if(!validSourceData.isEmpty()) {
                    collocateSourceBand(validSourceData, resampling, bandTileEntry.getValue());
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private void collocateSourceBand(final List<SourceData> validSourceData, final Resampling resampling,
                                     final Tile targetTile) throws OperatorException {
        try {
            final Rectangle targetRectangle = targetTile.getRectangle();
            final ProductData trgBuffer = targetTile.getDataBuffer();

            float sample;
            final int maxY = targetRectangle.y + targetRectangle.height;
            final int maxX = targetRectangle.x + targetRectangle.width;
            final TileIndex trgIndex = new TileIndex(targetTile);
            final float[] sampleList = new float[validSourceData.size()];
            final int[] sampleDistanceList = new int[validSourceData.size()];

            for (int y = targetRectangle.y, index = 0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                for (int x = targetRectangle.x; x < maxX; ++x, ++index) {

                    double targetVal = 0;
                    int numSamples = 0;
                    for(final SourceData srcDat : validSourceData) {
                        final PixelPos sourcePixelPos = srcDat.srcPixPos[index];
                        if(sourcePixelPos == null) {
                            continue;
                        }

                        resampling.computeIndex(sourcePixelPos.x, sourcePixelPos.y,
                                srcDat.srcRasterWidth-feather, srcDat.srcRasterHeight-feather, srcDat.resamplingIndex);

                        sample = resampling.resample(srcDat.resamplingRaster, srcDat.resamplingIndex);

                        if (!Float.isNaN(sample) && sample != srcDat.nodataValue && !MathUtils.equalValues(sample, 0.0F, 1e-4F)) {

                            if (normalizeByMean) {
                                sample -= srcDat.srcMean;
                                sample /= srcDat.srcStd;
                            }
                            targetVal = sample;

                            if (average) {
                                sampleList[numSamples] = sample;
                                sampleDistanceList[numSamples] = (int)(Math.min(sourcePixelPos.x + 1,
                                        srcDat.srcRasterWidth - sourcePixelPos.x)*
                                        Math.min(sourcePixelPos.y + 1,
                                                srcDat.srcRasterHeight - sourcePixelPos.y));
                                numSamples++;
                            }
                        }
                    }

                    if(targetVal != 0) {
                        if (average && numSamples > 1) {
                            double sum = 0;
                            int totalWeight = 0;
                            for(int i = 0; i < numSamples; i++) {
                                sum += sampleList[i]*sampleDistanceList[i];
                                totalWeight += sampleDistanceList[i];
                            }
                            targetVal = sum / totalWeight;
                        }

                        trgBuffer.setElemDoubleAt(trgIndex.getIndex(x), targetVal);
                    }
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void performGradientDomainMosaic(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle,
                                             final List<PixelPos[]> srcPixelCoords, final List<Product> validProducts,
                                             final Resampling resampling, ProgressMonitor pm)
            throws OperatorException {

        try {

            final int minX = targetRectangle.x;
            final int minY = targetRectangle.y;
            final int maxX = targetRectangle.x + targetRectangle.width - 1;
            final int maxY = targetRectangle.y + targetRectangle.height - 1;

            double[][] mosaicedTile = new double[targetRectangle.height][targetRectangle.width];
            double[][] gradientTile = new double[targetRectangle.height][targetRectangle.width];
            byte[][] mask = new byte[targetRectangle.height][targetRectangle.width];
            // -1: no data, 0: used by existing product, 1: used by new product, 2: need mosaic

            final List<SourceData> validSourceData = new ArrayList<SourceData>(validProducts.size());

            // loop through all target bands
            for(final Map.Entry<Band, Tile> bandTileEntry : targetTiles.entrySet()) {
                final String trgBandName = bandTileEntry.getKey().getName();
                if (trgBandName.contains("_gradient")) {
                    continue;
                }
                final Tile trgTile = bandTileEntry.getValue();
                final ProductData trgBuffer = trgTile.getDataBuffer();

                // for each target band, get source data for all related source bands
                getValidSourceData(validProducts, trgBandName, srcPixelCoords, resampling, validSourceData, pm);

                // for each target band, find all related source bands and use them in mosaic
                // for now we assume that source products have been sorted according to time with the oldest first
                for (int i = 0; i < validSourceData.size(); i++) {
                    if (i == 0) {
                        readFirstProduct(minX, maxX, minY, maxY, validSourceData.get(i), resampling,
                                mosaicedTile, mask);
                    } else {
                        readNextProduct(minX, maxX, minY, maxY, validSourceData.get(i), resampling,
                                mosaicedTile, mask, gradientTile);

                        performMosaic(mask, gradientTile, mosaicedTile);

                        cleanUpMask(mask);
                    }
                }

                // save mosaiced image
                final TileIndex trgIndex = new TileIndex(trgTile);
                for (int y = minY; y <= maxY; y++) {
                    trgIndex.calculateStride(y);
                    for (int x = minX; x <= maxX; x++) {
                        trgBuffer.setElemDoubleAt(trgIndex.getIndex(x), mosaicedTile[y-minY][x-minX]);
                    }
                }

                // save gradient
                if (outputGradientBand) {
                    final Band gradientBand = targetProduct.getBand(trgBandName + "_gradient");
                    final ProductData gradientBuffer = targetTiles.get(gradientBand).getDataBuffer();
                    for (int y = minY; y <= maxY; y++) {
                        trgIndex.calculateStride(y);
                        for (int x = minX; x <= maxX; x++) {
                            gradientBuffer.setElemDoubleAt(trgIndex.getIndex(x), gradientTile[y-minY][x-minX]);
                        }
                    }
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void getValidSourceData(final List<Product> validProducts, final String trgBandName,
                                    final List<PixelPos[]> srcPixelCoords, final Resampling resampling,
                                    List<SourceData> validSourceData, ProgressMonitor pm) {

        try {
            validSourceData.clear();
            int prodIndex = 0;
            for (final Product srcProduct : validProducts) {
                final Band srcBand = srcProduct.getBand(trgBandName);
                if(srcBand == null) {
                    continue;
                }

                final PixelPos[] pixPos = srcPixelCoords.get(prodIndex);
                final Rectangle sourceRectangle = getBoundingBox(
                        pixPos, 0, 0, srcProduct.getSceneRasterWidth(), srcProduct.getSceneRasterHeight(), feather);

                if (sourceRectangle != null) {
                    double mean = 0, min = 0, max = 0, std = 0;
                    if (normalizeByMean) {
                        try {
                            final Stx stats = srcBand.getStx(true, pm);
                            mean = stats.getMean();
                            min = stats.getMin();
                            max = stats.getMax();
                            std = stats.getStandardDeviation();
                        } catch (Throwable e) {
                            //OperatorUtils.catchOperatorException(getId(), e);
                            normalizeByMean = false;
                        }
                    }

                    final Tile srcTile = getSourceTile(srcBand, sourceRectangle);
                    if(srcTile != null) {
                        validSourceData.add(new SourceData(srcTile, pixPos, resampling, min, max, mean, std));
                    }
                }
                ++prodIndex;
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void readFirstProduct(final int minX, final int maxX, final int minY, final int maxY,
                                  final SourceData srcDat, final Resampling resampling,
                                  double[][] mosaicedTile, byte[][] mask)
            throws OperatorException {

        try {
            float sample;
            int yy, xx;
            for (int y = minY, index = 0; y <= maxY; ++y) {
                yy = y - minY;
                for (int x = minX; x <= maxX; ++x, ++index) {
                    xx = x - minX;

                    final PixelPos sourcePixelPos = srcDat.srcPixPos[index];
                    if(sourcePixelPos == null) {
                        mosaicedTile[yy][xx] = srcDat.nodataValue;
                        mask[yy][xx] = -1;
                        continue;
                    }

                    resampling.computeIndex(sourcePixelPos.x, sourcePixelPos.y,
                            srcDat.srcRasterWidth, srcDat.srcRasterHeight, srcDat.resamplingIndex);

                    sample = resampling.resample(srcDat.resamplingRaster, srcDat.resamplingIndex);

                    if (isValidSample(sample, srcDat.nodataValue)) {
                        if (normalizeByMean) {
                            sample -= srcDat.srcMean;
                            sample /= srcDat.srcStd;
                        }
                        mosaicedTile[yy][xx] = sample;
                        mask[yy][xx] = 0;
                    } else {
                        mosaicedTile[yy][xx] = srcDat.nodataValue;
                        mask[yy][xx] = -1;
                    }
                }
            }

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }


    private void readNextProduct(final int minX, final int maxX, final int minY, final int maxY,
                                 final SourceData srcDat, final Resampling resampling,
                                 double[][] mosaicedTile, byte[][] mask, double[][] gradientTile)
            throws OperatorException {

        try {
            final int targetTileWidth = mosaicedTile[0].length;
            final int targetTileHeight = mosaicedTile.length;
            double[] adjacentPixels = new double[4];

            float sample;
            int yy, xx;
            for (int y = minY, index = 0; y <= maxY; ++y) {
                yy = y - minY;
                for (int x = minX; x <= maxX; ++x, ++index) {
                    xx = x - minX;

                    final PixelPos sourcePixelPos = srcDat.srcPixPos[index];
                    if(sourcePixelPos == null) {
                        continue;
                    }

                    resampling.computeIndex(sourcePixelPos.x, sourcePixelPos.y,
                            srcDat.srcRasterWidth, srcDat.srcRasterHeight, srcDat.resamplingIndex);

                    sample = resampling.resample(srcDat.resamplingRaster, srcDat.resamplingIndex);

                    if (isValidSample(sample, srcDat.nodataValue)) {
                        if (normalizeByMean) {
                            sample -= srcDat.srcMean;
                            sample /= srcDat.srcStd;
                        }

                        if (mask[yy][xx] == -1) {
                            mosaicedTile[yy][xx] = sample;
                            mask[yy][xx] = 1;
                        } else if (mask[yy][xx] == 0 && isInnerPoint(index, targetTileWidth, targetTileHeight, srcDat,
                                resampling, adjacentPixels)) {

                            if (isInnerPoint(xx, yy, mask)) {
                                mask[yy][xx] = 2;
                                mosaicedTile[yy][xx] = sample;
                                //gradientTile[yy][xx] = computeGradient(xx, yy, mosaicedTile, sample, adjacentPixels);
                                gradientTile[yy][xx] = adjacentPixels[0] + adjacentPixels[1] + adjacentPixels[2] + adjacentPixels[3] - 4*sample;
                            } else {
                                mosaicedTile[yy][xx] = sample;
                            }
                        }
                    }
                }
            }

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private boolean isInnerPoint(final int index, final int targetTileWidth, final int targetTileHeight,
                                 final SourceData srcDat, final Resampling resampling, double[] adjacentPixels) {

        try {
            final int indexUp = index - targetTileWidth;
            final int indexDown = index + targetTileWidth;
            final int indexLeft = index - 1;
            final int indexRight = index + 1;

            if (indexUp >= 0 && indexDown < targetTileWidth*targetTileHeight &&
                    index % targetTileWidth != 0 && (index + 1) % targetTileWidth != 0 &&
                    srcDat.srcPixPos[indexUp] != null && srcDat.srcPixPos[indexDown] != null &&
                    srcDat.srcPixPos[indexLeft] != null && srcDat.srcPixPos[indexRight] != null) {

                resampling.computeIndex(srcDat.srcPixPos[indexUp].x, srcDat.srcPixPos[indexUp].y,
                        srcDat.srcRasterWidth, srcDat.srcRasterHeight, srcDat.resamplingIndex);

                final double s1 = resampling.resample(srcDat.resamplingRaster, srcDat.resamplingIndex);

                resampling.computeIndex(srcDat.srcPixPos[indexDown].x, srcDat.srcPixPos[indexDown].y,
                        srcDat.srcRasterWidth, srcDat.srcRasterHeight, srcDat.resamplingIndex);

                final double s2 = resampling.resample(srcDat.resamplingRaster, srcDat.resamplingIndex);

                resampling.computeIndex(srcDat.srcPixPos[indexLeft].x, srcDat.srcPixPos[indexLeft].y,
                        srcDat.srcRasterWidth, srcDat.srcRasterHeight, srcDat.resamplingIndex);

                final double s3 = resampling.resample(srcDat.resamplingRaster, srcDat.resamplingIndex);

                resampling.computeIndex(srcDat.srcPixPos[indexRight].x, srcDat.srcPixPos[indexRight].y,
                        srcDat.srcRasterWidth, srcDat.srcRasterHeight, srcDat.resamplingIndex);

                final double s4 = resampling.resample(srcDat.resamplingRaster, srcDat.resamplingIndex);

                if (isValidSample((float)s1, srcDat.nodataValue) && isValidSample((float)s2, srcDat.nodataValue) &&
                        isValidSample((float)s3, srcDat.nodataValue) && isValidSample((float)s4, srcDat.nodataValue)) {

                    if (normalizeByMean) {
                        adjacentPixels[0] = (s1 - srcDat.srcMean) / srcDat.srcStd;
                        adjacentPixels[1] = (s2 - srcDat.srcMean) / srcDat.srcStd;
                        adjacentPixels[2] = (s3 - srcDat.srcMean) / srcDat.srcStd;
                        adjacentPixels[3] = (s4 - srcDat.srcMean) / srcDat.srcStd;
                    } else {
                        adjacentPixels[0] = s1;
                        adjacentPixels[1] = s2;
                        adjacentPixels[2] = s3;
                        adjacentPixels[3] = s4;
                    }
                    return true;
                } else {
                    return false;
                }
            }

            return false;
        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
        return false;
    }

    private static boolean isInnerPoint(final int xx, final int yy, final byte[][] mask) {

        if (xx == 0 || yy == 0 || xx == mask[0].length - 1 || yy == mask.length - 1) {
            return false;
        } else {
            return (mask[yy-1][xx] == 0 || mask[yy-1][xx] == 2) &&
                    (mask[yy+1][xx] == 0 || mask[yy+1][xx] == 2) &&
                    (mask[yy][xx-1] == 0 || mask[yy][xx-1] == 2) &&
                    (mask[yy][xx+1] == 0 || mask[yy][xx+1] == 2);
        }
    }

    private static boolean isValidSample(final float sample, final double noDataValue) {
        return (!Float.isNaN(sample) && sample != noDataValue && !MathUtils.equalValues(sample, 0.0F, 1e-4F));
    }

    private double computeGradient(final int xx, final int yy, final double[][] mosaicedTile,
                                   final double s0, final double[] adjacentPixels) {

        double g2 = adjacentPixels[0] + adjacentPixels[1] + adjacentPixels[2] + adjacentPixels[3] - 4*s0;

        /*
        double g1 = mosaicedTile[yy-1][xx] + mosaicedTile[yy+1][xx] + mosaicedTile[yy][xx-1] +
                    mosaicedTile[yy][xx+1] - 4*mosaicedTile[yy][xx];

        if (Math.abs(g1) > Math.abs(g2)) {
            return g1;
        } else {
            return g2;
        }
        */
        return g2;
    }

    private void performMosaic(final byte[][] mask, final double[][] gradientTile, double[][] mosaicedTile) {

        final double w = 1.5;
        final int rows = mask.length;
        final int cols = mask[0].length;

        double sigma, update, error = 0.0;
        int it;
        for (it = 0; it < maxIterations; it++) {
            error = 0.0;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (mask[r][c] == 2) {

                        sigma = gradientTile[r][c] - mosaicedTile[r-1][c] - mosaicedTile[r+1][c] -
                                mosaicedTile[r][c-1] - mosaicedTile[r][c+1];

                        update = (1-w)*mosaicedTile[r][c] - w*sigma/4.0;
                        error = Math.max(error, Math.abs(mosaicedTile[r][c] - update));
                        mosaicedTile[r][c] = update;
                    }
                }
            }

            if (error < convergenceThreshold) {
                break;
            }
        }

        //if(error != 0)
        //    System.out.println("it = " + it + ", error = " + error);
    }

    private static void cleanUpMask(byte[][] mask) {

        final int rows = mask.length;
        final int cols = mask[0].length;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (mask[r][c] > 0) {
                    mask[r][c] = 0;
                }
            }
        }
    }


    private static class ResamplingRaster implements Resampling.Raster {

        private final Tile tile;
        private final boolean usesNoData;
        private final boolean scalingApplied;
        private final double noDataValue;
        private final double geophysicalNoDataValue;
        private final ProductData dataBuffer;
        private final int minX, minY, maxX, maxY;

        public ResamplingRaster(final Tile tile) {
            this.tile = tile;
            this.minX = tile.getMinX();
            this.minY = tile.getMinY();
            this.maxX = tile.getMaxX();
            this.maxY = tile.getMaxY();
            this.dataBuffer = tile.getDataBuffer();
            final RasterDataNode rasterDataNode = tile.getRasterDataNode();
            this.usesNoData = rasterDataNode.isNoDataValueUsed();
            this.noDataValue = rasterDataNode.getNoDataValue();
            this.geophysicalNoDataValue = rasterDataNode.getGeophysicalNoDataValue();
            this.scalingApplied = rasterDataNode.isScalingApplied();
        }

        public final int getWidth() {
            return tile.getWidth();
        }

        public final int getHeight() {
            return tile.getHeight();
        }

        public final float getSample(final double x, final double y) throws Exception {
            if(x < minX || y < minY || x > maxX || y > maxY)
                return Float.NaN;

            final double sample = dataBuffer.getElemDoubleAt(tile.getDataBufferIndex((int)x, (int)y));

            if (usesNoData) {
                if (scalingApplied && geophysicalNoDataValue == sample)
                    return Float.NaN;
                else if (noDataValue == sample)
                    return Float.NaN;
            }
            return (float) sample;
        }

        public void getSamples(final int[] x, final int[] y, final float[][] samples) throws Exception {
            for (int i = 0; i < y.length; i++) {
                for (int j = 0; j < x.length; j++) {
                    samples[i][j] = getSample(x[j], y[i]);
                }
            }
        }
    }

    private static class SourceData {
        final Tile srcTile;
        final ResamplingRaster resamplingRaster;
        final Resampling.Index resamplingIndex;
        final double nodataValue;
        final PixelPos[] srcPixPos;
        final int srcRasterHeight;
        final int srcRasterWidth;
        final double srcMean;
        final double srcMax;
        final double srcMin;
        final double srcStd;

        public SourceData(final Tile tile,
                          final PixelPos[] pixPos, final Resampling resampling,
                          final double min, final double max, final double mean, final double std) {
            srcTile = tile;
            resamplingRaster = new ResamplingRaster(srcTile);
            resamplingIndex = resampling.createIndex();
            nodataValue = tile.getRasterDataNode().getNoDataValue();
            srcPixPos = pixPos;

            final Product srcProduct = tile.getRasterDataNode().getProduct();
            srcRasterHeight = srcProduct.getSceneRasterHeight();
            srcRasterWidth = srcProduct.getSceneRasterWidth();
            srcMin = min;
            srcMax = max;
            srcMean = mean;
            srcStd = std;
        }
    }

    /**
     * Operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MosaicOp.class);
            super.setOperatorUI(MosaicOpUI.class);
        }
    }
}