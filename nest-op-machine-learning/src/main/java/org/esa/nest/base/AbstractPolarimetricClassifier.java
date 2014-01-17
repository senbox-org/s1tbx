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
package org.esa.nest.base;

import com.google.common.base.Preconditions;
import java.awt.Rectangle;
import java.util.Map;
import org.apache.mahout.math.DenseMatrix;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.Vector;
import org.csa.rstb.gpf.PolOpUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.Tile;
import org.esa.nest.gpf.PolBandUtils;

/**
 * Polarimetric Classifiers
 */
public abstract class AbstractPolarimetricClassifier {

    public final static int NODATACLASS = 0;
    protected final PolBandUtils.MATRIX sourceProductType;
    protected final int srcWidth;
    protected final int srcHeight;
    protected final int windowSize;
    protected final int halfWindowSize;
    protected final Map<Band, PolBandUtils.QuadSourceBand> bandMap;

    public AbstractPolarimetricClassifier(final PolBandUtils.MATRIX srcProductType,
                                final int srcWidth, final int srcHeight, final int windowSize,
                                final Map<Band, PolBandUtils.QuadSourceBand> bandMap) {
        this.sourceProductType = srcProductType;
        this.srcWidth = srcWidth;
        this.srcHeight = srcHeight;
        this.windowSize = windowSize;
        this.halfWindowSize = windowSize/2;

        this.bandMap = bandMap;
    }

    public boolean canProcessStacks() {
        return true;
    }

    /**
     * returns the number of classes
     * @return num classes
     */
    public abstract int getNumClasses();

    /**
     * Get source tile rectangle.
     * @param tx0 X coordinate for the upper left corner pixel in the target tile.
     * @param ty0 Y coordinate for the upper left corner pixel in the target tile.
     * @param tw The target tile width.
     * @param th The target tile height.
     * @return The source tile rectangle.
     */
    public Rectangle getSourceRectangle(final int tx0, final int ty0, final int tw, final int th) {

        final int x0 = Math.max(0, tx0 - halfWindowSize);
        final int y0 = Math.max(0, ty0 - halfWindowSize);
        final int xMax = Math.min(tx0 + tw - 1 + halfWindowSize, srcWidth);
        final int yMax = Math.min(ty0 + th - 1 + halfWindowSize, srcHeight);
        final int w = xMax - x0 + 1;
        final int h = yMax - y0 + 1;
        return new Rectangle(x0, y0, w, h);
    }

