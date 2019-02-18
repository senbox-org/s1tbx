package org.esa.snap.core.dataio.vfs.remote.object_storage.aws;

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
 * Walker for Amazon AWS S3 Object Storage VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
class S3Walker implements ObjectStorageWalker {

    private XMLReader xmlReader;

    S3Walker() throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        xmlReader = saxParser.getXMLReader();
    }

    public BasicFileAttributes getObjectStorageFile(String address, String prefix) throws IOException {
        URLConnection urlConnection = S3ResponseHandler.getConnectionChannel(new URL(address), "GET", null);
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
        StringBuffer paramBase = new StringBuffer();
        addParam(paramBase, "prefix", prefix.replace(S3FileSystemProvider.S3_ROOT, ""));
        addParam(paramBase, "delimiter", delimiter);

        ArrayList<BasicFileAttributes> items = new ArrayList<>();
        String nextContinuationToken = null;
        S3ResponseHandler handler;
        do {
            handler = new S3ResponseHandler(prefix, items);
            xmlReader.setContentHandler(handler);
            StringBuffer params = new StringBuffer(paramBase);
            addParam(params, "continuation-token", nextContinuationToken);
            String systemId = new S3FileSystemProvider().getProviderAddress();
            if (params.length() > 0) {
                systemId += "?" + params;
            }
            // System.out.println("systemId = " + systemId);
            try {
                xmlReader.parse(new InputSource(S3ResponseHandler.getConnectionChannel(new URL(systemId), "GET", null).getInputStream()));
            } catch (SAXException e) {
                throw new IOException(e);
            }
            nextContinuationToken = handler.getNextContinuationToken();
        } while (handler.getIsTruncated());

        return items;
    }

}
