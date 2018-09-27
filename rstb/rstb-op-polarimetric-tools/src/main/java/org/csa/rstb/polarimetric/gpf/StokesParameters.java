/*
 * Copyright (C) 2018 Skywatch Space Applications Inc. https://www.skywatch.co
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
package org.csa.rstb.polarimetric.gpf;

import org.apache.commons.math3.util.FastMath;

public class StokesParameters {

    public double DegreeOfPolarization;
    public double DegreeOfDepolarization;
    public double DegreeOfCircularity;
    public double DegreeOfEllipticity;
    public double circularPolarizationRatio;
    public double linearPolarizationRatio;
    public double RelativePhase;
    public double Alphas;
    public double Conformity;
    public double PhasePhi;


    /**
     * Compute Stokes parameters for given 4x1 compact pol Stokes vector.
     *
     * @param g           Stokes vector
     * @param compactMode RCH or LCH compact mode
     * @return The Stokes parameters.
     */
    public static StokesParameters computeStokesParameters(
            final double[] g, final String compactMode, final boolean useRCMConvention) {

        final StokesParameters parameters = new StokesParameters();

        parameters.DegreeOfPolarization = Math.sqrt(g[1] * g[1] + g[2] * g[2] + g[3] * g[3]) / g[0];
        parameters.DegreeOfDepolarization = 1 - parameters.DegreeOfPolarization;

        if (compactMode.equals(CompactPolProcessor.lch) && !useRCMConvention ||
                compactMode.equals(CompactPolProcessor.rch) && useRCMConvention) {

            parameters.DegreeOfCircularity = -g[3] / (g[0] * parameters.DegreeOfPolarization);
            parameters.circularPolarizationRatio = (g[0] - g[3]) / (g[0] + g[3]);
            parameters.RelativePhase = Math.atan2(-g[3], g[2]);
            parameters.Alphas = 0.5 * Math.atan2(Math.sqrt(g[1] * g[1] + g[2] * g[2]), g[3]);
            parameters.Conformity = g[3] / g[0];
            parameters.PhasePhi = Math.atan2(g[2], g[1]);

        } else {
            //  Right Circular Hybrid Mode
            parameters.DegreeOfCircularity = g[3] / (g[0] * parameters.DegreeOfPolarization);
            parameters.circularPolarizationRatio = (g[0] + g[3]) / (g[0] - g[3]);
            parameters.RelativePhase = Math.atan2(g[3], g[2]);
            parameters.Alphas = 0.5 * Math.atan2(Math.sqrt(g[1] * g[1] + g[2] * g[2]), -g[3]);
            parameters.Conformity = -g[3] / g[0];
            parameters.PhasePhi = Math.atan2(-g[2], g[1]);
        }

        parameters.DegreeOfEllipticity = FastMath.tan(0.5 * FastMath.asin(parameters.DegreeOfCircularity));
        parameters.linearPolarizationRatio = (g[0] - g[1]) / (g[0] + g[1]);

        return parameters;
    }

    /**
     * Compute 4x1 compact pol Stokes vector for given mean covariance matrix C2.
     *
     * @param Cr Real part of the mean covariance matrix.
     * @param Ci Imaginary part of the mean covariance matrix.
     * @param g  Stokes vector.
     */
    public static void computeCompactPolStokesVector(final double[][] Cr, final double[][] Ci, final double[] g) {

        g[0] = Cr[0][0] + Cr[1][1];
        g[1] = Cr[0][0] - Cr[1][1];
        g[2] = 2 * Cr[0][1];
        g[3] = -2 * Ci[0][1];
    }

    /**
     * Compute 4x1 compact pol Stokes vector for given 2x1 complex scatter vector.
     *
     * @param kr Real part of the scatter vector
     * @param ki Imaginary part of the scatter vector
     * @param g  The Stokes vector
     */
    public static void computeCompactPolStokesVector(final double[] kr, final double[] ki, final double[] g) {

        g[0] = kr[0] * kr[0] + ki[0] * ki[0] + kr[1] * kr[1] + ki[1] * ki[1];
        g[1] = kr[0] * kr[0] + ki[0] * ki[0] - kr[1] * kr[1] - ki[1] * ki[1];
        g[2] = 2 * (kr[0] * kr[1] + ki[0] * ki[1]);
        g[3] = -2 * (ki[0] * kr[0] - kr[0] * ki[1]);
    }

    /**
     * Compute degree of polarization for given 4x1 compact pol Stokes vector.
     *
     * @param g Stokes vector
     * @return degree of polarization
     */
    public static double computeDegreeOfPolarization(final double[] g) {

        if (g[0] == 0.0) {
            return -1;
        }
        return Math.sqrt(g[1] * g[1] + g[2] * g[2] + g[3] * g[3]) / g[0];
    }
}
