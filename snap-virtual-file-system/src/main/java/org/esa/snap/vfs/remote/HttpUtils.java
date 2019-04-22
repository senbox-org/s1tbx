package org.esa.snap.vfs.remote;

import java.net.HttpURLConnection;

/**
 * Created by jcoravu on 19/4/2019.
 */
public class HttpUtils {

    private HttpUtils() {
    }

    public static boolean isValidResponseCode(int responseCode) {
        return (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE);
    }
}
