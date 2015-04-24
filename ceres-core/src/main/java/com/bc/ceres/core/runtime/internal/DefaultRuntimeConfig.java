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

import com.bc.ceres.core.runtime.RuntimeConfig;
import com.bc.ceres.core.runtime.RuntimeConfigException;
import com.bc.ceres.util.TemplateReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.security.CodeSource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class DefaultRuntimeConfig implements RuntimeConfig {

    public static final String CONFIG_KEY_CERES_CONTEXT = "ceres.context";
    public static final String CONFIG_KEY_DEBUG = "debug";
    public static final String CONFIG_KEY_MAIN_CLASS = "mainClass";
    public static final String CONFIG_KEY_CLASSPATH = "classpath";
    public static final String CONFIG_KEY_HOME = "home";
    public static final String CONFIG_KEY_CONFIG_FILE_NAME = "config";
    public static final String CONFIG_KEY_MODULES = "modules";
    public static final String CONFIG_KEY_LIB_DIRS = "libDirs";
    public static final String CONFIG_KEY_APP = "app";
    public static final String CONFIG_KEY_CONSOLE_LOG = "consoleLog";
    public static final String CONFIG_KEY_LOG_LEVEL = "logLevel";

    public static final String DEFAULT_CERES_CONTEXT = "ceres";
    public static final String DEFAULT_MAIN_CLASS_NAME = "com.bc.ceres.core.runtime.RuntimeLauncher";
    public static final String DEFAULT_MODULES_DIR_NAME = "modules";
    public static final String DEFAULT_CONFIG_DIR_NAME = "config";
    public static final String DEFAULT_LIB_DIR_NAME = "lib";

    public static final SimpleDateFormat LOG_TIME_STAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private final Properties properties;

    private String contextId;

    private String debugKey;
    private boolean debug;

    private String homeDirKey;
    private String homeDirPath;

    private String mainClassKey;
    private String mainClassName;

    private String classpathKey;
    private String mainClassPath;

    private String applicationIdKey;
    private String applicationId;

    private String configFileKey;
    private String configFilePath;
    private String defaultRelConfigFilePath;
    private String defaultHomeConfigFilePath;

    private String modulesDirKey;
    private String modulesDirPath;
    private String defaultHomeModulesDirPath;

    private String libDirsKey;
    private String[] libDirPaths;
    private String defaultHomeLibDirPath;

    private Logger logger;
    private boolean homeDirAssumed;

    private String consoleLogKey;
    private String logLevelKey;
    private Level logLevel;
    private boolean consoleLog;

    public DefaultRuntimeConfig() throws RuntimeConfigException {
        properties = System.getProperties();
        initAll();
    }

    public String getContextId() {
        return contextId;
    }

    public String getContextProperty(String key) {
        return getContextProperty(key, null);
    }

    public String getContextProperty(String key, String defaultValue) {
        return getProperty(contextId + '.' + key, defaultValue);
    }

    public boolean isDebug() {
        return debug;
    }

    public String getMainClassName() {
        return mainClassName;
    }

    @Override
    public String getMainClassPath() {
        return mainClassPath;
    }

    public String getHomeDirPath() {
        return homeDirPath;
    }

    public String getConfigFilePath() {
        return configFilePath;
    }

    public String[] getLibDirPaths() {
        return libDirPaths;
    }

    public String getModulesDirPath() {
        return modulesDirPath;
    }

    public boolean isUsingModuleRuntime() {
        return DEFAULT_MAIN_CLASS_NAME.equals(mainClassName);
    }

    public String getApplicationId() {
        return applicationId;
    }

    public Logger getLogger() {
        return logger;
    }

    /////////////////////////////////////////////////////////////////////////
    // Private
    /////////////////////////////////////////////////////////////////////////

    private void initAll() throws RuntimeConfigException {
        initContext();
        initDebug();
        initHomeDirAndConfiguration();
        initDebug(); // yes, again
        initMainClassName();
        initClasspathPaths();
        initModulesDir();
        initLibDirs();
        if (isUsingModuleRuntime()) {
            initAppId();
        }
        initLogLevel();
        initConsoleLog();
        if (System.getProperty("platform") != null && System.getProperty("platform").equals("CEMS")) {
            // special case: processing on CEMS requires specific logging due to NFS concurrency issues
            initCemsLogger();
        } else {
            initDefaultLogger();
        }
        setAutoDetectProperties();
    }

    private void initContext() {

        // Initialize context identifier. Mandatory.
        contextId = System.getProperty(CONFIG_KEY_CERES_CONTEXT, DEFAULT_CERES_CONTEXT);

        // Initialize application specific configuration keys
        homeDirKey = String.format("%s.%s", contextId, CONFIG_KEY_HOME);
        debugKey = String.format("%s.%s", contextId, CONFIG_KEY_DEBUG);
        configFileKey = String.format("%s.%s", contextId, CONFIG_KEY_CONFIG_FILE_NAME);
        modulesDirKey = String.format("%s.%s", contextId, CONFIG_KEY_MODULES);
        libDirsKey = String.format("%s.%s", contextId, CONFIG_KEY_LIB_DIRS);
        mainClassKey = String.format("%s.%s", contextId, CONFIG_KEY_MAIN_CLASS);
        classpathKey = String.format("%s.%s", contextId, CONFIG_KEY_CLASSPATH);
        applicationIdKey = String.format("%s.%s", contextId, CONFIG_KEY_APP);
        logLevelKey = String.format("%s.%s", contextId, CONFIG_KEY_LOG_LEVEL);
        consoleLogKey = String.format("%s.%s", contextId, CONFIG_KEY_CONSOLE_LOG);

        // Initialize default file and directory paths
        char sep = File.separatorChar;
        defaultRelConfigFilePath = String.format("%s/%s", DEFAULT_CONFIG_DIR_NAME, configFileKey).replace('/', sep);
        defaultHomeConfigFilePath = String.format("${%s}/%s", homeDirKey, defaultRelConfigFilePath).replace('/', sep);
        defaultHomeModulesDirPath = String.format("${%s}/%s", homeDirKey, DEFAULT_MODULES_DIR_NAME).replace('/', sep);
        defaultHomeLibDirPath = String.format("${%s}/%s", homeDirKey, DEFAULT_LIB_DIR_NAME).replace('/', sep);
    }

    private void initDebug() {
        debug = Boolean.valueOf(System.getProperty(debugKey, Boolean.toString(debug)));
    }

    private void initHomeDirAndConfiguration() throws RuntimeConfigException {
        maybeInitHomeDirAndConfigFile();
        if (configFilePath != null) {
            loadConfiguration();
        } else {
            trace("A configuration file is not used.");
        }
        initHomeDirIfNotAlreadyDone();
    }

    private void maybeInitHomeDirAndConfigFile() throws RuntimeConfigException {
        maybeInitHomeDir();
        maybeInitConfigFile();
        if (homeDirPath == null && configFilePath == null) {
            // we have no home and no config file
            assumeHomeDir();
        }
        if (configFilePath == null) {
            maybeInitDefaultConfigFile();
        }
    }

    private void maybeInitHomeDir() throws RuntimeConfigException {
        String homeDirPath = getProperty(homeDirKey);
        if (!isNullOrEmptyString(homeDirPath)) {
            // ok set: must be an existing directory
            File homeDir = new File(homeDirPath);
            if (!homeDir.isDirectory()) {
                throw createInvalidPropertyValueException(homeDirKey, homeDirPath);
            }
            try {
                this.homeDirPath = homeDir.getCanonicalPath();
            } catch (IOException e) {
                throw new RuntimeConfigException(e.getMessage(), e);
            }
        }
    }

    private void maybeInitConfigFile() throws RuntimeConfigException {
        String configFilePath = getProperty(configFileKey);
        if (!isNullOrEmptyString(configFilePath)) {
            File configFile = new File(configFilePath);
            // ok set: must be an existing file
            if (!configFile.isFile()) {
                throw createInvalidPropertyValueException(configFileKey, configFile.getPath());
            }
            try {
                this.configFilePath = configFile.getCanonicalPath();
            } catch (IOException e) {
                throw new RuntimeConfigException(e.getMessage(), e);
            }
        }
    }

    private void maybeInitDefaultConfigFile() {
        File configFile = new File(substitute(defaultHomeConfigFilePath));
        if (configFile.isFile()) {
            configFilePath = configFile.getPath();
        }
    }

    private void assumeHomeDir() {
        List<File> possibleHomeDirList = createPossibleHomeDirList();
        List<String> homeContentPathList = createHomeContentPathList();
        // The existance of the files in homeContentPathList is optional.
        // We assume that the home directory is the one in which most files of homeContentPathList are present.
        trace("Auto-detecting home directory...");
        File mostLikelyHomeDir = findMostLikelyHomeDir(possibleHomeDirList, homeContentPathList);
        if (mostLikelyHomeDir != null) {
            homeDirPath = mostLikelyHomeDir.getPath();
        } else {
            homeDirPath = new File(".").getAbsolutePath();
        }
        homeDirAssumed = true;
        setProperty(homeDirKey, homeDirPath);
    }

    private File findMostLikelyHomeDir(List<File> possibleHomeDirList, List<String> homeContentPathList) {
        int numFoundMax = 0;
        File mostLikelyHomeDir = null;
        for (File possibleHomeDir : possibleHomeDirList) {
            trace(String.format("Is [%s] my home directory?", possibleHomeDir));

            int numFound = 0;
            for (String homeContentPath : homeContentPathList) {
                File homeContentFile = new File(possibleHomeDir, homeContentPath);
                if (homeContentFile.exists()) {
                    trace(String.format("  [%s] contained? Yes.", homeContentPath));
                    numFound++;
                } else {
                    trace(String.format("  [%s] contained? No.", homeContentPath));
                }
            }
            if (numFound == 0) {
                trace("No.");
            } else {
                trace(String.format("Maybe. %d related file(s) found.", numFound));
            }
            if (numFound > numFoundMax) {
                try {
                    mostLikelyHomeDir = possibleHomeDir.getCanonicalFile();
                    numFoundMax = numFound;
                } catch (IOException e) {
                    // ???
                }
            }
        }
        return mostLikelyHomeDir;
    }

    private static List<File> createPossibleHomeDirList() {
        List<File> homeDirCheckList = new ArrayList<>(4);

        // include codeSource dir in check list
        CodeSource lib = DefaultRuntimeConfig.class.getProtectionDomain().getCodeSource();
        if (lib != null) {
            URL libUrl = lib.getLocation();
            if (libUrl.getProtocol().equals("file")) {
                String libPath = libUrl.getPath();
                File libParentDir = new File(libPath).getParentFile();
                if (libParentDir != null) {
                    // include one above libParentDir
                    if (libParentDir.getParentFile() != null) {
                        homeDirCheckList.add(libParentDir.getParentFile());
                    }
                    // include libParentDir
                    homeDirCheckList.add(libParentDir);
                }
            }
        }
        // include CWD in check list
        homeDirCheckList.add(new File(".").getAbsoluteFile());
        // include one above CWD in check list
        homeDirCheckList.add(new File("..").getAbsoluteFile());
        return homeDirCheckList;
    }

    private List<String> createHomeContentPathList() {
        List<String> homeContentPathList = new ArrayList<>(8);
        homeContentPathList.add(defaultRelConfigFilePath);
        homeContentPathList.add("bin");
        homeContentPathList.add(DEFAULT_LIB_DIR_NAME);
        homeContentPathList.add(DEFAULT_MODULES_DIR_NAME);
        return homeContentPathList;
    }

    private void loadConfiguration() throws RuntimeConfigException {
        trace(String.format("Loading configuration from [%s]", configFilePath));
        try {
            try (InputStream stream = new FileInputStream(configFilePath)) {
                Properties fileProperties = new Properties();
                fileProperties.load(stream);
                // @todo check tests - code was not backward compatible with Java 5
                // so i changed it - but this is not the only place of uncompatibilty
                // add default properties so that they override file properties
                //Set<String> propertyNames = fileProperties.stringPropertyNames();
//                for (String propertyName : propertyNames) {
//                    String propertyValue = fileProperties.getProperty(propertyName);
//                    if (!isPropertySet(propertyName)) {
//                        setProperty(propertyName, propertyValue);
//                        trace(String.format("Configuration property [%s] added", propertyName));
//                    } else {
//                        trace(String.format("Configuration property [%s] ignored", propertyName));
//                    }
//                }

                Enumeration<?> enumeration = fileProperties.propertyNames();
                while (enumeration.hasMoreElements()) {
                    final Object key = enumeration.nextElement();
                    if (key instanceof String) {
                        final Object value = fileProperties.get(key);
                        if (value instanceof String) {
                            final String keyString = (String) key;
                            String propertyValue = fileProperties.getProperty(keyString);
                            if (!isPropertySet(keyString)) {
                                setProperty(keyString, propertyValue);
                                trace(String.format("Configuration property [%s] added", keyString));
                            } else {
                                trace(String.format("Configuration property [%s] ignored", keyString));
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeConfigException(String.format("Failed to load configuration [%s]", configFilePath),
                                             e);
        }
    }

    private void initHomeDirIfNotAlreadyDone() throws RuntimeConfigException {
        if (homeDirPath == null || homeDirAssumed) {
            maybeInitHomeDir();
        }
        if (homeDirPath == null) {
            homeDirPath = new File(".").getAbsolutePath();
            homeDirAssumed = true;
        }
        // remove redundant '.'s and '..'s.
        try {
            homeDirPath = new File(homeDirPath).getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeConfigException("Home directory is invalid.", e);
        }
        if (homeDirAssumed) {
            trace(String.format("Home directory not set. Using assumed default."));
        }
        trace(String.format("Home directory is [%s]", homeDirPath));
    }

    private void initMainClassName() throws RuntimeConfigException {
        mainClassName = getProperty(mainClassKey, DEFAULT_MAIN_CLASS_NAME);
        if (isNullOrEmptyString(mainClassName)) {
            throw createMissingPropertyKeyException(mainClassKey);
        }
    }

    private void initClasspathPaths() throws RuntimeConfigException {
        mainClassPath = getProperty(classpathKey, null);
    }

    private void initModulesDir() throws RuntimeConfigException {
        this.modulesDirPath = null;
        String modulesDirPath = getProperty(modulesDirKey);
        try {
            if (modulesDirPath != null) {
                File modulesDir = new File(modulesDirPath);
                if (!modulesDir.isDirectory()) {
                    throw createInvalidPropertyValueException(modulesDirKey, modulesDirPath);
                }
                this.modulesDirPath = modulesDir.getCanonicalPath();
            } else {
                // try default
                File modulesDir = new File(substitute(defaultHomeModulesDirPath));
                if (modulesDir.isDirectory()) {
                    this.modulesDirPath = modulesDir.getCanonicalPath();
                }
            }
        } catch (IOException e) {
            String msg = String.format("Could not convert modules dir path [%s] into its canonical form.", modulesDirPath);
            throw new RuntimeConfigException(msg, e);
        }
    }

    private void initLibDirs() throws RuntimeConfigException {
        this.libDirPaths = new String[0];
        String libDirPathsString = getProperty(libDirsKey);
        if (libDirPathsString != null) {
            String[] libDirPaths = splitLibDirPaths(libDirPathsString);
            for (String libDirPath : libDirPaths) {
                File libDir = new File(libDirPath);
                if (!libDir.isDirectory()) {
                    throw createInvalidPropertyValueException(libDirsKey, libDirPathsString);
                }
            }
            this.libDirPaths = libDirPaths;
        } else {
            // try default
            libDirPathsString = substitute(defaultHomeLibDirPath);
            File libDir = new File(libDirPathsString);
            if (libDir.isDirectory()) {
                this.libDirPaths = new String[]{libDirPathsString};
            }
        }
    }

    private void initAppId() throws RuntimeConfigException {
        applicationId = getProperty(applicationIdKey);
        if (applicationId != null && applicationId.length() == 0) {
            throw createMissingPropertyKeyException(applicationIdKey);
        }
    }

    private void initLogLevel() {
        String logLevelStr = getProperty(logLevelKey, Level.OFF.getName());
        Level[] validLevels = new Level[]{
                Level.SEVERE,
                Level.WARNING,
                Level.INFO,
                Level.CONFIG,
                Level.FINE,
                Level.FINER,
                Level.FINEST,
                Level.ALL,
                Level.OFF,
        };

        logLevel = Level.OFF;
        for (Level level : validLevels) {
            if (level.getName().equalsIgnoreCase(logLevelStr)) {
                logLevel = level;
                break;
            }
        }
    }

    private void initConsoleLog() {
        String consoleLogStr = getProperty(consoleLogKey, "false");
        consoleLog = Boolean.parseBoolean(consoleLogStr);
    }

    private void initDefaultLogger() {
        ConsoleHandler consoleHandler = null;

        Logger rootLogger = LogManager.getLogManager().getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            if (handler instanceof ConsoleHandler) {
                consoleHandler = (ConsoleHandler) handler;
                rootLogger.removeHandler(handler);
            }
        }

        logger = Logger.getLogger(contextId);
        logger.setLevel(logLevel);
        if (!logLevel.equals(Level.OFF)) {
            // if any of the log handler has our LogFormatter we have already configured the logger
            for (Handler handler : logger.getHandlers()) {
                if (handler.getFormatter() instanceof LogFormatter) {
                    return;
                }
            }
            LogFormatter formatter = new LogFormatter();
            if (consoleLog) {
                if (consoleHandler == null) {
                    consoleHandler = new ConsoleHandler();
                }
                consoleHandler.setFormatter(formatter);
                consoleHandler.setLevel(logLevel);
                logger.addHandler(consoleHandler);
            }
            String userHomePath = getProperty("user.home", ".");
            File logDir = new File(userHomePath, '.' + contextId + "/log");
            logDir.mkdirs();
            String logFilePattern = new File(logDir, contextId + "-%g.log").getPath();
            try {
                FileHandler fileHandler = new FileHandler(logFilePattern);
                fileHandler.setFormatter(formatter);
                fileHandler.setLevel(logLevel);
                logger.addHandler(fileHandler);
            } catch (IOException e) {
                System.err.println("Error: Failed to create log file: " + logFilePattern);
            }
        }
    }

    private void initCemsLogger() {
        // workaround for logging of CEMS LSB processes, following approach from SST project
        // This is just one special case! todo: find a more general solution!
        if (logger == null) {
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            final Formatter formatter = new Formatter() {
                @Override
                public String format(LogRecord record) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(dateFormat.format(new Date(record.getMillis())));
                    sb.append(" - ");
                    sb.append(record.getLevel().getName());
                    sb.append(": ");
                    sb.append(record.getMessage());
                    sb.append("\n");
                    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                    final Throwable thrown = record.getThrown();
                    if (thrown != null) {
                        sb.append(thrown.toString());
                        sb.append("\n");
                    }
                    return sb.toString();
                }
            };

            final ConsoleHandler handler = new ConsoleHandler();
            handler.setFormatter(formatter);
            handler.setLevel(Level.ALL);

            logger = Logger.getLogger("ga.cems");
            final Handler[] handlers = logger.getHandlers();
            for (Handler h : handlers) {
                logger.removeHandler(h);
            }

            logger.setUseParentHandlers(false);
            logger.addHandler(handler);
        }
//        logger.setLevel(Level.INFO);
        logger.setLevel(logLevel);
    }

    private void setAutoDetectProperties() {
        setPropertyIfNotSet(this.homeDirKey, getHomeDirPath());
        setPropertyIfNotSet(configFileKey, getConfigFilePath());
        setPropertyIfNotSet(modulesDirKey, getModulesDirPath());
        String libDirPaths = assembleLibDirPaths(getLibDirPaths());
        setPropertyIfNotSet(libDirsKey, libDirPaths.length() > 0 ? libDirPaths : null);
    }

    private void setPropertyIfNotSet(String key, String value) {
        if (!isPropertySet(key)) {
            setProperty(key, value);
        }
    }

    private static boolean isNullOrEmptyString(String value) {
        return value == null || value.length() == 0;
    }

    private static String[] splitLibDirPaths(String libDirPathsString) {
        List<String> libDirPathList = new ArrayList<>(8);
        StringTokenizer stringTokenizer = new StringTokenizer(libDirPathsString, File.pathSeparator);
        while (stringTokenizer.hasMoreElements()) {
            String libDirPath = (String) stringTokenizer.nextElement();
            libDirPathList.add(libDirPath);
        }
        return libDirPathList.toArray(new String[libDirPathList.size()]);
    }

    private static String assembleLibDirPaths(String[] libDirPaths) {
        StringBuilder sb = new StringBuilder(64);
        for (String libDirPath : libDirPaths) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparator);
            }
            sb.append(libDirPath);
        }
        return sb.toString();
    }

    private static RuntimeConfigException createMissingPropertyKeyException(String key) {
        return new RuntimeConfigException(
                String.format("Property '%s' has not been set.", key));
    }

    private static RuntimeConfigException createInvalidPropertyValueException(String key, String value) {
        return new RuntimeConfigException(String.format("Value of property '%s' is invalid: %s", key, value));
    }

    private void trace(String msg) {
        if (debug) {
            System.out.println(String.format("[DEBUG] ceres-config: %s", msg));
        }
    }

    private boolean isPropertySet(String key) {
        return properties.containsKey(key);
    }

    private String getProperty(String key) {
        return getProperty(key, null);
    }

    private String getProperty(String key, String defaultValue) {
        String property = properties.getProperty(key, defaultValue);
        if (property != null) {
            return substitute(property);
        }
        return property;
    }

    private void setProperty(String key, String value) {
        if (value != null) {
            properties.setProperty(key, value);
        } else {
            properties.remove(key);
        }
    }

    /**
     * Substitues all occurences of <code>${<i>configKey</i>}</code> in the given string
     * with the value of <code><i>configKey</i></code>.
     *
     * @param value the string in which to perform the substitution.
     * @return the resulting string.
     */
    private String substitute(String value) {
        if (value.indexOf('$') == -1) {
            return value;
        }
        StringReader r = new StringReader(value);
        try {
            return new TemplateReader(r, properties).readAll();
        } catch (IOException e) {
            return value;
        }
    }

    private static class LogFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.append('[');
            pw.append(record.getLevel().toString());
            pw.append(']');
            pw.append(' ');
            pw.append(LOG_TIME_STAMP_FORMAT.format(new Date(record.getMillis())));
            pw.append(' ');
            pw.append('-');
            pw.append(' ');
            pw.append(record.getMessage());
            Throwable thrown = record.getThrown();
            if (thrown != null) {
                pw.println();
                thrown.printStackTrace(pw);
            }
            pw.println();
            return sw.toString();
        }
    }
}
