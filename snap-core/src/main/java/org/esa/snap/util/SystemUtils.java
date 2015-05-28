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
package org.esa.snap.util;

import org.esa.snap.runtime.Config;
import org.geotools.referencing.factory.epsg.HsqlEpsgDatabase;

import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.swing.UIManager;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A collection of (BEAM-) system level functions.
 * <p>
 * <p> All functions have been implemented with extreme caution in order to provide a maximum performance.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 */
public class SystemUtils {

    public static final String SNAP_PARALLELISM_PROPERTY_NAME = getApplicationContextId() + ".parallelism";

    private static final String LAX_INSTALL_DIR_PROPERTY_NAME = "lax.root.install.dir";

    /**
     * The SNAP system logger. Default name is "org.esa.snap" which may be overridden by system property "snap.logger.name".
     */
    public static final Logger LOG = Config.instance().logger();

    public static final String LS = System.getProperty("line.separator");

    private static final char _URL_DIR_SEPARATOR_CHAR = '/';

    /**
     * Name of BEAM's extensions directory.
     */
    public static final String EXTENSION_DIR_NAME = "extensions";

    /**
     * Name of BEAM's auxdata directory.
     */
    public static final String AUXDATA_DIR_NAME = "auxdata";
    public static final String CACHE_DIR_NAME = "cache";
    private static final String _H5_CLASS_NAME = "ncsa.hdf.hdf5lib.H5";
    private static final String _H4_CLASS_NAME = "ncsa.hdf.hdflib.HDFLibrary";

    private static final String EPSG_DATABASE_DIR_NAME = "epsg-database";
    private static final String JAI_REGISTRY_PATH = "/META-INF/javax.media.jai.registryFile.jai";

    /**
     * Gets the current user's name, or the string <code>"unknown"</code> if the the user's name cannot be determined.
     *
     * @return the current user's name, never <code>null</code>
     */
    public static String getUserName() {
        return System.getProperty("user.name", "unknown");
    }

    /**
     * Gets the current user's home directory, or the directory pointed to by '.' if the user's actual home directory
     * cannot be determined.
     *
     * @return the current working directory, never <code>null</code>
     */
    public static File getUserHomeDir() {
        return new File(System.getProperty("user.home", "."));
    }

    /**
     * Gets the application home page URL as set by the system property "${ceres.context}.homepage.url". Default is
     * "http://sentinel.esa.int".
     *
     * @return the application homepage url
     * @since BEAM 4.10
     */
    public static String getApplicationHomepageUrl() {
        return Config.instance().preferences().get(getApplicationContextId() + ".homepage.url", "http://sentinel.esa.int");
    }

    /**
     * Gets the current user's application data directory.
     *
     * @return the current user's application data directory
     * @since BEAM 4.2
     */
    public static File getApplicationDataDir() {
        return getApplicationDataDir(false);
    }

