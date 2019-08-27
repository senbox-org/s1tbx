package org.esa.snap.vfs.remote.s3;

import org.esa.snap.vfs.remote.IRemoteConnectionBuilder;
import org.esa.snap.vfs.remote.VFSWalker;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * File System Service Provider for S3 VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public class S3FileSystemProviderHelper {

    /**
     * The default value of delimiter property, used on VFS instance creation parameters.
     */
    private static final String DELIMITER_PROPERTY_DEFAULT_VALUE = "/";

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
     * The name pattern of custom parameters, used on S3 VFS instance creation parameters and defining remote file repository properties.
     */
    private static final String CUSTOM_AWS_HEADER_PROPERTY_NAME_REGEX = "^x-amz-[\\w\\-]+$";

    private String fileSystemRoot;

    private String address;
    private String bucket;
    private String region;
    private String accessKeyId;
    private String secretAccessKey;
    private String delimiter;
    private Map<String, String> customParameters;
    private S3AuthenticationV4 s3AuthenticationV4;

    S3FileSystemProviderHelper(String fileSystemRoot) {
        this.fileSystemRoot = fileSystemRoot;
        this.address = "";
        this.bucket = "";
        this.region = "";
        this.accessKeyId = "";
        this.secretAccessKey = "";
        this.delimiter = DELIMITER_PROPERTY_DEFAULT_VALUE;
        customParameters = new HashMap<>();
        this.s3AuthenticationV4 = null;
    }

    private static HttpURLConnection buildConnection(URL url, String method, Map<String, String> requestProperties, String authorizationToken, Map<String, String> awsRequestProperties) throws IOException {
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
        Pattern customPattern = Pattern.compile(CUSTOM_AWS_HEADER_PROPERTY_NAME_REGEX);
        for (Map.Entry<String, ?> parameter : connectionData.entrySet()) {
            if (customPattern.matcher(parameter.getKey()).matches()) {
                customParameters.put(parameter.getKey(), (String) parameter.getValue());
            }
        }
        setupConnectionData(serviceAddress, newBucket, newRegion, newAccessKeyId, newSecretAccessKey);
    }

    /**
     * Creates the walker instance used by VFS provider to traverse VFS tree.
     *
     * @return The new VFS walker instance
     */
    VFSWalker newObjectStorageWalker(IRemoteConnectionBuilder remoteConnectionBuilder) {
        if (this.bucket.isEmpty()) {
            throw new IllegalArgumentException("Missing 'bucket' property.\nPlease provide a bucket name.");
        }
        return new S3Walker(this.address, this.bucket, this.delimiter, this.fileSystemRoot, remoteConnectionBuilder);
    }

    /**
     * Gets the service address of this VFS provider.
     *
     * @return The VFS service address
     */
    String getProviderAddress() {
        return this.address + this.bucket;
    }

    String getProviderFileSeparator() {
        return this.delimiter;
    }

    public HttpURLConnection buildConnection(URL url, String method, Map<String, String> requestProperties) throws IOException {
        method = method.toUpperCase();
        synchronized (this) {
            if (this.s3AuthenticationV4 == null) {
                this.s3AuthenticationV4 = new S3AuthenticationV4(method, this.region, this.accessKeyId, this.secretAccessKey, this.customParameters);
            }
        }
        return buildConnection(url, method, requestProperties, this.s3AuthenticationV4.getAuthorizationToken(url), this.s3AuthenticationV4.getAwsHeaders(url));
    }
}
