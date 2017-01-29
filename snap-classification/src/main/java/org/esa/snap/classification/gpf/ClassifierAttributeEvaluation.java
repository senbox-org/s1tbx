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

import com.bc.ceres.core.ProgressMonitor;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.evaluation.PerformanceMeasure;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.Instance;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ClassifierAttributeEvaluation {

    private final Classifier classifier;
    private final int numPerturbations;
    private final Random rg;

    public static class FeatureScore {
        public double tp;
        public double accuracy;
        public double precision;
        public double correlation;
        public double errorRate;
        public double cost;
        public double total;
    }

    public ClassifierAttributeEvaluation(final Classifier classifier, final int numPerturbations, final Random rg) {
        this.classifier = classifier;
        this.numPerturbations = numPerturbations;
        this.rg = rg;
    }

    private static double getTPPercentage(Map<Object, PerformanceMeasure> performanceMeasureMap) {

        int totalTP = 0;
        int totalSamples = 0;
        for (Object o : performanceMeasureMap.keySet()) {
            PerformanceMeasure perMea = performanceMeasureMap.get(o);
            totalTP += perMea.tp;
            totalSamples += (perMea.tp + perMea.fn);
        }

        return (double) totalTP / (double) totalSamples;
    }

    private static FeatureScore getFeatureScores(Map<Object, PerformanceMeasure> performanceMeasureMap) {

        double totalTP = 0;
        double totalSamples = 0;
        double tp = 0, errorRate = 0, accuracy = 0, precision = 0, correlation = 0, cost = 0, total = 0;
        final int numSets = performanceMeasureMap.keySet().size();
        for (Object o : performanceMeasureMap.keySet()) {
            PerformanceMeasure pm = performanceMeasureMap.get(o);
            totalTP += pm.tp;
            totalSamples += (pm.tp + pm.fn);
            errorRate += pm.getErrorRate();
            accuracy += pm.getAccuracy();
            if (!Double.isNaN(pm.getPrecision()))
                precision += pm.getPrecision();
            if (!Double.isNaN(pm.getCorrelation()))
                correlation += pm.getCorrelation();
            if (!Double.isNaN(pm.getCost()))
                cost += pm.getCost();
            total += pm.getTotal();
        }

        FeatureScore fs = new FeatureScore();
        fs.tp = totalTP / totalSamples;
        fs.accuracy = accuracy / numSets;
        fs.precision = precision / numSets;
        fs.correlation = correlation / numSets;
        fs.errorRate = errorRate / numSets;
        fs.cost = cost / numSets;
        fs.total = total / numSets;

        return fs;
    }

    public FeatureScore[] performEvaluation(final Dataset dataset, final ProgressMonitor pm) {

        final Map<Object, PerformanceMeasure> performanceMeasureMap = testDataset(classifier, dataset);
        final FeatureScore orignalFS = getFeatureScores(performanceMeasureMap);
        //SystemUtils.LOG.info("ClassifierAttributeEvaluation: original % correct predictions = " + orignalFS.tp);

        final FeatureScore[] importanceScores = new FeatureScore[dataset.noAttributes()];
        pm.beginTask("Evaluating classifier... ", dataset.noAttributes());
        for (int i = 0; i < dataset.noAttributes(); i++) {
            final FeatureScore sumFS = new FeatureScore();
            for (int j = 0; j < numPerturbations; j++) {

                // Build the perturbed dataset
                Dataset perturbed = new DefaultDataset();
                for (Instance inst : dataset) {
                    Instance per = inst.copy();
                    per.put(i, rg.nextDouble()); // perturb the i'th attribute
                    perturbed.add(per);
                }

                final Map<Object, PerformanceMeasure> perturbedPM = testDataset(classifier, perturbed);
                FeatureScore perturbedFS = getFeatureScores(perturbedPM);

                sumFS.tp += perturbedFS.tp;
                sumFS.accuracy += perturbedFS.accuracy;
                sumFS.precision += perturbedFS.precision;
                sumFS.correlation += perturbedFS.correlation;
                sumFS.errorRate += perturbedFS.errorRate;
                sumFS.cost += perturbedFS.cost;
            }

            final FeatureScore avgFS = new FeatureScore();
            avgFS.tp = sumFS.tp / (double) numPerturbations;
            avgFS.accuracy = sumFS.accuracy / (double) numPerturbations;
            avgFS.precision = sumFS.precision / (double) numPerturbations;
            avgFS.correlation = sumFS.correlation / (double) numPerturbations;
            avgFS.errorRate = sumFS.errorRate / (double) numPerturbations;
            avgFS.cost = sumFS.cost / (double) numPerturbations;

            importanceScores[i] = new FeatureScore();
            importanceScores[i].tp = orignalFS.tp - avgFS.tp;
            importanceScores[i].accuracy = orignalFS.accuracy - avgFS.accuracy;
            importanceScores[i].precision = orignalFS.precision - avgFS.precision;
            importanceScores[i].correlation = orignalFS.correlation - avgFS.correlation;
            importanceScores[i].errorRate = orignalFS.errorRate - avgFS.errorRate;
            importanceScores[i].cost = orignalFS.cost - avgFS.cost;

            //SystemUtils.LOG.info("ClassifierAttributeEvaluation: feature " + (i + 1) +
            //                             " average % correct predictions = " +
            //                             " score = " + importanceScores[i]);

            pm.worked(1);
        }

        pm.done();
        return importanceScores;
    }

    public static Map<Object, PerformanceMeasure> testDataset(Classifier classifier, Dataset dataset) {

        final Map<Object, PerformanceMeasure> out = new HashMap<>();
        for (Object o : dataset.classes()) {
            out.put(o, new PerformanceMeasure());
        }

        for (Instance instance : dataset) {
            Object prediction = classifier.classify(instance);
            // NOTE: It is possible that prediction is null!!
            if (instance.classValue().equals(prediction)) {
                // prediction == class value
                for (Object o : out.keySet()) {
                    if (o.equals(instance.classValue())) {
                        out.get(o).tp++;
                    } else {
                        out.get(o).tn++;
                    }
                }
            } else {
                // prediction != class value
                for (Object o : out.keySet()) {
                    // prediction is positive class
                    if (o.equals(prediction)) {
                        out.get(o).fp++;
                    }
                    // instance is positive class
                    else if (o.equals(instance.classValue())) {
                        out.get(o).fn++;
                    }
                    // none is positive class
                    else {
                        // If prediction is null, we end up here
                        out.get(o).tn++;
                    }
                }
            }
        }
        return out;
    }

    public static Object[] getSortedObjects(final Set<Object> set) {
        Object[] a = new Object[set.size()];
        int idx = 0;
        for (Object o : set) {
            a[idx++] = o;
        }
        java.util.Arrays.sort(a);
        return a;
    }
}
