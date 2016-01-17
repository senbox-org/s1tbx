package org.esa.snap.core.image;

import org.junit.Test;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class RawZipImageInputStreamFactoryTest {

    @Test
    public void createEntryPathString() throws Exception {
        final Path dir = Paths.get(".", "space dir").toAbsolutePath();
        final TiledFileOpImage.RawZipImageInputStreamFactory factory = new TiledFileOpImage.RawZipImageInputStreamFactory(dir);
        final String entryPathString = factory.createEntryPathString("marmalade");

        assertTrue(entryPathString.contains("%20"));
        assertTrue(entryPathString.startsWith("jar:file:/"));
        assertTrue(entryPathString.endsWith("!/"));

        try {
            //noinspection ResultOfMethodCallIgnored
            URI.create(entryPathString);
        } catch (IllegalArgumentException e) {
            fail("URI to zip is invalid. Probably wrongly encoded");
        }

    }
}