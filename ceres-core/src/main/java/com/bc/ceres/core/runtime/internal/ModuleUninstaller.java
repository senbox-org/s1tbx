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
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.core.runtime.ModuleState;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.logging.Logger;

/**
 * An uninstaller for modules.
 */
public class ModuleUninstaller {

    private Logger logger;

    public ModuleUninstaller(Logger logger) {
        Assert.notNull(logger, "logger");
        this.logger = logger;
    }

    public void uninstallModule(ModuleImpl module) throws IOException {
        markLocationFileAsUninstalled(module);
        module.setState(ModuleState.UNINSTALLED);
    }

    public void uninstallModules(File moduleDir, ProgressMonitor pm) {

        DirScanner dirScanner = new DirScanner(moduleDir, false);
        String[] uninstallMarkers = dirScanner.scan(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(RuntimeImpl.UNINSTALL_FILE_SUFFIX);
            }
        });

        if (uninstallMarkers.length == 0) {
            return;
        }

        pm.beginTask("Uninstalling modules", uninstallMarkers.length);
        for (String uninstallMarker : uninstallMarkers) {
            String moduleFileName = uninstallMarker.substring(0,
                                                              uninstallMarker.length() - RuntimeImpl.UNINSTALL_FILE_SUFFIX.length());
            File moduleFile = new File(moduleDir, moduleFileName);
            boolean deleted = uninstallModuleFile(moduleFile, SubProgressMonitor.create(pm, 1));
            if (deleted) {
                File uninstallMarkerFile = new File(moduleDir, uninstallMarker);
                uninstallMarkerFile.delete();
            }
        }
        pm.done();
    }

    private void markLocationFileAsUninstalled(ModuleImpl module) throws IOException {
        URL location = module.getLocation();
        logger.info(MessageFormat.format("Marking module file [{0}] for deinstallation.", location));

        File locationFile = UrlHelper.urlToFile(location);
        if (locationFile == null) {
            throw new IOException("Location is not a file.");
        }

        File markerFile = new File(locationFile.getPath() + RuntimeImpl.UNINSTALL_FILE_SUFFIX);
        FileWriter writer = new FileWriter(markerFile);
        try {
            writer.write(location.toString());
        } finally {
            writer.close();
        }
    }

    private boolean uninstallModuleFile(File locationFile, ProgressMonitor pm) {
        logger.info(MessageFormat.format("Uninstalling module [{0}]...", locationFile));
        boolean deleted = true;
        if (locationFile.exists()) {
            if (locationFile.isDirectory()) {
                deleted = uninstallModuleDirectory(locationFile, pm);
            } else {
                deleted = uninstallModuleArchive(locationFile, pm);
            }
        }
        return deleted;
    }

    private static boolean uninstallModuleArchive(File archiveFile, ProgressMonitor pm) {
        pm.beginTask(MessageFormat.format("Uninstalling {0}", archiveFile.getName()), 1);
        try {
            return archiveFile.delete();
        } finally {
            pm.done();
        }
    }

    private boolean uninstallModuleDirectory(File moduleDir, ProgressMonitor pm) {
        File installInfoFile = new File(moduleDir, ModuleInstaller.INSTALL_INFO_XML);

        InstallInfo installInfo;
        try {
            FileReader reader = new FileReader(installInfoFile);
            installInfo = null;
            try {
                installInfo = InstallInfo.read(reader);
            } finally {
                reader.close();
            }
        } catch (FileNotFoundException e) {
            logger.warning(
                    MessageFormat.format("[{0}] not found.", installInfoFile.getName()));
            logger.warning(
                    MessageFormat.format("Please remove directory [{1}] manually to get rid of this warning.", installInfoFile.getName(), moduleDir));
            return false;
        } catch (IOException e) {
            logger.warning(
                    MessageFormat.format("Failed to read [{0}]: {1}", installInfoFile.getName(), e.getMessage()));
            return false;
        }

        String[] items = installInfo.getItems();
        pm.beginTask(MessageFormat.format("Uninstalling {0}", moduleDir.getName()), items.length);
        try {
            for (int i = items.length - 1; i >= 0; --i) {
                String item = items[i];
                File installedFile = new File(moduleDir, item);
                pm.setTaskName(MessageFormat.format("Uninstalling {0}", installedFile.getName()));
                if (installedFile.isFile()) {
                    long installTime = installInfo.getDate().getTime();
                    long lastModifiedTime = installedFile.lastModified();
                    long oneSecond = 1000L;
                    boolean fileIsOlderOrEqualInstallTime = lastModifiedTime <= installTime + oneSecond;
                    if (fileIsOlderOrEqualInstallTime) {
                        delete(installedFile);
                    } else {
                        logger.warning(MessageFormat.format("Module file component [{0}] has been modified since installation.",
                                                            installedFile));
                    }
                } else if (installedFile.isDirectory()) {
                    delete(installedFile);
                } else {
                    logger.warning(MessageFormat.format("Module file component [{0}] no longer exists.", installedFile));
                }
                pm.worked(1);
            }
            delete(installInfoFile);
            delete(moduleDir);
        } finally {
            pm.done();
        }

        return moduleDir.exists();
    }

    private void delete(File file) {
        if (file.delete()) {
            logger.info(MessageFormat.format("Deleted module file component [{0}].", file));
        } else {
            logger.warning(MessageFormat.format("Unable to delete module file component [{0}].", file));
        }
    }
}
