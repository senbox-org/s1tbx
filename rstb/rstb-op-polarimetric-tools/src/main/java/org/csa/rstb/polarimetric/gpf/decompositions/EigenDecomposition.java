package org.csa.rstb.polarimetric.gpf.decompositions;

public class EigenDecomposition {

    /**
     * Perform eigenvalue decomposition for a given Hermitian matrix
     *
     * @param n           Matrix dimension
     * @param HMr         Real part of the Hermitian matrix
     * @param HMi         Imaginary part of the Hermitian matrix
     * @param EigenVectRe Real part of the eigenvector matrix
     * @param EigenVectIm Imaginary part of the eigenvector matrix
     * @param EigenVal    Eigenvalue vector
     */
    public static void eigenDecomposition(final int n, final double[][] HMr, final double[][] HMi,
                                          final double[][] EigenVectRe, final double[][] EigenVectIm, final double[] EigenVal) {

        final double[][] ar = new double[n][n];
        final double[][] ai = new double[n][n];
        final double[][] vr = new double[n][n];
        final double[][] vi = new double[n][n];
        final double[] d = new double[n];
        final double[] z = new double[n];
        final double[] w = new double[2];
        final double[] s = new double[2];
        final double[] c = new double[2];
        final double[] titi = new double[2];
        final double[] gc = new double[2];
        final double[] hc = new double[2];
        double sm, tresh, x, toto, e, f, g, h, r, d1, d2;
        int p, q, ii, i, j, k;
        int n2 = n * n;

        for (i = 0; i < n; i++) {
            for (j = 0; j < n; j++) {
                ar[i][j] = HMr[i][j];
                ai[i][j] = HMi[i][j];
                vr[i][j] = 0.0;
                vi[i][j] = 0.0;
            }
            vr[i][i] = 1.;
            vi[i][i] = 0.;

            d[i] = ar[i][i];
            z[i] = 0.;
        }

        final int iiMax = 1000 * n2;
        for (ii = 1; ii < iiMax; ii++) {

            sm = 0.;
            for (p = 0; p < n - 1; p++) {
                for (q = p + 1; q < n; q++) {
                    sm += 2.0 * Math.sqrt(ar[p][q] * ar[p][q] + ai[p][q] * ai[p][q]);
                }
            }
            sm /= (n2 - n);

            if (sm < 1.E-16) {
                break;
            }

            tresh = 1.E-17;
            if (ii < 4) {
                tresh = (long) 0.2 * sm / n2;
            }

            x = -1.E-15;
            p = 0;
            q = 0;
            for (i = 0; i < n - 1; i++) {
                for (j = i + 1; j < n; j++) {
                    toto = Math.sqrt(ar[i][j] * ar[i][j] + ai[i][j] * ai[i][j]);
                    if (x < toto) {
                        x = toto;
                        p = i;
                        q = j;
                    }
                }
            }
            toto = Math.sqrt(ar[p][q] * ar[p][q] + ai[p][q] * ai[p][q]);
            if (toto > tresh) {
                e = d[p] - d[q];
                w[0] = ar[p][q];
                w[1] = ai[p][q];
                g = Math.sqrt(w[0] * w[0] + w[1] * w[1]);
                g = g * g;
                f = Math.sqrt(e * e + 4.0 * g);
                d1 = e + f;
                d2 = e - f;
                if (Math.abs(d2) > Math.abs(d1)) {
                    d1 = d2;
                }
                r = Math.abs(d1) / Math.sqrt(d1 * d1 + 4.0 * g);
                s[0] = r;
                s[1] = 0.0;
                titi[0] = 2.0 * r / d1;
                titi[1] = 0.0;
                c[0] = titi[0] * w[0] - titi[1] * w[1];
                c[1] = titi[0] * w[1] + titi[1] * w[0];
                r = Math.sqrt(s[0] * s[0] + s[1] * s[1]);
                r = r * r;
                h = (d1 / 2.0 + 2.0 * g / d1) * r;
                d[p] = d[p] - h;
                z[p] = z[p] - h;
                d[q] = d[q] + h;
                z[q] = z[q] + h;
                ar[p][q] = 0.0;
                ai[p][q] = 0.0;

                for (j = 0; j < p; j++) {
                    gc[0] = ar[j][p];
                    gc[1] = ai[j][p];
                    hc[0] = ar[j][q];
                    hc[1] = ai[j][q];
                    ar[j][p] = c[0] * gc[0] - c[1] * gc[1] - s[0] * hc[0] - s[1] * hc[1];
                    ai[j][p] = c[0] * gc[1] + c[1] * gc[0] - s[0] * hc[1] + s[1] * hc[0];
                    ar[j][q] = s[0] * gc[0] - s[1] * gc[1] + c[0] * hc[0] + c[1] * hc[1];
                    ai[j][q] = s[0] * gc[1] + s[1] * gc[0] + c[0] * hc[1] - c[1] * hc[0];
                }
                for (j = p + 1; j < q; j++) {
                    gc[0] = ar[p][j];
                    gc[1] = ai[p][j];
                    hc[0] = ar[j][q];
                    hc[1] = ai[j][q];
                    ar[p][j] = c[0] * gc[0] + c[1] * gc[1] - s[0] * hc[0] - s[1] * hc[1];
                    ai[p][j] = c[0] * gc[1] - c[1] * gc[0] + s[0] * hc[1] - s[1] * hc[0];
                    ar[j][q] = s[0] * gc[0] + s[1] * gc[1] + c[0] * hc[0] + c[1] * hc[1];
                    ai[j][q] = -s[0] * gc[1] + s[1] * gc[0] + c[0] * hc[1] - c[1] * hc[0];
                }
                for (j = q + 1; j < n; j++) {
                    gc[0] = ar[p][j];
                    gc[1] = ai[p][j];
                    hc[0] = ar[q][j];
                    hc[1] = ai[q][j];
                    ar[p][j] = c[0] * gc[0] + c[1] * gc[1] - s[0] * hc[0] + s[1] * hc[1];
                    ai[p][j] = c[0] * gc[1] - c[1] * gc[0] - s[0] * hc[1] - s[1] * hc[0];
                    ar[q][j] = s[0] * gc[0] + s[1] * gc[1] + c[0] * hc[0] - c[1] * hc[1];
                    ai[q][j] = s[0] * gc[1] - s[1] * gc[0] + c[0] * hc[1] + c[1] * hc[0];
                }
                for (j = 0; j < n; j++) {
                    gc[0] = vr[j][p];
                    gc[1] = vi[j][p];
                    hc[0] = vr[j][q];
                    hc[1] = vi[j][q];
                    vr[j][p] = c[0] * gc[0] - c[1] * gc[1] - s[0] * hc[0] - s[1] * hc[1];
                    vi[j][p] = c[0] * gc[1] + c[1] * gc[0] - s[0] * hc[1] + s[1] * hc[0];
                    vr[j][q] = s[0] * gc[0] - s[1] * gc[1] + c[0] * hc[0] + c[1] * hc[1];
                    vi[j][q] = s[0] * gc[1] + s[1] * gc[0] + c[0] * hc[1] - c[1] * hc[0];
                }
            }
        }

        for (k = 0; k < n; k++) {
            d[k] = 0;
            for (i = 0; i < n; i++) {
                for (j = 0; j < n; j++) {
                    d[k] = d[k] + vr[i][k] * (HMr[i][j] * vr[j][k] - HMi[i][j] * vi[j][k]);
                    d[k] = d[k] + vi[i][k] * (HMr[i][j] * vi[j][k] + HMi[i][j] * vr[j][k]);
                }
            }
        }

        double tmp_r, tmp_i;
        for (i = 0; i < n; i++) {
            for (j = i + 1; j < n; j++) {
                if (d[j] > d[i]) {
                    x = d[i];
                    d[i] = d[j];
                    d[j] = x;
                    for (k = 0; k < n; k++) {
                        tmp_r = vr[k][i];
                        tmp_i = vi[k][i];
                        vr[k][i] = vr[k][j];
                        vi[k][i] = vi[k][j];
                        vr[k][j] = tmp_r;
                        vi[k][j] = tmp_i;
                    }
                }
            }
        }

        for (i = 0; i < n; i++) {
            EigenVal[i] = d[i];
            for (j = 0; j < n; j++) {
                EigenVectRe[i][j] = vr[i][j];
                EigenVectIm[i][j] = vi[i][j];
            }
        }
    }
}
