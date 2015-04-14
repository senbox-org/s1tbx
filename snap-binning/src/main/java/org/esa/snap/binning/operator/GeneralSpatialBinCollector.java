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

import java.io.IOException;
import java.util.List;

/**
 * An implementation of {@link SpatialBinCollector} which combines a {@link MapBackedSpatialBinCollector}
 * and a {@link FileBackedSpatialBinCollector} to one spatial bin collector.
 * This means that all data is kept in memory. There are other implementations which consume less memory.
 *
 * @see MapBackedSpatialBinCollector
 * @see FileBackedSpatialBinCollector
 */
public class GeneralSpatialBinCollector implements SpatialBinCollector {

    private final FileBackedSpatialBinCollector fileBinCollector;
    private MapBackedSpatialBinCollector mapBinCollector;
    private boolean consumingCompleted;


    public GeneralSpatialBinCollector(long numBins) throws IOException {
        fileBinCollector = new FileBackedSpatialBinCollector(numBins);
        mapBinCollector = new MapBackedSpatialBinCollector();
        consumingCompleted = false;
    }

    @Override
    public void consumeSpatialBins(BinningContext ctx, List<SpatialBin> spatialBins) throws Exception {
        if (consumingCompleted) {
            throw new IllegalStateException("Consuming of bins has already been completed.");
        }
        mapBinCollector.consumeSpatialBins(ctx, spatialBins);
        if (mapBinCollector.getSpatialBinCollection().size() > 12000) {
            moveBinsToFile(ctx);
        }
    }

    @Override
    public SpatialBinCollection getSpatialBinCollection() throws IOException {
        return fileBinCollector.getSpatialBinCollection();
    }

    @Override
    public void consumingCompleted() throws IOException {
        consumingCompleted = true;
        moveBinsToFile(null);
        mapBinCollector.consumingCompleted();
        mapBinCollector.close();
        mapBinCollector = null;
        fileBinCollector.consumingCompleted();
    }

    @Override
    public void close() throws IOException {
        if (fileBinCollector != null) {
            fileBinCollector.close();
        }
        if (mapBinCollector != null) {
            mapBinCollector.close();
        }
    }

    private void moveBinsToFile(BinningContext ignored) throws IOException {
        SpatialBinCollection spatialBinCollection = mapBinCollector.getSpatialBinCollection();

        Iterable<List<SpatialBin>> values = spatialBinCollection.getBinCollection();
        for (List<SpatialBin> value : values) {
            try {
                fileBinCollector.consumeSpatialBins(ignored, value);
            } catch (Exception e) {
                throw new IOException(e);
            }

        }
        mapBinCollector.clearMap();
    }
}
