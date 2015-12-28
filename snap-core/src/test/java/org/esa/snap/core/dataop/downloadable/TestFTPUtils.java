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
package org.esa.snap.core.dataop.downloadable;

import org.esa.snap.core.util.SystemUtils;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * FTPUtils Tester.
 *
 * @author lveci
 */
public class TestFTPUtils {

    @Test
    public void testConnect() throws Exception {
        final String server = "xftp.jrc.it";
        final String remotePath = "/pub/srtmV4/tiff/";

        final FtpUtils ftp = new FtpUtils(server);
        final Map<String, Long> fileSizeMap = FtpUtils.readRemoteFileList(ftp, server, remotePath);

        final String localPath = SystemUtils.getAuxDataPath().resolve("dem/SRTM_DEM/tiff").toString();
        final File localFile = new File(localPath, "srtm_35_03.zip");
        final String remoteFileName = localFile.getName();
        final Long fileSize = fileSizeMap.get(remoteFileName);

        final FtpUtils.FTPError result = ftp.retrieveFile(remotePath + remoteFileName, localFile, fileSize);
        assertTrue(result == FtpUtils.FTPError.OK);
    }

}