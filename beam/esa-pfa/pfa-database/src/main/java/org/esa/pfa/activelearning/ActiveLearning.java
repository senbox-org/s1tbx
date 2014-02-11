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

import org.esa.pfa.fe.op.Patch;

import java.util.*;

/**
 * Active learning class.
 */

public class ActiveLearning {

    private int h = 0; // number of batch samples selected with diversity criterion
    private int m = 0; // number of uncertainty samples selected with confidence criterion
    private int numClasses = 0;
    private List<Patch> testData = new ArrayList<Patch>();
    private List<Patch> validationData = new ArrayList<Patch>();
    private List<Patch> trainingData = new ArrayList<Patch>();
    private List<Patch> uncertainSamples = new ArrayList<Patch>();
    private List<Patch> diverseSamples = new ArrayList<Patch>();

    private SVM svmClassifier = new SVM();

    private int maxIterationsKmeans = 10;

    // UI: Pass in some parameters
    public ActiveLearning(final int h, final int m) throws Exception {

        this.h = h;
        this.m = m;
    }

    // UI: Pass in patches obtained from user's query image. These patch will be used in validation.
    public void setQueryPatches(Patch[] patchArray) throws Exception {

        validationData.addAll(Arrays.asList(patchArray));

        svmClassifier.selectModel(validationData);

        trainingData.addAll(validationData);

        svmClassifier.train(trainingData);
    }

    // UI: Pass in random patches obtained from archive. These patches will be used in active learning.
    public void setRandomPatches(Patch[] patchArray) throws Exception {

        testData.addAll(Arrays.asList(patchArray));

        selectMostUncertainSamples();

        selectMostDiverseSamples();

        classifySelectedSamples();
    }

    // UI: Get the selected most ambiguous patches for user to label.
    public Patch[] getMostAmbiguousPatches() {

        return diverseSamples.toArray(new Patch[diverseSamples.size()]);
    }

    // UI: Pass in user labelled patches and trigger the training.
    public void train(Patch[] userLabelledPatches) throws Exception {

        trainingData.addAll(Arrays.asList(userLabelledPatches));

        svmClassifier.train(trainingData);
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
            Patch data = testData.get((int)confidence[i][0]);
            data.setConfidence(confidence[i][1]);
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
    private double computeConfidence(Patch x) throws Exception {

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

        } catch (Throwable e) {
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Classify the h selected samples returned by query function.
     * @throws Exception The exception.
     */
    private void classifySelectedSamples() throws Exception {

        try {
            final double[] decValues = new double[numClasses*(numClasses-1)/2];
            for (Patch patch:diverseSamples) {
                double p = svmClassifier.classify(patch, decValues);
                patch.setLabel((int)p);
            }

        } catch (Throwable e) {
            throw new Exception(e.getMessage());
        }
    }

}