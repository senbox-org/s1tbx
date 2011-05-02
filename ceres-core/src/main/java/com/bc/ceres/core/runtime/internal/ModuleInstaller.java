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
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.core.runtime.ProxyConfig;
import com.bc.ceres.core.runtime.ModuleState;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.logging.Logger;

/**
 * An uninstaller for modules.
 */
public class ModuleInstaller {

    public static final String INSTALL_INFO_XML = "install-info.xml";

    private Logger logger;

    public ModuleInstaller(Logger logger) {
        Assert.notNull(logger, "logger");
        this.logger = logger;
    }

    public ModuleImpl installModule(URL url, ProxyConfig proxyConfig, File modulesDir, ProgressMonitor pm) throws CoreException {
        Assert.notNull(url, "url");
        Assert.notNull(proxyConfig, "proxyConfig");
        Assert.notNull(modulesDir, "modulesDir");
        Assert.notNull(pm, "pm");

        pm.beginTask("Installing module", 100);

        logger.info(MessageFormat.format("Installing [{0}] in [{1}]...", url, modulesDir));

        try {
            String fileName = IOHelper.getFileName(url);
            File tempFile = new File(modulesDir, fileName + ".incomplete");
            File targetFile = new File(modulesDir, fileName);

            try {
                logger.info(MessageFormat.format("Downloading [{0}] to [{1}]...", url, tempFile.getName()));
                pm.setSubTaskName(MessageFormat.format("Downloading [{0}]", fileName));
                URLConnection urlConnection = UrlHelper.openConnection(url, proxyConfig, "GET");
                IOHelper.copy(urlConnection, tempFile, SubProgressMonitor.create(pm, 90));

                logger.info(MessageFormat.format("Copying [{0}] to [{1}]...", tempFile, fileName));
                pm.setSubTaskName(MessageFormat.format("Copying [{0}]", fileName));
                IOHelper.copy(tempFile, targetFile, SubProgressMonitor.create(pm, 10));
            } finally {
                if (!tempFile.delete()) {
                    logger.warning(MessageFormat.format("Failed to delete file [{0}], reason unknown.", tempFile));
                }
            }

            ModuleReader moduleReader = new ModuleReader(logger);
            ModuleImpl module = moduleReader.readFromLocation(targetFile);
            module.setState(ModuleState.INSTALLED);
            return module;
        } catch (CoreException e) {
            e.printStackTrace();
            throw e;
        } catch (IOException e) {
            e.printStackTrace();
            throw new CoreException("Failed to install module [" + url + "]: " + e.getMessage(), e);
        } finally {
            pm.done();
        }
    }
}
