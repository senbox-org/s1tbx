/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.classification.gpf;

import be.abeel.util.MTRandom;
import com.bc.ceres.core.ProgressMonitor;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.evaluation.PerformanceMeasure;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.featureselection.scoring.GainRatio;
import org.esa.snap.core.gpf.OperatorException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Evaluate classification results
 */
public class Evaluator {

    private final Classifier mlClassifier;
    private final ClassifierReport classifierReport;
    private final Score score = new Score();

    private final static int NUM_PERTURBATIONS = 3;
    private final static boolean doCrossValidation = true;

    public static class Score {
        public double classifierPercent;
        public double crossValidationPercent;
        public Map<String, String> featureScoreMap = new HashMap<>();
    }

    public Evaluator(final Classifier classifier, final ClassifierReport classifierReport) {
        this.mlClassifier = classifier;
        this.classifierReport = classifierReport;
    }

    private static String f(double val) {
        return String.format("%-6.4f", val);
    }

    public Score getScore() {
        return score;
    }

    public Score evaluateClassifier(final Map<Double, String> labelMap,
                                    final List<Instance> instanceList, final Dataset dataset,
                                    final String datasetType) {
        final Map<Object, Integer> classDistribution = getClassDistributionInDataset(instanceList, dataset);

        // Evaluate the classifier using a separate test dataset
        //final Map<Object, PerformanceMeasure> performanceMeasureMap =
        //        ClassifierAttributeEvaluation.testDataset(mlClassifier, dataset);

        //score.classifierPercent = printEvaluation("Classifier Evaluation", labelMap, dataset, datasetType,
        //                                           classDistribution, performanceMeasureMap);

        if (doCrossValidation) {
            final CrossValidation cv = new CrossValidation(mlClassifier);
            try {
                score.crossValidationPercent = printEvaluation("Cross Validation", labelMap, dataset, datasetType,
                                                               classDistribution, cv);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        printDistribution(labelMap, classDistribution);

        return score;
    }

    private void printDistribution(final Map<Double, String> labelMap,
                                   final Map<Object, Integer> classDistribution) {
        final StringBuilder log = new StringBuilder(512);

        final Object[] sortedClassValues = ClassifierAttributeEvaluation.getSortedObjects(classDistribution.keySet());
        log.append("Distribution:\n");

        int sum = 0;
        for (Object o : sortedClassValues) {
            sum += classDistribution.get(o);
        }
        for (Object o : sortedClassValues) {
            final int cntVal = classDistribution.get(o);
            final String label = labelMap.get(o);

            log.append("   class " + o + ": " + String.format("%-25s", label) +
                               "  " + cntVal + "\t (" + f(100.0 * (double) cntVal / (double) sum) + "%)\n");
        }

        classifierReport.addClassifierEvaluation(log.toString());
    }

    private double printEvaluation(final String title,
                                   final Map<Double, String> labelMap, final Dataset dataset,
                                   final String datasetType, final Map<Object, Integer> classDistribution,
                                   final CrossValidation cv) {
        final StringBuilder log = new StringBuilder(512);

        final Map<Object, PerformanceMeasure> cvPerfMeasure = cv.crossValidation(dataset, 5, new Random());

        log.append(title + '\n');
        log.append("Number of classes = " + cvPerfMeasure.size() + '\n');

        int sum = 0;
        int totalTP = 0;
        int totalSamples = 0;
        final Object[] sortedClassValues = ClassifierAttributeEvaluation.getSortedObjects(cvPerfMeasure.keySet());
        for (Object o : sortedClassValues) {
            final PerformanceMeasure perMea = cvPerfMeasure.get(o);
            totalTP += perMea.tp;
            totalSamples += (perMea.tp + perMea.fn);
            final int cntVal = classDistribution.get(o);
            sum += cntVal;
            final String label = labelMap.get(o);

            log.append("   class " + o + ": " + String.format("%-25s", label) + '\n');
            log.append("   " + " accuracy = " + f(perMea.getAccuracy()) + " precision = " + f(perMea.getPrecision())
                               + " correlation = " + f(perMea.getCorrelation()) + " errorRate = " + f(perMea.getErrorRate()) + '\n');
            log.append("   " + " TruePositives = " + f(perMea.tp) + " FalsePositives = " + f(perMea.fp)
                               + " TrueNegatives = " + f(perMea.tn) + " FalseNegatives = " + f(perMea.fn) + '\n');
        }

        if (totalSamples != dataset.size()) {
            throw new OperatorException("totalSamples = " + totalSamples + " dataset size = " + dataset.size());
        }

        final double tpPct = (double) totalTP / (double) dataset.size();
        log.append("\nUsing " + datasetType + " dataset, % correct predictions = " + f(tpPct * 100.0) + '\n');
        log.append("Total samples = " + sum + '\n');
        log.append("RMSE = " + cv.getRMSE() + '\n');
        log.append("Bias = " + cv.getBias() + '\n');

        classifierReport.addClassifierEvaluation(log.toString());

        return tpPct;
    }

    public void evaluateFeatures(final BaseClassifier.FeatureInfo[] featureInfoList,
                                 final Dataset dataset, final String datasetType, final ProgressMonitor pm) {

        final StringBuilder log = new StringBuilder(512);

        log.append(datasetType + " feature importance score:\n");
        ClassifierAttributeEvaluation eval = new ClassifierAttributeEvaluation(
                mlClassifier, NUM_PERTURBATIONS, new MTRandom());
        ClassifierAttributeEvaluation.FeatureScore[] fs = eval.performEvaluation(dataset, pm);

        final TreeMap<Double, Integer> sortedMap = new TreeMap<>();
        for (int i = 0; i < featureInfoList.length; i++) {
            final double importanceScore = fs[i].tp;
            sortedMap.put(importanceScore, i);
        }

        GainRatio gr = new GainRatio();
        gr.build(dataset);

        log.append("Each feature is perturbed " + NUM_PERTURBATIONS + " times and the % correct predictions are averaged\n");
        log.append("The importance score is the original % correct prediction - average\n");

        Double key = sortedMap.lastKey();
        int rank = 1;
        while (key != null) {
            int i = sortedMap.get(key);
            String scoreStr = "score: tp=" + f(fs[i].tp) + " accuracy=" + f(fs[i].accuracy) + " precision=" + f(fs[i].precision)
                    + " correlation=" + f(fs[i].correlation) + " errorRate=" + f(fs[i].errorRate) + " cost=" + f(fs[i].cost)
                    + " GainRatio = " + f(gr.score(i));
            score.featureScoreMap.put(featureInfoList[i].featureBand.getName(), scoreStr);

            log.append("   rank " + String.format("%-3d", rank) + " feature " + String.format("%-3d", i + 1) + ": " +
                               String.format("%-25s", featureInfoList[i].featureBand.getName()) +
                               scoreStr + '\n');
            key = sortedMap.lowerKey(key);
            rank++;
        }
        if (rank <= featureInfoList.length) {
            log.append("Warning: rank <= featureBandList.length\n");
//            for (int i = 0; i < featureBandList.length; i++) {
//                log.append("   feature " + (i + 1) + ':' + featureBandList[i].getName()
//                                   + "\t importance score = " + eval.getImportanceScore(i) + '\n');
//            }
        }
        log.append('\n');

        classifierReport.addFeatureEvaluation(log.toString());
    }

    private static Map<Object, Integer> getClassDistributionInDataset(final List<Instance> instanceList,
                                                                      final Dataset dataset) {
        final Map<Object, Integer> cnt = new HashMap<>();
        for (Object o : dataset.classes()) {
            cnt.put(o, 0);
        }
        for (Instance i : instanceList) {
            final Object o = i.classValue();
            final Integer oldCnt = cnt.get(o);
            if(oldCnt != null) {
                cnt.put(o, oldCnt + 1);
            } else {
                cnt.put(o, 1);
            }
        }
        return cnt;
    }
}
