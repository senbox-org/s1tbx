package org.esa.snap.datamodel;


import org.esa.snap.datamodel.DownloadableFile;

import java.io.File;

/**
 * Created by lveci on 11/02/2015.
 */
public class DownloadableArchiveFile extends File implements DownloadableFile {

    public DownloadableArchiveFile(final File file) {
        super(file.getAbsolutePath());
    }

    public void dispose() {
    }
}
