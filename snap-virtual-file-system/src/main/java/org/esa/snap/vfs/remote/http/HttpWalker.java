package org.esa.snap.vfs.remote.http;

import org.esa.snap.core.util.StringUtils;
import org.esa.snap.vfs.remote.VFSFileAttributes;
import org.esa.snap.vfs.remote.VFSWalker;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
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

    BasicFileAttributes getVFSFileAttributes(String address, String prefix) throws IOException {
        HttpURLConnection connection = HttpResponseHandler.buildConnection(new URL(address), "GET", null, this.username, this.password);
        try {
            int responseCode = connection.getResponseCode();
            if (isValidResponseCode(responseCode)) {
                String sizeString = connection.getHeaderField("content-length");
                String lastModified = connection.getHeaderField("last-modified");
                if (!StringUtils.isNotNullAndNotEmpty(sizeString) && StringUtils.isNotNullAndNotEmpty(lastModified)) {
                    throw new IOException("filePath is not a file '"+prefix+"'.");
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

        Document document;
        URL url = new URL(urlAsString.toString());
        HttpURLConnection connection = HttpResponseHandler.getConnectionChannel(url, "GET", null, this.username, this.password);
        try {
            try (InputStream inputStream = connection.getInputStream();
                 BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, 10 * 1024)) {

                document = Jsoup.parse(bufferedInputStream, "UTF-8", url.toString());
            }
        } finally {
            connection.disconnect();
        }
        if (!dirPathAsString.endsWith(this.delimiter)) {
            dirPathAsString += this.delimiter;
        }
        HttpResponseHandler handler = new HttpResponseHandler(document, dirPathAsString, this.address, this.root, this);
        return handler.getElements();
    }
}
