package org.esa.snap.vfs.remote.http;

import org.esa.snap.vfs.remote.ObjectStorageFileAttributes;
import org.esa.snap.vfs.remote.ObjectStorageWalker;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
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
    @Override
    public BasicFileAttributes getObjectStorageFile(String address, String prefix) throws IOException {
        URLConnection urlConnection = HttpResponseHandler.getConnectionChannel(new URL(address), "GET", null, username, password);
        return ObjectStorageFileAttributes.newFile(prefix, urlConnection.getContentLengthLong(), urlConnection.getHeaderField("last-modified"));
    }

    /**
     * Gets a list of VFS files and directories from to the given prefix.
     *
     * @param dir The VFS path to traverse
     * @return The list of VFS files and directories
     * @throws IOException If an I/O error occurs
     */
    @Override
    public List<BasicFileAttributes> walk(Path dir) throws IOException {
        StringBuilder urlAsString = new StringBuilder();
        if (this.address.endsWith(this.delimiter)) {
            int endIndex = this.address.length() - this.delimiter.length();
            urlAsString.append(this.address.substring(0, endIndex)); // do not write the file separator at the end
        } else {
            urlAsString.append(this.address);
        }
        String dirPathAsString = dir.toString();
        String urlPathAsString = dirPathAsString;
        if (urlPathAsString.startsWith(this.root)) {
            urlPathAsString = urlPathAsString.substring(this.root.length());
        }
        if (!urlPathAsString.startsWith(this.delimiter)) {
            urlAsString.append(this.delimiter);
        }
        urlAsString.append(urlPathAsString);

        URL url = new URL(urlAsString.toString());
        URLConnection connection = HttpResponseHandler.getConnectionChannel(url, "GET", null, this.username, this.password);
        Document doc = Jsoup.parse(connection.getInputStream(), "UTF-8", url.toString());
        List<BasicFileAttributes> items = new ArrayList<>();
        if (!dirPathAsString.endsWith(this.delimiter)) {
            dirPathAsString += this.delimiter;
        }
        HttpResponseHandler handler = new HttpResponseHandler(doc, dirPathAsString, items);
        handler.getElements();
        return items;
    }
}
