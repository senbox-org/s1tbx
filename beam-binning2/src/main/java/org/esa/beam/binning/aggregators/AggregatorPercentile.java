/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.beam.binning.support.GrowableVector;
import org.esa.beam.framework.gpf.annotations.Parameter;

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

    public AggregatorPercentile(VariableContext varCtx, String varName, Integer percentage, Number fillValue) {
        this(varCtx, varName, percentage != null ? percentage : 90, fillValue);
    }

    private AggregatorPercentile(VariableContext varCtx, String varName, int percentage, Number fillValue) {
        super(Descriptor.NAME,
              createFeatureNames(varName, "sum"),
              createFeatureNames(varName, "p" + percentage),
              createFeatureNames(varName, "p" + percentage),
              fillValue);

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
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.0f);
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Observation observationVector, WritableVector spatialVector) {
        final float value = observationVector.get(varIndex);
        spatialVector.set(0, spatialVector.get(0) + value);
    }

    @Override
    public void completeSpatial(BinContext ctx, int numSpatialObs, WritableVector spatialVector) {
        spatialVector.set(0, spatialVector.get(0) / numSpatialObs);
    }

    @Override
    public void initTemporal(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.0f);
        ctx.put("ml", new GrowableVector(256));
    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs, WritableVector temporalVector) {
        GrowableVector measurementsVec = ctx.get("ml");
        measurementsVec.add(spatialVector.get(0));
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {
        GrowableVector measurementsVec = ctx.get("ml");
        float[] measurements = measurementsVec.getElements();
        Arrays.sort(measurements);
        temporalVector.set(0, computePercentile(percentage, measurements));
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
        @Parameter
        String varName;
        @Parameter
        Integer percentage;
        @Parameter
        Float fillValue;

        public Config() {
            super(Descriptor.NAME);
        }

        @Override
        public String[] getVarNames() {
            return new String[] {varName};
        }
    }


    public static class Descriptor implements AggregatorDescriptor {

        public static final String NAME = "PERCENTILE";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public AggregatorConfig createAggregatorConfig() {
            return new Config();
        }

        @Override
        public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
            PropertySet propertySet = aggregatorConfig.asPropertySet();
            return new AggregatorPercentile(varCtx,
                                            (String)propertySet.getValue("varName"),
                                            (Integer) propertySet.getValue("percentage"),
                                            (Float) propertySet.getValue("fillValue"));
        }
    }
}
