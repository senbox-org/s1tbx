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
package org.esa.snap.cluster;


/**
 * Cluster class.
 *
 * @author Ralf Quast
 * @version $Revision $ $Date $
 */
public class EMCluster {

    private final double[] mean;
    private final double[][] covariances;
    private final double priorProbability;

    public EMCluster(double[] mean, double[][] covariances, double priorProbability) {
        this.mean = mean;
        this.covariances = covariances;
        this.priorProbability = priorProbability;
    }

    public final double getPriorProbability() {
        return priorProbability;
    }

    public final double[] getMean() {
        return mean.clone();
    }

    public final double getMean(int i) {
        return mean[i];
    }

    public final double[][] getCovariances() {
        final double[][] covariances = this.covariances.clone();

        for (int i = 0; i < covariances.length; i++) {
            covariances[i] = covariances[i].clone();
        }

        return covariances;
    }

    public final double getCovariance(int i, int j) {
        return covariances[i][j];
    }
}
