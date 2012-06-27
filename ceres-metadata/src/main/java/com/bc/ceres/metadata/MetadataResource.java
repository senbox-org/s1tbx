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

package com.bc.ceres.metadata;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This class encapsulates the content of a metadata resource. This can be either XML or
 * Java properties.
 *
 * @author MarcoZ
 * @author Bettina
 * @since Ceres 0.13.2
 */
public class MetadataResource {

    private final boolean isXml;
    private final String content;
    private final SortedMap<String, String> map;

    /**
     * Creates a {@code MetadataResource} object from the given text.
     *
     * @param content The content in text format
     */
    public MetadataResource(String content) {
        this.content = content;
        if (isXml(content)) {
            this.map = new TreeMap<String, String>();
            this.isXml = true;
        } else {
            this.map = readProperties(content);
            this.isXml = false;
        }
    }

    public boolean isXml() {
        return isXml;
    }

    public String getContent() {
        return content;
    }

    public SortedMap<String, String> getMap() {
        return map;
    }

    private static SortedMap<String, String> readProperties(String text) {
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

    private static boolean isXml(String textContent) {
        String t = textContent.trim();
        return t.startsWith("<?xml ") || t.startsWith("<?XML ") || (t.startsWith("<") && t.endsWith(">"));
    }

}
