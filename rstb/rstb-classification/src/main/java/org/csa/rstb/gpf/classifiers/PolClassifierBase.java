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
package org.csa.rstb.gpf.classifiers;

import org.csa.rstb.gpf.PolOpUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.nest.gpf.PolBandUtils;

import java.awt.*;
import java.util.Map;

/**
 * Base class for polarimetric classifiers
 */
public abstract class PolClassifierBase {

    public final static int NODATACLASS = 0;
    protected final PolBandUtils.MATRIX sourceProductType;
    protected final int srcWidth;
    protected final int srcHeight;
    protected final int windowSize;
    protected final int halfWindowSize;
    protected final Map<Band, PolBandUtils.QuadSourceBand> bandMap;

    protected PolClassifierBase(final PolBandUtils.MATRIX srcProductType,
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
    protected Rectangle getSourceRectangle(final int tx0, final int ty0, final int tw, final int th) {

        final int x0 = Math.max(0, tx0 - halfWindowSize);
        final int y0 = Math.max(0, ty0 - halfWindowSize);
        final int xMax = Math.min(tx0 + tw - 1 + halfWindowSize, srcWidth);
        final int yMax = Math.min(ty0 + th - 1 + halfWindowSize, srcHeight);
        final int w = xMax - x0 + 1;
        final int h = yMax - y0 + 1;
        return new Rectangle(x0, y0, w, h);
    }

    protected static void computeSummationOfT3(final int zoneIdx, final double[][] Tr, final double[][] Ti,
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
    private static double determinantCmplxMatrix3(final double[][] Tr, final double[][] Ti) {

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
    private static void inverseCmplxMatrix3(final double[][] Tr, final double[][] Ti, double[][] iTr, double[][] iTi) {

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
        indexCoding.addIndex("no data", Wishart.NODATACLASS, "no data");
        for (int i = 1; i <= getNumClasses(); i++) {
            indexCoding.addIndex("class_" + i, i, "Cluster " + i);
        }
        return indexCoding;
    }

    public static class ClusterInfo {
        int zoneIndex;
        int size;
        double logDet;
        final double[][] centerRe = new double[3][3];
        final double[][] centerIm = new double[3][3];
        final double[][] invCenterRe = new double[3][3];
        final double[][] invCenterIm = new double[3][3];

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
}
