package org.esa.snap.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;
import java.util.prefs.Preferences;
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
 * @see Engine
 * @since SNAP 2.0
 */
public class Config {

    public static final String PROPERTY_INSTALL_DIR = "snap.home";
    public static final String PROPERTY_USER_DIR = "snap.userdir";
    public static final String PROPERTY_CONFIG_FILE = "snap.config";
    public static final String PROPERTY_EXCLUDED_CLUSTER_NAMES = "snap.excludedClusters";
    public static final String PROPERTY_EXCLUDED_MODULE_NAMES = "snap.excludedModules";
    public static final String PROPERTY_IGNORE_USER_CONFIG = "snap.ignoreUserConfig";
    public static final String PROPERTY_IGNORE_DEFAULT_CONFIG = "snap.ignoreDefaultConfig";
    public static final String PROPERTY_DEBUG = "snap.debug";
    public static final String PROPERTY_LOG_NAME = "snap.log.name";
    public static final String PROPERTY_LOG_LEVEL = "snap.log.level";

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

    private static Config instance = new Config();
    private LoggerConfig loggerConfig;
    private Logger logger;
    private Map<String, EnginePreferences> preferencesMap;
    private EnginePreferences preferences;

    private Config() {
        loggerConfig = new LoggerConfig(); // also sets 'logger'
        preferencesMap = new HashMap<>();
        preferences = new EnginePreferences("snap");
        preferencesMap.put(preferences.name(), preferences);
    }

    public static Config instance() {
        return instance;
    }

    static Config newInstance() {
        instance = new Config();
        return instance();
    }

    public Logger logger() {
        return logger;
    }

    public Config debug(boolean value) {
        preferences().putBoolean(PROPERTY_DEBUG, value);
        return this;
    }

    public boolean debug() {
        return preferences().getBoolean(PROPERTY_DEBUG, false);
    }

    public Config installDir(Path value) {
        preferences().put(PROPERTY_INSTALL_DIR, value.toString());
        return this;
    }

    public Path installDir() {
        return Paths.get(preferences().get(PROPERTY_INSTALL_DIR, ""));
    }

    public Config userDir(Path value) {
        preferences().put(PROPERTY_USER_DIR, value.toString());
        return this;
    }

    public Path userDir() {
        String value = preferences().get(PROPERTY_USER_DIR, Paths.get(System.getProperty("user.home"), ".snap").toString());
        return Paths.get(value);
    }

    public Config configFile(Path value) {
        preferences().put(PROPERTY_CONFIG_FILE, value.toString());
        return this;
    }

    public Path configFile() {
        String value = preferences().get(PROPERTY_CONFIG_FILE, null);
        return value != null ? Paths.get(value) : null;
    }

    public Config ignoreDefaultConfig(boolean value) {
        preferences().putBoolean(PROPERTY_IGNORE_DEFAULT_CONFIG, value);
        return this;
    }

    public boolean ignoreDefaultConfig() {
        return preferences().getBoolean(PROPERTY_IGNORE_DEFAULT_CONFIG, false);
    }

    public Config ignoreUserConfig(boolean value) {
        preferences().putBoolean(PROPERTY_IGNORE_USER_CONFIG, value);
        return this;
    }

    public boolean ignoreUserConfig() {
        return preferences().getBoolean(PROPERTY_IGNORE_USER_CONFIG, false);
    }

    public Config excludedClusterNames(String... values) {
        preferences().put(PROPERTY_EXCLUDED_CLUSTER_NAMES, String.join(",", values));
        return this;
    }

    public String[] excludedClusterNames() {
        String value = preferences().get(PROPERTY_EXCLUDED_CLUSTER_NAMES, null);
        return value != null ? value.split(",") : DEFAULT_EXCLUDED_CLUSTER_NAMES;
    }

    public Config excludedModuleNames(String... values) {
        preferences().put(PROPERTY_EXCLUDED_MODULE_NAMES, String.join(",", values));
        return this;
    }

    public String[] excludedModuleNames() {
        String value = preferences().get(PROPERTY_EXCLUDED_MODULE_NAMES, null);
        return value != null ? value.split(",") : DEFAULT_EXCLUDED_MODULE_NAMES;
    }

