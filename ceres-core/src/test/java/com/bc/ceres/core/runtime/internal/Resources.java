/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
