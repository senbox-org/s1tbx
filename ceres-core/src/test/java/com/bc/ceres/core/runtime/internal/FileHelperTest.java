package com.bc.ceres.core.runtime.internal;

import junit.framework.TestCase;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import com.bc.ceres.core.runtime.Constants;


public class FileHelperTest extends TestCase {
    public void testUrlToFileWithNull() throws MalformedURLException {
        try {
            FileHelper.urlToFile(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
    }

    public void testUrlToFileWithValidFileUrl() throws MalformedURLException {
        File file = new File("").getAbsoluteFile();
        URL fileUrl = file.toURI().toURL();
        assertEquals(file, FileHelper.urlToFile(fileUrl));
    }

    public void testUrlToFileWithInvalidFileUrl() throws MalformedURLException {
        URL someUrl = new URL("http://www.google.com");
        assertEquals(null, FileHelper.urlToFile(someUrl));
    }

    public void testUrlToFileWithAJarEntry() throws MalformedURLException {
        File dir = new File("").getAbsoluteFile();
        File file = new File(dir, "test.jar");
        URL fileUrl = file.toURI().toURL();
        URL jarUrl = new URL("jar:" + fileUrl + "!/module.xml");
        assertEquals(file, FileHelper.urlToFile(jarUrl));
    }

    public void testUrlToFileWithAJarFile() throws MalformedURLException {
        File dir = new File("").getAbsoluteFile();
        File file = new File(dir, "test.jar");
        URL fileUrl = file.toURI().toURL();
        URL jarUrl = new URL("jar:" + fileUrl + "!/");
        assertEquals(file, FileHelper.urlToFile(jarUrl));
    }

    public void testFileToUrlWithNull() throws MalformedURLException {
        try {
            FileHelper.fileToUrl(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
    }

    public void testFileToUrlWithValidFile() throws MalformedURLException {
        File file = new File("").getAbsoluteFile();
        URL fileUrl = file.toURI().toURL();
        assertEquals(fileUrl, FileHelper.fileToUrl(file));
    }


    // prerequisite for ...
    public void testThatDirectoryUrlsAlwaysEndWithASlash() {
        File dir = new File("").getAbsoluteFile();
        while (dir != null) {
            URL url = FileHelper.fileToUrl(dir);
            assertTrue(dir.getPath(), url.getPath().endsWith("/"));
            dir = dir.getParentFile();
        }
    }


    public void testManifestToLocationUrl() throws MalformedURLException {
        try {
            FileHelper.manifestToLocationUrl(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }

        URL locationUrl = new URL("file:/usr/local/app-a/module-b/");
        URL manifestUrl = new URL(locationUrl.toExternalForm() + Constants.MODULE_MANIFEST_NAME);
        assertEquals(locationUrl, FileHelper.manifestToLocationUrl(manifestUrl));

        locationUrl = new URL("file:/usr/local/app-a/module-b.jar");
        manifestUrl = new URL("jar:" + locationUrl.toExternalForm() + "!/" + Constants.MODULE_MANIFEST_NAME);
        assertEquals(locationUrl, FileHelper.manifestToLocationUrl(manifestUrl));

        locationUrl = new URL("file:/usr/local/app-a/module-b.JAR");
        manifestUrl = new URL("jar:" + locationUrl.toExternalForm() + "!/" + Constants.MODULE_MANIFEST_NAME);
        assertEquals(locationUrl, FileHelper.manifestToLocationUrl(manifestUrl));

        locationUrl = new URL("file:/usr/local/app-a/module-b.zip");
        manifestUrl = new URL("jar:" + locationUrl.toExternalForm() + "!/" + Constants.MODULE_MANIFEST_NAME);
        assertEquals(locationUrl, FileHelper.manifestToLocationUrl(manifestUrl));

        locationUrl = null;
        manifestUrl = new URL("file:/usr/local/app-a/module-b.txt");
        assertEquals(locationUrl, FileHelper.manifestToLocationUrl(manifestUrl));
    }


    public void testLoctionToManifestUrl() throws MalformedURLException {
        try {
            FileHelper.locationToManifestUrl(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }

        URL locationUrl = new URL("file:/usr/local/app-a/module-b/");
        URL manifestUrl = new URL(locationUrl.toExternalForm() + Constants.MODULE_MANIFEST_NAME);
        assertEquals(manifestUrl, FileHelper.locationToManifestUrl(locationUrl));

        locationUrl = new URL("file:/usr/local/app-a/module-b.jar");
        manifestUrl = new URL("jar:" + locationUrl.toExternalForm() + "!/" + Constants.MODULE_MANIFEST_NAME);
        assertEquals(manifestUrl, FileHelper.locationToManifestUrl(locationUrl));

        locationUrl = new URL("file:/usr/local/app-a/module-b.JAR");
        manifestUrl = new URL("jar:" + locationUrl.toExternalForm() + "!/" + Constants.MODULE_MANIFEST_NAME);
        assertEquals(manifestUrl, FileHelper.locationToManifestUrl(locationUrl));

        locationUrl = new URL("file:/usr/local/app-a/module-b.zip");
        manifestUrl = new URL("jar:" + locationUrl.toExternalForm() + "!/" + Constants.MODULE_MANIFEST_NAME);
        assertEquals(manifestUrl, FileHelper.locationToManifestUrl(locationUrl));

        locationUrl = new URL("file:/usr/local/app-a/module-b.txt");
        manifestUrl = null;
        assertEquals(manifestUrl, FileHelper.locationToManifestUrl(locationUrl));

        locationUrl = new URL("file:/usr/local/app-a/" + Constants.MODULE_MANIFEST_NAME);
        manifestUrl = null;
        assertEquals(manifestUrl, FileHelper.locationToManifestUrl(locationUrl));
    }
}
