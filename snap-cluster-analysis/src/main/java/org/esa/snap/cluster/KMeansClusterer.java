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

/**
 * K-means clustering algorithm.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class KMeansClusterer {

    private final int dimensionCount;
    private final int clusterCount;
    private final double[][] means;
    private final int[] memberCounts;
    private double[][] sums;

    /**
     * Constructs a new instance of this class.
     *
     * @param clusterCount      the number of clusters.
     * @param dimensionCount    the number of dimensions.
     */
    KMeansClusterer(int clusterCount, int dimensionCount) {
        this.clusterCount = clusterCount;
        this.dimensionCount = dimensionCount;
        this.memberCounts = new int[clusterCount];
        this.means = new double[clusterCount][dimensionCount];
    }
    
    void initialize(RandomSceneIter sceneIter) {
        for (int c = 0; c < clusterCount; ++c) {
            boolean accepted = true;
            do {
                means[c] = sceneIter.getNextValue();
                for (int i = 0; i < c; ++i) {
                    accepted = !Arrays.equals(means[c], means[i]);
                    if (!accepted) {
                        break;
                    }
                }
            } while (!accepted);
        }
    }
    
    void startIteration() {
        sums = new double[clusterCount][dimensionCount];
        Arrays.fill(memberCounts, 0);
    }
    
    void iterateTile(PixelIter iter) {
        final double[] point = new double[dimensionCount];

        while (iter.next(point) != null) {
            final int closestCluster = getClosestCluster(means, point);
            final double[] sumsOfClosestCluster = sums[closestCluster];
            for (int d = 0; d < dimensionCount; ++d) {
                sumsOfClosestCluster[d] += point[d];
            }
            memberCounts[closestCluster]++;
        }
    }
    
    boolean endIteration() {
        double diff = 0;
        for (int c = 0; c < clusterCount; ++c) {
            final double[] sumsOfC = sums[c];
            final double[] meansOfC = means[c];
            for (int d = 0; d < dimensionCount; ++d) {
                if (memberCounts[c] > 0) {
                    final double newMean = sumsOfC[d] / memberCounts[c];
                    diff += (newMean- meansOfC[d])*(newMean- meansOfC[d]);
                    meansOfC[d] = newMean;
                }
            }
        }
        return diff == 0.0;
    }

    /**
     * Returns the clusters found.
     *
     * @return the clusters found.
     */
    KMeansClusterSet getClusters() {
        final KMeansCluster[] clusters = new KMeansCluster[clusterCount];
        for (int c = 0; c < clusterCount; ++c) {
            clusters[c] = new KMeansCluster(means[c], memberCounts[c]);
        }
        Arrays.sort(clusters, new ClusterComparator());
        return new KMeansClusterSet(clusters);
    }
    
    static int getClosestCluster(double[][] mean, final double[] point) {
        double minDistance = Double.MAX_VALUE;
        int closestCluster = 0;
        for (int c = 0; c < mean.length; ++c) {
            final double distance = squaredDistance(mean[c], point);
            if (distance < minDistance) {
                closestCluster = c;
                minDistance = distance;
            }
        }
        return closestCluster;
    }

    /**
     * Distance measure used by the k-means method.
     *
     * @param x a point.
     * @param y a point.
     *
     * @return squared Euclidean distance between x and y.
     */
    private static double squaredDistance(double[] x, double[] y) {
        double distance = 0.0;
        for (int d = 0; d < x.length; ++d) {
            final double difference = y[d] - x[d];
            distance += difference * difference;
        }

        return distance;
    }
    
    /**
     * Cluster comparator.
     * <p>
     * Compares two clusters according to their membership counts.
     */
    private static class ClusterComparator implements Comparator<KMeansCluster> {
        @Override
        public int compare(KMeansCluster c1, KMeansCluster c2) {
            return Double.compare(c2.getMemberCount(), c1.getMemberCount());
        }
    }
}
