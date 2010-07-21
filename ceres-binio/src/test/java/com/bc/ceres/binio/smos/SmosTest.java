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

package com.bc.ceres.binio.smos;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.binio.TracingIOHandler;
import static com.bc.ceres.binio.smos.SmosProduct.*;
import com.bc.ceres.binio.util.ByteArrayIOHandler;
import junit.framework.TestCase;

import java.io.IOException;
import java.nio.ByteOrder;

public class SmosTest extends TestCase {

    public void testSequentialProductReading() throws IOException {
        byte[] byteData = createTestProductData(ByteOrder.LITTLE_ENDIAN);
        final TracingIOHandler reader = new TracingIOHandler(new ByteArrayIOHandler(byteData));
        final DataContext context = MIR_SCLF1C_FORMAT.createContext(reader);
        final CompoundData data = context.getData();

        assertEquals(0L, data.getPosition());
        assertEquals(-1L, data.getSize());
        assertEquals("", reader.getTrace());

        int snapshotCounter = data.getInt(0);
        assertEquals(3, snapshotCounter);

        assertEquals("R(0,4)", reader.getTrace());

        SequenceData snapshotList = data.getSequence(1);
        assertNotNull(snapshotList);
        assertNotNull(snapshotList.getType());
        assertEquals(3, snapshotList.getType().getElementCount());
        testSnapshotInfo(snapshotList, 0);
        testSnapshotInfo(snapshotList, 1);
        testSnapshotInfo(snapshotList, 2);
        assertEquals("R(0,4)"   // 1 x Snapshot_Counter
                + "R(4,483)",   // 3 x Snapshot_Info
                                reader.getTrace());

        int gridPointCounter = data.getInt(2);
        assertEquals(4, gridPointCounter);
        assertEquals("R(0,4)"  // 1 x Snapshot_Counter
                + "R(4,483)"   // 3 x Snapshot_Info
                + "R(487,4)",  // 1 x Grid_Point_Counter
                               reader.getTrace());

        SequenceData gridPointList = data.getSequence(3);
        assertNotNull(gridPointList);
        assertNotNull(gridPointList.getType());
        assertEquals(4, gridPointList.getType().getElementCount());
        testGridPointData(gridPointList, 0);
        testGridPointData(gridPointList, 1);
        testGridPointData(gridPointList, 2);
        testGridPointData(gridPointList, 3);
        assertEquals("R(0,4)"    // 1 x Snapshot_Counter
                + "R(4,483)"     // 3 x Snapshot_Info
                + "R(487,4)"     // 1 x Grid_Point_Counter
                + "R(491,18)"    // 1 x Grid_Point_Data up to SEQ(BT_Data)
                + "R(509,56)"    // 2 x BT_Data
                + "R(565,18)"    // 1 x Grid_Point_Data up to SEQ(BT_Data)
                + "R(583,112)"   // 4 x BT_Data
                + "R(695,18)"    // 1 x Grid_Point_Data up to SEQ(BT_Data)
                + "R(713,84)"    // 3 x BT_Data
                + "R(797,18)"    // 1 x Grid_Point_Data up to SEQ(BT_Data)
                + "R(815,168)",  // 6 x BT_Data
                                 reader.getTrace());

        reader.reset();
        testGridPointData(gridPointList, 3);
        assertEquals("", reader.getTrace()); // tests that no re-reading is required!

        testGridPointData(gridPointList, 3);
        testGridPointData(gridPointList, 3);
        testGridPointData(gridPointList, 3);
        assertEquals("", reader.getTrace()); // tests that no re-reading is required!

        reader.reset();
        testGridPointData(gridPointList, 1);
        testGridPointData(gridPointList, 0);
        testGridPointData(gridPointList, 3);
        testGridPointData(gridPointList, 2);
        assertEquals("" +
                "R(565,18)" +
                "R(583,112)" +
                "R(491,18)" +
                "R(509,56)" +
                "R(797,18)" +
                "R(815,168)",
                     reader.getTrace());
    }

    public void testRandomAccessProductReading() throws IOException {
        byte[] byteData = createTestProductData(ByteOrder.LITTLE_ENDIAN);
        final TracingIOHandler handler = new TracingIOHandler(new ByteArrayIOHandler(byteData));
        final DataContext context = MIR_SCLF1C_FORMAT.createContext(handler);
        final CompoundData data = context.getData();

        SequenceData gridPointList = data.getSequence(3);
        assertNotNull(gridPointList);
        assertNotNull(gridPointList.getType());
        assertEquals(4, gridPointList.getType().getElementCount());
        testGridPointData(gridPointList, 2);
        assertEquals("R(0,4)" +
                "R(487,4)" +
                "R(491,18)" +
                "R(565,18)" +
                "R(695,18)" +
                "R(713,84)", handler.getTrace());
    }

    private static void testSnapshotInfo(SequenceData snapshotList, int i) throws IOException {
        final int[] expectedTime = getSnapshotTime(i);
        final int expectedId = getSnapshotId(i);
        final CompoundData snapshotData = snapshotList.getCompound(i);
        final SequenceData timeData = snapshotData.getSequence(0);
        assertEquals(expectedTime[0], timeData.getInt(0));
        assertEquals(expectedTime[1], timeData.getInt(1));
        assertEquals(expectedTime[2], timeData.getInt(2));
        assertEquals(expectedId, snapshotData.getInt(1));
        final int expectedSize = SNAPSHOT_INFO_TYPE.getSize();
        final long expectedOffset = snapshotList.getPosition() + i * snapshotData.getSize();
        assertEquals(expectedSize, snapshotData.getSize());
        assertEquals(expectedOffset, snapshotData.getPosition());
    }


    private static void testGridPointData(SequenceData gridPointList, int i) throws IOException {
        CompoundData gridPointData = gridPointList.getCompound(i);

        assertEquals(getGridPointId(i), gridPointData.getInt(0));
        assertEquals(getGridPointLatitude(i), gridPointData.getFloat(1), 1.0e-6F);
        assertEquals(getGridPointLongitude(i), gridPointData.getFloat(2), 1.0e-6F);


        final SequenceData btDataList = gridPointData.getSequence(6);
        assertEquals(BT_DATA_COUNTERS[i], btDataList.getElementCount());

        for (int j = 0; j < BT_DATA_COUNTERS[i]; j++) {
            final CompoundData btData = btDataList.getCompound(j);
            assertEquals(getBtDataFlags(j), btData.getShort("Flags"));
            assertEquals(getBtValueReal(j), btData.getFloat("BT_Value_Real"));
            assertEquals(getBtValueImag(j), btData.getFloat("BT_Value_Imag"));
        }


        final long size = gridPointData.getSize();
        if (size >= 0L) { // Only assert if size is known (which may not necessarily be the case)
            final int expectedSize = 18 + BT_DATA_COUNTERS[i] * F1C_BT_DATA_TYPE.getSize();
            assertEquals(expectedSize, size);
        }
    }

}
