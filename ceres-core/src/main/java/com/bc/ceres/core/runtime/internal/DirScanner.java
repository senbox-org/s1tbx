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

import com.bc.ceres.core.Assert;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class DirScanner {

    private File baseDir;
    private boolean recursive;
    private boolean filesOnly;

    public DirScanner(File baseDir) {
        this(baseDir, false);
    }

    public DirScanner(File baseDir, boolean recursive) {
        this(baseDir, recursive, false);
    }

    public DirScanner(File baseDir, boolean recursive, boolean filesOnly) {
        Assert.notNull(baseDir, "baseDir");
        this.baseDir = baseDir;
        this.recursive = recursive;
        this.filesOnly = filesOnly;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public String[] scan() {
        return scan(new NullFilenameFilter());
    }

    public String[] scan(FilenameFilter filter) {
        Assert.notNull(filter, "filter");
        ArrayList<String> strings = new ArrayList<String>(16);
        collectFiles(filter, "", strings);
        return strings.toArray(new String[0]);
    }

    private void collectFiles(FilenameFilter filter, String relDirPath, ArrayList<String> entries) {
        File dir = new File(baseDir, relDirPath);
        String[] fileNames = dir.list();
        if (fileNames != null) {
            for (String fileName : fileNames) {
                String relFilePath = relDirPath + fileName;
                File file = new File(baseDir, relFilePath);
                if (file.isDirectory()) {
                    if (!filesOnly && filter.accept(dir, fileName)) {
                        entries.add(relFilePath);
                    }
                    if (recursive) {
                        collectFiles(filter, relFilePath + '/', entries);
                    }
                } else if (filter.accept(dir, fileName)) {
                    entries.add(relFilePath);
                }
            }
        }
    }

    private static class NullFilenameFilter implements FilenameFilter {
        /**
         * @return always returns <code>true</code>
         */
        public boolean accept(File dir, String name) {
            return true;
        }
    }
}
