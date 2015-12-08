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
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ProductUtils;

import java.io.IOException;

/**
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class ProductFlipper extends AbstractProductBuilder {

    public static final int FLIP_HORIZONTAL = 1;
    public static final int FLIP_VERTICAL = 2;
    public static final int FLIP_BOTH = 3;

    private int flipType;

    public ProductFlipper(int flipType) {
        this(flipType, false);
    }

    public ProductFlipper(int flipType, boolean sourceProductOwner) {
        super(sourceProductOwner);
        if ((flipType != FLIP_HORIZONTAL) && (flipType != FLIP_VERTICAL) && (flipType != FLIP_BOTH)) {
            throw new IllegalArgumentException("invalid flip type");
        }
        this.flipType = flipType;
    }

    public static Product createFlippedProduct(Product sourceProduct, int flipType, String name, String desc) throws
                                                                                                              IOException {
        return createFlippedProduct(sourceProduct, false, flipType, name, desc);
    }

    public static Product createFlippedProduct(Product sourceProduct, boolean sourceProductOwner, int flipType,
                                               String name, String desc) throws IOException {
        ProductFlipper productFlipper = new ProductFlipper(flipType, sourceProductOwner);
        return productFlipper.readProductNodes(sourceProduct, null, name, desc);
    }

    public int getFlipType() {
        return flipType;
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
        if (flipType == 0) {
            throw new IllegalStateException("no flip type set");
        }

        sceneRasterWidth = sourceProduct.getSceneRasterWidth();
        sceneRasterHeight = sourceProduct.getSceneRasterHeight();

        return createProduct();
    }

    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p>Overrides of this method should always call <code>super.close();</code> after disposing this instance.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        disposeBandMap();
        super.close();
    }

    /**
     * Reads raster data from the data source specified by the given destination band into the given in-memory buffer
     * and region.
     * <p>For a complete description, please refer to the {@link ProductReader#readBandRasterData(Band, int, int, int, int, ProductData, com.bc.ceres.core.ProgressMonitor)}  interface definition}
     * of this method.
     * <p>The <code>AbstractProductReader</code> implements this method using the <i>Template Method</i> pattern. The
     * template method in this case is the abstract method to which the call is delegated after an optional spatial
     * subset given by {@link #getSubsetDef()} has been applied to the input parameters.
     *
     * @param destBand    the destination band which identifies the data source from which to read the sample values
     * @param destOffsetX the X-offset in the band's raster co-ordinates
     * @param destOffsetY the Y-offset in the band's raster co-ordinates
     * @param destWidth   the width of region to be read given in the band's raster co-ordinates
     * @param destHeight  the height of region to be read given in the band's raster co-ordinates
     * @param destBuffer  the destination buffer which receives the sample values to be read
     * @param pm          a monitor to inform the user about progress
     *
     * @throws IOException              if an I/O error occurs
     * @throws IllegalArgumentException if the number of elements destination buffer not equals <code>destWidth *
     *                                  destHeight</code> or the destination region is out of the band's raster
     * @see #readBandRasterDataImpl
     * @see #getSubsetDef()
     * @see ProductReader#readBandRasterData(Band, int, int, int, int, ProductData, com.bc.ceres.core.ProgressMonitor)
     * @see Band#getRasterWidth()
     * @see Band#getRasterHeight()
     */
    @Override
    public void readBandRasterData(Band destBand,
                                   int destOffsetX,
                                   int destOffsetY,
                                   int destWidth,
                                   int destHeight,
                                   ProductData destBuffer,
                                   ProgressMonitor pm) throws IOException {

        Band sourceBand = (Band) bandMap.get(destBand);
        Debug.assertNotNull(sourceBand);

        Guardian.assertNotNull("destBand", destBand);
        Guardian.assertNotNull("destBuffer", destBuffer);

        if (destBuffer.getNumElems() < destWidth * destHeight) {
            throw new IllegalArgumentException("destination buffer too small");
        }
        if (destBuffer.getNumElems() > destWidth * destHeight) {
            throw new IllegalArgumentException("destination buffer too big");
        }


        final int sourceW = sourceProduct.getSceneRasterWidth();
        final int sourceH = sourceProduct.getSceneRasterHeight();

        float[] line = new float[sourceW];

        pm.beginTask("Flipping raster data...", destHeight);
        try {
            int sourceX;
            int sourceY;
            if (flipType == FLIP_HORIZONTAL) {
                for (int j = 0; j < destHeight; j++) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    sourceY = destOffsetY + j;
                    sourceBand.readPixels(0, sourceY, sourceW, 1, line, SubProgressMonitor.create(pm, 1));
                    for (int i = 0; i < destWidth; i++) {
                        sourceX = sourceW - (destOffsetX + i + 1);
                        destBuffer.setElemFloatAt(j * destWidth + i, line[sourceX]);
                    }
                }
            } else if (flipType == FLIP_VERTICAL) {
                for (int j = 0; j < destHeight; j++) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    sourceY = sourceH - (destOffsetY + j + 1);
                    sourceBand.readPixels(0, sourceY, sourceW, 1, line, SubProgressMonitor.create(pm, 1));
                    for (int i = 0; i < destWidth; i++) {
                        sourceX = destOffsetX + i;
                        destBuffer.setElemFloatAt(j * destWidth + i, line[sourceX]);
                    }
                }
            } else {
                for (int j = 0; j < destHeight; j++) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    sourceY = sourceH - (destOffsetY + j + 1);
                    sourceBand.readPixels(0, sourceY, sourceW, 1, line, SubProgressMonitor.create(pm, 1));
                    for (int i = 0; i < destWidth; i++) {
                        sourceX = sourceW - (destOffsetX + i + 1);
                        destBuffer.setElemFloatAt(j * destWidth + i, line[sourceX]);
                    }
                }
            }
        } finally {
            pm.done();
        }
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
     * @param destOffsetX   the X-offset in the band's raster co-ordinates
     * @param destOffsetY   the Y-offset in the band's raster co-ordinates
     * @param destWidth     the width of region to be read given in the band's raster co-ordinates
     * @param destHeight    the height of region to be read given in the band's raster co-ordinates
     * @param destBuffer    the destination buffer which receives the sample values to be read
     * @param pm            a monitor to inform the user about progress
     *
     * @throws IOException if an I/O error occurs
     * @see #getSubsetDef
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX,
                                          int sourceOffsetY,
                                          int sourceWidth,
                                          int sourceHeight,
                                          int sourceStepX,
                                          int sourceStepY,
                                          Band destBand,
                                          int destOffsetX,
                                          int destOffsetY,
                                          int destWidth,
                                          int destHeight,
                                          ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("invalid call");
    }

    private Product createProduct() {
        Debug.assertNotNull(getSourceProduct());
        Debug.assertTrue(getSceneRasterWidth() > 0);
        Debug.assertTrue(getSceneRasterHeight() > 0);
        final String newProductName;
        if (this.newProductName == null || this.newProductName.length() == 0) {
            newProductName = getSourceProduct().getName();
        } else {
            newProductName = this.newProductName;
        }
        final Product product = new Product(newProductName, getSourceProduct().getProductType(),
                                            getSceneRasterWidth(),
                                            getSceneRasterHeight(),
                                            this);
        product.setPointingFactory(getSourceProduct().getPointingFactory());
        if (newProductDesc == null || newProductDesc.length() == 0) {
            product.setDescription(getSourceProduct().getDescription());
        } else {
            product.setDescription(newProductDesc);
        }
        if (!isMetadataIgnored()) {
            addMetadataToProduct(product);
        }
        addTiePointGridsToProduct(product);
        addFlagCodingsToProduct(product);
        addIndexCodingsToProduct(product);
        addBandsToProduct(product);
        addGeoCodingToProduct(product);
        ProductUtils.copyMasks(getSourceProduct(), product);
        ProductUtils.copyVectorData(sourceProduct, product);
        ProductUtils.copyOverlayMasks(sourceProduct, product);
        ProductUtils.copyPreferredTileSize(sourceProduct, product);
        product.setStartTime(sourceProduct.getStartTime());
        product.setEndTime(sourceProduct.getEndTime());
        if (sourceProduct.getQuicklookBandName() != null
            && product.getQuicklookBandName() == null
            && product.containsBand(sourceProduct.getQuicklookBandName())) {
            product.setQuicklookBandName(sourceProduct.getQuicklookBandName());
        }
        product.setAutoGrouping(sourceProduct.getAutoGrouping());

        return product;
    }

    // @todo 1 nf/nf - duplicated code in ProductProjectionBuilder, ProductFlipper and ProductSubsetBulider
    private void addBandsToProduct(Product product) {
        Debug.assertNotNull(getSourceProduct());
        Debug.assertNotNull(product);
        for (int i = 0; i < getSourceProduct().getNumBands(); i++) {
            Band sourceBand = getSourceProduct().getBandAt(i);
            String bandName = sourceBand.getName();
            if (isNodeAccepted(bandName)) {
                Band destBand;
                if (sourceBand.isScalingApplied()) {
                    destBand = new Band(bandName,
                                        ProductData.TYPE_FLOAT32,
                                        getSceneRasterWidth(),
                                        getSceneRasterHeight());
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
                destBand.setSpectralBandIndex(sourceBand.getSpectralBandIndex());
                destBand.setSpectralWavelength(sourceBand.getSpectralWavelength());
                destBand.setSpectralBandwidth(sourceBand.getSpectralBandwidth());
                destBand.setSolarFlux(sourceBand.getSolarFlux());
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
                ImageInfo sourceImageInfo = sourceBand.getImageInfo();
                if (sourceImageInfo != null) {
                    destBand.setImageInfo(sourceImageInfo.createDeepCopy());
                }
                product.addBand(destBand);
                bandMap.put(destBand, sourceBand);
            }
        }
    }

    private void addTiePointGridsToProduct(final Product product) {
        for (int i = 0; i < getSourceProduct().getNumTiePointGrids(); i++) {
            final TiePointGrid sourceTiePointGrid = getSourceProduct().getTiePointGridAt(i);
            if (isNodeAccepted(sourceTiePointGrid.getName())) {

                double sourceOffsetX = sourceTiePointGrid.getOffsetX();
                double sourceOffsetY = sourceTiePointGrid.getOffsetY();
                double sourceStepX = sourceTiePointGrid.getSubSamplingX();
                double sourceStepY = sourceTiePointGrid.getSubSamplingY();
                if (getSubsetDef() != null) {
                    sourceStepX /= getSubsetDef().getSubSamplingX();
                    sourceStepY /= getSubsetDef().getSubSamplingY();
                    if (getSubsetDef().getRegion() != null) {
                        sourceOffsetX -= getSubsetDef().getRegion().x;
                        sourceOffsetY -= getSubsetDef().getRegion().y;
                    }
                }

                final float[] sourcePoints = sourceTiePointGrid.getTiePoints();
                final float[] targetPoints = new float[sourcePoints.length];
                final int width = sourceTiePointGrid.getGridWidth();
                final int height = sourceTiePointGrid.getGridHeight();

                if (flipType == FLIP_HORIZONTAL) {
                    sourceOffsetX = (sourceTiePointGrid.getRasterWidth() - (width - 1) * sourceStepX) - sourceOffsetX;
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            targetPoints[x + y * width] = sourcePoints[width - x - 1 + y * width];
                        }
                    }
                } else if (flipType == FLIP_VERTICAL) {
                    sourceOffsetY = (sourceTiePointGrid.getRasterHeight() - (height - 1) * sourceStepY) - sourceOffsetY;
                    for (int y = 0; y < height; y++) {
                        System.arraycopy(sourcePoints, (height - y - 1) * width, targetPoints, y * width, width);
                    }
                } else {
                    sourceOffsetX = (sourceTiePointGrid.getRasterWidth() - (width - 1) * sourceStepX) - sourceOffsetX;
                    sourceOffsetY = (sourceTiePointGrid.getRasterHeight() - (height - 1) * sourceStepY) - sourceOffsetY;
                    for (int y = 0; y < height; y++) {
                        final int lineIndex = height - y - 1;
                        for (int x = 0; x < width; x++) {
                            targetPoints[x + y * width] = sourcePoints[((width - x - 1) + (lineIndex * width))];
                        }
                    }
                }

                final TiePointGrid tiePointGrid = new TiePointGrid(sourceTiePointGrid.getName(),
                                                                   sourceTiePointGrid.getGridWidth(),
                                                                   sourceTiePointGrid.getGridHeight(),
                                                                   sourceOffsetX, sourceOffsetY,
                                                                   sourceStepX, sourceStepY,
                                                                   targetPoints,
                                                                   sourceTiePointGrid.getDiscontinuity());
                tiePointGrid.setUnit(sourceTiePointGrid.getUnit());
                tiePointGrid.setDescription(sourceTiePointGrid.getDescription());
                product.addTiePointGrid(tiePointGrid);
            }
        }
    }

    private void addGeoCodingToProduct(final Product product) {
        getSourceProduct().transferGeoCodingTo(product, null);
    }
}
