/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.nest.base;

import org.apache.mahout.math.CardinalityException;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementors of this class need to provide a way to train linear
 * discriminative classifiers.
 *
 * As this is just the reference implementation we assume that the dataset fits
 * into main memory - this should be the first thing to change when switching to
 * Hadoop.
 */
public abstract class AbstractLinearClassifier {

    private static final Logger log = LoggerFactory.getLogger(AbstractLinearClassifier.class);

    /**
     * The model to train.
     */
    private final AbstractLinearModel model;

    /**
     * Initialize the trainer. Distance is initialized to cosine distance, all
     * weights are represented through a dense vector.
     *
     *
     * @param dimension number of expected features.
     * @param threshold threshold to use for classification.
     * @param init initial value of weight vector.
     * @param initBias initial classification bias.
     */
    protected AbstractLinearClassifier(int dimension, double threshold,
            double init, double initBias) {
        DenseVector initialWeights = new DenseVector(dimension);
        initialWeights.assign(init);
        this.model = new AbstractLinearModel(initialWeights, initBias, threshold);
    }

    /**
     * Initializes training. Runs through all data points in the training set
     * and updates the weight vector whenever a classification error occurs.
     *
     * Can be called multiple times.
     *
     * @param dataset the dataset to train on. Each column is treated as point.
     * @param labelset the set of labels, one for each data point. If the
     * cardinalities of data- and labelset do not match, a CardinalityException
     * is thrown
     */
    public void train(Vector labelset, Matrix dataset) throws Exception {
        if (labelset.size() != dataset.rowSize()) {
            throw new CardinalityException(labelset.size(), dataset.rowSize());
        }

        boolean converged = false;
        int iteration = 0;
        while (!converged) {
            if (iteration > 1000) {
                throw new Exception("Too many iterations needed to find hyperplane.");
            }

            converged = true;
            int columnCount = dataset.columnSize();
            for (int i = 0; i < columnCount; i++) {
                Vector dataPoint = dataset.viewColumn(i);
                log.debug("Training point: " + dataPoint);

                synchronized (this.model) {
                    boolean prediction = model.classify(dataPoint);
                    double label = labelset.get(i);
                    if (label <= 0 && prediction || label > 0 && !prediction) {
                        log.debug("updating");
                        converged = false;
                        update(label, dataPoint, this.model);
                    }
                }
            }
            iteration++;
        }
    }

    /**
     * Retrieves the trained model if called after train, otherwise the raw
     * model.
     */
    public AbstractLinearModel getModel() {
        return this.model;
    }

    /**
     * Implement this method to match your training strategy.
     *
     * @param model the model to update.
     * @param label the target label of the wrongly classified data point.
     * @param dataPoint the data point that was classified incorrectly.
     */
    protected abstract void update(double label, Vector dataPoint, AbstractLinearModel model);

}
