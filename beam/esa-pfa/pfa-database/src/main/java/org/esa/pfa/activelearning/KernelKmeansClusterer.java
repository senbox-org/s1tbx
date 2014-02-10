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

import java.util.ArrayList;
import java.util.List;


/**
 * Kernel K-means clustering algorithm [1].
 *
 * [1]. B. Demir, C. Persello, and L. Bruzzone, �Batch mode active learning methods for the interactive
 *      classification of remote sensing images,� IEEE Transactions on Geoscience and Remote Sensing,
 *      vol. 49, no.3, pp. 1014-1031, 2011.
 */

public class KernelKmeansClusterer {

    private int maxIterations = 0;
    private int numClusters = 0;
    private int numSamples = 0;
    private ActiveLearning.Data[] samples = null;
    private double[] confidence = null;
    private Cluster[] clusters = null;
    private SVM svmClassifier = null;

	public KernelKmeansClusterer(final int maxIterations, final int numClusters, final SVM svmClassifier) {

        this.maxIterations = maxIterations;
        this.numClusters = numClusters;
        this.clusters = new Cluster[numClusters];
        this.svmClassifier = svmClassifier;
	}

    /**
     * Set samples for clustering and their associated confidence values.
     * @param uncertainSamples List of m most uncertain samples.
     */
    public void setData(final List<ActiveLearning.Data> uncertainSamples) {

        this.numSamples = uncertainSamples.size();
        this.confidence = new double[numSamples];
        this.samples = new ActiveLearning.Data[numSamples];
        for (int sampleIdx = 0; sampleIdx < numSamples; sampleIdx++) {
            this.confidence[sampleIdx] = uncertainSamples.get(sampleIdx).confidence;
            this.samples[sampleIdx] = uncertainSamples.get(sampleIdx);
        }
    }

    /**
     * Perform clustering using Kernel K-means clustering algorithm.
     */
    public void clustering() throws Exception {

        setInitialClusterCenters();

        for (int i = 0; i < maxIterations; i++) {

            assignSamplesToClusters();

            if (i != maxIterations - 1) {
                updateClusterCenters();
            }
        }

        getRepresentatives();
    }

    /**
     * Initialize clusters with each has one randomly selected sample in it.
     */
    private void setInitialClusterCenters() {

        ArrayList<Integer> randomNumbers = new ArrayList<Integer>();
        int k = 0;
        while (k < numClusters) {
            final int idx = k;//JL: (int)(Math.random()*numSamples);
            if (!randomNumbers.contains(idx)) {
                randomNumbers.add(idx);
                clusters[k] = new Cluster();
                clusters[k].memberSampleIndices.add(idx);
                clusters[k].centerSampleIdx = idx;
                k++;
            }
        }
    }

    /**
     * Assign samples to their near clusters base on Euclidean distance in kernel space.
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
     * Compute kelnel space distance between two given samples.
     * @param sampleIdx1 Index of the first sample.
     * @param sampleIdx2 Index of the second sample.
     * @return The kernel space distance.
     */
    private double computeDistance(final int sampleIdx1, final int sampleIdx2) throws Exception {

        final double[] x1 = samples[sampleIdx1].feature;
        final double[] x2 = samples[sampleIdx2].feature;

        return svmClassifier.kernel(x1,x1) - 2*svmClassifier.kernel(x1,x2) + svmClassifier.kernel(x2,x2);
    }

    /**
     * Update centers of the clusters.
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
     */
    private double computeSum2(final List<Integer> memberSampleIndices) throws Exception {

        final int numMembers = memberSampleIndices.size();
        double sum2 = 0.0;
        for (int i = 0; i < numMembers; i++) {
            final double[] xi = samples[memberSampleIndices.get(i)].feature;
            for (int j = 0; j < numMembers; j++) {
                final double[] xj = samples[memberSampleIndices.get(j)].feature;
                sum2 += svmClassifier.kernel(xi,xj);
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
     */
    private double computeDistance(final int sampleIdx, final List<Integer> memberSampleIndices, final double sum2) throws Exception {

        final int numSamples = memberSampleIndices.size();
        final double[] x = samples[sampleIdx].feature;

        double sum1 = 0.0;
        for (int i = 0; i < numSamples; i++) {
            final double[] xi = samples[memberSampleIndices.get(i)].feature;
            sum1 += svmClassifier.kernel(x,xi);
        }

        return svmClassifier.kernel(x,x) - 2.0*sum1/numSamples + sum2/(numSamples*numSamples);
    }

    /**
     * Get representatives of the clusters.
     */
    private void getRepresentatives() {

        for (int i = 0; i < numClusters; i++) {
            int sampleIdx = findLeastConfidenceSample(clusters[i]);
            samples[sampleIdx].queryData = true;
        }
    }

    /**
     * Find the representative sample in a given cluster based on the sample confidence.
     * @param cluster The given cluster.
     * @return The representative sample index.
     */
    private int findLeastConfidenceSample(Cluster cluster) {

        double leastConfidence = Double.MAX_VALUE;
        int sampleIdx = 0;
        for (int i = 0; i < cluster.memberSampleIndices.size(); i++) {
            if (confidence[cluster.memberSampleIndices.get(i)] < leastConfidence) {
                leastConfidence = confidence[cluster.memberSampleIndices.get(i)];
                sampleIdx = cluster.memberSampleIndices.get(i);
            }
        }

        return sampleIdx;
    }


    public static class Cluster {
        public List<Integer> memberSampleIndices = new ArrayList<Integer>();
        public int centerSampleIdx;
    }
}
