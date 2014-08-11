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
package org.esa.snap.util;

import junit.framework.TestCase;

import java.io.File;
import java.util.Map;


/**
 * FTPUtils Tester.
 *
 * @author lveci
 */
public class TestFTPUtils extends TestCase {

    public TestFTPUtils(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testConnect() throws Exception {
        final String server = "xftp.jrc.it";
        final String remotePath = "/pub/srtmV4/tiff/";

        final ftpUtils ftp = new ftpUtils(server);
        final Map<String, Long> fileSizeMap = ftpUtils.readRemoteFileList(ftp, server, remotePath);

        final String localPath = Settings.instance().get("DEM/srtm3GeoTiffDEMDataPath");
        final File localFile = new File(localPath, "srtm_35_03.zip");
        final String remoteFileName = localFile.getName();
        final Long fileSize = fileSizeMap.get(remoteFileName);

        final ftpUtils.FTPError result = ftp.retrieveFile(remotePath + remoteFileName, localFile, fileSize);
        assertTrue(result == ftpUtils.FTPError.OK);
    }

}