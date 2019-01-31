/*
 * Copyright (C) 2019 by SkyWatch Space Applications http://www.skywatch.co
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
package org.csa.rstb.polarimetric.gpf.specklefilters;

import org.csa.rstb.polarimetric.gpf.DualPolProcessor;
import org.csa.rstb.polarimetric.gpf.PolarimetricSpeckleFilterOp;
import org.csa.rstb.polarimetric.gpf.QuadPolProcessor;
import org.esa.s1tbx.commons.polsar.PolBandUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.util.Map;

/**
 * Non-Local Speckle Filter for SAR/InSAR/PolInSAR data
 */
public class NonLocal implements SpeckleFilter, DualPolProcessor, QuadPolProcessor {

    private final PolarimetricSpeckleFilterOp operator;
    private final Product sourceProduct;
    private final Product targetProduct;
    private final PolBandUtils.MATRIX sourceProductType;
    private final PolBandUtils.PolSourceBand[] srcBandList;
    private final int numLooks;
    private final int windowSize, halfWindowSize;
	private final int patchSize, halfPatchSize;
	private final int scaleSize;
    private final int sourceImageWidth;
    private final int sourceImageHeight;
    private final int matrixSize; // D
    private final double gamma;

    private final static double TwoLog2 = 1.386294361119890572453527965990;

    public NonLocal(final PolarimetricSpeckleFilterOp op, final Product srcProduct, final Product trgProduct,
                    final PolBandUtils.MATRIX sourceProductType, final PolBandUtils.PolSourceBand[] srcBandList,
                    final int numLooks, final int searchWindowSize, final int patchSize, final int scaleSize) {
        this.operator = op;
        this.sourceProduct = srcProduct;
        this.targetProduct = trgProduct;
        this.sourceProductType = sourceProductType;
        this.srcBandList = srcBandList;
		this.numLooks = numLooks;
        this.windowSize = searchWindowSize;
        this.halfWindowSize = searchWindowSize / 2;
		this.patchSize = patchSize;
		this.halfPatchSize = patchSize / 2;
		this.scaleSize = scaleSize;
		
        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
		
        if (patchSize >= windowSize) {
            throw new OperatorException("Patch size should always be smaller than the search window size");
        }

        if (windowSize >= sourceImageWidth || windowSize >= sourceImageHeight) {
            throw new OperatorException("Image is too small. Please select larger image");
        }

        if (sourceProductType == PolBandUtils.MATRIX.C3) {
            matrixSize = 3;
        } else if (sourceProductType == PolBandUtils.MATRIX.C2) {
            matrixSize = 2;
        } else {
            throw new OperatorException("Expecting a C2 or C3 matrix");
        }

		if (numLooks >= matrixSize) {
            throw new OperatorException("Number of looks should always be smaller than the covariance matrix size");
        }

        gamma = Math.min((double)numLooks / (double)matrixSize, 1.0);
    }


    public void computeTiles(Map<Band, Tile> targetTiles, Rectangle targetRectangle, final Rectangle sourceRectangle) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int xMax = x0 + w;
        final int yMax = y0 + h;

        final int sx0 = sourceRectangle.x;
        final int sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;
        final int sxMax = sx0 + sw;
        final int syMax = sy0 + sh;

