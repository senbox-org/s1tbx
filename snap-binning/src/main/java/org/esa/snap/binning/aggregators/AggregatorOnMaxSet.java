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
 * An aggregator that sets an output if an input is maximal.
 */
public final class AggregatorOnMaxSet extends AbstractAggregator {

    private final int onMaxIndex;
    private final int[] setIndexes;
    private final int numSetFeatures;

    public AggregatorOnMaxSet(VariableContext varCtx, String onMaxVarName, String targetName, String... setVarNames) {
        super(Descriptor.NAME,
              createOutputFeatureNames(onMaxVarName, setVarNames),
              createOutputFeatureNames(onMaxVarName, setVarNames),
              createOutputFeatureNames(targetName, setVarNames));
        if (varCtx == null) {
            throw new NullPointerException("varCtx");
        }
        numSetFeatures = setVarNames.length;
        onMaxIndex = varCtx.getVariableIndex(onMaxVarName);
        if (onMaxIndex < 0) {
            throw new IllegalArgumentException("onMaxIndex < 0");
        }
        setIndexes = new int[setVarNames.length];
        for (int i = 0; i < setVarNames.length; i++) {
            int varIndex = varCtx.getVariableIndex(setVarNames[i]);
            if (varIndex < 0) {
                throw new IllegalArgumentException("setIndexes[" + i + "] < 0");
            }
            setIndexes[i] = varIndex;
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
        final float value = observationVector.get(onMaxIndex);
        final float currentMax = spatialVector.get(0);
        if (value > currentMax) {
            spatialVector.set(0, value);
            spatialVector.set(1, (float) observationVector.getMJD());
            for (int i = 0; i < numSetFeatures; i++) {
                spatialVector.set(i + 2, observationVector.get(setIndexes[i]));
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
            for (int i = 0; i < numSetFeatures + 1; i++) {
                temporalVector.set(i + 1, spatialVector.get(i + 1));
            }
        }
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {
    }

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        for (int i = 0; i < numSetFeatures + 2; i++) {
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
               "onMaxIndex=" + onMaxIndex +
               "setIndexes=" + Arrays.toString(setIndexes) +
               ", spatialFeatureNames=" + Arrays.toString(getSpatialFeatureNames()) +
               ", temporalFeatureNames=" + Arrays.toString(getTemporalFeatureNames()) +
               ", outputFeatureNames=" + Arrays.toString(getOutputFeatureNames()) +
               '}';
    }

    private static String[] createOutputFeatureNames(String targetName, String[] setVarNames) {
        if (StringUtils.isNullOrEmpty(targetName)) {
            throw new IllegalArgumentException("targetName must not be empty");
        }
        String[] featureNames = new String[setVarNames.length + 2];
        featureNames[0] = targetName + "_max";
        featureNames[1] = targetName + "_mjd";
        System.arraycopy(setVarNames, 0, featureNames, 2, setVarNames.length);
        return featureNames;
    }

    public static class Config extends AggregatorConfig {

        @Parameter(label = "Maximum band name", notEmpty = true, notNull = true,
                   description = "If this band reaches its maximum the values of the source bands are taken.")
        String onMaxVarName;
        @Parameter(label = "Target band name prefix (optional)", description = "The name prefix for the resulting maximum bands. " +
                                                                    "If empty, the source band name is used")
        String targetName;
        @Parameter(label = "Source band names", notNull = true, description = "The source bands used for aggregation when maximum band reaches its maximum.")
        String[] setVarNames;

        public Config() {
            this(null, null);
        }

        public Config(String targetName, String onMaxVarName, String... setVarNames) {
            super(Descriptor.NAME);
            this.targetName = targetName;
            this.onMaxVarName = onMaxVarName;
            this.setVarNames = setVarNames;
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
            String targetName = StringUtils.isNotNullAndNotEmpty(config.targetName)  ? config.targetName : config.onMaxVarName;
            return new AggregatorOnMaxSet(varCtx, config.onMaxVarName, targetName, config.setVarNames);
        }

        @Override
        public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
            Config config = (Config) aggregatorConfig;
            int varNameLength = 1;
            if (config.setVarNames != null) {
                varNameLength += config.setVarNames.length;
            }
            String[] varNames = new String[varNameLength];
            varNames[0] = config.onMaxVarName;
            if (config.setVarNames != null) {
                System.arraycopy(config.setVarNames, 0, varNames, 1, config.setVarNames.length);
            }
            return varNames;
        }

        @Override
        public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
            Config config = (Config) aggregatorConfig;
            String targetName = StringUtils.isNotNullAndNotEmpty(config.targetName)  ? config.targetName : config.onMaxVarName;
            String[] setVarNames = config.setVarNames != null ? config.setVarNames : new String[0];
            return createOutputFeatureNames(targetName, setVarNames);
        }
    }
}
