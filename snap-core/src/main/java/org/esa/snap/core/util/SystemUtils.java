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

import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.runtime.Config;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.referencing.factory.epsg.HsqlEpsgDatabase;
import org.geotools.util.logging.LoggerFactory;
import org.geotools.util.logging.Logging;

import javax.media.jai.JAI;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
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
    public static final String SNAP_CACHE_DIR_PROPERTY_NAME = getApplicationContextId() + ".cachedir";

    private static final String MANIFEST_ATTR_MODULE_NAME = "OpenIDE-Module-Name";
    private static final String MANIFEST_ATTR_MODULE_VERSION = "OpenIDE-Module-Specification-Version";


    /**
     * The SNAP system logger. Default name is "org.esa.snap" which may be overridden by system property "snap.logger.name".
     */
    public static final Logger LOG = Config.instance().logger();

    public static final String LS = System.getProperty("line.separator");

    private static final char _URL_DIR_SEPARATOR_CHAR = '/';

    /**
     * Name of SNAP's auxdata directory.
     */
    public static final String AUXDATA_DIR_NAME = "auxdata";

    private static final String EPSG_DATABASE_DIR_NAME = "epsg-database";

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
     *
     * @since BEAM 4.10
     */
    public static String getApplicationHomepageUrl() {
        return Config.instance().preferences().get(getApplicationContextId() + ".homepage.url", "http://step.esa.int");
    }

    /**
     * Gets the current user's application data directory.
     *
     * @return the current user's application data directory
     *
     * @since BEAM 4.2
     */
    public static File getApplicationDataDir() {
        return getApplicationDataDir(false);
    }

    /**
     * Gets the auxdata directory which stores dems, orbits, rgb profiles, etc.
     *
     * @return the auxiliary data directory
     *
     * @since SNAP 2.0
     */
    public static Path getAuxDataPath() {
        return getApplicationDataDir().toPath().resolve(AUXDATA_DIR_NAME);
    }

    /**
     * Gets the SNAP cache directory . This is the directory
     * where SNAP stores cached & temporary data.
     * <p>
     * The SNAP cache directory can be configured using the {@code snap.cachedir} configuration  property
     * (or Java system property).
     *
     * @return the cache directory
     *
     * @since SNAP 2
     */
    public static File getCacheDir() {
        String cacheDirPath = Config.instance().preferences().get(SNAP_CACHE_DIR_PROPERTY_NAME, null);
        if (cacheDirPath != null) {
            return new File(cacheDirPath);
        }
        return getDefaultCacheDir();
    }

    /**
     * Gets the default SNAP cache directory.
     *
     * @return the default cache directory
     *
     * @see #getCacheDir()
     */
    public static File getDefaultCacheDir() {
        return new File(new File(getApplicationDataDir(), "var"), "cache");
    }

    /**
     * Optionally creates and returns the current user's application data directory.
     *
     * @param force if true, the directory will be created if it didn't exist before
     * @return the current user's application data directory
     *
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
     *
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
     *
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
     *
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
     * Replace the separator character '/' with the system-dependent path-separator character.
     *
     * @param urlPath an URL path or any other string containing the forward slash '/' as directory separator.
     * @return a path string with all occurrences of '/'
     *
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
            final StringBuilder sb = new StringBuilder();
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

    /**
     * Initialize third party libraries of SNAP.
     *
     * @param cls The most useful class loader.
     * @since BEAM 4.8
     */
    public static void init3rdPartyLibs(Class<?> cls) {
        initJAI(cls);
        initGeoTools();
    }

    public static void initGeoTools() {
        // setting longitude first so we can, rely on the order
        GeoTools.init(new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, true));

        Logging.ALL.setLoggerFactory(new GeoToolsLoggerFactory());

        // Must store EPSG database in application data dir, otherwise it will be deleted from default temp location (Unix!, Windows?)
        File epsgDir = new File(SystemUtils.getApplicationDataDir(true), EPSG_DATABASE_DIR_NAME);
        System.setProperty(HsqlEpsgDatabase.DIRECTORY_KEY, epsgDir.getAbsolutePath());

        // disable unwanted logging messages from hsqldb
        System.setProperty("hsqldb.db.level", Level.WARNING.getName());
    }

    public static void initJAI(Class<?> cls) {
        initJAI(cls != null ? cls.getClassLoader() : Thread.currentThread().getContextClassLoader());
    }

    public static void initJAI(ClassLoader cl) {
        // Suppress ugly (and harmless) JAI error messages saying that a JAI is going to continue in pure Java mode.
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");  // disable native libraries for JAI

        final PrintStream originalErrStream = System.err;
        try (PrintStream nullErrStream = new PrintStream(new NullOutputStream())) {
            // suppress messages printed to the error stream during operator registration
            System.setErr(nullErrStream);
            JAI.getDefaultInstance().getOperationRegistry().registerServices(cl);
        } catch (Throwable t) {
            LOG.fine("Failed to register additional JAI operators: " + t.getMessage());
        } finally {
            System.setErr(originalErrStream);
        }
        int parallelism = Config.instance().preferences().getInt(SNAP_PARALLELISM_PROPERTY_NAME,
                                                                 Runtime.getRuntime().availableProcessors());
        JAI.getDefaultInstance().getTileScheduler().setParallelism(parallelism);
        LOG.fine(MessageFormat.format("JAI tile scheduler parallelism set to {0}", parallelism));

        long OneMiB = 1024L * 1024L;

        JAI.enableDefaultTileCache();
        Long size = Config.instance().preferences().getLong("snap.jai.tileCacheSize", 1024L) * OneMiB;
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(size);

        final long tileCacheSize = JAI.getDefaultInstance().getTileCache().getMemoryCapacity() / OneMiB;
        LOG.fine(MessageFormat.format("JAI tile cache size is {0} MiB", tileCacheSize));

        final int tileSize = Config.instance().preferences().getInt("snap.jai.defaultTileSize", 512);
        JAI.setDefaultTileSize(new Dimension(tileSize, tileSize));
        LOG.fine(MessageFormat.format("JAI default tile size is {0} pixels", tileSize));

        JAI.getDefaultInstance().setRenderingHint(JAI.KEY_CACHED_TILE_RECYCLING_ENABLED, Boolean.TRUE);
        LOG.fine("JAI tile recycling enabled");
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
     * Tries to load the metadata from {@code META-INF/MANIFEST.MF} contained in the module jar
     * of the specified class.
     *
     * @param aClass The module jar which contains this specified class will be used to look-up the
     *               {@code META-INF/MANIFEST.MF}
     * @return the module metadata, or {@code null} if a {@code META-INF/MANIFEST.MF} could not be found.
     */
    public static ModuleMetadata loadModuleMetadata(Class<?> aClass) {
        try {
            URL moduleLocation = aClass.getProtectionDomain().getCodeSource().getLocation();
            final Path pathFromURI = FileUtils.getPathFromURI(FileUtils.ensureJarURI(moduleLocation.toURI()));
            final Path manifestPath = pathFromURI.resolve("META-INF/MANIFEST.MF");
            try (InputStream inputStream = Files.newInputStream(manifestPath)) {
                Manifest manifest = new Manifest(inputStream);
                return new ManifestModuleMetadata(manifest);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the version string. It'S the version uf the last major release.
     *
     * @return the version string
     */
    public static String getReleaseVersion() {
        String version = null;
        Path versionFile = getApplicationHomeDir().toPath().resolve("VERSION.txt");
        if (Files.exists(versionFile)) {
            try {
                List<String> versionInfo = Files.readAllLines(versionFile);
                if (!versionInfo.isEmpty()) {
                    version = versionInfo.get(0);
                }
            } catch (IOException e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
        }
        if (version != null) {
            return version;
        }
        return "[no version info, missing ${SNAP_HOME}/VERSION.txt]";
    }

    /**
     * Empty all tiles from cache and garbage collect
     */
    public static void freeAllMemory() {
        JAI.getDefaultInstance().getTileCache().flush();
        JAI.getDefaultInstance().getTileCache().memoryControl();
        System.gc();
        System.gc();
        System.gc();
    }

    /**
     * tell tileCache that some old tiles can be removed
     */
    public static void tileCacheFreeOldTiles() {
        JAI.getDefaultInstance().getTileCache().memoryControl();
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

    private static class NullOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {

        }
    }

    private static class ManifestModuleMetadata implements ModuleMetadata {

        private final Manifest manifest;

        public ManifestModuleMetadata(Manifest manifest) {
            this.manifest = manifest;
        }

        @Override
        public String getDisplayName() {
            return getAttributeValue(manifest, MANIFEST_ATTR_MODULE_NAME);
        }

        @Override
        public String getSymbolicName() {
            return getDisplayName() + "_" + getVersion();
        }

        @Override
        public String getVersion() {
            return getAttributeValue(manifest, MANIFEST_ATTR_MODULE_VERSION);
        }

        private String getAttributeValue(Manifest manifest, String attrName) {
            final Attributes mainAttributes = manifest.getMainAttributes();
            final String attrValue = mainAttributes.getValue(attrName);
            if (attrValue == null) {
                return "unknown";
            }
            return attrValue;
        }

    }

    // reduces the amount of log messages
    private static class GeoToolsLoggerFactory extends LoggerFactory<Logger> {

        public GeoToolsLoggerFactory() {
            super(Logger.class);
        }

        @Override
        protected Logger getImplementation(String name) {
            Logger logger = Logger.getLogger(name);
            // only severe messages shall be logged
            logger.setLevel(Level.SEVERE);
            return logger;
        }

        @Override
        protected Logger wrap(String name, Logger implementation) {
            return implementation;
        }

        @Override
        protected Logger unwrap(Logger logger) {
            return logger;
        }
    }
}
