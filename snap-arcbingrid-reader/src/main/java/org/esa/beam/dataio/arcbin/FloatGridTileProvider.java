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

import com.bc.ceres.binio.util.ByteArrayCodec;
import org.esa.beam.dataio.arcbin.TileIndex.IndexEntry;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.image.DataBuffer;
import java.nio.ByteOrder;


class FloatGridTileProvider implements GridTileProvider {

    private static final ByteArrayCodec byteArrayCodec = ByteArrayCodec.getInstance(ByteOrder.BIG_ENDIAN);

    private final RasterDataFile rasterDataFile;
    private final TileIndex tileIndex;
    private final float nodataValue;
    private final int size;
    private final int productDataType;


    FloatGridTileProvider(RasterDataFile rasterDataFile, TileIndex tileIndex, float nodataValue, int size,
                          int productDataType) {
        this.rasterDataFile = rasterDataFile;
        this.tileIndex = tileIndex;
        this.nodataValue = nodataValue;
        this.size = size;
        this.productDataType = productDataType;
    }

    @Override
    public ProductData getData(int currentTileIndex) {
        ProductData data = ProductData.createInstance(productDataType, size);
        IndexEntry indexEntry = tileIndex.getIndexEntry(currentTileIndex);
        if (indexEntry == null) {
            fillBuffer(data, nodataValue);
        } else {
            try {
                byte[] rawTileData = rasterDataFile.loadRawTileData(indexEntry);
                int tileOffset = 2;
                for (int i = 0; i < size; i++) {
                    float value = byteArrayCodec.getFloat(rawTileData, tileOffset);
                    tileOffset += 4;
                    data.setElemFloatAt(i, value);
                }
            } catch (Exception ignored) {
                fillBuffer(data, nodataValue);
            }
        }
        return data;
    }

    @Override
    public void transferData(ProductData data, int sourceIndex, DataBuffer dataBuffer, int targetIndex) {
        float value = data.getElemFloatAt(sourceIndex);
        dataBuffer.setElemFloat(targetIndex, value);
    }

    private void fillBuffer(ProductData data, float value) {
        for (int i = 0; i < size; i++) {
            data.setElemFloatAt(i, value);
        }
    }
}
