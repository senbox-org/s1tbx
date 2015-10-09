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
package org.esa.snap.core.dataio;

import junit.framework.TestCase;
import org.esa.snap.GlobalTestConfig;
import org.esa.snap.GlobalTestTools;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class ImageInputAndOutputStreamTest extends TestCase {

    private File _testFile;
    private final int _lineLength = 4;
    private final int _numLines = 5;
    private final int elemsize = 4;

    @Override
    protected void setUp() throws Exception {
        deleteOutput();
        final File outputDirectory = GlobalTestConfig.getBeamTestDataOutputDirectory();
        outputDirectory.mkdirs();

        _testFile = new File(outputDirectory, "testData");

        final float[] floats = new float[_numLines * _lineLength];
        final Random random = new Random();
        for (int i = 0; i < floats.length; i++) {
            floats[i] = random.nextFloat();
        }

        final FileImageOutputStream imageOutputStream = new FileImageOutputStream(_testFile);
        imageOutputStream.writeFloats(floats, 0, floats.length);
        imageOutputStream.close();
    }

    @Override
    protected void tearDown() throws Exception {
        deleteOutput();
    }

    public void testFileImageIOStreams() {
        final float[] outputLineBuffer = new float[_lineLength];
        for (int i = 0; i < outputLineBuffer.length; i++) {
            outputLineBuffer[i] = i + 12;
        }
        final float[] inputLineBuffer = new float[_lineLength];
        try {
            final FileImageInputStream inputStream = new FileImageInputStream(_testFile);
            final FileImageOutputStream outputStream = new FileImageOutputStream(_testFile);
            final int byteLineSize = _lineLength * elemsize;
            for (int offset = 0; offset < byteLineSize * _numLines; offset += byteLineSize) {
                inputStream.seek(offset);
                inputStream.readFully(inputLineBuffer, 0, _lineLength);
                outputStream.seek(offset);
                outputStream.writeFloats(outputLineBuffer, 0, _lineLength);
            }
            inputStream.close();
            outputStream.close();

            final FileImageInputStream verifyInputStream = new FileImageInputStream(_testFile);
            for (int offset = 0; offset < byteLineSize * _numLines; offset += byteLineSize) {
                verifyInputStream.seek(offset);
                verifyInputStream.readFully(inputLineBuffer, 0, _lineLength);
                assertEquals(true, Arrays.equals(inputLineBuffer, outputLineBuffer));
            }
            verifyInputStream.close();
        } catch (IOException e) {
            fail("IOException not expected");
        }
    }

    private void deleteOutput() {
        GlobalTestTools.deleteTestDataOutputDirectory();
    }
}
