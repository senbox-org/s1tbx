/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.util.FastMath;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.dataio.PolBandUtils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.TileIndex;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Perform polarization orientation angle correction for given coherency matrix
 */

@OperatorMetadata(alias = "Orientation-Angle-Correction",
        category = "SAR Processing/Polarimetric",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Perform polarization orientation angle correction for given coherency matrix")
public final class OrientationAngleCorrectionOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    private PolBandUtils.QuadSourceBand[] srcBandList;
    private PolBandUtils.MATRIX sourceProductType;

    private final static double PI4 = Math.PI / 4.0;
    private final static double PI2 = Math.PI / 2.0;

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            sourceProductType = PolBandUtils.getSourceProductType(sourceProduct);

            srcBandList = PolBandUtils.getSourceBands(sourceProduct, sourceProductType);

            createTargetProduct();

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

        AbstractMetadata.getAbstractedMetadata(targetProduct).setAttributeInt(AbstractMetadata.polsarData, 1);
    }

    /**
     * Add bands to the target product.
     *
     * @throws OperatorException The exception.
     */
    private void addSelectedBands() throws OperatorException {

        final List<String> targetBandNameList = new ArrayList<>(10);
        targetBandNameList.add("T11");
        targetBandNameList.add("T12_real");
        targetBandNameList.add("T12_imag");
        targetBandNameList.add("T13_real");
        targetBandNameList.add("T13_imag");
        targetBandNameList.add("T22");
        targetBandNameList.add("T23_real");
        targetBandNameList.add("T23_imag");
        targetBandNameList.add("T33");
        //targetBandNameList.add("Ori_Ang");

        final String[] bandNames = targetBandNameList.toArray(new String[targetBandNameList.size()]);
        for (PolBandUtils.QuadSourceBand bandList : srcBandList) {
            final Band[] targetBands = OperatorUtils.addBands(targetProduct, bandNames, bandList.suffix);
            bandList.addTargetBands(targetBands);
        }
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

            final int x0 = targetRectangle.x;
            final int y0 = targetRectangle.y;
            final int w = targetRectangle.width;
            final int h = targetRectangle.height;
            final int maxY = y0 + h;
            final int maxX = x0 + w;
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final double[][] C3r = new double[3][3];
            final double[][] C3i = new double[3][3];
            final double[][] C4r = new double[4][4];
            final double[][] C4i = new double[4][4];
            final double[][] T3r = new double[3][3];
            final double[][] T3i = new double[3][3];
            final double[][] T4r = new double[4][4];
            final double[][] T4i = new double[4][4];

            final TileIndex tgtIndex = new TileIndex(targetTiles.get(getTargetProduct().getBandAt(0)));
            for (final PolBandUtils.QuadSourceBand bandList : srcBandList) {

                final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
                final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
                for (int i = 0; i < bandList.srcBands.length; i++) {
                    sourceTiles[i] = getSourceTile(bandList.srcBands[i], targetRectangle);
                    dataBuffers[i] = sourceTiles[i].getDataBuffer();
                }
                final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

                final ProductData[] targetDataBuffers = new ProductData[9];
                for (final Band targetBand : bandList.targetBands) {
                    final String targetBandName = targetBand.getName();
                    final ProductData dataBuffer = targetTiles.get(targetBand).getDataBuffer();
                    if (PolBandUtils.isBandForMatrixElement(targetBandName, "11"))
                        targetDataBuffers[0] = dataBuffer;
                    else if (PolBandUtils.isBandForMatrixElement(targetBandName, "12_real"))
                        targetDataBuffers[1] = dataBuffer;
                    else if (PolBandUtils.isBandForMatrixElement(targetBandName, "12_imag"))
                        targetDataBuffers[2] = dataBuffer;
                    else if (PolBandUtils.isBandForMatrixElement(targetBandName, "13_real"))
                        targetDataBuffers[3] = dataBuffer;
                    else if (PolBandUtils.isBandForMatrixElement(targetBandName, "13_imag"))
                        targetDataBuffers[4] = dataBuffer;
                    else if (PolBandUtils.isBandForMatrixElement(targetBandName, "22"))
                        targetDataBuffers[5] = dataBuffer;
                    else if (PolBandUtils.isBandForMatrixElement(targetBandName, "23_real"))
                        targetDataBuffers[6] = dataBuffer;
                    else if (PolBandUtils.isBandForMatrixElement(targetBandName, "23_imag"))
                        targetDataBuffers[7] = dataBuffer;
                    else if (PolBandUtils.isBandForMatrixElement(targetBandName, "33"))
                        targetDataBuffers[8] = dataBuffer;
                }

                final double[][] Tr = new double[3][3];
                final double[][] Ti = new double[3][3];

                int srcIdx, tgtIdx;
                double theta, c, s, c2, s2, cs;
                for (int y = y0; y < maxY; ++y) {
                    srcIndex.calculateStride(y);
                    tgtIndex.calculateStride(y);

                    for (int x = x0; x < maxX; ++x) {
                        srcIdx = srcIndex.getIndex(x);
                        tgtIdx = tgtIndex.getIndex(x);

                        if (sourceProductType == PolBandUtils.MATRIX.FULL) {

                            PolOpUtils.getT3(srcIdx, sourceProductType, dataBuffers, T3r, T3i);

                        } else if (sourceProductType == PolBandUtils.MATRIX.T3) {

                            PolOpUtils.getCoherencyMatrixT3(srcIdx, dataBuffers, T3r, T3i);

                        } else if (sourceProductType == PolBandUtils.MATRIX.T4) {

                            PolOpUtils.getCoherencyMatrixT4(srcIdx, dataBuffers, T4r, T4i);
                            PolOpUtils.t4ToT3(T4r, T4i, T3r, T3i);

                        } else if (sourceProductType == PolBandUtils.MATRIX.C3) {

                            PolOpUtils.getCovarianceMatrixC3(srcIdx, dataBuffers, C3r, C3i);
                            PolOpUtils.c3ToT3(C3r, C3i, T3r, T3i);

                        } else if (sourceProductType == PolBandUtils.MATRIX.C4) {

                            PolOpUtils.getCovarianceMatrixC4(srcIdx, dataBuffers, C4r, C4i);
                            PolOpUtils.c4ToT4(C4r, C4i, T4r, T4i);
                            PolOpUtils.t4ToT3(T4r, T4i, T3r, T3i);
                        }

                        theta = estimateOrientationAngle(T3r[1][2], T3r[1][1], T3r[2][2]);
                        c = FastMath.cos(2 * theta);
                        s = FastMath.sin(2 * theta);
                        c2 = c * c;
                        s2 = s * s;
                        cs = c * s;

                        Tr[0][0] = T3r[0][0];
                        Tr[0][1] = T3r[0][1] * c - T3r[0][2] * s;
                        Ti[0][1] = T3i[0][1] * c - T3i[0][2] * s;
                        Tr[0][2] = T3r[0][1] * s + T3r[0][2] * c;
                        Ti[0][2] = T3i[0][1] * s + T3i[0][2] * c;
                        Tr[1][1] = T3r[1][1] * c2 + T3r[2][2] * s2 - 2 * T3r[1][2] * cs;
                        Tr[1][2] = T3r[1][2] * (c2 - s2) + (T3r[1][1] - T3r[2][2]) * cs;
                        Ti[1][2] = T3i[1][2];
                        Tr[2][2] = T3r[1][1] * s2 + T3r[2][2] * c2 + 2 * T3r[1][2] * cs;

                        saveT3(Tr, Ti, tgtIdx, targetDataBuffers);
                    }
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Compute polarization orientation angle.
     *
     * @param t23Re Real part of the t23 element of coherency matrix
     * @param t22   The t22 element of the coherency matrix
     * @param t33   The t33 element of the coherency matrix
     * @return The polarization orientation angle in radian
     */
    private static double estimateOrientationAngle(final double t23Re, final double t22, final double t33) {
        if (t33 == 0.0) {
            return 0.0;
        }

        double theta = 0.25 * (Math.atan2(2 * t23Re, t33 - t22) + Math.PI);
        if (theta > PI4) {
            theta -= PI2;
        }
        return theta;
    }

    private static void saveT3(final double[][] Tr, final double[][] Ti,
                               final int idx, final ProductData[] targetDataBuffers) {

        targetDataBuffers[0].setElemFloatAt(idx, (float) Tr[0][0]); // T11
        targetDataBuffers[1].setElemFloatAt(idx, (float) Tr[0][1]); // T12_real
        targetDataBuffers[2].setElemFloatAt(idx, (float) Ti[0][1]); // T12_imag
        targetDataBuffers[3].setElemFloatAt(idx, (float) Tr[0][2]); // T13_real
        targetDataBuffers[4].setElemFloatAt(idx, (float) Ti[0][2]); // T13_imag
        targetDataBuffers[5].setElemFloatAt(idx, (float) Tr[1][1]); // T22
        targetDataBuffers[6].setElemFloatAt(idx, (float) Tr[1][2]); // T23_real
        targetDataBuffers[7].setElemFloatAt(idx, (float) Ti[1][2]); // T23_imag
        targetDataBuffers[8].setElemFloatAt(idx, (float) Tr[2][2]); // T33
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
            super(OrientationAngleCorrectionOp.class);
        }
    }
}