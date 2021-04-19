/*
 * Copyright (C) 2020 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.csa.rstb.polarimetric.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.csa.rstb.polarimetric.gpf.support.QuadPolProcessor;
import org.esa.s1tbx.commons.polsar.PolBandUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.util.Map;

/**
 * Perform Faraday-rotation correction for quad-pol product.
 */

@OperatorMetadata(alias = "Faraday-Rotation-Correction",
        category = "Radar/Polarimetric",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2020 by SkyWatch Space Applications Inc.",
        description = "Perform Faraday-rotation correction for quad-pol product")
public final class FaradayRotationCorrectionOp extends Operator implements QuadPolProcessor {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The sliding window size", interval = "[1, 100]", defaultValue = "10",
            label = "Window Size")
    private int windowSize = 10;

    private int halfWindowSize = 0;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;

    private PolBandUtils.MATRIX sourceProductType = null;
    private PolBandUtils.PolSourceBand[] srcBandList;

    private Band fraBand = null;
    private boolean outputEstimatedFRA = false;

    @Override
    public void initialize() throws OperatorException {

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSARProduct();

            sourceProductType = PolBandUtils.getSourceProductType(sourceProduct);
            if (sourceProductType != PolBandUtils.MATRIX.FULL) {
                throw new OperatorException("Quad-pol source product is expected.");
            }

            srcBandList = PolBandUtils.getSourceBands(sourceProduct, sourceProductType);
            halfWindowSize = windowSize / 2;
            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            createTargetProduct();

            updateTargetProductMetadata();
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        addSelectedBands();

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    /**
     * Add bands to the target product.
     *
     * @throws OperatorException The exception.
     */
    private void addSelectedBands() throws OperatorException {

        for (PolBandUtils.PolSourceBand bandList : srcBandList) {

            int k = 0;
            final Band[] targetBands = new Band[bandList.srcBands.length];
            for (int i = 0; i < bandList.srcBands.length; ++i) {
                final String bandName = bandList.srcBands[i].getName();
                final Band targetBand = new Band(
                        bandName, ProductData.TYPE_FLOAT32, sourceImageWidth, sourceImageHeight);
                targetBand.setUnit(bandList.srcBands[i].getUnit());
                targetProduct.addBand(targetBand);
                targetBands[k++] = targetBand;
            }
            bandList.addTargetBands(targetBands);

            if (outputEstimatedFRA) {
                fraBand = new Band(
                        "faraday_rotation_angle", ProductData.TYPE_FLOAT32, sourceImageWidth, sourceImageHeight);
                fraBand.setUnit(Unit.DEGREES);
                targetProduct.addBand(fraBand);
            }
        }
    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() throws Exception {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absRoot != null) {
            absRoot.setAttributeInt(AbstractMetadata.polsarData, 1);
        }
        PolBandUtils.saveNewBandNames(targetProduct, srcBandList);
    }


    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.snap.core.gpf.OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final double[][] mSr = new double[2][2]; // mean scattering matrix
        final double[][] mSi = new double[2][2];
        final double[][] Sr = new double[2][2]; // scattering matrix
        final double[][] Si = new double[2][2];
        final double[][] cSr = new double[2][2]; // FR corrected scattering matrix
        final double[][] cSi = new double[2][2];

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {
            try {
                final TileData[] tileDataList = new TileData[bandList.targetBands.length];
                int i = 0;
                for (Band targetBand : bandList.targetBands) {
                    final Tile targetTile = targetTiles.get(targetBand);
                    tileDataList[i++] = new TileData(targetTile, targetBand.getName());
                }
                final TileIndex tgtIndex = new TileIndex(tileDataList[0].tile);

                Tile estFRATile = null;
                ProductData estFRABuffer = null;
                if (outputEstimatedFRA) {
                    estFRATile = targetTiles.get(fraBand);
                    estFRABuffer = estFRATile.getDataBuffer();
                }

                final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
                final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
                final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h);

                for (int j = 0; j < bandList.srcBands.length; j++) {
                    final Band srcBand = bandList.srcBands[j];
                    sourceTiles[j] = getSourceTile(srcBand, sourceRectangle);
                    dataBuffers[j] = sourceTiles[j].getDataBuffer();
                }
                final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

                for (int y = y0; y < maxY; ++y) {
                    tgtIndex.calculateStride(y);
                    srcIndex.calculateStride(y);

                    for (int x = x0; x < maxX; ++x) {
                        final int tgtIdx = tgtIndex.getIndex(x);
                        final int srcIdx = srcIndex.getIndex(x);

//                        getMeanScatterMatrix(x, y, halfWindowSize, halfWindowSize, sourceProductType, sourceTiles,
//                                dataBuffers, mSr, mSi);
//
//                        final double omega = estimateFaradayRotationAngle(mSr, mSi);

                        // Lee's method
                        final double omega = getMeanFaradayRotationAngle(x, y, sourceTiles, dataBuffers);

                        if (outputEstimatedFRA) {
                            estFRABuffer.setElemFloatAt(tgtIdx, (float) (omega * Constants.RTOD));
                        }

                        getComplexScatterMatrix(srcIdx, dataBuffers, Sr, Si);

                        performFRCorrection(omega, Sr, Si, cSr, cSi);

                        for (final TileData tileData : tileDataList) {
                            if (tileData.bandName.contains("i_HH")) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) cSr[0][0]);
                            } else if (tileData.bandName.contains("q_HH")) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) cSi[0][0]);
                            } else if (tileData.bandName.contains("i_HV")) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) cSr[0][1]);
                            } else if (tileData.bandName.contains("q_HV")) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) cSi[0][1]);
                            } else if (tileData.bandName.contains("i_VH")) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) cSr[1][0]);
                            } else if (tileData.bandName.contains("q_VH")) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) cSi[1][0]);
                            } else if (tileData.bandName.contains("1_VV")) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) cSr[1][1]);
                            } else if (tileData.bandName.contains("q_VV")) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) cSi[1][1]);
                            }
                        }
                    }
                }

            } catch (Throwable e) {
                OperatorUtils.catchOperatorException(getId(), e);
            }
        }
    }

    private Rectangle getSourceRectangle(final int tx0, final int ty0, final int tw, final int th) {
        final int x0 = Math.max(0, tx0 - halfWindowSize);
        final int y0 = Math.max(0, ty0 - halfWindowSize);
        final int xMax = Math.min(tx0 + tw - 1 + halfWindowSize, sourceImageWidth - 1);
        final int yMax = Math.min(ty0 + th - 1 + halfWindowSize, sourceImageHeight - 1);
        final int w = xMax - x0 + 1;
        final int h = yMax - y0 + 1;
        return new Rectangle(x0, y0, w, h);
    }

    private static class TileData {
        final Tile tile;
        final ProductData dataBuffer;
        final String bandName;

        public TileData(final Tile tile, final String bandName) {
            this.tile = tile;
            this.dataBuffer = tile.getDataBuffer();
            this.bandName = bandName;
        }
    }

    private double estimateFaradayRotationAngle(final double[][] mSr, final double[][] mSi) {

        final double Zr01 = mSr[0][1] - mSr[1][0] - mSi[0][0] - mSi[1][1];
        final double Zi01 = mSi[0][1] - mSi[1][0] + mSr[0][0] + mSr[1][1];

        final double Zr10 = mSr[1][0] - mSr[0][1] - mSi[0][0] - mSi[1][1];
        final double Zi10 = mSi[1][0] - mSi[0][1] + mSr[0][0] + mSr[1][1];

        return -0.25 * Math.atan2(Zi01 * Zr10 - Zr01 * Zi10, Zr01 * Zr10 + Zi01 * Zi10);

//        Zr[0][0] = mSr[0][0] - mSr[1][1] - mSi[0][1] - mSi[1][0];
//        Zi[0][0] = mSi[0][0] - mSi[1][1] + mSr[0][1] + mSr[1][0];
//
//        Zr[0][1] = mSr[0][1] - mSr[1][0] - mSi[0][0] - mSi[1][1];
//        Zi[0][1] = mSi[0][1] - mSi[1][0] + mSr[0][0] + mSr[1][1];
//
//        Zr[1][0] = mSr[1][0] - mSr[0][1] - mSi[0][0] - mSi[1][1];
//        Zi[1][0] = mSi[1][0] - mSi[0][1] + mSr[0][0] + mSr[1][1];
//
//        Zr[1][1] = mSr[1][1] - mSr[0][0] - mSi[0][1] - mSi[1][0];
//        Zi[1][1] = mSi[1][1] - mSi[0][0] + mSr[0][1] + mSr[1][0];
    }

    private double getMeanFaradayRotationAngle(final int x, final int y, final Tile[] sourceTiles,
                                               final ProductData[] dataBuffers) {

        final int xSt = Math.max(x - halfWindowSize, sourceTiles[0].getMinX());
        final int xEd = Math.min(x + halfWindowSize, sourceTiles[0].getMaxX());
        final int ySt = Math.max(y - halfWindowSize, sourceTiles[0].getMinY());
        final int yEd = Math.min(y + halfWindowSize, sourceTiles[0].getMaxY());
        final int num = (yEd - ySt + 1) * (xEd - xSt + 1);

        final TileIndex srcIndex = new TileIndex(sourceTiles[0]);
        final double[][] tempSr = new double[2][2];
        final double[][] tempSi = new double[2][2];

        double zr = 0.0, zi = 0.0;
        for (int yy = ySt; yy <= yEd; ++yy) {
            srcIndex.calculateStride(yy);
            for (int xx = xSt; xx <= xEd; ++xx) {
                getComplexScatterMatrix(srcIndex.getIndex(xx), dataBuffers, tempSr, tempSi);
                final double[] zz = computeZrlZlr(tempSr, tempSi);
                zr += zz[0];
                zi += zz[1];
            }
        }
        zr /= num;
        zi /= num;

        return -0.25*Math.atan2(zi, zr);
    }

    private double[] computeZrlZlr(final double[][] mSr, final double[][] mSi) {

        final double Zr01 = mSr[0][1] - mSr[1][0] - mSi[0][0] - mSi[1][1];
        final double Zi01 = mSi[0][1] - mSi[1][0] + mSr[0][0] + mSr[1][1];

        final double Zr10 = mSr[1][0] - mSr[0][1] - mSi[0][0] - mSi[1][1];
        final double Zi10 = mSi[1][0] - mSi[0][1] + mSr[0][0] + mSr[1][1];

        return new double[]{Zr01 * Zr10 + Zi01 * Zi10, Zi01 * Zr10 - Zr01 * Zi10};
    }

    private void performFRCorrection(final double omega, final double[][] Sr, final double[][] Si,
                                     final double[][] cSr, final double[][] cSi) {

        final double c = Math.cos(omega);
        final double s = Math.sin(omega);
        final double c2 = c * c;
        final double s2 = s * s;
        final double cs = c * s;

        cSr[0][0] = c2 * Sr[0][0] + cs * Sr[0][1] - cs * Sr[1][0] - s2 * Sr[1][1];
        cSi[0][0] = c2 * Si[0][0] + cs * Si[0][1] - cs * Si[1][0] - s2 * Si[1][1];

        cSr[0][1] = c2 * Sr[0][1] - cs * Sr[0][0] - cs * Sr[1][1] + s2 * Sr[1][0];
        cSi[0][1] = c2 * Si[0][1] - cs * Si[0][0] - cs * Si[1][1] + s2 * Si[1][0];

        cSr[1][0] = c2 * Sr[1][0] + cs * Sr[0][0] + cs * Sr[1][1] + s2 * Sr[0][1];
        cSi[1][0] = c2 * Si[1][0] + cs * Si[0][0] + cs * Si[1][1] + s2 * Si[0][1];

        cSr[1][1] = c2 * Sr[1][1] + cs * Sr[0][1] - cs * Sr[1][0] - s2 * Sr[0][0];
        cSi[1][1] = c2 * Si[1][1] + cs * Si[0][1] - cs * Si[1][0] - s2 * Si[0][0];
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(Map, Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(FaradayRotationCorrectionOp.class);
        }
    }
}