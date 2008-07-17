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

import static java.lang.Math.exp;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class EMClusterSet {

    private final EMCluster[] clusters;

    public EMClusterSet(EMCluster[] clusters) {
        this.clusters = clusters;
    }

    public double[] getPosteriorProbabilities(double[] point) {
        final double[] h = new double[clusters.length];
        // this code is duplicated in EMClusterer.stepE()
        int underflowCount = 0;
        for (int k = 0; k < clusters.length; ++k) {
            h[k] = clusters[k].getPriorProbability() * clusters[k].getProbabilityDensity(point);
            if (h[k] == 0.0) {
                underflowCount++;
            }
        }
        // numerical underflow - compute probabilities using logarithm
        if (underflowCount == clusters.length) {
            final double[] sums = new double[clusters.length];
            for (int k = 0; k < clusters.length; ++k) {
                h[k] = clusters[k].getLogProbabilityDensity(point);
            }
            for (int k = 0; k < clusters.length; ++k) {
                for (int l = 0; l < clusters.length; ++l) {
                    if (l != k) {
                        sums[k] += (clusters[l].getPriorProbability() / clusters[k].getPriorProbability()) *
                                   exp(h[l] - h[k]);
                    }
                }
            }
            for (int k = 0; k < clusters.length; ++k) {
                h[k] = 1.0 / (1.0 + sums[k]);
            }
        }

        return normalizeProbabilities(h);
    }

    public final int getClusterCount() {
        return clusters.length;
    }

    public double[] getMean(int i) {
        return clusters[i].getMean();
    }

    public final EMCluster getEMCluster(int index) {
        return clusters[index];
    }

    private double[] normalizeProbabilities(double[] h) {
        double sum = 0.0;
        for (int k = 0; k < clusters.length; ++k) {
            sum += h[k];
        }
        for (int k = 0; k < clusters.length; ++k) {
            h[k] /= sum;
        }
        return h;
    }

}
