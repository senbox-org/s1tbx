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
import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;

import java.awt.geom.Rectangle2D;
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

    public static final String BT_INCIDENCE_ANGLE_MEMBER_NAME = "Incidence_Angle";
    public static final String BT_SNAPSHOT_ID_MEMBER_NAME = "Snapshot_ID_of_Pixel";

    public static final String SNAPSHOT_LIST_MEMBER_NAME = "Snapshot_List";
    public static final String SNAPSHOT_ID_NAME = "Snapshot_ID";

    public static final float CENTER_INCIDENCE_ANGLE = 42.5f;
    public static final float MIN_INCIDENCE_ANGLE = 37.5f;
    public static final float MAX_INCIDENCE_ANGLE = 52.5f;

    private final int flagsIndex;

    private final int incidenceAngleIndex;
    private final int snapshotIdIndex;

    private final SequenceData snapshotList;
    private final CompoundType snapshotType;

    private final int[] snapshotIndexes;
    private int snapshotIdMin;
    private int snapshotIdMax;
    private static final float INCIDENCE_ANGLE_FACTOR = 90.0f / (float) Math.pow(2, 16);

    public L1cScienceSmosFile(File file, DataFormat format) throws IOException {
        super(file, format);

        flagsIndex = getBtDataType().getMemberIndex(BT_FLAGS_MEMBER_NAME);
        incidenceAngleIndex = this.btDataType.getMemberIndex(BT_INCIDENCE_ANGLE_MEMBER_NAME);
        snapshotIdIndex = btDataType.getMemberIndex(BT_SNAPSHOT_ID_MEMBER_NAME);

        snapshotList = getDataBlock().getSequence(SNAPSHOT_LIST_MEMBER_NAME);
        if (snapshotList == null) {
            throw new IOException("Data block does not include snapshot list.");
        }

        snapshotType = (CompoundType) snapshotList.getSequenceType().getElementType();
        snapshotIndexes = createSnapshotIndexes();
    }

    @Override
    public short getBrowseBtData(int gridPointIndex, int fieldIndex, int polarization,
                                 short noDataValue) throws IOException {
        return (short) (int) getInterpolatedBtData(gridPointIndex, fieldIndex, polarization, noDataValue);
    }

    @Override
    public int getBrowseBtData(int gridPointIndex, int fieldIndex, int polarization,
                               int noDataValue) throws IOException {
        if (fieldIndex == flagsIndex) {
            return getCombinedBtData(gridPointIndex, polarization, noDataValue);
        } else {
            return (int) getInterpolatedBtData(gridPointIndex, fieldIndex, polarization, noDataValue);
        }
    }

    @Override
    public float getBrowseBtData(int gridPointIndex, int fieldIndex, int polarization,
                                 float noDataValue) throws IOException {
        return getInterpolatedBtData(gridPointIndex, fieldIndex, polarization, noDataValue);
    }

    private int getCombinedBtData(int gridPointIndex, int polarization, int noDataValue) throws IOException {
        final SequenceData btDataList = getBtDataList(gridPointIndex);
        final int elementCount = btDataList.getElementCount();

        int combinedFlags = 0;

        boolean hasLower = false;
        boolean hasUpper = false;

        CompoundData btData;
        float incidenceAngle;

        for (int i = 0; i < elementCount; ++i) {
            btData = btDataList.getCompound(i);
            final int flags = btData.getInt(flagsIndex) & 3;
            if (isPolarisationAccepted(flags & 3, polarization)) {
                incidenceAngle = INCIDENCE_ANGLE_FACTOR * btData.getInt(incidenceAngleIndex);
                if (incidenceAngle >= MIN_INCIDENCE_ANGLE && incidenceAngle <= MAX_INCIDENCE_ANGLE) {
                    combinedFlags |=  flags;
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
            return combinedFlags;
        }
        return noDataValue;
    }

    private float getInterpolatedBtData(int gridPointIndex, int fieldIndex, int polarization,
                                        float noDataValue) throws IOException {
        final SequenceData btDataList = getBtDataList(gridPointIndex);
        final int elementCount = btDataList.getElementCount();

        int count = 0;
        float sx = 0;
        float sy = 0;
        float sxx = 0;
        float sxy = 0;

        boolean hasLower = false;
        boolean hasUpper = false;

        CompoundData btData;
        float incidenceAngle;
        float btValue;

        for (int i = 0; i < elementCount; ++i) {
            btData = btDataList.getCompound(i);
            if (isPolarisationAccepted(btData, polarization)) {
                incidenceAngle = INCIDENCE_ANGLE_FACTOR * btData.getInt(incidenceAngleIndex);
                if (incidenceAngle >= MIN_INCIDENCE_ANGLE && incidenceAngle <= MAX_INCIDENCE_ANGLE) {
                    btValue = btData.getFloat(fieldIndex);

                    sx += incidenceAngle;
                    sy += btValue;
                    sxx += incidenceAngle * incidenceAngle;
                    sxy += incidenceAngle * btValue;
                    count++;

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
            final float a = (count * sxy - sx * sy) / (count * sxx - sx * sx);
            final float b = (sy - a * sx) / count;
            return a * CENTER_INCIDENCE_ANGLE + b;
        } else {
            return noDataValue;
        }
    }

    @Override
    public short getSnapshotBtData(int gridPointIndex, int fieldIndex,
                                   int polarization,
                                   int snapshotId, short noDataValue) throws IOException {
        final SequenceData btDataList = getBtDataList(gridPointIndex);
        final int elementCount = btDataList.getElementCount();
        CompoundData btData = btDataList.getCompound(0);
        if (btData.getInt(snapshotIdIndex) > snapshotId) {
            return noDataValue;
        }
        btData = btDataList.getCompound(elementCount - 1);
        if (btData.getInt(snapshotIdIndex) < snapshotId) {
            return noDataValue;
        }
        for (int i = 0; i < elementCount; ++i) {
            btData = btDataList.getCompound(i);
            if (isPolarisationAccepted(btData, polarization)
                    && btData.getInt(snapshotIdIndex) == snapshotId) {
                return btData.getShort(fieldIndex);
            }
        }
        return noDataValue;
    }

    @Override
    public int getSnapshotBtData(int gridPointIndex, int fieldIndex,
                                 int polarization,
                                 int snapshotId, int noDataValue) throws IOException {
        final SequenceData btDataList = getBtDataList(gridPointIndex);
        final int elementCount = btDataList.getElementCount();
        CompoundData btData = btDataList.getCompound(0);
        if (btData.getInt(snapshotIdIndex) > snapshotId) {
            return noDataValue;
        }
        btData = btDataList.getCompound(elementCount - 1);
        if (btData.getInt(snapshotIdIndex) < snapshotId) {
            return noDataValue;
        }
        for (int i = 0; i < elementCount; ++i) {
            btData = btDataList.getCompound(i);
            if (isPolarisationAccepted(btData, polarization)
                    && btData.getInt(snapshotIdIndex) == snapshotId) {
                return btData.getInt(fieldIndex);
            }
        }
        return noDataValue;
    }

    @Override
    public float getSnapshotBtData(int gridPointIndex, int fieldIndex,
                                   int polarization,
                                   int snapshotId, float noDataValue) throws IOException {
        final SequenceData btDataList = getBtDataList(gridPointIndex);
        final int elementCount = btDataList.getElementCount();
        CompoundData btData = btDataList.getCompound(0);
        if (btData.getInt(snapshotIdIndex) > snapshotId) {
            return noDataValue;
        }
        btData = btDataList.getCompound(elementCount - 1);
        if (btData.getInt(snapshotIdIndex) < snapshotId) {
            return noDataValue;
        }
        for (int i = 0; i < elementCount; ++i) {
            btData = btDataList.getCompound(i);
            if (isPolarisationAccepted(btData, polarization)
                    && btData.getInt(snapshotIdIndex) == snapshotId) {
                return btData.getFloat(fieldIndex);
            }
        }
        return noDataValue;
    }

    private boolean isPolarisationAccepted(CompoundData data, int polarization) throws IOException {
        final int polarizationFlags = data.getInt(flagsIndex) & 3;
        return isPolarisationAccepted(polarizationFlags, polarization);
    }

    private static boolean isPolarisationAccepted(int polarizationFlags, int polarization) {
        return polarization == polarizationFlags || (polarization & polarizationFlags & 2) != 0;
    }

    public int getSnapshotId(SequenceData btDataList, int btDataIndex) throws IOException {
        Assert.argument(btDataList.getSequenceType().getElementType() == btDataType);
        return btDataList.getCompound(btDataIndex).getInt(snapshotIdIndex);
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

    public Rectangle2D computeSnapshotRegion(int snapshotId, ProgressMonitor pm) throws IOException {
        Rectangle2D.Float region = null;
        int latIndex = getGridPointType().getMemberIndex("Grid_Point_Latitude");
        int lonIndex = getGridPointType().getMemberIndex("Grid_Point_Longitude");
        SequenceData gridPointList = getGridPointList();
        try {
            pm.beginTask("Visiting grid points...", gridPointList.getElementCount() / 100);
            for (int i = 0; i < gridPointList.getElementCount(); i++) {
                SequenceData btDataList = getBtDataList(i);
                if (btDataList.getElementCount() >= 1) {
                    int idMin = getSnapshotId(btDataList, 0);
                    if (snapshotId >= idMin) {
                        int idMax = getSnapshotId(btDataList, btDataList.getElementCount() - 1);
                        if (snapshotId <= idMax) {
                            float lon = gridPointList.getCompound(i).getFloat(lonIndex);
                            float lat = gridPointList.getCompound(i).getFloat(latIndex);
                            if (region == null) {
                                region = new Rectangle2D.Float(lon, lat, 0.0f, 0.0f);
                            } else {
                                region.add(lon, lat);
                            }
                        }
                    }
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
        return region;
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
