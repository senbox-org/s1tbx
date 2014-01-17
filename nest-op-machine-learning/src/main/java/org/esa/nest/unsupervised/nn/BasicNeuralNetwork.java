/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.nest.unsupervised.nn;

import org.esa.nest.base.AbstractLinearModel;
import org.esa.nest.base.AbstractLinearClassifier;
import org.apache.mahout.math.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author emmab
 */
public class BasicNeuralNetwork extends AbstractLinearClassifier {

    private static final Logger log = LoggerFactory.getLogger(BasicNeuralNetwork.class);

    /**
     * Rate the model is to be updated with at each step.
     */
    private final double learningRate;

    public BasicNeuralNetwork(int dimension, double threshold,
            double learningRate, double init, double initBias) {
        super(dimension, threshold, init, initBias);
        this.learningRate = learningRate;
    }

    @Override
    protected void update(double label, Vector dataPoint, AbstractLinearModel model) {
        double factor = 1.0;
        if (label == 0.0) {
            factor = -1.0;
        }

        Vector updateVector = dataPoint.times(factor).times(this.learningRate);
        log.debug("Updatevec: {}", updateVector);

        model.addDelta(updateVector);
        model.shiftBias(factor * this.learningRate);
        log.debug("{}", model);
    }
}
