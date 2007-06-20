/*
 * $Id: FileScanner.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package com.bc.io;

import java.io.File;
import java.io.FileFilter;


/**
 * A utility class for scaning directories in the local filesystem.
 *
 * @author Norman Fomferra
 * @version
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
