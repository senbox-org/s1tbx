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
package org.csa.rstb.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math.util.FastMath;
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
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.PolBandUtils;
import org.esa.nest.gpf.TileIndex;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Perform polarization orientation angle correction for given coherency matrix
 */

@OperatorMetadata(alias="Orientation-Angle-Correction",
                  category = "Polarimetric Tools",
                  description="Perform polarization orientation angle correction for given coherency matrix")
public final class OrientationAngleCorrectionOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    private PolBandUtils.QuadSourceBand[] srcBandList;
    private PolBandUtils.MATRIX sourceProductType;

    private final static double PI4 = Math.PI/4.0;
    private final static double PI2 = Math.PI/2.0;

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
            sourceProductType = PolBandUtils.getSourceProductType(sourceProduct);

            srcBandList = PolBandUtils.getSourceBands(sourceProduct, sourceProductType);

            createTargetProduct();

        } catch(Throwable e) {
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

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        AbstractMetadata.getAbstractedMetadata(targetProduct).setAttributeInt(AbstractMetadata.polsarData, 1);
    }

    /**
     * Add bands to the target product.
     * @throws OperatorException The exception.
     */
    private void addSelectedBands() throws OperatorException {

        final List<String> targetBandNameList = new ArrayList<String>(10);
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
        for(PolBandUtils.QuadSourceBand bandList : srcBandList) {
            final Band[] targetBands = PolBandUtils.addBands(targetProduct, bandNames, bandList.suffix);
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
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        try {

            final int x0 = targetRectangle.x;
            final int y0 = targetRectangle.y;
            final int w  = targetRectangle.width;
            final int h  = targetRectangle.height;
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

            for(final PolBandUtils.QuadSourceBand bandList : srcBandList) {

                final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
                final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
                for (int i = 0; i < bandList.srcBands.length; i++) {
                    sourceTiles[i] = getSourceTile(bandList.srcBands[i], targetRectangle);
                    dataBuffers[i] = sourceTiles[i].getDataBuffer();
                }
                final TileIndex trgIndex = new TileIndex(sourceTiles[0]);

                double theta, t11, t12Re, t12Im, t13Re, t13Im, t22, t23Re, t23Im, t33, c, s, c2, s2, s4, cs;
                for(int y = y0; y < maxY; ++y) {
                    trgIndex.calculateStride(y);
                    for(int x = x0; x < maxX; ++x) {

                        final int idx = trgIndex.getIndex(x);

                        if (sourceProductType == PolBandUtils.MATRIX.T3) {

                            PolOpUtils.getCoherencyMatrixT3(idx, dataBuffers, T3r, T3i);

                        } else if (sourceProductType == PolBandUtils.MATRIX.T4) {

                            PolOpUtils.getCoherencyMatrixT4(idx, dataBuffers, T4r, T4i);
                            PolOpUtils.t4ToT3(T4r, T4i, T3r, T3i);

                        } else if (sourceProductType == PolBandUtils.MATRIX.C3) {

                            PolOpUtils.getCovarianceMatrixC3(idx, dataBuffers, C3r, C3i);
                            PolOpUtils.c3ToT3(C3r, C3i, T3r, T3i);

                        } else if (sourceProductType == PolBandUtils.MATRIX.C4) {

                            PolOpUtils.getCovarianceMatrixC4(idx, dataBuffers, C4r, C4i);
                            PolOpUtils.c4ToT4(C4r, C4i, T4r, T4i);
                            PolOpUtils.t4ToT3(T4r, T4i, T3r, T3i);
                        }

                        theta = estimateOrientationAngle(T3r[1][2], T3r[1][1], T3r[2][2]);
                        c = FastMath.cos(2*theta);
                        s = FastMath.sin(2*theta);
                        c2 = c*c;
                        s2 = s*s;
                        cs = c*s;

                        t11 = T3r[0][0];
                        t12Re = T3r[0][1]*c - T3r[0][2]*s;
                        t12Im = T3i[0][1]*c - T3i[0][2]*s;
                        t13Re = T3r[0][1]*s + T3r[0][2]*c;
                        t13Im = T3i[0][1]*s + T3i[0][2]*c;
                        t22 = T3r[1][1]*c2 + T3r[2][2]*s2 - 2*T3r[1][2]*cs;
                        t23Re = T3r[1][2]*(c2 - s2) + (T3r[1][1] - T3r[2][2])*cs;
                        t23Im = T3i[1][2];
                        t33 = T3r[1][1]*s2 + T3r[2][2]*c2 + 2*T3r[1][2]*cs;

                        for (Band targetBand : bandList.targetBands){
                            final String targetBandName = targetBand.getName();
                            final Tile targetTile = targetTiles.get(targetBand);

                            if (targetBandName.contains("T11")) {
                                targetTile.getDataBuffer().setElemFloatAt(idx, (float)t11);
                            } else if (targetBandName.contains("T12_real")) {
                                targetTile.getDataBuffer().setElemFloatAt(idx, (float)t12Re);
                            } else if (targetBandName.contains("T12_imag")) {
                                targetTile.getDataBuffer().setElemFloatAt(idx, (float)t12Im);
                            } else if (targetBandName.contains("T13_real")) {
                                targetTile.getDataBuffer().setElemFloatAt(idx, (float)t13Re);
                            } else if (targetBandName.contains("T13_imag")) {
                                targetTile.getDataBuffer().setElemFloatAt(idx, (float)t13Im);
                            } else if (targetBandName.contains("T22")) {
                                targetTile.getDataBuffer().setElemFloatAt(idx, (float)t22);
                            } else if (targetBandName.contains("T23_real")) {
                                targetTile.getDataBuffer().setElemFloatAt(idx, (float)t23Re);
                            } else if (targetBandName.contains("T23_imag")) {
                                targetTile.getDataBuffer().setElemFloatAt(idx, (float)t23Im);
                            } else if (targetBandName.contains("T33")) {
                                targetTile.getDataBuffer().setElemFloatAt(idx, (float)t33);
                            }
                            /*
                            else if (targetBandName.equals("Ori_Ang")) {
                                targetTile.getDataBuffer().setElemFloatAt(idx, (float)(theta*180/Math.PI));
                            }
                            */
                        }
                    }
                }
            }
        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Compute polarization orientation angle.
     * @param t23Re Real part of the t23 element of coherency matrix
     * @param t22 The t22 element of the coherency matrix
     * @param t33 The t33 element of the coherency matrix
     * @return The polarization orientation angle in radian
     */
    private static double estimateOrientationAngle(final double t23Re, final double t22, final double t33) {
        if (t33 == 0.0) {
            return 0.0;
        }

        double theta = 0.25*(Math.atan2(2*t23Re, t33 - t22) + Math.PI);
        if (theta > PI4) {
            theta -= PI2;
        }
        return theta;
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(OrientationAngleCorrectionOp.class);
        }
    }
}