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
import com.bc.ceres.binio.IOHandler;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.binio.util.RandomAccessFileIOHandler;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * todo - API doc
 */
public class SmosFileReader {
    public static void main(String[] args) throws IOException {

        System.out.println("Sequential access...");

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage before = memoryMXBean.getHeapMemoryUsage();
        System.out.println(before);

        File file = new File(args[0]);
        if (file.isDirectory()) {
            file = new File(file, file.getName() + ".DBL");
        }
        IOHandler handler = createIOHandler(file);


        final DataContext context = SmosProduct.MIR_SCLF1C_FORMAT.createContext(handler);

        final CompoundData data = context.getData();
        final long snapshotCounter = data.getUInt("Snapshot_Counter");
        System.out.println("snapshotCounter = " + snapshotCounter);
        final long gridPointCounter = data.getUInt("Grid_Point_Counter");
        System.out.println("gridPointCounter = " + gridPointCounter);

        final SequenceData snapshotList = data.getSequence("Snapshot_List");
        for (int i = 0; i < snapshotList.getElementCount(); i++) {
            final CompoundData snapshotInfo = snapshotList.getCompound(i);
            final long snapshotId = snapshotInfo.getUInt(1);
            final SequenceData position = snapshotInfo.getSequence(3);
            final SequenceData velocity = snapshotInfo.getSequence(4);
//            System.out.println("snapshotId."+i+" = " + snapshotId);
//            System.out.println("position."+i+" = " + position.getDouble(0)+","+position.getDouble(1)+","+position.getDouble(2));
//            System.out.println("velocity."+i+" = " + velocity.getDouble(0)+","+velocity.getDouble(1)+","+velocity.getDouble(2));
        }

        long t0 = System.currentTimeMillis();
        sequentialAccess(data);
        long t1 = System.currentTimeMillis();
        long dt = t1 - t0;
        System.out.println("dt = " + dt + " ms");

        MemoryUsage after = memoryMXBean.getHeapMemoryUsage();
        System.out.println(after);
        System.out.println();
        System.out.println("usage: " + (after.getUsed() - before.getUsed()));

        System.out.println("Random access...");

        before = memoryMXBean.getHeapMemoryUsage();
        System.out.println(before);

        t0 = System.currentTimeMillis();
        randomAccess(data);
        t1 = System.currentTimeMillis();
        dt = t1 - t0;
        System.out.println("dt = " + dt + " ms");

        after = memoryMXBean.getHeapMemoryUsage();
        System.out.println(after);
        System.out.println();
        System.out.println("usage: " + (after.getUsed() - before.getUsed()));
    }

    private static SequenceData sequentialAccess(CompoundData data) throws IOException {
        final SequenceData gridPointList = data.getSequence("Grid_Point_List");
        for (int i = 0; i < gridPointList.getElementCount(); i++) {
            final CompoundData gridPointData = gridPointList.getCompound(i);
            final long gridPointId = gridPointData.getUInt(0);
//            final float longitude = gridPointData.getFloat(1);
//            final float latitude = gridPointData.getFloat(2);
//            final float altitude = gridPointData.getFloat(3);
//            System.out.println("gridPointId." + i + " = " + gridPointId);
//            System.out.println("  longitude = " + longitude);
//            System.out.println("  latitude  = " + latitude);
//            System.out.println("  altitude  = " + altitude);
        }
        return gridPointList;
    }

    private static void randomAccess(CompoundData data) throws IOException {
        final SequenceData gridPointList = data.getSequence("Grid_Point_List");
        for (int i = 0; i < gridPointList.getElementCount(); i++) {
            int index = (int) (Math.random() * gridPointList.getElementCount());
            final CompoundData gridPointData = gridPointList.getCompound(index);
            final long gridPointId = gridPointData.getUInt(0);
//            final float longitude = gridPointData.getFloat(1);
//            final float latitude = gridPointData.getFloat(2);
//            final float altitude = gridPointData.getFloat(3);
//            System.out.println("gridPointId." + i + " = " + gridPointId);
//            System.out.println("  longitude = " + longitude);
//            System.out.println("  latitude  = " + latitude);
//            System.out.println("  altitude  = " + altitude);
        }
    }

    private static IOHandler createIOHandler(File file) throws IOException {
//        return new FileChannelIOHandler(new RandomAccessFile(file, "r").getChannel());

        return new RandomAccessFileIOHandler(new RandomAccessFile(file, "r"));

//        final FileImageInputStream iis = new FileImageInputStream(file);
//        return new ImageIOHandler(iis);

//        final FileInputStream iis = new FileInputStream(file);
//        return new MappedFileChannelIOHandler(iis.getChannel());
    }
}
