/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.pfa.activelearning;

import org.esa.pfa.fe.op.Feature;
import org.esa.pfa.fe.op.Patch;

import java.util.ArrayList;
import java.util.List;


/**
 * Kernel K-means clustering algorithm [1].
 *
 * [1]. B. Demir, C. Persello, and L. Bruzzone, Batch mode active learning methods for the interactive
 *      classification of remote sensing images, IEEE Transactions on Geoscience and Remote Sensing,
 *      vol. 49, no.3, pp. 1014-1031, 2011.
 */

public class KernelKmeansClusterer {

    private int maxIterations = 0;
    private int numClusters = 0;
    private int numSamples = 0;
    private List<Patch> samples = new ArrayList<Patch>();
    private Cluster[] clusters = null;
    private SVM svmClassifier = null;
    private boolean debug = false;

	public KernelKmeansClusterer(final int maxIterations, final int numClusters, final SVM svmClassifier) {

        this.maxIterations = maxIterations;
        this.numClusters = numClusters;
        this.clusters = new Cluster[numClusters];
        this.svmClassifier = svmClassifier;
	}

    /**
     * Set samples for clustering.
     * @param uncertainSamples List of m most uncertain samples.
     */
    public void setData(final List<Patch> uncertainSamples) {

        this.numSamples = uncertainSamples.size();
        this.samples = uncertainSamples;
    }

    /**
     * Perform clustering using Kernel K-means clustering algorithm.
     * @throws Exception The exception.
     */
    public void clustering() throws Exception {

        setInitialClusterCenters();

        for (int i = 0; i < maxIterations; i++) {

            assignSamplesToClusters();

            if (debug) {
                System.out.println("Iteration: " + i);
                for (int clusterIdx = 0; clusterIdx < clusters.length; clusterIdx++) {
                    System.out.print("Cluster " + clusterIdx + ": ");
                    for (int sampleIdx : clusters[clusterIdx].memberSampleIndices) {
                        System.out.print(sampleIdx + ", ");
                    }
                    System.out.println();
                }
            }

            if (i != maxIterations - 1) {
                updateClusterCenters();

                if (debug) {
                    System.out.println("Updated cluster centers:");
                    for (int clusterIdx = 0; clusterIdx < clusters.length; clusterIdx++) {
                        System.out.println("Cluster " + clusterIdx + ": sample index " + clusters[clusterIdx].centerSampleIdx);
                    }
                }
            }
        }
    }

    /**
     * Initialize clusters with each has one randomly selected sample in it.
     */
    private void setInitialClusterCenters() {

        ArrayList<Integer> randomNumbers = new ArrayList<Integer>();
        int k = 0;
        while (k < numClusters) {
            final int idx = (int)(Math.random()*numSamples);
            if (!randomNumbers.contains(idx)) {
                randomNumbers.add(idx);
                clusters[k] = new Cluster();
                clusters[k].memberSampleIndices.add(idx);
                clusters[k].centerSampleIdx = idx;
                k++;
            }
        }

        if (debug) {
            System.out.println("Initial cluster centers:");
            for (int clusterIdx = 0; clusterIdx < clusters.length; clusterIdx++) {
                System.out.println("Cluster " + clusterIdx + ": sample index " + clusters[clusterIdx].centerSampleIdx);
            }
        }
    }

    /**
     * Assign samples to their near clusters base on Euclidean distance in kernel space.
     * @throws Exception The exception.
     */
    private void assignSamplesToClusters() throws Exception {

        for (int sampleIdx = 0; sampleIdx < numSamples; sampleIdx++) {
            if (!isClusterCenter(sampleIdx)) {
                final int clusterIdx = findNearestCluster(sampleIdx);
                clusters[clusterIdx].memberSampleIndices.add(sampleIdx);
            }
        }
    }

