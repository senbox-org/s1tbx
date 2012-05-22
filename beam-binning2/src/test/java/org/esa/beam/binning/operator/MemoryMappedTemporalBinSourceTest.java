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

import org.esa.beam.binning.TemporalBin;
import org.junit.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class MemoryMappedTemporalBinSourceTest {

    private static final int NUM_OBS = 20;
    private static final int FEATURE_COUNT = 5;
    private static final int NUM_PASSES = 7;

    @Test
    public void testTemporalBinSourceIteration() throws Exception {
        final ArrayList<TemporalBin> temporalBins = new ArrayList<TemporalBin>();
        temporalBins.add(TemporalBin.read(0L, new TestDataInput(0.0f)));
        temporalBins.add(TemporalBin.read(1L, new TestDataInput(2.0f)));
        temporalBins.add(TemporalBin.read(2L, new TestDataInput(3.0f)));

        final MemoryMappedTemporalBinSource temporalBinSource = new MemoryMappedTemporalBinSource(temporalBins);
        assertEquals(1, temporalBinSource.open());
        final Iterator<? extends TemporalBin> iterator = temporalBinSource.getPart(-1);

        assertTrue(iterator.hasNext());
        TemporalBin temporalBin = iterator.next();
        assertEquals(0L, temporalBin.getIndex());
        assertArrayEquals(new float[]{0.0f, 1.0f, 2.0f, 3.0f, 4.0f}, temporalBin.getFeatureValues(), 1.0E-4f);

        assertTrue(iterator.hasNext());
        temporalBin = iterator.next();
        assertEquals(1L, temporalBin.getIndex());
        assertArrayEquals(new float[]{2.0f, 3.0f, 4.0f, 5.0f, 6.0f}, temporalBin.getFeatureValues(), 1.0E-4f);

        assertTrue(iterator.hasNext());
        temporalBin = iterator.next();
        assertEquals(2L, temporalBin.getIndex());
        assertArrayEquals(new float[]{3.0f, 4.0f, 5.0f, 6.0f, 7.0f}, temporalBin.getFeatureValues(), 1.0E-4f);

        assertFalse(iterator.hasNext());

        temporalBinSource.close();
    }


    private static class TestDataInput extends ObjectInputStream {

        private float floatValue = 0;
        private int visitCount = 0;

        private TestDataInput(float startFloatValue) throws IOException {
            floatValue = startFloatValue;
        }

        @Override
        public int readInt() throws IOException {
            if(visitCount == 0) {
                visitCount++;
                return NUM_OBS;
            } else if (visitCount == 1) {
                visitCount++;
                return NUM_PASSES;
            } else if(visitCount == 2) {
                visitCount++;
                return FEATURE_COUNT;
            }
            throw new IllegalStateException("should not come here");
        }

        @Override
        public float readFloat() throws IOException {
            return floatValue++;
        }
    }

}
