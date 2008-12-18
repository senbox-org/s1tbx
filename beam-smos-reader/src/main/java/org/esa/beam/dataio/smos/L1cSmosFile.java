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

/**
 * Abstract representation of a SMOS L1c product file.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public abstract class L1cSmosFile extends SmosFile {

    protected final int btDataListIndex;
    protected final CompoundType btDataType;

    public L1cSmosFile(File file, DataFormat format) throws IOException {
        super(file, format);

        btDataListIndex = getGridPointType().getMemberIndex(SmosFormats.BT_DATA_LIST_NAME);
        if (btDataListIndex == -1) {
            throw new IOException("Grid point type does not include BT data list.");
        }

        final Type memberType = getGridPointType().getMemberType(btDataListIndex);
        if (!memberType.isSequenceType()) {
            throw new IOException(MessageFormat.format(
                    "Data type ''{0}'' is not of appropriate type", memberType.getName()));
        }

        final Type elementType = ((SequenceType) memberType).getElementType();
        if (!elementType.isCompoundType()) {
            throw new IOException(MessageFormat.format(
                    "Data type ''{0}'' is not a compound type", elementType.getName()));
        }

        btDataType = (CompoundType) elementType;
    }

    public final CompoundType getBtDataType() {
        return btDataType;
    }

    public final SequenceData getBtDataList(int gridPointIndex) throws IOException {
        return getGridPointData(gridPointIndex).getSequence(btDataListIndex);
    }

    public abstract short getBrowseBtData(int gridPointIndex, int fieldIndex, int polMode,
                                          short noDataValue) throws IOException;

    public abstract int getBrowseBtData(int gridPointIndex, int fieldIndex, int polMode,
                                        int noDataValue) throws IOException;

    public abstract float getBrowseBtData(int gridPointIndex, int fieldIndex, int polMode,
                                          float noDataValue) throws IOException;

    public abstract short getSnapshotBtData(int gridPointIndex, int fieldIndex, int polMode,
                                           int snapshotId,short noDataValue) throws IOException;

    public abstract int getSnapshotBtData(int gridPointIndex, int fieldIndex, int polMode,
                                         int snapshotId,int noDataValue) throws IOException;

    public abstract float getSnapshotBtData(int gridPointIndex, int fieldIndex, int polMode,
                                           int snapshotId,float noDataValue) throws IOException;
}
