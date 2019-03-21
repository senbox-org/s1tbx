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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File System Service Provider for the HTTP Object Storage VFS.
 *
 * @author Adrian Drăghici
 */
public class HttpFileSystemProvider extends ObjectStorageFileSystemProvider {

    /**
     * The name of root property, used on VFS instance creation parameters.
     */
    private static final String ROOT_PROPERTY_NAME = "root";

    /**
     * The name of delimiter property, used on VFS instance creation parameters.
     */
    private static final String DELIMITER_PROPERTY_NAME = "delimiter";

    /**
     * The default value of delimiter property, used on VFS instance creation parameters.
     */
    private static final String DELIMITER_PROPERTY_DEFAULT_VALUE = "/";

    /**
     * The name of username property, used on HTTP VFS instance creation parameters and defining remote file repository properties.
     */
    private static final String USERNAME_PROPERTY_NAME = "username";

    /**
     * The name of password property, used on HTTP VFS instance creation parameters and defining remote file repository properties.
     */
    private static final String CREDENTIAL_PROPERTY_NAME = "password";

    /**
     * The default value of root property, used on VFS instance creation parameters.
     */
    private static final String HTTP_ROOT = "HTTP:/";

    /**
     * The value of S3 provider scheme.
     */
    private static final String SCHEME = "http";

    private static Logger logger = Logger.getLogger(HttpFileSystemProvider.class.getName());

    private String address = "";
    private String username = "";
    private String password = "";
    private String delimiter = DELIMITER_PROPERTY_DEFAULT_VALUE;

    private String root = HTTP_ROOT;

    /**
     * Gets the HTTP Virtual File System without authentication.
     *
     * @param address The address of HTTP service. (mandatory)
     * @return The new HTTP Virtual File System
     * @throws IOException If an I/O error occurs
     */
    public static FileSystem getHttpFileSystem(String address) throws IOException {
        return getHttpFileSystem(address, "", "");
    }

    /**
     * Gets the new HTTP Virtual File System with authentication.
     *
     * @param address  The address of HTTP service. (mandatory)
     * @param username The username HTTP credential
     * @param password The password HTTP credential
     * @return The new HTTP Virtual File System
     * @throws IOException If an I/O error occurs
     */
    public static FileSystem getHttpFileSystem(String address, String username, String password) throws IOException {
        FileSystem fs = null;
        URI uri;
        try {
            uri = new URI(SCHEME + ":" + address);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Invalid URI for HTTP VFS. Details: " + ex.getMessage());
            throw new IOException(ex);
        }
        try {
            fs = FileSystems.getFileSystem(uri);
        } catch (Exception ex) {
            logger.log(Level.FINE, "HTTP VFS not loaded. Details: " + ex.getMessage());
        } finally {
            if (fs != null) {
                fs.close();
            }
        }
        try {
            Map<String, String> env = new HashMap<>();
            env.put(USERNAME_PROPERTY_NAME, username);
            env.put(CREDENTIAL_PROPERTY_NAME, password);
            fs = FileSystems.newFileSystem(uri, env, HttpFileSystemProvider.class.getClassLoader());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to initialize HTTP VFS. Details: " + ex.getMessage());
            throw new AccessDeniedException(ex.getMessage());
        }
        return fs;
    }

    /**
     * Save connection data on this provider.
     *
     * @param address  The address of HTTP service. (mandatory)
     * @param username The username HTTP credential
     * @param password The password HTTP credential
     */
    private void setupConnectionData(String address, String username, String password) {
        this.address = address != null ? address : "";
        this.username = username != null ? username : "";
        this.password = password != null ? password : "";
    }

    public void setConnectionData(String serviceAddress, Map<String, ?> connectionData) {
        String newRoot = (String) connectionData.get(ROOT_PROPERTY_NAME);
        root = newRoot != null && !newRoot.isEmpty() ? newRoot : HTTP_ROOT;
        String newUsername = (String) connectionData.get(USERNAME_PROPERTY_NAME);
        String newCredential = (String) connectionData.get(CREDENTIAL_PROPERTY_NAME);
        setupConnectionData(serviceAddress, newUsername, newCredential);
    }

    /**
     * Creates the VFS instance using this provider.
     *
     * @param address The VFS service address
     * @param env     The VFS parameters
     * @return The new VFS instance
     * @throws IOException If an I/O error occurs
     */
    @Override
    protected ObjectStorageFileSystem newFileSystem(String address, Map<String, ?> env) throws IOException {
        if (env != null) {
            String newDelimiter = (String) env.get(DELIMITER_PROPERTY_NAME);
            delimiter = newDelimiter != null && !newDelimiter.isEmpty() ? newDelimiter : DELIMITER_PROPERTY_DEFAULT_VALUE;
            setConnectionData(address, env);
        }
        try {
            return new ObjectStorageFileSystem(this, address, delimiter);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to create new HTTP VFS instance. Details: " + ex.getMessage());
            throw new IOException(ex);
        }
    }

    /**
     * Creates the walker instance used by VFS provider to traverse VFS tree.
     *
     * @return The new VFS walker instance
     */
    @Override
    protected ObjectStorageWalker newObjectStorageWalker() {
        try {
            return new HttpWalker(address, username, password, delimiter, root);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to create walker instance used by HTTP VFS to traverse tree. Details: " + ex.getMessage());
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Gets the service address of this VFS provider.
     *
     * @return The VFS service address
     */
    @Override
    public String getProviderAddress() {
        return address;
    }

    /**
     * Gets the root of this VFS provider.
     *
     * @return The root
     */
    @Override
    public String getRoot() {
        return root != null && !root.isEmpty() ? root : HTTP_ROOT;
    }

    /**
     * Gets the connection channel for this VFS provider.
     *
     * @param url               The URL address to connect
     * @param method            The HTTP method (GET POST DELETE etc)
     * @param requestProperties The properties used on the connection
     * @return The connection channel
     * @throws IOException If an I/O error occurs
     */
    public URLConnection getProviderConnectionChannel(URL url, String method, Map<String, String> requestProperties) throws IOException {
        return HttpResponseHandler.getConnectionChannel(url, method, requestProperties, username, password);
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

    /**
     * Gets the path delimiter for this VFS provider.
     *
     * @return The path delimiter
     */
    public String getDelimiter() {
        return delimiter != null && !delimiter.isEmpty() ? delimiter : DELIMITER_PROPERTY_DEFAULT_VALUE;
    }

}