    /**
     * Optionally creates and returns the current user's application data directory.
     *
     * @param force if true, the directory will be created if it didn't exist before
     * @return the current user's application data directory
     * @since BEAM 4.2
     */
    public static File getApplicationDataDir(boolean force) {
        File dir = Config.instance().userDir().toFile();
        if (force && !dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Gets the application context ID uses as prefix in a number of application configuration settings.
     * The context ID is configured using the system property "snap.context". If this property is not set,
     * the string "snap" is used.
     *
     * @return The application context ID.
     * @since BEAM 4.10
     */
    public static String getApplicationContextId() {
        return Config.instance().preferences().get("snap.context", "snap");
    }

    /**
     * Gets the application name used in logger output and information messages.
     * The context ID is configured using the system property
     * "${ceres.context}.application.name". If this property is not set,
     * the string "SNAP" is used.
     *
     * @return The application name.
     * @see #getApplicationContextId()
     * @since BEAM 4.10
     */
    public static String getApplicationName() {
        return Config.instance().preferences().get(getApplicationContextId() + ".application.name", "SNAP");
    }

    /**
     * Gets the current working directory, or the directory pointed to by '.' if the actual working directory cannot be
     * determined.
     *
     * @return the current working directory, never <code>null</code>
     */
    public static File getCurrentWorkingDir() {
        return new File(System.getProperty("user.dir", "."));
    }

    /**
     * Gets all files (class directory &amp; JAR file pathes) given in the current class path of the Java runtime which
     * loaded this class.
     * <p>
     * <p> The files pathes returned are either relative or absolute, just as they where defined for the runtime's class
     * path.
     *
     * @return all files in the current class path, never <code>null</code>
     */
    public static File[] getClassPathFiles() {
        String classPath = System.getProperty("java.class.path");
        if (classPath == null) {
            return new File[0];
        }
        StringTokenizer st = new StringTokenizer(classPath, File.pathSeparator);
        File[] files = new File[st.countTokens()];
        try {
            for (int i = 0; i < files.length; i++) {
                files[i] = new File(st.nextToken());
            }
        } catch (NoSuchElementException e) {
            // ignore
        }
        return files;
    }

    /**
     * Gets the application's home directory as set by the system property "${snap.context}.home".
     *
     * @return an assumption of an application's home directory, never <code>null</code>
     */
    public static File getApplicationHomeDir() {
        return Config.instance().installDir().toFile();
    }

    /**
     * Retrieves the file name of a class. For example, the string <code>"Date.class"</code> is returned for the
     * class <code>java.util.Date</code>.
     *
     * @param aClass The class.
     * @return the file name of the given class
     * @throws IllegalArgumentException if the given parameter is <code>null</code>.
     */
    public static String getClassFileName(final Class aClass) {
        Guardian.assertNotNull("aClass", aClass);
        final String qualClassName = aClass.getName();
        final int pos = qualClassName.lastIndexOf('.');
        final String className;
        if (pos > 0) {
            className = qualClassName.substring(pos + 1);
        } else {
            className = qualClassName;
        }
        return className + ".class";
    }

    /**
     * Gets the default BEAM cache directory. This is the directory
     * where BEAM stores temporary data.
     *
     * @return the default cache directory
     */
    public static File getDefaultCacheDir() {
        return new File(getApplicationDataDir(), CACHE_DIR_NAME);
    }

    /**
     * Replace the separator character '/' with the system-dependent path-separator character.
     *
     * @param urlPath an URL path or any other string containing the forward slash '/' as directory separator.
     * @return a path string with all occurrences of '/'
     * @throws IllegalArgumentException if the given parameter is <code>null</code>.
     */
    public static String convertToLocalPath(String urlPath) {
        Guardian.assertNotNull("urlPath", urlPath);
        if (File.separatorChar != _URL_DIR_SEPARATOR_CHAR
                && urlPath.indexOf(_URL_DIR_SEPARATOR_CHAR) >= 0) {
            return urlPath.replace(_URL_DIR_SEPARATOR_CHAR,
                                   File.separatorChar);
        }
        return urlPath;
    }

    /**
     * Creates a (more) human readable exception message text for the given exception. This method should be used when
     * exception messages are to be presented to the user in a GUI.
     * <p>
     * <p>Currently the only modifications are<br> 1. the first letter is turned into upper case <br> 2. the message is
     * suffixed with a dot ('.') character.
     *
     * @param e the exception
     * @return a modified message text, or <code>null</code> if <code>e</code> was null.
     */
    public static String createHumanReadableExceptionMessage(final Exception e) {
        if (e == null) {
            return null;
        }
        String message = e.getMessage();
        if (message != null && message.length() > 0) {
            final StringBuffer sb = new StringBuffer();
            sb.append(Character.toUpperCase(message.charAt(0)));
            sb.append(message.substring(1));
            String[] punctuators = new String[]{".", ",", "!", "?", ";", ":"};
            boolean punctuatorFound = false;
            for (String punctuator : punctuators) {
                if (message.endsWith(punctuator)) {
                    punctuatorFound = true;
                    break;
                }
            }
            if (!punctuatorFound) {
                sb.append('.');
            }
            message = sb.toString();
        } else {
            message = "No message text available.";
        }
        return message;
    }

    /**
     * Copies the given text to the system clipboard.
     *
     * @param text the text to copy
     */
    public static void copyToClipboard(final String text) {
        StringSelection selection = new StringSelection(text == null ? "" : text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (clipboard != null) {
            clipboard.setContents(selection, selection);
        } else {
            LOG.severe("failed to obtain clipboard instance");
        }
    }

    /**
     * Copies the given image to the system clipboard.
     *
     * @param image the image to copy
     */
    public static void copyToClipboard(final Image image) {
        ImageSelection selection = new ImageSelection(image);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (clipboard != null) {
            clipboard.setContents(selection, null);
        } else {
            LOG.severe("failed to obtain clipboard instance");
        }
    }

    public static boolean isRunningOnMacOS() {

        String osName = System.getProperty("os.name");
        if ("Mac OS X".equalsIgnoreCase(osName)) {
            return true;
        }

        final String macOsSpecificPropertyKey = "mrj.version";
        final String systemLafName = UIManager.getSystemLookAndFeelClassName();
        final String currentLafName = UIManager.getLookAndFeel().getClass().getName();

        return System.getProperty(macOsSpecificPropertyKey) != null
                && systemLafName.equals(currentLafName);
    }

    /**
     * Loads services from all <code>META-INF/services/</code> resources.
     *
     * @param serviceType the type of the service to be loaded.
     * @return the services of type <code>serviceType</code> found.
     */
    public static <S> Iterable<S> loadServices(Class<S> serviceType) {
        return ServiceLoader.load(serviceType);
    }

    /**
     * Loads services from all <code>META-INF/services/</code> resources.
     *
     * @param serviceType the type of the service to be loaded.
     * @param classLoader the class loader.
     * @return the services of type <code>serviceType</code> found.
     */
    public static <S> Iterable<S> loadServices(Class<S> serviceType, ClassLoader classLoader) {
        return ServiceLoader.load(serviceType, classLoader);
    }

    public static String getBuildNumber() {
        // todo - in BEAM 3.x org.esa.snap.resources.bundles.build resource has been used. 
        // todo - use application.properties with version ID set by Maven (resource Filter!)
        return System.getProperty(getApplicationContextId() + ".build.id", "1");
    }

    public static Level parseLogLevel(String levelName) throws IllegalArgumentException {
        if ("DEBUG".equalsIgnoreCase(levelName)) {
            return Level.FINE;
        } else if ("ERROR".equalsIgnoreCase(levelName)) {
            return Level.SEVERE;
        } else {
            return Level.parse(levelName);
        }
    }

    /**
     * @deprecated since BEAM 4.10 only used by {@code org.esa.snap.dataio.modis.ModisProductReaderPlugIn} - moved there as private method
     */
    @Deprecated
    public static Class<?> loadHdf4Lib(Class<?> callerClass) {
        return loadClassWithNativeDependencies(callerClass,
                                               _H4_CLASS_NAME,
                                               "{0}: HDF-4 library not available: {1}: {2}");
    }

    /**
     * @deprecated since BEAM 4.10 only used by {@code org.esa.snap.dataio.hdf5.HDF5ProductWriterPlugin} - moved there as private method
     */
    @Deprecated
    public static Class<?> loadHdf5Lib(Class<?> callerClass) {
        return loadClassWithNativeDependencies(callerClass,
                                               _H5_CLASS_NAME,
                                               "{0}: HDF-5 library not available: {1}: {2}");
    }

    @Deprecated
    private static Class<?> loadClassWithNativeDependencies(Class<?> callerClass, String className,
                                                            String warningPattern) {
        ClassLoader classLoader = callerClass.getClassLoader();

        String classResourceName = "/" + className.replace('.', '/') + ".class";
        SystemUtils.class.getResource(classResourceName);
        if (callerClass.getResource(classResourceName) != null) {
            try {
                return Class.forName(className, true, classLoader);
            } catch (Throwable error) {
                LOG.warning(
                        MessageFormat.format(warningPattern, callerClass, error.getClass(), error.getMessage()));
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Initialize third party libraries of BEAM.
     *
     * @param cl The most useful class loader.
     * @since BEAM 4.8
     */
    public static void init3rdPartyLibs(ClassLoader cl) {
        initJAI(cl);
        initGeoTools();
    }

    private static void initGeoTools() {
        // Must store EPSG database in BEAM home, otherwise it will be deleted from default temp location (Unix!, Windows?)
        File epsgDir = new File(SystemUtils.getApplicationDataDir(true), EPSG_DATABASE_DIR_NAME);
        System.setProperty(HsqlEpsgDatabase.DIRECTORY_KEY, epsgDir.getAbsolutePath());
    }

    private static void initJAI(ClassLoader cl) {
        // Suppress ugly (and harmless) JAI error messages saying that a JAI is going to continue in pure Java mode.
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");  // disable native libraries for JAI
        // Must use a new operation registry in order to register JAI operators defined in Ceres and BEAM
        OperationRegistry operationRegistry = OperationRegistry.getThreadSafeOperationRegistry();
        InputStream is = SystemUtils.class.getResourceAsStream(JAI_REGISTRY_PATH);
        if (is != null) {
            // Suppress ugly (and harmless) JAI error messages saying that a descriptor is already registered.
            final PrintStream oldErr = System.err;
            try {
                setSystemErr(new PrintStream(new ByteArrayOutputStream()));
                operationRegistry.updateFromStream(is);
                operationRegistry.registerServices(cl);
                JAI.getDefaultInstance().setOperationRegistry(operationRegistry);
            } catch (IOException e) {
                LOG.log(Level.SEVERE,
                        MessageFormat.format("Error loading {0}: {1}", JAI_REGISTRY_PATH,
                                             e.getMessage()), e);
            } finally {
                setSystemErr(oldErr);
            }
        } else {
            LOG.warning(MessageFormat.format("{0} not found", JAI_REGISTRY_PATH));
        }
        int parallelism = Config.instance().preferences().getInt(SNAP_PARALLELISM_PROPERTY_NAME,
                                                                 Runtime.getRuntime().availableProcessors());
        JAI.getDefaultInstance().getTileScheduler().setParallelism(parallelism);
        LOG.info(MessageFormat.format("JAI tile scheduler parallelism set to {0}", parallelism));
    }

    private static void setSystemErr(PrintStream oldErr) {
        try {
            System.setErr(oldErr);
        } catch (Exception e) {
            // ignore
        }
    }

    public static String getApplicationRemoteVersionUrl() {
        final String key = getApplicationContextId() + ".remoteVersion.url";
        String applicationHomepageUrl = getApplicationHomepageUrl();
        if (!applicationHomepageUrl.endsWith("/")) {
            applicationHomepageUrl = applicationHomepageUrl + "/";
        }
        return System.getProperty(key, applicationHomepageUrl + "software/version.txt");
    }

    /**
     * This class is used to hold an image while on the clipboard.
     */
    public static class ImageSelection implements Transferable {

        private Image _image;

        public ImageSelection(Image image) {
            _image = image;
        }

        // Returns supported flavors
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        // Returns true if flavor is supported
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        // Returns image
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException,
                IOException {
            if (!DataFlavor.imageFlavor.equals(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return _image;
        }
    }

}
