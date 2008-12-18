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

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;


public class SmosFile implements GridPointDataProvider {

    private final File file;
    private final DataFormat format;
    private final DataContext dataContext;
    private final CompoundData dataBlock;

    private final SequenceData gridPointList;
    private final CompoundType gridPointType;
    private final int gridPointIdIndex;
    private final int[] gridPointIndexes;
    private final Area region;

    private int minSeqnum;
    private int maxSeqnum;

    public SmosFile(File file, DataFormat format) throws IOException {
        this.file = file;
        this.format = format;

        dataContext = format.createContext(file, "r");
        dataBlock = dataContext.getData();

        gridPointList = dataBlock.getSequence(SmosFormats.GRID_POINT_LIST_NAME);
        if (gridPointList == null) {
            throw new IllegalStateException(MessageFormat.format(
                    "SMOS File ''{0}'': Missing grid point list.", file.getPath()));
        }

        gridPointType = (CompoundType) gridPointList.getSequenceType().getElementType();
        gridPointIdIndex = gridPointType.getMemberIndex(SmosFormats.GRID_POINT_ID_NAME);
        gridPointIndexes = createGridPointIndexes();
        region = computeRegion();
    }

    public final int getGridPointCount() {
        return gridPointList.getElementCount();
    }

    public final int getGridPointId(int i) throws IOException {
        return gridPointList.getCompound(i).getInt(gridPointIdIndex);
    }

    public final int getGridPointSeqnum(int i) throws IOException {
        return SmosDgg.smosGridPointIdToDggridSeqnum(getGridPointId(i));
    }

    public final File getFile() {
        return file;
    }

    public final DataFormat getFormat() {
        return format;
    }

    public final DataContext getDataContext() {
        return dataContext;
    }

    public final CompoundData getDataBlock() {
        return dataBlock;
    }

    public SequenceData getGridPointList() {
        return gridPointList;
    }

    @Override
    public int getGridPointIndex(int seqnum) {
        if (seqnum < minSeqnum || seqnum > maxSeqnum) {
            return -1;
        }

        return gridPointIndexes[seqnum - minSeqnum];
    }

    @Override
    public CompoundType getGridPointType() {
        return gridPointType;
    }

    @Override
    public CompoundData getGridPointData(int gridPointIndex) throws IOException {
        return gridPointList.getCompound(gridPointIndex);
    }

    public Area getRegion() {
        return region;
    }

    public void close() {
        dataContext.dispose();
    }

    private Area computeRegion() throws IOException {
        final int latIndex = getGridPointType().getMemberIndex(SmosFormats.GRID_POINT_LATITUDE_NAME);
        final int lonIndex = getGridPointType().getMemberIndex(SmosFormats.GRID_POINT_LONGITUDE_NAME);
        final SequenceData gridPointList = getGridPointList();

        final Area region = new Area();

        for (int i = 0; i < gridPointList.getElementCount(); i++) {
            final float lon = gridPointList.getCompound(i).getFloat(lonIndex);
            final float lat = gridPointList.getCompound(i).getFloat(latIndex);

            // todo - normalise longitudes to [-180, 180] (rq-20081218)

            if (!region.contains(lon, lat)) {
                final float x = Math.max(lon - 6.0f, -180.0f);
                final float y = Math.max(lat - 6.0f, -90.0f);
                final float w = Math.min(x + 12.0f, 180.0f) - x;
                final float h = Math.min(y + 12.0f, 90.0f) - y;

                region.add(new Area(new Rectangle2D.Float(x, y, w, h)));
            }
        }

        return region;
    }

    private int[] createGridPointIndexes() throws IOException {
        minSeqnum = getGridPointSeqnum(0);
        maxSeqnum = minSeqnum;

        final int gridPointCount = getGridPointCount();
        for (int i = 1; i < gridPointCount; i++) {
            final int seqnum = getGridPointSeqnum(i);

            if (seqnum < minSeqnum) {
                minSeqnum = seqnum;
            } else {
                if (seqnum > maxSeqnum) {
                    maxSeqnum = seqnum;
                }
            }
        }

        final int[] gridPointIndexes = new int[maxSeqnum - minSeqnum + 1];
        Arrays.fill(gridPointIndexes, -1);

        for (int i = 0; i < gridPointCount; i++) {
            gridPointIndexes[getGridPointSeqnum(i) - minSeqnum] = i;
        }

        // todo - user logger or remove (rq-20081203)
        System.out.println("SmosFile: gridPointCount = " + gridPointCount);
        System.out.println("SmosFile: gridPointIndexes.length = " + gridPointIndexes.length);

        int indexCount = 0;
        for (final int gridPointIndex : gridPointIndexes) {
            if (gridPointIndex != -1) {
                indexCount++;
            }
        }

        // todo - user logger or remove (rq-20081203)
        System.out.println("SmosFile: number of gridPointIndexes != -1: " + indexCount);

        return gridPointIndexes;
    }
}
