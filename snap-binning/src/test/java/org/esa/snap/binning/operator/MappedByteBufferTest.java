/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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


import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import static org.junit.Assert.*;

/**
 * Tests runtime behaviour and performance of {@link FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)}.
 * May be used to store intermediate spatial bins.
 *
 * @author Norman Fomferra
 */
public class MappedByteBufferTest {

    interface FileIO {
        void write(File file, int n, Producer producer) throws IOException;

        int read(File file, Consumer consumer) throws IOException;
    }

    interface Producer {
        long createKey();

        float[] createSamples();
    }

    interface Consumer {
        void process(long key, float[] samples);
    }


    static final int MiB = 1024 * 1024;
    static final int N = 25000;

    File file;

    @Before
    public void setUp() throws Exception {
        file = genTestFile();
        file.deleteOnExit();
        deleteFile("setUp", file);
    }

    @After
    public void tearDown() throws Exception {
        deleteFile("tearDown", file);
    }

//    /*
//     * This - failing and therefore ignored - test documents a case in which unmapping
//     * does not work. A long is written into the buffer using an index; after that, cleanup fails
//     * with an instance of java.lang.Error. See testMemoryMappedFileIOWithCleaning below
//     * for a nearly identical, non-failing test which is able to delete the temporary file.
//     */
//    @Test
//    @Ignore
//    public void testMemoryMappedFileIOWithCleaning_Failing() throws Exception {
//        final int fileSize = 1024 * 1024 * 100;
//
//        final RandomAccessFile raf = new RandomAccessFile(file, "rw");
//        final FileChannel fc = raf.getChannel();
//        MappedByteBuffer buffer = null;
//        try {
//            buffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);
//            buffer.putDouble(1.2);
//            buffer.putFloat(3.4f);
//            buffer.putLong(fileSize - 8, 1L);
//        } finally {
//            MemoryMappedFileCleaner.cleanup(raf, buffer);
//        }
//
//        deleteFile("MappedByteBufferTest.testMemoryMappedFileIOWithCleaning");
//        assertFalse(file.exists());
//    }

//    @Test
//    public void testMemoryMappedFileIOWithCleaning() throws Exception {
//        final int fileSize = 1024 * 1024 * 100;
//
//        final RandomAccessFile raf = new RandomAccessFile(file, "rw");
//        final FileChannel fc = raf.getChannel();
//        MappedByteBuffer buffer = null;
//        try {
//            buffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);
//            buffer.putDouble(1.2);
//            buffer.putFloat(3.4f);
//            buffer.putLong(1L);
//        } finally {
//            MemoryMappedFileCleaner.cleanup(raf, buffer);
//        }
//
//        deleteFile("MappedByteBufferTest.testMemoryMappedFileIOWithCleaning");
//        assertFalse(file.exists());
//    }

    @Ignore("fails on tearDown()")
    @Test
    public void testThatMemoryMappedFileIODoesNotConsumeHeapSpace() throws Exception {
        final int fileSize = Integer.MAX_VALUE; // 2GB!
        final long mem1, mem2, mem3, mem4;

        final RandomAccessFile raf = new RandomAccessFile(file, "rw");
        final FileChannel fc = raf.getChannel();
        try {
            mem1 = getFreeMiB();
            final MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);
            mem2 = getFreeMiB();

            // Modify buffer, so that it must be written when channel is closed.
            buffer.putDouble(1.2);
            buffer.putFloat(3.4f);
            buffer.putLong(fileSize - 8, 123456789L);

        } finally {
            mem3 = getFreeMiB();
            raf.close();
            mem4 = getFreeMiB();
        }

        assertTrue(file.exists());
        assertEquals(fileSize, file.length());

        System.out.println("free mem before opening: " + mem1 + " MiB");
        System.out.println("free mem after opening:  " + mem2 + " MiB");
        System.out.println("free mem before closing: " + mem3 + " MiB");
        System.out.println("free mem after closing:  " + mem4 + " MiB");

        // If these memory checks fail, check if 1 MiB is still too fine grained
        assertEquals(mem2, mem1);
        assertEquals(mem3, mem1);
        assertEquals(mem4, mem1);

