/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.binning.support.BinTracer;
import org.esa.beam.binning.support.VariableContextImpl;
import org.esa.beam.binning.support.VectorImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The bin manager class comprises a number of {@link Aggregator}s
 *
 * @author Norman Fomferra
 */
public class BinManager {

    private final VariableContext variableContext;
    private final CellProcessor cellProcessor;
    private final Aggregator[] aggregators;
    private final int spatialFeatureCount;
    private final int temporalFeatureCount;
    private final int outputFeatureCount;
    private final int postFeatureCount;
    private final int[] spatialFeatureOffsets;
    private final int[] temporalFeatureOffsets;
    private final int[] outputFeatureOffsets;
    private final String[] spatialFeatureNames;
    private final String[] temporalFeatureNames;
    private final String[] outputFeatureNames;
    private final String[] postFeatureNames;
    private BinTracer binTracer;

    public BinManager() {
        this(new VariableContextImpl());
    }

    public BinManager(VariableContext variableContext, Aggregator... aggregators) {
        this(variableContext, null, aggregators);
    }

    public BinManager(VariableContext variableContext, CellProcessorConfig cellProcessorConfig, Aggregator... aggregators) {
        this.variableContext = variableContext;
        this.aggregators = aggregators;
        this.spatialFeatureOffsets = new int[aggregators.length];
        this.temporalFeatureOffsets = new int[aggregators.length];
        this.outputFeatureOffsets = new int[aggregators.length];
        int spatialFeatureCount = 0;
        int temporalFeatureCount = 0;
        int outputFeatureCount = 0;
        List<String> spatialFeatureNameList = new ArrayList<>();
        List<String> temporalFeatureNameList = new ArrayList<>();
        for (int i = 0; i < aggregators.length; i++) {
            Aggregator aggregator = aggregators[i];
            spatialFeatureOffsets[i] = spatialFeatureCount;
            temporalFeatureOffsets[i] = temporalFeatureCount;
            outputFeatureOffsets[i] = outputFeatureCount;
            spatialFeatureCount += aggregator.getSpatialFeatureNames().length;
            temporalFeatureCount += aggregator.getTemporalFeatureNames().length;
            outputFeatureCount += aggregator.getOutputFeatureNames().length;
            Collections.addAll(spatialFeatureNameList, aggregator.getSpatialFeatureNames());
            Collections.addAll(temporalFeatureNameList, aggregator.getTemporalFeatureNames());
        }
        this.spatialFeatureCount = spatialFeatureCount;
        this.temporalFeatureCount = temporalFeatureCount;
        this.outputFeatureCount = outputFeatureCount;
        this.spatialFeatureNames = spatialFeatureNameList.toArray(new String[spatialFeatureNameList.size()]);
        this.temporalFeatureNames = temporalFeatureNameList.toArray(new String[temporalFeatureNameList.size()]);
        this.outputFeatureNames = new String[outputFeatureCount];
        final NameUnifier nameUnifier = new NameUnifier();
        int k = 0;
        for (Aggregator aggregator : aggregators) {
            for (int i = 0; i < aggregator.getOutputFeatureNames().length; i++) {
                outputFeatureNames[k] = nameUnifier.unifyName(aggregator.getOutputFeatureNames()[i]);
                k++;
            }
        }
        if (cellProcessorConfig != null) {
            this.cellProcessor = createPostProcessor(cellProcessorConfig, outputFeatureNames);
            this.postFeatureNames = cellProcessor.getOutputFeatureNames();
            this.postFeatureCount = this.postFeatureNames.length;
        } else {
            this.cellProcessor = null;
            this.postFeatureCount = 0;
            this.postFeatureNames = new String[0];
        }
    }

    private static CellProcessor createPostProcessor(CellProcessorConfig config, String[] outputFeatureNames) {
        VariableContextImpl variableContextAgg = new VariableContextImpl();
        for (String outputFeatureName : outputFeatureNames) {
            variableContextAgg.defineVariable(outputFeatureName);
        }
        TypedDescriptorsRegistry registry = TypedDescriptorsRegistry.getInstance();
        CellProcessorDescriptor descriptor = registry.getDescriptor(CellProcessorDescriptor.class, config.getName());
        if (descriptor != null) {
            return descriptor.createCellProcessor(variableContextAgg, config);
        } else {
            throw new IllegalArgumentException("Unknown cell processor type: " + config.getName());
        }
    }

