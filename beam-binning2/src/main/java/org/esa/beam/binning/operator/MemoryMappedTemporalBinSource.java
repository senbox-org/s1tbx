/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator;

import org.esa.beam.binning.TemporalBin;
import org.esa.beam.binning.TemporalBinSource;

import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.List;

/**
 * @author Thomas Storm
 */
class MemoryMappedTemporalBinSource implements TemporalBinSource {

    private static final int MB = 1024 * 1024;

    private final File file;
    private final TemporalBinIterator temporalBinIterator;

    private MappedByteBuffer readBuffer;
    private RandomAccessFile readRaf;

    MemoryMappedTemporalBinSource(List<TemporalBin> temporalBins) throws IOException {
        RandomAccessFile raf = null;
        MappedByteBuffer buffer = null;
        try {
            file = File.createTempFile(getClass().getSimpleName() + "-", ".dat");
            file.deleteOnExit();
            raf = new RandomAccessFile(file, "rw");
            FileChannel channel = raf.getChannel();
            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 100L * MB);
            temporalBinIterator = new TemporalBinIterator();
            for (TemporalBin temporalBin : temporalBins) {
                buffer.putLong(temporalBin.getIndex());
                buffer.putInt(temporalBin.getNumObs());
                buffer.putInt(temporalBin.getNumPasses());
                buffer.putInt(temporalBin.getFeatureValues().length);
                for (float v : temporalBin.getFeatureValues()) {
                    buffer.putFloat(v);
                }
            }
            buffer.putLong(-1L);
        } finally {
            MemoryMappedFileCleaner.cleanup(raf, buffer);
        }
    }

    @Override
    public int open() throws IOException {
        readRaf = new RandomAccessFile(file, "r");
        FileChannel channel = readRaf.getChannel();
        long length = file.length();
        readBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, length);
        // todo - maybe use different part count
        return 1;
    }

    @Override
    public Iterator<? extends TemporalBin> getPart(int index) throws IOException {
        return temporalBinIterator;
    }

    @Override
    public void partProcessed(int index, Iterator<? extends TemporalBin> part) throws IOException {
    }

    @Override
    public void close() throws IOException {
        MemoryMappedFileCleaner.cleanup(readRaf, readBuffer);
    }

    private class TemporalBinIterator implements Iterator<TemporalBin> {

        private boolean isIndexComputed = false;
        private long index;

        @Override
        public boolean hasNext() {
            return getIndex() != -1L;
        }

        @Override
        public TemporalBin next() {
            final TemporalBin temporalBin;
            try {
                final DataInput bufferWrapper = new ByteBufferWrapper(readBuffer);
                temporalBin = TemporalBin.read(getIndex(), bufferWrapper);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            isIndexComputed = false;
            return temporalBin;
        }

        @Override
        public void remove() {
        }

        private long getIndex() {
            if (isIndexComputed) {
                return index;
            } else {
                isIndexComputed = true;
                index = readBuffer.getLong();
                return index;
            }
        }

    }

    private static class ByteBufferWrapper extends ObjectInputStream {

        private final ByteBuffer buffer;

        public ByteBufferWrapper(ByteBuffer buffer) throws IOException {
            this.buffer = buffer;
        }

        @Override
        public int readInt() throws IOException {
            return buffer.getInt();
        }

        @Override
        public float readFloat() throws IOException {
            return buffer.getFloat();
        }
    }

}
