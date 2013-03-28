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

package org.esa.beam.binning;

import org.esa.beam.binning.support.VariableContextImpl;
import org.esa.beam.binning.support.VectorImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * The bin manager class comprises a number of {@link Aggregator}s
 *
 * @author Norman Fomferra
 */
public class BinManager {

    private final VariableContext variableContext;
    private final Aggregator[] aggregators;
    private final int spatialFeatureCount;
    private final int temporalFeatureCount;
    private final int outputFeatureCount;
    private final int[] spatialFeatureOffsets;
    private final int[] temporalFeatureOffsets;
    private final int[] outputFeatureOffsets;
    private final String[] outputFeatureNames;
    private final double[] outputFeatureFillValues;

    public BinManager() {
        this(new VariableContextImpl());
    }

    public BinManager(VariableContext variableContext, Aggregator... aggregators) {
        this.variableContext = variableContext;
        this.aggregators = aggregators;
        this.spatialFeatureOffsets = new int[aggregators.length];
        this.temporalFeatureOffsets = new int[aggregators.length];
        this.outputFeatureOffsets = new int[aggregators.length];
        int spatialFeatureCount = 0;
        int temporalFeatureCount = 0;
        int outputFeatureCount = 0;
        for (int i = 0; i < aggregators.length; i++) {
            Aggregator aggregator = aggregators[i];
            spatialFeatureOffsets[i] = spatialFeatureCount;
            temporalFeatureOffsets[i] = temporalFeatureCount;
            outputFeatureOffsets[i] = outputFeatureCount;
            spatialFeatureCount += aggregator.getSpatialFeatureNames().length;
            temporalFeatureCount += aggregator.getTemporalFeatureNames().length;
            outputFeatureCount += aggregator.getOutputFeatureNames().length;
        }
        this.spatialFeatureCount = spatialFeatureCount;
        this.temporalFeatureCount = temporalFeatureCount;
        this.outputFeatureCount = outputFeatureCount;
        this.outputFeatureNames = new String[outputFeatureCount];
        this.outputFeatureFillValues = new double[outputFeatureCount];
        final NameUnifier nameUnifier = new NameUnifier();
        int k = 0;
        for (Aggregator aggregator : aggregators) {
            for (int i = 0; i < aggregator.getOutputFeatureNames().length; i++) {
                outputFeatureNames[k] = nameUnifier.unifyName(aggregator.getOutputFeatureNames()[i]);
                outputFeatureFillValues[k] = aggregator.getOutputFillValue();
                k++;
            }
        }
    }

    public VariableContext getVariableContext() {
        return variableContext;
    }

    public final int getSpatialFeatureCount() {
        return spatialFeatureCount;
    }

    public final int getTemporalFeatureCount() {
        return temporalFeatureCount;
    }

    public final String[] getOutputFeatureNames() {
        return outputFeatureNames;
    }

    public final double getOutputFeatureFillValue(int i) {
        return outputFeatureFillValues[i];
    }

    public final int getAggregatorCount() {
        return aggregators.length;
    }

    public final Aggregator getAggregator(int aggIndex) {
        return aggregators[aggIndex];
    }

    public final Vector getSpatialVector(SpatialBin bin, int aggIndex) {
        final VectorImpl vector = new VectorImpl(bin.featureValues);
        final Aggregator aggregator = aggregators[aggIndex];
        vector.setOffsetAndSize(spatialFeatureOffsets[aggIndex], aggregator.getSpatialFeatureNames().length);
        return vector;
    }

    public final Vector getTemporalVector(TemporalBin bin, int aggIndex) {
        final VectorImpl vector = new VectorImpl(bin.featureValues);
        final Aggregator aggregator = aggregators[aggIndex];
        vector.setOffsetAndSize(temporalFeatureOffsets[aggIndex], aggregator.getTemporalFeatureNames().length);
        return vector;
    }

    public WritableVector createOutputVector() {
        return new VectorImpl(new float[outputFeatureCount]);
    }

    public SpatialBin createSpatialBin(long binIndex) {
        final SpatialBin spatialBin = new SpatialBin(binIndex, spatialFeatureCount);
        initSpatialBin(spatialBin);
        return spatialBin;
    }

    public void aggregateSpatialBin(Observation observation, SpatialBin spatialBin) {
        final VectorImpl spatialVector = new VectorImpl(spatialBin.featureValues);
        for (int i = 0; i < aggregators.length; i++) {
            final Aggregator aggregator = aggregators[i];
            spatialVector.setOffsetAndSize(spatialFeatureOffsets[i], aggregator.getSpatialFeatureNames().length);
            aggregator.aggregateSpatial(spatialBin, observation, spatialVector);
        }
        spatialBin.numObs++;
    }

