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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * loads the nest.config file
 */
public class Config {

    private final PropertyMap configPrefs = new PropertyMap();
    private final Map<String, PropertyMap> testPrefs = new HashMap<String, PropertyMap>(10);
    private static Config instance = null;

    private Config() {
        try {
            configPrefs.load(new File(SystemUtils.getApplicationHomeDir(), "config" + File.separator + SystemUtils.getApplicationContextId() + ".config"));

            final File[] testFiles = getTestFiles(new File(SystemUtils.getApplicationHomeDir(), "config"));
            for (File testFile : testFiles) {
                PropertyMap testPref = new PropertyMap();
                testPref.load(testFile);
                testPrefs.put(testFile.getName(), testPref);
            }
        } catch (IOException e) {
            System.out.println("Unable to load config preferences " + e.getMessage());
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

    public static PropertyMap getConfigPropertyMap() {
        if (instance == null) {
            instance = new Config();
        }
        return instance.configPrefs;
    }

    public static PropertyMap getAutomatedTestConfigPropertyMap(final String name) {
        if (instance == null) {
            instance = new Config();
        }
        return instance.testPrefs.get(name);
    }
}
