package org.esa.snap.vfs.remote;

import java.net.HttpURLConnection;

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
}
