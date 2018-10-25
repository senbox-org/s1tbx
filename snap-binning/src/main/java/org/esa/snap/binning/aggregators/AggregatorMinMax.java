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
import org.esa.snap.binning.WritableVector;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.util.StringUtils;

import java.util.Arrays;

/**
 * An aggregator that computes the minimum and maximum values.
 */
public class AggregatorMinMax extends AbstractAggregator {

    private final int varIndex;

    public AggregatorMinMax(VariableContext varCtx, String varName, String targetVarName) {
        super(Descriptor.NAME, createFeatureNames(varName, "min", "max"), createFeatureNames(varName, "min", "max"), createFeatureNames(targetVarName, "min", "max"));

        if (varCtx == null) {
            throw new NullPointerException("varCtx");
        }
        if (varName == null) {
            throw new NullPointerException("varName");
        }
        this.varIndex = varCtx.getVariableIndex(varName);
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        vector.set(0, Float.POSITIVE_INFINITY);
        vector.set(1, Float.NEGATIVE_INFINITY);
    }

    @Override
    public void initTemporal(BinContext ctx, WritableVector vector) {
        vector.set(0, Float.POSITIVE_INFINITY);
        vector.set(1, Float.NEGATIVE_INFINITY);
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Observation observationVector, WritableVector spatialVector) {
        final float value = observationVector.get(varIndex);
        if (!Float.isNaN(value)) {
            spatialVector.set(0, Math.min(spatialVector.get(0), value));
            spatialVector.set(1, Math.max(spatialVector.get(1), value));
        }
    }

    @Override
    public void completeSpatial(BinContext ctx, int numObs, WritableVector numSpatialObs) {
    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs,
                                  WritableVector temporalVector) {
        temporalVector.set(0, Math.min(temporalVector.get(0), spatialVector.get(0)));
        temporalVector.set(1, Math.max(temporalVector.get(1), spatialVector.get(1)));
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {
        temporalVector.set(0, Float.isInfinite(temporalVector.get(0)) ? Float.NaN : temporalVector.get(0));
        temporalVector.set(1, Float.isInfinite(temporalVector.get(1)) ? Float.NaN : temporalVector.get(1));
    }

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        outputVector.set(0, temporalVector.get(0));
        outputVector.set(1, temporalVector.get(1));
    }

    @Override
    public String toString() {
        return "AggregatorMinMax{" +
               "varIndex=" + varIndex +
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

        public Config() {
            this(null, null);
        }

        public Config(String targetName, String varName) {
            super(Descriptor.NAME);
            this.targetName = targetName;
            this.varName = varName;
        }
    }

    public static class Descriptor implements AggregatorDescriptor {

        public static final String NAME = "MIN_MAX";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
            Config config = (Config) aggregatorConfig;
            String targetName = config.targetName != null ? config.targetName : config.varName;
            return new AggregatorMinMax(varCtx, config.varName, targetName);
        }

        @Override
        public AggregatorConfig createConfig() {
            return new Config();
        }

        @Override
        public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
            Config config = (Config) aggregatorConfig;
            return new String[]{config.varName};
        }

        @Override
        public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
            Config config = (Config) aggregatorConfig;
            String targetName = StringUtils.isNotNullAndNotEmpty(config.targetName)  ? config.targetName : config.varName;
            return createFeatureNames(targetName, "min", "max");
        }

    }
}
