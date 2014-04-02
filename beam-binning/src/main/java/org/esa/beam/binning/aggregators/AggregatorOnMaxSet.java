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

import org.esa.beam.binning.AbstractAggregator;
import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.Observation;
import org.esa.beam.binning.VariableContext;
import org.esa.beam.binning.Vector;
import org.esa.beam.binning.WritableVector;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.util.StringUtils;

import java.util.Arrays;

/**
 * An aggregator that sets an output if an input is maximal.
 */
public final class AggregatorOnMaxSet extends AbstractAggregator {

    private final int[] varIndexes;
    private int numFeatures;

    public AggregatorOnMaxSet(VariableContext varCtx, String targetName, String... varNames) {
        super(Descriptor.NAME, createFeatureNames(varNames), createFeatureNames(varNames), createOutputFeatureNames(targetName, varNames));
        if (varCtx == null) {
            throw new NullPointerException("varCtx");
        }
        numFeatures = varNames.length;
        varIndexes = new int[varNames.length];
        for (int i = 0; i < varNames.length; i++) {
            int varIndex = varCtx.getVariableIndex(varNames[i]);
            if (varIndex < 0) {
                throw new IllegalArgumentException("varIndex < 0");
            }
            varIndexes[i] = varIndex;
        }
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        vector.set(0, Float.NEGATIVE_INFINITY);
    }

    @Override
    public void initTemporal(BinContext ctx, WritableVector vector) {
        vector.set(0, Float.NEGATIVE_INFINITY);
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Observation observationVector, WritableVector spatialVector) {
        final float value = observationVector.get(varIndexes[0]);
        final float currentMax = spatialVector.get(0);
        if (value > currentMax) {
            spatialVector.set(0, value);
            spatialVector.set(1, (float) observationVector.getMJD());
            for (int i = 1; i < numFeatures; i++) {
                spatialVector.set(i + 1, observationVector.get(varIndexes[i]));
            }
        }
    }

    @Override
    public void completeSpatial(BinContext ctx, int numObs, WritableVector numSpatialObs) {
    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs,
                                  WritableVector temporalVector) {
        final float value = spatialVector.get(0);
        final float currentMax = temporalVector.get(0);
        if (value > currentMax) {
            temporalVector.set(0, value);
            for (int i = 1; i < numFeatures + 1; i++) {
                temporalVector.set(i, spatialVector.get(i));
            }
        }
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {
    }

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        for (int i = 0; i < numFeatures + 1; i++) {
            float value = temporalVector.get(i);
            if (Float.isInfinite(value)) {
                value = Float.NaN;
            }
            outputVector.set(i, value);
        }
    }

    @Override
    public String toString() {
        return "AggregatorOnMaxSet{" +
               "varIndexes=" + Arrays.toString(varIndexes) +
               ", spatialFeatureNames=" + Arrays.toString(getSpatialFeatureNames()) +
               ", temporalFeatureNames=" + Arrays.toString(getTemporalFeatureNames()) +
               ", outputFeatureNames=" + Arrays.toString(getOutputFeatureNames()) +
               '}';
    }

    private static String[] createFeatureNames(String[] varNames) {
        if (varNames == null) {
            throw new NullPointerException("varNames");
        }
        if (varNames.length == 0) {
            throw new IllegalArgumentException("varNames.length == 0");
        }
        return createOutputFeatureNames(varNames[0], varNames);
    }

    private static String[] createOutputFeatureNames(String targetName, String[] varNames) {
        if (StringUtils.isNullOrEmpty(targetName)) {
            throw new IllegalArgumentException("targetName must not be empty");
        }
        String[] featureNames = new String[varNames.length + 1];
        featureNames[0] = targetName + "_max";
        featureNames[1] = targetName + "_mjd";
        System.arraycopy(varNames, 1, featureNames, 2, varNames.length - 1);
        return featureNames;
    }

    public static class Config extends AggregatorConfig {

        @Parameter(notNull = true)
        String[] varNames;

        @Parameter(notEmpty = true, notNull = false)
        String targetName;

        public Config() {
            super(Descriptor.NAME);
        }

        public Config(String targetName, String... varNames) {
            super(Descriptor.NAME);
            this.targetName = targetName;
            this.varNames = varNames;
        }

        @Override
        public String[] getSourceVarNames() {
            return varNames;
        }
    }


    public static class Descriptor implements AggregatorDescriptor {

        public static final String NAME = "ON_MAX_SET";

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
            Config config = (Config) aggregatorConfig;
            String targetVarName = config.targetName;
            String[] sourceVarNames = config.varNames;
            if (targetVarName == null && sourceVarNames.length > 0) {
                targetVarName = sourceVarNames[0];
            }
            return new AggregatorOnMaxSet(varCtx, targetVarName, sourceVarNames);
        }
    }
}
