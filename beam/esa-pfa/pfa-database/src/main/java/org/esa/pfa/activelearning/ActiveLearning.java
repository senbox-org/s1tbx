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

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Active learning action.
 */

public class ActiveLearning {

    private int h = 0; // number of batch samples selected with diversity criterion
    private int m = 0; // number of uncertainty samples selected with confidence criterion
    private int numIterations = 0;
    private int testDataSize = 0;
    private int featureSize = 0;
    private int numClasses = 0;
    private int[] numSamplesInClasses = null; // # of samples in each class
    private String testDataDirectory = null;
    private ArrayList<Integer> classLabels = new ArrayList<Integer>();
    private List<Data> testData = new ArrayList<Data>();
    private List<Data> validationData = new ArrayList<Data>();
    private List<Data> trainingData = new ArrayList<Data>();
    private List<Data> uncertainSamples = new ArrayList<Data>();

    private SVM svmClassifier = new SVM();

    private int maxIterationsKmeans = 10;
    private double validationDataPercentage = 0.2; // 20% of each class in the whole test data set
    private double unlabelledDataPercentage = 0.8; // 80% of each class in the whole test data set
    private double initialTrainingDataPercentage = 0.04; // 4% from each class in the unlabelled data set

    private boolean debug = true; // output debugging information

    public ActiveLearning(File testDataIndexFile) throws Exception {

        //h = ;

        //m =;

        testDataDirectory = testDataIndexFile.getParent();

        getTestData(testDataIndexFile);

        setValidationDataSet();

        selectModel();

        startActiveLearning();

        classifyTrainingData();
    }

    /**
     * Get test data from specified folder.
     * @param testDataIndexFile The summary file of test data.
     * @throws IOException The exceptions.
     */
    private void getTestData(final File testDataIndexFile) throws Exception {

        getTestDataSize(testDataIndexFile);

        final FileInputStream stream = new FileInputStream(testDataIndexFile);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        String line = "";
        while ((line = reader.readLine()) != null) {
            Data data = new Data();
            data.feature = new double[featureSize];
            data.tileID = getTileID(line);
            final String tileFeatureFile = getTileFeatureFile(line);
            getTileFeature(tileFeatureFile, data);
            testData.add(data);
        }

        reader.close();
        stream.close();

        numClasses = classLabels.size();
        numSamplesInClasses = new int[numClasses];
        for (int i = 0; i < numClasses; i++) {
            final int classLabel = classLabels.get(i);
            for (Data data:testData) {
                if (data.label == classLabel) {
                    numSamplesInClasses[i]++;
                }
            }
        }

        if (debug) {
            System.out.println("Total number of samples: " + testDataSize);
            System.out.println("Number of features: " + featureSize);
            System.out.println("Number of classes: " + numClasses);
            for (int i = 0; i < numClasses; i++) {
                System.out.println("Number of samples in class " + i + ": " + numSamplesInClasses[i]);
            }
        }
    }

