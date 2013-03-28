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
import com.bc.ceres.core.SubProgressMonitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;

/**
 * Installs resources from a given source to a given target.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ResourceInstaller {

    private ResourceScanner scanner;
    private File targetDir;

    /**
     * Creates an instance with a given source to a given target.
     *
     * @param sourceBaseUrl the source's base URL
     * @param sourceRelPath the source's relative path
     * @param targetDir     the target directory
     */
    public ResourceInstaller(URL sourceBaseUrl, String sourceRelPath, File targetDir) {
        this.targetDir = targetDir;
        scanner = new ResourceScanner(new URL[]{sourceBaseUrl}, sourceRelPath);
    }

    /**
     * Installs all resources found, matching the given pattern. Existing resources are left as-is
     * and are not overwritten.
     *
     * @param patternString the pattern
     * @param pm            progress monitor for indicating progress
     */
    public void install(String patternString, ProgressMonitor pm) throws IOException {
        try {
            pm.beginTask("Installing resource data: ", 2);
            scanner.scan(SubProgressMonitor.create(pm, 1));
            URL[] resources = scanner.getResourcesByPattern(patternString);
            copyResources(resources, SubProgressMonitor.create(pm, 1));
        } finally {
            pm.done();
        }
    }

    private void copyResources(URL[] resources, ProgressMonitor pm) throws IOException {
        pm.beginTask("Copying resource data...", resources.length);
        for (URL resource : resources) {
            String relFilePath = scanner.getRelativePath(resource);

            File targetFile = new File(targetDir, relFilePath);
            if (!targetFile.exists() && !resource.toExternalForm().endsWith("/")) {
                InputStream is = null;
                FileOutputStream fos = null;
                try {
                    is = resource.openStream();
                    File parentFile = targetFile.getParentFile();
                    if (parentFile == null) {
                        throw new IOException("Could not retrieve the parent directory of '" + targetFile.getAbsolutePath() + "'.");
                    }
                    parentFile.mkdirs();
                    targetFile.createNewFile();
                    fos = new FileOutputStream(targetFile);
                    byte[] bytes = new byte[1024 * 1024];
                    int bytesRead = is.read(bytes);
                    while (bytesRead != -1) {
                        fos.write(bytes, 0, bytesRead);
                        bytesRead = is.read(bytes);
                    }
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException ignored) {
                            // Ignore
                        }
                    }
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException ignored) {
                            // Ignore
                        }
                    }
                }
            }
            pm.worked(1);
        }
    }

    public static URL getSourceUrl(Class aClass) {
        CodeSource codeSource = aClass.getProtectionDomain().getCodeSource();
        URL sourceLocation;
        if (codeSource != null) {
            sourceLocation = codeSource.getLocation();
        } else {
            sourceLocation = aClass.getResource("/");
        }
        return sourceLocation;
    }

}
