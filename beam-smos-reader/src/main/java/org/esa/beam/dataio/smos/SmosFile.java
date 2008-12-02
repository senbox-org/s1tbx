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


import com.bc.ceres.binio.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;


public class SmosFile {

    public final int POL_MASK = 0x00000003;
    public static final int POL_MODE_HH = 0;
    public static final int POL_MODE_VV = 1;
    public static final int POL_MODE_HV_REAL = 2;
    public static final int POL_MODE_HV_IMAG = 3;

    public static final String GRID_POINT_LIST_NAME = "Grid_Point_List";
    public static final String BT_DATA_LIST_NAME = "BT_Data_List";
    public static final String FLAGS_FIELD_NAME = "Flags";
    public static final String INCIDENCE_ANGLE_FIELD_NAME = "Incidence_Angle";
    public static final String BT_DATA_TYPE_NAME = "BT_Data_Type";

    public static final int CENTER_INCIDENCE_ANGLE = 42500;
    public static final int INCIDENCE_ANGLE_RANGE = 10000;

    private final File file;
    private final DataFormat format;
    private final SequenceData gridPointList;
    private int[] gridPointIndexes;
    private final DataContext dataContext;
    private final CompoundType gridPointType;
    private final CompoundType btDataType;
    private final int btDataIndex;
    private final int incidenceAngleIndex;
    private final int flagsIndex;

    public SmosFile(File file, DataFormat format) throws IOException {
        this.file = file;
        this.format = format;
        this.dataContext = format.createContext(file, "r");
        CompoundData smosDataset = dataContext.getData();
        this.gridPointList = smosDataset.getSequence(GRID_POINT_LIST_NAME);
        if (this.gridPointList == null) {
            throw new IllegalStateException("Missing dataset '"+GRID_POINT_LIST_NAME+"' in SMOS file.");
        }
        this.gridPointType = (CompoundType) gridPointList.getSequenceType().getElementType();

        initGridPointIndexes();

        // todo - the following code is L1C sepecific. Create subclasses? (nf - 01.12.2008)
        btDataIndex = this.gridPointType.getMemberIndex(BT_DATA_LIST_NAME);
        if (btDataIndex != -1) {
            btDataType = (CompoundType) format.getTypeDef(BT_DATA_TYPE_NAME);
            flagsIndex = this.btDataType.getMemberIndex(FLAGS_FIELD_NAME);
            incidenceAngleIndex = this.btDataType.getMemberIndex(INCIDENCE_ANGLE_FIELD_NAME);
        } else {
            btDataType = null;
            flagsIndex = -1;
            incidenceAngleIndex = -1;
        }
    }

    public File getFile() {
        return file;
    }

    public DataFormat getFormat() {
        return format;
    }

    public DataContext getDataContext() {
        return dataContext;
    }

    public SequenceData getGridPointList() {
        return gridPointList;
    }

    public CompoundType getGridPointType() {
        return gridPointType;
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
     * @param polMode        {@link #POL_MODE_HH},{@link #POL_MODE_VV}, {@link #POL_MODE_HV_REAL} or {@link #POL_MODE_HV_IMAG}
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
            if ((flags & POL_MASK) == polMode) {
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
        CompoundData gridPointEntry = gridPointList.getCompound(gridPointIndex);
        return gridPointEntry.getSequence(btDataIndex);
    }

    public int getGridPointIndex(int seqnum) {
        return gridPointIndexes[seqnum];
    }

    public void initGridPointIndexes() throws IOException {
        // todo - OPT: first find seqnumMin, seqnumMax, then gridPointIndexes=new int[seqnumMax-seqnumMin+1];
        gridPointIndexes = new int[2621442 + 1];
        Arrays.fill(gridPointIndexes, -1);
        final int gridPointCounter = gridPointList.getElementCount();
        for (int i = 0; i < gridPointCounter; i++) {
            CompoundData gridPointData = gridPointList.getCompound(i);
            final int gridPointId = gridPointData.getInt(0);
            final int seqnum = SmosDgg.smosGridPointIdToDggridSeqnum(gridPointId);
            gridPointIndexes[seqnum] = i;
        }
        System.out.println("SmosFile: gridPointCounter = " + gridPointCounter);
        System.out.println("SmosFile: gridPointIndexes.length = " + gridPointIndexes.length);
        int n = 0;
        for (int gridPointIndex : gridPointIndexes) {
            if (gridPointIndex != -1) {
                n++;
            }
        }
        System.out.println("SmosFile: number of gridPointIndexes != -1: " + n);
    }

    public void close() {
        dataContext.dispose();
    }
}
