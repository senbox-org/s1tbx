/**
 * %SVN.HEADER% 
 */
package org.esa.snap.classification.gpf;

import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.evaluation.PerformanceMeasure;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.Instance;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Implementation of the cross-validation evaluation technique.
 *
 * 
 * @author Thomas Abeel
 * 
 */
public class CrossValidation {

    private Classifier classifier;
    private double rmseSum ;
    private int rmseTotal;
    private double biasSum1;
    private double biasSum2;

    public CrossValidation(Classifier classifier) {
        this.classifier = classifier;
    }

    public double getRMSE() {
        return rmseTotal == 0 ? 0 : Math.sqrt(rmseSum/rmseTotal);
    }

    public double getBias() {
        return rmseTotal == 0 ? 0 : (biasSum1/rmseTotal) - (biasSum2/rmseTotal);
    }

    /**
     * Performs cross validation with the specified parameters.
     * 
     * @param data
     *            the data set to use in the cross validation. This data set is
     *            split in the appropriate number of folds.
     * @param numFolds
     *            the number of folds to create
     * @param rg
     *            random generator to create the folds
     * @return the results of the cross-validation.
     */
    public Map<Object, PerformanceMeasure> crossValidation(Dataset data, int numFolds, Random rg) {
        // TODO use EvaluateDataset
        Dataset[] folds = data.folds(numFolds, rg);
        Map<Object, PerformanceMeasure> out = new HashMap<Object, PerformanceMeasure>();
        for (Object o : data.classes()) {
            out.put(o, new PerformanceMeasure());
        }

        rmseSum = 0;
        rmseTotal = 0;
        biasSum1 = 0;
        biasSum2 = 0;

        for (int i = 0; i < numFolds; i++) {
            Dataset validation = folds[i];
            Dataset training = new DefaultDataset();
            for (int j = 0; j < numFolds; j++) {
                if (j != i)
                    training.addAll(folds[j]);

            }

            classifier.buildClassifier(training);

            for (Instance instance : validation) {
                Object prediction = classifier.classify(instance);
                if(instance.classValue() instanceof Double) {
                    Double observed = (Double)instance.classValue();
                    Double predicted = (Double)prediction;

                    double diff = observed - predicted;
                    rmseSum += diff*diff;
                    rmseTotal++;
                    biasSum1 += predicted;
                    biasSum2 += observed;
                }

                if (instance.classValue().equals(prediction)) {// prediction
                    // ==class
                    for (Object o : out.keySet()) {
                        if (o.equals(instance.classValue())) {
                            out.get(o).tp++;
                        } else {
                            out.get(o).tn++;
                        }

                    }
                } else {// prediction != class
                    for (Object o : out.keySet()) {
                        /* prediction is positive class */
                        if (o.equals(prediction)) {
                            out.get(o).fp++;
                        }
                        /* instance is positive class */
                        else if (o.equals(instance.classValue())) {
                            out.get(o).fn++;
                        }
                        /* none is positive class */
                        else {
                            out.get(o).tn++;
                        }

                    }
                }
            }

        }
        return out;

    }

    /**
     * Performs cross validation with the specified parameters.
     * 
     * @param data
     *            the data set to use in the cross validation. This data set is
     *            split in the appropriate number of folds.
     * @param numFolds
     *            the number of folds to create
     * @return the results of the cross-validation.
     */
    public Map<Object, PerformanceMeasure> crossValidation(Dataset data, int folds) {
        return crossValidation(data, folds, new Random(System.currentTimeMillis()));

    }

    /**
     * Performs cross validation with the specified parameters. By default the
     * number of folds is 10.
     * 
     * @param data
     *            the data set to use in the cross validation. This data set is
     *            split in the appropriate number of folds.
     * 
     * @return the results of the cross-validation.
     */
    public Map<Object, PerformanceMeasure> crossValidation(Dataset data) {
        return crossValidation(data, 10);
    }

}
