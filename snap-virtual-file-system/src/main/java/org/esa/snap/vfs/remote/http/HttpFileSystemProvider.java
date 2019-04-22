package org.esa.snap.vfs.remote.http;

import org.esa.snap.core.util.StringUtils;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;
import org.esa.snap.vfs.remote.HttpUtils;
import org.esa.snap.vfs.remote.VFSWalker;
import org.esa.snap.vfs.remote.swift.SwiftFileSystemProvider;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File System Service Provider for the HTTP VFS.
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
     * The value of S3 provider scheme.
     */
    private static final String SCHEME = "http";

    private String address;
    private String username;
    private String password;
    private String delimiter;
    private String authorizationToken;

    public HttpFileSystemProvider() {
        super();

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

    @Override
    public void setConnectionData(String serviceAddress, Map<String, ?> connectionData) {
        String newUsername = (String) connectionData.get(USERNAME_PROPERTY_NAME);
        String newCredential = (String) connectionData.get(CREDENTIAL_PROPERTY_NAME);
        setupConnectionData(serviceAddress, newUsername, newCredential);
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
    protected VFSWalker newObjectStorageWalker(String fileSystemRoot) {
        return new HttpWalker(this.address, this.delimiter, fileSystemRoot, this);
    }

    /**
     * Gets the service address of this VFS provider.
     *
     * @return The VFS service address
     */
    @Override
    public String getProviderAddress() {
        return this.address;
    }

    @Override
    public String getProviderFileSeparator() {
        return this.delimiter;
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

    @Override
    public HttpURLConnection buildConnection(URL url, String method, Map<String, String> requestProperties) throws IOException {
        synchronized (this) {
            if (this.authorizationToken == null) {
                this.authorizationToken = getAuthorizationToken(this.username, this.password);
                if (this.authorizationToken == null) {
                    this.authorizationToken = ""; // do not request later the authorization token
                }
            }
        }
        return buildConnection(url, method, requestProperties, this.username, this.password);
    }

    /**
     * Creates the authorization token used for HTTP authentication.
     *
     * @param username The username HTTP credential
     * @param password The password HTTP credential
     * @return The authorization token
     */
    private static String getAuthorizationToken(String username, String password) {
        return (!StringUtils.isNotNullAndNotEmpty(username) && !StringUtils.isNotNullAndNotEmpty(password)) ? Base64.getEncoder().encodeToString((username + ":" + password).getBytes()) : "";
    }

    static HttpURLConnection buildConnection(URL url, String method, Map<String, String> requestProperties, String username, String password) throws IOException {
        String authorizationToken = getAuthorizationToken(username, password);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setDoInput(true);
        if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("DELETE")) {
            connection.setDoOutput(true);
        }
        connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        if (authorizationToken != null && !authorizationToken.isEmpty()) {
            connection.setRequestProperty("authorization", "Basic " + authorizationToken);
        }
        if (requestProperties != null && requestProperties.size() > 0) {
            Set<Map.Entry<String, String>> requestPropertiesSet = requestProperties.entrySet();
            for (Map.Entry<String, String> requestProperty : requestPropertiesSet) {
                connection.setRequestProperty(requestProperty.getKey(), requestProperty.getValue());
            }
        }
        connection.setRequestProperty("user-agent", "SNAP Virtual File System");
        return connection;
    }
}
