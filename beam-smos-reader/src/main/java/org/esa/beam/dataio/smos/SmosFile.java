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
import com.bc.ceres.binio.DataContext;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.text.MessageFormat;


public class SmosFile implements GridPointDataProvider {
    public static final int POL_MODE_MASK = 0x00000003;
    public static final int POL_MODE_H = 0;
    public static final int POL_MODE_V = 1;
    public static final int POL_MODE_HV1 = 2;
    public static final int POL_MODE_HV2 = 3;

    public static final String GRID_POINT_LIST_NAME = "Grid_Point_List";
    public static final String GRID_POINT_ID_NAME = "Grid_Point_ID";

    private final File file;
    private final DataFormat format;
    private final DataContext dataContext;
    private final CompoundData dataBlock;

    private final SequenceData gridPointList;
    private final CompoundType gridPointType;
    private final int gridPointIdIndex;
    private final int[] gridPointIndexes;

    private int minSeqnum;
    private int maxSeqnum;

    public SmosFile(File file, DataFormat format) throws IOException {
        this.file = file;
        this.format = format;

        dataContext = format.createContext(file, "r");
        dataBlock = dataContext.getData();

        gridPointList = dataBlock.getSequence(GRID_POINT_LIST_NAME);
        if (gridPointList == null) {
            throw new IllegalStateException(MessageFormat.format(
                    "SMOS File ''{0}'': Missing grid point list.", file.getPath()));
        }

        gridPointType = (CompoundType) gridPointList.getSequenceType().getElementType();
        gridPointIdIndex = gridPointType.getMemberIndex(GRID_POINT_ID_NAME);
        gridPointIndexes = createGridPointIndexes();
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

    public int getGridPointIndex(int seqnum) {
        if (seqnum < minSeqnum || seqnum > maxSeqnum) {
            return -1;
        }

        return gridPointIndexes[seqnum - minSeqnum];
    }

    public CompoundType getGridPointType() {
        return gridPointType;
    }

    public CompoundData getGridPointData(int gridPointIndex) throws IOException {
        return gridPointList.getCompound(gridPointIndex);
    }

    public void close() {
        dataContext.dispose();
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
