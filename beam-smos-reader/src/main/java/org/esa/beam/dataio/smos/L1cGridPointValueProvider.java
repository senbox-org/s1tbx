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

public class L1cGridPointValueProvider implements GridPointValueProvider {
    private final L1cSmosFile smosFile;
    private final int fieldIndex;
    private final int polarization;
    private volatile int snapshotId;

    public L1cGridPointValueProvider(L1cSmosFile smosFile, int fieldIndex, int polarization) {
        this.smosFile = smosFile;
        this.fieldIndex = fieldIndex;
        this.polarization = polarization;
        this.snapshotId = -1;
    }

    public int getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(int snapshotId) {
        this.snapshotId = snapshotId;
    }

    @Override
    public int getGridPointIndex(int seqnum) {
        return smosFile.getGridPointIndex(seqnum);
    }

    @Override
    public short getValue(int gridPointIndex, short noDataValue) {
        try {
            if (snapshotId == -1) {
                return smosFile.getBrowseBtData(gridPointIndex, fieldIndex, polarization, noDataValue);
            } else {
                return smosFile.getSnapshotBtData(gridPointIndex, fieldIndex, polarization, snapshotId, noDataValue);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getValue(int gridPointIndex, int noDataValue) {
        try {
            if (snapshotId == -1) {
                return smosFile.getBrowseBtData(gridPointIndex, fieldIndex, polarization, noDataValue);
            } else {
                return smosFile.getSnapshotBtData(gridPointIndex, fieldIndex, polarization, snapshotId, noDataValue);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public float getValue(int gridPointIndex, float noDataValue) {
        try {
            if (snapshotId == -1) {
                return smosFile.getBrowseBtData(gridPointIndex, fieldIndex, polarization, noDataValue);
            } else {
                return smosFile.getSnapshotBtData(gridPointIndex, fieldIndex, polarization, snapshotId, noDataValue);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
