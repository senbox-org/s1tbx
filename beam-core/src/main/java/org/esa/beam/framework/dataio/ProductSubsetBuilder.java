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
package org.esa.beam.framework.dataio;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.Debug;
import org.esa.beam.util.ProductUtils;

import javax.media.jai.Histogram;
import java.awt.*;
import java.io.IOException;
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
        return createProduct();
    }

    /**
     * The template method which is called by the <code>readBandRasterDataSubSampling</code> method after an optional
     * spatial subset has been applied to the input parameters.
     * <p/>
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
            if (sourceBand.getSceneRasterWidth() == destWidth
                    && sourceBand.getSceneRasterHeight() == destHeight) {
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
                readBandRasterDataSubSampling(sourceBand,
                                              sourceOffsetX, sourceOffsetY,
                                              sourceWidth, sourceHeight,
                                              sourceStepX, sourceStepY,
                                              destBuffer,
                                              destWidth,
                                              pm);
            }
        }
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

    private void readBandRasterDataSubSampling(Band sourceBand,
                                               int sourceOffsetX, int sourceOffsetY,
                                               int sourceWidth, int sourceHeight,
                                               int sourceStepX, int sourceStepY,
                                               ProductData destBuffer,
                                               int destWidth, ProgressMonitor pm) throws IOException {
        final int sourceMinY = sourceOffsetY;
        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
        ProductData lineBuffer = ProductData.createInstance(destBuffer.getType(), sourceWidth);
        int destPos = 0;
        try {
            pm.beginTask("Reading sub sampled raster data...", 2 * (sourceMaxY - sourceMinY));
            for (int sourceY = sourceMinY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                sourceBand.readRasterData(sourceOffsetX, sourceY, sourceWidth, 1, lineBuffer,
                                          SubProgressMonitor.create(pm, 1));
                if (sourceStepX == 1) {
                    copyData(lineBuffer, 0, destBuffer, destPos, destWidth);
                } else {
                    copyLine(lineBuffer, 0, sourceWidth, sourceStepX, destBuffer, destPos);
                }
                pm.worked(1);
                destPos += destWidth;
            }
        } finally {
            pm.done();
        }
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
                         sourceY * sourceBand.getSceneRasterWidth() + sourceOffsetX,
                         destBuffer,
                         destPos,
                         destWidth);
            } else {
                copyLine(sourceBand.getRasterData(),
                         sourceY * sourceBand.getSceneRasterWidth() + sourceOffsetX,
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
            addTiePointGridsToProduct(product);
            addFlagCodingsToProduct(product);
            addIndexCodingsToProduct(product);
        }
        addBandsToProduct(product);
        if (!isMetadataIgnored()) {
            addGeoCodingToProduct(product);
        }
        ProductUtils.copyVectorData(sourceProduct, product);
        ProductUtils.copyMasks(sourceProduct, product);
        ProductUtils.copyOverlayMasks(sourceProduct, product);
        ProductUtils.copyRoiMasks(sourceProduct, product);
        ProductUtils.copyPreferredTileSize(sourceProduct, product);
        setSceneRasterStartAndStopTime(product);
        addSubsetInfoMetadata(product);

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
                    destBand = new VirtualBand(bandName,
                                               sourceBand.getDataType(),
                                               getSceneRasterWidth(),
                                               getSceneRasterHeight(),
                                               virtualSource.getExpression());
                } else {
                    destBand = new Band(bandName,
                                        sourceBand.getDataType(),
                                        getSceneRasterWidth(),
                                        getSceneRasterHeight());
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
                    Debug.assertNotNull(
                            destFlagCoding); // should not happen because flag codings should be already in product
                    destBand.setSampleCoding(destFlagCoding);
                } else if (sourceIndexCoding != null) {
                    String indexCodingName = sourceIndexCoding.getName();
                    IndexCoding destIndexCoding = product.getIndexCodingGroup().get(indexCodingName);
                    Debug.assertNotNull(
                            destIndexCoding); // should not happen because index codings should be already in product
                    destBand.setSampleCoding(destIndexCoding);
                } else {
                    destBand.setSampleCoding(null);
                }
                if (isFullScene(getSubsetDef()) && sourceBand.isStxSet()) {
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
        final GeoCoding geoCoding = getSourceProduct().getGeoCoding();
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
                if (isFullScene(getSubsetDef()) && sourceTiePointGrid.isStxSet()) {
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

    private boolean isFullScene(ProductSubsetDef subsetDef) {
        if (subsetDef == null) {
            return true;
        }
        final Rectangle sourceRegion = new Rectangle(0, 0, sourceProduct.getSceneRasterWidth(), getSceneRasterHeight());
        return subsetDef.getRegion() == null
                || subsetDef.getRegion().equals(sourceRegion)
                && subsetDef.getSubSamplingX() == 1
                && subsetDef.getSubSamplingY() == 1;
    }

    protected void addGeoCodingToProduct(final Product product) {

        if (!getSourceProduct().transferGeoCodingTo(product, getSubsetDef())) {
            Debug.trace("GeoCoding could not be transferred.");
        }
    }
}
