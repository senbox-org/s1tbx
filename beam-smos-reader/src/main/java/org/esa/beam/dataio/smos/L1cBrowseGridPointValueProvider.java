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

import java.io.IOException;
import java.text.MessageFormat;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class L1cBrowseGridPointValueProvider implements GridPointValueProvider {
    private final GridPointDataProvider provider;
    private final int fieldIndex;
    private final int mode;

    private final int btDataListIndex;
    private final int flagsIndex;

    public L1cBrowseGridPointValueProvider(GridPointDataProvider provider, int fieldIndex, int polarization) {
        this.provider = provider;
        this.fieldIndex = fieldIndex;
        this.mode = polarization;

        btDataListIndex = provider.getGridPointType().getMemberIndex("BT_Data_List");
        if (btDataListIndex == -1) {
            throw new IllegalArgumentException("Grid point type not include BT data list.");
        }

        final Type memberType = provider.getGridPointType().getMemberType(btDataListIndex);
        if (!memberType.isSequenceType()) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Data type ''{0}'' is not of appropriate type", memberType.getName()));
        }

        final Type elementType = ((SequenceType) memberType).getElementType();
        if (!elementType.isCompoundType()) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Data type ''{0}'' is not of appropriate type", elementType.getName()));
        }

        flagsIndex = ((CompoundType) elementType).getMemberIndex("Flags");
    }

    @Override
    public int getGridPointIndex(int seqnum) {
        return provider.getGridPointIndex(seqnum);
    }

    @Override
    public short getValue(int gridPointIndex, short noDataValue) {
        try {
            final CompoundData data = getParentCompound(gridPointIndex);
            if (data == null) {
                return noDataValue;
            }
            return data.getShort(fieldIndex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getValue(int gridPointIndex, int noDataValue) {
        try {
            final CompoundData data = getParentCompound(gridPointIndex);
            if (data == null) {
                return noDataValue;
            }
            return data.getInt(fieldIndex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public float getValue(int gridPointIndex, float noDataValue) {
        try {
            final CompoundData data = getParentCompound(gridPointIndex);
            if (data == null) {
                return noDataValue;
            }
            return data.getFloat(fieldIndex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private CompoundData getParentCompound(int gridPointIndex) throws IOException {
        final SequenceData btDataList = provider.getGridPointData(gridPointIndex).getSequence(btDataListIndex);
        final int elementCount = btDataList.getSequenceType().getElementCount();

        for (int i = 0; i < elementCount; ++i) {
            final CompoundData data = btDataList.getCompound(i);
            if (mode == (data.getInt(flagsIndex) & 3)) {
                return data;
            }
        }

        return null;
    }
}
