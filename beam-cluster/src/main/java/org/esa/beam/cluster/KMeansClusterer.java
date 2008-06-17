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
import java.util.Comparator;
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
    private int[] memberCounts;

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
        this.points = points;
        this.clusterCount = clusterCount;
        pointCount = points.length;
        dimensionCount = points[0].length;
        memberCounts = new int[clusterCount];
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
        Arrays.fill(memberCounts, 0);
        for (int p = 0; p < pointCount; ++p) {
            double minDistance = Double.MAX_VALUE;
            int closestCluster = 0;
            for (int c = 0; c < clusterCount; ++c) {
                final double distance = squaredDistance(means[c], points[p]);
                if (distance < minDistance) {
                    closestCluster = c;
                    minDistance = distance;
                }
            }
            for (int l = 0; l < dimensionCount; ++l) {
                newMeans[closestCluster][l] += points[p][l];
            }
            memberCounts[closestCluster]++;
        }
        for (int c = 0; c < clusterCount; ++c) {
            for (int d = 0; d < dimensionCount; ++d) {
                if (memberCounts[c] > 0) {
                    means[c][d] = newMeans[c][d] / memberCounts[c];
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
        for (int c = 0; c < clusterCount; ++c) {
            clusters[c] = new KMeansCluster(means[c], memberCounts[c]);
        }
        Arrays.sort(clusters, new ClusterComparator());
        return new KMeansClusterSet(clusters);
    }

    /**
     * Randomly initializes the clusters using the k-means method.
     *
     * @param random the random number generator used for initialization.
     */
    private void initialize(Random random) {
        for (int c = 0; c < clusterCount; ++c) {
            boolean accepted = true;
            do {
                System.arraycopy(points[random.nextInt(pointCount)], 0, means[c], 0, dimensionCount);
                for (int i = 0; i < c; ++i) {
                    accepted = !Arrays.equals(means[c], means[i]);
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
        double distance = 0.0;
        for (int d = 0; d < dimensionCount; ++d) {
            distance += (y[d] - x[d]) * (y[d] - x[d]);
        }

        return distance;
    }
    
    /**
     * Cluster comparator.
     * <p/>
     * Compares two clusters according to their membershipCounts.
     */
    private static class ClusterComparator implements Comparator<KMeansCluster> {

        public int compare(KMeansCluster c1, KMeansCluster c2) {
            int thisVal = c1.getMemberCount();
            int anotherVal = c2.getMemberCount();
            return (thisVal<anotherVal ? -1 : (thisVal==anotherVal ? 0 : 1));
        }
    }
}
