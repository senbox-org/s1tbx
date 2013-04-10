package org.esa.nest.util;

import com.bc.io.FileDownloader;
import com.bc.io.FileUnpacker;
import org.esa.beam.visat.VisatApp;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Auto-download resource files
 */
public class AutoDownload {

    /**
     * Download and install a resource
     * @param message status message while downloading
     * @param installDir local destination path
     * @param ARCHIVE_URL_PATH  remote path
     * @return path where installed
     */
    public static String getDownloadedDirPath(final String message, final File installDir, final String ARCHIVE_URL_PATH) {

        if (!installDir.canRead() || installDir.listFiles().length == 0) {
            if(!installDir.canRead() && !installDir.mkdirs())
                return null;
            try {
                downloadDGGTiles(message, installDir, ARCHIVE_URL_PATH);
            } catch(IOException e) {
                return null;
            }
        }
        return installDir.getAbsolutePath();
    }

    private synchronized static void downloadDGGTiles(final String message, final File installDir,
                                                      final String ARCHIVE_URL_PATH) throws IOException {
        final VisatApp visatApp = VisatApp.getApp();
        if(visatApp != null) {
            visatApp.setStatusBarMessage(message);
        }
        final Component parent = visatApp != null ? visatApp.getMainFrame() : null;

        final File archiveFile = FileDownloader.downloadFile(new URL(ARCHIVE_URL_PATH), installDir, parent);
        FileUnpacker.unpackZip(archiveFile, installDir, parent);
        archiveFile.delete();

        if(visatApp != null) {
            visatApp.setStatusBarMessage("");
        }
    }
}
