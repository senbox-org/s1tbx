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

//import          java.awt.*;

import java.io.PrintWriter;

//import          java.util.*;

/**
 *is the set of zeroth, first and second moments of a weighted set of points<p>
 *@author H. Schiller / GKSS
 */
public class FirstMoments {

    /**the dimension of the points*/
    public int dim;
    /**the number of points*/
    public long npoints;
    /**their sum of weights*/
    public double swgt;
    /**avg[dim] the averages of the components*/
    public double[] avg;
    /**cov[dim][dim] the covariance matrix*/
    public double[][] cov;

    /**the moments of an empty data set
         @param dim the dimension of these not yet existing points*/
    public FirstMoments(int dim) {
        this.npoints = 0;
        this.dim = dim;
        this.swgt = 0.;
        this.avg = new double[dim];
        this.cov = new double[dim][dim];
        for (int i = 0; i < dim; i++) {
            this.avg[i] = 0.;
            for (int j = 0; j < dim; j++) {
                this.cov[i][j] = 0.;
            }
        }
    }

    /**combines this set of moments with that
         @param that moments are combined with this
         @return the combined moments*/
    public FirstMoments combine(FirstMoments that) {
        if (this.dim != that.dim) {
            throw new IllegalArgumentException("FirstMoments.combine: dim's differ: " +
                     this.dim + " " + that.dim);
        }
        FirstMoments res = new FirstMoments(this.dim);
        res.npoints = this.npoints + that.npoints;
        res.swgt = this.swgt + that.swgt;
        double rho = this.swgt / res.swgt;
        double omr = 1.0 - rho;
        for (int i = 0; i < this.dim; i++) {
            res.avg[i] = rho * this.avg[i] + omr * that.avg[i];
            for (int j = 0; j < this.dim; j++) {
                res.cov[i][j] =
                        rho * this.cov[i][j] +
                        omr * that.cov[i][j] +
                        rho * omr * (this.avg[i] - that.avg[i]) *
                        (this.avg[j] - that.avg[j]);
            }
        }
        return res;
    }
}
