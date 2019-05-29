package org.esa.snap.vfs.remote.s3;

import org.esa.snap.vfs.remote.AbstractRemoteWalker;
import org.esa.snap.vfs.remote.HttpUtils;
import org.esa.snap.vfs.remote.IRemoteConnectionBuilder;
import org.esa.snap.vfs.remote.VFSFileAttributes;
import org.esa.snap.vfs.remote.VFSPath;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Walker for S3 VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
class S3Walker extends AbstractRemoteWalker {

    private String address;
    private String bucket;
    private String delimiter;
    private String root;

    /**
     * Creates the new walker for S3  VFS
     *
     * @param address                 The address of S3 service. (mandatory)
     * @param bucket                  The bucket name (mandatory)
     * @param delimiter               The VFS path delimiter
     * @param root                    The root of S3 provider
     * @param remoteConnectionBuilder The connection builder
     */
    S3Walker(String address, String bucket, String delimiter, String root, IRemoteConnectionBuilder remoteConnectionBuilder) {
        super(remoteConnectionBuilder);

        this.address = address;
        this.bucket = bucket;
        this.delimiter = delimiter;
        this.root = root;
    }

    /**
     * Append the new request parameter represented by name and value to the request parameters builder.
     *
     * @param params The request parameters builder
     * @param name   The name of new request parameter
     * @param value  The value of new request parameter
     * @throws IOException If an I/O error occurs
     */
    private static void addParam(StringBuilder params, String name, String value) throws IOException {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (params.length() > 0) {
            params.append("&");
        }
        params.append(name).append("=").append(URLEncoder.encode(value, "UTF8"));
    }

    /**
     * Gets the VFS file basic attributes.
     *
     * @return The S3 file basic attributes
     * @throws IOException If an I/O error occurs
     */
    @Override
    public BasicFileAttributes readBasicFileAttributes(VFSPath path) throws IOException {
        String s3Path = path.toString();
        String s3Prefix = buildPrefix(s3Path + (s3Path.endsWith("/") ? "" : "/"));
        String s3URL = buildS3URL(s3Prefix, "");
        String fileSystemSeparator = path.getFileSystem().getSeparator();
        URL directoryURL = new URL(s3URL + (s3URL.endsWith(fileSystemSeparator) ? "" : fileSystemSeparator));
        HttpURLConnection connection = this.remoteConnectionBuilder.buildConnection(directoryURL, "GET", null);
        try {
            int responseCode = connection.getResponseCode();
            if (HttpUtils.isValidResponseCode(responseCode)) {
                // the address represents a directory
                return VFSFileAttributes.newDir(path.toString());
            }
        } finally {
            connection.disconnect();
        }
        // the address does not represent a directory
        s3URL = path.buildURL().toString();
        return readFileAttributes(s3URL, path.toString());
    }

    /**
     * Gets a list of VFS files and directories from to the given prefix.
     *
     * @param dir The VFS path to traverse
     * @return The list of VFS files and directories
     * @throws IOException If an I/O error occurs
     */
    @Override
    public synchronized List<BasicFileAttributes> walk(Path dir) throws IOException {
        String dirPath = dir.toString();
        String s3Prefix = buildPrefix(dirPath + (dirPath.endsWith("/") ? "" : "/"));
        List<BasicFileAttributes> items = new ArrayList<>();
        String nextContinuationToken = "";

        S3ResponseHandler handler;
        do {
            handler = new S3ResponseHandler(root + delimiter + s3Prefix, items, delimiter);
            String s3URL = buildS3URL(s3Prefix, nextContinuationToken);
            URL url = new URL(s3URL);
            HttpURLConnection connection = this.remoteConnectionBuilder.buildConnection(url, "GET", null);
            try {
                int responseCode = connection.getResponseCode();
                if (HttpUtils.isValidResponseCode(responseCode)) {
                    try (InputStream inputStream = connection.getInputStream();
                         BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, 10 * 1024)) {

                        try {
                            SAXParserFactory spf = SAXParserFactory.newInstance();
                            spf.setNamespaceAware(true);
                            SAXParser saxParser = spf.newSAXParser();
                            XMLReader xmlReader = saxParser.getXMLReader();
                            xmlReader.setContentHandler(handler);
                            xmlReader.parse(new InputSource(bufferedInputStream));
                        } catch (SAXException | ParserConfigurationException ex) {
                            throw new IOException(ex);
                        }
                    }
                } else {
                    throw new IOException(url.toString() + ": response code " + responseCode + ": " + connection.getResponseMessage());
                }
            } finally {
                connection.disconnect();
            }
            nextContinuationToken = handler.getNextContinuationToken();
        } while (handler.getIsTruncated());

        return items;
    }

    private String buildPrefix(String prefix) {
        prefix = prefix.replace(root, "");
        prefix = prefix.replaceAll("^/", "");
        return prefix;
    }

    private String buildS3URL(String prefix, String nextContinuationToken) throws IOException {
        String currentBucket = this.bucket;
        currentBucket = (currentBucket != null && !currentBucket.isEmpty()) ? currentBucket + delimiter : "";
        StringBuilder paramBase = new StringBuilder();
        addParam(paramBase, "prefix", prefix);
        addParam(paramBase, "delimiter", delimiter);
        StringBuilder params = new StringBuilder(paramBase);
        addParam(params, "continuation-token", nextContinuationToken);
        String s3URL = address + (address.endsWith(delimiter) ? "" : delimiter) + currentBucket;
        if (params.length() > 0) {
            s3URL += "?" + params;
        }
        return s3URL;
    }
}
