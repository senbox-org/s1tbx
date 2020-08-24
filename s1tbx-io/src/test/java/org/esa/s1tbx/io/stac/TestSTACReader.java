/*
 * Copyright (C) 2020 Skywatch Space Applications Inc. https://www.skywatch.com
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
package org.esa.s1tbx.io.stac;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestSTACReader extends ReaderTest {

    public TestSTACReader() {
        super(new STACProductReaderPlugIn());
    }

    @Test
    public void testReadCapellaProduct() throws Exception {
        URL resource = getClass().getResource("capella/CAPELLA_ARL_SP_GEO_VV_20190927234024_20190927234124.json");
        assertNotNull(resource);
        Path path = Paths.get(resource.toURI());

        final DecodeQualification canRead = readerPlugIn.getDecodeQualification(path);
        assertEquals(DecodeQualification.SUITABLE, canRead);

        ProductReader reader = readerPlugIn.createReaderInstance();
        Product srcProduct = reader.readProductNodes(path, null);
        assertNotNull(srcProduct);

    }
}