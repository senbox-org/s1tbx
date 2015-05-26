/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;

/**
 * Look up paths to resources
 */
public final class ResourceUtils {

    public static void deleteFile(final File file) {
        if (file.isDirectory()) {
            for (String aChild : file.list()) {
                deleteFile(new File(file, aChild));
            }
        }
        //final Product prod = ProductCache.instance().getProduct(file);
        //if (prod != null) {
        //    ProductCache.instance().removeProduct(file);
        //    prod.dispose();
        //}
        if (!file.delete())
            System.out.println("Could not delete " + file.getName());
    }

    public static Properties loadProperties(final String filename) throws IOException {
        final InputStream dbPropInputStream = getResourceAsStream(filename);
        return loadProperties(dbPropInputStream);
    }

    public static Properties loadProperties(final InputStream dbPropInputStream) throws IOException {
        final Properties dbProperties = new Properties();
        try {
            dbProperties.load(dbPropInputStream);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return dbProperties;
    }

    public static InputStream getResourceAsStream(final String filename) throws IOException {
        return getResourceAsStream(filename, ResourceUtils.class);
    }

    public static InputStream getResourceAsStream(final String filename, final Class theClass) throws IOException {
        // Try to load resource from jar
        final InputStream stream = ClassLoader.getSystemResourceAsStream(filename);
        if (stream != null) return stream;

        // If not found in jar, then load from disk
        final java.net.URL resURL = theClass.getClassLoader().getResource(filename);
        if (resURL != null) {
            return theClass.getClassLoader().getResourceAsStream(filename);
        }

        return new FileInputStream(filename);
    }

    /**
     * get the temporary data folder in the user's application data directory
     *
     * @return the temp folder
     */
    public static File getApplicationUserTempDataDir() {
        final File tmpDir = new File(SystemUtils.getApplicationDataDir(true), "temp");
        if (!tmpDir.exists())
            tmpDir.mkdirs();
        return tmpDir;
    }

    public static Path getGraphFolder(final String subFolder) {
        return SystemUtils.getApplicationDataDir().toPath().resolve("graphs").resolve(subFolder);
    }

    public static File getResFolder() {
        return new File(SystemUtils.getApplicationHomeDir(), "resource");
    }

    public static void sortFileList(final File[] filelist) {
        Comparator<File> byDirThenAlpha = new DirAlphaComparator();

        Arrays.sort(filelist, byDirThenAlpha);
    }

    private static class DirAlphaComparator implements Comparator<File> {

        // Comparator interface requires defining compare method.
        public int compare(File filea, File fileb) {
            //... Sort directories before files, otherwise alphabetical ignoring case.
            if (filea.isDirectory() && !fileb.isDirectory()) {
                return -1;
            } else if (!filea.isDirectory() && fileb.isDirectory()) {
                return 1;
            } else {
                return filea.getName().compareToIgnoreCase(fileb.getName());
            }
        }
    }
}
