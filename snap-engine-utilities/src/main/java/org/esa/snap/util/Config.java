/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * loads the *.config files
 */
public class Config {

    private final PropertiesMap appConfig = new PropertiesMap();
    private final Map<String, PropertiesMap> testPrefs = new HashMap<>(10);
    private static Config _instance = null;

    public static Config instance() {
        if (_instance == null) {
            _instance = new Config();
        }
        return _instance;
    }

    private Config() {
        load(appConfig, new File(SystemUtils.getApplicationDataDir(), "config" + File.separator + SystemUtils.getApplicationContextId() + ".config"));
    }

    public static void load(final PropertiesMap propMap, final File file) {
        if (!file.exists()) {
            SystemUtils.LOG.severe(file.getAbsolutePath() + " not found");
        }
        try {
            propMap.load(file);

        } catch (IOException e) {
            SystemUtils.LOG.severe("Unable to load application config " + e.getMessage());
        }
    }

    private void loadTestConfigs() {

        final File[] testFiles = getTestFiles(new File(SystemUtils.getApplicationDataDir(), "config"));
        for (File testFile : testFiles) {
            try {
                final PropertiesMap testPref = new PropertiesMap();
                testPref.load(testFile);
                testPrefs.put(testFile.getName(), testPref);

            } catch (IOException e) {
                SystemUtils.LOG.severe("Unable to load test config " + e.getMessage());
            }
        }
    }

    private static File[] getTestFiles(final File folder) {
        final List<File> testFiles = new ArrayList<>(10);
        final File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().toLowerCase().endsWith(".tests")) {
                    testFiles.add(file);
                }
            }
        }
        return testFiles.toArray(new File[testFiles.size()]);
    }

    public static PropertiesMap getAppConfigPropertyMap() {
        return instance().appConfig;
    }

    public static PropertiesMap getAutomatedTestConfigPropertyMap(final String name) {
        if (instance().testPrefs.isEmpty()) {
            instance().loadTestConfigs();
        }
        return instance().testPrefs.get(name);
    }
}
