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

    public static final float CENTER_BROWSE_INCIDENCE_ANGLE = 42.5f;
    public static final float MIN_BROWSE_INCIDENCE_ANGLE = 37.5f;
    public static final float MAX_BROWSE_INCIDENCE_ANGLE = 52.5f;
    public static final float INCIDENCE_ANGLE_FACTOR = 0.001373291f; // 90.0 / 2^16

    private final int flagsIndex;
    private final int incidenceAngleIndex;

    private final int snapshotIdIndex;
    private final SequenceData snapshotList;

    private final CompoundType snapshotType;
    private final int[] snapshotIndexes;
    private int snapshotIdMin;

    private int snapshotIdMax;

    public L1cScienceSmosFile(File file, DataFormat format) throws IOException {
        super(file, format);

        flagsIndex = getBtDataType().getMemberIndex(SmosFormats.BT_FLAGS_NAME);
        incidenceAngleIndex = this.btDataType.getMemberIndex(SmosFormats.BT_INCIDENCE_ANGLE_NAME);
        snapshotIdIndex = btDataType.getMemberIndex(SmosFormats.BT_SNAPSHOT_ID_OF_PIXEL_NAME);

        snapshotList = getDataBlock().getSequence(SmosFormats.SNAPSHOT_LIST_NAME);
        if (snapshotList == null) {
            throw new IOException("Data block does not include snapshot list.");
        }

        snapshotType = (CompoundType) snapshotList.getSequenceType().getElementType();
        snapshotIndexes = createSnapshotIndexes();
    }

    @Override
    public short getBrowseBtData(int gridPointIndex, int fieldIndex, int polMode,
                                 short noDataValue) throws IOException {
        if (fieldIndex == flagsIndex) {
            return (short) getCombinedBtFlags(gridPointIndex, polMode, noDataValue);
        } else {
            return (short) (int) getInterpolatedBtData(gridPointIndex, fieldIndex, polMode, noDataValue);
        }
    }

    @Override
    public int getBrowseBtData(int gridPointIndex, int fieldIndex, int polMode,
                               int noDataValue) throws IOException {
        if (fieldIndex == flagsIndex) {
            return getCombinedBtFlags(gridPointIndex, polMode, noDataValue);
        } else {
            return (int) getInterpolatedBtData(gridPointIndex, fieldIndex, polMode, noDataValue);
        }
    }

    @Override
    public float getBrowseBtData(int gridPointIndex, int fieldIndex, int polMode,
                                 float noDataValue) throws IOException {
        return getInterpolatedBtData(gridPointIndex, fieldIndex, polMode, noDataValue);
    }

    @Override
    public short getSnapshotBtData(int gridPointIndex, int fieldIndex, int polarisation, int snapshotId,
                                   short noDataValue) throws IOException {
        final CompoundData btData = getSnapshotBtData(gridPointIndex, snapshotId, polarisation);

        if (btData != null) {
            return btData.getShort(fieldIndex);
        }

        return noDataValue;
    }

    @Override
    public int getSnapshotBtData(int gridPointIndex, int fieldIndex, int polMode, int snapshotId,
                                 int noDataValue) throws IOException {
        final CompoundData btData = getSnapshotBtData(gridPointIndex, snapshotId, polMode);

        if (btData != null) {
            return btData.getInt(fieldIndex);
        }

        return noDataValue;
    }

    @Override
    public float getSnapshotBtData(int gridPointIndex, int fieldIndex, int polMode, int snapshotId,
                                   float noDataValue) throws IOException {
        final CompoundData btData = getSnapshotBtData(gridPointIndex, snapshotId, polMode);

        if (btData != null) {
            return btData.getFloat(fieldIndex);
        }

        return noDataValue;
    }

    private CompoundData getSnapshotBtData(int gridPointIndex, int snapshotId, int polMode) throws IOException {
        final SequenceData btDataList = getBtDataList(gridPointIndex);
        final int elementCount = btDataList.getElementCount();

        CompoundData btData = btDataList.getCompound(0);
        if (btData.getInt(snapshotIdIndex) > snapshotId) {
            return null;
        }
        btData = btDataList.getCompound(elementCount - 1);
        if (btData.getInt(snapshotIdIndex) < snapshotId) {
            return null;
        }
        for (int i = 0; i < elementCount; ++i) {
            btData = btDataList.getCompound(i);
            if (btData.getInt(snapshotIdIndex) == snapshotId) {
                final int flags = btData.getInt(flagsIndex);
                if (polMode == (flags & 3) || (polMode & flags & 2) != 0) {
                    return btData;
                }
            }
        }

        return null;
    }

    private int getCombinedBtFlags(int gridPointIndex, int polMode, int noDataValue) throws IOException {
        final SequenceData btDataList = getBtDataList(gridPointIndex);
        final int elementCount = btDataList.getElementCount();

        int combinedFlags = 0;

        boolean hasLower = false;
        boolean hasUpper = false;

        for (int i = 0; i < elementCount; ++i) {
            final CompoundData btData = btDataList.getCompound(i);
            final int flags = btData.getInt(flagsIndex);

            if (polMode == (flags & 3) || (polMode & flags & 2) != 0) {
                final float incidenceAngle = INCIDENCE_ANGLE_FACTOR * btData.getInt(incidenceAngleIndex);

                if (incidenceAngle >= MIN_BROWSE_INCIDENCE_ANGLE && incidenceAngle <= MAX_BROWSE_INCIDENCE_ANGLE) {
                    combinedFlags |= flags;

                    if (!hasLower) {
                        hasLower = incidenceAngle <= CENTER_BROWSE_INCIDENCE_ANGLE;
                    }
                    if (!hasUpper) {
                        hasUpper = incidenceAngle > CENTER_BROWSE_INCIDENCE_ANGLE;
                    }
                }
            }
        }
        if (hasLower && hasUpper) {
            return combinedFlags;
        }

        return noDataValue;
    }

    private float getInterpolatedBtData(int gridPointIndex, int fieldIndex, int polMode,
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

        for (int i = 0; i < elementCount; ++i) {
            final CompoundData btData = btDataList.getCompound(i);
            final int flags = btData.getInt(flagsIndex);

            if (polMode == (flags & 3) || (polMode & flags & 2) != 0) {
                final float incidenceAngle = INCIDENCE_ANGLE_FACTOR * btData.getInt(incidenceAngleIndex);

                if (incidenceAngle >= MIN_BROWSE_INCIDENCE_ANGLE && incidenceAngle <= MAX_BROWSE_INCIDENCE_ANGLE) {
                    final float btValue = btData.getFloat(fieldIndex);

                    sx += incidenceAngle;
                    sy += btValue;
                    sxx += incidenceAngle * incidenceAngle;
                    sxy += incidenceAngle * btValue;
                    count++;

                    if (!hasLower) {
                        hasLower = incidenceAngle <= CENTER_BROWSE_INCIDENCE_ANGLE;
                    }
                    if (!hasUpper) {
                        hasUpper = incidenceAngle > CENTER_BROWSE_INCIDENCE_ANGLE;
                    }
                }
            }
        }
        if (hasLower && hasUpper) {
            final float a = (count * sxy - sx * sy) / (count * sxx - sx * sx);
            final float b = (sy - a * sx) / count;
            return a * CENTER_BROWSE_INCIDENCE_ANGLE + b;
        }

        return noDataValue;
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

    public final Rectangle2D computeSnapshotRegion(int snapshotId, ProgressMonitor pm) throws IOException {
        final int latIndex = getGridPointType().getMemberIndex("Grid_Point_Latitude");
        final int lonIndex = getGridPointType().getMemberIndex("Grid_Point_Longitude");
        final SequenceData gridPointList = getGridPointList();

        Rectangle2D.Float region = null;
        try {
            pm.beginTask("Visiting grid points...", gridPointList.getElementCount() / 100);

            for (int i = 0; i < gridPointList.getElementCount(); i++) {
                final SequenceData btDataList = getBtDataList(i);

                if (btDataList.getElementCount() >= 1) {
                    final int minId = getSnapshotId(btDataList, 0);

                    if (snapshotId >= minId) {
                        final int maxId = getSnapshotId(btDataList, btDataList.getElementCount() - 1);
                        if (snapshotId <= maxId) {
                            final float lon = gridPointList.getCompound(i).getFloat(lonIndex);
                            final float lat = gridPointList.getCompound(i).getFloat(latIndex);
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

    private int getSnapshotId(SequenceData btDataList, int btDataIndex) throws IOException {
        Assert.argument(btDataList.getSequenceType().getElementType() == btDataType);
        return btDataList.getCompound(btDataIndex).getInt(snapshotIdIndex);
    }

    private int[] createSnapshotIndexes() throws IOException {
        final int snapshotIdIndex = snapshotType.getMemberIndex(SmosFormats.SNAPSHOT_ID_NAME);

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
