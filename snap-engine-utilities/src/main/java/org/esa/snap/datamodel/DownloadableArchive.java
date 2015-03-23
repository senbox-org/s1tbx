package org.esa.snap.datamodel;

import org.esa.snap.util.ZipUtils;

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
