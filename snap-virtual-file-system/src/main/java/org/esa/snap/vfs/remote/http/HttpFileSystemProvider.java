package org.esa.snap.vfs.remote.http;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;
import org.esa.snap.vfs.remote.VFSWalker;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * File System Service Provider for the HTTP VFS.
 *
 * @author Adrian Drăghici
 */
public final class HttpFileSystemProvider extends AbstractRemoteFileSystemProvider {

    /**
     * The value of HTTP provider scheme.
     */
    private static final String SCHEME = "http";

    private final Map<String, HttpFileSystemProviderHelper> httpFileSystemProviderList;

    public HttpFileSystemProvider() {
        super();
        this.httpFileSystemProviderList = new HashMap<>();
    }

    @Override
    public void setConnectionData(String fileSystemRoot, String serviceAddress, Map<String, ?> connectionData) {
        HttpFileSystemProviderHelper httpProviderHelper = getProviderHelperOrCreate(fileSystemRoot);
        httpProviderHelper.setConnectionData(serviceAddress, connectionData);
    }

    @Override
    protected HttpFileSystem newFileSystem(String fileSystemRoot, Map<String, ?> env) {
        return new HttpFileSystem(this, fileSystemRoot);
    }

    /**
     * Creates the walker instance used by VFS provider to traverse VFS tree.
     *
     * @return The new VFS walker instance
     */
    @Override
    protected final VFSWalker newObjectStorageWalker(String fileSystemRoot) {
        HttpFileSystemProviderHelper httpProviderHelper = getProviderHelperOrCreate(fileSystemRoot);
        return httpProviderHelper.newObjectStorageWalker(this);
    }

    /**
     * Gets the service address of this VFS provider.
     *
     * @return The VFS service address
     */
    @Override
    public String getProviderAddress(String fileSystemRoot) {
        HttpFileSystemProviderHelper httpProviderHelper = getProviderHelperOrCreate(fileSystemRoot);
        return httpProviderHelper.getProviderAddress();
    }

    @Override
    public String getProviderFileSeparator(String fileSystemRoot) {
        HttpFileSystemProviderHelper httpProviderHelper = getProviderHelperOrCreate(fileSystemRoot);
        return httpProviderHelper.getProviderFileSeparator();
    }

    /**
     * Gets the URI scheme that identifies this VFS provider.
     *
     * @return The URI scheme
     */
    @Override
    public final String getScheme() {
        return SCHEME;
    }

    private HttpFileSystemProviderHelper getProviderHelperOrCreate(String fileSystemRoot) {
        return this.httpFileSystemProviderList.computeIfAbsent(fileSystemRoot, provider -> new HttpFileSystemProviderHelper(fileSystemRoot));
    }

    @Override
    public HttpURLConnection buildConnection(String fileSystemRoot, URL url, String method, Map<String, String> requestProperties) throws IOException {
        HttpFileSystemProviderHelper httpProviderHelper = getProviderHelperOrCreate(fileSystemRoot);
        return httpProviderHelper.buildConnection(url, method, requestProperties);
    }
}
