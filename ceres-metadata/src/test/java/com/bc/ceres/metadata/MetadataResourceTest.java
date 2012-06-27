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


import org.junit.Test;

import java.util.SortedMap;

import static org.junit.Assert.*;

public class MetadataResourceTest {

    @Test
    public void testisXml() throws Exception {
        assertFalse(new MetadataResource("key = value").isXml());
        assertTrue(new MetadataResource("<?xml>this is XML</xml>").isXml());
    }

    @Test
    public void testGetMapXML() throws Exception {
        MetadataResource metadataResource = new MetadataResource("<?xml>this is XML</xml>");
        SortedMap<String, String> map = metadataResource.getMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetMapProperties() throws Exception {
        MetadataResource metadataResource = new MetadataResource("key = value\nkey2=anothervalue\n");
        SortedMap<String, String> map = metadataResource.getMap();
        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals("value", map.get("key"));
        assertEquals("anothervalue", map.get("key2"));
    }

    @Test
    public void testGetContent() throws Exception {
        assertEquals("key = value", new MetadataResource("key = value").getContent());
        assertEquals("<?xml>this is XML</xml>", new MetadataResource("<?xml>this is XML</xml>").getContent());
    }
}
