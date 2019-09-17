/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.dataio;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.Scene;
import org.esa.snap.core.datamodel.SceneFactory;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.ProductUtils;

import javax.media.jai.Histogram;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A special-purpose product reader used to build subsets of data products.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class ProductSubsetBuilder extends AbstractProductBuilder {

    public ProductSubsetBuilder() {
        this(false);
    }

    public ProductSubsetBuilder(boolean sourceProductOwner) {
        super(sourceProductOwner);
    }

    public static Product createProductSubset(Product sourceProduct, ProductSubsetDef subsetDef, String name,
                                              String desc) throws IOException {
        return createProductSubset(sourceProduct, false, subsetDef, name, desc);
    }

    public static Product createProductSubset(Product sourceProduct, boolean sourceProductOwner,
                                              ProductSubsetDef subsetDef, String name, String desc) throws IOException {
        ProductSubsetBuilder productSubsetBuilder = new ProductSubsetBuilder(sourceProductOwner);
        return productSubsetBuilder.readProductNodes(sourceProduct, subsetDef, name, desc);
    }

    private static void updateMetadata(
            final Product sourceProduct, final Product targetProduct, ProductSubsetDef subsetDef) throws IOException {

        try {
            final MetadataElement srcRoot = sourceProduct.getMetadataRoot();
            final MetadataElement srcAbsRoot = srcRoot.getElement("Abstracted_Metadata");
            if(srcAbsRoot == null)
                return;

            final MetadataElement trgRoot = targetProduct.getMetadataRoot();
            MetadataElement trgAbsRoot = trgRoot.getElement("Abstracted_Metadata");
            if(trgAbsRoot == null) {
                trgAbsRoot = new MetadataElement("Abstracted_Metadata");
                trgRoot.addElement(trgAbsRoot);
                ProductUtils.copyMetadata(srcAbsRoot, trgAbsRoot);
            }

            final Rectangle region = subsetDef.getRegion();
            final MetadataAttribute height = trgAbsRoot.getAttribute("num_output_lines");
            if(height != null)
                height.getData().setElemUInt(targetProduct.getSceneRasterHeight());

            final MetadataAttribute width = trgAbsRoot.getAttribute("num_samples_per_line");
            if(width != null)
                width.getData().setElemUInt(targetProduct.getSceneRasterWidth());

            final MetadataAttribute offsetX = trgAbsRoot.getAttribute("subset_offset_x");
            if(offsetX != null && region != null)
                offsetX.getData().setElemUInt(region.x);

            final MetadataAttribute offsetY = trgAbsRoot.getAttribute("subset_offset_y");
            if(offsetY != null && region != null)
                offsetY.getData().setElemUInt(region.y);

            boolean isSARProduct = trgAbsRoot.getAttributeDouble("radar_frequency", 99999) != 99999;
            if(!isSARProduct)
                return;

            // update subset metadata for SAR products

            boolean nearRangeOnLeft = isNearRangeOnLeft(targetProduct);

            final int sourceImageHeight = sourceProduct.getSceneRasterHeight();
            final double srcFirstLineTime = ProductData.UTC.parse(srcAbsRoot.getAttributeString("first_line_time")).getMJD(); // in days
            final double srcLastLineTime = ProductData.UTC.parse(srcAbsRoot.getAttributeString("last_line_time")).getMJD(); // in days
            final double lineTimeInterval = (srcLastLineTime - srcFirstLineTime) / (sourceImageHeight - 1); // in days
            if(region != null) {
                final int regionY = region.y;
                final double regionHeight = region.getHeight();
                final double newFirstLineTime = srcFirstLineTime + lineTimeInterval * regionY;
                final double newLastLineTime = newFirstLineTime + lineTimeInterval * (regionHeight - 1);
                final MetadataAttribute firstLineTime = trgAbsRoot.getAttribute("first_line_time");
                if (firstLineTime != null) {
                    firstLineTime.getData().setElems((new ProductData.UTC(newFirstLineTime)).getArray());
                }
                final MetadataAttribute lastLineTime = trgAbsRoot.getAttribute("last_line_time");
                if (lastLineTime != null) {
                    lastLineTime.getData().setElems((new ProductData.UTC(newLastLineTime)).getArray());
                }
            }

            final MetadataAttribute totalSize = trgAbsRoot.getAttribute("total_size");
            if(totalSize != null)
                totalSize.getData().setElemUInt(targetProduct.getRawStorageSize());

            if (nearRangeOnLeft) {
                setLatLongMetadata(targetProduct, trgAbsRoot, "first_near_lat", "first_near_long", 0.5f, 0.5f);
                setLatLongMetadata(targetProduct, trgAbsRoot, "first_far_lat", "first_far_long",
                        targetProduct.getSceneRasterWidth() - 1 + 0.5f, 0.5f);

                setLatLongMetadata(targetProduct, trgAbsRoot, "last_near_lat", "last_near_long",
                        0.5f, targetProduct.getSceneRasterHeight() - 1 + 0.5f);
                setLatLongMetadata(targetProduct, trgAbsRoot, "last_far_lat", "last_far_long",
                        targetProduct.getSceneRasterWidth() - 1 + 0.5f, targetProduct.getSceneRasterHeight() - 1 + 0.5f);
            } else {
                setLatLongMetadata(targetProduct, trgAbsRoot, "first_near_lat", "first_near_long",
                        targetProduct.getSceneRasterWidth() - 1 + 0.5f, 0.5f);
                setLatLongMetadata(targetProduct, trgAbsRoot, "first_far_lat", "first_far_long", 0.5f, 0.5f);

                setLatLongMetadata(targetProduct, trgAbsRoot, "last_near_lat", "last_near_long",
                        targetProduct.getSceneRasterWidth() - 1 + 0.5f, targetProduct.getSceneRasterHeight() - 1 + 0.5f);
                setLatLongMetadata(targetProduct, trgAbsRoot, "last_far_lat", "last_far_long",
                        0.5f, targetProduct.getSceneRasterHeight() - 1 + 0.5f);
            }

            final MetadataAttribute slantRange = trgAbsRoot.getAttribute("slant_range_to_first_pixel");
            if(slantRange != null) {
                final TiePointGrid srTPG = targetProduct.getTiePointGrid("slant_range_time");
                if(srTPG != null && region != null) {
                    final boolean srgrFlag = srcAbsRoot.getAttributeInt("srgr_flag") != 0;
                    double slantRangeDist;
                    if (srgrFlag) {
                        final double slantRangeTime;
                        if (nearRangeOnLeft) {
                            slantRangeTime = srTPG.getPixelDouble(
                                    region.x, region.y) / 1000000000.0; // ns to s
                        } else {
                            slantRangeTime = srTPG.getPixelDouble(
                                    targetProduct.getSceneRasterWidth() - region.x - 1,
                                    region.y) / 1000000000.0; // ns to s
                        }
                        final double halfLightSpeed = 299792458.0 / 2.0;
                        slantRangeDist = slantRangeTime * halfLightSpeed;
                        slantRange.getData().setElemDouble(slantRangeDist);
                    } else {
                        final double slantRangeToFirstPixel = srcAbsRoot.getAttributeDouble("slant_range_to_first_pixel");
                        final double rangeSpacing = srcAbsRoot.getAttributeDouble("RANGE_SPACING", 0);
                        if (nearRangeOnLeft) {
                            slantRangeDist = slantRangeToFirstPixel + region.x*rangeSpacing;
                        } else {
                            slantRangeDist = slantRangeToFirstPixel +
                                    (targetProduct.getSceneRasterWidth() - region.x - 1)*rangeSpacing;
                        }
                        slantRange.getData().setElemDouble(slantRangeDist);
                    }
                }
            }

            setSubsetSRGRCoefficients(sourceProduct, targetProduct, subsetDef, trgAbsRoot, nearRangeOnLeft);
        } catch(Exception e) {
            throw new IOException(e);
        }
    }

    private static boolean isNearRangeOnLeft(final Product product) {
        final TiePointGrid incidenceAngle = product.getTiePointGrid("incident_angle");
        if(incidenceAngle != null) {
            final double incidenceAngleToFirstPixel = incidenceAngle.getPixelDouble(0, 0);
            final double incidenceAngleToLastPixel = incidenceAngle.getPixelDouble(product.getSceneRasterWidth()-1, 0);
            return (incidenceAngleToFirstPixel < incidenceAngleToLastPixel);
        } else {
            return true;
        }
    }

    private static void setSubsetSRGRCoefficients(
            final Product sourceProduct, final Product targetProduct, final ProductSubsetDef subsetDef,
            final MetadataElement absRoot, final boolean nearRangeOnLeft) {

        final MetadataElement SRGRCoefficientsElem = absRoot.getElement("SRGR_Coefficients");
        if(SRGRCoefficientsElem != null) {
            final double rangeSpacing = absRoot.getAttributeDouble("RANGE_SPACING", 0);
            final double colIndex = subsetDef.getRegion() == null ? 0 : subsetDef.getRegion().getX();

            for(MetadataElement srgrList : SRGRCoefficientsElem.getElements()) {
                final double grO = srgrList.getAttributeDouble("ground_range_origin", 0);
                double ground_range_origin_subset;
                if (nearRangeOnLeft) {
                    ground_range_origin_subset = grO + colIndex*rangeSpacing;
                } else {
                    final double colIndexFromRight = sourceProduct.getSceneRasterWidth() - colIndex -
                                                     targetProduct.getSceneRasterWidth();
                    ground_range_origin_subset = grO + colIndexFromRight*rangeSpacing;
                }
                srgrList.setAttributeDouble("ground_range_origin", ground_range_origin_subset);
            }
        }
    }

    private static void setLatLongMetadata(final Product product, final MetadataElement absRoot,
                                           final String tagLat, final String tagLon, final float x, final float y) {
        final PixelPos pixelPos = new PixelPos(x, y);
        final GeoPos geoPos = new GeoPos();
        if(product.getSceneGeoCoding() == null) return;
        product.getSceneGeoCoding().getGeoPos(pixelPos, geoPos);

        final MetadataAttribute lat = absRoot.getAttribute(tagLat);
        if(lat != null)
            lat.getData().setElemDouble(geoPos.getLat());
        final MetadataAttribute lon = absRoot.getAttribute(tagLon);
        if(lon != null)
            lon.getData().setElemDouble(geoPos.getLon());
    }

    /**
     * Reads a data product and returns a in-memory representation of it. This method was called by
     * <code>readProductNodes(input, subsetInfo)</code> of the abstract superclass.
     *
     * @throws IllegalArgumentException if <code>input</code> type is not one of the supported input sources.
     * @throws IOException              if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        if (getInput() instanceof Product) {
            sourceProduct = (Product) getInput();
        } else {
            throw new IllegalArgumentException("unsupported input source: " + getInput());
        }
        Debug.assertNotNull(sourceProduct);
        sceneRasterWidth = sourceProduct.getSceneRasterWidth();
        sceneRasterHeight = sourceProduct.getSceneRasterHeight();
        if (getSubsetDef() != null) {
            Dimension s = getSubsetDef().getSceneRasterSize(sceneRasterWidth, sceneRasterHeight);
            sceneRasterWidth = s.width;
            sceneRasterHeight = s.height;
        }
        final Product targetProduct = createProduct();
        updateMetadata(sourceProduct, targetProduct, getSubsetDef());

        return targetProduct;
    }

    /**
     * The template method which is called by the <code>readBandRasterDataSubSampling</code> method after an optional
     * spatial subset has been applied to the input parameters.
     * <p>The destination band, buffer and region parameters are exactly the ones passed to the original
     * <code>readBandRasterDataSubSampling</code> call. Since the <code>destOffsetX</code> and <code>destOffsetY</code>
     * parameters are already taken into acount in the <code>sourceOffsetX</code> and <code>sourceOffsetY</code>
     * parameters, an implementor of this method is free to ignore them.
     *
     * @param sourceOffsetX the absolute X-offset in source raster co-ordinates
     * @param sourceOffsetY the absolute Y-offset in source raster co-ordinates
     * @param sourceWidth   the width of region providing samples to be read given in source raster co-ordinates
     * @param sourceHeight  the height of region providing samples to be read given in source raster co-ordinates
     * @param sourceStepX   the sub-sampling in X direction within the region providing samples to be read
     * @param sourceStepY   the sub-sampling in Y direction within the region providing samples to be read
     * @param destBand      the destination band which identifies the data source from which to read the sample values
     * @param destBuffer    the destination buffer which receives the sample values to be read
     * @param destOffsetX   the X-offset in the band's raster co-ordinates
     * @param destOffsetY   the Y-offset in the band's raster co-ordinates
     * @param destWidth     the width of region to be read given in the band's raster co-ordinates
     * @param destHeight    the height of region to be read given in the band's raster co-ordinates
     * @throws IOException if an I/O error occurs
     * @see #getSubsetDef
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                          int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand,
                                          int destOffsetX, int destOffsetY,
                                          int destWidth, int destHeight,
                                          ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        Band sourceBand = (Band) bandMap.get(destBand);
        // if the band already has an internal raster
        if (sourceBand.getRasterData() != null) {
            // if the destination region equals the entire raster
            if (sourceBand.getRasterWidth() == destWidth
                    && sourceBand.getRasterHeight() == destHeight) {
                copyBandRasterDataFully(sourceBand,
                                        destBuffer,
                                        destWidth, destHeight);
                // else if the destination region is smaller than the entire raster
            } else {
                copyBandRasterDataSubSampling(sourceBand,
                                              sourceOffsetX, sourceOffsetY,
                                              sourceWidth, sourceHeight,
                                              sourceStepX, sourceStepY,
                                              destBuffer,
                                              destWidth);
            }
        } else {
            // if the desired destination region equals the source raster
            if (sourceWidth == destWidth
                    && sourceHeight == destHeight) {
                readBandRasterDataRegion(sourceBand,
                                         sourceOffsetX, sourceOffsetY,
                                         sourceWidth, sourceHeight,
                                         destBuffer,
                                         pm);
                // else if the desired destination region is smaller than the source raster
            } else {
                Rectangle destRect = new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight);
                readBandRasterDataSubsampled(sourceBand, destBuffer, destRect, sourceOffsetX, sourceOffsetY,
                                             sourceWidth, sourceHeight, sourceStepX, sourceStepY);
            }
        }
    }

    private static void readBandRasterDataSubsampled(Band band, ProductData destData, Rectangle destRect, int sourceOffsetX, int sourceOffsetY,
                                                     int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY) throws IOException {

        Point[] tileIndices = band.getSourceImage().getTileIndices(new Rectangle(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight));
        HashMap<Rectangle, ProductData> tileMap = new HashMap<>();
        for (Point tileIndex : tileIndices) {
            Rectangle tileRect = band.getSourceImage().getTileRect(tileIndex.x, tileIndex.y);
            if (tileRect.isEmpty()) {
                continue;
            }
            final ProductData tileData = ProductData.createInstance(band.getDataType(), tileRect.width * tileRect.height);
            band.readRasterData(tileRect.x, tileRect.y, tileRect.width, tileRect.height, tileData, ProgressMonitor.NULL);
            tileMap.put(tileRect, tileData);
        }

        for (int y = 0; y < destRect.height; y++) {
            final int currentSrcYOffset = sourceOffsetY + y * sourceStepY;
            int currentDestYOffset = y * destRect.width;
            for (int x = 0; x < destRect.width; x++) {
                double value = getSourceValue(band, tileMap, sourceOffsetX + x * sourceStepX, currentSrcYOffset);
                destData.setElemDoubleAt(currentDestYOffset + x, value);
            }

        }
    }

    private static double getSourceValue(Band band, HashMap<Rectangle, ProductData> tileMap, int sourceX, int sourceY) {
        MultiLevelImage img = band.getSourceImage();
        Rectangle tileRect = img.getTileRect(img.XToTileX(sourceX), img.YToTileY(sourceY));
        ProductData productData = tileMap.get(tileRect);
        int currentX = sourceX - tileRect.x;
        int currentY = sourceY - tileRect.y;
        return productData.getElemDoubleAt(currentY * tileRect.width + currentX);
    }


    private void copyBandRasterDataFully(Band sourceBand, ProductData destBuffer, int destWidth, int destHeight) {
        copyData(sourceBand.getRasterData(),
                 0,
                 destBuffer,
                 0,
                 destWidth * destHeight);
    }

    private void readBandRasterDataRegion(Band sourceBand,
                                          int sourceOffsetX, int sourceOffsetY,
                                          int sourceWidth, int sourceHeight,
                                          ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        sourceBand.readRasterData(sourceOffsetX,
                                  sourceOffsetY,
                                  sourceWidth,
                                  sourceHeight,
                                  destBuffer, pm);
    }


    private void copyBandRasterDataSubSampling(Band sourceBand,
                                               int sourceOffsetX, int sourceOffsetY,
                                               int sourceWidth, int sourceHeight,
                                               int sourceStepX, int sourceStepY,
                                               ProductData destBuffer,
                                               int destWidth) {
        final int sourceMinY = sourceOffsetY;
        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
        int destPos = 0;
        for (int sourceY = sourceMinY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
            // no subsampling in x-direction
            if (sourceStepX == 1) {
                copyData(sourceBand.getRasterData(),
                         sourceY * sourceBand.getRasterWidth() + sourceOffsetX,
                         destBuffer,
                         destPos,
                         destWidth);
            } else {
                copyLine(sourceBand.getRasterData(),
                         sourceY * sourceBand.getRasterWidth() + sourceOffsetX,
                         sourceWidth,
                         sourceStepX,
                         destBuffer,
                         destPos);
            }
            destPos += destWidth;
        }
    }

    private static void copyData(ProductData sourceBuffer,
                                 int sourcePos,
                                 ProductData destBuffer,
                                 int destPos,
                                 int destLength) {
        System.arraycopy(sourceBuffer.getElems(), sourcePos, destBuffer.getElems(), destPos, destLength);
    }

    private static void copyLine(ProductData sourceBuffer,
                                 int sourceOffsetPos,
                                 int sourceWidth,
                                 int sourceStepX,
                                 ProductData destBuffer,
                                 int destOffsetPos) {
        final int sourceMinX = sourceOffsetPos;
        final int sourceMaxX = sourceOffsetPos + sourceWidth - 1;
        if (destBuffer.getElems() instanceof byte[]) {
            byte[] destArray = (byte[]) destBuffer.getElems();
            byte[] sourceArray = (byte[]) sourceBuffer.getElems();
            for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                destArray[destOffsetPos] = sourceArray[sourceX];
                destOffsetPos++;
            }
        } else if (destBuffer.getElems() instanceof short[]) {
            short[] destArray = (short[]) destBuffer.getElems();
            short[] sourceArray = (short[]) sourceBuffer.getElems();
            for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                destArray[destOffsetPos] = sourceArray[sourceX];
                destOffsetPos++;
            }
        } else if (destBuffer.getElems() instanceof int[]) {
            int[] destArray = (int[]) destBuffer.getElems();
            int[] sourceArray = (int[]) sourceBuffer.getElems();
            for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                destArray[destOffsetPos] = sourceArray[sourceX];
                destOffsetPos++;
            }
        } else if (destBuffer.getElems() instanceof float[]) {
            float[] destArray = (float[]) destBuffer.getElems();
            float[] sourceArray = (float[]) sourceBuffer.getElems();
            for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                destArray[destOffsetPos] = sourceArray[sourceX];
                destOffsetPos++;
            }
        } else if (destBuffer.getElems() instanceof double[]) {
            double[] destArray = (double[]) destBuffer.getElems();
            double[] sourceArray = (double[]) sourceBuffer.getElems();
            for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                destArray[destOffsetPos] = sourceArray[sourceX];
                destOffsetPos++;
            }
        } else {
            Debug.assertTrue(false, "illegal product data type");
            throw new IllegalStateException("illegal product data type");
        }
    }

    private Product createProduct() {
        Product sourceProduct = getSourceProduct();
        Debug.assertNotNull(sourceProduct);
        Debug.assertTrue(getSceneRasterWidth() > 0);
        Debug.assertTrue(getSceneRasterHeight() > 0);
        final String newProductName;
        if (this.newProductName == null || this.newProductName.length() == 0) {
            newProductName = sourceProduct.getName();
        } else {
            newProductName = this.newProductName;
        }
        final Product product = new Product(newProductName, sourceProduct.getProductType(),
                                            getSceneRasterWidth(),
                                            getSceneRasterHeight(),
                                            this);
        product.setPointingFactory(sourceProduct.getPointingFactory());
        if (newProductDesc == null || newProductDesc.length() == 0) {
            product.setDescription(sourceProduct.getDescription());
        } else {
            product.setDescription(newProductDesc);
        }
        if (!isMetadataIgnored()) {
            ProductUtils.copyMetadata(sourceProduct, product);
        }
        addTiePointGridsToProduct(product);
        addBandsToProduct(product);
        ProductUtils.copyMasks(sourceProduct, product);
        addFlagCodingsToProduct(product);
        addGeoCodingToProduct(product);

        // only copy index codings associated with accepted nodes
        copyAcceptedIndexCodings(product);

        ProductUtils.copyVectorData(sourceProduct, product);
        ProductUtils.copyOverlayMasks(sourceProduct, product);
        ProductUtils.copyPreferredTileSize(sourceProduct, product);
        setSceneRasterStartAndStopTime(product);
        addSubsetInfoMetadata(product);
        if (sourceProduct.getQuicklookBandName() != null
            && product.getQuicklookBandName() == null
            && product.containsBand(sourceProduct.getQuicklookBandName())) {
            product.setQuicklookBandName(sourceProduct.getQuicklookBandName());
        }
        product.setAutoGrouping(sourceProduct.getAutoGrouping());

        return product;
    }

    private void setSceneRasterStartAndStopTime(Product product) {
        final Product sourceProduct = getSourceProduct();
        final ProductData.UTC startTime = sourceProduct.getStartTime();
        final ProductData.UTC stopTime = sourceProduct.getEndTime();
        final ProductSubsetDef subsetDef = getSubsetDef();
        if (startTime != null && stopTime != null && subsetDef != null && subsetDef.getRegion() != null) {
            final double height = sourceProduct.getSceneRasterHeight();
            final Rectangle region = subsetDef.getRegion();
            final double regionY = region.getY();
            final double regionHeight = region.getHeight();
            final double dStart = startTime.getMJD();
            final double dStop = stopTime.getMJD();
            final double vPerLine = (dStop - dStart) / (height - 1);
            final double newStart = vPerLine * regionY + dStart;
            final double newStop = vPerLine * (regionHeight - 1) + newStart;
            product.setStartTime(new ProductData.UTC(newStart));
            product.setEndTime(new ProductData.UTC(newStop));
        } else {
            product.setStartTime(startTime);
            product.setEndTime(stopTime);
        }
    }

    private void addSubsetInfoMetadata(Product product) {
        if (getSubsetDef() != null) {
            ProductSubsetDef subsetDef = getSubsetDef();
            Product sourceProduct = getSourceProduct();
            String nameSubsetinfo = "SubsetInfo";
            MetadataElement subsetElem = new MetadataElement(nameSubsetinfo);
            addAttribString("SourceProduct.name", sourceProduct.getName(), subsetElem);
            subsetElem.setAttributeInt("SubSampling.x", subsetDef.getSubSamplingX());
            subsetElem.setAttributeInt("SubSampling.y", subsetDef.getSubSamplingY());
            if (subsetDef.getRegion() != null) {
                Rectangle region = subsetDef.getRegion();
                subsetElem.setAttributeInt("SubRegion.x", region.x);
                subsetElem.setAttributeInt("SubRegion.y", region.y);
                subsetElem.setAttributeInt("SubRegion.width", region.width);
                subsetElem.setAttributeInt("SubRegion.height", region.height);
            }
            String[] nodeNames = subsetDef.getNodeNames();
            if (nodeNames != null) {
                for (int i = 0; i < nodeNames.length; i++) {
                    addAttribString("ProductNodeName." + (i + 1), nodeNames[i], subsetElem);
                }
            }
            ProductUtils.addElementToHistory(product, subsetElem);
        }
    }

    // @todo 1 nf/nf - duplicated code in ProductProjectionBuilder, ProductFlipper and ProductSubsetBulider

    protected void addBandsToProduct(Product product) {
        Debug.assertNotNull(getSourceProduct());
        Debug.assertNotNull(product);
        for (int i = 0; i < getSourceProduct().getNumBands(); i++) {
            Band sourceBand = getSourceProduct().getBandAt(i);
            String bandName = sourceBand.getName();
            if (isNodeAccepted(bandName)) {
                Band destBand;
                boolean treatVirtualBandsAsRealBands = false;
                if (getSubsetDef() != null && getSubsetDef().getTreatVirtualBandsAsRealBands()) {
                    treatVirtualBandsAsRealBands = true;
                }

                //@todo 1 se/se - extract copy of a band or virtual band to create deep clone of band and virtual band
                if (!treatVirtualBandsAsRealBands && sourceBand instanceof VirtualBand) {
                    VirtualBand virtualSource = (VirtualBand) sourceBand;
                    if(getSubsetDef() == null) {
                        destBand = new VirtualBand(bandName,
                                                   sourceBand.getDataType(),
                                                   getSceneRasterWidth(),
                                                   getSceneRasterHeight(),
                                                   virtualSource.getExpression());
                    } else {
                        Dimension dim = getSubsetDef().getSceneRasterSize(sourceBand.getRasterWidth(),
                                                                          sourceBand.getRasterHeight(),
                                                                          sourceBand.getName());
                        destBand = new VirtualBand(bandName,
                                                   sourceBand.getDataType(),
                                                   dim.width,
                                                   dim.height,
                                                   virtualSource.getExpression());
                    }
                } else {
                    if(getSubsetDef() == null) {
                        destBand = new Band(bandName,
                                            sourceBand.getDataType(),
                                            getSceneRasterWidth(),
                                            getSceneRasterHeight());
                    }else {
                        Dimension dim = getSubsetDef().getSceneRasterSize(sourceBand.getRasterWidth(),
                                                                          sourceBand.getRasterHeight(),
                                                                          sourceBand.getName());
                        destBand = new Band(bandName,
                                            sourceBand.getDataType(),
                                            dim.width,
                                            dim.height);
                    }
                }
                if (sourceBand.getUnit() != null) {
                    destBand.setUnit(sourceBand.getUnit());
                }
                if (sourceBand.getDescription() != null) {
                    destBand.setDescription(sourceBand.getDescription());
                }
                destBand.setScalingFactor(sourceBand.getScalingFactor());
                destBand.setScalingOffset(sourceBand.getScalingOffset());
                destBand.setLog10Scaled(sourceBand.isLog10Scaled());
                destBand.setSpectralBandIndex(sourceBand.getSpectralBandIndex());
                destBand.setSpectralWavelength(sourceBand.getSpectralWavelength());
                destBand.setSpectralBandwidth(sourceBand.getSpectralBandwidth());
                destBand.setSolarFlux(sourceBand.getSolarFlux());
                if (sourceBand.isNoDataValueSet()) {
                    destBand.setNoDataValue(sourceBand.getNoDataValue());
                }
                destBand.setNoDataValueUsed(sourceBand.isNoDataValueUsed());
                destBand.setValidPixelExpression(sourceBand.getValidPixelExpression());
                FlagCoding sourceFlagCoding = sourceBand.getFlagCoding();
                IndexCoding sourceIndexCoding = sourceBand.getIndexCoding();
                if (sourceFlagCoding != null) {
                    String flagCodingName = sourceFlagCoding.getName();
                    FlagCoding destFlagCoding = product.getFlagCodingGroup().get(flagCodingName);
                    if (destFlagCoding == null) {
                        destFlagCoding = ProductUtils.copyFlagCoding(sourceFlagCoding, product);
                    }
                    destBand.setSampleCoding(destFlagCoding);
                } else if (sourceIndexCoding != null) {
                    String indexCodingName = sourceIndexCoding.getName();
                    IndexCoding destIndexCoding = product.getIndexCodingGroup().get(indexCodingName);
                    if (destIndexCoding == null) {
                        destIndexCoding = ProductUtils.copyIndexCoding(sourceIndexCoding, product);
                    }
                    destBand.setSampleCoding(destIndexCoding);
                } else {
                    destBand.setSampleCoding(null);
                }
                if (isFullScene(getSubsetDef(), sourceBand) && sourceBand.isStxSet()) {
                    copyStx(sourceBand, destBand);
                }

                product.addBand(destBand);
                bandMap.put(destBand, sourceBand);
            }
        }
        for (final Map.Entry<Band, RasterDataNode> entry : bandMap.entrySet()) {
            copyImageInfo(entry.getValue(), entry.getKey());
        }
    }

    protected void addTiePointGridsToProduct(final Product product) {
        final GeoCoding geoCoding = getSourceProduct().getSceneGeoCoding();
        final String latGridName;
        final String lonGridName;
        if (geoCoding instanceof TiePointGeoCoding) {
            final TiePointGeoCoding tiePointGeoCoding = (TiePointGeoCoding) geoCoding;
            final TiePointGrid latGrid = tiePointGeoCoding.getLatGrid();
            final TiePointGrid lonGrid = tiePointGeoCoding.getLonGrid();
            latGridName = latGrid.getName();
            lonGridName = lonGrid.getName();
        } else {
            latGridName = null;
            lonGridName = null;
        }

        for (int i = 0; i < getSourceProduct().getNumTiePointGrids(); i++) {
            final TiePointGrid sourceTiePointGrid = getSourceProduct().getTiePointGridAt(i);
            final String gridName = sourceTiePointGrid.getName();

            if (isNodeAccepted(gridName) || (gridName.equals(latGridName) || gridName.equals(lonGridName))) {
                final TiePointGrid tiePointGrid = TiePointGrid.createSubset(sourceTiePointGrid, getSubsetDef());
                if (isFullScene(getSubsetDef(), sourceTiePointGrid) && sourceTiePointGrid.isStxSet()) {
                    copyStx(sourceTiePointGrid, tiePointGrid);
                }
                product.addTiePointGrid(tiePointGrid);
                copyImageInfo(sourceTiePointGrid, tiePointGrid);
            }
        }
    }

    private void copyStx(RasterDataNode sourceRaster, RasterDataNode targetRaster) {
        final Stx sourceStx = sourceRaster.getStx();
        final Histogram sourceHistogram = sourceStx.getHistogram();
        final Histogram targetHistogram = new Histogram(sourceStx.getHistogramBinCount(),
                                                        sourceHistogram.getLowValue(0),
                                                        sourceHistogram.getHighValue(0),
                                                        1);

        System.arraycopy(sourceHistogram.getBins(0), 0, targetHistogram.getBins(0), 0, sourceStx.getHistogramBinCount());

        final Stx targetStx = new Stx(sourceStx.getMinimum(),
                                      sourceStx.getMaximum(),
                                      sourceStx.getMean(),
                                      sourceStx.getStandardDeviation(),
                                      sourceStx.getCoefficientOfVariation(),
                                      sourceStx.getEquivalentNumberOfLooks(),
                                      sourceStx.isLogHistogram(),
                                      sourceStx.isIntHistogram(),
                                      targetHistogram,
                                      sourceStx.getResolutionLevel());

        targetRaster.setStx(targetStx);
    }

    private void copyImageInfo(RasterDataNode sourceRaster, RasterDataNode targetRaster) {
        final ImageInfo imageInfo;
        if (sourceRaster.getImageInfo() != null) {
            imageInfo = sourceRaster.getImageInfo().createDeepCopy();
            targetRaster.setImageInfo(imageInfo);
        }
    }

    private boolean isFullScene(ProductSubsetDef subsetDef, RasterDataNode rasterDataNode) {
        if (subsetDef == null) {
            return true;
        }
        final Rectangle sourceRegion = new Rectangle(0, 0, sourceProduct.getSceneRasterWidth(), getSceneRasterHeight());
        if(subsetDef.getRegionMap() == null) {
            return subsetDef.getRegion() == null
                    || subsetDef.getRegion().equals(sourceRegion)
                    && subsetDef.getSubSamplingX() == 1
                    && subsetDef.getSubSamplingY() == 1;
        }
        return subsetDef.getRegionMap().get(rasterDataNode.getName()) == null
                || subsetDef.getRegionMap().get(rasterDataNode.getName()).equals(sourceRegion)
                && subsetDef.getSubSamplingX() == 1
                && subsetDef.getSubSamplingY() == 1;
    }

    protected void addGeoCodingToProduct(final Product product) {

        if (!getSourceProduct().transferGeoCodingTo(product, getSubsetDef())) {
            Debug.trace("GeoCoding could not be transferred.");
        }

        //Adjust sceneGeocoding
        if(getSubsetDef() != null && getSubsetDef().getRegionMap() != null) {
            for(RasterDataNode rasterDataNode : product.getRasterDataNodes()) {
                if(rasterDataNode.getRasterWidth() == product.getSceneRasterSize().getWidth() &&
                        rasterDataNode.getRasterHeight() == product.getSceneRasterSize().getHeight()
                        && rasterDataNode.getGeoCoding() != null) {
                    ProductUtils.copyGeoCoding(rasterDataNode,product);
                }
            }
        }
    }

    private void copyAcceptedIndexCodings(final Product product) {

        for(Band srcBand : product.getBands()) {
            if(srcBand.isIndexBand() && isNodeAccepted(srcBand.getName())) {

                IndexCoding sourceIndexCoding = srcBand.getIndexCoding();
                IndexCoding destIndexCoding = new IndexCoding(sourceIndexCoding.getName());
                destIndexCoding.setDescription(sourceIndexCoding.getDescription());
                cloneIndexes(sourceIndexCoding, destIndexCoding);
                product.getIndexCodingGroup().add(destIndexCoding);
            }
        }
    }
}
