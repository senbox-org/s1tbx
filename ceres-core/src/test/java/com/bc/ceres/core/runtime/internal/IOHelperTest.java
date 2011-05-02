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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import com.bc.ceres.core.runtime.Constants;


public class IOHelperTest extends TestCase {
    public void testUrlToFileWithNull() throws MalformedURLException {
        try {
            UrlHelper.urlToFile(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
    }

    public void testUrlToFileWithValidFileUrl() throws MalformedURLException {
        File file = new File("").getAbsoluteFile();
        URL fileUrl = file.toURI().toURL();
        assertEquals(file, UrlHelper.urlToFile(fileUrl));
    }

    public void testUrlToFileWithInvalidFileUrl() throws MalformedURLException {
        URL someUrl = new URL("http://www.google.com");
        assertEquals(null, UrlHelper.urlToFile(someUrl));
    }

    public void testUrlToFileWithAJarEntry() throws MalformedURLException {
        File dir = new File("").getAbsoluteFile();
        File file = new File(dir, "test.jar");
        URL fileUrl = file.toURI().toURL();
        URL jarUrl = new URL("jar:" + fileUrl + "!/module.xml");
        assertEquals(file, UrlHelper.urlToFile(jarUrl));
    }

    public void testUrlToFileWithAJarFile() throws MalformedURLException {
        File dir = new File("").getAbsoluteFile();
        File file = new File(dir, "test.jar");
        URL fileUrl = file.toURI().toURL();
        URL jarUrl = new URL("jar:" + fileUrl + "!/");
        assertEquals(file, UrlHelper.urlToFile(jarUrl));
    }

    public void testFileToUrlWithNull() throws MalformedURLException {
        try {
            UrlHelper.fileToUrl(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }
    }

    public void testFileToUrlWithValidFile() throws MalformedURLException {
        File file = new File("").getAbsoluteFile();
        URL fileUrl = file.toURI().toURL();
        assertEquals(fileUrl, UrlHelper.fileToUrl(file));
    }


    // prerequisite for ...
    public void testThatDirectoryUrlsAlwaysEndWithASlash() {
        File dir = new File("").getAbsoluteFile();
        while (dir != null) {
            URL url = UrlHelper.fileToUrl(dir);
            assertTrue(dir.getPath(), url.getPath().endsWith("/"));
            dir = dir.getParentFile();
        }
    }


    public void testManifestToLocationUrl() throws MalformedURLException {
        try {
            UrlHelper.manifestToLocationUrl(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }

        URL locationUrl = new URL("file:/usr/local/app-a/module-b/");
        URL manifestUrl = new URL(locationUrl.toExternalForm() + Constants.MODULE_MANIFEST_NAME);
        assertEquals(locationUrl, UrlHelper.manifestToLocationUrl(manifestUrl));

        locationUrl = new URL("file:/usr/local/app-a/module-b.jar");
        manifestUrl = new URL("jar:" + locationUrl.toExternalForm() + "!/" + Constants.MODULE_MANIFEST_NAME);
        assertEquals(locationUrl, UrlHelper.manifestToLocationUrl(manifestUrl));

        locationUrl = new URL("file:/usr/local/app-a/module-b.JAR");
        manifestUrl = new URL("jar:" + locationUrl.toExternalForm() + "!/" + Constants.MODULE_MANIFEST_NAME);
        assertEquals(locationUrl, UrlHelper.manifestToLocationUrl(manifestUrl));

        locationUrl = new URL("file:/usr/local/app-a/module-b.zip");
        manifestUrl = new URL("jar:" + locationUrl.toExternalForm() + "!/" + Constants.MODULE_MANIFEST_NAME);
        assertEquals(locationUrl, UrlHelper.manifestToLocationUrl(manifestUrl));

        locationUrl = null;
        manifestUrl = new URL("file:/usr/local/app-a/module-b.txt");
        assertEquals(locationUrl, UrlHelper.manifestToLocationUrl(manifestUrl));
    }


    public void testLoctionToManifestUrl() throws MalformedURLException {
        try {
            UrlHelper.locationToManifestUrl(null);
            fail();
        } catch (NullPointerException e) {
            // ok
        }

        URL locationUrl = new URL("file:/usr/local/app-a/module-b/");
        URL manifestUrl = new URL(locationUrl.toExternalForm() + Constants.MODULE_MANIFEST_NAME);
        assertEquals(manifestUrl, UrlHelper.locationToManifestUrl(locationUrl));

        locationUrl = new URL("file:/usr/local/app-a/module-b.jar");
        manifestUrl = new URL("jar:" + locationUrl.toExternalForm() + "!/" + Constants.MODULE_MANIFEST_NAME);
        assertEquals(manifestUrl, UrlHelper.locationToManifestUrl(locationUrl));

        locationUrl = new URL("file:/usr/local/app-a/module-b.JAR");
        manifestUrl = new URL("jar:" + locationUrl.toExternalForm() + "!/" + Constants.MODULE_MANIFEST_NAME);
        assertEquals(manifestUrl, UrlHelper.locationToManifestUrl(locationUrl));

        locationUrl = new URL("file:/usr/local/app-a/module-b.zip");
        manifestUrl = new URL("jar:" + locationUrl.toExternalForm() + "!/" + Constants.MODULE_MANIFEST_NAME);
        assertEquals(manifestUrl, UrlHelper.locationToManifestUrl(locationUrl));

        locationUrl = new URL("file:/usr/local/app-a/module-b.txt");
        manifestUrl = null;
        assertEquals(manifestUrl, UrlHelper.locationToManifestUrl(locationUrl));

        locationUrl = new URL("file:/usr/local/app-a/" + Constants.MODULE_MANIFEST_NAME);
        manifestUrl = null;
        assertEquals(manifestUrl, UrlHelper.locationToManifestUrl(locationUrl));
    }
}
