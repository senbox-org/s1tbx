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


import org.junit.Test;

import java.util.SortedMap;

import static org.junit.Assert.*;

public class ResourceTest {

    @Test
    public void testGetPath() throws Exception {
        TestResource testResource = new TestResource("path/prop", "key = value", null);
        assertEquals("path/prop", testResource.getPath());

        TestResource testResource1 = new TestResource("path/xml", "<?xml>this is XML</xml>", null);
        assertEquals("path/xml", testResource1.getPath());
    }

    @Test
    public void testisXml() throws Exception {
        TestResource testResource = new TestResource("path/prop", "key = value", null);
        assertFalse(testResource.isXml());

        TestResource testResource1 = new TestResource("path/xml", "<?xml>this is XML</xml>", null);
        assertTrue(testResource1.isXml());
    }

    @Test
    public void testGetMapXML() throws Exception {
        TestResource testResource = new TestResource("path/", "<?xml>this is XML</xml>", null);
        SortedMap<String, String> map = testResource.getMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetMapProperties() throws Exception {
        TestResource testResource = new TestResource("path", "key = value\nkey2=anothervalue\n", null);
        SortedMap<String, String> map = testResource.getMap();
        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals("value", map.get("key"));
        assertEquals("anothervalue", map.get("key2"));
    }

    @Test
    public void testGetContent() throws Exception {
        assertEquals("key = value", new TestResource("", "key = value", null).getContent());
        assertEquals("<?xml>this is XML</xml>", new TestResource("", "<?xml>this is XML</xml>", null).getContent());
    }

    @Test
    public void testToString() throws Exception {
        assertEquals("key = value", new TestResource("", "key = value", null).toString());
        assertEquals("<?xml>this is XML</xml>", new TestResource("", "<?xml>this is XML</xml>", null).toString());
    }

    @Test
    public void testGetOrigin() throws Exception {
        TestResource r1 = new TestResource("r1", "key = value", null);
        TestResource r2 = new TestResource("r2", "key2 = value2", r1);

        assertNull(r1.getOrigin());
        assertNotNull(r2.getOrigin());
        assertSame(r1, r2.getOrigin());
    }

    private static class TestResource extends Resource {
        private final String content;

        public TestResource(String path, String content, Resource origin) {
            super(path, origin);
            this.content = content;
        }

        @Override
        protected String read() {
            return content;
        }

    }
}
