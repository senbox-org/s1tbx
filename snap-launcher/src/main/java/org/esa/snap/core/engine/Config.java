package org.esa.snap.core.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;
import java.util.logging.*;

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
 * <li>{@code snap.ignoreUserConfig}: Don't load configuration file named {@code snap.config} from SNAP user directory</li>
 * <li>{@code snap.ignoreDefaultConfig}: Don't load configuration file named {@code snap.config} from SNAP installation directory</li>
 * <li>{@code snap.excludedClusters}: List of comma-separated NetBeans cluster names to be excluded from the SNAP Engine. Valid values are the ones contained in {@code $snap.home/etc/snap.clusters} (expert setting)</li>
 * <li>{@code snap.excludedModules}: List of comma-separated module names to be excluded from the SNAP Engine. Module name: {@code [groupId:]artifactId}, default {@code groupId} is {@code org.esa.snap} (expert setting)</li>
 * <li>{@code snap.debug}: Log debugging information about SNAP Engine configuration, runtime, and launcher (expert setting)</li>
 * <li>{@code snap.log.name}: Name for the logger to be used (expert setting)</li>
 * <li>{@code snap.log.level}: The logging level to be used, one of OFF, ALL, DEBUG, INFO, WARNING, ERROR (expert setting)</li>
 * <li>{@code snap.configLoaded}: Whether the configuration has been loaded from any configuration file (expert setting)</li>
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
 * @see Runtime
 * @since SNAP 2.0
 */
public class Config {

    public static final String PROPERTY_HOME_DIR = "snap.home";
    public static final String PROPERTY_USER_DIR = "snap.user";
    public static final String PROPERTY_CONFIG_FILE = "snap.config";
    public static final String PROPERTY_EXCLUDED_CLUSTER_NAMES = "snap.excludedClusters";
    public static final String PROPERTY_EXCLUDED_MODULE_NAMES = "snap.excludedModules";
    public static final String PROPERTY_IGNORE_USER_CONFIG = "snap.ignoreUserConfig";
    public static final String PROPERTY_IGNORE_DEFAULT_CONFIG = "snap.ignoreDefaultConfig";
    public static final String PROPERTY_DEBUG = "snap.debug";
    public static final String PROPERTY_LOG_NAME = "snap.log.name";
    public static final String PROPERTY_LOG_LEVEL = "snap.log.level";
    public static final String PROPERTY_CONFIG_LOADED = "snap.configLoaded";

    static String[] DEFAULT_EXCLUDED_CLUSTER_NAMES = new String[]{
            "platform", "ide"
    };

