package com.bc.ceres.core;

import com.thoughtworks.xstream.XStream;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ResourceLocatorTest {
    @Test
    public void testGetResources() throws Exception {
        Collection<Path> resources = ResourceLocator.getResources("META-INF/MANIFEST.MF");
        assertNotNull(resources);
        int size = resources.size();
        assertTrue(size > 5);
        for (Path resource : resources) {
            //System.out.println("path = " + path);
            assertNotNull(resource);
            assertTrue(Files.exists(resource));

            Path resolve = resource.resolve("..").resolve("..").resolve("module.xml").normalize();
            System.out.println("resolve = " + resolve.toUri() + ", " + Files.exists(resolve));

           //new ModuleReader(null).readFromManifest()

/*
            System.out.println("manifest: " + resource.toUri());
            Manifest manifest = new Manifest(Files.newInputStream(resource));
            Attributes mainAttributes = manifest.getMainAttributes();
            Set<Map.Entry<Object, Object>> entries = mainAttributes.entrySet();
            for (Map.Entry<Object, Object> entry : entries) {
                System.out.println(">> " + entry.getKey() + ": " + entry.getValue());
            }
*/
        }
    }

    @Test
    public void testNio() throws Exception {
        dumpPathProperties(String.class.getResource("String.class").toURI());
        dumpPathProperties(String.class.getResource("String.class").toURI());
        dumpPathProperties(ResourceLocatorTest.class.getResource("ResourceLocatorTest.class").toURI());
        dumpPathProperties(ResourceLocatorTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        dumpPathProperties(XStream.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        dumpPathProperties(new File(".").getAbsoluteFile().toURI());
    }

    private void dumpPathProperties(URI uri) {
        System.out.println("\nuri = " + uri);
        try {
            FileSystems.newFileSystem(uri, Collections.emptyMap());
        } catch (Exception e) {
            String message = e.getMessage();
            System.out.println(e.getClass().getSimpleName() + ": " + message);
        }
        Path path = Paths.get(uri);
        System.out.println("path.path: " + path.toString());
        System.out.println("path.uri: " + path.toUri());
        System.out.println("path.fs:  " + path.getFileSystem());
    }
}
