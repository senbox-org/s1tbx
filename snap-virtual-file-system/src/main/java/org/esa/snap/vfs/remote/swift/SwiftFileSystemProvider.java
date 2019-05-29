package org.esa.snap.vfs.remote.swift;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * File System Service Provider for OpenStack Swift VFS.
 *
 * @author Adrian Drăghici
 */
public class SwiftFileSystemProvider extends AbstractRemoteFileSystemProvider {

    /**
     * The name of authentication address property, used on OpenStack Swift VFS instance creation parameters and defining remote file repository properties.
     */
    private static final String AUTH_ADDRESS_PROPERTY_NAME = "authAddress";
    /**
     * The name of container property, used on OpenStack Swift VFS instance creation parameters and defining remote file repository properties.
     */
    private static final String CONTAINER_PROPERTY_NAME = "container";
    /**
     * The name of domain property, used on OpenStack Swift VFS instance creation parameters and defining remote file repository properties.
     */
    private static final String DOMAIN_PROPERTY_NAME = "domain";
    /**
     * The name of project id property, used on OpenStack Swift VFS instance creation parameters and defining remote file repository properties.
     */
    private static final String PROJECT_ID_PROPERTY_NAME = "projectId";
    /**
     * The name of user property, used on OpenStack Swift VFS instance creation parameters and defining remote file repository properties.
     */
    private static final String USER_PROPERTY_NAME = "user";
    /**
     * The name of password property, used on OpenStack Swift VFS instance creation parameters and defining remote file repository properties.
     */
    private static final String CREDENTIAL_PROPERTY_NAME = "password";
    /**
     * The value of OpenStack Swift provider scheme.
     */
    private static final String SCHEME = "oss";
    private static Logger logger = Logger.getLogger(SwiftFileSystemProvider.class.getName());
    private String address = "";
    private String authAddress = "";
    private String container = "";
    private String domain = "";
    private String projectId = "";
    private String user = "";
    private String password = "";
    private String delimiter = DELIMITER_PROPERTY_DEFAULT_VALUE;
    private SwiftAuthorizationToken authorizationToken;

    public SwiftFileSystemProvider() {
        super();
    }

