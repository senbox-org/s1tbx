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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Walker for S3 Object Storage VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
class S3Walker implements ObjectStorageWalker {

    private static Logger logger = Logger.getLogger(ObjectStorageWalker.class.getName());

    private XMLReader xmlReader;

    private String bucketAddress;
    private String accessKeyId;
    private String secretAccessKey;
    private String delimiter;
    private String root;

    /**
     * Creates the new walker for S3 Object Storage VFS
     *
     * @param bucketAddress   The address of S3 service. (mandatory)
     * @param accessKeyId     The access key id S3 credential (username)
     * @param secretAccessKey The secret access key S3 credential (password)
     * @param delimiter       The VFS path delimiter
     * @param root            The root of S3 provider
     * @throws ParserConfigurationException If an serious configuration error occurs
     * @throws SAXException                 Any SAX exception, possibly wrapping another exception
     */
    S3Walker(String bucketAddress, String accessKeyId, String secretAccessKey, String delimiter, String root) throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        xmlReader = saxParser.getXMLReader();
        this.bucketAddress = bucketAddress;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.delimiter = delimiter;
        this.root = root;
    }

    /**
     * Gets the VFS file basic attributes.
     *
     * @param address The VFS service address
     * @param prefix  The VFS path to traverse
     * @return The S3 file basic attributes
     * @throws IOException If an I/O error occurs
     */
    public BasicFileAttributes getObjectStorageFile(String address, String prefix) throws IOException {
        URLConnection urlConnection = S3ResponseHandler.getConnectionChannel(new URL(address), "GET", null, accessKeyId, secretAccessKey);
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
     * @param prefix The VFS path to traverse
     * @return The list of VFS files and directories
     * @throws IOException If an I/O error occurs
     */
    public List<BasicFileAttributes> walk(String prefix) throws IOException {
        StringBuilder paramBase = new StringBuilder();
        addParam(paramBase, "prefix", prefix.replace(root, ""));
        addParam(paramBase, "delimiter", delimiter);

        ArrayList<BasicFileAttributes> items = new ArrayList<>();
        String nextContinuationToken = null;
        S3ResponseHandler handler;
        do {
            prefix = prefix.isEmpty() ? root : prefix;
            handler = new S3ResponseHandler(prefix, items, delimiter);
            xmlReader.setContentHandler(handler);
            StringBuilder params = new StringBuilder(paramBase);
            addParam(params, "continuation-token", nextContinuationToken);
            String systemId = bucketAddress;
            if (params.length() > 0) {
                systemId += "?" + params;
            }
            try {
                xmlReader.parse(new InputSource(S3ResponseHandler.getConnectionChannel(new URL(systemId), "GET", null, accessKeyId, secretAccessKey).getInputStream()));
            } catch (SAXException ex) {
                logger.log(Level.SEVERE, "Unable to get a list of S3 VFS files and directories from to the given prefix. Details: " + ex.getMessage());
                throw new IOException(ex);
            }
            nextContinuationToken = handler.getNextContinuationToken();
        } while (handler.getIsTruncated());

        return items;
    }

}
