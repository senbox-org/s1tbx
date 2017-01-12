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

package org.esa.snap.binning.operator;

import org.esa.snap.binning.BinningContext;
import org.esa.snap.binning.SpatialBin;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static org.junit.Assert.*;

public class FileBackedSpatialBinCollectorTest {

    private static final int NUM_FEATURES = 2;

    @Test
    public void testReadFromStream() throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(byteStream);

        dos.writeLong(23);
        SpatialBin bin1 = createSpatialBin(23);
        bin1.write(dos);
        dos.writeLong(23);
        SpatialBin bin2 = createSpatialBin(23);
        bin2.write(dos);
        dos.writeLong(1048);
        SpatialBin bin3 = createSpatialBin(1048);
        bin3.write(dos);

        dos.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(byteStream.toByteArray());
        TreeMap<Long, List<SpatialBin>> map = new TreeMap<>();
        FileBackedSpatialBinCollector.readFromStream(new DataInputStream(bais), map);
        assertEquals(2, map.size());
        assertTrue(map.containsKey(23L));
        assertTrue(map.containsKey(1048L));
        List<SpatialBin> spatialBins23 = map.get(23L);
        assertEquals(2, spatialBins23.size());
        assertEquals(bin2.getFeatureValues()[0], spatialBins23.get(1).getFeatureValues()[0], 1.0e-6);
    }

    @Test
    public void testWriteToStream() throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(byteStream);

        ArrayList<SpatialBin> binList = new ArrayList<>();
        binList.add(createSpatialBin(42));
        binList.add(createSpatialBin(42));
        binList.add(createSpatialBin(42));
        binList.add(createSpatialBin(42));

        FileBackedSpatialBinCollector.writeToStream(binList, dos);

        ByteArrayInputStream bais = new ByteArrayInputStream(byteStream.toByteArray());
        TreeMap<Long, List<SpatialBin>> newMap = new TreeMap<>();
        FileBackedSpatialBinCollector.readFromStream(new DataInputStream(bais), newMap);

        assertEquals(1, newMap.size());
        List<SpatialBin> newBinList = newMap.get(42L);
        assertEquals(4, newBinList.size());

    }

    @Test
    public void testCollecting() throws Exception {
        FileBackedSpatialBinCollector binCollector = new FileBackedSpatialBinCollector(26000);
        try {
            BinningContext ctx = Mockito.mock(BinningContext.class);
            for (int i = 0; i < 26; i++) {
                ArrayList<SpatialBin> spatialBins = new ArrayList<>();
                int binIndexOffset = i * 1000;
                for (int j = 0; j < 1000; j++) {
                    spatialBins.add(new SpatialBin(binIndexOffset + j, 3));
                }
                binCollector.consumeSpatialBins(ctx, spatialBins);
            }
            binCollector.consumingCompleted();

            SpatialBinCollection spatialBinCollection = binCollector.getSpatialBinCollection();

            assertFalse(spatialBinCollection.isEmpty());
            assertEquals(26000, spatialBinCollection.size());
            Iterable<List<SpatialBin>> collectedBins = spatialBinCollection.getBinCollection();
            int counter = 0;
            for (List<SpatialBin> collectedBin : collectedBins) {
                assertEquals(counter++, collectedBin.get(0).getIndex());
            }
        } finally {
            binCollector.close();
        }

    }

    private SpatialBin createSpatialBin(int binIndex) {
        SpatialBin bin = new SpatialBin(binIndex, NUM_FEATURES);
        for (int i = 0; i < NUM_FEATURES; i++) {
            bin.getFeatureValues()[i] = (float) Math.random();
        }
        return bin;
    }
}
