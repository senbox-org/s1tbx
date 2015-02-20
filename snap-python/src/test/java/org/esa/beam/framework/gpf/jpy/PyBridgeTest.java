package org.esa.beam.framework.gpf.jpy;

import org.junit.Test;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;

/**
 * Created by Norman on 20.02.2015.
 */
public class PyBridgeTest {

    @Test
    public void testFS() throws Exception {
        URL url = PyBridgeTest.class.getResource("test.zip");
        assertNotNull(url);

        URI uri = url.toURI();

        Path path = Paths.get(new URI("jar", null, uri.getPath(), null));
        assertNotNull(path);
        System.out.println("path = " + path);

        Iterable<Path> rootDirectories = path.getFileSystem().getRootDirectories();
        for (Path rootDirectory : rootDirectories) {
            System.out.println("rootDirectory = " + rootDirectory);
        }
    }
}
