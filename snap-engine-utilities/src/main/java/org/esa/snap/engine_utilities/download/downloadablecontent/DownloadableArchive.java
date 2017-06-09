/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.download.downloadablecontent;

import org.esa.snap.engine_utilities.util.ZipUtils;

import java.io.File;
import java.net.URL;

/**
 * Used for retrieving a single file from a remote server
 */
public class DownloadableArchive extends DownloadableContentImpl {

    private final File localFile;

    public DownloadableArchive(final File localFile, final URL remotePath) {
        super(localFile, remotePath, ".zip");
        this.localFile = localFile;
    }

    public void getContentFiles() throws Exception {
        final File archiveFile = (File)getContentFile();

        if(archiveFile != null) {
            ZipUtils.unzipToFolder(archiveFile, localFile.getParentFile());
            archiveFile.delete();
        }
    }

    protected DownloadableFile createContentFile(final File dataFile) {
        return new DownloadableArchiveFile(dataFile);
    }
}
