package org.esa.snap.vfs.remote;

import org.esa.snap.core.util.StringUtils;

import java.io.IOException;
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

    public static RegularFileMetadata readRegularFileMetadata(String urlAddress, IRemoteConnectionBuilder remoteConnectionBuilder) throws IOException {
        URL fileURL = new URL(urlAddress);
        HttpURLConnection connection = remoteConnectionBuilder.buildConnection(fileURL, "GET", null);
        try {
            int responseCode = connection.getResponseCode();
            if (HttpUtils.isValidResponseCode(responseCode)) {
                String sizeString = connection.getHeaderField("content-length");
                String lastModified = connection.getHeaderField("last-modified");
                if (!StringUtils.isNotNullAndNotEmpty(sizeString) && StringUtils.isNotNullAndNotEmpty(lastModified)) {
                    throw new IOException("filePath is not a file '" + urlAddress + "'.");
                }
                long size = Long.parseLong(sizeString);
                return new RegularFileMetadata(lastModified, size);
            } else {
                throw new IOException(urlAddress + ": response code " + responseCode + ": " + connection.getResponseMessage());
            }
        } finally {
            connection.disconnect();
        }
    }
}
