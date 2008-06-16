package org.esa.beam.cluster;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;/*
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

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class KMeansClusterer {


    private final int pointCount;
    private final int dimensionCount;
    private final double[][] points;
    private final int clusterCount;

    private final double[][] means;
    private int[] h;

    /**
     * Finds a collection of clusters for a given set of data points.
     *
     * @param points         the data points.
     * @param clusterCount   the number of clusters.
     * @param iterationCount the number of EM iterations to be made.
     *
     * @return the cluster decomposition.
     */
    public static ClusterSet findClusters(double[][] points, int clusterCount, int iterationCount) {
        return new KMeansClusterer(points, clusterCount).findClusters(iterationCount);
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param points       the data points.
     * @param clusterCount the number of clusters.
     */
    public KMeansClusterer(double[][] points, int clusterCount) {
        this(points, clusterCount, 0.0);
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param points       the data points.
     * @param clusterCount the number of clusters.
     * @param dist         the minimum distance to be exceeded by any pair of initial clusters.
     */
    public KMeansClusterer(double[][] points, int clusterCount, double dist) {
        this(points, clusterCount, dist, 31415);
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param points       the data points.
     * @param clusterCount the number of clusters.
     * @param dist         the minimum distance to be exceeded by any pair of initial clusters.
     * @param seed         the seed used for the random initialization of clusters.
     */
    public KMeansClusterer(double[][] points, int clusterCount, double dist, int seed) {
        this(points.length, points[0].length, points, clusterCount, seed);
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param pointCount     the number of data points.
     * @param dimensionCount the number of dimension in point space.
     * @param points         the data points.
     * @param clusterCount   the number of clusters.
     * @param seed           the seed used for the random initialization of clusters.
     */
    private KMeansClusterer(int pointCount, int dimensionCount, double[][] points, int clusterCount,
                            int seed) {
        // todo: check arguments

        this.pointCount = pointCount;
        this.dimensionCount = dimensionCount;
        this.points = points;
        this.clusterCount = clusterCount;

        means = new double[clusterCount][dimensionCount];

        initialize(seed);
    }

    /**
     * Finds a collection of clusters.
     *
     * @param iterationCount the number of EM iterations to be made.
     *
     * @return the cluster decomposition.
     */
    private ClusterSet findClusters(int iterationCount) {
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
    public ClusterSet getClusters() {
        return getClusters(new DefaultClusterComparator());
    }

    public ClusterSet getClusters(Comparator<Cluster> clusterComparator) {
        final Cluster[] clusters = new Cluster[clusterCount];
        for (int k = 0; k < clusterCount; ++k) {
            clusters[k] = new KMeansCluster(means[k]);
        }
        Arrays.sort(clusters, clusterComparator);

        return new ClusterSet(clusters);
    }

    /**
     * Randomly initializes the clusters using the k-means method.
     *
     * @param seed the seed value used for initializing the random number generator.
     */
    private void initialize(int seed) {
        final Random random = new Random(seed);

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

    /**
     * Cluster comparator.
     */
    private static class DefaultClusterComparator implements Comparator<Cluster> {

        public int compare(Cluster c1, Cluster c2) {
            return Double.compare(c2.getPriorProbability(), c1.getPriorProbability());
        }
    }
}
