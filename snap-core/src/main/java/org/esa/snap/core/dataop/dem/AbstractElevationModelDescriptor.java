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
package org.esa.snap.core.dataop.dem;

import org.esa.snap.core.util.SystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>AbstractElevationModelDescriptor</code> implements general behaviour
 * common to all {@link ElevationModelDescriptor}
 *
 * @author Luis Veci
 * @author Norman Fomferra
 * @author Marco Peters
 */
public abstract class AbstractElevationModelDescriptor implements ElevationModelDescriptor {

    private final static String PROPERTIES_FILE_NAME = "dem.properties";
    private final static String INSTALL_DIR_PROPERTY_NAME = "dem.installDir";
    private final static Logger LOG = Logger.getLogger(AbstractElevationModelDescriptor.class.getName());

    private final File demInstallDir;
    private final Properties properties;

    protected AbstractElevationModelDescriptor() {
        File demPropertiesDir = new File(SystemUtils.getAuxDataPath().resolve("dem").toFile(), getName());
        if (!demPropertiesDir.exists()) {
            demPropertiesDir.mkdirs();
        }
        properties = loadProperties(new File(demPropertiesDir, PROPERTIES_FILE_NAME));
        demInstallDir = getDemInstallDir(demPropertiesDir);
    }

    public File getDemInstallDir() {
        return demInstallDir;
    }

    private String getProperty(String propertyName) {
        return properties.getProperty(propertyName);
    }

    /**
     * Loads DEM properties from the specified file.
     */
    private Properties loadProperties(File propertiesFile) {
        final Properties properties = new Properties();
        if (propertiesFile.exists()) {
            try (FileInputStream stream = new FileInputStream(propertiesFile)) {
                properties.load(stream);
            } catch (IOException ioe) {
                LOG.log(Level.WARNING, String.format("Could not load properties from '%s'", propertiesFile), ioe);
            }
        }
        return properties;
    }

    private File getDemInstallDir(File defaultDirectory) {
        final String installDirPath = getProperty(INSTALL_DIR_PROPERTY_NAME);
        if (installDirPath != null && installDirPath.length() > 0) {
            final File installDir = new File(installDirPath);
            if (installDir.exists()) {
                return installDir;
            }
        }
        return defaultDirectory;
    }

}
