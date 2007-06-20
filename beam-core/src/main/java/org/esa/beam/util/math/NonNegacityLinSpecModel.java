package org.esa.beam.util.math;

import Jama.Matrix;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class NonNegacityLinSpecModel {

    private int nchem, nmemb;
    private LinearSpectralUnmixing[][] trialModels;
    private boolean[][][] sortedemcombs;

    public NonNegacityLinSpecModel(Matrix endmembs) {

        this.nchem = endmembs.getRowDimension();
        this.nmemb = endmembs.getColumnDimension();
        int nposs = posp(nmemb) - 1;
        //System.out.println(nposs);
        int rb[] = new int[nposs];
        for (int i = 0; i < nposs; i++) {
            rb[i] = i + 1;
        }

        boolean[][]  emcombs = new boolean[nposs][nmemb];
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
        trialModels = new LinearSpectralUnmixing[nmemb][];
        for (int nem = nmemb - 1; nem >= 0; nem--) {
            int nc = 0;
            for (int p = 0; p < nposs; p++) {
                if (nem + 1 == numbin[p]) {
                    nc++;
                }
            }
            //System.out.println(nmemb-nem-1+"  "+nc);
            sortedemcombs[nmemb - nem - 1] = new boolean[nc][nmemb];
            trialModels[nmemb - nem - 1] = new LinearSpectralUnmixing[nc];
        }
        for (int nem = nmemb - 1; nem >= 0; nem--) {
            int nc = 0;
            for (int p = 0; p < nposs; p++) {
                if (nem + 1 == numbin[p]) {
                    for (int m = 0; m < nmemb; m++) {
                        sortedemcombs[nmemb - nem - 1][nc][m] = emcombs[p][m];
                    }
                    Matrix trem = extractCols(endmembs, sortedemcombs[nmemb - nem - 1][nc]);
                    trialModels[nmemb - nem - 1][nc] = new LinearSpectralUnmixing(trem);
                    //System.out.print(nmemb-nem-1+"  "+nc+"  ");
                    //for(int m=0; m<nmemb; m++) System.out.print(sortedemcombs[nmemb-nem-1][nc][m]+"  ");
                    //System.out.println();
                    nc++;
                }
            }
        }
    }
//	private int nchem, nmemb;
//	private LinPixSpecModel[][] trialModels;
//	private boolean[][][]  sortedemcombs;

    private Matrix unmix(Matrix specs, boolean abusum1) {
        // endlos bei negativen spektren- oder endmembs-anteilen
        Matrix res = new Matrix(nmemb, specs.getColumnDimension());
        for (int nspek = 0; nspek < specs.getColumnDimension(); nspek++) {
            Matrix singlesp = specs.getMatrix(0, nchem - 1, nspek, nspek);
            for (int nc = 0; nc < nmemb; nc++) {
                boolean foundlegal = false;
                int nmods = trialModels[nc].length;
                boolean allabupos[] = new boolean[nmods];
                Matrix[]abuc = new Matrix[nmods];
                for (int m = 0; m < nmods; m++) {
                    allabupos[m] = true;
                }
                double err[] = new double[nmods];
                for (int m = 0; m < nmods; m++) {
                    //System.out.println(nc+"  "+m+"  "+nmods);
                    if (abusum1) {
                        abuc[m] = trialModels[nc][m].unmixConstrained(singlesp);
                    } else {
                        abuc[m] = trialModels[nc][m].unmixUnconstrained(singlesp);
                    }
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

    public Matrix unmixUnconstrained(Matrix specs) {
        // specs-dim gegen nchem testen!!!!!!!!!!!!
        return unmix(specs, false);
    }


    public Matrix unmixConstrained(Matrix specs) {

        return unmix(specs, true);
        // );
    }

    public Matrix mix(Matrix abund) {
        return trialModels[0][0].mix(abund);
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

    private Matrix extractRows(Matrix M, boolean[] take) {
        int hm = 0;
        int nrow = M.getRowDimension();
        int ncol = M.getColumnDimension();
        for (int i = 0; i < take.length; i++) {
            if (take[i]) {
                hm++;
            }
        }
        double[][] res = new double[hm][ncol];
        int tr = 0;
        for (int ir = 0; ir < nrow; ir++) {
            if (take[ir]) {
                for (int ic = 0; ic < ncol; ic++) {
                    res[tr][ic] = M.get(ir, ic);
                }
                tr++;
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


    /**
     * @param args
     */
    public static void main(String[] args) {
        Matrix endmembs;
        try {
            endmembs = Matrix.read(new BufferedReader(new FileReader(
                    "endmembs.malres")));
            NonNegacityLinSpecModel mlm = new NonNegacityLinSpecModel(endmembs);
            Matrix specs = Matrix.read(new BufferedReader(new FileReader(
                    "specs.malres")));
            Matrix abucon = mlm.unmixConstrained(specs.getMatrix(0, 5, 10, 10));
            //Matrix abucon = mlm.unmixUnconstrained(specs);
            //Matrix abucon = mlm.unmixConstrained(specs);
            abucon.transpose().print(12, 5);
            Matrix specnocon = mlm.mix(abucon);
            specnocon.transpose().print(12, 5);
//			Matrix diffspecnocon = specnocon.minus(specs);
//			// diffspecnocon.transpose().print(12, 5);
//			Matrix abunconstr = Matrix.read(new BufferedReader(new FileReader(
//					"abunconstr.orig")));
//			// abunconstr.minus(abunocon).transpose().print(15, 10);
//			System.out.println("   no constraint abunddiff  "
//					+ maxabs(abunconstr.minus(abunocon)));
//
//			Matrix abucon = mlm.unmixConstrained(specs);
//			// abucon.transpose().print(12, 5);
//			Matrix speccon = mlm.mix(abucon, endmembs);
//			Matrix diffspeccon = speccon.minus(specs);
//			// diffspecnocon.transpose().print(12, 5);
//			Matrix abconstr = Matrix.read(new BufferedReader(new FileReader(
//					"abconstr.orig")));
//			// abconstr.minus(abucon).transpose().print(15, 10);
//			System.out.println("\nsum 1 constraint abunddiff  "
//					+ maxabs(abconstr.minus(abucon)));
//
//			double[][] bei=new double[2][3];
//			System.out.println(bei.length+"   "+bei[0].length);
//
//
//			int nrow = abucon.getRowDimension();
//			int ncol = abucon.getColumnDimension();
//			double[][] abucons = abucon.getArray();
//			double maxd1 = -1.;
//			for (int ic = 0; ic < ncol; ic++) {
//				double sum = 0.;
//				for (int ir = 0; ir < nrow; ir++) {
//					sum += abucons[ir][ic];
//				}
//				double ad = Math.abs(sum - 1.);
//				if (ad > maxd1)
//					maxd1 = ad;
//			}
//			System.out.println("\nsum 1 constraint diff  " + maxd1);
//
//
//			Matrix abuNCLS = mlm.unmixNCLS(specs);
//			abuNCLS.transpose().print(12, 5);
//			//Matrix speccon = mlm.mix(abucon, endmembs);
//			//Matrix diffspeccon = speccon.minus(specs);
//			// diffspecnocon.transpose().print(12, 5);


        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}