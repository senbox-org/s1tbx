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

import com.bc.ceres.core.runtime.RuntimeContext;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.referencing.factory.epsg.HsqlEpsgDatabase;

import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.StringTokenizer;
import java.util.logging.Level;

/**
 * A collection of (BEAM-) system level functions.
 * <p/>
 * <p> All functions have been implemented with extreme caution in order to provide a maximum performance.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 */
public class SystemUtils {

    /**
     * The URL string to the BEAM home page.
     *
     * @deprecated since 4.10, use {@link #getApplicationHomepageUrl()} instead
     */
    @Deprecated
    public static final String BEAM_HOME_PAGE = getApplicationHomepageUrl();
    // public static final String BEAM_HOME_PAGE = "http://envisat.esa.int/beam/";

    /**
     * The name of the system property that specifies the application home (= installation) directory.
     *
     * @deprecated since 4.10, use {@link #getApplicationHomePropertyName()} instead
     */
    @Deprecated
    public static final String BEAM_HOME_PROPERTY_NAME = getApplicationHomePropertyName();
    // public static final String BEAM_HOME_PROPERTY_NAME = "beam.home";

    /**
     * @deprecated since 4.10, not in use.
     */
    @Deprecated
    public static final String BEAM_PLUGIN_PATH_PROPERTY_NAME = "beam.plugin.path";

    public static final String BEAM_PARALLELISM_PROPERTY_NAME = "beam.parallelism";

    public static final String LAX_INSTALL_DIR_PROPERTY_NAME = "lax.root.install.dir";

    /**
     * SYSTEM_DEPENDENT_LINE_SEPARATOR
     */
    public static final String LS = System.getProperty("line.separator");

