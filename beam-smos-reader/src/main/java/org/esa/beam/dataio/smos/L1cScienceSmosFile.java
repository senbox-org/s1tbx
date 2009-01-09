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
import java.text.MessageFormat;
import java.util.*;

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

    private final int snapshotIdOfPixelIndex;
    private final SequenceData snapshotList;
    private final CompoundType snapshotType;

    private Map<Integer, Integer> snapshotIndexMap;

    private Integer[] snapshotIds;
    private Integer[] xPolSnapshotIds;
    private Integer[] yPolSnapshotIds;
    private Integer[] xyPolSnapshotIds;

    public L1cScienceSmosFile(File file, DataFormat format) throws IOException {
        super(file, format);

        flagsIndex = getBtDataType().getMemberIndex(SmosFormats.BT_FLAGS_NAME);
        incidenceAngleIndex = this.btDataType.getMemberIndex(SmosFormats.BT_INCIDENCE_ANGLE_NAME);
        snapshotIdOfPixelIndex = btDataType.getMemberIndex(SmosFormats.BT_SNAPSHOT_ID_OF_PIXEL_NAME);

        snapshotList = getDataBlock().getSequence(SmosFormats.SNAPSHOT_LIST_NAME);
        if (snapshotList == null) {
            throw new IOException("Data block does not include snapshot list.");
        }
        snapshotType = (CompoundType) snapshotList.getSequenceType().getElementType();

        tabulateSnapshotIds();
    }

    private void tabulateSnapshotIds() throws IOException {
        final SortedSet<Integer> x = new TreeSet<Integer>();
        final SortedSet<Integer> y = new TreeSet<Integer>();
        final SortedSet<Integer> xy = new TreeSet<Integer>();
        final SortedSet<Integer> union = new TreeSet<Integer>();

        final int gridPointCount = getGridPointCount();
        for (int i = 0; i < gridPointCount; i++) {
            final SequenceData btList = getBtDataList(i);

            final int btCount = btList.getElementCount();
            for (int j = 0; j < btCount; j++) {
                final CompoundData btData = btList.getCompound(j);
                final int sid = btData.getInt(snapshotIdOfPixelIndex);
                final int flags = btData.getInt(flagsIndex);

                if (!union.contains(sid)) {
                    switch (flags & SmosFormats.L1C_POL_FLAGS_MASK) {
                        case SmosFormats.L1C_POL_MODE_X:
                            x.add(sid);
                            break;
                        case SmosFormats.L1C_POL_MODE_Y:
                            y.add(sid);
                            break;
                        case SmosFormats.L1C_POL_MODE_XY1:
                        case SmosFormats.L1C_POL_MODE_XY2:
                            xy.add(sid);
                            break;
                    }
                    union.add(sid);
                }
            }
        }

        snapshotIds = union.toArray(new Integer[union.size()]);
        xPolSnapshotIds = x.toArray(new Integer[x.size()]);
        yPolSnapshotIds = y.toArray(new Integer[y.size()]);
        xyPolSnapshotIds = xy.toArray(new Integer[xy.size()]);

        System.out.println("SmosFile: snapshotCount(x) = " + x.size());
        System.out.println("SmosFile: snapshotCount(y) = " + y.size());
        System.out.println("SmosFile: snapshotCount(xy) = " + xy.size());
        System.out.println("SmosFile: snapshotCount(all) = " + union.size());

        snapshotIndexMap = new TreeMap<Integer, Integer>();

        final int snapshotIdIndex = snapshotType.getMemberIndex(SmosFormats.SNAPSHOT_ID_NAME);
        final int snapshotCount = snapshotList.getElementCount();
        for (int i = 0; i < snapshotCount; i++) {
            final CompoundData snapshotData = getSnapshotData(i);
            final int sid = snapshotData.getInt(snapshotIdIndex);

            if (union.contains(sid)) {
                snapshotIndexMap.put(sid, i);
            }
        }

        System.out.println("snapshotIds.length = " + snapshotIds.length);
        System.out.println("snapshotIndexMap.size() = " + snapshotIndexMap.size());
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
        if (btData.getInt(snapshotIdOfPixelIndex) > snapshotId) {
            return null;
        }
        btData = btDataList.getCompound(elementCount - 1);
        if (btData.getInt(snapshotIdOfPixelIndex) < snapshotId) {
            return null;
        }
        for (int i = 0; i < elementCount; ++i) {
            btData = btDataList.getCompound(i);
            if (btData.getInt(snapshotIdOfPixelIndex) == snapshotId) {
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
        return snapshotIds[0];
    }

    public final int getSnapshotIdMax() {
        return snapshotIds[snapshotIds.length - 1];
    }

    public final int getSnapshotIndex(int snapshotId) {
        if (!snapshotIndexMap.containsKey(snapshotId)) {
            throw new IllegalArgumentException(MessageFormat.format("Illegal snapshot ID: {0}", snapshotId));
        }

        return snapshotIndexMap.get(snapshotId);
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
                            float lon = gridPointList.getCompound(i).getFloat(lonIndex);
                            float lat = gridPointList.getCompound(i).getFloat(latIndex);
                            // normalisation to [-180, 180] necessary for some L1c test products
                            if (lon > 180.0f) {
                                lon = lon - 360.0f;
                            }
                            final Rectangle2D.Float rectangle =
                                    new Rectangle2D.Float(lon - 0.02f, lat - 0.02f, 0.04f, 0.04f);
                            if (region == null) {
                                region = rectangle;
                            } else {
                                region.add(rectangle);
                            }
                        }
                    }
                }
                pm.worked(1);
            }
            if (region == null) {
                region = new Rectangle2D.Float(-180.0f, -90.0f, 360.0f, 180.0f);
            }
        } finally {
            pm.done();
        }

        return region;
    }

    private int getSnapshotId(SequenceData btDataList, int btDataIndex) throws IOException {
        Assert.argument(btDataList.getSequenceType().getElementType() == btDataType);
        return btDataList.getCompound(btDataIndex).getInt(snapshotIdOfPixelIndex);
    }

    public Integer[] getSnapshotIds() {
        return snapshotIds.clone();
    }

    public int getSnapshotIdCount() {
        return snapshotIds.length;
    }
}
