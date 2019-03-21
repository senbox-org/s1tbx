package org.esa.snap.core.dataio.vfs.remote.object_storage.aws;

import org.esa.snap.core.dataio.vfs.remote.object_storage.ObjectStorageFileSystem;
import org.esa.snap.core.dataio.vfs.remote.object_storage.ObjectStorageFileSystemProvider;
import org.esa.snap.core.dataio.vfs.remote.object_storage.ObjectStorageWalker;
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
 * File System Service Provider for S3 Object Storage VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public class S3FileSystemProvider extends ObjectStorageFileSystemProvider {

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
     * The name of access key ID property, used on S3 VFS instance creation parameters and defining remote file repository properties.
     */
    private static final String ACCESS_KEY_ID_PROPERTY_NAME = "accessKeyId";

    /**
     * The name of secret access key property, used on S3 VFS instance creation parameters and defining remote file repository properties.
     */
    private static final String SECRET_ACCESS_KEY_PROPERTY_NAME = "secretAccessKey";

    /**
     * The default value of root property, used on VFS instance creation parameters.
     */
    private static final String S3_ROOT = "S3:/";

    /**
     * The value of S3 provider scheme.
     */
    private static final String SCHEME = "s3";

    private static Logger logger = Logger.getLogger(S3FileSystemProvider.class.getName());

    private String bucketAddress = "";
    private String accessKeyId = "";
    private String secretAccessKey = "";
    private String delimiter = DELIMITER_PROPERTY_DEFAULT_VALUE;

    private String root = S3_ROOT;

    /**
     * Gets the S3 Virtual File System without authentication.
     *
     * @param bucketAddress The address of S3 service. (mandatory)
     * @return The new S3 Virtual File System
     * @throws IOException If an I/O error occurs
     */
    public static FileSystem getS3FileSystem(String bucketAddress) throws IOException {
        return getS3FileSystem(bucketAddress, "", "");
    }

    /**
     * Creates the S3 Virtual File System with authentication.
     *
     * @param bucketAddress   The address of S3 service. (mandatory)
     * @param accessKeyId     The access key id S3 credential (username)
     * @param secretAccessKey The secret access key S3 credential (password)
     * @return The new S3 Virtual File System
     * @throws IOException If an I/O error occurs
     */
    public static FileSystem getS3FileSystem(String bucketAddress, String accessKeyId, String secretAccessKey) throws IOException {
        FileSystem fs = null;
        URI uri;
        try {
            uri = new URI(SCHEME + ":" + bucketAddress);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Invalid URI for S3 VFS. Details: " + ex.getMessage());
            throw new IOException(ex);
        }
        try {
            fs = FileSystems.getFileSystem(uri);
        } catch (Exception ex) {
            logger.log(Level.FINE, "S3 VFS not loaded. Details: " + ex.getMessage());
        } finally {
            if (fs != null) {
                fs.close();
            }
        }
        try {
            Map<String, String> env = new HashMap<>();
            env.put(ACCESS_KEY_ID_PROPERTY_NAME, accessKeyId);
            env.put(SECRET_ACCESS_KEY_PROPERTY_NAME, secretAccessKey);
            fs = FileSystems.newFileSystem(uri, env, S3FileSystemProvider.class.getClassLoader());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to initialize S3 VFS. Details: " + ex.getMessage());
            throw new AccessDeniedException(ex.getMessage());
        }
        return fs;
    }

    /**
     * Save connection data on this provider.
     *
     * @param bucketAddress   The bucket Address
     * @param accessKeyId     The access key id S3 credential (username)
     * @param secretAccessKey The secret access key S3 credential (password)
     */
    private void setupConnectionData(String bucketAddress, String accessKeyId, String secretAccessKey) {
        this.bucketAddress = bucketAddress != null ? bucketAddress : "";
        this.accessKeyId = accessKeyId != null ? accessKeyId : "";
        this.secretAccessKey = secretAccessKey != null ? secretAccessKey : "";
    }

    public void setConnectionData(String serviceAddress, Map<String, ?> connectionData) {
        String newRoot = (String) connectionData.get(ROOT_PROPERTY_NAME);
        root = newRoot != null && !newRoot.isEmpty() ? newRoot : S3_ROOT;
        String newAccessKeyId = (String) connectionData.get(ACCESS_KEY_ID_PROPERTY_NAME);
        String newSecretAccessKey = (String) connectionData.get(SECRET_ACCESS_KEY_PROPERTY_NAME);
        setupConnectionData(serviceAddress, newAccessKeyId, newSecretAccessKey);
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
            logger.log(Level.SEVERE, "Unable to create new S3 VFS instance. Details: " + ex.getMessage());
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
            return new S3Walker(bucketAddress, accessKeyId, secretAccessKey, delimiter, root);
        } catch (ParserConfigurationException | SAXException ex) {
            logger.log(Level.SEVERE, "Unable to create walker instance used by S3 VFS to traverse tree. Details: " + ex.getMessage());
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Gets the service address of this VFS provider.
     *
     * @return The VFS service address
     */
    public String getProviderAddress() {
        return bucketAddress;
    }

    /**
     * Gets the root of this VFS provider.
     *
     * @return The root
     */
    @Override
    public String getRoot() {
        return root != null && !root.isEmpty() ? root : S3_ROOT;
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
        return S3ResponseHandler.getConnectionChannel(url, method, requestProperties, accessKeyId, secretAccessKey);
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
