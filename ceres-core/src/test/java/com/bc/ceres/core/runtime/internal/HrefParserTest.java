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

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import com.bc.ceres.core.runtime.internal.HrefParser;

public class HrefParserTest extends TestCase {
    private MockHandler handler;

    @Override
    protected void setUp() throws Exception {
        handler = new MockHandler();
    }

    public void testEmpty() throws IOException {
        final HrefParser hrefParser = new HrefParser(new StringReader(""));
        hrefParser.parse(handler);
        assertEquals("", handler.toString());
    }

    public void testApacheDirListing() throws IOException {
        final String name = "html/apache-dir-listing.html";
        final InputStream resourceAsStream = HrefParserTest.class.getResourceAsStream(name);
        final HrefParser hrefParser = new HrefParser(new InputStreamReader(resourceAsStream));
        hrefParser.parse(handler);
        assertEquals(
                "/beam/|" +
                        "app-a-module-a-1.0-SNAPSHOT.jar|" +
                        "app-a-module-b-1.0-SNAPSHOT.jar|" +
                        "app-a-module-c-1.0-SNAPSHOT.jar|" +
                        "app-a-module-d-1.0-SNAPSHOT.jar|" +
                        "app-a-module-e-1.0-SNAPSHOT.jar|" +
                        "old-versions/" +
                        "", handler.toString());
    }

    public void testApacheDirListingWithVariations() throws IOException {
        final String name = "html/apache-dir-listing-var.html";
        final InputStream resourceAsStream = HrefParserTest.class.getResourceAsStream(name);
        final HrefParser hrefParser = new HrefParser(new InputStreamReader(resourceAsStream));
        hrefParser.parse(handler);
        assertEquals(
                "/beam/|" +
                        "app-a-module-a-1.0-SNAPSHOT.jar|" +
                        "app-a-module-b-1.0-SNAPSHOT.jar|" +
                        "app-a-module-c-1.0-SNAPSHOT.jar|" +
                        "app-a-module-d-1.0-SNAPSHOT.jar|" +
                        "app-a-module-e-1.0-SNAPSHOT.jar|" +
                        "old-versions/" +
                        "", handler.toString());
    }

    private static class MockHandler implements HrefParser.Handler {
        StringBuilder sb = new StringBuilder();

        @Override
        public String toString() {
            return sb.toString();
        }

        public void onValueSeen(String value) {
            if (sb.length() > 0)
                sb.append('|');
            sb.append(value);
        }
    }
}
