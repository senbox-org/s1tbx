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

import java.io.IOException;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.5
 */
public  class L2GridPointValueProvider implements GridPointValueProvider {

    private final GridPointDataProvider provider;
    private final int fieldIndex;

    protected L2GridPointValueProvider(GridPointDataProvider provider, int fieldIndex) {
        this.provider = provider;
        this.fieldIndex = fieldIndex;
    }

    @Override
    public final int getGridPointIndex(int seqnum) {
        return provider.getGridPointIndex(seqnum);
    }

    @Override
    public short getValue(int gridPointIndex, short noDataValue) {
        try {
            return getParentCompound(gridPointIndex).getShort(fieldIndex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getValue(int gridPointIndex, int noDataValue) {
        try {
            return getParentCompound(gridPointIndex).getInt(fieldIndex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public float getValue(int gridPointIndex, float noDataValue) {
        try {
            return getParentCompound(gridPointIndex).getFloat(fieldIndex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private CompoundData getParentCompound(int gridPointIndex) throws IOException {
        return provider.getGridPointData(gridPointIndex).getCompound("Retrieval_Results_Data");
    }
}
