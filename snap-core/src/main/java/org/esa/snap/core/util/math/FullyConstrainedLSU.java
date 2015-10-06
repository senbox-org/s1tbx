/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.util.math;

import Jama.Matrix;

/**
 * Performs a fully constrained linear spectral unmixing, where all
 * abundances are non-negative and their sum is equal to unity.
 *
 * @author Helmut Schiller (GKSS)
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since 4.1
 */
public class FullyConstrainedLSU implements SpectralUnmixing {

    private final int nchem;
    private final int nmemb;

    private final SpectralUnmixing[][] trialModels;
    private final boolean[][][] sortedemcombs;

    public FullyConstrainedLSU(double[][] endmembers) {
        if (!LinearAlgebra.isMatrix(endmembers)) {
            throw new IllegalArgumentException("Parameter 'endmembers' is not a matrix.");
        }

        nchem = endmembers.length;
        nmemb = endmembers[0].length;

        final int nposs = (1 << nmemb) - 1;
        int rb[] = new int[nposs];
        for (int i = 0; i < nposs; i++) {
            rb[i] = i + 1;
        }

        final boolean[][] emcombs = new boolean[nposs][nmemb];
        for (int k = 0; k < nposs; k++) {
            int zpot = 1;
            for (int l = 0; l < nmemb; l++) {
                int h = zpot & rb[k];
                emcombs[k][l] = h > 0;
                zpot *= 2;
            }
        }
        final int[] numbin = new int[nposs];
        for (int k = 0; k < nposs; k++) {
            numbin[k] = countTrue(emcombs[k]);
        }
        sortedemcombs = new boolean[nmemb][][];
        trialModels = new ConstrainedLSU[nmemb][];
        for (int nem = nmemb - 1; nem >= 0; nem--) {
            int nc = 0;
            for (int p = 0; p < nposs; p++) {
                if (nem + 1 == numbin[p]) {
                    nc++;
                }
            }
            sortedemcombs[nmemb - nem - 1] = new boolean[nc][nmemb];
            trialModels[nmemb - nem - 1] = new ConstrainedLSU[nc];
        }
        for (int nem = nmemb - 1; nem >= 0; nem--) {
            int nc = 0;
            for (int p = 0; p < nposs; p++) {
                if (nem + 1 == numbin[p]) {
                    System.arraycopy(emcombs[p], 0, sortedemcombs[nmemb - nem - 1][nc], 0, nmemb);
                    double[][] trem = extractColumns(endmembers, sortedemcombs[nmemb - nem - 1][nc]);
                    trialModels[nmemb - nem - 1][nc] = new ConstrainedLSU(trem);
                    nc++;
                }
            }
        }
    }

    @Override
    public double[][] unmix(double[][] spectra) {
        final int colCount = spectra[0].length;

        final Matrix res = new Matrix(nmemb, colCount);
        for (int nspek = 0; nspek < colCount; nspek++) {
            final double[][] singlesp = extractSingleColum(spectra, nspek);
            double totalerrbest=1.e21;
            for (int nc = 0; nc < nmemb; nc++) {
                boolean foundlegal = false;
                final int nmods = trialModels[nc].length;
                final boolean allabupos[] = new boolean[nmods];
                final double[][][] abuc = new double[nmods][][];
                for (int m = 0; m < nmods; m++) {
                    allabupos[m] = true;
                }
                final double err[] = new double[nmods];
                for (int m = 0; m < nmods; m++) {
                    abuc[m] = trialModels[nc][m].unmix(singlesp);
                    for (int k = 0; k < abuc[m].length; k++) {
                        if (abuc[m][k][0] < 0.0) {
                            allabupos[m] = false;
                        }
                    }
                    if (allabupos[m]) {
                        foundlegal = true;
                        final double[][] rspek = trialModels[nc][m].mix(abuc[m]);
                        double sum = 0.0;
                        for (int k = 0; k < nchem; k++) {
                            final double diff = singlesp[k][0] - rspek[k][0];
                            sum += diff * diff;
                        }
                        err[m] = sum;
                    }
                }
                if (foundlegal) {
                    int mbest = -1;
                    double errbest = Double.POSITIVE_INFINITY;
                    for (int m = 0; m < nmods; m++) {
                        if (allabupos[m]) {
                            if (err[m] < errbest) {
                                errbest = err[m];
                                mbest = m;
                            }
                        }
                    }
                    if (mbest != -1) {
                        final double[][] abucd = abuc[mbest];
                        final double[][] abu = new double[nmemb][1];
                        int take = 0;
                        for (int k = 0; k < nmemb; k++) {
                            if (sortedemcombs[nc][mbest][k]) {
                                abu[k][0] = abucd[take][0];
                                take++;
                            }
                        }
                        if( errbest< totalerrbest) {
                            res.setMatrix(0, nmemb-1, nspek, nspek, new Matrix(abu));
                            totalerrbest=errbest;
                        }
                    }
                }
            }
        }

        return res.getArrayCopy();
    }

    @Override
    public double[][] mix(double[][] abundances) {
        return trialModels[0][0].mix(abundances);
    }

    private static double[][] extractColumns(double[][] a, boolean[] columns) {
        final int rowCount = a.length;
        final int colCount = a[0].length;

        int hm = 0;
        for (final boolean column : columns) {
            if (column) {
                hm++;
            }
        }

        final double[][] c = new double[rowCount][hm];

        for (int j = 0, k = 0; j < colCount; j++) {
            if (columns[j]) {
                for (int i = 0; i < rowCount; i++) {
                    c[i][k] = a[i][j];
                }
                k++;
            }
        }

        return c;
    }

    private static double[][] extractSingleColum(double[][] a, int j) {
        final double[][] c = new double[a.length][1];

        for (int i = 0; i < a.length; i++) {
            c[i][0] = a[i][j];
        }

        return c;
    }

    private static int countTrue(boolean[] combs) {
        int count = 0;
        for (final boolean comb : combs) {
            if (comb) {
                count++;
            }
        }

        return count;
    }
}
