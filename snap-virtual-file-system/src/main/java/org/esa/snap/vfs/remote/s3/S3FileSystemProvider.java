package org.esa.snap.vfs.remote.s3;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;
import org.esa.snap.vfs.remote.VFSWalker;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
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
     * The name of bucket property, used on S3 VFS instance creation parameters and defining remote file repository properties.
     */
    private static final String BUCKET_PROPERTY_NAME = "bucket";

    /**
     * The name of AWS Region property, used on S3 VFS instance creation parameters and defining remote file repository properties.
     */
    private static final String AWS_REGION_PROPERTY_NAME = "region";

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

    private String address = "";
    private String bucket = "";
    private String region = "";
    private String accessKeyId = "";
    private String secretAccessKey = "";
    private String delimiter = DELIMITER_PROPERTY_DEFAULT_VALUE;
    private S3AuthenticationV4 s3AuthenticationV4 = null;

    public S3FileSystemProvider() {
        super();
    }

    /**
     * Save connection data on this provider.
     *
     * @param address         The address of AWS S3 service
     * @param bucket          The bucket name
     * @param region          The region of AWS S3 service
     * @param accessKeyId     The access key id S3 credential (username)
     * @param secretAccessKey The secret access key S3 credential (password)
     */
    private void setupConnectionData(String address, String bucket, String region, String accessKeyId, String secretAccessKey) {
        this.address = address != null ? address : this.address;
        this.bucket = bucket != null ? bucket : this.bucket;
        this.region = region != null ? region : this.region;
        this.accessKeyId = accessKeyId != null ? accessKeyId : this.accessKeyId;
        this.secretAccessKey = secretAccessKey != null ? secretAccessKey : this.secretAccessKey;
    }

    public void setConnectionData(String serviceAddress, Map<String, ?> connectionData) {
        String newRegion = (String) connectionData.get(AWS_REGION_PROPERTY_NAME);
        String newBucket = (String) connectionData.get(BUCKET_PROPERTY_NAME);
        String newAccessKeyId = (String) connectionData.get(ACCESS_KEY_ID_PROPERTY_NAME);
        String newSecretAccessKey = (String) connectionData.get(SECRET_ACCESS_KEY_PROPERTY_NAME);
        setupConnectionData(serviceAddress, newBucket, newRegion, newAccessKeyId, newSecretAccessKey);
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
        if (this.bucket.isEmpty()) {
            throw new IllegalArgumentException("Missing 'bucket' property.\nPlease provide a bucket name.");
        }
        return new S3Walker(this.address, this.bucket, this.delimiter, fileSystemRoot, this);
    }

    /**
     * Gets the service address of this VFS provider.
     *
     * @return The VFS service address
     */
    @Override
    public String getProviderAddress() {
        return this.address + this.bucket;
    }

    @Override
    public String getProviderFileSeparator() {
        return this.delimiter;
    }

    @Override
    public HttpURLConnection buildConnection(URL url, String method, Map<String, String> requestProperties) throws IOException {
        method = method.toUpperCase();
        if (this.s3AuthenticationV4 == null) {
            this.s3AuthenticationV4 = new S3AuthenticationV4(method, this.region, this.accessKeyId, this.secretAccessKey);
        }
        String authorizationToken = this.s3AuthenticationV4.getAuthorizationToken(url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setDoInput(true);
        if (method.equals("POST") || method.equals("PUT") || method.equals("DELETE")) {
            connection.setDoOutput(true);
        }
        if (authorizationToken != null && !authorizationToken.isEmpty())
            connection.setRequestProperty("Authorization", authorizationToken);
        if (requestProperties != null && requestProperties.size() > 0) {
            Set<Map.Entry<String, String>> requestPropertiesSet = requestProperties.entrySet();
            for (Map.Entry<String, String> requestProperty : requestPropertiesSet) {
                connection.setRequestProperty(requestProperty.getKey(), requestProperty.getValue());
            }
        }
        Map<String, String> awsRequestProperties = this.s3AuthenticationV4.getAwsHeaders();
        if (awsRequestProperties != null && awsRequestProperties.size() > 0) {
            Set<Map.Entry<String, String>> awsRequestPropertiesSet = awsRequestProperties.entrySet();
            for (Map.Entry<String, String> awsRequestProperty : awsRequestPropertiesSet) {
                connection.setRequestProperty(awsRequestProperty.getKey(), awsRequestProperty.getValue());
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
