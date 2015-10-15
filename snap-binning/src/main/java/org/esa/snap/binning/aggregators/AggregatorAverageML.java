/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning.aggregators;

import org.esa.snap.binning.AbstractAggregator;
import org.esa.snap.binning.Aggregator;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.AggregatorDescriptor;
import org.esa.snap.binning.BinContext;
import org.esa.snap.binning.Observation;
import org.esa.snap.binning.VariableContext;
import org.esa.snap.binning.Vector;
import org.esa.snap.binning.WeightFn;
import org.esa.snap.binning.WritableVector;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.util.StringUtils;

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

    public AggregatorAverageML(VariableContext ctx, String varName, double weightCoeff) {
        this(ctx, varName, varName, weightCoeff, false);
    }

    public AggregatorAverageML(VariableContext ctx, String varName, String targetName, double weightCoeff, boolean outputSums) {
        super(Descriptor.NAME,
              createFeatureNames(varName, "sum", "sum_sq"),
              createFeatureNames(varName, "sum", "sum_sq", "weights"),
              outputSums ?
                      createFeatureNames(targetName, "sum", "sum_sq", "weights") :
                      createFeatureNames(targetName, "mean", "sigma", "median", "mode")
        );
        this.outputSums = outputSums;
        this.varIndex = ctx.getVariableIndex(varName);
        this.weightFn = WeightFn.createPow(weightCoeff);
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

        @Parameter(label = "Source band name", notEmpty = true, notNull = true, description = "The source band used for aggregation.")
        String varName;
        @Parameter(label = "Target band name prefix (optional)", description = "The name prefix for the resulting bands. If empty, the source band name is used.")
        String targetName;
        @Parameter(defaultValue = "0.5", description = "The number of spatial observation to the power of this value \n" +
                                                       "will define the weighting factor of the sums.")
        Double weightCoeff;
        @Parameter(defaultValue = "false",
                   description = "If true, the result will include the sum of all values.")
        Boolean outputSums;

        public Config() {
            this(null, null, 0.0, false);
        }

        public Config(String varName, String targetName, double weightCoeff, boolean outputSums) {
            super(Descriptor.NAME);
            this.varName = varName;
            this.targetName = targetName;
            this.weightCoeff = weightCoeff;
            this.outputSums = outputSums;
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
            Config config = ((Config) aggregatorConfig);
            boolean outputSums = config.outputSums != null ? config.outputSums : false;
            String targetName = StringUtils.isNotNullAndNotEmpty(config.targetName)  ? config.targetName : config.varName;
            double weightCoeff = config.weightCoeff != null ? config.weightCoeff : 0.5;
            return new AggregatorAverageML(varCtx, config.varName, targetName, weightCoeff, outputSums);
        }

        @Override
        public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
            Config config = (Config) aggregatorConfig;
            return new String[]{config.varName};
        }

        @Override
        public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
            Config config = ((Config) aggregatorConfig);
            boolean outputSums = config.outputSums != null ? config.outputSums : false;
            String targetName = StringUtils.isNotNullAndNotEmpty(config.targetName)  ? config.targetName : config.varName;
            return outputSums ?
                    createFeatureNames(targetName, "sum", "sum_sq", "weights") :
                    createFeatureNames(targetName, "mean", "sigma", "median", "mode");
        }
    }
}
