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
import com.bc.ceres.binio.DataFormat;

import java.io.File;
import java.io.IOException;

/**
 * Represents a SMOS L1c Browse product file.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.5
 */
public class L1cBrowseSmosFile extends L1cSmosFile {

    public L1cBrowseSmosFile(File file, DataFormat format) throws IOException {
        super(file, format);
    }

    @Override
    public short getBrowseBtData(int gridPointIndex, int fieldIndex, int polMode,
                                 short noDataValue) throws IOException {
        return getBtData(gridPointIndex, polMode).getShort(fieldIndex);
    }

    @Override
    public int getBrowseBtData(int gridPointIndex, int fieldIndex, int polMode,
                               int noDataValue) throws IOException {
        return getBtData(gridPointIndex, polMode).getInt(fieldIndex);
    }

    @Override
    public float getBrowseBtData(int gridPointIndex, int fieldIndex, int polMode,
                                 float noDataValue) throws IOException {
        return getBtData(gridPointIndex, polMode).getFloat(fieldIndex);
    }

    @Override
    public short getSnapshotBtData(int gridPointIndex, int fieldIndex, int polMode,
                                   int snapshotId, short noDataValue) throws IOException {
        return getBtData(gridPointIndex, polMode).getShort(fieldIndex);
    }

    @Override
    public int getSnapshotBtData(int gridPointIndex, int fieldIndex, int polMode,
                                 int snapshotId, int noDataValue) throws IOException {
        return getBtData(gridPointIndex, polMode).getInt(fieldIndex);
    }

    @Override
    public float getSnapshotBtData(int gridPointIndex, int fieldIndex, int polMode,
                                   int snapshotId, float noDataValue) throws IOException {
        return getBtData(gridPointIndex, polMode).getFloat(fieldIndex);
    }

    private CompoundData getBtData(int gridPointIndex, int polMode) throws IOException {
        return getBtDataList(gridPointIndex).getCompound(polMode);
    }
}
