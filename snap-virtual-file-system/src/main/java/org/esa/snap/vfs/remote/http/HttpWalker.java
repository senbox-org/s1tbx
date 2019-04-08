package org.esa.snap.vfs.remote.http;

import org.esa.snap.vfs.remote.VFSFileAttributes;
import org.esa.snap.vfs.remote.VFSWalker;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Walker for HTTP VFS.
 *
 * @author Adrian Drăghici
 */
class HttpWalker implements VFSWalker {

    private final String address;
    private final String username;
    private final String password;
    private final String delimiter;
    private final String root;

    /**
     * Creates the new walker for HTTP  VFS
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
        // check if the address represents a directory
        HttpURLConnection connection = HttpResponseHandler.buildConnection(new URL(address+"/"), "GET", null, this.username, this.password);
        int responseCode = connection.getResponseCode();
        if (isValidResponseCode(responseCode)) {
            // the address represents a directory
            return VFSFileAttributes.newDir(prefix);
        } else /*if (responseCode == HttpURLConnection.HTTP_NOT_FOUND)*/ {
            // the address does not represent a directory
            connection = HttpResponseHandler.buildConnection(new URL(address), "GET", null, this.username, this.password);
            responseCode = connection.getResponseCode();
            if (isValidResponseCode(responseCode)) {
                return VFSFileAttributes.newFile(prefix, connection.getContentLengthLong(), connection.getHeaderField("last-modified"));
            } else {
                throw new IOException(address + ": response code " + responseCode + ": " + connection.getResponseMessage());
            }
        }
    }

    private static boolean isValidResponseCode(int responseCode) {
        return (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE);
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
            urlAsString.append(this.address, 0, endIndex); // do not write the file separator at the end
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
