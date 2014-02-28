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

import libsvm.svm_model;
import org.esa.pfa.fe.op.Feature;
import org.esa.pfa.fe.op.Patch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Active learning class.
 *
 * [1] Begum Demir and Lorenzo Bruzzone, "An effective active learning method for interactive content-based retrieval
 *     in remote sensing images", Geoscience and Remote Sensing Symposium (IGARSS), 2013 IEEE International.
 */

public class ActiveLearning {

    private int h = 0; // number of batch samples selected with diversity and density criteria
    private int q = 0; // number of uncertainty samples selected with uncertainty criterion
    private int iteration = 0;  // Iteration index in active learning
    private int numFeatures = 0;
    private List<Patch> testData = new ArrayList<Patch>();
    private List<Patch> queryData = new ArrayList<Patch>();
    private List<Patch> trainingData = new ArrayList<Patch>();
    private List<Patch> uncertainSamples = new ArrayList<Patch>();
    private List<Patch> diverseSamples = new ArrayList<Patch>();
    private SVM svmClassifier = null;
    private double[] featureMin = null;
    private double[] featureMax = null;

    private static int numInitialIterations = 3; // AL parameter
    private static int maxIterationsKmeans = 10; // KKC parameter
    private static int numFolds = 5;    // SVM parameter: number of folds for cross validation
    private static double lower = 0.0;  // SVM parameter: training data scaling lower limit
    private static double upper = 1.0;  // SVM parameter: training data scaling upper limit

    private boolean debug = false;

    public ActiveLearning() throws Exception {
        svmClassifier = new SVM(numFolds, lower, upper);
    }

    public void addQueryImage(final Patch patch) {
        queryData.add(patch);
    }

    public Patch[] getQueryPatches() {
        return queryData.toArray(new Patch[queryData.size()]);
    }

    public void resetQuery() {
        iteration = 0;
    }

    /**
     * Set training data with relevant patches from query image.
     * @param patchArray The patch array.
     * @throws Exception The exception.
     */
    public void setQueryPatches(final Patch[] patchArray) throws Exception {

        trainingData.clear();
        checkQueryPatchesValidation(patchArray);
        trainingData.addAll(Arrays.asList(patchArray));
        numFeatures = patchArray[0].getFeatures().length;

        queryData.clear();
        queryData.addAll(Arrays.asList(patchArray));

        if (debug) {
            System.out.println("Number of patches from query image: " + patchArray.length);
        }
    }

    /**
     * Set random patches obtained from archive. Some patches are added to training set as irrelevant patches.
     * The rest will be used in active learning.
     * @param patchArray The patch array.
     * @throws Exception The exception.
     */
    public void setRandomPatches(final Patch[] patchArray) throws Exception {

        setTestDataSetWithValidPatches(patchArray);

        if (iteration == 0) {
            setInitialTrainingSet();
            svmClassifier.train(trainingData);
        }

        if (debug) {
            System.out.println("Number of random patches: " + patchArray.length);
            System.out.println("Number of patches in test data pool: " + testData.size());
            System.out.println("Number of patches in training data set: " + trainingData.size());
        }
    }

    public int getNumIterations() {
        return iteration;
    }

    /**
     * Set training data set with training patches saved.
     * @param patchArray The patch array.
     * @param iterationIndex The iteration index.
     * @exception Exception The exception.
     */
    public void setTrainingData(final Patch[] patchArray, final int iterationIndex) throws Exception {

        trainingData.clear();
        trainingData.addAll(Arrays.asList(patchArray));
        svmClassifier.train(trainingData);
        iteration = iterationIndex;
    }