    public static void computeSummationOfT3(final int zoneIdx, final double[][] Tr, final double[][] Ti,
                                      double[][][] sumRe, double[][][] sumIm) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                sumRe[zoneIdx-1][i][j] += Tr[i][j];
                sumIm[zoneIdx-1][i][j] += Ti[i][j];
            }
        }
    }

    /**
     * Compute determinant of a 3x3 Hermitian matrix
     * @param Tr Real part of the 3x3 Hermitian matrix
     * @param Ti Imaginary part of the 3x3 Hermitian matrix
     * @return The determinant
     */
    public static double determinantCmplxMatrix3(final double[][] Tr, final double[][] Ti) {

        final double cof00R = Tr[1][1]*Tr[2][2] - Ti[1][1]*Ti[2][2] - Tr[1][2]*Tr[2][1] + Ti[1][2]*Ti[2][1];
        final double cof00I = Tr[1][1]*Ti[2][2] + Ti[1][1]*Tr[2][2] - Tr[1][2]*Ti[2][1] - Ti[1][2]*Tr[2][1];

        final double cof01R = Tr[1][0]*Tr[2][2] - Ti[1][0]*Ti[2][2] - Tr[1][2]*Tr[2][0] + Ti[1][2]*Ti[2][0];
        final double cof01I = Tr[1][0]*Ti[2][2] + Ti[1][0]*Tr[2][2] - Tr[1][2]*Ti[2][0] - Ti[1][2]*Tr[2][0];

        final double cof02R = Tr[1][0]*Tr[2][1] - Ti[1][0]*Ti[2][1] - Tr[1][1]*Tr[2][0] + Ti[1][1]*Ti[2][0];
        final double cof02I = Tr[1][0]*Ti[2][1] + Ti[1][0]*Tr[2][1] - Tr[1][1]*Ti[2][0] - Ti[1][1]*Tr[2][0];

        final double detR = Tr[0][0]*cof00R - Ti[0][0]*cof00I - Tr[0][1]*cof01R +
                            Ti[0][1]*cof01I + Tr[0][2]*cof02R + Ti[0][2]*cof02I;

        final double detI = Tr[0][0]*cof00I + Ti[0][0]*cof00R - Tr[0][1]*cof01I -
                            Ti[0][1]*cof01R + Tr[0][2]*cof02I + Ti[0][2]*cof02R;

        double det = Math.sqrt(detR*detR + detI*detI);
        if (det < PolOpUtils.EPS) {
            det = PolOpUtils.EPS;
        }
        return det;
    }

    /**
     * Compute inverse of a 3x3 Hermitian matrix
     * @param Tr Real part of the 3x3 Hermitian matrix
     * @param Ti Imaginary part of the 3x3 Hermitian matrix
     * @param iTr Real part of the inversed 3x3 Hermitian matrix
     * @param iTi Imaginary part of the inversed 3x3 Hermitian matrix
     */
    public static void inverseCmplxMatrix3(final double[][] Tr, final double[][] Ti, double[][] iTr, double[][] iTi) {

        iTr[0][0] = Tr[1][1]*Tr[2][2] - Ti[1][1]*Ti[2][2] - Tr[1][2]*Tr[2][1] + Ti[1][2]*Ti[2][1];
        iTi[0][0] = Tr[1][1]*Ti[2][2] + Ti[1][1]*Tr[2][2] - Tr[1][2]*Ti[2][1] - Ti[1][2]*Tr[2][1];

        iTr[0][1] = Tr[2][1]*Tr[0][2] - Ti[2][1]*Ti[0][2] - Tr[2][2]*Tr[0][1] + Ti[2][2]*Ti[0][1];
        iTi[0][1] = Tr[2][1]*Ti[0][2] + Ti[2][1]*Tr[0][2] - Tr[2][2]*Ti[0][1] - Ti[2][2]*Tr[0][1];

        iTr[0][2] = Tr[0][1]*Tr[1][2] - Ti[0][1]*Ti[1][2] - Tr[1][1]*Tr[0][2] + Ti[1][1]*Ti[0][2];
        iTi[0][2] = Tr[0][1]*Ti[1][2] + Ti[0][1]*Tr[1][2] - Tr[1][1]*Ti[0][2] - Ti[1][1]*Tr[0][2];

        iTr[1][0] = Tr[2][0]*Tr[1][2] - Ti[2][0]*Ti[1][2] - Tr[1][0]*Tr[2][2] + Ti[1][0]*Ti[2][2];
        iTi[1][0] = Tr[2][0]*Ti[1][2] + Ti[2][0]*Tr[1][2] - Tr[1][0]*Ti[2][2] - Ti[1][0]*Tr[2][2];

        iTr[1][1] = Tr[0][0]*Tr[2][2] - Ti[0][0]*Ti[2][2] - Tr[2][0]*Tr[0][2] + Ti[2][0]*Ti[0][2];
        iTi[1][1] = Tr[0][0]*Ti[2][2] + Ti[0][0]*Tr[2][2] - Tr[2][0]*Ti[0][2] - Ti[2][0]*Tr[0][2];

        iTr[1][2] = Tr[1][0]*Tr[0][2] - Ti[1][0]*Ti[0][2] - Tr[0][0]*Tr[1][2] + Ti[0][0]*Ti[1][2];
        iTi[1][2] = Tr[1][0]*Ti[0][2] + Ti[1][0]*Tr[0][2] - Tr[0][0]*Ti[1][2] - Ti[0][0]*Tr[1][2];

        iTr[2][0] = Tr[1][0]*Tr[2][1] - Ti[1][0]*Ti[2][1] - Tr[2][0]*Tr[1][1] + Ti[2][0]*Ti[1][1];
        iTi[2][0] = Tr[1][0]*Ti[2][1] + Ti[1][0]*Tr[2][1] - Tr[2][0]*Ti[1][1] - Ti[2][0]*Tr[1][1];

        iTr[2][1] = Tr[2][0]*Tr[0][1] - Ti[2][0]*Ti[0][1] - Tr[0][0]*Tr[2][1] + Ti[0][0]*Ti[2][1];
        iTi[2][1] = Tr[2][0]*Ti[0][1] + Ti[2][0]*Tr[0][1] - Tr[0][0]*Ti[2][1] - Ti[0][0]*Tr[2][1];

        iTr[2][2] = Tr[0][0]*Tr[1][1] - Ti[0][0]*Ti[1][1] - Tr[1][0]*Tr[0][1] + Ti[1][0]*Ti[0][1];
        iTi[2][2] = Tr[0][0]*Ti[1][1] + Ti[0][0]*Tr[1][1] - Tr[1][0]*Ti[0][1] - Ti[1][0]*Tr[0][1];

        final double detR = Tr[0][0]*iTr[0][0] - Ti[0][0]*iTi[0][0] + Tr[1][0]*iTr[0][1] -
                            Ti[1][0]*iTi[0][1] + Tr[2][0]*iTr[0][2] - Ti[2][0]*iTi[0][2];

        final double detI = Tr[0][0]*iTi[0][0] + Ti[0][0]*iTr[0][0] + Tr[1][0]*iTi[0][1] +
                            Ti[1][0]*iTr[0][1] + Tr[2][0]*iTi[0][2] + Ti[2][0]*iTr[0][2];

        final double det = Math.sqrt(detR*detR + detI*detI);

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                iTr[i][j] /= det;
                iTi[i][j] /= det;
            }
        }
    }

    public IndexCoding createIndexCoding() {
        final IndexCoding indexCoding = new IndexCoding("Cluster_classes");
        indexCoding.addIndex("no data", NODATACLASS, "no data");
        for (int i = 1; i <= getNumClasses(); i++) {
            indexCoding.addIndex("class_" + i, i, "Cluster " + i);
        }
        return indexCoding;
    }

    public static class ClusterInfo {
        public int zoneIndex;
        public int size;
        public double logDet;
        public final double[][] centerRe = new double[3][3];
        public final double[][] centerIm = new double[3][3];
        public final double[][] invCenterRe = new double[3][3];
        public final double[][] invCenterIm = new double[3][3];

        public ClusterInfo() {
        }

        public void setClusterCenter(final int zoneIdx, final double[][] Tr, final double[][] Ti, final int size) {
            this.zoneIndex = zoneIdx;
            this.size = size;
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 3; ++j) {
                    this.centerRe[i][j] = Tr[i][j];
                    this.centerIm[i][j] = Ti[i][j];
                }
            }

            this.logDet = Math.log(determinantCmplxMatrix3(Tr, Ti));
            inverseCmplxMatrix3(Tr, Ti, invCenterRe, invCenterIm);
        }
    }

    /**
     * Return the band name for the target product
     *
     * @return band name
     */
    public abstract String getTargetBandName();

    /**
     * returns the number of classes
     *
     * @return num classes
     */

    /**
     * Perform decomposition for given tile.
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be
     * computed.
     * @param op the polarimetric decomposition operator
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs
     * during computation of the filtered value.
     */
    public abstract void computeTile(final Band targetBand, final Tile targetTile, final Operator op);

    /**
     * Classify a vector returning a vector of numCategories-1 scores. It is
     * assumed that the score for the missing category is one minus the sum of
     * the scores that are returned.
     *
     * Note that the missing score is the 0-th score.
     *
     * @param instance A feature vector to be classified.
     * @return A vector of probabilities in 1 of n-1 encoding.
     */
    public abstract Vector classify(Vector instance);

    /**
     * Classify a vector, but don't apply the inverse link function. For
     * logistic regression and other generalized linear models, this is just the
     * linear part of the classification.
     *
     * @param features A feature vector to be classified.
     * @return A vector of scores. If transformed by the link function, these
     * will become probabilities.
     */
    public Vector classifyNoLink(Vector features) {
        throw new UnsupportedOperationException("Classifier " + this.getClass().getName()
                + " doesn't support classification without a link");
    }

    /**
     * Classifies a vector in the special case of a binary classifier where
     * <code>classify(Vector)</code> would return a vector with only one
     * element. As such, using this method can void the allocation of a vector.
     *
     * @param instance The feature vector to be classified.
     * @return The score for category 1.
     *
     * @see #classify(Vector)
     */
    public abstract double classifyScalar(Vector instance);

    /**
     * Returns n probabilities, one for each category. If you can use an n-1
     * coding, and are touchy about allocation performance, then the classify
     * method is probably better to use. The 0-th element of the score vector
     * returned by this method is the missing score as computed by the classify
     * method.
     *
     * @see #classify(Vector)
     * @see #classifyFull(Vector r, Vector instance)
     *
     * @param instance A vector of features to be classified.
     * @return A vector of probabilities, one for each category.
     */
    public Vector classifyFull(Vector instance) {
        return classifyFull(new DenseVector(getNumClasses()), instance);
    }

    /**
     * Returns n probabilities, one for each category into a pre-allocated
     * vector. One vector allocation is still done in the process of multiplying
     * by the coefficient matrix, but that is hard to avoid. The cost of such an
     * ephemeral allocation is very small in any case compared to the
     * multiplication itself.
     *
     * @param r Where to put the results.
     * @param instance A vector of features to be classified.
     * @return A vector of probabilities, one for each category.
     */
    public Vector classifyFull(Vector r, Vector instance) {
        r.viewPart(1, getNumClasses() - 1).assign(classify(instance));
        r.setQuick(0, 1 - r.zSum());
        return r;
    }

    /**
     * Returns n-1 probabilities, one for each category but the last, for each
     * row of a matrix. The probability of the missing 0-th category is 1 -
     * rowSum(this result).
     *
     * @param data The matrix whose rows are vectors to classify
     * @return A matrix of scores, one row per row of the input matrix, one
     * column for each but the last category.
     */
    public Matrix classify(Matrix data) {
        Matrix r = new DenseMatrix(data.numRows(), getNumClasses() - 1);
        for (int row = 0; row < data.numRows(); row++) {
            r.assignRow(row, classify(data.viewRow(row)));
        }
        return r;
    }

    /**
     * Returns n probabilities, one for each category, for each row of a matrix.
     *
     * @param data The matrix whose rows are vectors to classify
     * @return A matrix of scores, one row per row of the input matrix, one
     * column for each but the last category.
     */
    public Matrix classifyFull(Matrix data) {
        Matrix r = new DenseMatrix(data.numRows(), getNumClasses());
        for (int row = 0; row < data.numRows(); row++) {
            classifyFull(r.viewRow(row), data.viewRow(row));
        }
        return r;
    }

    /**
     * Returns a vector of probabilities of the first category, one for each row
     * of a matrix. This only makes sense if there are exactly two categories,
     * but calling this method in that case can save a number of vector
     * allocations.
     *
     * @param data The matrix whose rows are vectors to classify
     * @return A vector of scores, with one value per row of the input matrix.
     */
    public Vector classifyScalar(Matrix data) {
        Preconditions.checkArgument(getNumClasses() == 2, "Can only call classifyScalar with two categories");

        Vector r = new DenseVector(data.numRows());
        for (int row = 0; row < data.numRows(); row++) {
            r.set(row, classifyScalar(data.viewRow(row)));
        }
        return r;
    }

    /**
     * Returns a measure of how good the classification for a particular example
     * actually is.
     *
     * @param actual The correct category for the example.
     * @param data The vector to be classified.
     * @return The log likelihood of the correct answer as estimated by the
     * current model. This will always be <= 0 and larger (closer to 0)
     * indicates better accuracy. In order to simplify code that maintains
     * running averages, we bound this value at -100.
     */
    public double logLikelihood(int actual, Vector data) {
        if (getNumClasses() == 2) {
            double p = classifyScalar(data);
            if (actual > 0) {
                return Math.max(-100, Math.log(p));
            } else {
                return Math.max(-100, Math.log(1 - p));
            }
        } else {
            Vector p = classify(data);
            if (actual > 0) {
                return Math.max(-100, Math.log(p.get(actual - 1)));
            } else {
                return Math.max(-100, Math.log(1 - p.zSum()));
            }
        }
    }
}
