package org.esa.snap.runtime;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This class is used to configure a stand-alone SNAP Engine application. Various configuration settings may be
 * chained in a single expression. Using the various setter methods provided by this class will always result in a
 * change of an according global Java system property.
 * <p>
 * The following configuration properties are managed by this class.
 * <ul>
 * <li>{@code snap.home}: Path to the SNAP installation directory</li>
 * <li>{@code snap.user}: Path to the SNAP user directory</li>
 * <li>{@code snap.config}: Path to a SNAP configuration file (Java properties file format)</li>
 * <li>{@code snap.ignoreUserConfig}: Don't load any <code>&lt;name&gt;</code><code>.properties</code> file in SNAP user directory</li>
 * <li>{@code snap.ignoreDefaultConfig}: Don't load any <code>&lt;name&gt;</code><code>.properties</code> file in SNAP installation directory</li>
 * <li>{@code snap.excludedClusters}: Used only in SNAP Engine stand-alone mode: List of comma-separated NetBeans cluster names to be excluded from the Engine. Valid values are the ones contained in {@code $snap.home/etc/snap.clusters} (expert setting)</li>
 * <li>{@code snap.excludedModules}: Used only in SNAP Engine stand-alone mode: List of comma-separated module names to be excluded from the Engine. Module name: {@code [groupId:]artifactId}, default {@code groupId} is {@code org.esa.snap} (expert setting)</li>
 * <li>{@code snap.debug}: Log debugging information about SNAP Engine configuration, runtime, and launcher (expert setting)</li>
 * <li>{@code snap.logger.name}: Name for the logger to be used (expert setting)</li>
 * <li>{@code snap.log.level}: The logging level to be used, one of OFF, ALL, DEBUG, INFO, WARNING, ERROR (expert setting)</li>
 * </ul>
 * Configurations are not read from any configured (Java properties) files until the {@link #load()} method is called.
 * Loading of a configuration file adds new global system properties, but only <b>if individual properties
 * they have not already been set</b>.
 * <p>
 * The {@link #load()} method loads the various configuration files mentioned above in a well defined order:
 * <ol>
 * <li>Load file given by {@code $snap.config}, if any</li>
 * <li>Load file given by {@code $snap.user/snap.config}, if any and if not {@code $snap.ignoreUserConfig}</li>
 * <li>Load file given by {@code $snap.home/etc/snap.config}, if any and if not {@code $snap.ignoreDefaultConfig}</li>
 * </ol>
 *
 * @author Norman Fomferra
 * @see Launcher
 * @see Engine
 * @since SNAP 2.0
 */
public class EngineConfig extends Config {

    public static final String PROPERTY_INSTALL_DIR = "snap.home";
    public static final String PROPERTY_USER_DIR = "snap.userdir";
    public static final String PROPERTY_CONFIG_FILE = "snap" + CONFIG_KEY_SUFFIX;
    public static final String PROPERTY_EXCLUDED_CLUSTER_NAMES = "snap.excludedClusters";
    public static final String PROPERTY_EXCLUDED_MODULE_NAMES = "snap.excludedModules";
    public static final String PROPERTY_IGNORE_USER_CONFIG = "snap.ignoreUserConfig";
    public static final String PROPERTY_IGNORE_DEFAULT_CONFIG = "snap.ignoreDefaultConfig";
    public static final String PROPERTY_DEBUG = "snap.debug";
    public static final String PROPERTY_LOGGER_NAME = "snap.logger.name";
    public static final String PROPERTY_LOG_LEVEL = "snap.log.level";

    static String[] DEFAULT_EXCLUDED_CLUSTER_NAMES = new String[]{
            "bin", "etc", "platform", "ide", "java"
    };

    static String[] DEFAULT_EXCLUDED_MODULE_NAMES = new String[]{
            "org.esa.snap:netbeans-docwin",
            "org.esa.snap:netbeans-tile",
            "org.esa.snap:snap-worldwind",
            "org.esa.snap:snap-rcp",
            "org.esa.snap:snap-ui",
            "org.esa.snap:ceres-ui",
            "org.esa.snap:snap-gpf-ui",
            "org.esa.snap:snap-dem-ui",
            "org.esa.snap:snap-pixel-extraction-ui",
            "org.esa.snap:snap-unmix-ui",
            "org.esa.snap:snap-binning-ui",
            "org.esa.snap:snap-collocation-ui",
    };

    private static EngineConfig INSTANCE = new EngineConfig();
    private Logger logger;

    private EngineConfig() {
        super("snap", new EnginePreferences("snap"));
        add(this);
        initLogger();
    }

    public static EngineConfig instance() {
        return INSTANCE;
    }

    @Override
    public Logger logger() {
        return logger;
    }

    public EngineConfig debug(boolean value) {
        preferences().putBoolean(PROPERTY_DEBUG, value);
        return this;
    }

    @Override
    public boolean debug() {
        return preferences().getBoolean(PROPERTY_DEBUG, false);
    }

    public EngineConfig installDir(Path value) {
        preferences().put(PROPERTY_INSTALL_DIR, value.toString());
        return this;
    }

    @Override
    public Path installDir() {
        String value = preferences().get(PROPERTY_INSTALL_DIR, null);
        if (value != null) {
            return Paths.get(value).normalize();
        }
        return Paths.get(System.getProperty("user.dir")).normalize();
    }

    public EngineConfig userDir(Path value) {
        preferences().put(PROPERTY_USER_DIR, value.toString());
        return this;
    }

    @Override
    public Path userDir() {
        String value = preferences().get(PROPERTY_USER_DIR, null);
        if (value != null) {
            return Paths.get(value);
        }
        return Paths.get(System.getProperty("user.home"), ".snap");
    }

    public EngineConfig configFile(Path value) {
        preferences().put(PROPERTY_CONFIG_FILE, value.toString());
        return this;
    }

    public Path configFile() {
        String value = preferences().get(PROPERTY_CONFIG_FILE, null);
        return value != null ? Paths.get(value) : null;
    }

    public EngineConfig ignoreDefaultConfig(boolean value) {
        preferences().putBoolean(PROPERTY_IGNORE_DEFAULT_CONFIG, value);
        return this;
    }

    public String loggerName() {
        return preferences().get(PROPERTY_LOGGER_NAME, "org.esa.snap");
    }

    public Config loggerName(String name) {
        preferences().put(PROPERTY_LOGGER_NAME, name);
        updateLogger();
        return this;
    }

    public Level logLevel() {
        return parseLogLevelName(preferences().get(PROPERTY_LOG_LEVEL, "INFO"));
    }

    public EngineConfig logLevel(Level level) {
        return logLevel(level.getName());
    }

    public EngineConfig logLevel(String levelName) {
        preferences().put(PROPERTY_LOG_LEVEL, levelName);
        updateLogger();
        return this;
    }

    @Override
    public boolean ignoreDefaultConfig() {
        return preferences().getBoolean(PROPERTY_IGNORE_DEFAULT_CONFIG, false);
    }

    public EngineConfig ignoreUserConfig(boolean value) {
        preferences().putBoolean(PROPERTY_IGNORE_USER_CONFIG, value);
        return this;
    }

    @Override
    public boolean ignoreUserConfig() {
        return preferences().getBoolean(PROPERTY_IGNORE_USER_CONFIG, false);
    }

    public EngineConfig excludedClusterNames(String... values) {
        preferences().put(PROPERTY_EXCLUDED_CLUSTER_NAMES, String.join(",", values));
        return this;
    }

    public String[] excludedClusterNames() {
        String value = preferences().get(PROPERTY_EXCLUDED_CLUSTER_NAMES, null);
        return value != null ? value.split(",") : DEFAULT_EXCLUDED_CLUSTER_NAMES;
    }

    public EngineConfig excludedModuleNames(String... values) {
        preferences().put(PROPERTY_EXCLUDED_MODULE_NAMES, String.join(",", values));
        return this;
    }

    public String[] excludedModuleNames() {
        String value = preferences().get(PROPERTY_EXCLUDED_MODULE_NAMES, null);
        return value != null ? value.split(",") : DEFAULT_EXCLUDED_MODULE_NAMES;
    }


    /**
     * Loads configured (Java properties) configuration files as described for the {@link Config this class}.
     *
     * @return this instance.
     * @see #loaded()
     */
    @Override
    public EngineConfig load() {
        super.load();
        Set<String> clusterNames = loadOtherClusterNames();
        for (String clusterName : clusterNames) {
            instance(clusterName).load();
        }
        return this;
    }

    @Override
    protected Properties loadProperties(Path propertiesFile, boolean mustExist) {
        Properties properties = super.loadProperties(propertiesFile, mustExist);
        if (properties != null) {
            updateLogger();
        }
        return properties;
    }


    private Set<String> loadOtherClusterNames() {
        Set<String> clusterNames = Collections.emptySet();
        Path clustersFile = installDir().resolve("etc").resolve("snap.clusters");
        if (Files.isRegularFile(clustersFile)) {
            try {
                clusterNames = Files.readAllLines(clustersFile).stream().filter(name -> !name.trim().isEmpty()).collect(Collectors.toSet());
            } catch (IOException e) {
                logger().log(Level.SEVERE, String.format("Failed to load clusters file from '%s'", clustersFile), e);
            }
        }
        for (String clusterName : excludedClusterNames()) {
            clusterNames.remove(clusterName);
        }
        clusterNames.remove("snap");
        return clusterNames;
    }

    private static Level parseLogLevelName(String levelName) {
        if ("DEBUG".equalsIgnoreCase(levelName)) {
            return Level.FINE;
        } else if ("ERROR".equalsIgnoreCase(levelName)) {
            return Level.SEVERE;
        } else {
            try {
                return Level.parse(levelName);
            } catch (IllegalArgumentException e) {
                return Level.INFO;
            }
        }
    }

    private void initLogger() {
        // This prevents our logger to be reconfigured by hsqldb (used by geotools)
        // see documentation of org.hsqldb.lib.FrameworkLogger
        System.setProperty("hsqldb.reconfig_logging", "false");
        setLogger(loggerName(), logLevel());
    }

    void updateLogger() {
        String loggerName = loggerName();
        Level logLevel = logLevel();
        if (logger == null || !loggerName.equals(logger.getName()) || !logLevel.equals(logger.getLevel())) {
            setLogger(loggerName, logLevel);
        }
    }

    private void setLogger(String loggerName, Level logLevel) {
        logger = Logger.getLogger(loggerName);
        logger.setLevel(logLevel);
        replaceConsoleLoggerFormatter(Logger.getLogger(""));
        replaceConsoleLoggerFormatter(logger);
    }

    private void replaceConsoleLoggerFormatter(Logger logger) {
        Handler[] handlers = logger.getHandlers();
        for (Handler handler : handlers) {
            if (handler instanceof ConsoleHandler) {
                ConsoleHandler consoleHandler = (ConsoleHandler) handler;
                consoleHandler.setFormatter(new LogRecordFormatter());
            }
        }
    }

    private static class LogRecordFormatter extends Formatter {
        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        @Override
        public String format(LogRecord record) {
            if (record.getThrown() != null) {
                StringWriter stackTrace = new StringWriter();
                record.getThrown().printStackTrace(new PrintWriter(stackTrace));
                return String.format("%s: %s: %s%n%s%n",
                                     record.getLevel(),
                                     record.getSourceClassName(),
                                     record.getMessage(),
                                     stackTrace.toString());
            } else {
                return String.format("%s: %s: %s%n",
                                     record.getLevel(),
                                     record.getSourceClassName(),
                                     record.getMessage());
            }
        }
    }
}
