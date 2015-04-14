package org.esa.snap.binning;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Produces spatial bins by processing a given "slice" of observations.
 * A slice is referred to as a spatially contiguous region.
 * The class uses a {@link SpatialBinConsumer} to inform clients about a new slice of spatial bins ready to be consumed.
 *
 * @author Norman Fomferra
 * @see ObservationSlice
 * @see TemporalBinner
 */
public class SpatialBinner {

    private final BinningContext binningContext;
    private final PlanetaryGrid planetaryGrid;
    private final BinManager binManager;
    private final SpatialBinConsumer consumer;

    // State variables
    private final Map<Long, SpatialBin> activeBinMap;
    private final Map<Long, SpatialBin> finalizedBinMap;
    private final ArrayList<Exception> exceptions;

    /**
     * Constructs a spatial binner.
     *
     * @param binningContext The binning context.
     * @param consumer       The consumer that receives the spatial bins processed from observations.
     */
    public SpatialBinner(BinningContext binningContext, SpatialBinConsumer consumer) {
        this.binningContext = binningContext;
        this.planetaryGrid = binningContext.getPlanetaryGrid();
        this.binManager = binningContext.getBinManager();
        this.consumer = consumer;
        this.activeBinMap = new TreeMap<Long, SpatialBin>();
        this.finalizedBinMap = new TreeMap<Long, SpatialBin>();
        this.exceptions = new ArrayList<Exception>();
    }

    /**
     * @return The binning context that will also be passed to {@link  SpatialBinConsumer#consumeSpatialBins(BinningContext, java.util.List)}.
     */
    public BinningContext getBinningContext() {
        return binningContext;
    }

    /**
     * @return The exceptions occurred during processing.
     */
    public Exception[] getExceptions() {
        return exceptions.toArray(new Exception[exceptions.size()]);
    }

    /**
     * Processes a slice of observations.
     * Will cause the {@link SpatialBinConsumer} to be invoked.
     *
     * @param observations The observations.
     *
     * @return The number of processed observations
     */
    public long processObservationSlice(Iterable<Observation> observations) {

        finalizedBinMap.putAll(activeBinMap);

        long observationCounter = 0;
        for (Observation observation : observations) {
            observationCounter++;
            Long binIndex = planetaryGrid.getBinIndex(observation.getLatitude(), observation.getLongitude());
            SpatialBin bin = activeBinMap.get(binIndex);
            if (bin == null) {
                bin = binManager.createSpatialBin(binIndex);
                activeBinMap.put(binIndex, bin);
            }
            binManager.aggregateSpatialBin(observation, bin);
            finalizedBinMap.remove(binIndex);
        }

        if (!finalizedBinMap.isEmpty()) {
            for (Long key : finalizedBinMap.keySet()) {
                activeBinMap.remove(key);
            }
            emitSliceBins(finalizedBinMap);
            finalizedBinMap.clear();
        }

        return observationCounter;
    }

    /**
     * Processes a slice of observations.
     * Convenience method for {@link #processObservationSlice(Iterable)}.
     *
     * @param observations The observations.
     *
     * @return The number of processed observations
     */
    public long processObservationSlice(Observation... observations) {
        return processObservationSlice(Arrays.asList(observations));
    }

    /**
     * Must be called after all observations have been send to {@link #processObservationSlice(Iterable)}.
     * Calling this method multiple times has no further effect.
     */
    public void complete() {
        if (!activeBinMap.isEmpty()) {
            emitSliceBins(activeBinMap);
            activeBinMap.clear();
        }
        finalizedBinMap.clear();
    }

    private void emitSliceBins(Map<Long, SpatialBin> binMap) {
        List<SpatialBin> bins = new ArrayList<SpatialBin>(binMap.values());
        for (SpatialBin bin : bins) {
            binManager.completeSpatialBin(bin);
        }
        try {
            consumer.consumeSpatialBins(getBinningContext(), bins);
        } catch (Exception e) {
            exceptions.add(e);
        }
    }
}
