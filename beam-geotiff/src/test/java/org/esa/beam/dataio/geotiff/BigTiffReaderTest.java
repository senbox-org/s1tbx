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

package org.esa.beam.dataio.geotiff;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFRenderedImage;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import junit.framework.TestCase;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.Iterator;


/**
 * TiffHeader Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>02/11/2005</pre>
 */

public class BigTiffReaderTest extends TestCase {

    @Override
    public void setUp() throws Exception {
        //TestUtils.initTestEnvironment();
    }

    @Override
    public void tearDown() throws Exception {
    }

    @Test
    public void testReadImageFile() throws IOException {

        //String filePath = "\\\\fileserver\\Projects\\s1tbx\\s1tbx\\Data\\InSAR\\NapaValley\\S1A_S1_SLC__1SSV_20140807T142342_20140807T142411_001835_001BC1_05AA.SAFE\\measurement\\";
        //String fileName = "s1a-s1-slc-vv-20140807t142342-20140807t142411-001835-001bc1-001.tiff";
        String filePath = "C:\\beam\\";
        String fileName = "temp.tif";
        FileInputStream inputStream = new FileInputStream(filePath + fileName);
        //ByteArraySeekableStream inputStream = new ByteArraySeekableStream(outputStream.toByteArray());
        final MemoryCacheImageInputStream imageStream = new MemoryCacheImageInputStream(inputStream);
        Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageStream);
        TIFFImageReader imageReader = null;
        while(imageReaders.hasNext()) {
            final ImageReader nextReader = imageReaders.next();
            if (nextReader instanceof TIFFImageReader) {
                imageReader = (TIFFImageReader) nextReader;
            }
        }
        if (imageReader == null) {
            throw new IllegalStateException("No TIFFImageReader found");
        }

        imageReader.setInput(imageStream);
        //assertEquals(1, imageReader.getNumImages(true));

        final ImageReadParam readParam = imageReader.getDefaultReadParam();
        TIFFRenderedImage image = (TIFFRenderedImage) imageReader.readAsRenderedImage(0, readParam);
        //System.out.println("image " + image);
        //assertEquals(1, image.getSampleModel().getNumBands());
        inputStream.close();
    }
}
