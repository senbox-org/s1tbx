package org.esa.snap.core.dataio.vfs.remote.object_storage.swift;

import org.esa.snap.core.dataio.vfs.remote.object_storage.ObjectStorageFileAttributes;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Response Handler for HTTP Object Storage VFS.
 *
 * @author Adrian Drăghici
 */
public class SwiftResponseHandler extends DefaultHandler {

    private static final String NAME = "name";
    private static final String BYTES = "bytes";
    private static final String OBJECT = "object";
    private static final String LAST_MODIFIED = "last_modified";
    private static final String SUBDIRECTORY = "subdir";
    private static final String CONTAINER = "container";

    private LinkedList<String> elementStack = new LinkedList<>();
    private List<BasicFileAttributes> items;

    private String name;
    private long size;
    private String lastModified;
    private String marker;
    private boolean isTruncated;
    private String prefix;

    SwiftResponseHandler(String prefix, List<BasicFileAttributes> items) {
        this.prefix = prefix;
        this.items = items;
    }

    private static String getAuthorizationToken(String domain, String projectId, String user, String password) {
        try {
            URL authUrl = new URL(new SwiftFileSystemProvider().getProviderAuthAddress());
            HttpsURLConnection connection = (HttpsURLConnection) authUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("user-agent", "SNAP Virtual File System");
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            out.print("{\n" +
                    "    \"auth\": {\n" +
                    "        \"identity\": {\n" +
                    "            \"methods\": [\n" +
                    "                \"password\"\n" +
                    "            ],\n" +
                    "            \"password\": {\n" +
                    "                \"user\": {\n" +
                    "                    \"domain\": {\n" +
                    "                        \"name\": \"" + domain + "\"\n" +
                    "                    },\n" +
                    "                    \"name\": \"" + user + "\",\n" +
                    "                    \"password\": \"" + password + "\"\n" +
                    "                }\n" +
                    "            }\n" +
                    "        },\n" +
                    "        \"scope\": {\n" +
                    "            \"project\": {\n" +
                    "                \"id\": \"" + projectId + "\"\n" +
                    "            }\n" +
                    "        }\n" +
                    "    }\n" +
                    "}");
            out.flush();
            if (connection.getResponseCode() == 201) {
                return connection.getHeaderField("X-Subject-Token");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

//    static boolean revokeToken(String authorizationToken) {
//        try {
//            URL authUrl = new URL(new SwiftFileSystemProvider().getProviderAuthAddress());
//            HttpsURLConnection connection = (HttpsURLConnection) authUrl.openConnection();
//            connection.setRequestMethod("DELETE");
//            connection.setDoInput(true);
//            connection.setConnectTimeout(60000);
//            connection.setReadTimeout(60000);
//            connection.setRequestProperty("Connection", "keep-alive");
//            connection.setRequestProperty("user-agent", "SNAP Virtual File System");
//            if (authorizationToken != null && !authorizationToken.isEmpty()) {
//                connection.setRequestProperty("X-Auth-Token", authorizationToken);
//                connection.setRequestProperty("X-Subject-Token", authorizationToken);
//            }
//            if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 300) {
//                return true;
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        return false;
//    }

    static URLConnection getConnectionChannel(URL url, String method, Map<String, String> requestProperties) throws IOException {
        String authorizationToken = getAuthorizationToken(SwiftFileSystemProvider.getDomain(), SwiftFileSystemProvider.getProjectId(), SwiftFileSystemProvider.getUser(), SwiftFileSystemProvider.getPassword());
        HttpURLConnection connection;
        if (url.getProtocol().equals("https")) {
            connection = (HttpsURLConnection) url.openConnection();
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }
        connection.setRequestMethod(method);
        connection.setDoInput(true);
        method = method.toUpperCase();
        if (method.equals("POST") || method.equals("PUT") || method.equals("DELETE")) {
            connection.setDoOutput(true);
        }
        connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        if (authorizationToken != null && !authorizationToken.isEmpty())
            connection.setRequestProperty("X-Auth-Token", authorizationToken);
        if (requestProperties != null && requestProperties.size() > 0) {
            Set<Map.Entry<String, String>> requestProperties_set = requestProperties.entrySet();
            for (Map.Entry<String, String> requestProperty : requestProperties_set) {
                connection.setRequestProperty(requestProperty.getKey(), requestProperty.getValue());
            }
        }
        connection.setRequestProperty("user-agent", "SNAP Virtual File System");
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            /* error from server */
            throw new IOException(url + ": response code " + responseCode + ": " + connection.getResponseMessage());
        } else {
            return connection;
        }
    }

    private static String getTextValue(char[] ch, int start, int length) {
        return new String(ch, start, length).trim();
    }

    String getMarker() {
        return marker;
    }

    boolean getIsTruncated() {
        return isTruncated;
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        try {
            String currentElement = localName.intern();
            elementStack.addLast(currentElement);
        } catch (Exception ex) {
            throw new SAXException(ex);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        try {
            String currentElement = elementStack.removeLast();
            assert currentElement != null && currentElement.equals(localName);
            if (currentElement.equals(NAME) && elementStack.size() == 2 && (elementStack.get(1).equals(SUBDIRECTORY) || elementStack.get(1).equals(CONTAINER)) && !prefix.endsWith(name)) {
                items.add(ObjectStorageFileAttributes.newDir(prefix + name));
                isTruncated = true;
            } else if (currentElement.equals(NAME) && elementStack.size() == 2 && elementStack.get(1).equals(OBJECT) && !prefix.endsWith(name)) {
                items.add(ObjectStorageFileAttributes.newFile(prefix + name, size, lastModified));
                isTruncated = true;
            }
        } catch (Exception ex) {
            throw new SAXException(ex);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        try {
            String currentElement = elementStack.getLast();
            switch (currentElement) {
                case NAME:
                    marker = getTextValue(ch, start, length);
                    String[] name_parts = marker.split(new SwiftFileSystemProvider().getDelimiter());
                    name = marker.endsWith(new SwiftFileSystemProvider().getDelimiter()) ? name_parts[name_parts.length - 1] + new SwiftFileSystemProvider().getDelimiter() : name_parts[name_parts.length - 1];
                    break;
                case BYTES:
                    size = getLongValue(ch, start, length);
                    break;
                case LAST_MODIFIED:
                    lastModified = getTextValue(ch, start, length);
                    break;
            }
        } catch (Exception ex) {
            throw new SAXException(ex);
        }
    }

    private long getLongValue(char[] ch, int start, int length) {
        return Long.parseLong(getTextValue(ch, start, length));
    }

}
