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

/**
 * File System Service Provider for Amazon AWS S3 Object Storage VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public class S3FileSystemProvider extends ObjectStorageFileSystemProvider {

    public static final String S3_ROOT = "AWS-S3:/";
    private final static String SCHEME = "s3";
    private static String bucketAddress = "https://s3.amazonaws.com";
    private static String awsAccessKeyId = "";
    private static String awsSecretAccessKey = "";
    private static String delimiter = "/";

    static String getAwsAccessKeyId() {
        return awsAccessKeyId;
    }

    static String getAwsSecretAccessKey() {
        return awsSecretAccessKey;
    }

    public static FileSystem getS3FileSystem() throws AccessDeniedException {
        FileSystem fs = null;
        try {
            URI uri = new URI(new S3FileSystemProvider().getScheme() + ":" + new S3FileSystemProvider().getProviderAddress());
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
     * Setup this provider with connection data for connect to AWS S3 Service.
     *
     * @param bucketAddress      Bucket Address. (mandatory)
     * @param awsAccessKeyId     Username for login to AWS S3 Service.
     * @param awsSecretAccessKey Password for login to AWS S3 Service.
     * @link https://docs.aws.amazon.com/AmazonS3/latest/dev/MakingRequests.html
     */
    public static void setupConnectionData(String bucketAddress, String awsAccessKeyId, String awsSecretAccessKey) {
        S3FileSystemProvider.bucketAddress = bucketAddress != null ? bucketAddress : S3FileSystemProvider.bucketAddress;
        S3FileSystemProvider.awsAccessKeyId = awsAccessKeyId != null ? awsAccessKeyId : S3FileSystemProvider.awsAccessKeyId;
        S3FileSystemProvider.awsSecretAccessKey = awsSecretAccessKey != null ? awsSecretAccessKey : S3FileSystemProvider.awsSecretAccessKey;
    }

    @Override
    protected ObjectStorageFileSystem newFileSystem(String address, Map<String, ?> env) throws IOException {
        Object delimiter = env.get("delimiter");
        try {
            return new ObjectStorageFileSystem(this,
                    address,
                    delimiter != null && !delimiter.toString().isEmpty() ? delimiter.toString() : S3FileSystemProvider.delimiter
            );
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    @Override
    protected ObjectStorageWalker newObjectStorageWalker() {
        try {
            return new S3Walker();
        } catch (ParserConfigurationException | SAXException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getProviderAddress() {
        return bucketAddress;
    }

    @Override
    public String getRoot() {
        return S3_ROOT;
    }

    public URLConnection getProviderConnectionChannel(URL url, String method, Map<String, String> requestProperties) throws IOException {
        return S3ResponseHandler.getConnectionChannel(url, method, requestProperties);
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
     * Setup this provider with custom separator for AWS S3 Virtual File System.
     *
     * @param delimiter AWS S3 Virtual File System separator.
     */
    public static void setDelimiter(String delimiter) {
        S3FileSystemProvider.delimiter = delimiter != null && !delimiter.isEmpty() ? delimiter : S3FileSystemProvider.delimiter;
    }

}
