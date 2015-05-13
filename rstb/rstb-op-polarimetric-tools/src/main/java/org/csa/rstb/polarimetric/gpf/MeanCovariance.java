/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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

import org.esa.s1tbx.dataio.PolBandUtils;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.gpf.TileIndex;
import org.jblas.DoubleMatrix;

/**
 * mean covariance matrix
 */
public class MeanCovariance {
    private final double[][] tempCr, tempCi;
    private final DoubleMatrix CrMat, CiMat;

    private final PolBandUtils.MATRIX sourceProductType;
    private final Tile[] sourceTiles;
    private final ProductData[] dataBuffers;
    private final int halfWindowSizeX, halfWindowSizeY;
    private final TileIndex srcIndex;

    public MeanCovariance(final PolBandUtils.MATRIX sourceProductType, final Tile[] sourceTiles,
                          final ProductData[] dataBuffers, final int halfWindowSizeX, final int halfWindowSizeY) {

        this.sourceProductType = sourceProductType;
        this.sourceTiles = sourceTiles;
        this.dataBuffers = dataBuffers;
        this.halfWindowSizeX = halfWindowSizeX;
        this.halfWindowSizeY = halfWindowSizeY;

        srcIndex = new TileIndex(sourceTiles[0]);

        this.tempCr = new double[3][3];
        this.tempCi = new double[3][3];
        this.CrMat = new DoubleMatrix(3, 3);
        this.CiMat = new DoubleMatrix(3, 3);
    }

    public void getMeanCovarianceMatrix(final int x, final int y, final double[][] Cr, final double[][] Ci) {

        final int xSt = Math.max(x - halfWindowSizeX, sourceTiles[0].getMinX());
        final int xEd = Math.min(x + halfWindowSizeX, sourceTiles[0].getMaxX() - 1);
        final int ySt = Math.max(y - halfWindowSizeY, sourceTiles[0].getMinY());
        final int yEd = Math.min(y + halfWindowSizeY, sourceTiles[0].getMaxY() - 1);
        final int num = (yEd - ySt + 1) * (xEd - xSt + 1);

        if (sourceProductType == PolBandUtils.MATRIX.C3) {

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    PolOpUtils.getCovarianceMatrixC3(srcIndex.getIndex(xx), dataBuffers, tempCr, tempCi);
                    CrMat.add(new DoubleMatrix(tempCr));
                    CiMat.add(new DoubleMatrix(tempCi));
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.T3) {
            final double[][] tempTr = new double[3][3];
            final double[][] tempTi = new double[3][3];

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    PolOpUtils.getCoherencyMatrixT3(srcIndex.getIndex(xx), dataBuffers, tempTr, tempTi);
                    PolOpUtils.t3ToC3(tempTr, tempTi, tempCr, tempCi);
                    CrMat.add(new DoubleMatrix(tempCr));
                    CiMat.add(new DoubleMatrix(tempCi));
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.FULL) {

            DoubleMatrix matSr = new DoubleMatrix(3, 3);
            DoubleMatrix matSi= new DoubleMatrix(3, 3);
            DoubleMatrix matCr = new DoubleMatrix(3, 3);
            DoubleMatrix matCi= new DoubleMatrix(3, 3);
            CrMat.fill(0);
            CiMat.fill(0);

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    PolOpUtils.getComplexScatterMatrix(srcIndex.getIndex(xx), dataBuffers, matSr, matSi);
                    PolOpUtils.computeCovarianceMatrixC3(matSr, matSi, matCr, matCi);
                    CrMat.addi(matCr);
                    CiMat.addi(matCi);
                }
            }
        }

        CrMat.muli(1.0 / num);
        CiMat.muli(1.0 / num);

        for (int i = 0; i < 3; i++) {
            Cr[i][0] = CrMat.get(i, 0);
            Ci[i][0] = CiMat.get(i, 0);

            Cr[i][1] = CrMat.get(i, 1);
            Ci[i][1] = CiMat.get(i, 1);

            Cr[i][2] = CrMat.get(i, 2);
            Ci[i][2] = CiMat.get(i, 2);
        }
    }

}
