package org.esa.snap.vfs.remote.swift;

import org.apache.commons.io.IOUtils;
import org.esa.snap.vfs.remote.AbstractRemoteWalker;
import org.esa.snap.vfs.remote.HttpUtils;
import org.esa.snap.vfs.remote.IRemoteConnectionBuilder;
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
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Walker for OpenStack Swift VFS.
 *
 * @author Adrian Drăghici
 */
class SwiftWalker extends AbstractRemoteWalker {

    private String address;
    private String container;
    private String delimiter;
    private String root;

    /**
     * Creates the new walker for OpenStack Swift  VFS
     *
     * @param address                 The address of OpenStack Swift service (mandatory)
     * @param container               The container name (bucket) (mandatory)
     * @param delimiter               The VFS path delimiter
     * @param root                    The root of S3 provider
     * @param remoteConnectionBuilder The connection builder
     */
    SwiftWalker(String address, String container, String delimiter, String root, IRemoteConnectionBuilder remoteConnectionBuilder) {
        super(remoteConnectionBuilder);

        this.address = address;
        this.container = container;
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
     * Gets a list of VFS files and directories from to the given prefix.
     *
     * @param dir The VFS path to traverse
     * @return The list of VFS files and directories
     * @throws IOException If an I/O error occurs
     */
    public synchronized List<BasicFileAttributes> walk(VFSPath dir) throws IOException {
        String dirPath = dir.toString();
        String swiftPrefix = buildPrefix(dirPath + (dirPath.endsWith("/") ? "" : "/"));
        String fileSystemRoot = dir.getFileSystem().getRoot().getPath();
        List<BasicFileAttributes> items = new ArrayList<>();
        String marker = "";

        SwiftResponseHandler handler;
        do {
            handler = new SwiftResponseHandler(this.root + this.delimiter + swiftPrefix, items, this.delimiter);
            String swiftURL = buildSwiftURL(swiftPrefix, marker);
            URL url = new URL(swiftURL);
            HttpURLConnection connection = this.remoteConnectionBuilder.buildConnection(fileSystemRoot, url, "GET", null);
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
                    Logger.getLogger(HttpUtils.class.getName()).warning("HTTP error response:");
                    Logger.getLogger(HttpUtils.class.getName()).warning(() -> {
                        try {
                            return IOUtils.toString(connection.getErrorStream(), "UTF-8").replaceAll("<AWSAccessKeyId>.*</AWSAccessKeyId>","<AWSAccessKeyId>***</AWSAccessKeyId>");
                        } catch (IOException ignored) {
                        }
                        return "";
                    });
                    throw new IOException(url.toString() + ": response code " + responseCode + ": " + connection.getResponseMessage());
                }
            } finally {
                connection.disconnect();
            }
            marker = handler.getMarker();
        } while (handler.getIsTruncated());
        return items;
    }

    private String buildPrefix(String prefix) {
        prefix = prefix.replace(this.root, "");
        prefix = prefix.replaceAll("^/", "");
        return prefix;
    }

    private String buildSwiftURL(String prefix, String marker) throws IOException {
        String currentContainer = this.container;
        currentContainer = (currentContainer != null && !currentContainer.isEmpty()) ? currentContainer + this.delimiter : "";
        StringBuilder paramBase = new StringBuilder();
        addParam(paramBase, "format", "xml");
        addParam(paramBase, "limit", "1000");
        addParam(paramBase, "prefix", prefix);
        addParam(paramBase, "delimiter", this.delimiter);
        StringBuilder params = new StringBuilder(paramBase);
        addParam(params, "marker", marker);
        String swiftURL = this.address + (this.address.endsWith(this.delimiter) ? "" : this.delimiter) + currentContainer;
        if (params.length() > 0) {
            swiftURL += "?" + params;
        }
        return swiftURL;
    }
}
