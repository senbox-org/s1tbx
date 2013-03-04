/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.beam.binning.*;
import org.esa.beam.framework.gpf.annotations.Parameter;

import java.util.Arrays;

/**
 * An aggregator that computes an average.
 */
public final class AggregatorAverage extends AbstractAggregator {
    private final int varIndex;
    private final WeightFn weightFn;

    public AggregatorAverage(VariableContext varCtx, String varName, Double weightCoeff, Number fillValue) {
        super(Descriptor.NAME,
              createFeatureNames(varName, "sum", "sum_sq"),
              createFeatureNames(varName, "sum", "sum_sq", "weights"),
              createFeatureNames(varName, "mean", "sigma"),
              fillValue);
        if (varCtx == null) {
            throw new NullPointerException("varCtx");
        }
        if (varName == null) {
            throw new NullPointerException("varName");
        }
        if (weightCoeff != null && weightCoeff < 0.0) {
            throw new IllegalArgumentException("weightCoeff < 0.0");
        }
        this.varIndex = varCtx.getVariableIndex(varName);
        this.weightFn = WeightFn.createPow(weightCoeff != null ? weightCoeff : 0.0);
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
        final float value = observationVector.get(varIndex);
        spatialVector.set(0, spatialVector.get(0) + value);
        spatialVector.set(1, spatialVector.get(1) + value * value);
    }

    @Override
    public void completeSpatial(BinContext ctx, int numSpatialObs, WritableVector spatialVector) {
        spatialVector.set(0, spatialVector.get(0) / numSpatialObs);
        spatialVector.set(1, spatialVector.get(1) / numSpatialObs);
    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs, WritableVector temporalVector) {
        final float w = weightFn.eval(numSpatialObs);
        temporalVector.set(0, temporalVector.get(0) + spatialVector.get(0) * w);
        temporalVector.set(1, temporalVector.get(1) + spatialVector.get(1) * w);
        temporalVector.set(2, temporalVector.get(2) + w);
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {
    }

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        final double sumX = temporalVector.get(0);
        final double sumXX = temporalVector.get(1);
        final double sumW = temporalVector.get(2);
        final double mean = sumX / sumW;
        final double sigmaSqr = sumXX / sumW - mean * mean;
        final double sigma = sigmaSqr > 0.0 ? Math.sqrt(sigmaSqr) : 0.0 * sigmaSqr; // multiplication to pass through NaN
        outputVector.set(0, (float) mean);
        outputVector.set(1, (float) sigma);
    }

    @Override
    public String toString() {
        return "AggregatorAverage{" +
                ", varIndex=" + varIndex +
                ", weightFn=" + weightFn +
                ", spatialFeatureNames=" + Arrays.toString(getSpatialFeatureNames()) +
                ", temporalFeatureNames=" + Arrays.toString(getTemporalFeatureNames()) +
                ", outputFeatureNames=" + Arrays.toString(getOutputFeatureNames()) +
                '}';
    }

    public static class Config extends AggregatorConfig {
        @Parameter(notEmpty = true, notNull = true)
        String varName;
        @Parameter
        Double weightCoeff;
        @Parameter
        Float fillValue;


        public Config() {
            this(null, null, null);
        }

        public Config(String varName, Double weightCoeff, Float fillValue) {
            super(Descriptor.NAME);
            this.varName = varName;
            this.weightCoeff = weightCoeff;
            this.fillValue = fillValue;
        }

        public void setVarName(String varName) {
            this.varName = varName;
        }

        @Override
        public String[] getVarNames() {
            return new String[]{varName};
        }
    }

    public static class Descriptor implements AggregatorDescriptor {

        public static final String NAME = "AVG";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
            PropertySet propertySet = aggregatorConfig.asPropertySet();
            return new AggregatorAverage(varCtx,
                                         (String) propertySet.getValue("varName"),
                                         (Double) propertySet.getValue("weightCoeff"),
                                         (Float) propertySet.getValue("fillValue"));
        }

        @Override
        public AggregatorConfig createAggregatorConfig() {
            return new Config();
        }
    }
}
