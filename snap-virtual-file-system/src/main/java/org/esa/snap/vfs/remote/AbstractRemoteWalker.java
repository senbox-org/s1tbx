package org.esa.snap.vfs.remote;

import org.esa.snap.core.util.StringUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * AbstractRemoteWalker for VFSWalker
 *
 * @author Jean Coravu
 * @author Adrian Drăghici
 */
public abstract class AbstractRemoteWalker implements VFSWalker {

    protected final IRemoteConnectionBuilder remoteConnectionBuilder;

    protected AbstractRemoteWalker(IRemoteConnectionBuilder remoteConnectionBuilder) {
        this.remoteConnectionBuilder = remoteConnectionBuilder;
    }

    /**
     * Gets the VFS file basic attributes.
     *
     * @return The HTTP file basic attributes
     * @throws IOException If an I/O error occurs
     */
    @Override
    public BasicFileAttributes readBasicFileAttributes(VFSPath path) throws IOException {
        // check if the address represents a directory
        String address = path.buildURL().toString();
        String fileSystemSeparator = path.getFileSystem().getSeparator();
        URL directoryURL = new URL(address + (address.endsWith(fileSystemSeparator) ? "" : fileSystemSeparator));
        HttpURLConnection connection = this.remoteConnectionBuilder.buildConnection(directoryURL, "GET", null);
        try {
            int responseCode = connection.getResponseCode();
            if (HttpUtils.isValidResponseCode(responseCode)) {
                // the address represents a directory
                return VFSFileAttributes.newDir(path.toString());
            }
        } finally {
            connection.disconnect();
        }
        // the address does not represent a directory
        return readFileAttributes(address, path.toString());
    }

    public BasicFileAttributes readFileAttributes(String address, String filePath) throws IOException {
        URL fileURL = new URL(address);
        HttpURLConnection connection = this.remoteConnectionBuilder.buildConnection(fileURL, "GET", null);
        try {
            int responseCode = connection.getResponseCode();
            if (HttpUtils.isValidResponseCode(responseCode)) {
                String sizeString = connection.getHeaderField("content-length");
                String lastModified = connection.getHeaderField("last-modified");
                if (!StringUtils.isNotNullAndNotEmpty(sizeString) && StringUtils.isNotNullAndNotEmpty(lastModified)) {
                    throw new IOException("filePath is not a file '" + filePath + "'.");
                }
                long size = Long.parseLong(sizeString);
                return VFSFileAttributes.newFile(filePath, size, lastModified);
            } else {
                throw new IOException(address + ": response code " + responseCode + ": " + connection.getResponseMessage());
            }
        } finally {
            connection.disconnect();
        }
    }
}
