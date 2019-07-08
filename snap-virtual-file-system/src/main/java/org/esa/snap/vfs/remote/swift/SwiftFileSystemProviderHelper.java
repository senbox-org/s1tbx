package org.esa.snap.vfs.remote.swift;

import org.esa.snap.vfs.remote.IRemoteConnectionBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;

/**
 * File System Service Provider for OpenStack Swift VFS.
 *
 * @author Adrian Drăghici
 */
public class SwiftFileSystemProviderHelper {

    /**
     * The default value of delimiter property, used on VFS instance creation parameters.
     */
    private static final String DELIMITER_PROPERTY_DEFAULT_VALUE = "/";

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

    private String fileSystemRoot;

    private String address;
    private String authAddress;
    private String container;
    private String domain;
    private String projectId;
    private String user;
    private String password;
    private String delimiter;
    private SwiftAuthenticationV3 swiftAuthenticationV3;

    SwiftFileSystemProviderHelper(String fileSystemRoot) {
        this.fileSystemRoot = fileSystemRoot;
        this.address = "";
        this.authAddress = "";
        this.container = "";
        this.domain = "";
        this.projectId = "";
        this.user = "";
        this.password = "";
        this.delimiter = DELIMITER_PROPERTY_DEFAULT_VALUE;
        this.swiftAuthenticationV3 = null;
    }

    private static HttpURLConnection buildConnection(URL url, String method, Map<String, String> requestProperties, String authorizationToken) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setDoInput(true);
        if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("DELETE")) {
            connection.setDoOutput(true);
        }
        if (authorizationToken != null && !authorizationToken.isEmpty()) {
            connection.setRequestProperty("X-Auth-Token", authorizationToken);
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

    public void setConnectionData(String serviceAddress, Map<String, ?> connectionData) {
        String newAuthAddress = (String) connectionData.get(AUTH_ADDRESS_PROPERTY_NAME);
        String newContainer = (String) connectionData.get(CONTAINER_PROPERTY_NAME);
        String newDomain = (String) connectionData.get(DOMAIN_PROPERTY_NAME);
        String newProjectId = (String) connectionData.get(PROJECT_ID_PROPERTY_NAME);
        String newUser = (String) connectionData.get(USER_PROPERTY_NAME);
        String newCredential = (String) connectionData.get(CREDENTIAL_PROPERTY_NAME);
        setupConnectionData(serviceAddress, newAuthAddress, newContainer, newDomain, newProjectId, newUser, newCredential);
    }

    /**
     * Creates the walker instance used by VFS provider to traverse VFS tree.
     *
     * @return The new VFS walker instance
     */
    SwiftWalker newObjectStorageWalker(IRemoteConnectionBuilder remoteConnectionBuilder) {
        if (this.container.isEmpty()) {
            throw new IllegalArgumentException("Missing 'container' property.\nPlease provide a container name.");
        }
        if (!(this.domain.isEmpty() && this.projectId.isEmpty() && this.user.isEmpty() && this.password.isEmpty()) && this.authAddress.isEmpty()) {
            throw new IllegalArgumentException("Missing 'authAddress' property.\nPlease provide authentication address required to access authentication service.");
        }
        return new SwiftWalker(this.address, this.container, this.delimiter, this.fileSystemRoot, remoteConnectionBuilder);
    }

    /**
     * Gets the service address of this VFS provider.
     *
     * @return The VFS service address
     */
    String getProviderAddress() {
        return this.address + this.container;
    }

    String getProviderFileSeparator() {
        return this.delimiter;
    }

    public HttpURLConnection buildConnection(URL url, String method, Map<String, String> requestProperties) throws IOException {
        synchronized (this) {
            if (this.swiftAuthenticationV3 == null) {
                this.swiftAuthenticationV3 = new SwiftAuthenticationV3(this.authAddress, this.domain, this.projectId, this.user, this.password);
            }
        }
        return buildConnection(url, method, requestProperties, this.swiftAuthenticationV3.getAuthorizationToken());
    }
}
