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
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.core.CanceledException;
import static com.bc.ceres.core.runtime.Constants.MODULE_MANIFEST_NAME;
import com.bc.ceres.core.runtime.ModuleState;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A loader for modules.
 */
public class ModuleLoader {

    private static final String MODULE_LOCATION_REJECTED_0 = "Module location rejected: [{0}]";

    private Logger logger;
    private HashSet<URL> visitedLocations;

    public ModuleLoader(Logger logger) {
        Assert.notNull(logger, "logger");
        this.logger = logger;
        this.visitedLocations = new HashSet<>(32);
    }

    public ModuleImpl[] loadModules(ClassLoader classLoader, ProgressMonitor pm) throws IOException {
        Assert.notNull(classLoader, "classLoader");
        Assert.notNull(pm, "pm");

        Enumeration<URL> resources = classLoader.getResources(MODULE_MANIFEST_NAME);
        ArrayList<URL> resourceList = new ArrayList<>(32);
        while (resources.hasMoreElements()) {
            resourceList.add(resources.nextElement());
        }

        pm.beginTask("Scanning classpath for modules", resourceList.size());
        try {
            ArrayList<ModuleImpl> moduleList = new ArrayList<>(32);
            for (URL manifestUrl : resourceList) {
                URL locationUrl = UrlHelper.manifestToLocationUrl(manifestUrl);
                if (locationUrl != null) {
                    if (!visitedLocations.contains(locationUrl)) {
                        try {
                            ModuleImpl module = new ModuleReader(logger).readFromLocation(locationUrl);
                            module.setState(ModuleState.INSTALLED);
                            moduleList.add(module);
                            visitedLocations.add(locationUrl);
                        } catch (CoreException e) {
                            logger.log(Level.WARNING, MessageFormat.format(MODULE_LOCATION_REJECTED_0, locationUrl), e);
                        }
                    }
                } else {
                    logger.log(Level.WARNING, MessageFormat.format(MODULE_LOCATION_REJECTED_0, manifestUrl));
                }
                pm.worked(1);
            }
            return moduleList.toArray(new ModuleImpl[moduleList.size()]);
        } finally {
            pm.done();
        }
    }

    public ModuleImpl[] loadModules(File modulesDir, ProgressMonitor pm) throws IOException {
        Assert.notNull(modulesDir, "modulesDir");
        Assert.notNull(pm, "pm");

        if (!modulesDir.isDirectory()) {
            throw new IOException(MessageFormat.format("Directory not found: [{0}]", modulesDir));
        }
        File[] moduleFiles = modulesDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return JarFilenameFilter.isJarName(file.getPath()) || file.isDirectory();
            }
        });
        if (moduleFiles == null) {
            return ModuleImpl.EMPTY_ARRAY;
        }

        pm.beginTask("Scanning directory for modules", moduleFiles.length);
        try {
            ArrayList<ModuleImpl> moduleList = new ArrayList<>(32);
            for (File moduleFile : moduleFiles) {
                File uninstallMarker = new File(moduleFile.getPath() + RuntimeImpl.UNINSTALL_FILE_SUFFIX);
                int toWork = 1;
                if (uninstallMarker.exists()) {
                    logger.warning(MessageFormat.format("Skipping uninstalled (but not yet deleted) module file [{0}].", moduleFile));
                } else {
                    URL locationUrl = UrlHelper.fileToUrl(moduleFile);
                    if (!visitedLocations.contains(locationUrl)) {
                        try {
                            ModuleImpl module = loadModule(moduleFile, SubProgressMonitor.create(pm, 1));
                            moduleList.add(module);
                            visitedLocations.add(locationUrl);
                            toWork = 0;
                        } catch (CoreException e) {
                            logger.log(Level.WARNING, MessageFormat.format(MODULE_LOCATION_REJECTED_0, locationUrl), e);
                        }
                    }
                }
                pm.worked(toWork);
            }
            return moduleList.toArray(new ModuleImpl[moduleList.size()]);
        } finally {
            pm.done();
        }
    }


    public ModuleImpl loadModule(File moduleFile, ProgressMonitor pm) throws CoreException {
        pm.beginTask("Loading module", 2);
        try {
            ModuleImpl module = new ModuleReader(logger).readFromLocation(moduleFile);
            pm.worked(1);
            if ("dir".equalsIgnoreCase(module.getPackaging())
                    && !moduleFile.isDirectory()) {
                logger.info(MessageFormat.format("Unpacking [{0}]...", moduleFile.getName()));
                File archiveFile = moduleFile;
                try {
                    moduleFile = unpack(archiveFile, module.isNative(), SubProgressMonitor.create(pm, 1));
                } catch (IOException e) {
                    throw new CoreException("Failed to install module [" + moduleFile + "]", e);
                } finally {
                    if (!archiveFile.delete()) {
                        logger.warning(MessageFormat.format("Failed to delete file [{0}], reason unknown.", archiveFile));
                    }
                }
                module = new ModuleReader(logger).readFromLocation(moduleFile);
            } else {
                pm.worked(1);
            }
            module.setState(ModuleState.INSTALLED);
            return module;
        } finally {
            pm.done();
        }
    }

    private File unpack(File archiveFile, boolean isNative, ProgressMonitor pm) throws IOException, CanceledException {
        String dirName = IOHelper.getBaseName(archiveFile);
        File moduleDir = new File(archiveFile.getParent(), dirName);

        List<String> installedFiles = IOHelper.unpack(archiveFile, moduleDir, isNative, pm);

        try {
            writeInstallInfo(moduleDir, installedFiles);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to write install info file.", e);
        }

        return moduleDir;
    }

    private void writeInstallInfo(File moduleDir, List<String> installedFiles) throws IOException {
        File installInfoFile = new File(moduleDir, ModuleInstaller.INSTALL_INFO_XML);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(installInfoFile))) {
            InstallInfo installInfo = new InstallInfo(installedFiles.toArray(new String[installedFiles.size()]));
            installInfo.write(writer);
        }
    }
}
