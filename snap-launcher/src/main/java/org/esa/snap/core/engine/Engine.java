package org.esa.snap.core.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * This class is used to initialise a stand-alone use the SNAP Engine.
 */
public class Engine {

    private static final Engine instance = new Engine();
    private LoggerConfig loggerConfig;
    private Logger logger;

    private Engine() {
        loggerConfig = new LoggerConfig();
    }

    public static Engine instance() {
        return instance;
    }

    public Engine debug(boolean flag) {
        System.setProperty("snap.debug", Boolean.toString(flag));
        return this;
    }

    public boolean debug() {
        return Boolean.getBoolean("snap.debug");
    }

    public Engine installationDir(Path dir) {
        System.setProperty("snap.home", dir.toString());
        return this;
    }

    public Path installationDir() {
        return Paths.get(System.getProperty("snap.home", ""));
    }

    public Engine userDir(Path dir) {
        System.setProperty("snap.user", dir.toString());
        return this;
    }

    public Path userDir() {
        return Paths.get(System.getProperty("snap.user", ""));
    }

    public Engine configFile(Path dir) {
        System.setProperty("snap.config", dir.toString());
        return this;
    }

    public Path configFile() {
        String property = System.getProperty("snap.config");
        return property != null ? Paths.get(property) : null;
    }

    public Engine useDefaultConfig(boolean flag) {
        System.setProperty("snap.useDefaultConfig", Boolean.toString(flag));
        return this;
    }

    public boolean useDefaultConfig() {
        return Boolean.getBoolean("snap.useDefaultConfig");
    }

    public Engine useUserConfig(boolean flag) {
        System.setProperty("snap.useUserConfig", Boolean.toString(flag));
        return this;
    }

    public boolean useUserConfig() {
        return Boolean.getBoolean("snap.useUserConfig");
    }

    public void configure() {

        // Level 1: Custom configuration file
        Path configFile = configFile();
        if (configFile != null) {
            loadProperties(configFile);
        }

        // Level 2: Configuration file from SNAP's user directory
        if (useUserConfig()) {
            loadProperties(userDir().resolve("snap.config"));
        }

        // Level 3: Configuration file from SNAP's installation directory
        if (useDefaultConfig()) {
            loadProperties(installationDir().resolve(Paths.get("etc", "snap.config")));
        }
    }

    private void loadProperties(Path propertiesFile) {
        if (!Files.isRegularFile(propertiesFile)) {
            logger.warning(String.format("Can't find configuration file '%s'", propertiesFile));
            return;
        }

        Properties properties = new Properties();
        try {
            BufferedReader reader = Files.newBufferedReader(propertiesFile);
            try {
                properties.load(reader);
            } catch (IOException e) {
                reader.close();
            }
        } catch (IOException e) {
            logger.severe(String.format("Can't load system properties from '%s': %s", propertiesFile, e.getMessage()));
        }

        Set<String> names = properties.stringPropertyNames();
        for (String name : names) {
            String value = System.getProperty(name);
            if (value == null) {
                try {
                    System.setProperty(name, properties.getProperty(name));
                } catch (Exception e) {
                    logger.info(String.format("Can't set system property '%s': %s", name, e.getMessage()));
                }
            } else {
                logger.fine(String.format("Can't set system property '%s' as it is already set", name));
            }
        }

        loggerConfig.update();
    }


    private class LoggerConfig {
        private String loggerName;
        private String loggerLevel;

        public LoggerConfig() {
            update();
        }

        public void update() {
            String loggerName = System.getProperty("snap.logger.name", "org.esa.snap");
            String loggerLevel = System.getProperty("snap.logger.level", "INFO");
            if (!loggerName.equals(this.loggerName) || !loggerLevel.equals(this.loggerLevel)) {
                Engine.this.logger = Logger.getLogger(loggerName);
                this.loggerName = loggerName;
                this.loggerLevel = loggerLevel;
                try {
                    Level level = parseLogLevel(loggerLevel);
                    logger.setLevel(level);
                } catch (IllegalArgumentException e) {
                    logger.warning(String.format("Illegal log level '%s'", loggerLevel));
                } catch (SecurityException e) {
                    logger.warning(String.format("Failed to set log level '%s'", loggerLevel));
                }
                replaceConsoleLoggerFormatter(Logger.getLogger(""));
                replaceConsoleLoggerFormatter(Engine.this.logger);
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
