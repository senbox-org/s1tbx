package org.esa.beam.util.math;

import Jama.Matrix;

public class FullyConstrainedLSU implements SpectralUnmixing {

    private int nchem, nmemb;
    private SpectralUnmixing[][] trialModels;
    private boolean[][][] sortedemcombs;

    public FullyConstrainedLSU(Matrix endmembs) {

        this.nchem = endmembs.getRowDimension();
        this.nmemb = endmembs.getColumnDimension();
        int nposs = posp(nmemb) - 1;
        //System.out.println(nposs);
        int rb[] = new int[nposs];
        for (int i = 0; i < nposs; i++) {
            rb[i] = i + 1;
        }

        boolean[][] emcombs = new boolean[nposs][nmemb];
        for (int k = 0; k < nposs; k++) {
            int zpot = 1;
            for (int l = 0; l < nmemb; l++) {
                int h = zpot & rb[k];
                if (h > 0) {
                    emcombs[k][l] = true;
                } else {
                    emcombs[k][l] = false;
                }
                zpot *= 2;
                //System.out.println(k+1+"  "+l+"  "+zpot+" "+h+" "+emcombs[k][l]);
            }
            //System.out.println();
        }
        int[] numbin = new int[nposs];
        for (int k = 0; k < nposs; k++) {
            numbin[k] = cttrue(emcombs[k]);
            //System.out.println(numbin[k]);
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
            //System.out.println(nmemb-nem-1+"  "+nc);
            sortedemcombs[nmemb - nem - 1] = new boolean[nc][nmemb];
            trialModels[nmemb - nem - 1] = new ConstrainedLSU[nc];
        }
        for (int nem = nmemb - 1; nem >= 0; nem--) {
            int nc = 0;
            for (int p = 0; p < nposs; p++) {
                if (nem + 1 == numbin[p]) {
                    for (int m = 0; m < nmemb; m++) {
                        sortedemcombs[nmemb - nem - 1][nc][m] = emcombs[p][m];
                    }
                    Matrix trem = extractCols(endmembs, sortedemcombs[nmemb - nem - 1][nc]);
                    trialModels[nmemb - nem - 1][nc] = new ConstrainedLSU(trem);
                    //System.out.print(nmemb-nem-1+"  "+nc+"  ");
                    //for(int m=0; m<nmemb; m++) System.out.print(sortedemcombs[nmemb-nem-1][nc][m]+"  ");
                    //System.out.println();
                    nc++;
                }
            }
        }
    }

    private Matrix unmix0(Matrix specs) {
        // endlos bei negativen spektren- oder endmembs-anteilen
        Matrix res = new Matrix(nmemb, specs.getColumnDimension());
        for (int nspek = 0; nspek < specs.getColumnDimension(); nspek++) {
            Matrix singlesp = specs.getMatrix(0, nchem - 1, nspek, nspek);
            for (int nc = 0; nc < nmemb; nc++) {
                boolean foundlegal = false;
                int nmods = trialModels[nc].length;
                boolean allabupos[] = new boolean[nmods];
                Matrix[] abuc = new Matrix[nmods];
                for (int m = 0; m < nmods; m++) {
                    allabupos[m] = true;
                }
                double err[] = new double[nmods];
                for (int m = 0; m < nmods; m++) {
                    //System.out.println(nc+"  "+m+"  "+nmods);
                    abuc[m] = trialModels[nc][m].unmix(singlesp);
                    for (int k = 0; k < abuc[m].getRowDimension(); k++) {
                        if (abuc[m].get(k, 0) < 0.) {
                            allabupos[m] = false;
                        }
                    }
                    //abuc[m].transpose().print(12, 4);
                    if (allabupos[m]) {
                        foundlegal = true;
                        Matrix rspek = trialModels[nc][m].mix(abuc[m]);
                        double sum = 0.;
                        for (int k = 0; k < nchem; k++) {
                            double diff = singlesp.get(k, 0) - rspek.get(k, 0);
                            sum += diff * diff;
                        }
                        err[m] = sum;
                    }
                    //System.out.println(nc+"  "+m+"  "+allabupos[m]);
                }
                if (foundlegal) {
                    int mbest = -1;
                    double errbest = 1.e20;
                    for (int m = 0; m < nmods; m++) {
                        if (allabupos[m]) {
                            if (err[m] < errbest) {
                                errbest = err[m];
                                mbest = m;
                            }
                        }
                    }
                    double[][] abucd = abuc[mbest].getArray();
                    double[][] abu = new double[nmemb][1];
                    int take = 0;
                    for (int k = 0; k < nmemb; k++) {
                        if (sortedemcombs[nc][mbest][k]) {
                            abu[k][0] = abucd[take][0];
                            take++;
                        }
                    }
                    res.setMatrix(0, nmemb - 1, nspek, nspek, new Matrix(abu));
                    break;
                }
            }
        }
        return res;
    }

    public Matrix unmix(Matrix specs) {
        return unmix0(specs);
    }

    public Matrix mix(Matrix abundances) {
        return trialModels[0][0].mix(abundances);
    }

    private Matrix extractCols(Matrix M, boolean[] take) {
        int hm = 0;
        int nrow = M.getRowDimension();
        int ncol = M.getColumnDimension();
        for (int i = 0; i < take.length; i++) {
            if (take[i]) {
                hm++;
            }
        }
        double[][] res = new double[nrow][hm];
        int tc = 0;
        for (int ic = 0; ic < ncol; ic++) {
            if (take[ic]) {
                for (int ir = 0; ir < nrow; ir++) {
                    res[ir][tc] = M.get(ir, ic);
                }
                tc++;
            }
        }
        return new Matrix(res);
    }

    static int posp(int n) {
        int res = 1;
        for (int i = 0; i < n; i++) {
            res *= 2;
        }
        return res;
    }

    static int cttrue(boolean[] combs) {
        int res = 0;
        for (int i = 0; i < combs.length; i++) {
            if (combs[i]) {
                res++;
            }
        }
        return res;
    }

}