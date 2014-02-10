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

import libsvm.*;

import java.util.List;

/**
 * SVM based classification operator.
 *
 * The following steps are followed:
 * 1. Transform data to the format of an SVM package
 * 2. Conduct simple scaling on the data
 * 3. Consider the RBF kernel
 * 4. Use cross-validation to find the best parameter C and gamma
 * 5. Use the best parameter C and gamma to train the whole training set
 * 6. Test
 */

public class SVM {

    private int numSamples = 0;  // number of samples in the training data
    private int numFeatures = 0; // number of features in each sample
    private double[] featureMin = null;
    private double[] featureMax = null;

    private svm_problem problem = new svm_problem();
    private svm_parameter modelParameters = new svm_parameter();
    private svm_model model = new svm_model();

    private static int numFolds = 5;    // number of folds for cross validation
    private static double lower = 0.0;  // training data scaling lower limit
    private static double upper = 1.0;  // training data scaling upper limit

	public SVM() {
	}

    /**
     * Select optimal SVM (RBF) model parameters C and Gamma using grid search and given training data.
     * @param validationSet The data set for grid search.
     */
    public void selectModel(List<ActiveLearning.Data> validationSet) throws Exception {

        setProblem(validationSet);

        scaleData();

        setSVMModelParameters();

        findOptimalModelParameters();
    }

    /**
     * Define SVM problem.
     * @param dataSet The training data set.
     */
	private void setProblem(List<ActiveLearning.Data> dataSet) {
		
		numSamples = dataSet.size();
		numFeatures = dataSet.get(0).feature.length;
	    
        featureMin = new double[numFeatures];
        featureMax = new double[numFeatures];
        for (int i = 0; i < numFeatures; i++) {
            featureMin[i] = Double.MAX_VALUE;
            featureMax[i] = -Double.MAX_VALUE;
        }

		problem.l = numSamples;
        problem.y = new double[numSamples];
        problem.x = new svm_node[numSamples][numFeatures];
        for (int i = 0; i < numSamples; i++) {
            for (int j = 0; j < numFeatures; j++) {
                problem.x[i][j] = new svm_node();
            }
        }
		
		for (int i = 0; i < numSamples; i++) {
			problem.y[i] = dataSet.get(i).label;
			for (int j = 0; j < numFeatures; j++) {
				problem.x[i][j].index = j+1;
				problem.x[i][j].value = dataSet.get(i).feature[j];
				if (problem.x[i][j].value < featureMin[j]) {
					featureMin[j] = problem.x[i][j].value;
				}
				
				if (problem.x[i][j].value > featureMax[j]) {
					featureMax[j] = problem.x[i][j].value;
				}
			}
		}
	}

	/**
	 * Train SVM model with given training data.
     * @param trainingSet The training data set.
	 */
	public void train(List<ActiveLearning.Data> trainingSet) throws Exception {

        setProblem(trainingSet);

		scaleData();

        findOptimalModelParameters();

        trainSVMModel();
	}

    /**
     * Compute kernel function value for a given pair of samples.
     * @param x1 The first sample.
     * @param x2 The second sample.
     * @return The kernel function value.
     */
    public double kernel(final double[] x1, final double[] x2) throws Exception {

        if (x1.length != x2.length) {
            throw new Exception("Samples to the kernel function must have the same dimension");
        }

        double sum = 0.0;
        for (int i = 0; i < x1.length; i++) {
            final double d = x1[i] - x2[i];
            sum += d*d;
        }
        return Math.exp(-modelParameters.gamma*sum);
    }

    /**
     * Classify given test data using the trained SVM model.
     * @param testData A sample to classify.
     * @param decValues Decision values.
     * @return The predicted class label.
     */
    public double classify(ActiveLearning.Data testData, double[] decValues) {

			svm_node[] x = new svm_node[numFeatures];
            for (int i = 0; i < numFeatures; i++) {
                x[i] = new svm_node();
				x[i].index = i + 1;
				x[i].value = testData.feature[i];
            }

            //return svm.svm_predict(model, x);
            return svm.svm_predict_values(model, x, decValues);
    }

    /**
     * Scale training data to user specified range [lower, upper].
     */
    private void scaleData() {

            double featureValue, lambda;
            for (int i = 0; i < numSamples; i++) {
                for (int j = 0; j < numFeatures; j++) {
                    featureValue = problem.x[i][j].value;
                    if (featureMin[j] < featureMax[j]) {
                        lambda = (featureValue - featureMin[j]) / (featureMax[j] - featureMin[j]);
                        problem.x[i][j].value = lower + lambda*(upper - lower);
                    } else {
                        problem.x[i][j].value = lower;
                    }
                }
            }
    }

    /**
     * Set parameters used by SVM model.
     */
    private void setSVMModelParameters() throws Exception {

        modelParameters.svm_type = svm_parameter.C_SVC;
        modelParameters.kernel_type = svm_parameter.RBF;
        modelParameters.cache_size = 100.0;
        modelParameters.eps = 0.001;
        modelParameters.C = 1.0;

        String error_msg = svm.svm_check_parameter(problem, modelParameters);
        if (error_msg != null) {
            throw new Exception(error_msg);
        }
    }

    /**
     * Find optimal RBF model parameters (C, gamma) using grid search.
     */
    private void findOptimalModelParameters() throws Exception {

        try {
            final double[] c = {0.03125, 0.125, 0.5, 2.0, 8.0, 32.0, 128.0, 512.0, 2048.0, 8192.0, 32768.0};
            final double[] gamma = {0.000030517578125, 0.0001220703125, 0.00048828125, 0.001953125, 0.0078125,
                                    0.03125, 0.125, 0.5, 2.0, 8.0};
            double[][] accuracyArray = new double[c.length][gamma.length];

            double accuracyMax = 0.0;
            int cIdx = 0, gammaIdx = 0;
            for (int i = 0; i < c.length; i++) {
                for (int j = 0; j < gamma.length; j++) {
                    final double accuracy = performCrossValidation(c[i], gamma[j]);
                    accuracyArray[i][j] = accuracy;
                    if (accuracy > accuracyMax) {
                        accuracyMax = accuracy;
                        cIdx = i;
                        gammaIdx = j;
                    }
                }
            }

            modelParameters.C = c[cIdx];
            modelParameters.gamma = gamma[gammaIdx];

            for (int i = 0; i < c.length; i++) {
                for (int j = 0; j < gamma.length; j++) {
                    System.out.println("C = " + c[i] + ", gamma = " + gamma[j] + ", accuracy = " + accuracyArray[i][j]);
                }
            }
            System.out.println("Optimal: C = " + c[cIdx] + ", gamma = " + gamma[gammaIdx] + ", accuracy = " + accuracyMax);
        } catch (Throwable e) {
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Perform  cross-validation to find the best parameter C and gamma for RBF model.
     * @param C C parameter for RBF model.
     * @param gamma Gamma parameter for RBF model.
     * @return The model accuracy.
     */
    private double performCrossValidation(final double C, final double gamma) {

            modelParameters.C = C;
            modelParameters.gamma = gamma;
            double[] target = new double[problem.l];
            svm.svm_cross_validation(problem, modelParameters, numFolds, target);

            int countErr = 0;
            for(int i = 0; i < problem.l; i++) {
                if (problem.y[i] != target[i]) {
                    countErr++;
                }
            }
            return (1.0 - (double)countErr / (double)problem.l) * 100.0;
    }

    /**
     * Train SVM model.
     */
    private void trainSVMModel() {
            model = svm.svm_train(problem, modelParameters);
    }
}
