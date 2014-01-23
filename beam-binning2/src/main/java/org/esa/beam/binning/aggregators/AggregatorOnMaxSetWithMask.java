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
import org.esa.beam.binning.WritableVector;
import org.esa.beam.framework.gpf.annotations.Parameter;

import java.util.Arrays;

/**
 * An aggregator that sets an output if an input is maximal.
 */
public final class AggregatorOnMaxSetWithMask extends AbstractAggregator {

    private final int maskIndex;
    private final int onMaxIndex;
    private final int[] setIndexes;
    private int numFeatures;

    public AggregatorOnMaxSetWithMask(VariableContext varCtx, String onMaxName, String maskName, String... setNames) {
        super(Descriptor.NAME, createFeatures(onMaxName, setNames));
        if (varCtx == null) {
            throw new NullPointerException("varCtx");
        }
        numFeatures = setNames.length + 3;
        setIndexes = new int[setNames.length];
        for (int i = 0; i < setNames.length; i++) {
            int varIndex = varCtx.getVariableIndex(setNames[i]);
            if (varIndex < 0) {
                throw new IllegalArgumentException("setNames[" + i + "] < 0");
            }
            setIndexes[i] = varIndex;
        }
        onMaxIndex = varCtx.getVariableIndex(onMaxName);
        if (onMaxIndex < 0) {
            throw new IllegalArgumentException("onMaxIndex < 0");
        }
        maskIndex = varCtx.getVariableIndex(maskName);
        if (maskIndex < 0) {
            throw new IllegalArgumentException("maskIndex < 0");
        }
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        vector.set(0, Float.NEGATIVE_INFINITY);
        vector.set(2, 0f);
    }

    @Override
    public void initTemporal(BinContext ctx, WritableVector vector) {
        vector.set(0, Float.NEGATIVE_INFINITY);
        vector.set(2, 0f);
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Observation observationVector, WritableVector spatialVector) {
        final float maskValue = observationVector.get(maskIndex);
        if (maskValue > 0) {
            //  increase counter
            spatialVector.set(2, spatialVector.get(2) + 1);
            // compare against current_max
            final float onMaxValue = observationVector.get(onMaxIndex);
            final float currentMax = spatialVector.get(0);
            if (onMaxValue > currentMax) {
                spatialVector.set(0, onMaxValue);
                spatialVector.set(1, (float) observationVector.getMJD());
                for (int i = 0; i < setIndexes.length; i++) {
                    spatialVector.set(i + 3, observationVector.get(setIndexes[i]));
                }
            }
        }
    }

    @Override
    public void completeSpatial(BinContext ctx, int numObs, WritableVector numSpatialObs) {
    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs,
                                  WritableVector temporalVector) {
        final float counterValue = spatialVector.get(2);
        if (counterValue > 0) {
            temporalVector.set(2, temporalVector.get(2) + counterValue);
            final float onMaxValue = spatialVector.get(0);
            final float currentMax = temporalVector.get(0);
            if (onMaxValue > currentMax) {
                temporalVector.set(0, onMaxValue);
                temporalVector.set(1, spatialVector.get(1));
                for (int i = 0; i < setIndexes.length; i++) {
                    temporalVector.set(i + 3, spatialVector.get(i + 3));
                }
            }
        }
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {
    }

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        final float counterValue = temporalVector.get(2);
        if (counterValue > 0) {
            for (int i = 0; i < numFeatures; i++) {
                outputVector.set(i, temporalVector.get(i));
            }
        } else {
            for (int i = 0; i < numFeatures; i++) {
                outputVector.set(i, Float.NaN);
            }
        }
    }

    @Override
    public String toString() {
        return "AggregatorOnMaxSetWithMask{" +
               "setIndexes=" + Arrays.toString(setIndexes) +
               ", spatialFeatureNames=" + Arrays.toString(getSpatialFeatureNames()) +
               ", temporalFeatureNames=" + Arrays.toString(getTemporalFeatureNames()) +
               ", outputFeatureNames=" + Arrays.toString(getOutputFeatureNames()) +
               '}';
    }

    private static String[] createFeatures(String onMaxName, String[] setNames) {
        if (setNames == null) {
            throw new NullPointerException("setNames");
        }

        String[] featureNames = new String[setNames.length + 3];
        featureNames[0] = onMaxName + "_max";
        featureNames[1] = onMaxName + "_mjd";
        featureNames[2] = onMaxName + "_count";
        System.arraycopy(setNames, 0, featureNames, 3, setNames.length);
        return featureNames;
    }

    public static class Config extends AggregatorConfig {

        @Parameter
        String onMaxName;
        @Parameter
        String maskName;
        @Parameter
        String[] setNames;

        public Config() {
            super(Descriptor.NAME);
        }

        @Override
        public String[] getVarNames() {
            int varNameLength = 2;
            if (setNames != null) {
                varNameLength += setNames.length;
            }
            String[] varNames = new String[varNameLength];
            varNames[0] = onMaxName;
            varNames[1] = maskName;
            if (setNames != null) {
                System.arraycopy(setNames, 0, varNames, 2, setNames.length);
            }
            return varNames;
        }
    }


    public static class Descriptor implements AggregatorDescriptor {

        public static final String NAME = "ON_MAX_SET_WITH_MASK";

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
            String onMaxName = propertySet.getValue("onMaxName");
            String maskName = propertySet.getValue("maskName");
            String[] setNames = propertySet.getValue("setNames");
            if (setNames == null) {
                return new AggregatorOnMaxSetWithMask(varCtx, onMaxName, maskName);
            } else {
                return new AggregatorOnMaxSetWithMask(varCtx, onMaxName, maskName, setNames);
            }
        }
    }
}