        final TileIndex trgIndex = new TileIndex(targetTiles.get(targetProduct.getBandAt(0)));

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {

            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
            for (int i = 0; i < bandList.srcBands.length; ++i) {
                sourceTiles[i] = operator.getSourceTile(bandList.srcBands[i], sourceRectangle);
                dataBuffers[i] = sourceTiles[i].getDataBuffer();
            }

            final ProductData[] targetDataBuffers = getTargetDataBuffers(bandList, targetTiles);

            final CovarianceMatrix[][] originalMatrix = new CovarianceMatrix[sh][sw];
            getOriginalCovarianceMatrix(sx0, sy0, sxMax, syMax, sourceTiles, dataBuffers, originalMatrix);

            final CovarianceMatrix[][] preEstimatedMatrix = new CovarianceMatrix[sh][sw];
            computePreEstimatedCovarianceMatrix(sx0, sy0, sxMax, syMax, originalMatrix, preEstimatedMatrix);

            System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", computeWeightedEstimate start");
            if (matrixSize == 3) {
                for (int y = y0; y < yMax; ++y) {
                    trgIndex.calculateStride(y);
                    for (int x = x0; x < xMax; ++x) {
                        final double[][] weight = computeWeights(x, y, sx0, sy0, preEstimatedMatrix);
                        CovarianceMatrix sigmaNL = computeWeightedEstimate(x, y, sx0, sy0, weight, originalMatrix);
                        final double enlNL = computeENL(weight);
                        final CovarianceMatrix sigmaNLBR = new CovarianceMatrix(matrixSize);
                        final double enlNLRB = performBiasReduction(
                                x, y, sx0, sy0, weight, enlNL, originalMatrix, sigmaNL, sigmaNLBR);

                        saveC3(sigmaNLBR, trgIndex.getIndex(x), targetDataBuffers);
                    }
                }

            } else if (matrixSize == 2) {
                for (int y = y0; y < yMax; ++y) {
                    trgIndex.calculateStride(y);
                    for (int x = x0; x < xMax; ++x) {
                        final double[][] weight = computeWeights(x, y, sx0, sy0, preEstimatedMatrix);
                        CovarianceMatrix sigmaNL = computeWeightedEstimate(x, y, sx0, sy0, weight, originalMatrix);
                        final double enlNL = computeENL(weight);
                        final CovarianceMatrix sigmaNLBR = new CovarianceMatrix(matrixSize);
                        final double enlNLRB = performBiasReduction(
                                x, y, sx0, sy0, weight, enlNL, originalMatrix, sigmaNL, sigmaNLBR);
                        saveC2(sigmaNLBR, trgIndex.getIndex(x), targetDataBuffers);
                    }
                }
            }
            System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", computeWeightedEstimate finished");
        }
    }

    private ProductData[] getTargetDataBuffers(final PolBandUtils.PolSourceBand bandList, Map<Band, Tile> targetTiles) {

        if (matrixSize == 3) {
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
            return targetDataBuffers;

        } else if (matrixSize == 2) {

            final ProductData[] targetDataBuffers = new ProductData[4];
            for (final Band targetBand : bandList.targetBands) {
                final String targetBandName = targetBand.getName();
                final ProductData dataBuffer = targetTiles.get(targetBand).getDataBuffer();
                if (PolBandUtils.isBandForMatrixElement(targetBandName, "11"))
                    targetDataBuffers[0] = dataBuffer;
                else if (PolBandUtils.isBandForMatrixElement(targetBandName, "12_real"))
                    targetDataBuffers[1] = dataBuffer;
                else if (PolBandUtils.isBandForMatrixElement(targetBandName, "12_imag"))
                    targetDataBuffers[2] = dataBuffer;
                else if (PolBandUtils.isBandForMatrixElement(targetBandName, "22"))
                    targetDataBuffers[3] = dataBuffer;
            }
            return targetDataBuffers;

        } else {
            return null;
        }
    }

    private static void saveC3(final CovarianceMatrix sigmaNLRB, final int idx, final ProductData[] targetDataBuffers) {

        final double[][] Cr = sigmaNLRB.getRealCovarianceMatrix();
        final double[][] Ci = sigmaNLRB.getImagCovarianceMatrix();
        targetDataBuffers[0].setElemFloatAt(idx, (float) Cr[0][0]); // C11
        targetDataBuffers[1].setElemFloatAt(idx, (float) Cr[0][1]); // C12_real
        targetDataBuffers[2].setElemFloatAt(idx, (float) Ci[0][1]); // C12_imag
        targetDataBuffers[3].setElemFloatAt(idx, (float) Cr[0][2]); // C13_real
        targetDataBuffers[4].setElemFloatAt(idx, (float) Ci[0][2]); // C13_imag
        targetDataBuffers[5].setElemFloatAt(idx, (float) Cr[1][1]); // C22
        targetDataBuffers[6].setElemFloatAt(idx, (float) Cr[1][2]); // C23_real
        targetDataBuffers[7].setElemFloatAt(idx, (float) Ci[1][2]); // C23_imag
        targetDataBuffers[8].setElemFloatAt(idx, (float) Cr[2][2]); // C33
    }

    private static void saveC2(final CovarianceMatrix sigmaNLRB, final int idx, final ProductData[] targetDataBuffers) {

        final double[][] Cr = sigmaNLRB.getRealCovarianceMatrix();
        final double[][] Ci = sigmaNLRB.getImagCovarianceMatrix();
        targetDataBuffers[0].setElemFloatAt(idx, (float) Cr[0][0]); // C11
        targetDataBuffers[1].setElemFloatAt(idx, (float) Cr[0][1]); // C12_real
        targetDataBuffers[2].setElemFloatAt(idx, (float) Ci[0][1]); // C12_imag
        targetDataBuffers[3].setElemFloatAt(idx, (float) Cr[1][1]); // C22
    }

    private void computePreEstimatedCovarianceMatrix(
            final int sx0, final int sy0, final int sxMax, final int syMax, final CovarianceMatrix[][] originalMatrix,
            final CovarianceMatrix[][] preEstimatedMatrix) {

        if (scaleSize > 0) {
            performGaussianFiltering(sx0, sy0, sxMax, syMax, originalMatrix, preEstimatedMatrix);
        } else {
            copyOriginalMatrix(sx0, sy0, sxMax, syMax, originalMatrix, preEstimatedMatrix);
        }

        rescaleCovarianceMatrix(sx0, sy0, sxMax, syMax, preEstimatedMatrix);
    }

    private void getOriginalCovarianceMatrix(final int sx0, final int sy0, final int sxMax, final int syMax,
                                             final Tile[] sourceTiles, final ProductData[] dataBuffers,
                                             final CovarianceMatrix[][] originalMatrix) {

        final TileIndex srcIndex = new TileIndex(sourceTiles[0]);
        final double[][] Cr = new double[matrixSize][matrixSize];
        final double[][] Ci = new double[matrixSize][matrixSize];

        for (int y = sy0; y < syMax; ++y) {
            final int yy = y - sy0;
            srcIndex.calculateStride(y);
            for (int x = sx0; x < sxMax; ++x) {
                final int xx = x - sx0;
                if (matrixSize == 3) {
                    getCovarianceMatrixC3(srcIndex.getIndex(x), dataBuffers, Cr, Ci);
                } else if (matrixSize == 2) {
                    getCovarianceMatrixC2(srcIndex.getIndex(x), dataBuffers, Cr, Ci);
                }
                originalMatrix[yy][xx] = new CovarianceMatrix(matrixSize);
                originalMatrix[yy][xx].setCovarianceMatrix(Cr, Ci);
            }
        }
    }

    private void copyOriginalMatrix(final int sx0, final int sy0, final int sxMax, final int syMax,
                                    final CovarianceMatrix[][] originalMatrix,
                                    final CovarianceMatrix[][] preEstimatedMatrix) {
        for (int y = sy0; y < syMax; ++y) {
            final int yy = y - sy0;
            for (int x = sx0; x < sxMax; ++x) {
                final int xx = x - sx0;
                preEstimatedMatrix[yy][xx] = new CovarianceMatrix(matrixSize);
                preEstimatedMatrix[yy][xx].setCovarianceMatrix(originalMatrix[yy][xx].getRealCovarianceMatrix(),
                        originalMatrix[yy][xx].getImagCovarianceMatrix());
            }
        }
    }

    private void performGaussianFiltering(final int sx0, final int sy0, final int sxMax, final int syMax,
                                          final CovarianceMatrix[][] originalMatrix,
                                          final CovarianceMatrix[][] preEstimatedMatrix) {

        double[][] weight = new double[2*scaleSize+1][2*scaleSize+1];
        double totalWeight = 0.0;
        for (int i = -scaleSize; i <= scaleSize; ++i) {
            for (int j = -scaleSize; j <= scaleSize; ++j) {
                final double w = Math.exp(-Math.PI*(i*i + j*j)/((scaleSize + 0.5)*(scaleSize + 0.5)));
                weight[i + scaleSize][j + scaleSize] = w;
                totalWeight += w;
            }
        }

        for (int i = -scaleSize; i <= scaleSize; ++i) {
            for (int j = -scaleSize; j <= scaleSize; ++j) {
                weight[i + scaleSize][j + scaleSize] /= totalWeight;
            }
        }

        final int sw = sxMax - sx0;
        final int sh = syMax - sy0;
        for (int y = sy0; y < syMax; ++y) {
            final int yy = y - sy0;
            for (int x = sx0; x < sxMax; ++x) {
                final int xx = x - sx0;
                preEstimatedMatrix[yy][xx] = new CovarianceMatrix(matrixSize);

                for (int i = -scaleSize; i <= scaleSize; ++i) {
                    final int dyy = yy + i;
                    if (dyy < 0 || dyy > sh - 1) {
                        continue;
                    }
                    final int ii = i + scaleSize;

                    for (int j = -scaleSize; j <= scaleSize; ++j) {
                        final int dxx = xx + j;
                        if (dxx < 0 || dxx > sw - 1) {
                            continue;
                        }

                        preEstimatedMatrix[yy][xx].addWeightedCovarianceMatrix(
                                weight[ii][j + scaleSize],
                                originalMatrix[dyy][dxx].getRealCovarianceMatrix(),
                                originalMatrix[dyy][dxx].getImagCovarianceMatrix());
                    }
                }
            }
        }
    }

    private void rescaleCovarianceMatrix(final int sx0, final int sy0, final int sxMax, final int syMax,
                                         final CovarianceMatrix[][] preEstimatedMatrix) {

        for (int y = sy0; y < syMax; ++y) {
            final CovarianceMatrix[] matrix = preEstimatedMatrix[y - sy0];
            for (int x = sx0; x < sxMax; ++x) {
                matrix[x - sx0].rescaleMatrix(gamma);
            }
        }
    }

    private double[][] computeWeights(final int xc, final int yc, final int sx0, final int sy0,
                                      final CovarianceMatrix[][] preEstimatedMatrix) {

        final int xSt = Math.max(xc - halfWindowSize, 0);
        final int ySt = Math.max(yc - halfWindowSize, 0);
        final int xEd = Math.min(xc + halfWindowSize, sourceImageWidth - 1);
        final int yEd = Math.min(yc + halfWindowSize, sourceImageHeight - 1);

        // No normalization is needed. We want w(x,x) = 1
        final double h = 1.0 / 3.0; // filtering parameter
        double[][] weight = new double[yEd - ySt + 1][xEd - xSt + 1];
        for (int y = ySt; y <= yEd; ++y) {
            final int yy = y - ySt;
            for (int x = xSt; x <= xEd; ++x) {
                final int xx = x - xSt;
                final double delta = computeDissimilarity(xc, yc, x, y, sx0, sy0, preEstimatedMatrix);
                if (delta < 0.0) {
                    weight[yy][xx] = 0.0;
                } else {
                    weight[yy][xx] = Math.exp(-delta / h);
                }
            }
        }

        return weight;
    }

    private double computeDissimilarity(
            final int xc1, final int yc1, final int xc2, final int yc2, final int sx0, final int sy0,
            final CovarianceMatrix[][] preEstimatedMatrix) {

        final double matrixSizeTwoLog2 = matrixSize * TwoLog2;
        double dissimilarity = 0.0;
        boolean validPixel = false;
        for (int i = 0; i < patchSize; ++i) {
            final int y1 = yc1 - halfPatchSize + i;
            final int y2 = yc2 - halfPatchSize + i;
            if (y1 < 0 || y1 >= sourceImageHeight || y2 < 0 || y2 >= sourceImageHeight) {
                continue;
            }
            final int dy1 = y1 - sy0;
            final int dy2 = y2 - sy0;

            for (int j = 0; j < patchSize; ++j) {
                final int x1 = xc1 - halfPatchSize + j;
                final int x2 = xc2 - halfPatchSize + j;
                if (x1 < 0 || x1 >= sourceImageWidth || x2 < 0 || x2 >= sourceImageWidth) {
                    continue;
                }
                final int dx1 = x1 - sx0;
                final int dx2 = x2 - sx0;

                final CovarianceMatrix C12 = new CovarianceMatrix(matrixSize);

                C12.setCovarianceMatrix(preEstimatedMatrix[dy1][dx1].getRealCovarianceMatrix(),
                        preEstimatedMatrix[dy1][dx1].getImagCovarianceMatrix());

                C12.addCovarianceMatrix(preEstimatedMatrix[dy2][dx2].getRealCovarianceMatrix(),
                        preEstimatedMatrix[dy2][dx2].getImagCovarianceMatrix());

                final double detC12 = C12.getDeterminant();
                final double detC1 = preEstimatedMatrix[dy1][dx1].getDeterminant();
                final double detC2 = preEstimatedMatrix[dy2][dx2].getDeterminant();

                if (detC12*detC1*detC2 <= 0.0) {
                    continue;
                }

                dissimilarity += -Math.log(detC1*detC2/(detC12*detC12)) - matrixSizeTwoLog2;
                validPixel = true;
            }
        }

        if (validPixel) {
            return dissimilarity;
        } else {
            return -1.0;
        }
    }

    private CovarianceMatrix computeWeightedEstimate(
            final int xc, final int yc, final int sx0, final int sy0, final double[][] weight,
            final CovarianceMatrix[][] originalMatrix) {

        final int xSt = Math.max(xc - halfWindowSize, 0);
        final int ySt = Math.max(yc - halfWindowSize, 0);
        final int xEd = Math.min(xc + halfWindowSize, sourceImageWidth - 1);
        final int yEd = Math.min(yc + halfWindowSize, sourceImageHeight - 1);

        final CovarianceMatrix avgC = new CovarianceMatrix(matrixSize);
        for (int y = ySt; y <= yEd; ++y) {
            final int yy = y - ySt;
            final int i = y - sy0;
            for (int x = xSt; x <= xEd; ++x) {
                final int j = x - sx0;
                avgC.addWeightedCovarianceMatrix(
                        weight[yy][x - xSt],
                        originalMatrix[i][j].getRealCovarianceMatrix(),
                        originalMatrix[i][j].getImagCovarianceMatrix());
            }
        }

        return avgC;
    }

    private double computeENL(final double[][] weight) {

        final int cols = weight[0].length;

        double sum = 0.0;
        double sum2 = 0.0;
        for (double[] aWeight : weight) {
            for (int c = 0; c < cols; ++c) {
                sum += aWeight[c];
                sum2 += aWeight[c] * aWeight[c];
            }
        }

        return sum * sum / sum2;
    }

    private double performBiasReduction(
            final int xc, final int yc, final int sx0, final int sy0, final double[][] weight, final double enlNL,
            final CovarianceMatrix[][] originalMatrix, final CovarianceMatrix sigmaNL, CovarianceMatrix sigmaNLBR) {

        final int xSt = Math.max(xc - halfWindowSize, 0);
        final int ySt = Math.max(yc - halfWindowSize, 0);
        final int xEd = Math.min(xc + halfWindowSize, sourceImageWidth - 1);
        final int yEd = Math.min(yc + halfWindowSize, sourceImageHeight - 1);

        // compute weighted variance
        final double[] diagNL = sigmaNL.getDiagonalElements();
        final double[] varNL = new double[matrixSize];
        for (int j = 0; j < matrixSize; ++j) {
            varNL[j] = -diagNL[j] * diagNL[j];
        }

        for (int y = ySt; y <= yEd; ++y) {
            final int yy = y - ySt;
            final int i = y - sy0;
            for (int x = xSt; x <= xEd; ++x) {
                final int xx = x - xSt;
                final double[] diagOri = originalMatrix[i][x - sx0].getDiagonalElements();
                for (int j = 0; j < matrixSize; ++j) {
                    varNL[j] += weight[yy][xx] * diagOri[j] * diagOri[j];
                }
            }
        }

        // compute alpha
        double alpha = 0.0;
        for (int j = 0; j < matrixSize; ++j) {
            alpha = Math.max(alpha, 1.0 - diagNL[j]*diagNL[j] / (varNL[j]*numLooks));
        }

        // bias reduction
        sigmaNLBR.addWeightedCovarianceMatrix(
                1 - alpha, sigmaNL.getRealCovarianceMatrix(), sigmaNL.getImagCovarianceMatrix());

        sigmaNLBR.addWeightedCovarianceMatrix(alpha, originalMatrix[yc - sy0][xc - sx0].getRealCovarianceMatrix(),
                originalMatrix[yc - sy0][xc - sx0].getImagCovarianceMatrix());

        // compute ENL after bias reduction
        final double totalWeight = getTotalWeight(weight);
        return enlNL / ((1-alpha)*(1-alpha) + enlNL*(alpha*alpha + (2.0*alpha*(1-alpha))/totalWeight));
    }

    private static double getTotalWeight(final double[][] weight) {

        final int cols = weight[0].length;

        double totalWeight = 0.0;
        for (double[] aWeight : weight) {
            for (int c = 0; c < cols; ++c) {
                totalWeight += aWeight[c];
            }
        }

        return totalWeight;
    }


    private class CovarianceMatrix {
        private double Cr00, Cr01, Cr02, Cr11, Cr12, Cr22;
        private double Ci01, Ci02, Ci12;
        private int d;
        private double det;

        private CovarianceMatrix(final  int matrixSize) {
            this.d = matrixSize;
            this.det = -1.0;

            if (d == 2) {
                Cr00 = 0.0;
                Cr01 = 0.0;
                Cr11 = 0.0;
                Ci01 = 0.0;
            } else if (d == 3) {
                Cr00 = 0.0;
                Cr01 = 0.0;
                Cr02 = 0.0;
                Cr11 = 0.0;
                Cr12 = 0.0;
                Cr22 = 0.0;
                Ci01 = 0.0;
                Ci02 = 0.0;
                Ci12 = 0.0;
            }
        }

        private void setCovarianceMatrix(final double[][] Cr, final double[][] Ci) {

            if (Cr.length != d || Cr[0].length != d || Ci.length != d || Ci[0].length != d) {
                throw new OperatorException("CovarianceMatrix: invalid input matrix dimension");
            }

            if (d == 2) {
                Cr00 = Cr[0][0];
                Cr01 = Cr[0][1];
                Cr11 = Cr[1][1];
                Ci01 = Ci[0][1];
            } else if (d == 3) {
                Cr00 = Cr[0][0];
                Cr01 = Cr[0][1];
                Cr02 = Cr[0][2];
                Cr11 = Cr[1][1];
                Cr12 = Cr[1][2];
                Cr22 = Cr[2][2];
                Ci01 = Ci[0][1];
                Ci02 = Ci[0][2];
                Ci12 = Ci[1][2];
            }

            det = -1.0;
        }

        private double[][] getRealCovarianceMatrix() {

            final double[][] Cr = new double[d][d];
            if (d == 2) {
                Cr[0][0] = Cr00;
                Cr[0][1] = Cr01;
                Cr[1][0] = Cr01;
                Cr[1][1] = Cr11;
            } else if (d == 3) {
                Cr[0][0] = Cr00;
                Cr[0][1] = Cr01;
                Cr[0][2] = Cr02;
                Cr[1][0] = Cr01;
                Cr[1][1] = Cr11;
                Cr[1][2] = Cr12;
                Cr[2][0] = Cr02;
                Cr[2][1] = Cr12;
                Cr[2][2] = Cr22;
            }
            return Cr;
        }

        private double[][] getImagCovarianceMatrix() {

            final double[][] Ci = new double[d][d];
            if (d == 2) {
                Ci[0][0] = 0.0;
                Ci[0][1] = Ci01;
                Ci[1][0] = -Ci01;
                Ci[1][1] = 0.0;
            } else if (d == 3) {
                Ci[0][0] = 0.0;
                Ci[0][1] = Ci01;
                Ci[0][2] = Ci02;
                Ci[1][0] = -Ci01;
                Ci[1][1] = 0.0;
                Ci[1][2] = Ci12;
                Ci[2][0] = -Ci02;
                Ci[2][1] = -Ci12;
                Ci[2][2] = 0.0;
            }
            return Ci;
        }

        private void addCovarianceMatrix(final double[][] Cr, final double[][] Ci) {

            if (Cr.length != d || Cr[0].length != d || Ci.length != d || Ci[0].length != d) {
                throw new OperatorException("CovarianceMatrix: invalid input matrix dimension");
            }
            if (d == 2) {
                Cr00 += Cr[0][0];
                Cr01 += Cr[0][1];
                Cr11 += Cr[1][1];
                Ci01 += Ci[0][1];
            } else if (d == 3) {
                Cr00 += Cr[0][0];
                Cr01 += Cr[0][1];
                Cr02 += Cr[0][2];
                Cr11 += Cr[1][1];
                Cr12 += Cr[1][2];
                Cr22 += Cr[2][2];
                Ci01 += Ci[0][1];
                Ci02 += Ci[0][2];
                Ci12 += Ci[1][2];
            }

            det = -1.0;
        }

        private void addWeightedCovarianceMatrix(final double w, final double[][] Cr, final double[][] Ci) {

            if (Cr.length != d || Cr[0].length != d || Ci.length != d || Ci[0].length != d) {
                throw new OperatorException("CovarianceMatrix: invalid input matrix dimension");
            }
            if (d == 2) {
                Cr00 += w*Cr[0][0];
                Cr01 += w*Cr[0][1];
                Cr11 += w*Cr[1][1];
                Ci01 += w*Ci[0][1];
            } else if (d == 3) {
                Cr00 += w*Cr[0][0];
                Cr01 += w*Cr[0][1];
                Cr02 += w*Cr[0][2];
                Cr11 += w*Cr[1][1];
                Cr12 += w*Cr[1][2];
                Cr22 += w*Cr[2][2];
                Ci01 += w*Ci[0][1];
                Ci02 += w*Ci[0][2];
                Ci12 += w*Ci[1][2];
            }

            det = -1.0;
        }

        private void rescaleMatrix(final double gamma) {
            // apply to off diagonal elements only
            if (d == 2) {
                Cr01 *= gamma;
                Ci01 *= gamma;
            } else if (d == 3) {
                Cr01 *= gamma;
                Cr02 *= gamma;
                Cr12 *= gamma;
                Ci01 *= gamma;
                Ci02 *= gamma;
                Ci12 *= gamma;
            }

            det = -1.0;
        }

        private double getDeterminant() {

            if (det != -1.0) {
                return det;
            }

            if (d == 2) {
                det = Math.abs(Cr00*Cr11 - Cr01*Cr01 - Ci01*Ci01);
            } else if (d == 3) {
                det = Math.abs(Cr00*Cr11*Cr22 - Cr00*(Cr12*Cr12 + Ci12*Ci12) - Cr11*(Cr02*Cr02 + Ci02*Ci02) -
                        Cr22*(Cr01*Cr01 + Ci01*Ci01) + 2.0*(Cr12*(Cr01*Cr02 + Ci01*Ci02) + Ci12*(Cr01*Ci02 -
                        Ci01*Cr02)));
            }

            return det;
        }

        private double[] getDiagonalElements() {

            final double[] diagonal = new double[d];
            if (d == 2) {
                diagonal[0] = Cr00;
                diagonal[1] = Cr11;
            } else if (d == 3) {
                diagonal[0] = Cr00;
                diagonal[1] = Cr11;
                diagonal[2] = Cr22;
            }
            return diagonal;
        }
    }

}

