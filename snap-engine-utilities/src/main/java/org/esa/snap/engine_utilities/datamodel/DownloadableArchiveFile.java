package org.esa.snap.engine_utilities.datamodel;


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
