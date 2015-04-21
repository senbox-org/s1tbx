/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package de.gkss.hs.datev2004;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 * calculations with the Gaussian
 *
 * @author H. Schiller / GKSS
 */
public class Gaussian {

    public Matrix covinv;
    public double[] cog;
    public double normfactor;
    public double var;
    public double center;

    /**
     * for dimension &gt; 1
     *
     * @param cov the covariance matrix
     * @param cog center of gravity
     */
    public Gaussian(double[] cog, double[][] cov) {
        SingularValueDecomposition svd;
        Matrix u, v, sinvm;
        //double s[], h[], detr = 1.0, pot = 1.0, sinv[][];
        double s[], detr = 1.0, pot = 1.0, sinv[][];
        int rang;
        try {
            this.cog = cog;
            Matrix covm = new Matrix(cov);
            svd = new SingularValueDecomposition(covm);
            s = svd.getSingularValues();
            rang = svd.rank();
            for (int i = 0; i < rang; i++) {
                pot *= 2.0 * Math.PI;
                detr *= s[i];
            }
            sinv = new double[s.length][s.length];
            for (int i = 0; i < s.length; i++)
                if (s[i] < 1.e-10)
                    sinv[i][i] = 0.;//=1.e10; this sometimes gives -1.e10 after next multiplications.
                    //therefor this test is repeated (see below)
                else
                    sinv[i][i] = 1.0 / s[i];
            u = svd.getU();
            v = svd.getV();
            sinvm = new Matrix(sinv);
            this.covinv = v.times(sinvm.times(u.transpose()));
            for (int i = 0; i < s.length; i++)// we repeat the test
                if (s[i] < 1.e-10)
                    this.covinv.set(i, i, 1.e10);
            this.var = -1.;
            this.normfactor = 1. / Math.sqrt(pot * detr);
        } catch (IllegalArgumentException e) {
            General.error("Gaussian: " + e.toString());
        }
    }


    /**
     * For dimension=1
     *
     * @param center the center of the distribution
     * @param var    the variance
     */
    public Gaussian(double center, double var) {
        this.center = center;
        this.var = var;
    }


    /**
     * @param pt point
     * @return the square of the  Mahalanobis distance of the point from center of gravity
     */
    public double distancesqu(double[] pt) {
        double[] df = new double[pt.length];
        for (int k = 0; k < pt.length; k++){
            df[k] = pt[k] - this.cog[k];
        }
        Matrix dfm = new Matrix(df, pt.length);
        return Math.abs(dfm.transpose().times(this.covinv.times(dfm)).getArray()[0][0]);
    }

    /**
     * @param a one point
     * @return the square of the Mahalanobis distance of the point from the center
     */
    public double distancesqu(double a) {
        double res;
        if (this.var < 1.e-10) {
            if (Math.abs(this.center - a) < 1.e-10)
                res = 0.;
            else
                res = 1.e10;
        } else {
            res = (this.center - a) * (this.center - a) / var;
        }
        return res;
    }

    /**
     * @param a a point
     * @return the density of the gaussian at this point
     */
    public double density(double[] a) {
        return this.normfactor * Math.exp(-0.5 * this.distancesqu(a));
    }

    /**
     * @param a one point
     * @return the density of the gaussian at this point
     */
    public double density(double a) {
        return Math.exp(-0.5 * this.distancesqu(a)) /
                Math.sqrt(2.0 * Math.PI * Math.max(this.var, 1.e-10));
    }
}
