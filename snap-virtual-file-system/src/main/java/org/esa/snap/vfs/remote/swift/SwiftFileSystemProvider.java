package org.esa.snap.vfs.remote.swift;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * File System Service Provider for OpenStack Swift VFS.
 *
 * @author Adrian Drăghici
 */
public final class SwiftFileSystemProvider extends AbstractRemoteFileSystemProvider {

    /**
     * The value of OpenStack Swift provider scheme.
     */
    private static final String SCHEME = "oss";

    private final Map<String, SwiftFileSystemProviderHelper> swiftFileSystemProviderList;


    public SwiftFileSystemProvider() {
        super();
        this.swiftFileSystemProviderList = new HashMap<>();
    }

    @Override
    public void setConnectionData(String fileSystemRoot, String serviceAddress, Map<String, ?> connectionData) {
        SwiftFileSystemProviderHelper swiftFileSystemProviderHelper = getProviderHelperOrCreate(fileSystemRoot);
        swiftFileSystemProviderHelper.setConnectionData(serviceAddress, connectionData);
    }

    @Override
    protected SwiftFileSystem newFileSystem(String fileSystemRoot, Map<String, ?> env) {
        return new SwiftFileSystem(this, fileSystemRoot);
    }

    /**
     * Creates the walker instance used by VFS provider to traverse VFS tree.
     *
     * @return The new VFS walker instance
     */
    @Override
    protected SwiftWalker newObjectStorageWalker(String fileSystemRoot) {
        SwiftFileSystemProviderHelper swiftFileSystemProviderHelper = getProviderHelperOrCreate(fileSystemRoot);
        return swiftFileSystemProviderHelper.newObjectStorageWalker(this);
    }

    /**
     * Gets the service address of this VFS provider.
     *
     * @return The VFS service address
     */
    @Override
    public String getProviderAddress(String fileSystemRoot) {
        SwiftFileSystemProviderHelper swiftFileSystemProviderHelper = getProviderHelperOrCreate(fileSystemRoot);
        return swiftFileSystemProviderHelper.getProviderAddress();
    }

    @Override
    public String getProviderFileSeparator(String fileSystemRoot) {
        SwiftFileSystemProviderHelper swiftFileSystemProviderHelper = getProviderHelperOrCreate(fileSystemRoot);
        return swiftFileSystemProviderHelper.getProviderFileSeparator();
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

    private SwiftFileSystemProviderHelper getProviderHelperOrCreate(String fileSystemRoot) {
        return this.swiftFileSystemProviderList.computeIfAbsent(fileSystemRoot, provider -> new SwiftFileSystemProviderHelper(fileSystemRoot));
    }

    @Override
    public HttpURLConnection buildConnection(String fileSystemRoot, URL url, String method, Map<String, String> requestProperties) throws IOException {
        SwiftFileSystemProviderHelper swiftFileSystemProviderHelper = getProviderHelperOrCreate(fileSystemRoot);
        return swiftFileSystemProviderHelper.buildConnection(url, method, requestProperties);
    }
}
