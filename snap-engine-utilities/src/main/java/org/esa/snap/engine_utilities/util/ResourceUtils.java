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
package org.esa.snap.engine_utilities.util;

import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.TreeCopier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
        if (!file.delete())
            System.out.println("Could not delete " + file.getName());
    }

    public static Properties loadProperties(final String filename) throws IOException {
        try (final InputStream dbPropInputStream = getResourceAsStream(filename, ResourceUtils.class)) {
            return loadProperties(dbPropInputStream);
        }
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

    public static InputStream getResourceAsStream(final String filename, final Class theClass) throws IOException {
        final Path basePath = ResourceInstaller.findModuleCodeBasePath(theClass);
        return Files.newInputStream(basePath.resolve(filename));
    }

    public static File getReportFolder() {
        final File reportFolder = new File(SystemUtils.getApplicationDataDir(true) + File.separator + "var" + File.separator + "log");
        if (!reportFolder.exists()) {
            if(!reportFolder.mkdirs()) {
                SystemUtils.LOG.severe("Unable to create folders in "+reportFolder);
            }
        }
        return reportFolder;
    }

    public static Path getGraphFolder(final String subFolder) {
        return SystemUtils.getApplicationDataDir().toPath().resolve("graphs").resolve(subFolder);
    }

    public static File getResFolder() {
        return new File(SystemUtils.getApplicationHomeDir(), "resource");
    }

    public static void installGraphs(final Class callingClass, final String path) {
        installFiles(callingClass, path, getGraphFolder(""));
    }

    public static void installFiles(final Class callingClass, final String srcResPath, final Path dstPath) {
        final Path moduleBasePath = ResourceInstaller.findModuleCodeBasePath(callingClass);
        final Path srcGraphPath = moduleBasePath.resolve(srcResPath);

        try {
            if (!Files.exists(dstPath)) {
                Files.createDirectories(dstPath);
            }
            TreeCopier.copy(srcGraphPath, dstPath);
        } catch (IOException e) {
            SystemUtils.LOG.severe("Unable to install files "+srcGraphPath+" to "+dstPath+' '+e.getMessage());
        }
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
