package org.esa.snap.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * This class is used to configure a stand-alone SNAP Engine application. Various configuration settings may be
 * chained in a single expression. Using the various setter methods provided by this class will always result in a
 * change of an according global Java system property.
 * <p>
 * The following configuration properties are managed by this class.
 * <ul>
 * <li>{@code snap.home}: Path to the SNAP installation directory</li>
 * <li>{@code snap.userdir}: Path to the SNAP user directory</li>
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
 * <li>Load file given by {@code $snap.userdir/etc/snap.properties}, if any and if not {@code $snap.ignoreUserConfig}</li>
 * <li>Load file given by {@code $snap.home/etc/snap.properties}, if any and if not {@code $snap.ignoreDefaultConfig}</li>
 * </ol>
 *
 * @author Norman Fomferra
 * @see Launcher
 * @see Engine
 * @since SNAP 2.0
 */
public class Config {
    static final String CONFIG_FILE_EXT = ".properties";
    static final String CONFIG_KEY_SUFFIX = ".config";

    private final static Map<String, Config> INSTANCES = new HashMap<>();
    private final String name;
    private final EnginePreferences preferences;
    private boolean loaded;

    private Config(String name) {
        this(name, new EnginePreferences((EnginePreferences) EngineConfig.instance().preferences(), name));
    }

    protected Config(String name, EnginePreferences preferences) {
        this.name = name;
        this.preferences = preferences;
    }

    public static Config instance() {
        return EngineConfig.instance();
    }

    public static Config instance(String name) {
        Config config = INSTANCES.get(name);
        if (config == null) {
            config = new Config(name);
            add(config);
        }
        return config;
    }

    static void add(Config config) {
        INSTANCES.put(config.name(), config);
    }

    // Evil method! For testing only!
    void clear() {
        try {
            preferences.clear();
        } catch (BackingStoreException e) {
            // ok
        }
        loaded = false;
    }

    public String name() {
        return name;
    }

    public Logger logger() {
        return EngineConfig.instance().logger();
    }

    public boolean debug() {
        return EngineConfig.instance().debug();
    }

    public Path installDir() {
        return EngineConfig.instance().installDir();
    }

    public Path userDir() {
        return EngineConfig.instance().userDir();
    }

    public boolean ignoreDefaultConfig() {
        return EngineConfig.instance().ignoreDefaultConfig();
    }

    public boolean ignoreUserConfig() {
        return EngineConfig.instance().ignoreUserConfig();
    }

    /**
     * @return true, if the configuration has already been loaded
     */
    public boolean loaded() {
        return loaded;
    }

    /**
     * Gets the preferences containing the configuration values.
     *
     * @return The preferences.
     */
    public Preferences preferences() {
        return preferences;
    }

    /**
     * List all property keys beginning with prefix.
     *
     * @param prefix that a key must start with.
     * @return The keys beginning with prefix.
     */
    public String[] listKeys(final String prefix) throws BackingStoreException {
        final List<String> keyList = new ArrayList<>();
        final String[] keys = preferences.keys();
        for (String key : keys) {
            if (key.startsWith(prefix)) {
                keyList.add(key);
            }
        }
        return keyList.toArray(new String[keyList.size()]);
    }

    public Path storagePath() {
        return preferences.getBackingStorePath();
    }

    public Config storagePath(Path propertiesFile) {
        preferences.setBackingStorePath(propertiesFile);
        return this;
    }

    /**
     * Loads configuration values from a configuration file (Java properties) into this configuration.
     *
     * @return this instance
     */
    public Config load(Path propertiesFile) {
        Properties customProperties = loadProperties(propertiesFile, true);
        if (customProperties != null) {
            preferences.getProperties().putAll(customProperties);
        }
        return this;
    }

    /**
     * Loads configuration values from configuration files (Java properties) stored at standard locations;
     * <ol>
     *     <li>Default configuration file from ${installDir}/etc</li>
     *     <li>User configuration file from ${userDir}/etc</li>
     *     <li>Custom configuration file given by property name ${name}.config</li>
     * </ol>
     *
     * @return this instance
     * @see #loaded()
     */
    public Config load() {

        if (loaded) {
            return this;
        }

        loaded = true;

        // Configuration level 1: Default configuration file from ${installDir}/etc
        if (!ignoreDefaultConfig()) {
            Properties defaultProperties = loadProperties(installDir().resolve("etc").resolve(name() + CONFIG_FILE_EXT), false);
            if (defaultProperties != null) {
                // Properties from $installDir/etc are not put into preferences, they only serve as default values.
                Properties newProperties = new Properties(defaultProperties);
                newProperties.putAll(preferences.getProperties());
                preferences.setProperties(newProperties);
            }
        }

        // Configuration level 2: User configuration file from ${userDir}/etc
        if (!ignoreUserConfig()) {
            Properties userProperties = loadProperties(userDir().resolve("etc").resolve(name() + CONFIG_FILE_EXT), false);
            if (userProperties != null) {
                // Properties from $userDir/etc *are* put into preferences, they overwrite any existing values.
                preferences.getProperties().putAll(userProperties);
            }
        }

        // Configuration level 3: Custom configuration file
        String configPath = preferences.get(name() + CONFIG_KEY_SUFFIX, null);
        if (configPath != null) {
            Properties additionalProperties = loadProperties(Paths.get(configPath), true);
            if (additionalProperties != null) {
                // Properties from $configPath *are also* put into preferences, they overwrite any existing values.
                preferences.getProperties().putAll(additionalProperties);
            }
        }

        return this;
    }

    protected Properties loadProperties(Path propertiesFile, boolean mustExist) {

        if (!Files.isRegularFile(propertiesFile)) {
            String msg = String.format("Can't find configuration file '%s'", propertiesFile);
            if (mustExist) {
                throw new RuntimeException(msg);
            } else if (debug()) {
                logger().info(msg);
            }
            return null;
        }

        Properties properties = new Properties();
        try {
            try (BufferedReader reader = Files.newBufferedReader(propertiesFile)) {
                properties.load(reader);
            }
        } catch (IOException e) {
            logger().log(Level.SEVERE, String.format("Can't load system properties from file '%s'", propertiesFile), e);
            return null;
        }

        return properties;
    }

    boolean setSystemProperty(String name, String value) {
        try {
            System.setProperty(name, value);
        } catch (Throwable t) {
            String msg = String.format("Can't set system property '%s'", name);
            logger().log(Level.SEVERE, msg, t);
            return false;
        }
        if (debug()) {
            String msg = String.format("System property '%s' set to value '%s'", name, System.getProperty(name));
            logger().info(msg);
        }
        return true;
    }
}
