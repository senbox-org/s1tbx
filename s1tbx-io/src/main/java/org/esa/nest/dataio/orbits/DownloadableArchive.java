package org.esa.nest.dataio.orbits;

import org.esa.snap.datamodel.DownloadableContentImpl;
import org.esa.snap.datamodel.DownloadableFile;

import java.io.File;
import java.net.URL;

/**
 * Used for retrieving a single file from a remote server
 */
public class DownloadableArchive extends DownloadableContentImpl {

    public DownloadableArchive(final File localFile, final URL remotePath) {
        super(localFile, remotePath, ".zip");
    }

    protected DownloadableFile createContentFile(final File dataFile) {
        return new DownloadableArchiveFile(dataFile);
    }
}
