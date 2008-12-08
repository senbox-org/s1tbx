/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Represents a SMOS L1c Science product file.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class L1cScienceSmosFile extends L1cSmosFile {

    public static final String BT_DATA_FLAGS_FIELD_NAME = "Flags";
    public static final String BT_DATA_INCIDENCE_ANGLE_FIELD_NAME = "Incidence_Angle";
    public static final String SNAPSHOT_LIST_NAME = "Snapshot_List";
    public static final String SNAPSHOT_ID_NAME = "Snapshot_ID";

    public static final int CENTER_INCIDENCE_ANGLE = 42500;
    public static final int MIN_INCIDENCE_ANGLE = 37500;
    public static final int MAX_INCIDENCE_ANGLE = 52500;

    private final int flagsIndex;

    private final int incidenceAngleIndex;
    private final SequenceData snapshotList;

    private final CompoundType snapshotType;
    private final int[] snapshotIndexes;

    private int snapshotIdMin;
    private int snapshotIdMax;

    public L1cScienceSmosFile(File file, DataFormat format) throws IOException {
        super(file, format);

        flagsIndex = getBtDataType().getMemberIndex(BT_DATA_FLAGS_FIELD_NAME);
        incidenceAngleIndex = this.btDataType.getMemberIndex(BT_DATA_INCIDENCE_ANGLE_FIELD_NAME);

        snapshotList = getDataBlock().getSequence(SNAPSHOT_LIST_NAME);
        if (snapshotList == null) {
            throw new IOException("Data block does not include snapshot list.");
        }

        snapshotType = (CompoundType) snapshotList.getSequenceType().getElementType();
        snapshotIndexes = createSnapshotIndexes();
    }

    @Override
    public short getBrowseBtData(int gridPointIndex, int fieldIndex, int polarization,
                                 short noDataValue) throws IOException {
        return (short) (int) getInterpolatedBtData(gridPointIndex, fieldIndex, polarization, noDataValue, PointFilter.NULL);
    }

    @Override
    public int getBrowseBtData(int gridPointIndex, int fieldIndex, int polarization,
                               int noDataValue) throws IOException {
        return (int) getInterpolatedBtData(gridPointIndex, fieldIndex, polarization, noDataValue, PointFilter.NULL);
    }

    @Override
    public float getBrowseBtData(int gridPointIndex, int fieldIndex, int polarization,
                                 float noDataValue) throws IOException {
        return (float) getInterpolatedBtData(gridPointIndex, fieldIndex, polarization, noDataValue, PointFilter.NULL);
    }

    private double getInterpolatedBtData(int gridPointIndex, int fieldIndex, int polarization, double noDataValue,
                                         PointFilter pointFilter) throws IOException {
        final SequenceData btDataList = getBtDataList(gridPointIndex);
        final int elementCount = btDataList.getElementCount();
        // todo - inline regression
        final SimpleLinearRegressor regressor = new SimpleLinearRegressor(pointFilter);

        boolean hasLower = false;
        boolean hasUpper = false;

        for (int i = 0; i < elementCount; ++i) {
            final CompoundData data = btDataList.getCompound(i);
            final int polarizationFlags = data.getInt(flagsIndex) & 3;
            final int incidenceAngle = data.getInt(incidenceAngleIndex);

            if ((polarization == polarizationFlags || (polarization & polarizationFlags & 2) != 0)) {
                if (incidenceAngle >= MIN_INCIDENCE_ANGLE && incidenceAngle <= MAX_INCIDENCE_ANGLE) {
                    final double fieldValue = data.getDouble(fieldIndex);
                    regressor.add(incidenceAngle, fieldValue);
                    if (fieldIndex == incidenceAngleIndex) {
                        System.out.println("incidenceAngle = " + incidenceAngle);    
                        System.out.println("fieldValue = " + fieldValue);    
                    }
                    if (!hasLower) {
                        hasLower = incidenceAngle <= CENTER_INCIDENCE_ANGLE;
                    }
                    if (!hasUpper) {
                        hasUpper = incidenceAngle > CENTER_INCIDENCE_ANGLE;
                    }
                }
            }
        }
        if (hasLower && hasUpper) {
            final Point2D point = regressor.getRegression();
            return point.getX() * CENTER_INCIDENCE_ANGLE + point.getY();
        } else {
            return noDataValue;
        }
    }

    public final int getSnapshotIdMin() {
        return snapshotIdMin;
    }

    public final int getSnapshotIdMax() {
        return snapshotIdMax;
    }

    public final int getSnapshotIndex(int snapshotId) {
        return snapshotIndexes[snapshotId - snapshotIdMin];
    }

    public final SequenceData getSnapshotList() {
        return snapshotList;
    }

    public final CompoundData getSnapshotData(int snapshotIndex) throws IOException {
        return snapshotList.getCompound(snapshotIndex);
    }

    public final CompoundType getSnapshotType() {
        return snapshotType;
    }

    public int[] createSnapshotIndexes() throws IOException {
        final int snapshotIdIndex = snapshotType.getMemberIndex(SNAPSHOT_ID_NAME);

        int minId = Integer.MAX_VALUE;
        int maxId = Integer.MIN_VALUE;

        final int snapshotCount = snapshotList.getElementCount();
        for (int i = 0; i < snapshotCount; i++) {
            final CompoundData snapshotData = getSnapshotData(i);
            final int id = snapshotData.getInt(snapshotIdIndex);

            if (id < minId) {
                minId = id;
            }
            if (id > maxId) {
                maxId = id;
            }
        }

        // todo - user logger or remove (rq-20081205)
        System.out.println("SmosFile: snapshotIdMin = " + minId);
        System.out.println("SmosFile: snapshotIdMax = " + maxId);

        final int[] snapshotIndexes = new int[maxId - minId + 1];

        Arrays.fill(snapshotIndexes, -1);
        for (int i = 0; i < snapshotCount; i++) {
            final CompoundData snapshotData = getSnapshotData(i);
            final int id = snapshotData.getInt(snapshotIdIndex);

            snapshotIndexes[id - minId] = i;
        }

        // todo - user logger or remove (rq-20081205)
        System.out.println("SmosFile: snapshotCount = " + snapshotCount);
        System.out.println("SmosFile: total number of snapshotIndexes = " + snapshotIndexes.length);

        int n = 0;
        for (int snapshotIndex : snapshotIndexes) {
            if (snapshotIndex != -1) {
                n++;
            }
        }

        // todo - user logger or remove (rq-20081205)
        System.out.println("SmosFile: number of valid snapshotIndexes = " + n);

        snapshotIdMin = minId;
        snapshotIdMax = maxId;

        return snapshotIndexes;
    }
}
