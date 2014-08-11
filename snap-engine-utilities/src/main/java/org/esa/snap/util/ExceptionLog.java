package org.esa.snap.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Logs operator exceptions
 */
public class ExceptionLog {

    public static void log(final String msg) {
        try {
            String urlStr = VersionUtil.getRemoteVersionURL("Error");
            urlStr = urlStr.substring(0, urlStr.lastIndexOf("&s=")) + "&s=" + msg;
            urlStr = urlStr.replace(' ', '_');
            submit(new URL(urlStr));
        } catch (Exception e) {
            System.out.println("ExceptionLog: " + e.getMessage());
        }
    }

    private static String submit(final URL url) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        final String line;
        try {
            line = reader.readLine();
        } finally {
            reader.close();
        }
        return line;
    }
}
