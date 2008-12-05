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

class L1cGridPointValueProvider implements GridPointValueProvider {
    private final L1cSmosFile smosFile;
    private final int fieldIndex;
    private final int polarization;

    L1cGridPointValueProvider(L1cSmosFile smosFile, int fieldIndex, int polarization) {
        this.smosFile = smosFile;
        this.fieldIndex = fieldIndex;
        this.polarization = polarization;
    }

    @Override
    public int getGridPointIndex(int seqnum) {
        return smosFile.getGridPointIndex(seqnum);
    }

    @Override
    public short getValue(int gridPointIndex, short noDataValue) {
        try {
            return smosFile.getBrowseBtData(gridPointIndex, fieldIndex, polarization, noDataValue);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getValue(int gridPointIndex, int noDataValue) {
        try {
            return smosFile.getBrowseBtData(gridPointIndex, fieldIndex, polarization, noDataValue);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public float getValue(int gridPointIndex, float noDataValue) {
        try {
            return smosFile.getBrowseBtData(gridPointIndex, fieldIndex, polarization, noDataValue);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
