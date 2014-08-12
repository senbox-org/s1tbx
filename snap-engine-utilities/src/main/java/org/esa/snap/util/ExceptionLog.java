/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