    /**
     * Get the number of samples in the test data set and the dimension of each sample (feature size).
     * @param testDataIndexFile The summary file of test data.
     * @throws IOException The exceptions.
     */
    private void getTestDataSize(final File testDataIndexFile) throws Exception {

        try {
            LineNumberReader testDataIndexReader  = new LineNumberReader(new FileReader(testDataIndexFile));
            String lineRead = testDataIndexReader.readLine();
            testDataSize = getNumberOfLines(testDataIndexReader);
            testDataIndexReader.close();

            final String tileFeatureFile = getTileFeatureFile(lineRead);
            LineNumberReader tileFeatureReader  = new LineNumberReader(new FileReader(tileFeatureFile));
            featureSize = getNumberOfLines(tileFeatureReader);
            tileFeatureReader.close();

        } catch (Throwable e) {
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Get the complete path to a feature file of a given tile.
     * @param line A line in the test data summary file (contains information of a tile).
     * @return The absolute path to the feature file.
     */
    private String getTileFeatureFile(final String line) {

        final String[] words = line.split("\\s+");
        final String[] parts = words[0].split("/");
        return testDataDirectory + "\\" + parts[0] + "\\" + parts[1] + "\\features.txt";
    }

    /**
     * Get tile ID string (e.g. x08y01)
     * @param line A line in the test data summary file (contains information of a tile).
     * @return The tile ID string.
     */
    private String getTileID(final String line) {

        final String[] words = line.split("\\s+");
        final String[] parts = words[0].split("/");
        return parts[1];
    }

    /**
     * Read feature from feature file and save them as a sample in test data.
     * Here 3 classes are assumed. The 3 classes are defined by samples with their percentOverPnt4 feature
     * int the following ranges: class 1 (30.0, inf), class 2 (10, 30], class 3 [0, 10]. This should be done
     * by the user when user interface is available.
     * @param tileFeatureFile The tile feature file.
     * @param data The test data.
     * @throws IOException The exceptions.
     */
    private void getTileFeature(final String tileFeatureFile, final Data data) throws Exception {

        final FileInputStream stream = new FileInputStream(new File(tileFeatureFile));
        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line = "";
        int index = 0;
        while ((line = reader.readLine()) != null) {
            final String[] words = line.split("=");
            data.feature[index] = Double.parseDouble(words[1]);

            if (words[0].contains("percentOverPnt4")) {
                if (data.feature[index] > 30.0) {
                    data.label = 1;
                } else if (data.feature[index] > 10.0) {
                    data.label = 2;
                } else {
                    data.label = 3;
                }
            }

            index++;
        }

        if (!classLabels.contains(data.label)) {
            classLabels.add(data.label);
        }

        if (index != data.feature.length) {
            throw new Exception("Invalid number of features: " + index);
        }
    }

    /**
     * Get the number of lines of a given text file.
     * @param reader Reader of the given text file.
     * @return The number of lines.
     * @throws Exception The exception.
     */
    private int getNumberOfLines(final LineNumberReader reader) throws Exception {

        int numLines = 0;
        try {
            String lineRead = reader.readLine();
            while (lineRead != null) {
                lineRead = reader.readLine();
            }

            numLines = reader.getLineNumber();
        } catch (Throwable e) {
            throw new Exception(e.getMessage());
        }

        return numLines;
    }

    /**
     * Select RBF model parameters C and gamma using grid search.
     * @throws Exception The exception.
     */
    private void selectModel() throws Exception {

        svmClassifier.selectModel(validationData);
    }

    /**
     * Select samples from all classes in the test data to form validation data set (V) for grid search.
     * A percentage of samples from each class are selected.
     */
    private void setValidationDataSet() {

        validationData.clear();
        for (int i = 0; i < numClasses; i++) {
            final int classLabel = classLabels.get(i);
            final int samplesToSelect = (int)Math.max(1, Math.round(validationDataPercentage*numSamplesInClasses[i]));
            int count = 0;
            for (Iterator<Data> itr = testData.iterator(); itr.hasNext() & count < samplesToSelect;) {
                Data data = itr.next();
                if (data.label == classLabel) {
                    validationData.add(data);
                    itr.remove();
                    count++;
                }
            }
        }

        if (debug) {
            System.out.println("Number of samples in validation data set: " + validationData.size());
            for (int i = 0; i < numClasses; i++) {
                System.out.println("Number of samples in class " + i + ": " +
                        (int)Math.max(1, Math.round(validationDataPercentage*numSamplesInClasses[i])));
            }
            System.out.println("Number of samples in test data set: " + testData.size());
        }
    }

    /**
     * Main function for active learning.
     * @throws Exception The exception.
     */
    private void startActiveLearning() throws Exception {

        setInitialTrainingDataSet();

        svmClassifier.train(trainingData);

        numIterations = (testData.size() - m)/h;

        for (int i = 0; i < numIterations; i++) {

            System.out.println("Iteration: " + i);

            getNewData();

            classifyTestData();

            updateTrainingData();

            svmClassifier.train(trainingData);
        }
    }

    /**
     * Set initial training data set. A percentage of samples from each class in unlabelled data set (U) are selected.
     */
    private void setInitialTrainingDataSet() {

        trainingData.clear();

        final double overallPercentage = initialTrainingDataPercentage*unlabelledDataPercentage;
        for (int i = 0; i < numClasses; i++) {
            final int classLabel = classLabels.get(i);
            final int samplesToSelect = (int)Math.max(1, Math.round(overallPercentage*numSamplesInClasses[i]));
            int count = 0;
            for (Iterator<Data> itr = testData.iterator(); itr.hasNext() & count < samplesToSelect;) {
                Data data = itr.next();
                if (data.label == classLabel) {
                    trainingData.add(data);
                    itr.remove();
                    count++;
                }
            }
        }

        if (debug) {
            System.out.println("Number of samples in the initial training data set: " + trainingData.size());
            for (int i = 0; i < numClasses; i++) {
                System.out.println("Number of samples in class " + i + ": " +
                        (int)Math.max(1, Math.round(overallPercentage*numSamplesInClasses[i])));
            }
            System.out.println("Number of samples in test data set: " + testData.size());
        }
    }

    /**
     * This is the query function that selects the most informative samples from the unlabelled sample set (U)
     * using MCLU-ECBD algorithm.
     * @throws Exception The exception.
     */
    private void getNewData() throws Exception {

        selectMostUncertainSamples();

        selectMostDiverseSamples();
    }

    /**
     * Add new data to the training data set.
     */
    private void updateTrainingData() {

        int count = 0;
        for (Iterator<Data> itr = testData.iterator(); itr.hasNext() & count < h;) {
            Data data = itr.next();
            if (data.queryData) {
                trainingData.add(data);
                itr.remove();
                count++;
            }
        }

        if (debug) {
            System.out.println("Number of samples added to the training data set: " + h);
            System.out.println("Number of samples in the new training data set: " + trainingData.size());
            System.out.println("Number of samples in the test data set: " + testData.size());
        }
    }

    /**
     * Select m unlabelled samples from test data with lower confidence values.
     * @throws Exception The exception.
     */
    private void selectMostUncertainSamples() throws Exception {

        // Compute confidence c(x) for all samples in U. (MCLU)
        final double[][] confidence = new double[testData.size()][2];
        int k = 0;
        for (int i = 0; i < testData.size(); i++) {
            confidence[k][0] = i;                                  // sample index in testData
            confidence[k][1] = computeConfidence(testData.get(i)); // sample confidence value
            k++;
        }

        // Select m unlabelled samples with lower confidence values. (MCLU)
        java.util.Arrays.sort(confidence, new java.util.Comparator<double[]>() {
            public int compare(double[] a, double[] b) {
                return Double.compare(a[1], b[1]);
            }
        });

        // Add the selected samples to uncertainSamples list.
        uncertainSamples.clear();
        for (int i = 0; i < m; i++) {
            Data data = testData.get((int)confidence[i][0]);
            data.confidence = confidence[i][1];
            uncertainSamples.add(data);
        }
    }

    /**
     * Compute confidence for given sample. Here it is assumed that the decision values returned by svm.classify
     * function are the distances from the given sample to the hyperplanes in kernel space.
     * @param x The given sample.
     * @return The confidence value.
     * @throws Exception The exception.
     */
    private double computeConfidence(Data x) throws Exception {

        final double[] decValues = new double[numClasses*(numClasses-1)/2];
        svmClassifier.classify(x, decValues);
        if (numClasses > 2) {
            Arrays.sort(decValues);
            return Math.abs(decValues[decValues.length-1] - decValues[decValues.length-2]);
        } else {
            return Math.abs(decValues[0]);
        }
    }

    /**
     * Select h most diverse samples from the m most uncertain samples.
     * @throws Exception The exception.
     */
    private void selectMostDiverseSamples() throws Exception {

        KernelKmeansClusterer kkc = new KernelKmeansClusterer(maxIterationsKmeans, h, svmClassifier);
        kkc.setData(uncertainSamples);
        kkc.clustering();
    }

    /**
     * Classify the h samples returned by query function.
     * @throws Exception The exception.
     */
    private void classifyTestData() throws Exception {

        try {
            int count = 0;
            int countErr = 0;
            final double[] decValues = new double[numClasses*(numClasses-1)/2];
            for (Iterator<Data> itr = testData.iterator(); itr.hasNext() & count < h;) {
                Data data = itr.next();
                if (data.queryData) {
                    double p = svmClassifier.classify(data, decValues);
                    if (p != data.label) {
                        countErr++;
                    }
                    count++;
                }
            }
            double accuracy = (1.0 - (double)countErr / (double)h) * 100.0;
            System.out.println("Classifying query data, accuracy: " + accuracy);
        } catch (Throwable e) {
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Applied the trained model to the training data set to check its accuracy.
     * @throws Exception The exception.
     */
    private void classifyTrainingData() throws Exception {

        try {
            int countErr = 0;
            final double[] decValues = new double[numClasses*(numClasses-1)/2];
            for (Data data:trainingData) {
                double p = svmClassifier.classify(data, decValues);
                if (p != data.label) {
                    countErr++;
                }
            }
            double accuracy = (1.0 - (double)countErr / (double)trainingData.size()) * 100.0;
            System.out.println("Classifying training data, accuracy: " + accuracy);
        } catch (Throwable e) {
            throw new Exception(e.getMessage());
        }
    }

    public static class Data {
        public String tileID;         // e.g. x08y01
        public boolean queryData;     // true if this sample is selected by query function
        public double confidence;     // confidence used by query function in selecting the most informative samples
        public int label;             // class index (1, 2, 3...) labelled by user
        public double[] feature;      // array of statistics
    }
}