/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.imageio;

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;

/**
 * Test ERS CEOS Product Reader.
 *
 * @author lveci
 */
public class TestImageIOReader {

    static {
        TestUtils.initTestEnvironment();
    }
    ImageIOReaderPlugIn readerPlugin;
    ProductReader reader;

    String filePath = "P:\\nest\\nest\\ESA Data\\Other\\Imagefiles\\Submarine_operators_countries.png";

    public TestImageIOReader() {
        readerPlugin = new ImageIOReaderPlugIn();
        reader = readerPlugin.createReaderInstance();
    }

    @Test
    public void testImageIO() throws IOException {
        final String[] readerFormats = ImageIO.getReaderFormatNames();
        final String[] readerSuffixes = ImageIO.getReaderFileSuffixes();
        final String[] writerFormats = ImageIO.getWriterFormatNames();
        final String[] writerSuffixes = ImageIO.getWriterFileSuffixes();

        for (String s : readerFormats)
            TestUtils.log.info("ImageIOreader: " + s);
        for (String s : readerSuffixes)
            TestUtils.log.info("ImageIOreaderSuffix: " + s);
        for (String s : writerFormats)
            TestUtils.log.info("ImageIOwriter: " + s);
        for (String s : writerSuffixes)
            TestUtils.log.info("ImageIOwriterSuffix: " + s);
    }

    @Test
    public void testOpen() throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            TestUtils.skipTest(this, filePath+" not found");
            return;
        }

        Product product = reader.readProductNodes(file, null);
        assertNotNull(product);
    }

}