    /**
     * Determine if a given sample is the center of any cluster.
     * @param sampleIdx Index of the given sample.
     * @return True if the given sample is the center of some cluster, false otherwise.
     */
    private boolean isClusterCenter(final int sampleIdx) {

        for (int clusterIdx = 0; clusterIdx < numClusters; clusterIdx++) {
            if (sampleIdx == clusters[clusterIdx].centerSampleIdx) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find the nearest cluster for each given sample.
     * @param sampleIdx Index of the given sample.
     * @return Cluster index.
     * @throws Exception The exception.
     */
    private int findNearestCluster(final int sampleIdx) throws Exception {

        double minDistance = Double.MAX_VALUE;
        int nearestClusterIdx = 0;
        for (int clusterIdx = 0; clusterIdx < numClusters; clusterIdx++) {
            if (sampleIdx == clusters[clusterIdx].centerSampleIdx) {
                return clusterIdx;
            }

            final double distance = computeDistance(sampleIdx, clusters[clusterIdx].centerSampleIdx);
            if (distance < minDistance) {
                minDistance = distance;
                nearestClusterIdx = clusterIdx;
            }
        }

        return nearestClusterIdx;
    }

    /**
     * Compute kernel space distance between two given samples.
     * @param sampleIdx1 Index of the first sample.
     * @param sampleIdx2 Index of the second sample.
     * @return The kernel space distance.
     * @throws Exception The exception.
     */
    private double computeDistance(final int sampleIdx1, final int sampleIdx2) throws Exception {

        final double[] x1 = getFeatures(samples.get(sampleIdx1));
        final double[] x2 = getFeatures(samples.get(sampleIdx2));

        return svmClassifier.kernel(x1,x1) - 2*svmClassifier.kernel(x1,x2) + svmClassifier.kernel(x2,x2);
    }

    private static double[] getFeatures(final Patch patch) {

        final Feature[] features = patch.getFeatures();
        double[] featureArray = new double[features.length];
        int idx = 0;
        for (Feature feature:features) {
            featureArray[idx++] = Double.parseDouble(feature.getValue().toString());
        }
        return featureArray;
    }

    /**
     * Update centers of the clusters.
     * @throws Exception The exception.
     */
    private void updateClusterCenters() throws Exception {

        for (int clusterIdx = 0; clusterIdx < numClusters; clusterIdx++) {

            final double sum2 = computeSum2(clusters[clusterIdx].memberSampleIndices);

            double minDistance = Double.MAX_VALUE;
            int sampleIdx = 0;
            for (int i = 0; i < clusters[clusterIdx].memberSampleIndices.size(); i++) {

                final double distance = computeDistance(clusters[clusterIdx].memberSampleIndices.get(i),
                                                        clusters[clusterIdx].memberSampleIndices, sum2);

                if (distance < minDistance) {
                    minDistance = distance;
                    sampleIdx = clusters[clusterIdx].memberSampleIndices.get(i);
                }
            }

            clusters[clusterIdx].centerSampleIdx = sampleIdx;
            clusters[clusterIdx].memberSampleIndices.clear();
            clusters[clusterIdx].memberSampleIndices.add(sampleIdx);
        }
    }

    /**
     * For a cluster with n samples {xi, i=1,...,n}, this function computes the summation of K(xi,xj), i,j=1,...,n.
     * This summation will be used in the calculation of distance between a given sample and a given cluster.
     * @param memberSampleIndices The list of indices of samples in a given cluster.
     * @return The summation.
     * @throws Exception The exception.
     */
    private double computeSum2(final List<Integer> memberSampleIndices) throws Exception {

        final int numMembers = memberSampleIndices.size();
        double sum2 = 0.0;
        for (Integer memberSampleIndice : memberSampleIndices) {
            final double[] xi = getFeatures(samples.get(memberSampleIndice));
            for (Integer memberSampleIndice1 : memberSampleIndices) {
                final double[] xj = getFeatures(samples.get(memberSampleIndice1));
                sum2 += svmClassifier.kernel(xi, xj);
            }
        }
        return sum2;
    }

    /**
     * Compute the distance between a given sample and a given cluster.
     * @param sampleIdx The index of the given sample.
     * @param memberSampleIndices The list of indices of samples in the given cluster.
     * @param sum2 The summation computed by computeSum2 function.
     * @return The distance.
     * @throws Exception The exception.
     */
    private double computeDistance(final int sampleIdx, final List<Integer> memberSampleIndices, final double sum2)
            throws Exception {

        final int numSamples = memberSampleIndices.size();
        final double[] x = getFeatures(samples.get(sampleIdx));

        double sum1 = 0.0;
        for (Integer idx:memberSampleIndices) {
            final double[] xi = getFeatures(samples.get(idx));
            sum1 += svmClassifier.kernel(x,xi);
        }

        return svmClassifier.kernel(x,x) - 2.0*sum1/numSamples + sum2/(numSamples*numSamples);
    }

    /**
     * Get representatives of the clusters using density criterion.
     * @return patchIDs Array of IDs of the selected patches.
     * @throws Exception The exception.
     */
    public int[] getRepresentatives() throws Exception {

        int[] rep = new int[numClusters];
        int[] patchIDs = new int[numClusters];
        for (int i = 0; i < numClusters; i++) {
            int sampleIdx = findHighestDensitySample(clusters[i]);
            patchIDs[i] = samples.get(sampleIdx).getID();
            rep[i] = sampleIdx;
        }

        if (debug) {
            for (int clusterIdx = 0; clusterIdx < clusters.length; clusterIdx++) {
                System.out.println("Cluster " + clusterIdx + ": representative sample index " + rep[clusterIdx]);
            }
        }

        return patchIDs;
    }

    /**
     * Find the representative sample in a given cluster based on density criterion.
     * @param cluster The given cluster.
     * @return The representative sample index.
     * @throws Exception The exception.
     */
    private int findHighestDensitySample(Cluster cluster) throws Exception {

        // The density of a sample in a cluster is defined through the average distance of the sample to all other
        // samples in the cluster. Therefore, the lower the average distance, the higher the density.
        double leastAverageDistance = Double.MAX_VALUE;
        int sampleIdx = 0;
        for (Integer idx:cluster.memberSampleIndices) {
            final double averageDistance = computeAverageDistance(idx, cluster.memberSampleIndices);
            if (averageDistance < leastAverageDistance) {
                leastAverageDistance = averageDistance;
                sampleIdx = idx;
            }
        }

        return sampleIdx;
    }

    /**
     * Compute the average distance of a given sample to all other samples in its cluster.
     * @param sampleIdx The index of the given sample.
     * @param memberSampleIndices The list of indices of samples in the cluster.
     * @return The average distance.
     * @throws Exception The exception.
     */
    private double computeAverageDistance(final int sampleIdx, final List<Integer> memberSampleIndices)
            throws Exception {

        final int numSamples = memberSampleIndices.size();
        final double[] x = getFeatures(samples.get(sampleIdx));

        double sum1 = 0.0, sum2 = 0.0;
        for (Integer idx:memberSampleIndices) {
            final double[] xi = getFeatures(samples.get(idx));
            sum1 += svmClassifier.kernel(x,xi);
            sum2 += svmClassifier.kernel(xi,xi);
        }

        return svmClassifier.kernel(x,x) - 2.0*sum1/numSamples + sum2/numSamples;
    }


    public static class Cluster {
        public List<Integer> memberSampleIndices = new ArrayList<Integer>();
        public int centerSampleIdx;
    }
}
