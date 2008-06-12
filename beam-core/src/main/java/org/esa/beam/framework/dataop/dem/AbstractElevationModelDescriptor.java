/*
 * $Id: AbstractElevationModelDescriptor.java,v 1.7 2007/04/20 09:09:31 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.dataop.dem;

import org.esa.beam.util.Debug;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.FileChooserFactory;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Date;
import java.util.Properties;

/**
 * The <code>AbstractElevationModelDescriptor</code> offers a default implementation for the {@link #installDemFiles(Object)}
 * method. It uses the properties values returned {@link #getDemArchiveUrl()} and {@link #getDemInstallDir()} methods
 * in order to download and install the DEM.
 *
 * @author Norman Fomferra
 * @version $Revision$
 */
public abstract class AbstractElevationModelDescriptor implements ElevationModelDescriptor {

    public static final String PROPERTIES_FILE_NAME = "dem.properties";
    public static final String INSTALL_DIR_PROPERTY_NAME = "dem.installDir";

    private final File demPropertiesDir;

    private ElevationModelInstaller installer;
    private File demInstallDir;

    protected AbstractElevationModelDescriptor() {
        demPropertiesDir = new File(SystemUtils.getApplicationDataDir(),
                                    "beam-core/auxdata/dem" + File.separator + getName());
        if (!demPropertiesDir.exists()) {
            demPropertiesDir.mkdirs();
        }
        demInstallDir = demPropertiesDir;
        maybeOverwriteDemInstallDir();
    }

    /**
     * Gets the DEM properties file "${BEAM-HOME}/beam-core/auxdata/dem/${DEM}/dem.properties".
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

    public boolean isInstallingDem() {
        return getInstallationStatus() == DEM_INSTALLATION_IN_PROGRESS;
    }

    public int getInstallationStatus() {
        if (isDemInstalled()) {
            return DEM_INSTALLED;
        }
        if (installer != null) {
            return installer.getStatus();
        }
        return 0;
    }

    public boolean installDemFiles(Object uiComponent) {
        if (isDemInstalled()) {
            return true;
        }
        if (isInstallingDem()) {
            return true;
        }
        final Component parent = uiComponent instanceof Component ? (Component) uiComponent : null;
        final String title = "Missing DEM"; /*I18N*/
        String message = "The DEM '" + getName() + "' is not installed yet.\n" +
                         "Please visit the BEAM home page at " + SystemUtils.BEAM_HOME_PAGE + "\n" +
                         "in order to download and install it.\n";  /*I18N*/
        if (getDemInstallDir() == null || getDemArchiveUrl() == null) {
            JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
            return false;
        }

        final int archiveSizeB;
        try {
            final URLConnection urlConnection = getDemArchiveUrl().openConnection();
            archiveSizeB = urlConnection.getContentLength();
        } catch (IOException e) {
            Debug.trace(e);
            JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
            return false;
        }

        double archiveSizeMB = Math.round(10.0 * archiveSizeB / (1024 * 1024)) / 10.0;

        message += "\n" +
                   "BEAM can also download and install the DEM for you now.\n" +
                   "The size of the DEM archive to be downloaded is " + archiveSizeMB + " MB,\n" +
                   "total disk space for the DEM will be around 8x the archive size.\n" +
                   "You can continue using BEAM while the DEM is installed.\n\n" +
                   "Do you wish to install the DEM now?";   /*I18N*/
        final int answer = JOptionPane.showConfirmDialog(parent,
                                                         message,
                                                         title,
                                                         JOptionPane.YES_NO_OPTION,
                                                         JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return false;
        }

        final File installDir = promptForDemInstallDir(parent);
        if (installDir == null) {
            return false;
        }
        setDemInstallDir(installDir);
        installer = new ElevationModelInstaller(this, parent);
        installer.execute();
        return true;
    }

    /**
     * Loads DEM properties from the "${BEAM-HOME}/beam-core/auxdata/dem/${DEM}/dem.properties" file.
     */
    protected Properties loadProperties() {
        final Properties properties = new Properties();
        final File propertiesFile = getDemPropertiesFile();
        if (propertiesFile.exists()) {
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(propertiesFile);
                properties.load(stream);
            } catch (IOException e) {
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        return properties;
    }

    /**
     * Stores DEM properties in the "${BEAM-HOME}/beam-core/auxdata/dem/${DEM}/dem.properties" file.
     */
    protected void storeProperties() {
        final Properties properties = new Properties();
        properties.put("dem.name", getName());
        properties.put("dem.datum", getDatum().getName());
        properties.put("dem.archiveUrl", getDemArchiveUrl().toExternalForm());
        properties.put(INSTALL_DIR_PROPERTY_NAME, getDemInstallDir().getPath());
        properties.put("dem.installTime", new Date().toString());
        properties.put("dem.version", "1.0");
        storeProperties(properties);
    }

    /**
     * Stores DEM properties in the "${BEAM-HOME}/beam-core/auxdata/dem/${DEM}/dem.properties" file.
     *
     * @param properties the properties
     */
    protected void storeProperties(final Properties properties) {
        final File demPropertiesFile = getDemPropertiesFile();
        final File parentDir = demPropertiesFile.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
        }
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(getDemPropertiesFile());
            properties.store(stream, "BEAM DEM Properties File");
        } catch (IOException e) {
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Prompts the user for the installation directory.
     *
     * @param parent the parent UI component
     *
     * @return the installation directory or null if the user canceled the directory selection
     */
    protected File promptForDemInstallDir(final Component parent) {
        final File demInstallDir = getDemInstallDir();
        File initDir = demInstallDir;
        if (!initDir.exists()) {
            initDir = demPropertiesDir;
        }
        final JFileChooser fileChooser = FileChooserFactory.getInstance().createDirChooser(initDir);
        final String dialogTitle = "Select DEM Installation Directory";
        fileChooser.setDialogTitle(dialogTitle); /*I18N*/
        fileChooser.setCurrentDirectory(demInstallDir);
        final int selectAnswer = fileChooser.showDialog(parent, "Select"); /*I18N*/
        final File selectedDir = fileChooser.getSelectedFile();
        if (selectAnswer == JFileChooser.APPROVE_OPTION && selectedDir != null) {
            if (!selectedDir.exists()) {
                final int createAnswer = JOptionPane.showConfirmDialog(parent,
                                                                       "The selected directory\n" +
                                                                       selectedDir.getPath() + "\n" +
                                                                       "does not exists. Shall it be created?",
                                                                       dialogTitle,
                                                                       JOptionPane.YES_NO_OPTION,
                                                                       JOptionPane.QUESTION_MESSAGE);
                if (createAnswer != JOptionPane.YES_OPTION) {
                    return null;
                }
            }
            return selectedDir;
        }
        return null;
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