    /**
     * Creates the authorization token used for OpenStack Swift authentication.
     *
     * @param authAddress The address of authentication service used by OpenStack Swift service (mandatory if credentials is provided)
     * @param domain      The domain name OpenStack Swift credential
     * @param projectId   The account ID/ Project/ Tenant name OpenStack Swift credential
     * @param user        The username OpenStack Swift credential
     * @param password    The password OpenStack Swift credential
     * @return The authorization token
     */
    private static SwiftAuthorizationToken createAuthorizationToken(String authAddress, String domain, String projectId, String user, String password) throws IOException {
        if (domain == null || domain.isEmpty() || projectId == null || projectId.isEmpty() || user == null || user.isEmpty() || password == null || password.isEmpty()) {
            return null;
        }
        URL authUrl = new URL(authAddress);
        HttpURLConnection connection = (HttpURLConnection) authUrl.openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("user-agent", "SNAP Virtual File System");
            try (OutputStream outputStream = connection.getOutputStream();
                 PrintWriter out = new PrintWriter(outputStream)) {

                out.print("{\n" +
                        "    \"auth\": {\n" +
                        "        \"identity\": {\n" +
                        "            \"methods\": [\n" +
                        "                \"password\"\n" +
                        "            ],\n" +
                        "            \"password\": {\n" +
                        "                \"user\": {\n" +
                        "                    \"domain\": {\n" +
                        "                        \"name\": \"" + domain + "\"\n" +
                        "                    },\n" +
                        "                    \"name\": \"" + user + "\",\n" +
                        "                    \"password\": \"" + password + "\"\n" +
                        "                }\n" +
                        "            }\n" +
                        "        },\n" +
                        "        \"scope\": {\n" +
                        "            \"project\": {\n" +
                        "                \"id\": \"" + projectId + "\"\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}");
                out.flush();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                    String token = connection.getHeaderField("X-Subject-Token");
                    InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String result = bufferedReader.lines().collect(Collectors.joining("\n"));
                    String expirationDate = result.replaceAll("[\\s\\S]*\"expires_at\": \"(.*?)\",[\\s\\S]*", "$1");
                    return new SwiftAuthorizationToken(token, expirationDate);
                }
            }
        } finally {
            connection.disconnect();
        }
        return null;
    }

    private static HttpURLConnection buildConnection(URL url, String method, Map<String, String> requestProperties, SwiftAuthorizationToken authorizationToken) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setDoInput(true);
        if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("DELETE")) {
            connection.setDoOutput(true);
        }
        if (authorizationToken != null && authorizationToken.getToken() != null && !authorizationToken.getToken().isEmpty()) {
            connection.setRequestProperty("X-Auth-Token", authorizationToken.getToken());
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

    /**
     * Save connection data on this provider.
     *
     * @param address     The address of OpenStack Swift service (mandatory)
     * @param authAddress The address of authentication service used by OpenStack Swift service (mandatory if credentials is provided)
     * @param container   The container name (bucket) (mandatory)
     * @param domain      The domain name OpenStack Swift credential
     * @param projectId   The account ID/ Project/ Tenant name OpenStack Swift credential
     * @param user        The username OpenStack Swift credential
     * @param password    The password OpenStack Swift credential
     * @link https://developer.openstack.org/api-ref/object-store/
     */
    private void setupConnectionData(String address, String authAddress, String container, String domain, String projectId, String user, String password) {
        this.address = address != null ? address : this.address;
        this.authAddress = authAddress != null ? authAddress : this.authAddress;
        this.container = container != null ? container : this.container;
        this.domain = domain != null ? domain : this.domain;
        this.projectId = projectId != null ? projectId : this.projectId;
        this.user = user != null ? user : this.user;
        this.password = password != null ? password : this.password;
    }

    @Override
    public void setConnectionData(String serviceAddress, Map<String, ?> connectionData) {
        String newAuthAddress = (String) connectionData.get(AUTH_ADDRESS_PROPERTY_NAME);
        String newContainer = (String) connectionData.get(CONTAINER_PROPERTY_NAME);
        String newDomain = (String) connectionData.get(DOMAIN_PROPERTY_NAME);
        String newProjectId = (String) connectionData.get(PROJECT_ID_PROPERTY_NAME);
        String newUser = (String) connectionData.get(USER_PROPERTY_NAME);
        String newCredential = (String) connectionData.get(CREDENTIAL_PROPERTY_NAME);
        setupConnectionData(serviceAddress, newAuthAddress, newContainer, newDomain, newProjectId, newUser, newCredential);
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
        if (this.container.isEmpty()) {
            throw new IllegalArgumentException("Missing 'container' property.\nPlease provide a container name.");
        }
        if (!(this.domain.isEmpty() && this.projectId.isEmpty() && this.user.isEmpty() && this.password.isEmpty()) && this.authAddress.isEmpty()) {
            throw new IllegalArgumentException("Missing 'authAddress' property.\nPlease provide authentication address required to access authentication service.");
        }
        return new SwiftWalker(this.address, this.container, this.delimiter, fileSystemRoot, this);
    }

    /**
     * Gets the service address of this VFS provider.
     *
     * @return The VFS service address
     */
    @Override
    public String getProviderAddress() {
        return this.address + this.container;
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
            if (this.authorizationToken == null || !this.authorizationToken.isValid()) {
                try {
                    this.authorizationToken = createAuthorizationToken(this.authAddress, this.domain, this.projectId, this.user, this.password);
                    if (this.authorizationToken == null) {
                        this.authorizationToken = new SwiftAuthorizationToken(null, null); // do not request later the authorization token
                    }
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "Unable to create authorization token used for OpenStack Swift authentication. Details: " + ex.getMessage(), ex);
                }
            }
        }
        return buildConnection(url, method, requestProperties, this.authorizationToken);
    }
}
