package org.esa.snap.core.dataio.vfs.remote.object_storage.swift;

import org.esa.snap.core.dataio.vfs.remote.object_storage.ObjectStorageFileSystem;
import org.esa.snap.core.dataio.vfs.remote.object_storage.ObjectStorageFileSystemProvider;
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

/**
 * File System Service Provider for EO Cloud OpenStack Swift Object Storage VFS.
 *
 * @author Adrian Drăghici
 */
public class SwiftFileSystemProvider extends ObjectStorageFileSystemProvider {

    public static final String SWIFT_ROOT = "EOCloud-OpenStack-Swift:/";
    private final static String ADDRESS = "https://eocloud.eu:8080/swift/v1/";
    private final static String AUTH_ADDRESS = "https://eocloud.eu:5000/v3/auth/tokens/";
    private final static String SCHEME = "oss";
    private static String container = "";
    private static String domain = "";
    private static String projectId = "";
    private static String user = "";
    private static String password = "";
    private static String delimiter = "/";

    static String getContainer() {
        return container;
    }

    static String getDomain() {
        return domain;
    }

    static String getProjectId() {
        return projectId;
    }

    static String getUser() {
        return user;
    }

    static String getPassword() {
        return password;
    }

    public static FileSystem getSwiftFileSystem() throws AccessDeniedException {
        FileSystem fs = null;
        try {
            URI uri = new URI(new SwiftFileSystemProvider().getScheme() + ":" + new SwiftFileSystemProvider().getProviderAddress());
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
     * Setup this provider with connection data for connect to EO Cloud OpenStack Swift Service.
     *
     * @param container Container name (bucket).
     * @param domain    Domain name.
     * @param projectId Account ID/ Project/ Tenant name.
     * @param user      Username for login to EO Cloud OpenStack Object Storage Swift service.
     * @param password  Password for login to EO Cloud OpenStack Object Storage Swift Service.
     * @link https://developer.openstack.org/api-ref/object-store/
     */
    public static void setupConnectionData(String container, String domain, String projectId, String user, String password) {
        SwiftFileSystemProvider.container = container != null ? container : SwiftFileSystemProvider.container;
        SwiftFileSystemProvider.domain = domain != null ? domain : SwiftFileSystemProvider.domain;
        SwiftFileSystemProvider.projectId = projectId != null ? projectId : SwiftFileSystemProvider.projectId;
        SwiftFileSystemProvider.user = user != null ? user : SwiftFileSystemProvider.user;
        SwiftFileSystemProvider.password = password != null ? password : SwiftFileSystemProvider.password;
    }

    protected ObjectStorageFileSystem newFileSystem(String address, Map<String, ?> env) throws IOException {
        Object delimiter = env.get("delimiter");
        try {
            return new ObjectStorageFileSystem(this,
                    address,
                    delimiter != null && !delimiter.toString().isEmpty() ? delimiter.toString() : SwiftFileSystemProvider.delimiter
            );
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    protected SwiftWalker newObjectStorageWalker() {
        try {
            return new SwiftWalker();
        } catch (ParserConfigurationException | SAXException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getProviderAddress() {
        return ADDRESS;
    }

    @Override
    public String getRoot(){
        return SWIFT_ROOT;
    }

    String getProviderAuthAddress() {
        return AUTH_ADDRESS;
    }

    public URLConnection getProviderConnectionChannel(URL url, String method, Map<String, String> requestProperties) throws IOException {
        return SwiftResponseHandler.getConnectionChannel(url, method, requestProperties);
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
        SwiftFileSystemProvider.delimiter = delimiter != null && !delimiter.isEmpty() ? delimiter : SwiftFileSystemProvider.delimiter;
    }

}
