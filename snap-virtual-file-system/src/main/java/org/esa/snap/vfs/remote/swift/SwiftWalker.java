package org.esa.snap.vfs.remote.swift;

import org.esa.snap.vfs.remote.ObjectStorageFileAttributes;
import org.esa.snap.vfs.remote.ObjectStorageWalker;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Walker for OpenStack Swift Object Storage VFS.
 *
 * @author Adrian Drăghici
 */
class SwiftWalker implements ObjectStorageWalker {

    private static Logger logger = Logger.getLogger(ObjectStorageWalker.class.getName());

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
     * Creates the new walker for OpenStack Swift Object Storage VFS
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
     * Gets the VFS file basic attributes.
     *
     * @param address The VFS service address
     * @param prefix  The VFS path to traverse
     * @return The OpenStack Swift file basic attributes
     * @throws IOException If an I/O error occurs
     */
    public BasicFileAttributes getObjectStorageFile(String address, String prefix) throws IOException {
        URLConnection urlConnection = SwiftResponseHandler.getConnectionChannel(new URL(address), "GET", null, authAddress, domain, projectId, user, password);
        return ObjectStorageFileAttributes.newFile(prefix, urlConnection.getContentLengthLong(), urlConnection.getHeaderField("last-modified"));
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
    public List<BasicFileAttributes> walk(Path dir) throws IOException {
        String prefix = dir.toString();
        String currentContainer;
        if (container != null && !container.isEmpty()) {
            currentContainer = container;
        } else {
            String[] parts = prefix.split(delimiter);
            if (prefix.startsWith(root)) {
                currentContainer = parts.length > 1 ? parts[1] : "";
            } else {
                currentContainer = parts.length > 0 ? parts[0] : "";
            }
        }
        currentContainer = (currentContainer != null && !currentContainer.isEmpty()) ? currentContainer + delimiter : "";
        prefix = prefix.replace(root, "");
        prefix = prefix.replace(currentContainer, "");
        StringBuilder paramBase = new StringBuilder();
        addParam(paramBase, "format", "xml");
        addParam(paramBase, "limit", "1000");
        addParam(paramBase, "prefix", prefix);
        addParam(paramBase, "delimiter", delimiter);

        ArrayList<BasicFileAttributes> items = new ArrayList<>();
        String marker = "";
        SwiftResponseHandler handler;
        do {
            handler = new SwiftResponseHandler(root + currentContainer + prefix, items, delimiter);
            xmlReader.setContentHandler(handler);
            StringBuilder params = new StringBuilder(paramBase);
            addParam(params, "marker", marker);
            String systemId = address + (address.endsWith(delimiter) ? "" : delimiter) + currentContainer;
            if (params.length() > 0) {
                systemId += "?" + params;
            }
            try {
                xmlReader.parse(new InputSource(SwiftResponseHandler.getConnectionChannel(new URL(systemId), "GET", null, authAddress, domain, projectId, user, password).getInputStream()));
            } catch (SAXException ex) {
                logger.log(Level.SEVERE, "Unable to get a list of OpenStack Swift VFS files and directories from to the given prefix. Details: " + ex.getMessage());
                throw new IOException(ex);
            }
            marker = handler.getMarker();
        } while (handler.getIsTruncated());
        return items;
    }

}
