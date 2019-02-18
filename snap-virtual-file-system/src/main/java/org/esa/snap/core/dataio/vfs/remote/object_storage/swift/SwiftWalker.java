package org.esa.snap.core.dataio.vfs.remote.object_storage.swift;

import org.esa.snap.core.dataio.vfs.remote.object_storage.ObjectStorageFileAttributes;
import org.esa.snap.core.dataio.vfs.remote.object_storage.ObjectStorageWalker;
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
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Walker for EO Cloud OpenStack Swift Object Storage VFS.
 *
 * @author Adrian Drăghici
 */
class SwiftWalker implements ObjectStorageWalker {

    private XMLReader xmlReader;

    SwiftWalker() throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        xmlReader = saxParser.getXMLReader();
    }

    public BasicFileAttributes getObjectStorageFile(String address, String prefix) throws IOException {
        URLConnection urlConnection = SwiftResponseHandler.getConnectionChannel(new URL(address), "GET", null);
        return ObjectStorageFileAttributes.newFile(prefix, urlConnection.getContentLengthLong(), urlConnection.getHeaderField("last-modified"));
    }

    private static void addParam(StringBuffer params, String name, String value) throws IOException {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (params.length() > 0) {
            params.append("&");
        }
        params.append(name).append("=").append(URLEncoder.encode(value, "UTF8"));
    }

    public List<BasicFileAttributes> walk(String prefix, String delimiter) throws IOException {
        String container;
        if (SwiftFileSystemProvider.getContainer() != null && !SwiftFileSystemProvider.getContainer().isEmpty()) {
            container = SwiftFileSystemProvider.getContainer();
        } else {
            String[] parts = prefix.split(new SwiftFileSystemProvider().getDelimiter());
            if (prefix.startsWith(SwiftFileSystemProvider.SWIFT_ROOT)) {
                container = parts.length > 1 ? parts[1] : "";
            } else {
                container = parts.length > 0 ? parts[0] : "";
            }
        }
        container = (container != null && !container.isEmpty()) ? container + new SwiftFileSystemProvider().getDelimiter() : "";
        prefix = prefix.replace(SwiftFileSystemProvider.SWIFT_ROOT, "");
        prefix = prefix.replace(container, "");
        StringBuffer paramBase = new StringBuffer();
        addParam(paramBase, "format", "xml");
        addParam(paramBase, "limit", "1000");
        addParam(paramBase, "prefix", prefix);
        addParam(paramBase, "delimiter", delimiter);

        ArrayList<BasicFileAttributes> items = new ArrayList<>();
        String marker = "";
        SwiftResponseHandler handler;
        do {
            handler = new SwiftResponseHandler(SwiftFileSystemProvider.SWIFT_ROOT + container + prefix, items);
            xmlReader.setContentHandler(handler);
            StringBuffer params = new StringBuffer(paramBase);
            addParam(params, "marker", marker);
            String systemId = new SwiftFileSystemProvider().getProviderAddress() + container;
            if (params.length() > 0) {
                systemId += "?" + params;
            }
            // System.out.println("systemId = " + systemId);
            try {
                xmlReader.parse(new InputSource(SwiftResponseHandler.getConnectionChannel(new URL(systemId), "GET", null).getInputStream()));
            } catch (SAXException e) {
                throw new IOException(e);
            }
            marker = handler.getMarker();
        } while (handler.getIsTruncated());
        return items;
    }

}
