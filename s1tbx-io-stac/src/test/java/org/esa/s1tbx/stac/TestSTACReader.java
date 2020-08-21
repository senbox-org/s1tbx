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
package org.esa.s1tbx.stac;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.snap.core.datamodel.Product;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

@Ignore
public class TestSTACReader extends ReaderTest {

    private final static File inputFolder = new File("C:\\out\\results\\SkyWatch\\3S");

    public TestSTACReader() {
        super(new STACProductReaderPlugIn());
    }

    @Test
    public void testReadProduct() throws Exception {
        Product srcProduct = testReader(inputFolder.toPath());

    }
}