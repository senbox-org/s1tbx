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

import org.esa.beam.util.math.Array;
import org.esa.pfa.fe.op.Feature;
import org.esa.pfa.fe.op.Patch;

import java.util.*;

/**
 * Active learning class.
 *
 * [1] Begum Demir and Lorenzo Bruzzone, "An effective active learning method for interactive content-based retrieval
 *     in remote sensing images", Geoscience and Remote Sensing Symposium (IGARSS), 2013 IEEE International.
 */

public class ActiveLearning {

    private int h = 0; // number of batch samples selected with diversity and density criteria
    private int q = 0; // number of uncertainty samples selected with uncertainty criterion
    private int numClasses = 0; // should be 2
    private int iteration = 0;  // Iteration index in active learning
    private List<Patch> testData = new ArrayList<Patch>();
    private List<Patch> trainingData = new ArrayList<Patch>();
    private List<Patch> uncertainSamples = new ArrayList<Patch>();
    private List<Patch> diverseSamples = new ArrayList<Patch>();
    private SVM svmClassifier = null;

    private static int numInitialIterations = 3; // AL parameter
    private static int maxIterationsKmeans = 10; // KKC parameter
    private static int numFolds = 5;    // SVM parameter: number of folds for cross validation
    private static double lower = 0.0;  // SVM parameter: training data scaling lower limit
    private static double upper = 1.0;  // SVM parameter: training data scaling upper limit

    private boolean debug = false;

    public ActiveLearning() throws Exception {
        svmClassifier = new SVM(numFolds, lower, upper);
    }

    /**
     * Set patches obtained from user's query image. These patch forms the initial training data set.
     * @param patchArray The patch array.
     * @throws Exception The exception.
     */
    public void setQueryPatches(Patch[] patchArray) throws Exception {

        iteration = 0;
        getNumberOfClasses(patchArray);
        trainingData.clear();
        setTrainingDataWithValidPatches(patchArray); // temp code
        //trainingData.addAll(Arrays.asList(patchArray));
        svmClassifier.train(trainingData);

        if (debug) {
            System.out.println("Number of classes: " + numClasses);
            System.out.println("Number of patches from query image: " + patchArray.length);
            System.out.println("Number of patches initially in training data set: " + trainingData.size());
        }
    }

    /**
     * Set random patches obtained from archive. These patches will be used in active learning.
     * @param patchArray The patch array.
     * @throws Exception The exception.
     */
    public void setRandomPatches(Patch[] patchArray) throws Exception {

        testData.addAll(Arrays.asList(patchArray));

        if (debug) {
            System.out.println("Number of random patches: " + patchArray.length);
            System.out.println("Number of patches in test data pool: " + testData.size());
        }
    }

    /**
     * Get the most ambiguous patches selected by the active learning algorithm.
     * @param numImages The number of ambiguous patches.
     * @return The patch array.
     * @throws Exception The exceptions.
     */
    public Patch[] getMostAmbiguousPatches(int numImages) throws Exception {

        this.h = numImages;
        this.q = 4 * h;
        if (debug) {
            System.out.println("Number of uncertain patches to select: " + q);
            System.out.println("Number of diverse patches to select: " + h);
        }

        selectMostUncertainSamples();

        selectMostDiverseSamples();

        for (Patch patch:diverseSamples) {
            System.out.println("Ambiguous patch: x" + patch.getPatchX() + "y" + patch.getPatchY());
        }

        return diverseSamples.toArray(new Patch[diverseSamples.size()]);
    }

    /**
     * Update training set with user labeled patches and train the classifier.
     * @param userLabelledPatches The user labeled patch array.
     * @throws Exception The exception.
     */
    public void train(Patch[] userLabelledPatches) throws Exception {

        checkLabels(userLabelledPatches);

        trainingData.addAll(Arrays.asList(userLabelledPatches));

        svmClassifier.train(trainingData);

        iteration++;

        if (debug) {
            System.out.println("Number of patches in training data set: " + trainingData.size());
        }
    }