    static String[] DEFAULT_EXCLUDED_MODULE_NAMES = new String[]{
            "org.esa.snap:netbeans-docwin",
            "org.esa.snap:netbeans-tile",
            "org.esa.snap:snap-gui-utilities",
            "org.esa.snap:snap-visat-rcp",
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

    private static final Config instance = new Config();
    private LoggerConfig loggerConfig;
    private Logger logger;

    private Config() {
        loggerConfig = new LoggerConfig();
    }

    public static Config instance() {
        return instance;
    }

    public Logger logger() {
        return logger;
    }

    public Config debug(boolean value) {
        setSystemProperty(PROPERTY_DEBUG, value);
        return this;
    }

    public boolean debug() {
        return Boolean.getBoolean(PROPERTY_DEBUG);
    }

    public Config homeDir(Path value) {
        setSystemProperty(PROPERTY_HOME_DIR, value);
        return this;
    }

    public Path homeDir() {
        return Paths.get(System.getProperty(PROPERTY_HOME_DIR, ""));
    }

    public Config userDir(Path value) {
        setSystemProperty(PROPERTY_USER_DIR, value);
        return this;
    }

    public Path userDir() {
        return Paths.get(System.getProperty(PROPERTY_USER_DIR, Paths.get(System.getProperty("user.home"), ".snap").toString()));
    }

    public Config configFile(Path value) {
        setSystemProperty(PROPERTY_CONFIG_FILE, value);
        return this;
    }

    public Path configFile() {
        String value = System.getProperty(PROPERTY_CONFIG_FILE);
        return value != null ? Paths.get(value) : null;
    }

    public Config ignoreDefaultConfig(boolean value) {
        setSystemProperty(PROPERTY_IGNORE_DEFAULT_CONFIG, value);
        return this;
    }

    public boolean ignoreDefaultConfig() {
        return Boolean.getBoolean(PROPERTY_IGNORE_DEFAULT_CONFIG);
    }

    public Config ignoreUserConfig(boolean value) {
        setSystemProperty(PROPERTY_IGNORE_USER_CONFIG, value);
        return this;
    }

    public boolean ignoreUserConfig() {
        return Boolean.getBoolean(PROPERTY_IGNORE_USER_CONFIG);
    }

    public Config excludedClusterNames(String... values) {
        setSystemProperty(PROPERTY_EXCLUDED_CLUSTER_NAMES, String.join(",", values));
        return this;
    }

    public String[] excludedClusterNames() {
        String value = System.getProperty(PROPERTY_EXCLUDED_CLUSTER_NAMES);
        return value != null ? value.split(",") : DEFAULT_EXCLUDED_CLUSTER_NAMES;
    }

    public Config excludedModuleNames(String... values) {
        setSystemProperty(PROPERTY_EXCLUDED_MODULE_NAMES, String.join(",", values));
        return this;
    }

    public String[] excludedModuleNames() {
        String value = System.getProperty(PROPERTY_EXCLUDED_MODULE_NAMES);
        return value != null ? value.split(",") : DEFAULT_EXCLUDED_MODULE_NAMES;
    }

    /**
     * @return true, if any configuration file has been loaded
     * @see #PROPERTY_CONFIG_LOADED
     */
    public boolean loaded() {
        return Boolean.getBoolean(PROPERTY_CONFIG_LOADED);
    }

    Config loaded(boolean value) {
        setSystemProperty(PROPERTY_CONFIG_LOADED, value);
        return this;
    }

    /**
     * Loads configured (Java properties) configuration files as described for {@link Config this class}.
     *
     * @return true, if any configuration file has been loaded
     * @see #loaded()
     */
    public boolean load() {

        if (loaded()) {
            return false;
        }

        boolean loaded = false;

        // Configuration level 0: System properties state at this point in time

        // Configuration level 1: Custom configuration file
        Path configFile = configFile();
        if (configFile != null) {
            loaded = loadProperties(configFile, true);
        }

        // Configuration level 2: User configuration file from SNAP's user directory
        if (!ignoreUserConfig()) {
            loaded = loadProperties(userDir().resolve("snap.config"), false) || loaded;
        }

        // Configuration level 3: Default configuration file from SNAP's installation directory
        if (!ignoreDefaultConfig()) {
            loaded = loadProperties(homeDir().resolve(Paths.get("etc", "snap.config")), false) || loaded;
        }

        loaded(loaded);

        return loaded;
    }

    private boolean loadProperties(Path propertiesFile, boolean mustExist) {

        if (!Files.isRegularFile(propertiesFile)) {
            String msg = String.format("Can't find configuration file '%s'", propertiesFile);
            if (mustExist) {
                throw new RuntimeException(msg);
            } else if (debug()) {
                logger.info(msg);
            }
            return false;
        }

        Properties properties = new Properties();
        try {
            try (BufferedReader reader = Files.newBufferedReader(propertiesFile)) {
                properties.load(reader);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, String.format("Can't load system properties from file '%s'", propertiesFile), e);
            return false;
        }

        boolean change = false;

        Set<String> names = properties.stringPropertyNames();
        for (String name : names) {
            String value = System.getProperty(name);
            if (value == null) {
                boolean ok = setSystemProperty(name, properties.getProperty(name), propertiesFile);
                if (ok) {
                    change = true;
                    if (debug()) {
                        logger.info(String.format("System property '%s' set to value '%s' (from file '%s')", name, System.getProperty(name), propertiesFile));
                    }
                }
            } else {
                if (debug()) {
                    logger.info(String.format("Ignored property '%s' (from file '%s') as it is already set", name, propertiesFile));
                }
            }
        }

        if (change) {
            loggerConfig.update();
        }

        return change;
    }

    boolean setSystemProperty(String name, boolean value) {
        return setSystemProperty(name, Boolean.toString(value));
    }

    boolean setSystemProperty(String name, Path value) {
        return setSystemProperty(name, value.toString());
    }

    boolean setSystemProperty(String name, String value) {
        return setSystemProperty(name, value, null);
    }

    boolean setSystemProperty(String name, String value, Path file) {
        try {
            System.setProperty(name, value);
        } catch (Throwable t) {
            String msg = String.format("Can't set system property '%s' (from %s)",
                    name,
                    file != null ? String.format("file '%s'", file) : "code");
            logger.log(Level.SEVERE, msg, t);
            return false;
        }
        if (debug()) {
            String msg = String.format("System property '%s' set to value '%s' (from %s)",
                    name,
                    System.getProperty(name),
                    file != null ? String.format("file '%s'", file) : "code");
            logger.info(msg);
        }
        return true;
    }


    private class LoggerConfig {
        private String loggerName;
        private String loggerLevel;

        public LoggerConfig() {
            update();
        }

        public void update() {
            String loggerName = System.getProperty(PROPERTY_LOG_NAME, "org.esa.snap");
            String loggerLevel = System.getProperty(PROPERTY_LOG_LEVEL, "INFO");
            if (!loggerName.equals(this.loggerName) || !loggerLevel.equals(this.loggerLevel)) {
                Config.this.logger = Logger.getLogger(loggerName);
                this.loggerName = loggerName;
                this.loggerLevel = loggerLevel;
                try {
                    Level level = parseLogLevel(loggerLevel);
                    logger.setLevel(level);
                } catch (IllegalArgumentException e) {
                    logger.warning(String.format("Illegal log-level '%s'", loggerLevel));
                } catch (SecurityException e) {
                    logger.warning(String.format("Failed to set log-level '%s'", loggerLevel));
                }
                replaceConsoleLoggerFormatter(Logger.getLogger(""));
                replaceConsoleLoggerFormatter(Config.this.logger);
            }
        }

        private Level parseLogLevel(String levelName) throws IllegalArgumentException {
            if ("DEBUG".equalsIgnoreCase(levelName)) {
                return Level.FINE;
            } else if ("ERROR".equalsIgnoreCase(levelName)) {
                return Level.SEVERE;
            } else {
                return Level.parse(levelName);
            }
        }

        private void replaceConsoleLoggerFormatter(Logger logger) {
            Handler[] handlers = logger.getHandlers();
            for (Handler handler : handlers) {
                if (handler instanceof ConsoleHandler) {
                    ConsoleHandler consoleHandler = (ConsoleHandler) handler;
                    consoleHandler.setFormatter(new Formatter() {
                        @Override
                        public String format(LogRecord record) {
                            return String.format("%s: %s%n", record.getLevel(), record.getMessage());
                        }
                    });
                }
            }
        }
    }
}
