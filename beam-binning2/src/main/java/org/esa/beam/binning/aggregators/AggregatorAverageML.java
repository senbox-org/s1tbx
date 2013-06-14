/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.aggregators;

import com.bc.ceres.binding.PropertySet;
import org.esa.beam.binning.AbstractAggregator;
import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.Observation;
import org.esa.beam.binning.VariableContext;
import org.esa.beam.binning.Vector;
import org.esa.beam.binning.WeightFn;
import org.esa.beam.binning.WritableVector;
import org.esa.beam.framework.gpf.annotations.Parameter;

import java.util.Arrays;

import static java.lang.Math.*;

/**
 * An aggregator that computes a maximum-likelihood average.
 */
public class AggregatorAverageML extends AbstractAggregator {

    public final static double EPS = 0.000000001;

    private final int varIndex;
    private final WeightFn weightFn;
    private final boolean outputSums;

    public AggregatorAverageML(VariableContext ctx, String varName, Double weightCoeff) {
        this(ctx, varName, weightCoeff, false);
    }

    public AggregatorAverageML(VariableContext ctx, String varName, Double weightCoeff, boolean outputSums) {
        super(Descriptor.NAME,
              createFeatureNames(varName, "sum", "sum_sq"),
              createFeatureNames(varName, "sum", "sum_sq", "weights"),
              outputSums ?
                      createFeatureNames(varName, "sum", "sum_sq", "weights") :
                      createFeatureNames(varName, "mean", "sigma", "median", "mode"));
        this.outputSums = outputSums;
        this.varIndex = ctx.getVariableIndex(varName);
        this.weightFn = WeightFn.createPow(weightCoeff != null ? weightCoeff : 0.5);
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.0f);
        vector.set(1, 0.0f);
    }

    @Override
    public void initTemporal(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.0f);
        vector.set(1, 0.0f);
        vector.set(2, 0.0f);
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Observation observationVector, WritableVector spatialVector) {
        final double x = observationVector.get(varIndex);
        final double logX = log(x > EPS ? x : EPS);
        spatialVector.set(0, spatialVector.get(0) + (float) (logX));
        spatialVector.set(1, spatialVector.get(1) + (float) (logX * logX));
    }

    @Override
    public void completeSpatial(BinContext ctx, int numObs, WritableVector numSpatialObs) {
        final float w = weightFn.eval(numObs);
        numSpatialObs.set(0, numSpatialObs.get(0) / w);
        numSpatialObs.set(1, numSpatialObs.get(1) / w);
    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs,
                                  WritableVector temporalVector) {
        temporalVector.set(0, temporalVector.get(0) + spatialVector.get(0));  // sumX
        temporalVector.set(1, temporalVector.get(1) + spatialVector.get(1));  // sumXX
        temporalVector.set(2, temporalVector.get(2) + weightFn.eval(numSpatialObs)); // sumW
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {
    }

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        final double sumX = temporalVector.get(0);
        final double sumXX = temporalVector.get(1);
        final double sumW = temporalVector.get(2);
        if (outputSums) {
            outputVector.set(0, (float) sumX);
            outputVector.set(1, (float) sumXX);
            outputVector.set(2, (float) sumW);
        } else {
            final double avLogs = sumX / sumW;
            final double vrLogs = sumXX / sumW - avLogs * avLogs;
            final double mean = exp(avLogs + 0.5 * vrLogs);
            final double expVrLogs = exp(vrLogs);
            final double sigma = mean * (expVrLogs > 1.0 ? sqrt(expVrLogs - 1.0) : 0.0);
            final double median = exp(avLogs);
            final double mode = exp(avLogs - vrLogs);

            outputVector.set(0, (float) mean);
            outputVector.set(1, (float) sigma);
            outputVector.set(2, (float) median);
            outputVector.set(3, (float) mode);
        }
    }

    @Override
    public String toString() {
        return "AggregatorAverageML{" +
                "varIndex=" + varIndex +
                ", weightFn=" + weightFn +
                ", spatialFeatureNames=" + Arrays.toString(getSpatialFeatureNames()) +
                ", temporalFeatureNames=" + Arrays.toString(getTemporalFeatureNames()) +
                ", outputFeatureNames=" + Arrays.toString(getOutputFeatureNames()) +
                '}';
    }

    public static class Config extends AggregatorConfig {

        @Parameter
        String varName;
        @Parameter
        Double weightCoeff;
        @Parameter
        Boolean outputSums;

        public Config() {
            super(Descriptor.NAME);
        }

        @Override
        public String[] getVarNames() {
            return new String[]{varName};
        }
    }

    public static class Descriptor implements AggregatorDescriptor {

        public static final String NAME = "AVG_ML";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public AggregatorConfig createConfig() {
            return new Config();
        }

        @Override
        public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
            PropertySet propertySet = aggregatorConfig.asPropertySet();
            Boolean outputSums = propertySet.getValue("outputSums");
            return new AggregatorAverageML(varCtx,
                                           (String) propertySet.getValue("varName"),
                                           (Double) propertySet.getValue("weightCoeff"),
                                           outputSums != null ? outputSums : false);
        }
    }
}
