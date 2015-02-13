/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.binning.reader;

import org.esa.beam.framework.datamodel.Band;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class SparseGridAccessor extends AbstractGridAccessor {

    private final NetcdfFile netcdfFile;
    private final int[] binIndexes;
    private final int[] binOffsets;
    private final int[] binExtents;

    /**
     * Key: BinIndex in PlanetaryGrid
     * Value: BinIndex in bin_list
     */
    private final Map<Integer, Integer> indexMap;

    SparseGridAccessor(NetcdfFile netcdfFile) throws IOException {
        this.netcdfFile = netcdfFile;

        final Variable bl_bin_num = netcdfFile.findVariable("bl_bin_num");
        final Variable bi_begin = netcdfFile.findVariable("bi_begin");
        final Variable bi_extent = netcdfFile.findVariable("bi_extent");

        synchronized (this.netcdfFile) {
            final Object storage = bl_bin_num.read().getStorage();

            binIndexes = (int[]) storage;
            indexMap = new HashMap<Integer, Integer>(binIndexes.length);
            for (int i = 0; i < binIndexes.length; i++) {
                indexMap.put(binIndexes[i], i);
            }
            binOffsets = (int[]) bi_begin.read().getStorage();
            binExtents = (int[]) bi_extent.read().getStorage();
        }
    }

    @Override
    void dispose() {
        indexMap.clear();
    }

    @Override
    Array getLineValues(Band destBand, VariableReader variableReader, int lineIndex) throws IOException {
        Array lineValues = null;
        final int binOffset = binOffsets[lineIndex];
        if (binOffset > 0) {
            int firstIndex = indexMap.get(binOffset);
            int length = binExtents[lineIndex];
            synchronized (netcdfFile) {
                lineValues = variableReader.read(firstIndex, length);
            }
        }
        return lineValues;
    }

    @Override
    int getStartBinIndex(int sourceOffsetX, int lineIndex) {
        return 0;
    }

    @Override
    int getEndBinIndex(int sourceOffsetX, int sourceWidth, int lineIndex) {
        return binExtents[lineIndex];
    }

    @Override
    int getBinIndexInGrid(int binIndex, int lineIndex) {
        final int binOffset = binOffsets[lineIndex];
        return binIndexes[indexMap.get(binOffset) + binIndex];
    }
}
