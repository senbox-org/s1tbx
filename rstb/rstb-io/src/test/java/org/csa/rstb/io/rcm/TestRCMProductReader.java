/*
 * Copyright (C) 2018 Skywatch. https://www.skywatch.co
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
package org.csa.rstb.io.rcm;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.junit.Test;

import java.io.File;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestRCMProductReader extends ReaderTest {

    private final static File inputGRDZip = new File("E:\\Data\\RCM\\RCM1_OK77686_PK688502_4_SC4D_20130822_020936_HH_GRD.zip");
    private final static File inputGRDFolder = new File("E:\\Data\\RCM\\RCM1_OK77686_PK688502_4_SC4D_20130822_020936_HH_GRD");
    private final static File inputGRDManifest = new File("E:\\Data\\RCM\\RCM1_OK77686_PK688502_4_SC4D_20130822_020936_HH_GRD\\manifest.safe");

    private final static File inputSLCZip = new File("E:\\data\\RCM\\RCM1_OK77686_PK688502_2_3M26_20130822_020936_HH_SLC.zip");
    private final static File inputSLCFolder = new File("E:\\data\\RCM\\RCM1_OK77686_PK688502_2_3M26_20130822_020936_HH_SLC");
    private final static File inputSLCManifest = new File("E:\\data\\RCM\\RCM1_OK77686_PK688502_2_3M26_20130822_020936_HH_SLC\\manifest.safe");

    private final static File inputCPSLC = new File("E:\\data\\RCM\\RCM1_OK21594_PK225278_2_16MCP8_20110624_225945_CH_CV_SLC");
    private final static File inputCPGRC = new File("E:\\data\\RCM\\RCM1_OK21594_PK225278_6_16MCP8_20110624_225945_CH_CV_GRC");
    private final static File inputGRCZip = new File("E:\\data\\RCM\\RCM1_OK77686_PK688502_6_3M27_20130822_020936_HH_GRC.zip");

    public TestRCMProductReader() {
        super(new RCMProductReaderPlugIn());
    }

    @Test
    public void testOpeningGRDManifest() throws Exception {
        testReader(inputGRDManifest);
    }

    @Test
    public void testOpeningGRDZip() throws Exception {
        testReader(inputGRDZip);
    }

    @Test
    public void testOpeningGRDFolder() throws Exception {
        testReader(inputGRDFolder);
    }

    @Test
    public void testOpeningSLCManifest() throws Exception {
        testReader(inputSLCManifest);
    }

    @Test
    public void testOpeningSLCZip() throws Exception {
        testReader(inputSLCZip);
    }

    @Test
    public void testOpeningSLCFolder() throws Exception {
        testReader(inputSLCFolder);
    }

    @Test
    public void testOpeningCPSLCZip() throws Exception {
        testReader(inputCPSLC);
    }

    @Test
    public void testOpeningCPGRCZip() throws Exception {
        testReader(inputCPGRC);
    }

    @Test
    public void testOpeningGRCZip() throws Exception {
        testReader(inputGRCZip);
    }
}
