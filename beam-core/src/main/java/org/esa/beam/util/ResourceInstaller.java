/*
 * $Id: ResourceInstaller.java,v 1.7 2007/03/23 18:03:33 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.util;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;

/**
 * Installls resources from a given source to a given target.
 *
 * @author Marco Peters
 * @version $Revision: 1.7 $ $Date: 2007/03/23 18:03:33 $
 */
public class ResourceInstaller {

    private ResourceScanner scanner;
    private URL targetUrl;

    /**
     * Creates an instance with a given source to a given target.
     *
     * @param sourceUrl     the source
     * @param relSourcePath
     * @param targetUrl     the target
     */
    public ResourceInstaller(URL sourceUrl, String relSourcePath, URL targetUrl) {
        this.targetUrl = targetUrl;
        scanner = new ResourceScanner(new URL[]{sourceUrl}, relSourcePath);
    }

    /**
     * Installs all resources found, matching the given pattern
     *
     * @param patternString the pattern
     * @param pm
     */
    public void install(String patternString, ProgressMonitor pm) {
        try {
            pm.beginTask("Installing resource data: ", 2);
            scanner.scan(SubProgressMonitor.create(pm, 1));
            URL[] resources = scanner.getResourcesByPattern(patternString);
            copyResources(resources, SubProgressMonitor.create(pm, 1));
        } finally {
            pm.done();
        }
    }

    private void copyResources(URL[] resources, ProgressMonitor pm) {
        pm.beginTask("Copying resource data...", resources.length);
        for (URL resource : resources) {
            String relFilePath = scanner.getRelativePath(resource);

            File targetFile = null;
            try {
                targetFile = new File(new File(targetUrl.toURI()), relFilePath);
            } catch (URISyntaxException e) {
                // should not come here
            }
            if (!targetFile.exists() && !resource.toExternalForm().endsWith("/")) {
                InputStream is = null;
                FileOutputStream fos = null;
                try {
                    is = resource.openStream();
                    targetFile.getParentFile().mkdirs();
                    targetFile.createNewFile();
                    fos = new FileOutputStream(targetFile);
                    byte[] bytes = new byte[100];
                    int bytesRead = is.read(bytes);
                    while (bytesRead != -1) {
                        fos.write(bytes, 0, bytesRead);
                        bytesRead = is.read(bytes);
                    }
                } catch (IOException e) {
                    BeamLogManager.getSystemLogger().log(Level.WARNING,
                                                         "Not able to copy resource [" + resource + "] to user directory",
                                                         e);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                }
            }
            pm.worked(1);
        }
    }
}
