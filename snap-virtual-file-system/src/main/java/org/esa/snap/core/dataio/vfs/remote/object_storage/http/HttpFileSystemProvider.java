package org.esa.snap.core.dataio.vfs.remote.object_storage.http;

import org.esa.snap.core.dataio.vfs.remote.object_storage.ObjectStorageFileSystem;
import org.esa.snap.core.dataio.vfs.remote.object_storage.ObjectStorageFileSystemProvider;
import org.esa.snap.core.dataio.vfs.remote.object_storage.ObjectStorageWalker;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;

/**
 * File System Service Provider for the HTTP Object Storage VFS.
 *
 * @author Adrian Drăghici
 */
public class HttpFileSystemProvider extends ObjectStorageFileSystemProvider {

    public static final String HTTP_ROOT = "HTTP:/";
    private final static String SCHEME = "http";

    private static String address = "http://localhost";
    private static String username = "";
    private static String password = "";
    private static String delimiter = "/";

    static String getUsername() {
        return username;
    }

    static String getPassword() {
        return password;
    }

    public static FileSystem getHttpFileSystem() throws AccessDeniedException {
        FileSystem fs = null;
        try {
            URI uri = new URI(new HttpFileSystemProvider().getScheme() + ":" + new HttpFileSystemProvider().getProviderAddress());
            try {
                fs = FileSystems.getFileSystem(uri);
            } catch (Exception ignored) {
            }
            if (fs != null) {
                fs.close();
            }
            fs = FileSystems.newFileSystem(uri, new HashMap<>());
            fs.getRootDirectories();
        } catch (Exception e) {
            throw new AccessDeniedException(e.getMessage());
        }
        return fs;
    }

    /**
     * Setup this provider with connection data for connect to HTTP Service.
     *
     * @param address  Address of HTTP service. (mandatory)
     * @param username Username for login to HTTP service. (mandatory)
     * @param password Password for login to HTTP Service. (mandatory)
     */
    public static void setupConnectionData(String address, String username, String password) {
        HttpFileSystemProvider.address = address != null ? address : HttpFileSystemProvider.address;
        HttpFileSystemProvider.username = username != null ? username : HttpFileSystemProvider.username;
        HttpFileSystemProvider.password = password != null ? password : HttpFileSystemProvider.password;
    }

    @Override
    protected ObjectStorageFileSystem newFileSystem(String address, Map<String, ?> env) throws IOException {
        try {
            Object delimiter = env.get("delimiter");
            return new ObjectStorageFileSystem(this,
                                               address,
                                               delimiter != null ? delimiter.toString() : "/");
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    @Override
    protected ObjectStorageWalker newObjectStorageWalker() {
        try {
            return new HttpWalker();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getProviderAddress() {
        return address;
    }

    @Override
    public String getRoot() {
        return HTTP_ROOT;
    }

    public URLConnection getProviderConnectionChannel(URL url, String method, Map<String, String> requestProperties) throws IOException {
        return HttpResponseHandler.getConnectionChannel(url, method, requestProperties);
    }

    /**
     * Returns the URI scheme that identifies this provider.
     *
     * @return The URI scheme
     */
    @Override
    public String getScheme() {
        return SCHEME;
    }

    public String getDelimiter() {
        return delimiter;
    }

    /**
     * Setup this provider with custom separator for Openstack Object Storage Swift Virtual File System.
     *
     * @param delimiter Openstack Object Storage Swift Virtual File System separator.
     */
    public static void setDelimiter(String delimiter) {
        HttpFileSystemProvider.delimiter = delimiter != null && !delimiter.isEmpty() ? delimiter : HttpFileSystemProvider.delimiter;
    }

}
