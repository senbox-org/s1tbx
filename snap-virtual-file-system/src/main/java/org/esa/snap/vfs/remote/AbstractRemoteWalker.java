package org.esa.snap.vfs.remote;

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

    protected BasicFileAttributes readFileAttributes(String urlAddress, String filePath) throws IOException {
        RegularFileMetadata regularFileMetadata = HttpUtils.readRegularFileMetadata(urlAddress, this.remoteConnectionBuilder);
        return VFSFileAttributes.newFile(filePath, regularFileMetadata);
    }
}
