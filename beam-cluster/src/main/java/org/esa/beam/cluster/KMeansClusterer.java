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

import java.util.Arrays;
import java.util.Random;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class KMeansClusterer {

    private static final int SEED = 31415;
    private final int pointCount;
    private final int dimensionCount;
    private final double[][] points;
    private final int clusterCount;

    private final double[][] means;

    /**
     * Finds a collection of clusters for a given set of data points.
     *
     * @param points         the data points.
     * @param clusterCount   the number of clusters.
     * @param iterationCount the number of EM iterations to be made.
     *
     * @return the cluster decomposition.
     */
    public static KMeansClusterSet findClusters(double[][] points, int clusterCount, int iterationCount) {
        return new KMeansClusterer(points, clusterCount).findClusters(iterationCount);
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param points       the data points.
     * @param clusterCount the number of clusters.
     */
    public KMeansClusterer(double[][] points, int clusterCount) {
        // todo: check arguments

        pointCount = points.length;
        dimensionCount = points[0].length;
        this.points = points;
        this.clusterCount = clusterCount;

        means = new double[clusterCount][dimensionCount];

        initialize(new Random(SEED));
    }

    /**
     * Finds a collection of clusters.
     *
     * @param iterationCount the number of EM iterations to be made.
     *
     * @return the cluster decomposition.
     */
    private KMeansClusterSet findClusters(int iterationCount) {
        while (iterationCount > 0) {
            iterate();
            iterationCount--;
        }

        return getClusters();
    }

    /**
     * Carries out a single EM iteration.
     * todo - make private when observer notifications implemented
     */
    public void iterate() {
        final double[][] newMeans = new double[clusterCount][dimensionCount];
        final int[] memberCount = new int[clusterCount];

        for (int i = 0; i < pointCount; ++i) {
            double minDistance = Double.MAX_VALUE;
            int closestCluster = 0;
            for (int k = 0; k < clusterCount; ++k) {
                final double distance = squaredDistance(means[k], points[i]);
                if (distance < minDistance) {
                    closestCluster = k;
                    minDistance = distance;
                }
            }
            for (int l = 0; l < dimensionCount; ++l) {
                newMeans[closestCluster][l] += points[i][l];
            }
            memberCount[closestCluster]++;
        }
        for (int k = 0; k < clusterCount; ++k) {
            for (int l = 0; l < dimensionCount; ++l) {
                if (memberCount[k] > 0) {
                    means[k][l] = newMeans[k][l] / memberCount[k];
                }
            }
        }
    }

    /**
     * Returns the clusters found.
     * todo - make private when observer notifications implemented
     *
     * @return the clusters found.
     */

    public KMeansClusterSet getClusters() {
        final KMeansCluster[] clusters = new KMeansCluster[clusterCount];
        for (int k = 0; k < clusterCount; ++k) {
            clusters[k] = new KMeansCluster(means[k]);
        }
        return new KMeansClusterSet(clusters);
    }

    /**
     * Randomly initializes the clusters using the k-means method.
     *
     * @param random the random number generator used for initialization.
     */
    private void initialize(Random random) {
        for (int k = 0; k < clusterCount; ++k) {
            boolean accepted = true;
            do {
                System.arraycopy(points[random.nextInt(pointCount)], 0, means[k], 0, dimensionCount);
                for (int i = 0; i < k; ++i) {
                    accepted = !Arrays.equals(means[k], means[i]);
                    if (!accepted) {
                        break;
                    }
                }
            } while (!accepted);
        }
    }

    /**
     * Distance measure used by the k-means method.
     *
     * @param x a point.
     * @param y a point.
     *
     * @return squared Euclidean distance between x and y.
     */
    private double squaredDistance(double[] x, double[] y) {
        double d = 0.0;
        for (int l = 0; l < dimensionCount; ++l) {
            d += (y[l] - x[l]) * (y[l] - x[l]);
        }

        return d;
    }
}
