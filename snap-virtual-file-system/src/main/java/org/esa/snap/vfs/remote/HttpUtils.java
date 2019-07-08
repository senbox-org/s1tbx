package org.esa.snap.vfs.remote;

import org.esa.snap.core.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * HttpUtils for VFS
 *
 * @author Jean Coravu
 */
public class HttpUtils {

    private HttpUtils() {
    }

    public static boolean isValidResponseCode(int responseCode) {
        return (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE);
    }

    public static RegularFileMetadata readRegularFileMetadata(String urlAddress, IRemoteConnectionBuilder remoteConnectionBuilder, String fileSystemRoot) throws IOException {
        URL fileURL = new URL(urlAddress);
        HttpURLConnection connection = remoteConnectionBuilder.buildConnection(fileSystemRoot, fileURL, "GET", null);
        try {
            int responseCode = connection.getResponseCode();
            if (HttpUtils.isValidResponseCode(responseCode)) {
                String sizeString = connection.getHeaderField("content-length");
                String lastModified = connection.getHeaderField("last-modified");
                if (!StringUtils.isNotNullAndNotEmpty(sizeString) || !StringUtils.isNotNullAndNotEmpty(lastModified)) {
                    if (!connection.getURL().toString().contentEquals(urlAddress)) {
                        throw new IOException("Invalid VFS service.\nReason: Redirect from: " + urlAddress + " to: " + connection.getURL().toString());
                    }
                    throw new IOException("filePath is not a file '" + urlAddress + "'.");
                }
                long size = Long.parseLong(sizeString);
                return new RegularFileMetadata(urlAddress, lastModified, size);
            } else {
                throw new IOException(urlAddress + ": response code " + responseCode + ": " + connection.getResponseMessage());
            }
        } finally {
            connection.disconnect();
        }
    }

    public static String readResponse(String urlAddress, IRemoteConnectionBuilder remoteConnectionBuilder, String fileSystemRoot) throws IOException {
        URL pageURL = new URL(urlAddress);
        HttpURLConnection connection = remoteConnectionBuilder.buildConnection(fileSystemRoot, pageURL, "GET", null);
        try {
            int responseCode = connection.getResponseCode();
            if (HttpUtils.isValidResponseCode(responseCode)) {
                try (InputStream inputStream = connection.getInputStream();
                     ByteArrayOutputStream result = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        result.write(buffer, 0, length);
                    }
                    return result.toString("UTF-8");
                }
            } else {
                throw new IOException(urlAddress + ": response code " + responseCode + ": " + connection.getResponseMessage());
            }
        } finally {
            connection.disconnect();
        }
    }
}
