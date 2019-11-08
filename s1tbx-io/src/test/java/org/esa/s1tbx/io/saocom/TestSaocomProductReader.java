/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.saocom;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestSaocomProductReader extends ReaderTest {

    private final static File inputMetadataFile = new File("E:\\data\\SAOCOM\\EOL1CSARSAO1A185028\\10793-EOL1CSARSAO1A185028\\S1A_OPER_SAR_EOSSP__CORE_L1C_OLVF_20190801T145831.xemt");

    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        assumeTrue(inputMetadataFile + " not found", inputMetadataFile.exists());
    }

    public TestSaocomProductReader() {
        super(new SaocomProductReaderPlugIn());
    }

    @Test
    public void testReadMetadata() throws Exception {
        testReader(inputMetadataFile.toPath());
    }
}
