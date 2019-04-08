package org.esa.snap.vfs.remote.swift;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

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

    private String address = "";
    private String authAddress = "";
    private String container = "";
    private String domain = "";
    private String projectId = "";
    private String user = "";
    private String password = "";
    private String delimiter = DELIMITER_PROPERTY_DEFAULT_VALUE;

    public SwiftFileSystemProvider() {
        super();
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
        this.address = address != null ? address : "";
        this.authAddress = authAddress != null ? authAddress : "";
        this.container = container != null ? container : "";
        this.domain = domain != null ? domain : "";
        this.projectId = projectId != null ? projectId : "";
        this.user = user != null ? user : this.user;
        this.password = password != null ? password : "";
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
        try {
            return new SwiftWalker(address, authAddress, container, domain, projectId, user, password, delimiter, fileSystemRoot);
        } catch (ParserConfigurationException | SAXException ex) {
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
        return this.address;
    }

    @Override
    public String getProviderFileSeparator() {
        return this.delimiter;
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
    public HttpURLConnection getProviderConnectionChannel(URL url, String method, Map<String, String> requestProperties) throws IOException {
        return SwiftResponseHandler.getConnectionChannel(url, method, requestProperties, authAddress, domain, projectId, user, password);
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
