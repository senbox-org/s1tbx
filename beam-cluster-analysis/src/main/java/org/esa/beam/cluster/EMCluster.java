/*
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.cluster;


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
