package org.csa.rstb.gpf;

import Jama.Matrix;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Tile;
import org.esa.nest.dataio.PolBandUtils;
import org.esa.snap.gpf.TileIndex;

/**
 * mean covariance matrix
 */
public class MeanCovariance {
    private final double[][] tempCr, tempCi;
    private final Matrix CrMat, CiMat;

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
        this.CrMat = new Matrix(3, 3);
        this.CiMat = new Matrix(3, 3);
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
                        CrMat.plusEquals(new Matrix(tempCr));
                        CiMat.plusEquals(new Matrix(tempCi));
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
                        CrMat.plusEquals(new Matrix(tempCr));
                        CiMat.plusEquals(new Matrix(tempCi));
                    }
                }

            } else if (sourceProductType == PolBandUtils.MATRIX.FULL) {
                final double[][] tempSr = new double[2][2];
                final double[][] tempSi = new double[2][2];

                for (int yy = ySt; yy <= yEd; ++yy) {
                    srcIndex.calculateStride(yy);
                    for (int xx = xSt; xx <= xEd; ++xx) {
                        PolOpUtils.getComplexScatterMatrix(srcIndex.getIndex(xx), dataBuffers, tempSr, tempSi);
                        PolOpUtils.computeCovarianceMatrixC3(tempSr, tempSi, tempCr, tempCi);
                        CrMat.plusEquals(new Matrix(tempCr));
                        CiMat.plusEquals(new Matrix(tempCi));
                    }
                }
            }

            CrMat.timesEquals(1.0 / num);
            CiMat.timesEquals(1.0 / num);
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
