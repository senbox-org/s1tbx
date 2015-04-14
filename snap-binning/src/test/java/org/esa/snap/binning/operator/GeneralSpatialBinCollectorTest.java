/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning.operator;

import org.esa.snap.binning.BinningContext;
import org.esa.snap.binning.SpatialBin;
import org.esa.snap.binning.support.SEAGrid;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class GeneralSpatialBinCollectorTest {

    @Test
    public void testConsumeSpatialBins() throws Exception {

        final SEAGrid seaGrid = new SEAGrid(10);
        GeneralSpatialBinCollector store = new GeneralSpatialBinCollector(seaGrid.getNumBins());
        try {
            BinningContext ctx = Mockito.mock(BinningContext.class);
            Mockito.when(ctx.getPlanetaryGrid()).thenReturn(seaGrid);

            ArrayList<SpatialBin> spatialBins = new ArrayList<SpatialBin>();
            spatialBins.add(createSpatialBin(23));
            store.consumeSpatialBins(ctx, spatialBins);
            store.consumingCompleted();
            SpatialBinCollection binMap = store.getSpatialBinCollection();
            Iterable<List<SpatialBin>> actualBinLists = binMap.getBinCollection();
            List<SpatialBin> binList = actualBinLists.iterator().next();
            assertEquals(23, binList.get(0).getIndex());
        } finally {
            store.close();
        }
    }

    private SpatialBin createSpatialBin(long binIndex) {
        return new SpatialBin(binIndex, 2);
    }
}