    public void completeSpatialBin(SpatialBin spatialBin) {
        final VectorImpl spatialVector = new VectorImpl(spatialBin.featureValues);
        for (int i = 0; i < aggregators.length; i++) {
            final Aggregator aggregator = aggregators[i];
            spatialVector.setOffsetAndSize(spatialFeatureOffsets[i], aggregator.getSpatialFeatureNames().length);
            aggregator.completeSpatial(spatialBin, spatialBin.numObs, spatialVector);
        }
    }

    public TemporalBin createTemporalBin(long binIndex) {
        final TemporalBin temporalBin = new TemporalBin(binIndex, temporalFeatureCount);
        initTemporalBin(temporalBin);
        return temporalBin;
    }

    public TemporalBin createTemporalBin(long binIndex, Iterable<? extends SpatialBin> spatialBins) {
        TemporalBin temporalBin = createTemporalBin(binIndex);
        for (SpatialBin spatialBin : spatialBins) {
            aggregateTemporalBin(spatialBin, temporalBin);
        }
        completeTemporalBin(temporalBin);
        return temporalBin;
    }

    public void aggregateTemporalBin(SpatialBin inputBin, TemporalBin outputBin) {
        aggregateBin(inputBin, outputBin);
        outputBin.numPasses++;
    }

    public void aggregateTemporalBin(TemporalBin inputBin, TemporalBin outputBin) {
        aggregateBin(inputBin, outputBin);
        outputBin.numPasses += inputBin.numPasses;
    }

    private void aggregateBin(Bin inputBin, Bin outputBin) {
        final VectorImpl spatialVector = new VectorImpl(inputBin.featureValues);
        final VectorImpl temporalVector = new VectorImpl(outputBin.featureValues);
        for (int i = 0; i < aggregators.length; i++) {
            final Aggregator aggregator = aggregators[i];
            spatialVector.setOffsetAndSize(spatialFeatureOffsets[i], aggregator.getSpatialFeatureNames().length);
            temporalVector.setOffsetAndSize(temporalFeatureOffsets[i], aggregator.getTemporalFeatureNames().length);
            aggregator.aggregateTemporal(outputBin, spatialVector, inputBin.numObs, temporalVector);
        }
        outputBin.numObs += inputBin.numObs;
    }

    public void completeTemporalBin(TemporalBin temporalBin) {
        final VectorImpl temporalVector = new VectorImpl(temporalBin.featureValues);
        for (int i = 0; i < aggregators.length; i++) {
            final Aggregator aggregator = aggregators[i];
            temporalVector.setOffsetAndSize(temporalFeatureOffsets[i], aggregator.getTemporalFeatureNames().length);
            aggregator.completeTemporal(temporalBin, temporalBin.numObs, temporalVector);
        }
    }

    public void computeOutput(TemporalBin temporalBin, WritableVector outputVector) {
        final VectorImpl temporalVector = new VectorImpl(temporalBin.featureValues);
        final VectorImpl outputVectorImpl = (VectorImpl) outputVector;
        for (int i = 0; i < aggregators.length; i++) {
            final Aggregator aggregator = aggregators[i];
            temporalVector.setOffsetAndSize(temporalFeatureOffsets[i], aggregator.getTemporalFeatureNames().length);
            outputVectorImpl.setOffsetAndSize(outputFeatureOffsets[i], aggregator.getOutputFeatureNames().length);
            aggregator.computeOutput(temporalVector, outputVector);
        }
        outputVectorImpl.setOffsetAndSize(0, outputFeatureCount);
    }

    protected void initSpatialBin(SpatialBin bin) {
        final VectorImpl vector = new VectorImpl(bin.featureValues);
        for (int i = 0; i < aggregators.length; i++) {
            final Aggregator aggregator = aggregators[i];
            vector.setOffsetAndSize(spatialFeatureOffsets[i], aggregator.getSpatialFeatureNames().length);
            aggregator.initSpatial(bin, vector);
        }
    }

    protected void initTemporalBin(TemporalBin bin) {
        final VectorImpl vector = new VectorImpl(bin.featureValues);
        for (int i = 0; i < aggregators.length; i++) {
            final Aggregator aggregator = aggregators[i];
            vector.setOffsetAndSize(temporalFeatureOffsets[i], aggregator.getTemporalFeatureNames().length);
            aggregator.initTemporal(bin, vector);
        }
    }

    static class NameUnifier {

        private final Map<String, Integer> addedNames = new HashMap<String, Integer>();

        String unifyName(String name) {
            if(!addedNames.containsKey(name)) {
                addedNames.put(name, 0);
                return name;
            } else {
                addedNames.put(name, addedNames.get(name) + 1);
            }
            return name + "_" + addedNames.get(name);
        }
    }
}
