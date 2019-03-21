package org.esa.snap.vfs.remote.http;

import org.esa.snap.vfs.remote.ObjectStorageFileAttributes;
import org.esa.snap.vfs.remote.ObjectStorageWalker;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Walker for HTTP Object Storage VFS.
 *
 * @author Adrian Drăghici
 */
class HttpWalker implements ObjectStorageWalker {

    private final String address;
    private final String username;
    private final String password;
    private final String delimiter;
    private final String root;

    /**
     * Creates the new walker for HTTP Object Storage VFS
     *
     * @param address   The address of HTTP service. (mandatory)
     * @param username  The username HTTP credential
     * @param password  The password HTTP credential
     * @param delimiter The VFS path delimiter
     * @param root      The root of S3 provider
     */
    HttpWalker(String address, String username, String password, String delimiter, String root) {
        this.address = address;
        this.username = username;
        this.password = password;
        this.delimiter = delimiter;
        this.root = root;
    }

    /**
     * Gets the VFS file basic attributes.
     *
     * @param address The VFS service address
     * @param prefix  The VFS path to traverse
     * @return The HTTP file basic attributes
     * @throws IOException If an I/O error occurs
     */
    public BasicFileAttributes getObjectStorageFile(String address, String prefix) throws IOException {
        URLConnection urlConnection = HttpResponseHandler.getConnectionChannel(new URL(address), "GET", null, username, password);
        return ObjectStorageFileAttributes.newFile(prefix, urlConnection.getContentLengthLong(), urlConnection.getHeaderField("last-modified"));
    }

    /**
     * Gets a list of VFS files and directories from to the given prefix.
     *
     * @param prefix The VFS path to traverse
     * @return The list of VFS files and directories
     * @throws IOException If an I/O error occurs
     */
    public List<BasicFileAttributes> walk(String prefix) throws IOException {
        ArrayList<BasicFileAttributes> items = new ArrayList<>();
        URL url = new URL(address + (address.endsWith(delimiter) ? "" : delimiter) + prefix.replace(root, ""));
        URLConnection connection = HttpResponseHandler.getConnectionChannel(url, "GET", null, username, password);
        Document doc = Jsoup.parse(connection.getInputStream(), "UTF-8", url.toString());
        prefix = prefix.isEmpty() ? root : prefix;
        HttpResponseHandler handler = new HttpResponseHandler(doc, prefix, items);
        handler.getElements();
        return items;
    }

}
