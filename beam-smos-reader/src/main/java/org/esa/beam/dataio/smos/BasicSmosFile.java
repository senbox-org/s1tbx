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

import com.bc.ceres.binio.*;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class BasicSmosFile implements GridPointDataProvider {
    public static final String GRID_POINT_LIST_NAME = "Grid_Point_List";
    public static final String GRID_POINT_ID_NAME = "Grid_Point_ID";

    private final File file;
    private final DataFormat format;
    private final DataContext dataContext;
    private final CompoundData dataBlock;

    private final SequenceData gridPointList;
    private final CompoundType gridPointType;
    private final int gridPointIdIndex;
    private int[] gridPointIndexes;

    public BasicSmosFile(File file, DataFormat format) throws IOException {
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

        initGridPointIndexes();
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

    public CompoundData getDataBlock() {
        return dataBlock;
    }

    public final SequenceData getGridPointList() {
        return gridPointList;
    }

    @Override
    public final int getGridPointIndex(int seqnum) {
        return gridPointIndexes[seqnum];
    }

    @Override
    public final CompoundType getGridPointType() {
        return gridPointType;
    }

    @Override
    public final CompoundData getGridPointData(int gridPointIndex) throws IOException {
        return gridPointList.getCompound(gridPointIndex);
    }

    public void close() {
        dataContext.dispose();
    }

    private void initGridPointIndexes() throws IOException {
        // todo - first find seqnumMin, seqnumMax, then gridPointIndexes = new int[seqnumMax-seqnumMin+1]  (nf-20081203)
        gridPointIndexes = new int[2621442 + 1];

        Arrays.fill(gridPointIndexes, -1);
        final int gridPointCount = gridPointList.getElementCount();

        for (int i = 0; i < gridPointCount; i++) {
            final int seqnum = getGridPointSeqnum(i);
            gridPointIndexes[seqnum] = i;
        }

        // todo - user logger or remove (rq-20081203)
        System.out.println("SmosFile: gridPointCount = " + gridPointCount);
        System.out.println("SmosFile: gridPointIndexes.length = " + gridPointIndexes.length);

        int indexCount = 0;
        for (int gridPointIndex : gridPointIndexes) {
            if (gridPointIndex != -1) {
                indexCount++;
            }
        }

        // todo - user logger or remove (rq-20081203)
        System.out.println("SmosFile: number of gridPointIndexes != -1: " + indexCount);
    }
}