    /**
     * @return true, if the configuration has already been loaded
     */
    public boolean loaded() {
        return preferences.isLoaded();
    }

    /**
     * Gets the default configuration as Java preferences.
     *
     * @return The default configuration, never {@code null}.
     */
    public Preferences preferences() {
        return preferences;
    }

    /**
     * Gets the configuration with the specified name as Java preferences.
     *
     * @param configName The configuration name.
     * @return The named configuration, never {@code null}.
     */
    private Preferences preferences(String configName) {
        return loadPreferences(configName);
    }

    /**
     * Loads configured (Java properties) configuration files as described for {@link Config this class}.
     *
     * @return true, if any configuration file has been loaded
     * @see #loaded()
     */
    public boolean load() {
        loadPreferences("snap");
        return loaded();
    }


    /**
     * Loads configured (Java properties) configuration files as described for {@link Config this class}.
     *
     * @return The loaded properties or {@code null} if no configuration file was found.
     * @see #loaded()
     */
    private EnginePreferences loadPreferences(String configName) {

        EnginePreferences preferences = preferencesMap.get(configName);
        if (preferences == null) {
            preferences = new EnginePreferences(this.preferences, configName);
            preferencesMap.put(configName, preferences);
        }

        if (preferences.isLoaded()) {
            return preferences;
        }

        preferences.setLoaded(true);

        // Configuration level 1: Default configuration file from SNAP installation directory
        if (!ignoreDefaultConfig()) {
            Properties defaultProperties = loadProperties(installDir().resolve(Paths.get("etc", configName + ".config")), false);
            if (defaultProperties != null) {
                Properties newProperties = new Properties(defaultProperties);
                newProperties.putAll(preferences.getProperties());
                preferences.setProperties(newProperties);
            }
        }

        // Configuration level 2: User configuration file from SNAP user directory
        if (!ignoreUserConfig()) {
            Properties userProperties = loadProperties(userDir().resolve(configName + ".config"), false);
            if (userProperties != null) {
                preferences.getProperties().putAll(userProperties);
            }
        }

        // Configuration level 3: Custom configuration file
        String configPath = preferences.get(configName + ".config", null);
        if (configPath != null) {
            Properties additionalProperties = loadProperties(Paths.get(configPath), true);
            if (additionalProperties != null) {
                preferences.getProperties().putAll(additionalProperties);
            }
        }

        // Configuration level 4: System properties
        Set<String> names = System.getProperties().stringPropertyNames();
        for (String name : names) {
            if (name.startsWith(configName + ".")) {
                preferences.getProperties().put(name, System.getProperty(name));
            }
        }

        // Load configurations of clusters other than 'snap'
        if (preferences == this.preferences) {
            List<String> clusterNames = loadClusterNames();
            for (String clusterName : clusterNames) {
                if (!clusterName.equals(preferences.name())) {
                    loadPreferences(clusterName);
                }
            }
        }

        return preferences;
    }

    private Properties loadProperties(Path propertiesFile, boolean mustExist) {

        if (!Files.isRegularFile(propertiesFile)) {
            String msg = String.format("Can't find configuration file '%s'", propertiesFile);
            if (mustExist) {
                throw new RuntimeException(msg);
            } else if (debug()) {
                logger.info(msg);
            }
            return null;
        }

        Properties properties = new Properties();
        try {
            try (BufferedReader reader = Files.newBufferedReader(propertiesFile)) {
                properties.load(reader);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, String.format("Can't load system properties from file '%s'", propertiesFile), e);
            return null;
        }

        loggerConfig.update();

        return properties;
    }


    private List<String> loadClusterNames() {
        List<String> clusterNames = new ArrayList<>();
        Path clustersFile = installDir().resolve("etc").resolve("snap.clusters");
        if (Files.isRegularFile(clustersFile)) {
            try {
                clusterNames = Files.readAllLines(clustersFile).stream().filter(name -> !name.trim().isEmpty()).collect(Collectors.toList());
            } catch (IOException e) {
                logger.log(Level.SEVERE, String.format("Failed to load clusters file from '%s'", clustersFile), e);
            }
        }
        if (!clusterNames.contains("snap")) {
            clusterNames.add("snap");
        }
        return clusterNames;
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
