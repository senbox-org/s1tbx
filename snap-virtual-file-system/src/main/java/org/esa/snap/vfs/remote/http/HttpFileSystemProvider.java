package org.esa.snap.vfs.remote.http;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;
import org.esa.snap.vfs.remote.ObjectStorageWalker;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

/**
 * File System Service Provider for the HTTP Object Storage VFS.
 *
 * @author Adrian Drăghici
 */
public class HttpFileSystemProvider extends AbstractRemoteFileSystemProvider {

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

    private String address;
    private String username;
    private String password;
    private String delimiter;

    public HttpFileSystemProvider() {
        super(HTTP_ROOT);

        address = "";
        username = "";
        password = "";
        delimiter = DELIMITER_PROPERTY_DEFAULT_VALUE;
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
    protected HttpFileSystem newFileSystem(String address, Map<String, ?> env) throws IOException {
        return new HttpFileSystem(this, address, delimiter);
    }

    /**
     * Creates the walker instance used by VFS provider to traverse VFS tree.
     *
     * @return The new VFS walker instance
     */
    @Override
    protected ObjectStorageWalker newObjectStorageWalker() {
        return new HttpWalker(address, username, password, delimiter, root);
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
}
