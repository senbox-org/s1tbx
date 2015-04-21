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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Expectation maximization (EM) cluster algorithm.
 * <p>
 * todo - make algorithm use a point acessor instead of a point array
 * todo - revise API to reduce the number of fields
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class EMClusterer {

    private final int pointCount;
    private final int dimensionCount;
    private final int clusterCount;

    private final double[][] points;

    // prior cluster probabilities
    private final double[] priors;
    // cluster means
    private final double[][] means;
    // cluster covariances
    private final double[][][] covariances;
    // cluster distributions
    private final Distribution[] distributions;

    // strategy for calculating posterior cluster probabilities
    private final ProbabilityCalculator calculator;

    /**
     * Creates a probability calculator for a set of clusters.
     *
     * @param clusters the set of clusters.
     *
     * @return the probability calculator.
     */
    static ProbabilityCalculator createProbabilityCalculator(final EMCluster[] clusters) {
        final Distribution[] distributions = new Distribution[clusters.length];
        final double[] priors = new double[clusters.length];

        for (int i = 0; i < clusters.length; i++) {
            distributions[i] = new MultinormalDistribution(clusters[i].getMean(), clusters[i].getCovariances());
            priors[i] = clusters[i].getPriorProbability();
        }

        return new ProbabilityCalculator(distributions, priors);
    }

    /**
     * Finds a collection of clusters for a given set of data points.
     *
     * @param points         the data points.
     * @param clusterCount   the number of clusters.
     * @param iterationCount the number of EM iterations to be made.
     * @param randomSeed     the seed used to initialize the cluster algorithm
     *
     * @return the cluster decomposition.
     */
    static EMCluster[] findClusters(double[][] points, int clusterCount, int iterationCount, int randomSeed) {
        return new EMClusterer(points, clusterCount, randomSeed).findClusters(iterationCount);
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param points       the data points.
     * @param clusterCount the number of clusters.
     * @param randomSeed   the seed used to initialize the cluster algorithm
     */
    EMClusterer(double[][] points, int clusterCount, int randomSeed) {
        pointCount = points.length;
        dimensionCount = points[0].length;

        this.points = points;
        this.clusterCount = clusterCount;

        priors = new double[clusterCount];

        means = new double[clusterCount][dimensionCount];
        covariances = new double[clusterCount][dimensionCount][dimensionCount];
        distributions = new MultinormalDistribution[clusterCount];
        calculator = new ProbabilityCalculator(distributions, priors);

        initialize(new Random(randomSeed));
    }

    /**
     * Finds a collection of clusters.
     *
     * @param iterationCount the number of EM iterations to be made.
     *
     * @return the cluster decomposition.
     */
    private EMCluster[] findClusters(int iterationCount) {
        while (iterationCount > 0) {
            iterate();
            iterationCount--;
            // todo - logging
        }

        return getClusters();
    }

    /**
     * Returns the clusters found.
     *
     * @return the clusters found.
     */
    EMCluster[] getClusters() {
        return getClusters(new PriorProbabilityClusterComparator());
    }

    EMCluster[] getClusters(Comparator<EMCluster> clusterComparator) {
        final EMCluster[] clusters = new EMCluster[clusterCount];

        for (int k = 0; k < clusterCount; ++k) {
            clusters[k] = new EMCluster(means[k], covariances[k], priors[k]);
        }
        Arrays.sort(clusters, clusterComparator);

        return clusters;
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
            priors[k] = 1.0; // same prior probability for all clusters

            for (int l = 0; l < dimensionCount; ++l) {
                // initialization of diagonal elements with unity comes close
                // to an initial run with the k-means algorithm
                covariances[k][l][l] = 1.0;
            }

            distributions[k] = new MultinormalDistribution(means[k], covariances[k]);
        }
    }

    /**
     * Carries out a single EM iteration.
     */
    void iterate() {
        final double[] sums = new double[clusterCount];
        final double[] posteriors = new double[clusterCount];

        for (int i = 0; i < pointCount; ++i) {
            calculator.calculate(points[i], posteriors);

            // ensure non-zero probabilities for all clusters to prevent the
            // covariance matrixes from becoming singular
            double sum = 0.0;
            for (int k = 0; k < clusterCount; ++k) {
                posteriors[k] += 1.0E-4;
                sum += posteriors[k];
            }
            for (int k = 0; k < clusterCount; ++k) {
                posteriors[k] /= sum;
            }

            // calculate cluster means and covariances in a single pass
            // D. H. D. West (1979, Communications of the ACM, 22, 532)
            if (i == 0) {
                for (int k = 0; k < clusterCount; ++k) {
                    for (int l = 0; l < dimensionCount; ++l) {
                        means[k][l] = points[0][l];
                        for (int m = l; m < dimensionCount; ++m) {
                            covariances[k][l][m] = 0.0;
                        }
                    }
                    sums[k] = posteriors[k];
                }
            } else {
                for (int k = 0; k < clusterCount; ++k) {
                    final double temp = posteriors[k] + sums[k];

                    for (int l = 0; l < dimensionCount; ++l) {
                        for (int m = l; m < dimensionCount; ++m) {
                            covariances[k][l][m] += sums[k] * posteriors[k] * (points[i][l] - means[k][l]) * (points[i][m] - means[k][m]) / temp;
                        }
                        means[k][l] += posteriors[k] * (points[i][l] - means[k][l]) / temp;
                    }

                    sums[k] = temp;
                }
            }
        }

        for (int k = 0; k < clusterCount; ++k) {
            for (int l = 0; l < dimensionCount; ++l) {
                for (int m = l; m < dimensionCount; ++m) {
                    covariances[k][l][m] /= sums[k];
                    covariances[k][m][l] = covariances[k][l][m];
                }
            }

            priors[k] = sums[k] / pointCount;
            distributions[k] = new MultinormalDistribution(means[k], covariances[k]);
        }
    }

    /**
     * Cluster comparator.
     * <p>
     * Compares two clusters according to their prior probability.
     */
    private static class PriorProbabilityClusterComparator implements Comparator<EMCluster> {

        @Override
        public int compare(EMCluster c1, EMCluster c2) {
            return Double.compare(c2.getPriorProbability(), c1.getPriorProbability());
        }
    }
}
