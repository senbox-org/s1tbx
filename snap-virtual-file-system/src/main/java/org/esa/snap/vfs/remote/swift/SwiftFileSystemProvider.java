package org.esa.snap.vfs.remote.swift;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
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
 * File System Service Provider for OpenStack Swift Object Storage VFS.
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
     * The default value of root property, used on VFS instance creation parameters.
     */
    private static final String SWIFT_ROOT = "OpenStack-Swift:/";

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

    public SwiftFileSystemProvider() {
        super(SWIFT_ROOT);
    }
        /**
         * Gets the OpenStack Swift Virtual File System without authentication.
         *
         * @param address   The address of OpenStack Swift service (mandatory)
         * @param container The container name (bucket) (mandatory)
         * @return The new OpenStack Swift Virtual File System
         * @throws IOException If an I/O error occurs
         * @link https://developer.openstack.org/api-ref/object-store/
         */
    public static FileSystem getSwiftFileSystem(String address, String container) throws IOException {
        return getSwiftFileSystem(address, "", container, "", "", "", "");
    }

    /**
     * Gets OpenStack Swift Virtual File System with authentication.
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
    public static FileSystem getSwiftFileSystem(String address, String authAddress, String container, String domain, String projectId, String user, String password) throws IOException {
        FileSystem fs = null;
        URI uri;
        try {
            uri = new URI(SCHEME + ":" + address);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Invalid URI for OpenStack Swift VFS. Details: " + ex.getMessage());
            throw new IOException(ex);
        }
        try {
            fs = FileSystems.getFileSystem(uri);
        } catch (Exception ex) {
            logger.log(Level.FINE, "OpenStack Swift VFS not loaded. Details: " + ex.getMessage());
        } finally {
            if (fs != null) {
                fs.close();
            }
        }
        try {
            Map<String, String> env = new HashMap<>();
            env.put(AUTH_ADDRESS_PROPERTY_NAME, authAddress);
            env.put(CONTAINER_PROPERTY_NAME, container);
            env.put(DOMAIN_PROPERTY_NAME, domain);
            env.put(PROJECT_ID_PROPERTY_NAME, projectId);
            env.put(USER_PROPERTY_NAME, user);
            env.put(CREDENTIAL_PROPERTY_NAME, password);
            fs = FileSystems.newFileSystem(uri, env, SwiftFileSystemProvider.class.getClassLoader());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to initialize OpenStack Swift VFS. Details: " + ex.getMessage());
            throw new AccessDeniedException(ex.getMessage());
        }
        return fs;
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
        String newRoot = (String) connectionData.get(ROOT_PROPERTY_NAME);
        root = newRoot != null && !newRoot.isEmpty() ? newRoot : SWIFT_ROOT;
        String newAuthAddress = (String) connectionData.get(AUTH_ADDRESS_PROPERTY_NAME);
        String newContainer = (String) connectionData.get(CONTAINER_PROPERTY_NAME);
        String newDomain = (String) connectionData.get(DOMAIN_PROPERTY_NAME);
        String newProjectId = (String) connectionData.get(PROJECT_ID_PROPERTY_NAME);
        String newUser = (String) connectionData.get(USER_PROPERTY_NAME);
        String newCredential = (String) connectionData.get(CREDENTIAL_PROPERTY_NAME);
        setupConnectionData(serviceAddress, newAuthAddress, newContainer, newDomain, newProjectId, newUser, newCredential);
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
    protected SwiftFileSystem newFileSystem(String address, Map<String, ?> env) throws IOException {
        return new SwiftFileSystem(this, address, delimiter);
    }

    /**
     * Creates the walker instance used by VFS provider to traverse VFS tree.
     *
     * @return The new VFS walker instance
     */
    protected SwiftWalker newObjectStorageWalker() {
        if (this.container.isEmpty()) {
            throw new IllegalArgumentException("Missing 'container' property.\nPlease provide a container name.");
        }
        if (!(this.domain.isEmpty() && this.projectId.isEmpty() && this.user.isEmpty() && this.password.isEmpty()) && this.authAddress.isEmpty()) {
            throw new IllegalArgumentException("Missing 'authAddress' property.\nPlease provide authentication address required to access authentication service.");
        }
        try {
            return new SwiftWalker(address, authAddress, container, domain, projectId, user, password, delimiter, root);
        } catch (ParserConfigurationException | SAXException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Gets the service address of this VFS provider.
     *
     * @return The VFS service address
     */
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
