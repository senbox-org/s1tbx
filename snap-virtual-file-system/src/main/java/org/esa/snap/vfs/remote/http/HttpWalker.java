package org.esa.snap.vfs.remote.http;

import org.esa.snap.core.util.StringUtils;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    private static boolean isValidResponseCode(int responseCode) {
        return (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE);
    }

    private static BasicFileAttributes fetchFileAttributes(HttpURLConnection fileConnection, String filePath) {
        try {
            String sizeString = fileConnection.getHeaderField("content-length");
            String lastModified = fileConnection.getHeaderField("last-modified");
            if (!StringUtils.isNotNullAndNotEmpty(sizeString) && StringUtils.isNotNullAndNotEmpty(lastModified)) {
                throw new IllegalStateException("filePath is not a file");
            }
            long size = Long.parseLong(sizeString);
            DateFormat srcDf = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
            Date lastModifiedDate = srcDf.parse(lastModified);
            DateFormat destDf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'");
            lastModified = destDf.format(lastModifiedDate);
            return VFSFileAttributes.newFile(filePath, size, lastModified);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to fetch file attributes", e);
        }
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
    public BasicFileAttributes getVFSBasicFileAttributes(String address, String prefix) throws IOException {
        // check if the address represents a directory
        HttpURLConnection connection = HttpResponseHandler.buildConnection(new URL(address + (address.endsWith("/") ? "" : "/")), "GET", null, this.username, this.password);
        int responseCode = connection.getResponseCode();
        if (isValidResponseCode(responseCode)) {
            // the address represents a directory
            return VFSFileAttributes.newDir(prefix);
        } else {
            // the address does not represent a directory
            return getVFSFileAttributes(address, prefix);
        }
    }

    BasicFileAttributes getVFSFileAttributes(String address, String prefix) throws IOException {
        HttpURLConnection connection = HttpResponseHandler.buildConnection(new URL(address), "GET", null, this.username, this.password);
        int responseCode = connection.getResponseCode();
        if (isValidResponseCode(responseCode)) {
            return fetchFileAttributes(connection, prefix);
        } else {
            throw new IOException(address + ": response code " + responseCode + ": " + connection.getResponseMessage());
        }
    }

    /**
     * Gets a list of VFS files and directories from to the given prefix.
     *
     * @param dir The VFS path to traverse
     * @return The list of VFS files and directories
     * @throws IOException If an I/O error occurs
     */
    @Override
    public synchronized List<BasicFileAttributes> walk(Path dir) throws IOException {
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
        if (!dirPathAsString.endsWith(this.delimiter)) {
            dirPathAsString += this.delimiter;
        }
        HttpResponseHandler handler = new HttpResponseHandler(doc, dirPathAsString, this.address, this.root, this);
        return handler.getElements();
    }
}
