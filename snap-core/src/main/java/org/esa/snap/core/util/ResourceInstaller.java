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
package org.esa.snap.core.util;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.util.io.FileUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.*;

/**
 * Installs resources from a given source to a given target.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ResourceInstaller {

    private final Path sourceBasePath;
    private final Path targetDirPath;

    /**
     * Creates an instance with a given source to a given target.
     *
     * @param sourceDirPath the source directory path
     * @param targetDirPath the target directory
     */
    public ResourceInstaller(Path sourceDirPath, Path targetDirPath) {
        this.sourceBasePath = sourceDirPath;
        this.targetDirPath = targetDirPath;
    }

    /**
     * Installs all resources found, matching the given pattern. Existing resources are left as-is
     * and are not overwritten.
     *
     * @param patternString the search pattern. Specifies the pattern and the syntax for searching for resources.
     *                      The syntax can either be 'glob:' or 'regex:'. If the syntax does not start with one of the syntax
     *                      identifiers 'regex:' is pre-pended.
     * @param pm            progress monitor for indicating progress
     * @see FileSystem#getPathMatcher(String)
     */
    public void install(String patternString, ProgressMonitor pm) throws IOException {
        if (!patternString.startsWith("glob:") && !patternString.startsWith("regex:")) {
            patternString = "regex:" + patternString;
        }

        pm.beginTask("Installing resources...", 100);
        try {
            Collection<Path> resources = collectResources(patternString);
            pm.worked(20);
            copyResources(resources, new SubProgressMonitor(pm, 80));
        } finally {
            pm.done();
        }
    }

    private void copyResources(Collection<Path> resources, ProgressMonitor pm) throws IOException {
        synchronized (ResourceInstaller.class) {
            pm.beginTask("Copying resources...", resources.size());
            try {
                for (Path resource : resources) {
                    Path relFilePath = sourceBasePath.relativize(resource);
                    String relPathString = relFilePath.toString();
                    Path targetFile = targetDirPath.resolve(relPathString);
                    if (!Files.exists(targetFile) && !Files.isDirectory(resource)) {
                        Path parentPath = targetFile.getParent();
                        if (parentPath == null) {
                            throw new IOException("Could not retrieve the parent directory of '" + targetFile.toString() + "'.");
                        }
                        Files.createDirectories(parentPath);
                        Files.copy(resource, targetFile, REPLACE_EXISTING, COPY_ATTRIBUTES);
                    }
                    pm.worked(1);
                }
            } finally {
                pm.done();
            }
        }
    }

    private Collection<Path> collectResources(String patternString) throws IOException {
        Collection<Path> resources = new ArrayList<>();
        collectResources(sourceBasePath, resources, patternString);
        return resources;
    }


    private static void collectResources(Path searchPath, Collection<Path> resourcePaths, String patternString) throws IOException {
        if (Files.isDirectory(searchPath)) {
            PathMatcher pathMatcher = searchPath.getFileSystem().getPathMatcher(patternString);
            collectResources(searchPath, resourcePaths, pathMatcher);
        } else {
            resourcePaths.add(searchPath);
        }

    }

    private static void collectResources(Path searchPath, Collection<Path> resourcePaths, PathMatcher pathMatcher) throws IOException {
        Stream<Path> files = Files.list(searchPath);
        files.forEach(path -> {
            if (pathMatcher.matches(path)) {
                resourcePaths.add(path);
            }
            if (Files.isDirectory(path)) {
                try {
                    collectResources(path, resourcePaths, pathMatcher);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });

    }

    public static Path findModuleCodeBasePath(Class clazz) {
        try {
            URI uri = clazz.getProtectionDomain().getCodeSource().getLocation().toURI();
            return FileUtils.getPathFromURI(FileUtils.ensureJarURI(uri));
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("Failed to detect the module's code base path", e);
        }
    }

}
