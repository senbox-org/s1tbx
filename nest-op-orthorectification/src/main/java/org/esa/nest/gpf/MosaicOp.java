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
    private Product targetProduct = null;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames = null;

    @Parameter(valueSet = {ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
            ResamplingFactory.BILINEAR_INTERPOLATION_NAME, ResamplingFactory.CUBIC_CONVOLUTION_NAME},
            defaultValue = ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
            description = "The method to be used when resampling the slave grid onto the master grid.",
            label = "Resampling Type")
    private String resamplingMethod = ResamplingFactory.NEAREST_NEIGHBOUR_NAME;

    //@Parameter(description = "The coordinate reference system in well known text format", defaultValue="WGS84(DD)")
    //private String mapProjection = "WGS84(DD)";

    @Parameter(defaultValue = "true", description = "Average the overlapping areas", label = "Average Overlap")
    private boolean average = true;
    @Parameter(defaultValue = "true", description = "Normalize by Mean", label = "Normalize by Mean")
    private boolean normalizeByMean = true;

    @Parameter(defaultValue = "0", description = "Pixel Size (m)", label = "Pixel Size (m)")
    private double pixelSize = 0;
    @Parameter(defaultValue = "0", description = "Target width", label = "Scene Width (pixels)")
    private int sceneWidth = 0;
    @Parameter(defaultValue = "0", description = "Target height", label = "Scene Height (pixels)")
    private int sceneHeight = 0;
    @Parameter(defaultValue = "20", description = "Feather amount around source image", label = "Feature (pixels)")
    private int feather = 0;

    private final OperatorUtils.SceneProperties scnProp = new OperatorUtils.SceneProperties();
    private final Map<Product, Band> srcBandMap = new HashMap<Product, Band>(10);
    private final Map<Product, Rectangle> srcRectMap = new HashMap<Product, Rectangle>(10);
    private Product[] selectedProducts = null;

    @Override
    public void initialize() throws OperatorException {
        try {
            GeoCoding srcGeocoding = null;
            for (final Product prod : sourceProduct) {
                if (prod.getGeoCoding() == null) {
                    throw new OperatorException(
                            MessageFormat.format("Product ''{0}'' has no geo-coding.", prod.getName()));
                }
                if(srcGeocoding == null) {
                    srcGeocoding = prod.getGeoCoding();
                }
                // check that all source products have same geocoding

            }

            final Band[] srcBands = getSourceBands();
            final List<Product> selectedProductList = new ArrayList<Product>();
            for (Band srcBand : srcBands) {
                srcBandMap.put(srcBand.getProduct(), srcBand);
                selectedProductList.add(srcBand.getProduct());
            }
            selectedProducts = selectedProductList.toArray(new Product[selectedProductList.size()]);

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
            
            final Band targetBand = new Band("mosaic", ProductData.TYPE_FLOAT32, sceneWidth, sceneHeight);

            targetBand.setUnit(sourceProduct[0].getBandAt(0).getUnit());
            targetBand.setNoDataValue(0);
            targetBand.setNoDataValueUsed(true);
            targetProduct.addBand(targetBand);

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
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing, pixelSize);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing, pixelSize);
    }

    private Band[] getSourceBands() throws OperatorException {
        final List<Band> bandList = new ArrayList<Band>(5);

        if (sourceBandNames.length == 0) {
            for (final Product srcProduct : sourceProduct) {
                final String qlBandName = srcProduct.getQuicklookBandName();
                if(qlBandName != null) {
                    bandList.add(srcProduct.getBand(qlBandName));
                } else {
                    for (final Band band : srcProduct.getBands()) {
                        if (band.getUnit() != null && band.getUnit().equals(Unit.PHASE))
                            continue;
                        if (band instanceof VirtualBand)
                            continue;
                        bandList.add(band);
                        break;
                    }
                }
            }
        } else {

            for (final String name : sourceBandNames) {
                final String bandName = getBandName(name);
                final String productName = getProductName(name);

                final Product prod = getProduct(productName);
                final Band band = prod.getBand(bandName);
                final String bandUnit = band.getUnit();
                if (bandUnit != null) {
                    if (bandUnit.contains(Unit.IMAGINARY) || bandUnit.contains(Unit.REAL)) {
                        throw new OperatorException("Real and imaginary bands not handled");
                    } else {
                        bandList.add(band);
                    }
                } else {
                    bandList.add(band);
                }
            }
        }
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

    private String getProductName(final String name) {
        if (name.contains("::"))
            return name.substring(name.indexOf("::") + 2, name.length());
        return sourceProduct[0].getName();
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

        return getBoundingBox(pixelPos, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    private static Rectangle getBoundingBox(final PixelPos[] pixelPositions,
                                            final int minOffsetX, final int minOffsetY,
                                            final int maxWidth, final int maxHeight) {
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

        minX = Math.max(minX - 4, minOffsetX);
        maxX = Math.min(maxX + 4, maxWidth - 1);
        minY = Math.max(minY - 4, minOffsetY);
        maxY = Math.min(maxY + 4, maxHeight - 1);

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException if an error occurs during computation of the target rasters.
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
            if (validProducts.isEmpty())
                return;

            final List<PixelPos[]> srcPixelCoords = new ArrayList<PixelPos[]>(validProducts.size());
            final int numPixelPos = targetRectangle.width * targetRectangle.height;
            for (Product validProduct : validProducts) {
                srcPixelCoords.add(new PixelPos[numPixelPos]);
            }

            final GeoCoding targetGeoCoding = targetProduct.getGeoCoding();
            final GeoPos geoPos = new GeoPos();
            final PixelPos pixelPos = new PixelPos();
            final int maxX = targetRectangle.x + targetRectangle.width - 1;
            final int maxY = targetRectangle.y + targetRectangle.height - 1;

            int coordIndex = 0;
            int index;
            for (int y = targetRectangle.y; y <= maxY; ++y) {
                for (int x = targetRectangle.x; x <= maxX; ++x) {
                    pixelPos.x = x + 0.5f;
                    pixelPos.y = y + 0.5f;
                    targetGeoCoding.getGeoPos(pixelPos, geoPos);

                    index = 0;
                    for (final Product srcProduct : validProducts) {
                        srcProduct.getGeoCoding().getPixelPos(geoPos, pixelPos);
                        if (pixelPos.x >= feather && pixelPos.y >= feather &&
                                pixelPos.x < srcProduct.getSceneRasterWidth()-feather &&
                                pixelPos.y < srcProduct.getSceneRasterHeight()-feather) {

                            srcPixelCoords.get(index)[coordIndex] = new PixelPos(pixelPos.x, pixelPos.y);
                        } else {
                            srcPixelCoords.get(index)[coordIndex] = null;
                        }
                        ++index;
                    }
                    ++coordIndex;
                }
            }

            final Resampling resampling = ResamplingFactory.createResampling(resamplingMethod);
            final List<SourceData> validSourceData = new ArrayList<SourceData>(validProducts.size());

            final Set<Band> bandSet = targetTiles.keySet();
            for(final Band trgBand : bandSet) {

                index = 0;
                for (final Product srcProduct : validProducts) {
                    final PixelPos[] pixPos = srcPixelCoords.get(index);
                    final Rectangle sourceRectangle = getBoundingBox(
                            pixPos, feather, feather,
                            srcProduct.getSceneRasterWidth()-feather,
                            srcProduct.getSceneRasterHeight()-feather);

                    if (sourceRectangle != null) {
                        final Band srcBand = srcBandMap.get(srcProduct);
                        double min = 0, max = 0, mean = 0, std = 0;
                        if(normalizeByMean) {                  // get stat values
                            try {
                                final Stx stats = srcBand.getStx();
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
                    ++index;
                }

                if(!validSourceData.isEmpty()) {
                    collocateSourceBand(validSourceData, resampling, targetTiles.get(trgBand));
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

        public void getSamples(int[] x, int[] y, float[][] samples) throws Exception {
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