        // Now make sure we get the values back
        final DataInputStream stream = new DataInputStream(new FileInputStream(file));
        try {
            assertEquals(1.2, stream.readDouble(), 1e-10); // 8 bytes
            assertEquals(3.4, stream.readFloat(), 1e-5f);  // 4 bytes
            stream.skip(fileSize - (8 + 4 + 8));
            assertEquals(123456789L, stream.readLong());
        } finally {
            stream.close();
        }
    }

    @Ignore("fails on tearDown()")
    @Test
    public void testThatFileMappingsCanGrow() throws Exception {

        final int chunkSize = 100 * MiB;

        final RandomAccessFile raf1 = new RandomAccessFile(file, "rw");
        final FileChannel gc1 = raf1.getChannel();
        try {
            final MappedByteBuffer buffer1 = gc1.map(FileChannel.MapMode.READ_WRITE, 0, chunkSize);
            buffer1.putDouble(0, 0.111);
            buffer1.putDouble(chunkSize - 8, 1.222);
        } finally {
            raf1.close();
            assertEquals(chunkSize, file.length());
        }

        final RandomAccessFile raf2 = new RandomAccessFile(file, "rw");
        final FileChannel fc2 = raf2.getChannel();
        try {
            final MappedByteBuffer buffer2 = fc2.map(FileChannel.MapMode.READ_WRITE, 0, 2 * chunkSize);
            assertEquals(0.111, buffer2.getDouble(0), 1e-10);
            assertEquals(1.222, buffer2.getDouble(chunkSize - 8), 1e-10);
            buffer2.putDouble(2 * chunkSize - 8, 2.333);
        } finally {
            raf2.close();
            assertEquals(2 * chunkSize, file.length());
        }

        final RandomAccessFile raf3 = new RandomAccessFile(file, "rw");
        final FileChannel fc3 = raf3.getChannel();
        try {
            final MappedByteBuffer buffer3 = fc3.map(FileChannel.MapMode.READ_WRITE, 0, 3 * chunkSize);
            assertEquals(0.111, buffer3.getDouble(0), 1e-10);
            assertEquals(1.222, buffer3.getDouble(chunkSize - 8), 1e-10);
            assertEquals(2.333, buffer3.getDouble(2 * chunkSize - 8), 1e-10);
            buffer3.putDouble(3 * chunkSize - 8, 3.444);
        } finally {
            fc3.close();
            raf3.close();
            assertEquals(3 * chunkSize, file.length());
        }

        final RandomAccessFile raf4 = new RandomAccessFile(file, "rw");
        final FileChannel fc4 = raf4.getChannel();
        try {
            final MappedByteBuffer buffer4 = fc4.map(FileChannel.MapMode.READ_WRITE, 0, 3 * chunkSize);
            assertEquals(0.111, buffer4.getDouble(0), 1e-10);
            assertEquals(1.222, buffer4.getDouble(chunkSize - 8), 1e-10);
            assertEquals(2.333, buffer4.getDouble(2 * chunkSize - 8), 1e-10);
            assertEquals(3.444, buffer4.getDouble(3 * chunkSize - 8), 1e-10);
        } finally {
            raf4.close();
            assertEquals(3 * chunkSize, file.length());
        }
    }

    @Test
    public void testStreamedFileIOPerformance() throws Exception {
        testFileIOPerformance(new StreamedFileIO());
    }

    @Ignore("fails on tearDown()")
    @Test
    public void testMemoryMappedFileIOPerformance() throws Exception {
        testFileIOPerformance(new MemoryMappedFileIO());
    }

    private void testFileIOPerformance(FileIO fileIO) throws IOException {

        System.out.println("Testing " + fileIO.getClass().getSimpleName() + " for " + N + " samples");

        MyProducer producer = new MyProducer();
        MyConsumer consumer = new MyConsumer();

        long t1 = System.currentTimeMillis();
        fileIO.write(file, N, producer);
        long t2 = System.currentTimeMillis();
        fileIO.read(file, consumer);
        long t3 = System.currentTimeMillis();

        assertEquals(N, producer.n);
        assertEquals(N, consumer.n);

        System.out.println("buf write time: " + (t2 - t1) + " ms");
        System.out.println("buf read time:  " + (t3 - t2) + " ms");
        System.out.println("buf total time: " + (t3 - t1) + " ms");
        System.out.println("file size:      " + file.length() + " bytes");
    }

    class MemoryMappedFileIO implements FileIO {
        @Override
        public void write(File file, int n, Producer producer) throws IOException {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel channel = raf.getChannel();
            ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 100L * MiB);
            try {
                for (int i = 0; i < n; i++) {
                    long key = producer.createKey();
                    float[] samples = producer.createSamples();
                    writeKey(buffer, key);
                    writeSamples(buffer, samples);
                }
            } finally {
                writeKey(buffer, -1L);
                raf.close();
            }
        }

        @Override
        public int read(File file, Consumer consumer) throws IOException {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            FileChannel channel = raf.getChannel();
            long length = file.length();
            ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, length);
            int n = 0;
            try {
                while (true) {
                    long key = readKey(buffer);
                    if (key == -1L) {
                        break;
                    }
                    float[] samples = readSamples(buffer);
                    consumer.process(key, samples);
                    n++;
                }
            } finally {
                raf.close();
            }
            return n;
        }

        private long readKey(ByteBuffer is) throws IOException {
            return is.getLong();
        }

        private float[] readSamples(ByteBuffer is) throws IOException {
            int n = is.getInt();
            float[] samples = new float[n];
            for (int i = 0; i < samples.length; i++) {
                samples[i] = is.getFloat();
            }
            return samples;
        }

        private void writeKey(ByteBuffer stream, long key) throws IOException {
            stream.putLong(key);
        }

        private void writeSamples(ByteBuffer stream, float[] samples) throws IOException {
            stream.putInt(samples.length);
            for (float sample : samples) {
                stream.putFloat(sample);
            }
        }


    }

    class StreamedFileIO implements FileIO {
        @Override
        public void write(File file, int n, Producer producer) throws IOException {
            DataOutputStream stream = new DataOutputStream(new FileOutputStream(file));
            try {
                for (int i = 0; i < n; i++) {
                    long key = producer.createKey();
                    float[] samples = producer.createSamples();
                    writeKey(stream, key);
                    writeSamples(stream, samples);
                }
            } finally {
                stream.close();
            }
        }

        @Override
        public int read(File file, Consumer consumer) throws IOException {
            int n = 0;
            DataInputStream stream = new DataInputStream(new FileInputStream(file));
            try {
                while (true) {
                    long key;
                    try {
                        key = readKey(stream);
                    } catch (EOFException eof) {
                        break;
                    }
                    float[] samples = readSamples(stream);
                    consumer.process(key, samples);
                    n++;
                }
            } finally {
                stream.close();
            }
            return n;
        }

        private long readKey(DataInputStream is) throws IOException {
            return is.readLong();
        }

        private float[] readSamples(DataInputStream is) throws IOException {
            int n = is.readInt();
            float[] samples = new float[n];
            for (int i = 0; i < samples.length; i++) {
                samples[i] = is.readFloat();
            }
            return samples;
        }

        private void writeKey(DataOutputStream stream, long key) throws IOException {
            stream.writeLong(key);
        }

        private void writeSamples(DataOutputStream stream, float[] samples) throws IOException {
            stream.writeInt(samples.length);
            for (float sample : samples) {
                stream.writeFloat(sample);
            }
        }
    }

    /**
     * Sample producer which creates randomly-sized sample arrays (length: 0 ... 10).
     */
    private static class MyProducer implements Producer {
        long n;

        @Override
        public long createKey() {
            return n++;
        }

        @Override
        public float[] createSamples() {
            final float[] samples = new float[(int) (Math.random() * 11)];
            for (int i = 0; i < samples.length; i++) {
                samples[i] = 0.1f * i;
            }
            return samples;
        }
    }

    /**
     * Sample consumer which simply counts received sample arrays.
     */
    private static class MyConsumer implements Consumer {
        long n;

        @Override
        public void process(long key, float[] samples) {
            n++;
        }
    }

    private static long getFreeMiB() {
        return Runtime.getRuntime().freeMemory() / MiB;
    }

    public static File genTestFile() throws IOException {
        return File.createTempFile(MappedByteBufferTest.class.getSimpleName() + "-", ".dat");
    }

    public static void deleteFile(String msg, File file) throws InterruptedException {
        if (file.exists()) {
            if (!file.delete()) {
                fail("error: " + msg + ": failed to delete test file " + file);
            }
        }
    }
}
