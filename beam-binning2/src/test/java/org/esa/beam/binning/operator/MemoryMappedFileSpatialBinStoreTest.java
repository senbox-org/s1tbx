/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator;

import org.esa.beam.binning.SpatialBin;
import org.junit.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class MemoryMappedFileSpatialBinStoreTest {

    public static final int NUM_OBS = 20;
    public static final int FEATURE_COUNT = 5;

    @Test
    public void testConsumeAndGet() throws Exception {
        final MemoryMappedFileSpatialBinStore binStore = new MemoryMappedFileSpatialBinStore();
        final ArrayList<SpatialBin> spatialBins = new ArrayList<SpatialBin>();
        spatialBins.add(SpatialBin.read(0L, new TestDataInput(0)));
        spatialBins.add(SpatialBin.read(0L, new TestDataInput(1)));
        spatialBins.add(SpatialBin.read(1L, new TestDataInput(100)));
        binStore.consumeSpatialBins(null, spatialBins);
        binStore.consumingCompleted();

        final SortedMap<Long,List<SpatialBin>> spatialBinMap = binStore.getSpatialBinMap();

        validateSpatialBinMap(spatialBinMap);
    }

    @Test
    public void testCanGetSpatialMapTwice() throws Exception {
        final MemoryMappedFileSpatialBinStore binStore = new MemoryMappedFileSpatialBinStore();
        final ArrayList<SpatialBin> spatialBins = new ArrayList<SpatialBin>();
        spatialBins.add(SpatialBin.read(0L, new TestDataInput(0)));
        spatialBins.add(SpatialBin.read(0L, new TestDataInput(1)));
        spatialBins.add(SpatialBin.read(1L, new TestDataInput(100)));
        binStore.consumeSpatialBins(null, spatialBins);
        binStore.consumingCompleted();

        validateSpatialBinMap(binStore.getSpatialBinMap());
        validateSpatialBinMap(binStore.getSpatialBinMap());
    }

    private void validateSpatialBinMap(SortedMap<Long, List<SpatialBin>> spatialBinMap) {
        assertEquals(2, spatialBinMap.size());
        final List<SpatialBin> spatialBinsWithIndex0 = spatialBinMap.get(0L);
        assertEquals(2, spatialBinsWithIndex0.size());
        final SpatialBin firstSpatialBinWithIndex0 = spatialBinsWithIndex0.get(0);
        assertNotNull(firstSpatialBinWithIndex0);
        assertEquals(0L, firstSpatialBinWithIndex0.getIndex());
        assertEquals(NUM_OBS, firstSpatialBinWithIndex0.getNumObs());
        assertEquals(FEATURE_COUNT, firstSpatialBinWithIndex0.getFeatureValues().length);
        assertArrayEquals(new float[]{0.0f, 1.0f, 2.0f, 3.0f, 4.0f}, firstSpatialBinWithIndex0.getFeatureValues(), 1.0E-4f);

        final SpatialBin secondSpatialBinWithIndex0 = spatialBinsWithIndex0.get(1);
        assertNotNull(secondSpatialBinWithIndex0);
        assertEquals(0L, secondSpatialBinWithIndex0.getIndex());
        assertEquals(NUM_OBS, secondSpatialBinWithIndex0.getNumObs());
        assertEquals(FEATURE_COUNT, secondSpatialBinWithIndex0.getFeatureValues().length);
        assertArrayEquals(new float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f,}, secondSpatialBinWithIndex0.getFeatureValues(), 1.0E-4f);

        final List<SpatialBin> spatialBinsWithIndex1 = spatialBinMap.get(1L);
        assertEquals(1, spatialBinsWithIndex1.size());
        final SpatialBin firstSpatialBinWithIndex1 = spatialBinsWithIndex1.get(0);
        assertEquals(1L, firstSpatialBinWithIndex1.getIndex());
        assertEquals(NUM_OBS, firstSpatialBinWithIndex1.getNumObs());
        assertEquals(FEATURE_COUNT, firstSpatialBinWithIndex1.getFeatureValues().length);
        assertArrayEquals(new float[]{100.0f, 101.0f, 102.0f, 103.0f, 104.0f,}, firstSpatialBinWithIndex1.getFeatureValues(), 1.0E-4f);
    }

    private static class TestDataInput extends ObjectInputStream {

        private float floatValue = 0;
        private boolean firstTime = true;

        private TestDataInput(float startFloatValue) throws IOException {
            floatValue = startFloatValue;
        }

        @Override
        public int readInt() throws IOException {
            final int i = firstTime ? NUM_OBS : FEATURE_COUNT;
            firstTime = false;
            return i;
        }

        @Override
        public float readFloat() throws IOException {
            return floatValue++;
        }
    }
}
