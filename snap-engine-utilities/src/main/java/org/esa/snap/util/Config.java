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

import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.logging.BeamLogManager;

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

    private final PropertyMap appConfig = new PropertyMap();
    private final Map<String, PropertyMap> testPrefs = new HashMap<String, PropertyMap>(10);
    private static Config _instance = null;

    public static Config instance() {
        if (_instance == null) {
            _instance = new Config();
        }
        return _instance;
    }

    private Config() {
        load(appConfig, new File(SystemUtils.getApplicationHomeDir(), "config" + File.separator + SystemUtils.getApplicationContextId() + ".config"));
    }

    public static void load(final PropertyMap propMap, final File file) {
        if (!file.exists()) {
            BeamLogManager.getSystemLogger().severe(file.getAbsolutePath() + " not found");
        }
        try {
            propMap.load(file);

        } catch (IOException e) {
            BeamLogManager.getSystemLogger().severe("Unable to load application config " + e.getMessage());
        }
    }

    private void loadTestConfigs() {

        final File[] testFiles = getTestFiles(new File(SystemUtils.getApplicationHomeDir(), "config"));
        for (File testFile : testFiles) {
            try {
                PropertyMap testPref = new PropertyMap();
                testPref.load(testFile);
                testPrefs.put(testFile.getName(), testPref);

            } catch (IOException e) {
                BeamLogManager.getSystemLogger().severe("Unable to load test config " + e.getMessage());
            }
        }
    }

    private static File[] getTestFiles(final File folder) {
        final List<File> testFiles = new ArrayList<File>(10);
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

    public static PropertyMap getAppConfigPropertyMap() {
        return instance().appConfig;
    }

    public static PropertyMap getAutomatedTestConfigPropertyMap(final String name) {
        if (instance().testPrefs.isEmpty()) {
            instance().loadTestConfigs();
        }
        return instance().testPrefs.get(name);
    }
}
