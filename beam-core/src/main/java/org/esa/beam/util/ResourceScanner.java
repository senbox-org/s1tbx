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
package org.esa.beam.util;

import com.bc.ceres.core.ProgressMonitor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This scanner can be used to retrieve resources, e.g. auxillary data.
 * The scanner searches for all resources in the given locations.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ResourceScanner {

    private URL[] scanLocations;
    private Map<String, URL> resourcesMap = new HashMap<String, URL>();
    private String relPath;

    /**
     * Creates an instance of this class with the given locations.
     *
     * @param locations the locations used for scanning
     * @param relPath
     */
    public ResourceScanner(URL[] locations, String relPath) {
        scanLocations = locations;
        this.relPath = relPath;
    }

    /**
     * Scans recursivley for resources in the location given by the constructor.
     * Afterwards you can retrieve the resource by the multiple getter methods
     */
    public void scan(ProgressMonitor pm) {
        pm.beginTask("Scanning for resources...", scanLocations.length);
        for (int i = 0; i < scanLocations.length; i++) {
            URL scanLocation = scanLocations[i];
            ArrayList<URL> resourceUrls = new ArrayList<URL>();
            collectResources(scanLocation, relPath, resourceUrls);
            resourcesMap.putAll(splitResourceUrls(scanLocation, relPath, resourceUrls));
            pm.worked(1);
        }
    }

    /**
     * Gets the resource with the given relative path.
     *
     * @param relPath the relative path to the resource.
     * @return the url to the resource found. Can be <code>null</code> if no resource was found.
     */
    public URL getResource(String relPath) {
        return resourcesMap.get(relPath);
    }

    /**
     * Returns the relative path of the given resource url
     *
     * @param resourceURL
     * @return the realtive path, may be <code>null</code>
     */
    public String getRelativePath(URL resourceURL) {
        Set<Map.Entry<String, URL>> entries = resourcesMap.entrySet();
        for (Map.Entry<String, URL> entry : entries) {
            if (entry.getValue().equals(resourceURL)) {
                return entry.getKey();
            }
        }
        return null;
    }

// todo - simplify pattern: client should only specify something like the Ant file pattern (e.g. */**)

    /**
     * Retrieves all resource matching the given pattern.
     * For an explanation of the pattern syntax see {@link Pattern}.
     *
     * @param patternString the pattern given as a regular expression
     * @return all resources matching the given pattern. Never <code>null</code>.
     */
    public URL[] getResourcesByPattern(String patternString) {
        Pattern pattern = Pattern.compile(patternString);
        ArrayList<URL> resourceUrls = new ArrayList<URL>();

        collectMatchingUrls(resourcesMap, pattern, resourceUrls);

        return resourceUrls.toArray(new URL[resourceUrls.size()]);
    }


    private static void collectResources(URL location, String relPath, ArrayList<URL> resourceUrls) {
        if (isUrlToCompressedFile(location)) {
            collectResourcesFromJar(location, relPath, resourceUrls);
        } else {
            collectResourcesFromDir(location, relPath, resourceUrls);
        }
    }

    private static void collectMatchingUrls(Map<String, URL> resourcesMap, Pattern pattern,
                                            ArrayList<URL> resourceUrls) {
        for (Map.Entry<String, URL> entry : resourcesMap.entrySet()) {
            Matcher matcher = pattern.matcher(entry.getKey());
            if (matcher.matches()) {
                resourceUrls.add(entry.getValue());
            }
        }
    }

    private static Map<String, URL> splitResourceUrls(URL baseLocation, String relPath, Collection<URL> resourceUrls) {
        HashMap<String, URL> resourceMap = new HashMap<String, URL>(200);

        try {
            if (isUrlToCompressedFile(baseLocation)) {
                baseLocation = new URL("jar:" + baseLocation + "!/" + relPath);
            } else {
                baseLocation = new URL(baseLocation + relPath);
            }
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
        for (Iterator<URL> iterator = resourceUrls.iterator(); iterator.hasNext();) {
            URL url = iterator.next();
            String extForm = url.toExternalForm();
            String relResourcePath = extForm.substring(baseLocation.toExternalForm().length());
            resourceMap.put(relResourcePath, url);
        }
        return resourceMap;
    }

    private static void collectResourcesFromJar(URL urlTojar, String relPath, Collection<URL> resourceUrls) {
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(new File(urlTojar.toURI()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }

        Enumeration<JarEntry> jarEnum = jarFile.entries();
        while (jarEnum.hasMoreElements()) {
            JarEntry jarEntry = jarEnum.nextElement();
            String name = jarEntry.getName();
            if (name.startsWith(relPath)) {
                try {
                    URL resourceUrl = new URL("jar:" + urlTojar + "!/" + name);
                    resourceUrls.add(resourceUrl);
                } catch (MalformedURLException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    private static void collectResourcesFromDir(URL baseLocation, String relPath, Collection<URL> resourceUrls) {
        try {
            File auxdataDir = new File(new File(baseLocation.toURI()), relPath);
            collectResourcesFromDir(auxdataDir, resourceUrls);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }

    }

    private static void collectResourcesFromDir(File dir, Collection<URL> resourceUrls) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    resourceUrls.add(file.toURI().toURL());
                } catch (MalformedURLException e) {
                    throw new IllegalStateException(e);
                }
                if (file.isDirectory()) {
                    collectResourcesFromDir(file, resourceUrls);
                }
            }
        }

    }

    private static boolean isUrlToCompressedFile(URL location) {
        return location.toExternalForm().toLowerCase().endsWith(
                ".jar") || location.toExternalForm().toLowerCase().endsWith(".zip");
    }


}
