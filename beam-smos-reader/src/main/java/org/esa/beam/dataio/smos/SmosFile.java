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
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;


public class SmosFile {

    private final File file;
    private final DataFormat format;
    private final SequenceData gridPointList;
    private int[] gridPointIndexes;
    private final DataContext dataContext;

    public SmosFile(File file, DataFormat format) throws IOException {
        this.file = file;
        this.format = format;
        this.dataContext = format.createContext(file, "r");
        CompoundData smosDataset = dataContext.getData();
        this.gridPointList = smosDataset.getSequence("Grid_Point_List");
        initGridPointIndexes();
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

    public short getL1CBrowseBtDataShort(int gridPointIndex, int btDataIndex) throws IOException {
        SequenceData btDataList = getBtDataList(gridPointIndex);
        CompoundData btData = btDataList.getCompound(0);
        return btData.getShort(btDataIndex);
    }

    public int getL1CBrowseBtDataInt(int gridPointIndex, int btDataIndex) throws IOException {
        SequenceData btDataList = getBtDataList(gridPointIndex);
        CompoundData btData = btDataList.getCompound(0);
        return btData.getInt(btDataIndex);
    }

    public float getL1CBrowseBtDataFloat(int gridPointIndex, int btDataIndex) throws IOException {
        SequenceData btDataList = getBtDataList(gridPointIndex);
        CompoundData btData = btDataList.getCompound(0);
        return btData.getInt(btDataIndex);
    }

    public float getL1CBtDataFloat(int gridPointIndex, int btDataIndex) throws IOException {
        SequenceData btDataList = getBtDataList(gridPointIndex);
        final int btDataListCount = btDataList.getElementCount();
        float mean = 0.0f;
        // todo - collect values around incidence angle 42.5 degrees and average
        int n = Math.min(1, btDataListCount);
        for (int i = 0; i < n; i++) {
            CompoundData btData = btDataList.getCompound(i);
            mean += btData.getFloat(btDataIndex);
        }
        return mean / n;
    }

    public SequenceData getBtDataList(int gridPointIndex) throws IOException {
        CompoundData gridPointEntry = gridPointList.getCompound(gridPointIndex);
        return gridPointEntry.getSequence(6);
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
