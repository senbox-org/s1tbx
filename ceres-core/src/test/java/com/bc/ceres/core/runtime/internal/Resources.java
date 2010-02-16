package com.bc.ceres.core.runtime.internal;

import java.io.*;

public class Resources {

    public static InputStream openStream(String resource) {
        InputStream stream = Resources.class.getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalStateException("resource not found: " + resource);
        }
        return stream;
    }

    public static Reader openReader(String resource) {
        return new BufferedReader(new InputStreamReader(openStream(resource)));
    }

    public static String loadText(String resource) {
        return loadAsText(openReader(resource), resource);
    }

    private static String loadAsText(Reader reader, String resource) {
        char[] buffer = new char[1024];
        StringBuilder sb = new StringBuilder(4 * buffer.length);
        try {
            while (true) {
                int len = reader.read(buffer);
                if (len == -1) {
                    break;
                }
                sb.append(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new IllegalStateException("failed to read resource: " + resource, e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                throw new IllegalStateException("failed to close resource: " + resource, e);
            }
        }
        return sb.toString();
    }

}
