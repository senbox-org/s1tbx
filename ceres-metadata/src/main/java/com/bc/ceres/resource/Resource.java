/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This class encapsulates the content of a resource. This can be either XML or
 * Java properties.
 *
 * @author MarcoZ
 * @author Bettina
 * @since Ceres 0.13.2
 */
public abstract class Resource {
    private final String path;
    private boolean isXml;
    private String content;
    private SortedMap<String, String> map;
    private final Resource origin;

    /**
     * Creates a {@code Resource} object for the given path.
     *
     * @param path The path of this resource
     */
    public Resource(String path, Resource origin) {
        this.path = path;
        this.origin = origin;
    }

    private void init() {
        this.content = read();
        if (isXml(content)) {
            this.map = new TreeMap<String, String>();
            this.isXml = true;
        } else {
            this.map = readProperties(content);
            this.isXml = false;
        }
    }

    abstract protected String read();

    public String getPath() {
        return path;
    }

    public Resource getOrigin() {
        return origin;
    }

    public boolean isXml() {
        if (content == null) {
            init();
        }
        return isXml;
    }

    public String getContent() {
        if (content == null) {
            init();
        }
        return content;
    }

    public SortedMap<String, String> getMap() {
        if (content == null) {
            init();
        }
        return map;
    }

    @Override
    public String toString() {
        return getContent();
    }

    static SortedMap<String, String> readProperties(String text) {
        try {
            Properties properties = new Properties();
            properties.load(new StringReader(text));
            TreeMap<String, String> map = new TreeMap<String, String>();
            for (String name : properties.stringPropertyNames()) {
                map.put(name, properties.getProperty(name));
            }
            return map;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static boolean isXml(String textContent) {
        String t = textContent.trim();
        return t.startsWith("<?xml ") || t.startsWith("<?XML ") || (t.startsWith("<") && t.endsWith(">"));
    }

    public static String readText(Reader reader) throws IOException {
        final BufferedReader br = new BufferedReader(reader);
        StringBuilder text = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            text.append(line);
            text.append("\n");
        }
        return text.toString().trim();
    }
}
