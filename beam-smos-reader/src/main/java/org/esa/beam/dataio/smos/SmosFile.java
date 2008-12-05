/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.smos;


import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;


public class SmosFile extends BasicSmosFile {

    public static final int POL_MODE_MASK = 0x00000003;
    public static final int POL_MODE_H = 0;
    public static final int POL_MODE_V = 1;
    public static final int POL_MODE_HV1 = 2;
    public static final int POL_MODE_HV2 = 3;

    public static final String BT_DATA_LIST_NAME = "BT_Data_List";
    public static final String FLAGS_FIELD_NAME = "Flags";
    public static final String INCIDENCE_ANGLE_FIELD_NAME = "Incidence_Angle";
    public static final String SNAPSHOT_LIST_NAME = "Snapshot_List";
    public static final String BT_DATA_TYPE_NAME = "BT_Data_Type";

    public static final int CENTER_INCIDENCE_ANGLE = 42500;

    public static final int INCIDENCE_ANGLE_RANGE = 10000;
    private final CompoundType btDataType;
    private final int btDataIndex;
    private final int incidenceAngleIndex;
    private final int flagsIndex;
    private final SequenceData snapshotList;
    private final CompoundType snapshotType;
    private int[] snapshotIndexes;
    private int snapshotIdMin;
    private int snapshotIdMax;

    public SmosFile(File file, DataFormat format) throws IOException {
        super(file, format);

        // todo - the following code is L1C sepecific. Create subclasses? (nf - 01.12.2008)
        btDataIndex = getGridPointType().getMemberIndex(BT_DATA_LIST_NAME);
        if (btDataIndex != -1) {
            btDataType = (CompoundType) format.getTypeDef(BT_DATA_TYPE_NAME);
            flagsIndex = this.btDataType.getMemberIndex(FLAGS_FIELD_NAME);
            incidenceAngleIndex = this.btDataType.getMemberIndex(INCIDENCE_ANGLE_FIELD_NAME);
        } else {
            btDataType = null;
            flagsIndex = -1;
            incidenceAngleIndex = -1;
        }

        // todo - the following code is D1C/F1C sepecific. Create subclasses? (nf - 04.12.2008)
        snapshotList = getDataBlock().getSequence(SNAPSHOT_LIST_NAME);
        if (snapshotList != null) {
            snapshotType = (CompoundType) snapshotList.getSequenceType().getElementType();
            initSnapshotIndexes();
        } else {
            snapshotType = null;
        }
    }

    public int getSnapshotIdMin() {
        return snapshotIdMin;
    }

    public int getSnapshotIdMax() {
        return snapshotIdMax;
    }

    public int getSnapshotIndex(int snapshotId) {
        return snapshotIndexes[snapshotId -  snapshotIdMin];
    }

    public SequenceData getSnapshotList() {
        return snapshotList;
    }

    public CompoundData getSnapshotData(int snapshotIndex) throws IOException {
        return snapshotList.getCompound(snapshotIndex);
    }

    public CompoundType getSnapshotType() {
        return snapshotType;
    }

    public CompoundType getBtDataType() {
        return btDataType;
    }

    public short getL1cBrowseBtDataShort(int gridPointIndex, int btDataIndex, int polMode) throws IOException {
        SequenceData btDataList = getBtDataList(gridPointIndex);
        CompoundData btData = btDataList.getCompound(polMode);
        return btData.getShort(btDataIndex);
    }

    public int getL1cBrowseBtDataInt(int gridPointIndex, int btDataIndex, int polMode) throws IOException {
        SequenceData btDataList = getBtDataList(gridPointIndex);
        CompoundData btData = btDataList.getCompound(polMode);
        return btData.getInt(btDataIndex);
    }

    public float getL1cBrowseBtDataFloat(int gridPointIndex, int btDataIndex, int polMode) throws IOException {
        SequenceData btDataList = getBtDataList(gridPointIndex);
        CompoundData btData = btDataList.getCompound(polMode);
        return btData.getFloat(btDataIndex);
    }

