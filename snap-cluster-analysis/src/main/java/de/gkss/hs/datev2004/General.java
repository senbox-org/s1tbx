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

import java.util.Random;

/**
 * A collection of useful general stuff
 * @author H. Schiller / GKSS
 */
public final class General {


    /**exits after printing
         @param msg the message*/
    public static void error(String msg) {
   throw new IllegalStateException(msg);
    }

    public static double eucliddist(double[] v1, double[] v2) {
        double res = 0.;
        for (int i = 0; i < v1.length; i++){
            res += (v1[i] - v2[i]) * (v1[i] - v2[i]);
        }
        return Math.sqrt(res);
    }

    /**
         @return -1 if argument has length zero*/
    public static int indmin(double[] a) {
        if (a.length == 0) return -1;
        int res = 0;
        double min = a[0];
        for (int i = 1; i < a.length; i++) {
            if (a[i] < min) {
                min = a[i];
                res = i;
            }
        }
        return res;
    }

    /**
         @return -1 if argument has length zero*/
    public static int indmax(double[] a) {
        if (a.length == 0) return -1;
        int res = 0;
        double max = a[0];
        for (int i = 1; i < a.length; i++) {
            if (a[i] > max) {
                max = a[i];
                res = i;
            }
        }
        return res;
    }

    /**
         @return -1 if argument has length zero*/
    public static int indmax(long[] a) {
        if (a.length == 0) return -1;
        int res = 0;
        long max = a[0];
        for (int i = 1; i < a.length; i++) {
            if (a[i] > max) {
                max = a[i];
                res = i;
            }
        }
        return res;
    }

    /**@param dim the dimension of the vector
         @param gen seeded to users needs
         @return a normalized randomly oriented vector*/
    public static double[] vector_rand(int dim, Random gen) {
        double[] res = new double[dim];
        double sum = 0.;
        for (int i = 0; i < dim; i++) {
            res[i] = gen.nextGaussian();
            sum += res[i] * res[i];
        }
        sum = 1.0 / Math.sqrt(sum);
        for (int i = 0; i < dim; i++) res[i] *= sum;
        return res;
    }

    /**@param dim the dimension of the space
         @param gen seeded to meet users needs
         @return two normalized vectors <code>[2][dim]</code> spanning a random plane*/
    public static double[][] plane_rand(int dim, Random gen) {
        if (dim < 3) error("plane_rand called with dim=" + dim);
        double[][] res = new double[2][dim];
        res[0] = vector_rand(dim, gen);
        boolean collinear = true;
        double sum = 0., a, b;
        while (collinear) {
            res[1] = vector_rand(dim, gen);
            sum = 0.0;
            for (int i = 0; i < dim; i++) sum += res[0][i] * res[1][i];
            if (sum * sum < 0.98) collinear = false;
        }
        b = Math.sqrt(1.0 / (1.0 - sum * sum));
        a = -b * sum;
        for (int i = 0; i < dim; i++) res[1][i] = a * res[0][i] + b * res[1][i];
        return res;
    }
}