    /**
     * UI: Classify an array of patches. UI needs to sort the patches according to their distances to hyperplane.
     * @param patchArray The Given patch array.
     * @throws Exception The exception.
     */
    public void classify(Patch[] patchArray) throws Exception {

        try {
            final double[] decValues = new double[numClasses*(numClasses-1)/2];
            for (Patch patch:patchArray) {
                double p = svmClassifier.classify(patch, decValues);
                final int label = p<1?0:1;
                patch.setLabel(label);
                patch.setDistance(decValues[0]);
                System.out.println("Classified patch: x" + patch.getPatchX() + "y" + patch.getPatchY() + ", label: " + label);
            }

            if (debug) {
                System.out.println("Number of patches to classify: " + patchArray.length);
            }
        } catch (Throwable e) {
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Get patches in the training set.
     * @return The patch array.
     */
    public Patch[] getTrainingData() {
        return trainingData.toArray(new Patch[trainingData.size()]);
    }

    /**
     * Get the number of classes.
     * @param patchArray The patch array.
     * @throws Exception The exception.
     */
    private void getNumberOfClasses(final Patch[] patchArray) throws Exception {

        ArrayList<Integer> classLabels = new ArrayList<Integer>();
        for (Patch patch:patchArray) {
            final int label = patch.getLabel();
            if (!classLabels.contains(label)) {
                classLabels.add(label);
            }

        }
        numClasses = classLabels.size();
        if (numClasses < 2) {
            throw new Exception("Number of classes cannot less than 2");
        }

    }

    // Temp code: select patches with valid features.
    private void setTrainingDataWithValidPatches(Patch[] patchArray) {

        for (Patch patch:patchArray) {
            Feature[] features = patch.getFeatures();
            boolean isPatchValid = true;
            for (Feature f:features) {
                if (Double.isNaN(Double.parseDouble(f.getValue().toString()))) {
                    isPatchValid = false;
                    break;
                }
            }
            if (isPatchValid) {
                trainingData.add(patch);
            }
        }
    }

    /**
     * Check if there is any unlabeled patch.
     * @param patchArray Patch array.
     * @throws Exception The exception.
     */
    private void checkLabels(Patch[] patchArray) throws Exception {

        for (Patch patch:patchArray) {
            if (patch.getLabel() == -1) {
                throw new Exception("Found unlabeled patch(s)");
            }
        }
    }

    /**
     * Select uncertain samples from test data.
     * @throws Exception The exception.
     */
    private void selectMostUncertainSamples() throws Exception {

        final double[][] distance = computeFunctionalDistanceForAllSamples();

        if (iteration < numInitialIterations) {
            getAllUncertainSamples(distance);
            if (uncertainSamples.size() < q) {
                getMostUncertainSamples(distance);
            }
        } else {
            getMostUncertainSamples(distance);
        }

        if (debug) {
            System.out.println("Number of uncertain patches selected: " + uncertainSamples.size());
        }
    }

    /**
     * Compute functional distance for all samples in test data set.
     * @return The distance array.
     * @throws Exception The exception.
     */
    private double[][] computeFunctionalDistanceForAllSamples() throws Exception {

        final double[][] distance = new double[testData.size()][2];
        int k = 0;
        for (int i = 0; i < testData.size(); i++) {
            distance[k][0] = i; // sample index in testData
            try {
                distance[k][1] = computeFunctionalDistance(testData.get(i));
            } catch(Exception e) {
                throw new Exception(e.getMessage());
            }
            k++;
        }

        return distance;
    }

    /**
     * Compute functional distance of a given sample to the SVM hyperplane.
     * @param x The given sample.
     * @return The functional distance.
     * @throws Exception The exception.
     */
    private double computeFunctionalDistance(Patch x) throws Exception {

        final double[] decValues = new double[numClasses*(numClasses-1)/2];
        svmClassifier.classify(x, decValues);
        return Math.abs(decValues[0]);
    }

    /**
     * Get all uncertain samples from test data set if their functional distances are less than 1.
     * @param distance The functional distance array.
     */
    private void getAllUncertainSamples(final double[][] distance) {

        uncertainSamples.clear();
        for (int i = 0; i < testData.size(); i++) {
            if (distance[i][1] < 1.0) {
                uncertainSamples.add(testData.get((int)distance[i][0]));
            }
        }
    }

    /**
     * Get q most uncertain samples from test data set based on their functional distances.
     * @param distance The functional distance array.
     */
    private void getMostUncertainSamples(final double[][] distance) {

        java.util.Arrays.sort(distance, new java.util.Comparator<double[]>() {
            public int compare(double[] a, double[] b) {
                return Double.compare(a[1], b[1]);
            }
        });

        uncertainSamples.clear();
        final int maxUncertainSample = Math.min(q, distance.length);
        for (int i = 0; i < maxUncertainSample; i++) {
            uncertainSamples.add(testData.get((int)distance[i][0]));
        }
    }

    /**
     * Select h most diverse samples from the q most uncertain samples.
     * @throws Exception The exception.
     */
    private void selectMostDiverseSamples() throws Exception {

        try {
            KernelKmeansClusterer kkc = new KernelKmeansClusterer(maxIterationsKmeans, h, svmClassifier);
            kkc.setData(uncertainSamples);
            kkc.clustering();
            final int[] diverseSampleIDs = kkc.getRepresentatives();

            diverseSamples.clear();
            for (int patchID : diverseSampleIDs) {
                for (Iterator<Patch> itr = testData.iterator(); itr.hasNext();) {
                    Patch patch = itr.next();
                    if (patch.getID() == patchID) {
                        diverseSamples.add(patch);
                        itr.remove();
                        break;
                    }
                }
            }

            if (debug) {
                System.out.println("Number of diverse patches IDs: " + diverseSampleIDs.length);
                System.out.println("Number of diverse patches selected: " + diverseSamples.size());
            }

            if (diverseSamples.size() != diverseSampleIDs.length) {
                throw new Exception("Invalid diverse patch array.");
            }

        } catch (Throwable e) {
            throw new Exception(e.getMessage());
        }
    }

}