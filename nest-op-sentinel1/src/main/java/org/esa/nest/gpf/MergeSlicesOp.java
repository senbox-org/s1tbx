/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.*;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.eo.Constants;
import org.esa.nest.util.MathUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges Sentinel-1 slice products
 */
@OperatorMetadata(alias = "MergeSlices",
        category = "SAR Tools\\SENTINEL-1",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description="Merges Sentinel-1 slice products")
public final class MergeSlicesOp extends Operator {

    @SourceProducts
    private Product[] sourceProducts;
    @TargetProduct
    private Product targetProduct;

    private MetadataElement absRoot = null;

    private int targetWidth = 0, targetHeight = 0;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public MergeSlicesOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProducts[0]);

            computeTargetWidthAndHeight();

            createTargetProduct();

            updateTargetProductMetadata();

        } catch (Throwable e) {
            throw new OperatorException(e.getMessage());
        }
    }


    private void computeTargetWidthAndHeight() {
        for(Product srcProduct : sourceProducts) {
            if(targetWidth < srcProduct.getSceneRasterWidth())
                targetWidth = srcProduct.getSceneRasterWidth();
            targetHeight += srcProduct.getSceneRasterHeight();
        }

    }

    private void createTargetProduct() {

        final Product srcProduct0 = sourceProducts[0];
        targetProduct = new Product(srcProduct0.getName(), srcProduct0.getProductType(), targetWidth, targetHeight);

        final Band[] sourceBands = srcProduct0.getBands();
        for (Band srcBand : sourceBands) {
            final Band newBand = targetProduct.addBand(srcBand.getName(), srcBand.getDataType());
            ProductUtils.copyRasterDataNodeProperties(srcBand, newBand);

            targetProduct.addBand(newBand);
        }

        OperatorUtils.copyProductNodes(srcProduct0, targetProduct);
    }

    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);

    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles The current tiles to be computed for each target band.
     * @param pm          A progress monitor which should be used to determine computation cancellation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        try {
   /*         final int tx0 = targetRectangle.x;
            final int ty0 = targetRectangle.y;
            final int tw = targetRectangle.width;
            final int th = targetRectangle.height;
            final double tileSlrtToFirstPixel = targetSlantRangeTimeToFirstPixel + tx0*targetDeltaSlantRangeTime;
            final double tileSlrtToLastPixel = targetSlantRangeTimeToFirstPixel + (tx0+tw-1)*targetDeltaSlantRangeTime;
            //System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

            // determine subswaths covered by the tile
            int firstSubSwathIndex = 0;
            for (int i = 0; i < numOfSubSwath; i++) {
                if (tileSlrtToFirstPixel >= subSwath[i].slrTimeToFirstPixel &&
                    tileSlrtToFirstPixel <= subSwath[i].slrTimeToLastPixel) {
                    firstSubSwathIndex = i + 1;
                    break;
                }
            }

            int lastSubSwathIndex = 0;
            for (int i = 0; i < numOfSubSwath; i++) {
                if (tileSlrtToLastPixel >= subSwath[i].slrTimeToFirstPixel &&
                    tileSlrtToLastPixel <= subSwath[i].slrTimeToLastPixel) {
                    lastSubSwathIndex = i + 1;
                }
            }

            final int numOfSourceTiles = lastSubSwathIndex - firstSubSwathIndex + 1;
            final boolean tileInOneSubSwath = (numOfSourceTiles == 1);

            final Rectangle[] sourceRectangle = new Rectangle[numOfSourceTiles];
            int k = 0;
            for (int i = firstSubSwathIndex; i <= lastSubSwathIndex; i++) {
                 sourceRectangle[k++] = getSourceRectangle(tx0, ty0, tw, th, i);
            }

            final BurstInfo burstInfo = new BurstInfo();
            final int lastX = tx0 + tw;
            final String bandNameI = "i_" + acquisitionMode;
            final String bandNameQ = "q_" + acquisitionMode;

            for (String pol:selectedPolarisations) {
                if (pol != null) {
                    final Tile targetTileI = targetTiles.get(targetProduct.getBand("i_" + pol));
                    final Tile targetTileQ = targetTiles.get(targetProduct.getBand("q_" + pol));

                    if (tileInOneSubSwath) {
                        computeTileInOneSwath(tx0, ty0, lastX, th, firstSubSwathIndex, pol,
                                sourceRectangle, bandNameI, bandNameQ, targetTileI, targetTileQ);

                    }
                }
            }     */
        } catch(Throwable e) {
            throw new OperatorException(e.getMessage());
        }
    }

    private void computeTileInOneSwath(final int tx0, final int ty0, final int lastX, final int th,
                                       final int firstSubSwathIndex, final String pol,
                                       final Rectangle[] sourceRectangle,
                                       final String bandNameI, final String bandNameQ,
                                       final Tile targetTileI, final Tile targetTileQ) {

   /*     final int yMin = computeYMin(subSwath[firstSubSwathIndex - 1]);
        final int yMax = computeYMax(subSwath[firstSubSwathIndex-1]);
        final int firstY = Math.max(ty0, yMin);
        final int lastY = Math.min(ty0 + th, yMax + 1);

        if(firstY >= lastY)
            return;

        final Band srcBandI = sourceProduct.getBand(bandNameI + firstSubSwathIndex + '_' + pol);
        final Band srcBandQ = sourceProduct.getBand(bandNameQ + firstSubSwathIndex + '_' + pol);
        final Tile sourceRasterI = getSourceTile(srcBandI, sourceRectangle[0]);
        final Tile sourceRasterQ = getSourceTile(srcBandQ, sourceRectangle[0]);
        final TileIndex srcTileIndex = new TileIndex(sourceRasterI);
        final TileIndex tgtIndex = new TileIndex(targetTileI);

        final short[] srcArrayI = (short[])sourceRasterI.getDataBuffer().getElems();
        final short[] srcArrayQ = (short[])sourceRasterQ.getDataBuffer().getElems();
        final short[] tgtArrayI = (short[])targetTileI.getDataBuffer().getElems();
        final short[] tgtArrayQ = (short[])targetTileQ.getDataBuffer().getElems();

        for (int y = firstY; y < lastY; y++) {

            if(!getLineIndicesInSourceProduct(y, subSwath[firstSubSwathIndex-1], burstInfo)) {
                continue;
            }

            final int tgtOffset = tgtIndex.calculateStride(y);
            final SubSwathInfo firstSubSwath = subSwath[firstSubSwathIndex-1];
            int offset;
            if (burstInfo.sy1 != -1 && burstInfo.targetTime > burstInfo.midTime) {
                offset = srcTileIndex.calculateStride(burstInfo.sy1);
            } else {
                offset = srcTileIndex.calculateStride(burstInfo.sy0);
            }

            final int sx = (int)Math.round(( (targetSlantRangeTimeToFirstPixel + tx0*targetDeltaSlantRangeTime)
                    - firstSubSwath.slrTimeToFirstPixel)/targetDeltaSlantRangeTime);

            System.arraycopy(srcArrayI, sx-offset, tgtArrayI, tx0-tgtOffset, lastX-tx0);
            System.arraycopy(srcArrayQ, sx-offset, tgtArrayQ, tx0-tgtOffset, lastX-tx0);
        }    */
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(MergeSlicesOp.class);
        }
    }
}