    private static final char _URL_DIR_SEPARATOR_CHAR = '/';
    public static final int LL_DEBUG = 10;
    public static final int LL_INFO = 20;
    public static final int LL_WARNING = 30;
    public static final int LL_ERROR = 40;
    public static final String LLS_DEBUG = "DEBUG";
    public static final String LLS_INFO = "INFO";
    public static final String LLS_WARNING = "WARNING";
    public static final String LLS_ERROR = "ERROR";

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
    private static final String FILE_PROTOCOL_PREFIX = "file:";
    private static final String JAR_PROTOCOL_PREFIX = "jar:";

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
     * "http://www.brockmann-consult.de/beam/".
     *
     * @return the current user's application data directory
     * @since BEAM 4.10
     */
    public static String getApplicationHomepageUrl() {
        return System.getProperty(getApplicationContextId() + ".homepage.url", "http://www.brockmann-consult.de/beam/");
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
        String contextId = getApplicationContextId();
        final File dir = new File(getUserHomeDir(), "." + contextId);
        if (force && !dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Gets the application context ID uses as prefix in a number of application configuration settings.
     * The context ID is configured using the system property "ceres.context". If this property is not set,
     * the string "beam" is used.
     *
     * @return The application context ID.
     * @since BEAM 4.10
     */
    public static String getApplicationContextId() {
        String contextId = null;
        if (RuntimeContext.getModuleContext() != null) {
            contextId = RuntimeContext.getModuleContext().getRuntimeConfig().getContextId();
        }
        if (contextId == null) {
            contextId = System.getProperty("ceres.context", "beam");
        }
        return contextId;
    }

    /**
     * Gets the application name used in logger output and information messages.
     * The context ID is configured using the system property
     * "${ceres.context}.application.name". If this property is not set,
     * the string "BEAM" is used.
     *
     * @return The application name.
     * @see #getApplicationContextId()
     * @since BEAM 4.10
     */
    public static String getApplicationName() {
        return System.getProperty(getApplicationContextId() + ".application.name", "BEAM");
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
     * Gets all files (class directory & JAR file pathes) given in the current class path of the Java runtime which
     * loaded this class.
     * <p/>
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
     * Gets the application's home directory as set by the system property "${ceres.context}.home".
     * If not set, the method determines the home directory by retrieving the URL of this
     * class using the method {@link #getApplicationHomeDir(java.net.URL)}.
     *
     * @return an assumption of an application's home directory, never <code>null</code>
     */
    public static File getApplicationHomeDir() {
        final String homeKey = getApplicationHomePropertyName();
        final String homeValue = System.getProperty(homeKey);
        if (homeValue != null) {
            return new File(homeValue);
        }
        // Use fallback
        final URL url = SystemUtils.class.getResource(getClassFileName(SystemUtils.class));
        return getApplicationHomeDir(url);
    }

    public static String getApplicationHomePropertyName() {
        return getApplicationContextId() + ".home";
    }

    /**
     * Extracts an application's home directory from the given URL.
     * <p/>
     * The URL is than scanned for the last occurence of the string <code>&quot;/modules/&quot;</code>.
     * If this succeeds the method returns the absolute
     * (parent) path to the directory which contains <code>modules</code>, which is
     * then assumed to be the requested home directory.
     *
     * @param url the URL
     * @return an assumption of an application's home directory, never <code>null</code>
     * @throws IllegalArgumentException if the given url is <code>null</code>.
     */
    public static File getApplicationHomeDir(final URL url) {
        Guardian.assertNotNull("url", url);
        String path = url.getPath();
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // ignored
        }
        path = stripUrlProtocolPrefixes(path);
        path = path.replace(File.separatorChar, '/');
        path = stripClassLibraryPaths(path);
        path = path.replace('/', File.separatorChar);
        return new File(path);
    }

    private static String stripClassLibraryPaths(String path) {
        int pos = path.lastIndexOf("/modules/");
        if (pos >= 0) {
            path = path.substring(0, pos);
        }
        return path;
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


    private static String stripUrlProtocolPrefixes(String path) {
        while (true) {
            if (path.startsWith(FILE_PROTOCOL_PREFIX)) {
                path = path.substring(FILE_PROTOCOL_PREFIX.length());
            } else if (path.startsWith(JAR_PROTOCOL_PREFIX)) {
                path = path.substring(JAR_PROTOCOL_PREFIX.length());
            } else {
                break;
            }
        }
        return path;
    }

    /**
     * Gets the BEAM Java home directory. The method evaluates the system property <code>org.esa.beam.home</code>. If it
     * is given, it is returned, otherwise <code>getApplicationHomeDir()</code> is returned.
     *
     * @return the BEAM home directory
     * @deprecated since BEAM 4.10, use {@link #getApplicationHomeDir()} instead
     */
    @Deprecated
    public static File getBeamHomeDir() {

        String homeKey = getApplicationHomePropertyName();

        String homeDir = System.getProperty(homeKey);
        if (homeDir != null && homeDir.length() > 0) {
            return new File(homeDir);
        }
        homeDir = System.getProperty(LAX_INSTALL_DIR_PROPERTY_NAME);
        if (homeDir != null && homeDir.length() > 0) {
            return new File(homeDir);
        }

        final URL url = SystemUtils.class.getResource(getClassFileName(SystemUtils.class));
        String path = url.getPath();
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // ignored
        }
        path = path.replace(File.separatorChar, '/');
        String beam4Key = "/beam4/";
        int beam4Index = path.indexOf(beam4Key);
        if (beam4Index != -1) {
            path = path.substring(0, beam4Index + beam4Key.length() - 1);
            path = path.replace('/', File.separatorChar);
            return new File(path);
        } else {
            return new File(".").getAbsoluteFile();
        }
    }


    /**
     * Gets the default BEAM cache directory. This is the directory
     * where BEAM stores temporary data.
     *
     * @return the default cache directory
     */
    public static File getDefaultBeamCacheDir() {
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
     * @deprecated Since BEAM 4.9, use {@link FileUtils#deleteTree(File)} instead
     */
    @Deprecated
    public static void deleteFileTree(File treeRoot) {
        FileUtils.deleteTree(treeRoot);
    }

    /**
     * Creates a (more) human readable exception message text for the given exception. This method should be used when
     * exception messages are to be presented to the user in a GUI.
     * <p/>
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
            BeamLogManager.getSystemLogger().severe("failed to obtain clipboard instance");
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
            BeamLogManager.getSystemLogger().severe("failed to obtain clipboard instance");
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
        // todo - in BEAM 3.x org.esa.beam.resources.bundles.build resource has been used. 
        // todo - use application.properties with version ID set by Maven (resource Filter!)
        return System.getProperty(getApplicationContextId() + ".build.id", "1");
    }

    public static int getLogLevel(String logLevelStr) {
        int logLevel = LL_INFO;
        if (LLS_DEBUG.equalsIgnoreCase(logLevelStr)) {
            logLevel = LL_DEBUG;
        } else if (LLS_INFO.equalsIgnoreCase(logLevelStr)) {
            logLevel = LL_INFO;
        } else if (LLS_ERROR.equalsIgnoreCase(logLevelStr)) {
            logLevel = LL_ERROR;
        } else if (LLS_WARNING.equalsIgnoreCase(logLevelStr)) {
            logLevel = LL_WARNING;
        }
        return logLevel;
    }

    /**
     * @deprecated since BEAM 4.10 only used by {@link org.esa.beam.dataio.modis.ModisProductReaderPlugIn} - moved there as private method
     */
    @Deprecated
    public static Class<?> loadHdf4Lib(Class<?> callerClass) {
        return loadClassWithNativeDependencies(callerClass,
                                               _H4_CLASS_NAME,
                                               "{0}: HDF-4 library not available: {1}: {2}");
    }

    /**
     * @deprecated since BEAM 4.10 only used by {@link org.esa.beam.dataio.hdf5.HDF5ProductWriterPlugin} - moved there as private method
     */
    @Deprecated
    public static Class<?> loadHdf5Lib(Class<?> callerClass) {
        return loadClassWithNativeDependencies(callerClass,
                                               _H5_CLASS_NAME,
                                               "{0}: HDF-5 library not available: {1}: {2}");
    }

    @Deprecated
    private static Class<?> loadClassWithNativeDependencies(Class<?> callerClass, String className, String warningPattern) {
        ClassLoader classLoader = callerClass.getClassLoader();

        String classResourceName = "/" + className.replace('.', '/') + ".class";
        SystemUtils.class.getResource(classResourceName);
        if (callerClass.getResource(classResourceName) != null) {
            try {
                return Class.forName(className, true, classLoader);
            } catch (Throwable error) {
                BeamLogManager.getSystemLogger().warning(MessageFormat.format(warningPattern, callerClass, error.getClass(), error.getMessage()));
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
        // Must use a new operation registry in order to register JAI operators defined in Ceres and BEAM
        OperationRegistry operationRegistry = OperationRegistry.getThreadSafeOperationRegistry();
        InputStream is = SystemUtils.class.getResourceAsStream(JAI_REGISTRY_PATH);
        if (is != null) {
            try {
                operationRegistry.updateFromStream(is);
                operationRegistry.registerServices(cl);
                JAI.getDefaultInstance().setOperationRegistry(operationRegistry);
            } catch (IOException e) {
                BeamLogManager.getSystemLogger().log(Level.SEVERE, MessageFormat.format("Error loading {0}: {1}", JAI_REGISTRY_PATH, e.getMessage()), e);
            }
        } else {
            BeamLogManager.getSystemLogger().warning(MessageFormat.format("{0} not found", JAI_REGISTRY_PATH));
        }
        Integer parallelism = Integer.getInteger(BEAM_PARALLELISM_PROPERTY_NAME, Runtime.getRuntime().availableProcessors());
        JAI.getDefaultInstance().getTileScheduler().setParallelism(parallelism);
        BeamLogManager.getSystemLogger().info(MessageFormat.format("JAI tile scheduler parallelism set to {0}", parallelism));
    }

    public static String getApplicationRemoteVersionUrl() {
        final String key = getApplicationContextId() + ".remoteVersion.url";
        return System.getProperty(key, getApplicationHomepageUrl() + "software/version.txt");
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
