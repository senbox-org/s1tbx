/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.commons.test;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.util.TestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReaderTest extends ProcessorTest {

    protected final ProductReaderPlugIn readerPlugIn;
    protected final ProductReader reader;
    protected boolean verifyTime = true;
    protected boolean verifyGeoCoding = true;

    public ReaderTest(final ProductReaderPlugIn readerPlugIn) {
        this.readerPlugIn = readerPlugIn;
        this.reader = readerPlugIn.createReaderInstance();
    }

    protected void close() throws IOException {
        reader.close();
    }

    protected Product testReader(final Path inputPath) throws Exception {
        return testReader(inputPath, readerPlugIn);
    }

    protected Product testReader(final Path inputPath, final ProductReaderPlugIn readerPlugIn) throws Exception {
        if (!Files.exists(inputPath)) {
            TestUtils.skipTest(this, inputPath + " not found");
            return null;
        }

        final DecodeQualification canRead = readerPlugIn.getDecodeQualification(inputPath);
        if (canRead != DecodeQualification.INTENDED) {
            throw new Exception("Reader not intended");
        }

        final ProductReader reader = readerPlugIn.createReaderInstance();
        final Product product = reader.readProductNodes(inputPath, null);
        if (product == null) {
            throw new Exception("Unable to read product");
        }

        return product;
    }
}
