/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.dataio.arcbin;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import static com.bc.ceres.binio.TypeBuilder.*;

/**
 * Contains the Tile Index
 */
class TileIndex {

    static final String FILE_NAME = "w001001x.adf";

    private final Map<Integer, IndexEntry> tileIndex;

    private TileIndex(Map<Integer, IndexEntry> index) {
        this.tileIndex = index;
    }

    IndexEntry getIndexEntry(int index) {
        return tileIndex.get(index);
    }

    static TileIndex create(File file, int numTiles) throws IOException {
        DataFormat dataFormat = new DataFormat(createType(numTiles), ByteOrder.BIG_ENDIAN);
        DataContext context = dataFormat.createContext(file, "r");
        CompoundData data = context.createData();

        Map<Integer, IndexEntry> index = new HashMap<Integer, IndexEntry>(numTiles);
        SequenceData indexSeq = data.getSequence("Indices");
        for (int i = 0; i < numTiles; i++) {
            CompoundData indexData = indexSeq.getCompound(i);
            int size = indexData.getInt(1);
            int offset = indexData.getInt(0);
            IndexEntry indexEntry = new IndexEntry(offset * 2, size * 2);
            index.put(i, indexEntry);
        }
        context.dispose();
        return new TileIndex(index);
    }

    private static CompoundType createType(int numtiles) {
        return COMPOUND("Header",
                        MEMBER("Magic", SEQUENCE(BYTE, 8)),
                        MEMBER("ZeroFill1", SEQUENCE(BYTE, 16)),
                        MEMBER("TFileSize", INT),
                        MEMBER("ZeroFill2", SEQUENCE(BYTE, 72)),
                        MEMBER("Indices", SEQUENCE(COMPOUND("Entry",
                                                            MEMBER("Offset", INT),
                                                            MEMBER("Size", INT)), numtiles))
        );
    }

    static class IndexEntry {

        final int offset;
        final int size;

        IndexEntry(int offset, int size) {
            this.offset = offset;
            this.size = size;
        }
    }
}
