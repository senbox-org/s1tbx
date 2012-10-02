/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.metadata;

import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Resolves names of resources belonging to sources and target of processings.
 */
public class MetadataResourceResolver {

    public static final String VELOCITY_TEMPLATE_EXTENSION = ".vm";

    public static class TargetResourceInfo {

        public final String templateName;
        public final String templateBaseName;
        public final String targetName;

        public TargetResourceInfo(String templateName, String templateBaseName, String targetName) {
            this.templateName = templateName;
            this.templateBaseName = templateBaseName;
            this.targetName = targetName;
        }
    }

    private final SimpleFileSystem simpleFileSystem;

    public MetadataResourceResolver(SimpleFileSystem simpleFileSystem) {
        this.simpleFileSystem = simpleFileSystem;
    }

    public SortedMap<String, String> getSourceMetadataPaths(String sourcePath) throws IOException {

        final String basename = getBasename(sourcePath);
        final String dirname = getDirname(sourcePath);
        final String wantedPrefix = removeFileExtension(basename) + "-";

        SortedMap<String, String> sourceNames = new TreeMap<String, String>();
        String[] directoryList = simpleFileSystem.list(dirname);
        if (directoryList != null) {
            for (String filename : directoryList) {
                if (!filename.equalsIgnoreCase(basename) && filename.startsWith(wantedPrefix)) {
                    String metadataBaseName = filename.substring(wantedPrefix.length());
                    if (dirname.isEmpty()) {
                        sourceNames.put(metadataBaseName, filename);
                    } else {
                        sourceNames.put(metadataBaseName, dirname + "/" + filename);
                    }

                }
            }
        }
        return sourceNames;
    }

    public TargetResourceInfo getTargetName(String templatePath, String targetPath) {
        String templateName = getBasename(templatePath);
        String templateBaseName = templateName.substring(0, templateName.length() - VELOCITY_TEMPLATE_EXTENSION.length());

        String targetPathWithoutExtension = removeFileExtension(targetPath);
        String targetName;
        if (targetPath.equals(targetPathWithoutExtension)) {
            targetName = targetPath + "/" + templateBaseName;
        } else {
            targetName = targetPathWithoutExtension + "-" + templateBaseName;
        }
        return new TargetResourceInfo(templateName, templateBaseName, targetName);
    }

    String removeFileExtension(String path) {
        if (simpleFileSystem.isFile(path)) {
            int i = path.lastIndexOf('.');
            if (i > 0) {
                return path.substring(0, i);
            }
        }
        return path;
    }

    static String getBasename(String path) {
        String pathNormalized = path.replace('\\', '/');
        int i = pathNormalized.lastIndexOf('/');
        if (i >= 0) {
            return pathNormalized.substring(i + 1);
        }
        return pathNormalized;
    }

    static String getDirname(String path) {
        String pathNormalized = path.replace('\\', '/');
        int i = pathNormalized.lastIndexOf('/');
        if (i > 0) {
            return pathNormalized.substring(0, i);
        }
        return "";
    }
}
