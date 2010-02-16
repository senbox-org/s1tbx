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
