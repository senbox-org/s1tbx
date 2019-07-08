package org.esa.snap.vfs.remote.s3;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;
import org.esa.snap.vfs.remote.VFSWalker;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * File System Service Provider for S3 VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public final class S3FileSystemProvider extends AbstractRemoteFileSystemProvider {

    /**
     * The value of S3 provider scheme.
     */
    private static final String SCHEME = "s3";

    private final Map<String, S3FileSystemProviderHelper> s3FileSystemProviderList;

    public S3FileSystemProvider() {
        super();
        this.s3FileSystemProviderList = new HashMap<>();
    }

    @Override
    public void setConnectionData(String fileSystemRoot, String serviceAddress, Map<String, ?> connectionData) {
        S3FileSystemProviderHelper s3ProviderHelper = getProviderHelperOrCreate(fileSystemRoot);
        s3ProviderHelper.setConnectionData(serviceAddress, connectionData);
    }

    @Override
    protected S3FileSystem newFileSystem(String fileSystemRoot, Map<String, ?> env) {
        return new S3FileSystem(this, fileSystemRoot);
    }

    /**
     * Creates the walker instance used by VFS provider to traverse VFS tree.
     *
     * @return The new VFS walker instance
     */
    @Override
    protected VFSWalker newObjectStorageWalker(String fileSystemRoot) {
        S3FileSystemProviderHelper s3ProviderHelper = getProviderHelperOrCreate(fileSystemRoot);
        return s3ProviderHelper.newObjectStorageWalker(this);
    }

    /**
     * Gets the service address of this VFS provider.
     *
     * @return The VFS service address
     */
    @Override
    public String getProviderAddress(String fileSystemRoot) {
        S3FileSystemProviderHelper s3ProviderHelper = getProviderHelperOrCreate(fileSystemRoot);
        return s3ProviderHelper.getProviderAddress();
    }

    @Override
    public String getProviderFileSeparator(String fileSystemRoot) {
        S3FileSystemProviderHelper s3ProviderHelper = getProviderHelperOrCreate(fileSystemRoot);
        return s3ProviderHelper.getProviderFileSeparator();
    }

    /**
     * Gets the URI scheme that identifies this VFS provider.
     *
     * @return The URI scheme
     */
    @Override
    public String getScheme() {
        return SCHEME;
    }

    private S3FileSystemProviderHelper getProviderHelperOrCreate(String fileSystemRoot) {
        return this.s3FileSystemProviderList.computeIfAbsent(fileSystemRoot, provider -> new S3FileSystemProviderHelper(fileSystemRoot));
    }

    @Override
    public HttpURLConnection buildConnection(String fileSystemRoot, URL url, String method, Map<String, String> requestProperties) throws IOException {
        S3FileSystemProviderHelper s3ProviderHelper = getProviderHelperOrCreate(fileSystemRoot);
        return s3ProviderHelper.buildConnection(url, method, requestProperties);
    }
}
