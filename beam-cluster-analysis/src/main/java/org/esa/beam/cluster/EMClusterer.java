/* Copyright (C) 2002-2008 by Brockmann Consult
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Expectation maximization (EM) cluster algorithm.
 * <p/>
 * todo - observer notifications
 * todo - make algorithm use tiles
 *
 * @author Ralf Quast
 * @version $Revision: 2221 $ $Date: 2008-06-16 11:19:52 +0200 (Mo, 16 Jun 2008) $
 */
public class EMClusterer {

    private final int pointCount;
    private final int dimensionCount;
    private final double[][] points;
    private final int clusterCount;

    private final double[] p;
    private final double[][] h;
    private final double[][] means;
    private final double[][][] covariances;
    private final Distribution[] distributions;

    /**
     * Finds a collection of clusters for a given set of data points.
     *
     * @param points         the data points.
     * @param clusterCount   the number of clusters.
     * @param iterationCount the number of EM iterations to be made.
     *
     * @return the cluster decomposition.
     */
    public static EMClusterSet findClusters(double[][] points, int clusterCount, int iterationCount, int randomSeed) {
        return new EMClusterer(points, clusterCount, randomSeed).findClusters(iterationCount);
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param points       the data points.
     * @param clusterCount the number of clusters.
     */
    public EMClusterer(double[][] points, int clusterCount, int randomSeed) {
        pointCount = points.length;
        dimensionCount = points[0].length;
        this.points = points;
        this.clusterCount = clusterCount;

        p = new double[clusterCount];
        h = new double[clusterCount][pointCount];
        means = new double[clusterCount][dimensionCount];
        covariances = new double[clusterCount][dimensionCount][dimensionCount];
        distributions = new Distribution[clusterCount];

        initialize(new Random(randomSeed));
    }

    /**
     * Finds a collection of clusters.
     *
     * @param iterationCount the number of EM iterations to be made.
     *
     * @return the cluster decomposition.
     */
    private EMClusterSet findClusters(int iterationCount) {
        while (iterationCount > 0) {
            iterate();
            iterationCount--;
            // todo - notify observer
        }

        return getClusters();
    }

    /**
     * Carries out a single EM iteration.
     */
    public void iterate() {
        stepE();
        stepM();
    }

    /**
     * Returns the clusters found.
     * todo - make private when observer notifications implemented
     *
     * @return the clusters found.
     */
    public EMClusterSet getClusters() {
        return getClusters(new PriorProbabilityClusterComparator());
    }

    public EMClusterSet getClusters(Comparator<EMCluster> clusterComparator) {
        final EMCluster[] clusters = new EMCluster[clusterCount];
        for (int k = 0; k < clusterCount; ++k) {
            clusters[k] = new EMCluster(distributions[k], p[k]);
        }
        Arrays.sort(clusters, clusterComparator);

        return new EMClusterSet(clusters);
    }

    /**
     * Randomly initializes the clusters.
     *
     * @param random the random number generator used for initialization.
     */
    private void initialize(Random random) {
        for (int k = 0; k < clusterCount; ++k) {
            System.arraycopy(points[random.nextInt(pointCount)], 0, means[k], 0, dimensionCount);
        }

        for (int k = 0; k < clusterCount; ++k) {
            p[k] = 1.0;
            for (int l = 0; l < dimensionCount; ++l) {
                // initialization of diagonal elements with unity comes close
                // to an initial run with the k-means algorithm
                covariances[k][l][l] = 1.0;
            }

            distributions[k] = new MultinormalDistribution(means[k], covariances[k]);
        }
    }

    /**
     * Performs an E-step.
     */
    private void stepE() {
        for (int i = 0; i < pointCount; ++i) {
            double sum = 0.0;
            for (int k = 0; k < clusterCount; ++k) {
                h[k][i] = p[k] * distributions[k].probabilityDensity(points[i]);
                sum += h[k][i];
            }
            if (sum > 0.0) {
                for (int k = 0; k < h.length; ++k) {
                    final double t = h[k][i] / sum;
                    h[k][i] = t;
                }
            } else { // numerical underflow - recompute posterior cluster probabilities
                final double[] sums = new double[clusterCount];
                for (int k = 0; k < clusterCount; ++k) {
                    h[k][i] = distributions[k].logProbabilityDensity(points[i]);
                }
                for (int k = 0; k < clusterCount; ++k) {
                    for (int l = 0; l < clusterCount; ++l) {
                        if (l != k) {
                            sums[k] += (p[l] / p[k]) * exp(h[l][i] - h[k][i]);
                        }
                    }
                }
                for (int k = 0; k < clusterCount; ++k) {
                    final double t = 1.0 / (1.0 + sums[k]);
                    h[k][i] = t;
                }
            }
        }
    }

    /**
     * Performs an M-step.
     */
    private void stepM() {
        for (int k = 0; k < clusterCount; ++k) {
            p[k] = calculateMoments(h[k], means[k], covariances[k]);
            distributions[k] = new MultinormalDistribution(means[k], covariances[k]);
        }
    }

    /**
     * Calculates the statistical moments.
     *
     * @param h           the posterior probabilities associated with the data points.
     * @param mean        the mean of the data points.
     * @param covariances the covariances of the data points.
     *
     * @return the mean posterior probability.
     */
    private double calculateMoments(double[] h, double[] mean, double[][] covariances) {
        for (int k = 0; k < dimensionCount; ++k) {
            for (int l = k; l < dimensionCount; ++l) {
                covariances[k][l] = 0.0;
            }
            mean[k] = 0.0;
        }
        double sum = 0.0;
        for (int i = 0; i < pointCount; ++i) {
            for (int k = 0; k < dimensionCount; ++k) {
                mean[k] += h[i] * points[i][k];
            }
            sum += h[i];
        }
        for (int k = 0; k < dimensionCount; ++k) {
            mean[k] /= sum;
        }
        for (int i = 0; i < pointCount; ++i) {
            for (int k = 0; k < dimensionCount; ++k) {
                for (int l = k; l < dimensionCount; ++l) {
                    covariances[k][l] += h[i] * (points[i][k] - mean[k]) * (points[i][l] - mean[l]);
                }
            }
        }
        for (int k = 0; k < dimensionCount; ++k) {
            for (int l = k; l < dimensionCount; ++l) {
                covariances[k][l] /= sum;
                covariances[l][k] = covariances[k][l];
            }
        }

        return sum / pointCount;
    }

    /**
     * Cluster comparator.
     * <p/>
     * Compares two clusters according to their prior probability.
     */
    private static class PriorProbabilityClusterComparator implements Comparator<EMCluster> {

        public int compare(EMCluster c1, EMCluster c2) {
            return Double.compare(c2.getPriorProbability(), c1.getPriorProbability());
        }
    }
}
