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
import org.esa.snap.binning.support.GrowableVector;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.util.StringUtils;

import java.util.Arrays;

/**
 * An aggregator that computes the p-th percentile,
 * the value of a variable below which a certain percent (p) of observations fall.
 *
 * @author MarcoZ
 * @author Norman
 */
public class AggregatorPercentile extends AbstractAggregator {

    private final int varIndex;
    private final int percentage;
    private final String mlName;
    private final String icName;

    public AggregatorPercentile(VariableContext varCtx, String varName, String targetName, int percentage) {
        super(Descriptor.NAME,
              createFeatureNames(varName, "sum"),
              createFeatureNames(varName, "p" + percentage),
              createFeatureNames(targetName, "p" + percentage));

        if (varCtx == null) {
            throw new NullPointerException("varCtx");
        }
        if (varName == null) {
            throw new NullPointerException("varName");
        }
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("percentage < 0 || percentage > 100");
        }
        this.varIndex = varCtx.getVariableIndex(varName);
        this.percentage = percentage;
        this.mlName = "ml." + varName;
        this.icName = "ic." + varName;
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.0f);
        ctx.put(icName, new int[1]);
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Observation observationVector, WritableVector spatialVector) {
        float value = observationVector.get(varIndex);
        if (!Float.isNaN(value)) {
            spatialVector.set(0, spatialVector.get(0) + value);
        } else {
            // We count invalids rather than valid because it is more efficient.
            // (Key/value map operations are relatively slow, and it is more likely that we will receive valid measurements.)
            ((int[]) ctx.get(icName))[0]++;
        }
    }

    @Override
    public void completeSpatial(BinContext ctx, int numSpatialObs, WritableVector spatialVector) {
        Integer invalidCount = ((int[]) ctx.get(icName))[0];
        int effectiveCount = numSpatialObs - invalidCount;
        if (effectiveCount > 0) {
            spatialVector.set(0, spatialVector.get(0) / effectiveCount);
        } else {
            spatialVector.set(0, Float.NaN);
        }
    }

    @Override
    public void initTemporal(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.0f);
        ctx.put(mlName, new GrowableVector(256));
    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs, WritableVector temporalVector) {
        GrowableVector measurementsVec = ctx.get(mlName);
        float value = spatialVector.get(0);
        if (!Float.isNaN(value)) {
            measurementsVec.add(value);
        }
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {
        GrowableVector measurementsVec = ctx.get(mlName);
        float[] measurements = measurementsVec.getElements();
        if (measurements.length > 0) {
            Arrays.sort(measurements);
            temporalVector.set(0, computePercentile(percentage, measurements));
        } else {
            temporalVector.set(0, Float.NaN);
        }
    }


    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        outputVector.set(0, temporalVector.get(0));
    }

    @Override
    public String toString() {
        return "AggregatorPercentile{" +
               "varIndex=" + varIndex +
               ", percentage=" + percentage +
               ", spatialFeatureNames=" + Arrays.toString(getSpatialFeatureNames()) +
               ", temporalFeatureNames=" + Arrays.toString(getTemporalFeatureNames()) +
               ", outputFeatureNames=" + Arrays.toString(getOutputFeatureNames()) +
               '}';
    }

    /**
     * Computes the p-th percentile of an array of measurements following
     * the "Engineering Statistics Handbook: Percentile". NIST.
     * http://www.itl.nist.gov/div898/handbook/prc/section2/prc252.htm.
     * Retrieved 2011-03-16.
     *
     * @param p            The percentage in percent ranging from 0 to 100.
     * @param measurements Sorted array of measurements.
     * @return The  p-th percentile.
     */
    public static float computePercentile(int p, float[] measurements) {
        int N = measurements.length;
        float n = (p / 100.0F) * (N + 1);
        int k = (int) Math.floor(n);
        float d = n - k;
        float yp;
        if (k == 0) {
            yp = measurements[0];
        } else if (k >= N) {
            yp = measurements[N - 1];
        } else {
            yp = measurements[k - 1] + d * (measurements[k] - measurements[k - 1]);
        }
        return yp;
    }

    public static class Config extends AggregatorConfig {

        @Parameter(label = "Source band name", notEmpty = true, notNull = true, description = "The source band used for aggregation.")
        String varName;
        @Parameter(label = "Target band name prefix (optional)", description = "The name prefix for the resulting bands. If empty, the source band name is used")
        String targetName;
        @Parameter(label = "Percentile", defaultValue = "90", interval = "[0,100]",
                   description = "The percentile to be created. Must be in the interval [0..100].")
        Integer percentage;

        public Config() {
            this(null, null, 90);
        }

        public Config(String targetName, String varName, int percentage) {
            super(Descriptor.NAME);
            this.targetName = targetName;
            this.varName = varName;
            this.percentage = percentage;
        }
    }


    private static int getEffectivePercentage(Integer percentage) {
        return (percentage != null ? percentage : 90);
    }

    public static class Descriptor implements AggregatorDescriptor {

        public static final String NAME = "PERCENTILE";

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
            String targetName = StringUtils.isNotNullAndNotEmpty(config.targetName)  ? config.targetName : config.varName;
            int effectivePercentage = getEffectivePercentage(config.percentage);
            return new AggregatorPercentile(varCtx, config.varName, targetName, effectivePercentage);
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
            int percentage = getEffectivePercentage(config.percentage);
            return createFeatureNames(targetName, "p" + percentage);
        }
    }
}