    /**
     * Get the most ambiguous patches selected by the active learning algorithm.
     * @param numImages The number of ambiguous patches.
     * @return The patch array.
     * @throws Exception The exceptions.
     */
    public Patch[] getMostAmbiguousPatches(final int numImages) throws Exception {

        this.h = numImages;
        this.q = 4 * h;
        if (debug) {
            System.out.println("Number of uncertain patches to select: " + q);
            System.out.println("Number of diverse patches to select: " + h);
        }

        selectMostUncertainSamples();

        selectMostDiverseSamples();

        if (debug) {
            for (Patch patch : diverseSamples) {
                System.out.println("Ambiguous patch: x" + patch.getPatchX() + "y" + patch.getPatchY());
            }
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
     * Classify an array of patches. UI needs to sort the patches according to their distances to hyperplane.
     * @param patchArray The Given patch array.
     * @throws Exception The exception.
     */
    public void classify(final Patch[] patchArray) throws Exception {

        final double[] decValues = new double[1];
        for (Patch patch : patchArray) {
            double p = svmClassifier.classify(patch, decValues);
            final int label = p < 1 ? Patch.LABEL_IRRELEVANT : Patch.LABEL_RELEVANT;
            patch.setLabel(label);
            patch.setDistance(Math.abs(decValues[0]));
            //System.out.println("Classified patch: x" + patch.getPatchX() + "y" + patch.getPatchY() + ", label: " + label);
        }

        if (debug) {
            System.out.println("Number of patches to classify: " + patchArray.length);
        }
    }

    /**
     * Get patches in the training set.
     * @return The patch array.
     */
    public Patch[] getTrainingData() {

        return trainingData.toArray(new Patch[trainingData.size()]);
    }

    public void setModel(final svm_model model) {
        svmClassifier.setModel(model);
    }

    public svm_model getModel() {
        return svmClassifier.getModel();
    }
	
    /**
     * Save SVM model to file.
     * @param fileName The file name string.
     * @throws Exception The exception.
     */
    public void saveSVMModel(String fileName) throws Exception {

        svmClassifier.saveSVMModel(fileName);
    }

    /**
     * Load the SVM model saved in file.
     * @param fileName The file name string.
     * @throws Exception The exception.
     */
    public void loadSVMModel(String fileName) throws Exception {

        svmClassifier.loadSVMModel(fileName);
    }

    /**
     * Check validity of the query patches.
     * @param patchArray The patch array.
     * @throws Exception The exception.
     */
    private static void checkQueryPatchesValidation(final Patch[] patchArray) throws Exception {

        ArrayList<Integer> classLabels = new ArrayList<Integer>();
        for (Patch patch : patchArray) {

            final int label = patch.getLabel();
            if (!classLabels.contains(label)) {
                classLabels.add(label);
            }

            if (!checkFeatureValidation(getValues(patch.getFeatures()))) {
                throw new Exception("Found invalid feature value in query patch.");
            }
        }

        if (classLabels.size() > 1) {
            throw new Exception("Found different labels in query patches.");
        }
    }

    /**
     * Convert feature values to double.
     * @param features The feature array.
     * @return The feature value array.
     */
    private static double[] getValues(final Feature[] features) {

        double[] values = new double[features.length];
        for (int i = 0; i < features.length; i++) {
            values[i] = Double.parseDouble(features[i].getValue().toString());
        }

        return values;
    }

    /**
     * Check validation of given features.
     * @param featureValues The feature value array.
     * @return True if all features are valid, false otherwise.
     */
    private static boolean checkFeatureValidation(final double[] featureValues) {

        for (double v : featureValues) {
            if (Double.isNaN(v)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Set test data set with valid random patches.
     * @param patchArray The patch array.
     */
    private void setTestDataSetWithValidPatches(final Patch[] patchArray) {

        int counter = 0;
        for (Patch patch : patchArray) {
            if (checkFeatureValidation(getValues(patch.getFeatures()))) {
                testData.add(patch);
                counter++;
            }
        }

        if (debug) {
            System.out.println("Number of invalid random patches: " + (patchArray.length - counter));
        }
    }

    /**
     * Set initial training data set with relevant patches from query image and irrelevant patches from random patches.
     * Random patches that are not close to the query patches are considered irrelevant. Euclidean space distance
     * is used in measuring the distance between patches.
     */
    private void setInitialTrainingSet() {

        getFeatureMinMax();

        final double[] relevantPatchClusterCenter = computeClusterCenter(trainingData);

        final double[][] distance = computeDistanceToClusterCenter(relevantPatchClusterCenter);

        java.util.Arrays.sort(distance, new java.util.Comparator<double[]>() {
            public int compare(double[] a, double[] b) {
                return Double.compare(b[1], a[1]);
            }
        });

        final int numIrrelevantSample = Math.min(queryData.size(), distance.length);
        int[] patchIDs = new int[numIrrelevantSample];
        for (int i = 0; i < numIrrelevantSample; i++) {
            final Patch patch = testData.get((int)distance[i][0]);
            patch.setLabel(Patch.LABEL_IRRELEVANT);
            patchIDs[i] = patch.getID();
            trainingData.add(patch);
        }

        for (Iterator<Patch> itr = testData.iterator(); itr.hasNext(); ) {
            Patch patch = itr.next();
            for (int patchID : patchIDs) {
                if (patch.getID() == patchID) {
                    itr.remove();
                    break;
                }
            }
        }
    }

    private void getFeatureMinMax() {

        featureMin = new double[numFeatures];
        featureMax = new double[numFeatures];
        for (int i = 0; i < numFeatures; i++) {
            featureMin[i] = Double.MAX_VALUE;
            featureMax[i] = -Double.MAX_VALUE;
        }

        List<Patch> tempList = new ArrayList<Patch>();
        tempList.addAll(testData);
        tempList.addAll(queryData);

        for (Patch patch:tempList) {
            final double[] featureValues = getValues(patch.getFeatures());
            for (int i = 0; i < numFeatures; i++) {
                if (featureValues[i] < featureMin[i]) {
                    featureMin[i] = featureValues[i];
                }

                if (featureValues[i] > featureMax[i]) {
                    featureMax[i] = featureValues[i];
                }
            }
        }
    }

    private double[] scale(final double[] features) {

        double[] scaledFeatures = new double[numFeatures];
        for (int i = 0; i < numFeatures; i++) {
            scaledFeatures[i] = scale(i, features[i]);
        }
        return scaledFeatures;
    }

    private double scale(final int featureIdx, final double featureValue) {

        if (featureMin[featureIdx] < featureMax[featureIdx]) {
            double lambda = (featureValue - featureMin[featureIdx]) / (featureMax[featureIdx] - featureMin[featureIdx]);
            return lower + lambda*(upper - lower);
        } else {
            return lower;
        }
    }

    /**
     * Compute cluster center of the given list of patches.
     * @param patchList The patch list.
     * @return The cluster center.
     */
    private double[] computeClusterCenter(final List<Patch> patchList) {

        double[] center = new double[patchList.get(0).getFeatures().length];
        for (Patch patch : patchList) {
            final double[] featureValues = getValues(patch.getFeatures());
            for (int i = 0; i < featureValues.length; i++) {
                center[i] += featureValues[i];
            }
        }

        for (int i = 0; i < center.length; i++) {
            center[i] /= patchList.size();
        }

        return center;
    }

    /**
     * Compute for all samples the Euclidean distance to the center of the cluster.
     * @param clusterCenter The cluster center.
     * @return The distance array.
     */
    private double[][] computeDistanceToClusterCenter(final double[] clusterCenter) {

        final double[][] distance = new double[testData.size()][2];
        int k = 0;
        for (Patch patch : testData) {
            distance[k][0] = k; // sample index in testData
            distance[k][1] = computeEuclideanDistance(scale(getValues(patch.getFeatures())), scale(clusterCenter));
            k++;
        }

        return distance;
    }

    /**
     * Compute Euclidean space distance between two given points.
     * @param x1 The first point.
     * @param x2 The second point.
     * @return The distance.
     */
    private double computeEuclideanDistance(final double[] x1, final double[] x2) {

        double distance = 0.0;
        for (int i = 0; i < x1.length; i++) {
            distance += (x1[i] - x2[i])*(x1[i] - x2[i]);
        }

        return distance;
    }

    /**
     * Check if there is any unlabeled patch.
     * @param patchArray Patch array.
     * @throws Exception The exception.
     */
    private static void checkLabels(final Patch[] patchArray) throws Exception {

        for (Patch patch : patchArray) {
            if (patch.getLabel() == Patch.LABEL_NONE) {
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
            distance[k][1] = computeFunctionalDistance(testData.get(i));
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
    private double computeFunctionalDistance(final Patch x) throws Exception {

        final double[] decValues = new double[1];
        svmClassifier.classify(x, decValues);
        return Math.abs(decValues[0]);
    }

    /**
     * Get all uncertain samples from test data set if their functional distances are less than 1.
     * @param distance The functional distance array.
     */
    private void getAllUncertainSamples(final double[][] distance) {

        uncertainSamples.clear();
        for (int i = 0; i < distance.length; i++) {
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

        KernelKmeansClusterer kkc = new KernelKmeansClusterer(maxIterationsKmeans, h, svmClassifier);
        kkc.setData(uncertainSamples);
        kkc.clustering();
        final int[] diverseSampleIDs = kkc.getRepresentatives();

        diverseSamples.clear();
        for (int patchID : diverseSampleIDs) {
            for (Iterator<Patch> itr = testData.iterator(); itr.hasNext(); ) {
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
    }

}