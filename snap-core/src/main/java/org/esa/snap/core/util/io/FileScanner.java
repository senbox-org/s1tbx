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
package org.esa.snap.core.util.io;

import java.io.File;
import java.io.FileFilter;


/**
 * A utility class for scaning directories in the local filesystem.
 *
 * @author Norman Fomferra
 */
public class FileScanner {

    public static void scanDirectory(String dirPath, FileFilter fileFilter, Handler fileHandler) {
        scanDirectory(new File(dirPath), fileFilter, fileHandler);
    }

    public static void scanDirectories(String[] dirPaths, FileFilter fileFilter, Handler fileHandler) {
        for (int i = 0; i < dirPaths.length; i++) {
            scanDirectory(dirPaths[i], fileFilter, fileHandler);
        }
    }

    public static void scanDirectory(File dir, FileFilter fileFilter, Handler fileHandler) {
        scanDirectories(new File[] {dir}, fileFilter, fileHandler);
    }

    public static void scanDirectories(File[] dirs, FileFilter fileFilter, Handler fileHandler) {
        for (int i = 0; i < dirs.length; i++) {
            scanDirectoryImpl(dirs[i], fileFilter, fileHandler);
        }
    }

    private static void scanDirectoryImpl(File dir, FileFilter fileFilter, Handler fileHandler) {
        final File[] files = dir.listFiles();
        if (files == null) {
            fileHandler.onDirectoryScanRejected(dir);
            return;
        }
        fileHandler.onDirectoryScanStarted(dir);
        for (int i = 0; i < files.length; i++) {
            final File file = files[i];
            if (file.isDirectory()) {
                scanDirectoryImpl(file, fileFilter, fileHandler);
            } else if (fileFilter.accept(file)) {
                fileHandler.onFileAccepted(file);
            }
        }
        fileHandler.onDirectoryScanEnded(dir);
    }

    public static interface Handler {
        void onDirectoryScanStarted(File dir);
        void onDirectoryScanEnded(File dir);
        void onDirectoryScanRejected(File dir);
        void onFileAccepted(File file);
    }

    public static class HandlerAdapter implements Handler {
        public void onDirectoryScanEnded(File dir) {
        }

        public void onDirectoryScanStarted(File dir) {
        }

        public void onDirectoryScanRejected(File dir) {
        }

        public void onFileAccepted(File file) {
        }
    }
}
