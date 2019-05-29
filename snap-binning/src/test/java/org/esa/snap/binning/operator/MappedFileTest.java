package org.esa.snap.binning.operator;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.nio.MappedByteBuffer;

import static org.junit.Assert.*;

/**
 * @author Norman
 */
public class MappedFileTest {

    int MiB = 1024 * 1024;

    File file;

    @Before
    public void setUp() throws Exception {
        file = MappedByteBufferTest.genTestFile();
        file.deleteOnExit();
        MappedByteBufferTest.deleteFile("setUp", file);
    }

    @After
    public void tearDown() throws Exception {
        MappedByteBufferTest.deleteFile("tearDown", file);
    }

    @Ignore("fails on tearDown()")
    @Test
    public void testRemap() throws Exception {

        final int chunkSize = 100 * MiB;

        MappedFile mappedFile = MappedFile.open(file, chunkSize);
        try {
            final MappedByteBuffer buffer1 = mappedFile.getBuffer();
            buffer1.putDouble(0, 0.111);
            buffer1.putDouble(chunkSize - 8, 1.222);
        } finally {
            assertEquals(chunkSize, file.length());
        }

        try {
            final MappedByteBuffer buffer2 = mappedFile.remap(0, 2 * chunkSize);
            assertEquals(0.111, buffer2.getDouble(0), 1e-10);
            assertEquals(1.222, buffer2.getDouble(chunkSize - 8), 1e-10);
            buffer2.putDouble(2 * chunkSize - 8, 2.333);
        } finally {
            assertEquals(2 * chunkSize, file.length());
        }

        try {
            final MappedByteBuffer buffer3 = mappedFile.remap(0, 3 * chunkSize);
            assertEquals(0.111, buffer3.getDouble(0), 1e-10);
            assertEquals(1.222, buffer3.getDouble(chunkSize - 8), 1e-10);
            assertEquals(2.333, buffer3.getDouble(2 * chunkSize - 8), 1e-10);
            buffer3.putDouble(3 * chunkSize - 8, 3.444);
        } finally {
            assertEquals(3 * chunkSize, file.length());
        }

        mappedFile.close();

        mappedFile = MappedFile.open(file, 3 * chunkSize);
        try {
            final MappedByteBuffer buffer4 = mappedFile.remap(0, 3 * chunkSize);
            assertEquals(0.111, buffer4.getDouble(0), 1e-10);
            assertEquals(1.222, buffer4.getDouble(chunkSize - 8), 1e-10);
            assertEquals(2.333, buffer4.getDouble(2 * chunkSize - 8), 1e-10);
            assertEquals(3.444, buffer4.getDouble(3 * chunkSize - 8), 1e-10);
        } finally {
            assertEquals(3 * chunkSize, file.length());
            mappedFile.close();
        }
    }
}
