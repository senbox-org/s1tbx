package org.esa.snap.vfs.remote.s3;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;
import org.esa.snap.vfs.remote.VFSWalker;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

/**
 * File System Service Provider for S3 VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public class S3FileSystemProvider extends AbstractRemoteFileSystemProvider {

    /**
     * The name of access key ID property, used on S3 VFS instance creation parameters and defining remote file repository properties.
     */
    private static final String ACCESS_KEY_ID_PROPERTY_NAME = "accessKeyId";

    /**
     * The name of secret access key property, used on S3 VFS instance creation parameters and defining remote file repository properties.
     */
    private static final String SECRET_ACCESS_KEY_PROPERTY_NAME = "secretAccessKey";

    /**
     * The value of S3 provider scheme.
     */
    private static final String SCHEME = "s3";

    private String bucketAddress = "";
    private String accessKeyId = "";
    private String secretAccessKey = "";
    private String delimiter = DELIMITER_PROPERTY_DEFAULT_VALUE;

    public S3FileSystemProvider() {
        super();
    }

    /**
     * Creates the authorization token used for S3 authentication.
     *
     * @param accessKeyId     The access key id S3 credential (username)
     * @param secretAccessKey The secret access key S3 credential (password)
     * @return The authorization token
     */
    private static String getAuthorizationToken(String accessKeyId, String secretAccessKey) {//not real S3 authentication - only for function definition
        try {
            if (accessKeyId == null || accessKeyId.isEmpty() || secretAccessKey == null || secretAccessKey.isEmpty()) {
                throw new NullPointerException();
            }
            String data = "";
            SecretKeySpec signingKey = new SecretKeySpec(secretAccessKey.getBytes(), "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            byte[] macSignature = mac.doFinal(data.getBytes());
            String signature = Base64.getEncoder().encodeToString(macSignature);
            return "AWS " + accessKeyId + ":" + signature;
        } catch (Exception e) {
            return null;
        }
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
        String newAccessKeyId = (String) connectionData.get(ACCESS_KEY_ID_PROPERTY_NAME);
        String newSecretAccessKey = (String) connectionData.get(SECRET_ACCESS_KEY_PROPERTY_NAME);
        setupConnectionData(serviceAddress, newAccessKeyId, newSecretAccessKey);
    }

    @Override
    protected S3FileSystem newFileSystem(String fileSystemRoot, Map<String, ?> env) {
        return new S3FileSystem(this, fileSystemRoot);
    }

    /**
     * Creates the walker instance used by VFS provider to traverse VFS tree.
     *
     * @return The new VFS walker instance
     */
    @Override
    protected VFSWalker newObjectStorageWalker(String fileSystemRoot) {
        return new S3Walker(bucketAddress, delimiter, fileSystemRoot, this);
    }

    /**
     * Gets the service address of this VFS provider.
     *
     * @return The VFS service address
     */
    @Override
    public String getProviderAddress() {
        return this.bucketAddress;
    }

    @Override
    public String getProviderFileSeparator() {
        return this.delimiter;
    }

    @Override
    public HttpURLConnection buildConnection(URL url, String method, Map<String, String> requestProperties) throws IOException {
        String authorizationToken = getAuthorizationToken(accessKeyId, secretAccessKey);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setDoInput(true);
        method = method.toUpperCase();
        if (method.equals("POST") || method.equals("PUT") || method.equals("DELETE")) {
            connection.setDoOutput(true);
        }
        if (authorizationToken != null && !authorizationToken.isEmpty())
            connection.setRequestProperty("Authorization ", "Basic " + authorizationToken);
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
     * Gets the URI scheme that identifies this VFS provider.
     *
     * @return The URI scheme
     */
    @Override
    public String getScheme() {
        return SCHEME;
    }
}