    /**
     * Gets the value of a 'BT_Data' field for a given grid point.
     *
     * @param gridPointIndex The grid point index.
     * @param btDataIndex    The index of the requested 'BT_Data' field.
     * @param polMode        {@link #POL_MODE_H},{@link #POL_MODE_V}, {@link #POL_MODE_HV1} or {@link #POL_MODE_HV2}
     * @param noDataValue    The no data value which is returned if no value could be found.
     * @return the value read or {@code noDataValue}
     * @throws IOException if an I/O error occurs
     */
    public float getL1cInterpolatedBtDataFloat(int gridPointIndex, int btDataIndex, int polMode, float noDataValue) throws IOException {
        final SequenceData btDataList = getBtDataList(gridPointIndex);
        final int btDataListCount = btDataList.getElementCount();
        int flags;
        int delta, deltaAbs;
        int deltaMin1 = Integer.MAX_VALUE;
        int deltaMin2 = Integer.MAX_VALUE;
        int incidenceAngle;
        float incidenceAngle1 = 0;
        float incidenceAngle2 = 0;
        float btValue;
        float btValue1 = 0;
        float btValue2 = 0;
        for (int i = 0; i < btDataListCount; i++) {
            CompoundData btData = btDataList.getCompound(i);
            flags = btData.getInt(flagsIndex);
            if ((flags & POL_MODE_MASK) == polMode) {
                incidenceAngle = btData.getInt(incidenceAngleIndex);
                delta = CENTER_INCIDENCE_ANGLE - incidenceAngle;
                deltaAbs = Math.abs(delta);
                if (deltaAbs < INCIDENCE_ANGLE_RANGE) {
                    btValue = btData.getFloat(btDataIndex);
                    if (delta < 0) {
                        if (deltaAbs < deltaMin1) {
                            deltaMin1 = deltaAbs;
                            incidenceAngle1 = incidenceAngle;
                            btValue1 = btValue;
                        }
                    } else if (delta > 0) {
                        if (deltaAbs < deltaMin2) {
                            deltaMin2 = deltaAbs;
                            incidenceAngle2 = incidenceAngle;
                            btValue2 = btValue;
                        }
                    } else {
                        return btValue;
                    }
                }
            }
        }
        final boolean hasValue1 = deltaMin1 < Integer.MAX_VALUE;
        final boolean hasValue2 = deltaMin2 < Integer.MAX_VALUE;
        if (hasValue1 && hasValue2) {
            return btValue1 + (CENTER_INCIDENCE_ANGLE - incidenceAngle1) * (btValue2 - btValue1) / (incidenceAngle2 - incidenceAngle1);
        } else {
            return noDataValue;
        }
    }

    public SequenceData getBtDataList(int gridPointIndex) throws IOException {
        CompoundData gridPointEntry = getGridPointList().getCompound(gridPointIndex);
        return gridPointEntry.getSequence(btDataIndex);
    }

    public void initSnapshotIndexes() throws IOException {
        snapshotIdMin = Integer.MAX_VALUE;
        snapshotIdMax = Integer.MIN_VALUE;
        final int snapshotCounter = snapshotList.getElementCount();
        for (int i = 0; i < snapshotCounter; i++) {
            CompoundData snapshotData = snapshotList.getCompound(i);
            int snapshotId = snapshotData.getInt(1);
            snapshotIdMin = Math.min(snapshotIdMin, snapshotId);
            snapshotIdMax = Math.max(snapshotIdMax, snapshotId);
        }

        System.out.println("SmosFile: snapshotIdMin = " + snapshotIdMin);
        System.out.println("SmosFile: snapshotIdMax = " + snapshotIdMax);

        snapshotIndexes = new int[snapshotIdMax - snapshotIdMin + 1];

        Arrays.fill(snapshotIndexes, -1);
        for (int i = 0; i < snapshotCounter; i++) {
            CompoundData snapshotData = snapshotList.getCompound(i);
            int snapshotId = snapshotData.getInt(1);
            snapshotIndexes[snapshotId - snapshotIdMin] = i;
        }
        System.out.println("SmosFile: snapshotCounter = " + snapshotCounter);
        System.out.println("SmosFile: snapshotIndexes.length = " + snapshotIndexes.length);
        int n = 0;
        for (int snapshotIndex : snapshotIndexes) {
            if (snapshotIndex != -1) {
                n++;
            }
        }
        System.out.println("SmosFile: number of snapshotIndexes != -1: " + n);
    }

    public static int smosSnapshotIdToSeqnum(int snapshotId) {
        return snapshotId % 10000;
    }
}
