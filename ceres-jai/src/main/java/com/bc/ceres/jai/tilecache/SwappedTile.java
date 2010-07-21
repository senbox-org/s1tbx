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

package com.bc.ceres.jai.tilecache;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;


final class SwappedTile {
    private final File file;
    private final long fileSize;
    private final SampleModel sampleModel;
    private final boolean writable;
    private final Point location;
    private final Object tileCacheMetric;

    SwappedTile(MemoryTile mt, File cacheDir) {
        this.file = new File(cacheDir, mt.getKeyAsString());
        this.fileSize = mt.getTileSize();
        this.sampleModel = mt.getTile().getSampleModel();
        this.location = (Point) mt.getTile().getBounds().getLocation().clone();
        this.writable = mt.getTile() instanceof WritableRaster;
        this.tileCacheMetric = mt.getTileCacheMetric();
    }

    public boolean isAvailable() {
        return file.length() == fileSize;
    }

    public File getFile() {
        return file;
    }

    public SampleModel getSampleModel() {
        return sampleModel;
    }

    public boolean isWritable() {
        return writable;
    }

    public Point getLocation() {
        return location;
    }

    public Object getTileCacheMetric() {
        return tileCacheMetric;
    }

    public boolean delete() {
        return file.delete();
    }

    public Raster restoreTile() throws IOException {
        final ImageInputStream stream = new FileImageInputStream(file);
        final DataBuffer dataBuffer;
        try {
            dataBuffer = readTileData(stream, sampleModel);
        } finally {
            stream.close();
        }
        final Raster tile;
        if (writable) {
            tile = Raster.createWritableRaster(sampleModel, dataBuffer, location);
        } else {
            tile = Raster.createRaster(sampleModel, dataBuffer, location);
        }
        return tile;
    }

    public void storeTile(Raster tile) throws IOException {
        final ImageOutputStream stream = new FileImageOutputStream(file);
        try {
            writeTileData(stream, tile.getDataBuffer());
        } finally {
            stream.close();
        }
    }

    private static DataBuffer readTileData(ImageInputStream stream, SampleModel sampleModel) throws IOException {
        final int dataType = sampleModel.getDataType();
        final int arrayLength = stream.readInt();
        final int bufferSize = stream.readInt();
        final int bufferOffset = stream.readInt();
        if (dataType == DataBuffer.TYPE_BYTE) {
            byte[] data = new byte[arrayLength];
            stream.readFully(data, 0, arrayLength);
            return new DataBufferByte(data, bufferSize, bufferOffset);
        } else if (dataType == DataBuffer.TYPE_SHORT || dataType == DataBuffer.TYPE_USHORT) {
            short[] data = new short[arrayLength];
            stream.readFully(data, 0, arrayLength);
            return new DataBufferShort(data, bufferSize, bufferOffset);
        } else if (dataType == DataBuffer.TYPE_INT) {
            int[] data = new int[arrayLength];
            stream.readFully(data, 0, arrayLength);
            return new DataBufferInt(data, bufferSize, bufferOffset);
        } else if (dataType == DataBuffer.TYPE_FLOAT) {
            float[] data = new float[arrayLength];
            stream.readFully(data, 0, arrayLength);
            return new DataBufferFloat(data, bufferSize, bufferOffset);
        } else if (dataType == DataBuffer.TYPE_DOUBLE) {
            double[] data = new double[arrayLength];
            stream.readFully(data, 0, arrayLength);
            return new DataBufferDouble(data, bufferSize, bufferOffset);
        } else {
            throw new IllegalStateException();
        }
    }

    private static void writeTileData(ImageOutputStream stream, DataBuffer dataBuffer) throws IOException {
        final Object data;
        try {
            final Method method = dataBuffer.getClass().getMethod("getData");
            data = method.invoke(dataBuffer);
        } catch (Exception e) {
            throw new IllegalArgumentException("illegal dataBuffer: " + dataBuffer.getClass(), e);
        }
        if (data instanceof byte[]) {
            final byte[] array = (byte[]) data;
            stream.writeInt(array.length);
            stream.writeInt(dataBuffer.getSize());
            stream.writeInt(dataBuffer.getOffset());
            stream.write(array,
                         dataBuffer.getOffset(),
                         dataBuffer.getSize());
        } else if (data instanceof short[]) {
            final short[] array = (short[]) data;
            stream.writeInt(array.length);
            stream.writeInt(dataBuffer.getSize());
            stream.writeInt(dataBuffer.getOffset());
            stream.writeShorts(array,
                               dataBuffer.getOffset(),
                               dataBuffer.getSize());
        } else if (data instanceof int[]) {
            final int[] array = (int[]) data;
            stream.writeInt(array.length);
            stream.writeInt(dataBuffer.getSize());
            stream.writeInt(dataBuffer.getOffset());
            stream.writeInts(array,
                             dataBuffer.getOffset(),
                             dataBuffer.getSize());
        } else if (data instanceof float[]) {
            final float[] array = (float[]) data;
            stream.writeInt(array.length);
            stream.writeInt(dataBuffer.getSize());
            stream.writeInt(dataBuffer.getOffset());
            stream.writeFloats(array,
                               dataBuffer.getOffset(),
                               dataBuffer.getSize());
        } else if (data instanceof double[]) {
            final double[] array = (double[]) data;
            stream.writeInt(array.length);
            stream.writeInt(dataBuffer.getSize());
            stream.writeInt(dataBuffer.getOffset());
            stream.writeDoubles(array,
                                dataBuffer.getOffset(),
                                dataBuffer.getSize());
        } else {
            throw new IllegalArgumentException("illegal dataBuffer: " + dataBuffer.getClass());
        }
    }

}