    public final VariableContext getVariableContext() {
        return variableContext;
    }

    public final int getSpatialFeatureCount() {
        return spatialFeatureCount;
    }

    public final int getTemporalFeatureCount() {
        return temporalFeatureCount;
    }

    public final int getOutputFeatureCount() {
        return outputFeatureCount;
    }

    public final String[] getSpatialFeatureNames() {
        return spatialFeatureNames;
    }

    public final String[] getTemporalFeatureNames() {
        return temporalFeatureNames;
    }

    public final int getPostProcessFeatureCount() {
        return postFeatureCount;
    }

    public final String[] getOutputFeatureNames() {
        return outputFeatureNames;
    }

    public final String[] getPostProcessFeatureNames() {
        return postFeatureNames;
    }

    public final int getResultFeatureCount() {
        if (hasPostProcessor()) {
            return postFeatureCount;
        } else {
            return outputFeatureCount;
        }
    }

    public final String[] getResultFeatureNames() {
        if (hasPostProcessor()) {
            return getPostProcessFeatureNames();
        } else {
            return getOutputFeatureNames();
        }
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

    // method is used in Calvalus - undocumented API :-) don't remove
    public WritableVector createOutputVector() {
        return new VectorImpl(new float[outputFeatureCount]);
    }

    public SpatialBin createSpatialBin(long binIndex) {
        final SpatialBin spatialBin = new SpatialBin(binIndex, spatialFeatureCount);
        initSpatialBin(spatialBin);
        traceSpatial("createSpatial", null, spatialBin);
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
        traceSpatial("aggregateSpatial", observation, spatialBin);
    }

    public void completeSpatialBin(SpatialBin spatialBin) {
        final VectorImpl spatialVector = new VectorImpl(spatialBin.featureValues);
        for (int i = 0; i < aggregators.length; i++) {
            final Aggregator aggregator = aggregators[i];
            spatialVector.setOffsetAndSize(spatialFeatureOffsets[i], aggregator.getSpatialFeatureNames().length);
            aggregator.completeSpatial(spatialBin, spatialBin.numObs, spatialVector);
        }
        traceSpatial("completeSpatial", null, spatialBin);
    }

    public TemporalBin createTemporalBin(long binIndex) {
        final TemporalBin temporalBin = new TemporalBin(binIndex, temporalFeatureCount);
        initTemporalBin(temporalBin);
        traceTemporal("createTemporal", null, temporalBin);
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
        traceTemporal("aggregateTemporal", inputBin, outputBin);
    }

    // method is used in Calvalus - undocumented API :-) don't remove
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
        traceTemporal("completeTemporal", null, temporalBin);
    }

    public TemporalBin createOutputBin(long binIndex) {
        return new TemporalBin(binIndex, outputFeatureCount);
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
        traceOutput(temporalBin, outputVector);
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

    public boolean hasPostProcessor() {
        return cellProcessor != null;
    }

    public TemporalBin createProcessBin(long binIndex) {
        return new TemporalBin(binIndex, postFeatureCount);
    }

    public void postProcess(Vector outputVector, WritableVector postVector) {
        cellProcessor.compute(outputVector, postVector);
    }

    public void setBinTracer(BinTracer binTracer) {
        this.binTracer = binTracer;
    }
    public BinTracer getBinTracer() {
        return binTracer;
    }

    static class NameUnifier {

        private final Map<String, Integer> addedNames = new HashMap<String, Integer>();

        String unifyName(String name) {
            if (!addedNames.containsKey(name)) {
                addedNames.put(name, 0);
                return name;
            } else {
                addedNames.put(name, addedNames.get(name) + 1);
            }
            return name + "_" + addedNames.get(name);
        }
    }

    private void traceSpatial(String action, Observation observation, SpatialBin spatialBin) {
        if (BinTracer.traceThis(binTracer, spatialBin.getIndex())) {
            binTracer.traceSpatial(action, observation, spatialBin);
        }
    }

    private void traceTemporal(String action, SpatialBin spatialBin, TemporalBin temporalBin) {
        if (BinTracer.traceThis(binTracer, temporalBin.getIndex())) {
            binTracer.traceTemporal(action, spatialBin, temporalBin);
        }
    }

    private void traceOutput(TemporalBin temporalBin, WritableVector outputVector) {
        if (BinTracer.traceThis(binTracer, temporalBin.getIndex())) {
            binTracer.traceOutput(temporalBin, outputVector);
        }
    }
}
