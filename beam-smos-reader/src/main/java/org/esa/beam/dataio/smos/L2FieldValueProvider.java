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

import java.io.IOException;
import java.awt.geom.Area;

/**
 * Provides the value of a certain field in the grid point data record.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class L2FieldValueProvider implements GridPointValueProvider {

    private final GridPointDataProvider provider;
    private final int fieldIndex;

    protected L2FieldValueProvider(GridPointDataProvider provider, int fieldIndex) {
        this.provider = provider;
        this.fieldIndex = fieldIndex;
    }

    @Override
    public Area getRegion() {
        return provider.getRegion();
    }

    @Override
    public final int getGridPointIndex(int seqnum) {
        return provider.getGridPointIndex(seqnum);
    }

    @Override
    public short getValue(int gridPointIndex, short noDataValue) {
        try {
            return provider.getGridPointData(gridPointIndex).getShort(fieldIndex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getValue(int gridPointIndex, int noDataValue) {
        try {
            return provider.getGridPointData(gridPointIndex).getInt(fieldIndex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public float getValue(int gridPointIndex, float noDataValue) {
        try {
            return provider.getGridPointData(gridPointIndex).getFloat(fieldIndex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
