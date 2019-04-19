package org.esa.snap.vfs.remote.swift;

import org.esa.snap.core.util.StringUtils;
import org.esa.snap.vfs.remote.VFSFileAttributes;
import org.esa.snap.vfs.remote.VFSWalker;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Walker for OpenStack Swift VFS.
 *
 * @author Adrian Drăghici
 */
class SwiftWalker implements VFSWalker {

    private static Logger logger = Logger.getLogger(VFSWalker.class.getName());

    private XMLReader xmlReader;

    private String address;
    private String authAddress;
    private String container;
    private String domain;
    private String projectId;
    private String user;
    private String password;
    private String delimiter;
    private String root;

    /**
     * Creates the new walker for OpenStack Swift  VFS
     *
     * @param address     The address of OpenStack Swift service (mandatory)
     * @param authAddress The address of authentication service used by OpenStack Swift service (mandatory if credentials is provided)
     * @param container   The container name (bucket) (mandatory)
     * @param domain      The domain name OpenStack Swift credential
     * @param projectId   The account ID/ Project/ Tenant name OpenStack Swift credential
     * @param user        The username OpenStack Swift credential
     * @param password    The password OpenStack Swift credential
     * @param delimiter   The VFS path delimiter
     * @param root        The root of S3 provider
     * @throws ParserConfigurationException If an serious configuration error occurs
     * @throws SAXException                 Any SAX exception, possibly wrapping another exception
     */
    SwiftWalker(String address, String authAddress, String container, String domain, String projectId, String user, String password, String delimiter, String root) throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        xmlReader = saxParser.getXMLReader();
        this.address = address;
        this.authAddress = authAddress;
        this.container = container;
        this.domain = domain;
        this.projectId = projectId;
        this.user = user;
        this.password = password;
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

    private static boolean isValidResponseCode(int responseCode) {
        return (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE);
    }

    /**
     * Gets the VFS file basic attributes.
     *
     * @param address The VFS service address
     * @param prefix  The VFS path to traverse
     * @return The OpenStack Swift file basic attributes
     * @throws IOException If an I/O error occurs
     */
    public BasicFileAttributes getVFSBasicFileAttributes(String address, String prefix) throws IOException {
        HttpURLConnection connection = SwiftResponseHandler.buildConnection(new URL(address + (address.endsWith("/") ? "" : "/")), "GET", null, authAddress, domain, projectId, user, password);
        try {
            int responseCode = connection.getResponseCode();
            if (isValidResponseCode(responseCode)) {
                // the address represents a directory
                return VFSFileAttributes.newDir(prefix);
            }
        } finally {
            connection.disconnect();
        }
        // the address does not represent a directory
        return getVFSFileAttributes(address, prefix);
    }

    private BasicFileAttributes getVFSFileAttributes(String address, String prefix) throws IOException {
        HttpURLConnection connection = SwiftResponseHandler.buildConnection(new URL(address), "GET", null, authAddress, domain, projectId, user, password);
        try {
            int responseCode = connection.getResponseCode();
            if (isValidResponseCode(responseCode)) {
                String sizeString = connection.getHeaderField("content-length");
                String lastModified = connection.getHeaderField("last-modified");
                if (!StringUtils.isNotNullAndNotEmpty(sizeString) && StringUtils.isNotNullAndNotEmpty(lastModified)) {
                    throw new IOException("filePath is not a file '" + prefix + "'.");
                }
                long size = Long.parseLong(sizeString);
                return VFSFileAttributes.newFile(prefix, size, lastModified);
            } else {
                throw new IOException(address + ": response code " + responseCode + ": " + connection.getResponseMessage());
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Gets a list of VFS files and directories from to the given prefix.
     *
     * @param dir The VFS path to traverse
     * @return The list of VFS files and directories
     * @throws IOException If an I/O error occurs
     */
    public synchronized List<BasicFileAttributes> walk(Path dir) throws IOException {
        String prefix = dir.toString();
        String swiftPrefix = buildPrefix(prefix + (prefix.endsWith("/") ? "" : "/"));
        ArrayList<BasicFileAttributes> items = new ArrayList<>();
        String marker = "";
        SwiftResponseHandler handler;
        do {
            handler = new SwiftResponseHandler(root + delimiter + swiftPrefix, items, delimiter);
            xmlReader.setContentHandler(handler);
            String swiftURL = buildSwiftURL(swiftPrefix, marker);
            try {
                HttpURLConnection connection = SwiftResponseHandler.getConnectionChannel(new URL(swiftURL), "GET", null, authAddress, domain, projectId, user, password);
                try {
                    try (InputStream input = connection.getInputStream()) {
                        xmlReader.parse(new InputSource(input));
                    }
                } finally {
                    connection.disconnect();
                }
            } catch (SAXException ex) {
                logger.log(Level.SEVERE, "Unable to get a list of OpenStack Swift VFS files and directories from to the given prefix. Details: " + ex.getMessage());
                throw new IOException(ex);
            }
            marker = handler.getMarker();
        } while (handler.getIsTruncated());
        return items;
    }

    private String buildPrefix(String prefix) {
        prefix = prefix.replace(root, "");
        prefix = prefix.replaceAll("^/", "");
        return prefix;
    }

    private String buildSwiftURL(String prefix, String marker) throws IOException {
        String currentContainer = container;
        currentContainer = (currentContainer != null && !currentContainer.isEmpty()) ? currentContainer + delimiter : "";
        StringBuilder paramBase = new StringBuilder();
        addParam(paramBase, "format", "xml");
        addParam(paramBase, "limit", "1000");
        addParam(paramBase, "prefix", prefix);
        addParam(paramBase, "delimiter", delimiter);
        StringBuilder params = new StringBuilder(paramBase);
        addParam(params, "marker", marker);
        String swiftURL = address + (address.endsWith(delimiter) ? "" : delimiter) + currentContainer;
        if (params.length() > 0) {
            swiftURL += "?" + params;
        }
        return swiftURL;
    }

}
