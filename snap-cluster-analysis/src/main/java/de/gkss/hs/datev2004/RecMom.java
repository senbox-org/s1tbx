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

/**
 * Recursiv calculation of zeroth up to second moment
 * @author H. Schiller / GKSS
 */
public class RecMom {

    /**the dimension of the points*/
    int dim;
    /**the number of points*/
    public long npoints;
    /**the sum of weights*/
    public double swgt;
    /**the average (dim=1)*/
    public double avg;
    /**the variance (dim=1)*/
    public double var;
    /**the first moment (dim>1)*/
    double[] av;
    /**the second moment (dim>1)*/
    double[] cov;

    /**
         @param dim the dimension of the points to be considered. Starts recursion.*/
    public RecMom(int dim) {
        if (dim < 1) General.error("RecMom: dim=" + dim);
        this.dim = dim;
        this.npoints = 0;
        this.swgt = 0.;
        if (dim == 1) {
            this.avg = 0.;
            this.var = 0.;
        } else {
            this.av = new double[dim];
            this.cov = new double[dim * (dim + 1) / 2];
            int k = 0;
            for (int i = 0; i < dim; i++) {
                this.av[i] = 0.;
                for (int j = 0; j < i + 1; j++) {
                    this.cov[k] = 0.;
                    k++;
                }
            }
        }
    }

    /**@param p1dim the weighted 1dim point
         @param wgt its weight*/
    public void recalc(double p1dim, double wgt) {
        npoints++;
        double wold = swgt;
        swgt += wgt;
        var = wold * var / swgt + (wold * wgt) / (swgt * swgt) *
                (p1dim * p1dim + avg * avg - 2.0 * p1dim * avg);
        avg = (wold * avg + wgt * p1dim) / swgt;
    }



    /**@param pt the unweighted point of dim&gt;1
         @param wgt its weight*/
    public void recalc(double[] pt, double wgt) {
        if (dim != pt.length)
            General.error("RecMom: dim's=" + dim + " " + pt.length);
        npoints++;
        double wold = swgt;
        swgt += wgt;
        int k = 0;
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < i + 1; j++) {
                cov[k] = wold * cov[k] / swgt + (wold * wgt) / (swgt * swgt) *
                        (pt[i] * pt[j] + av[i] * av[j] -
                        pt[i] * av[j] - av[i] * pt[j]);
                k++;
            }
        }
        for (int i = 0; i < dim; i++) av[i] = (wold * av[i] + wgt * pt[i]) / swgt;
    }

    /**get the moments (for dim=1 they can be accessed directly)
         @return the actual moments*/
    public FirstMoments get() {
        FirstMoments res = new FirstMoments(this.dim);
        res.npoints = npoints;
        res.swgt = swgt;
        if (dim == 1) {
            res.avg[0] = avg;
            res.cov[0][0] = var;
        } else {
            int k = 0;
            for (int i = 0; i < dim; i++) {
                res.avg[i] = av[i];
                for (int j = 0; j < i + 1; j++) {
                    res.cov[i][j] = cov[k];
                    res.cov[j][i] = cov[k];
                    k++;
                }
            }
        }
        return res;
    }
}
