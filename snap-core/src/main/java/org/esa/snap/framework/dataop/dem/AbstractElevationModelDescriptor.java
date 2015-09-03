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
package org.esa.snap.framework.dataop.dem;

import org.esa.snap.util.SystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>AbstractElevationModelDescriptor</code> offers a default implementation for the {@link #installDemFiles(Object)}
 * method. It uses the properties values returned {@link #getDemArchiveUrl()} and {@link #getDemInstallDir()} methods
 * in order to download and install the DEM.
 *
 * @author Norman Fomferra
 * @version $Revision$
 */
public abstract class AbstractElevationModelDescriptor implements ElevationModelDescriptor {

    private final static Logger LOG = Logger.getLogger(AbstractElevationModelDescriptor.class.getName());


    public static final String PROPERTIES_FILE_NAME = "dem.properties";
    public static final String INSTALL_DIR_PROPERTY_NAME = "dem.installDir";

    private final File demPropertiesDir;

    private File demInstallDir;

    protected AbstractElevationModelDescriptor() {
        demPropertiesDir = new File(SystemUtils.getAuxDataPath().resolve("dem").toFile(), getName());
        if (!demPropertiesDir.exists()) {
            demPropertiesDir.mkdirs();
        }
        demInstallDir = demPropertiesDir;
        maybeOverwriteDemInstallDir();
    }

    /**
     * Gets the DEM properties file "${USER_APP_DATA}/auxdata/dem/${DEM}/dem.properties".
     */
    public File getDemPropertiesFile() {
        return new File(demPropertiesDir, PROPERTIES_FILE_NAME);
    }

    public File getDemInstallDir() {
        return demInstallDir;
    }

    public void setDemInstallDir(File demInstallDir) {
        this.demInstallDir = demInstallDir;
    }

    public int getInstallationStatus() {
        return DEM_INSTALLED;
    }

    public boolean installDemFiles(Object uiComponent) {
        return true;
    }

    /**
     * Loads DEM properties from the "${USER_APP_DATA}/auxdata/dem/${DEM}/dem.properties" file.
     */
    protected Properties loadProperties() {
        final Properties properties = new Properties();
        final File propertiesFile = getDemPropertiesFile();
        if (propertiesFile.exists()) {
            try (FileInputStream stream = new FileInputStream(propertiesFile)) {
                properties.load(stream);
            } catch (IOException ioe) {
                LOG.log(Level.WARNING, String.format("Could not load properties from '%s'", propertiesFile), ioe);
            }
        }
        return properties;
    }

    private void maybeOverwriteDemInstallDir() {
        final Properties properties = loadProperties();
        final String installDirPath = properties.getProperty(INSTALL_DIR_PROPERTY_NAME);
        if (installDirPath != null && installDirPath.length() > 0) {
            final File installDir = new File(installDirPath);
            if (installDir.exists()) {
                demInstallDir = installDir;
            }
        }
    }
}
