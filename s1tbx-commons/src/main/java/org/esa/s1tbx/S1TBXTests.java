/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx;

import org.esa.snap.gpf.TestProcessor;
import org.esa.snap.runtime.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

/**
 * Utilities for Operator unit tests
 * In order to test the datasets at Array, set the following to true in the nest.config
 * nest.test.ReadersOnAllProducts=true nest.test.ProcessingOnAllProducts=true
 */
public class S1TBXTests {

    private static final String S1TBX_TESTS = "s1tbx.tests";

    private static final Preferences testPreferences = Config.instance(S1TBX_TESTS).load().preferences();

    public static String rootPathTestProducts = "";

    public final static File[] rootArchivePaths = loadFilePath("test.rootArchivePaths");
    public final static File[] rootPathsTerraSarX = loadFilePath("test.rootPathTerraSarX");
    public final static File[] rootPathsASAR = loadFilePath("test.rootPathASAR");
    public final static File[] rootPathsRadarsat2 = loadFilePath("test.rootPathRadarsat2");
    public final static File[] rootPathsRadarsat1 = loadFilePath("test.rootPathRadarsat1");
    public final static File[] rootPathsSentinel1 = loadFilePath("test.rootPathSentinel1");
    public final static File[] rootPathsERS = loadFilePath("test.rootPathERS");
    public final static File[] rootPathsJERS = loadFilePath("test.rootPathJERS");
    public final static File[] rootPathsALOS = loadFilePath("test.rootPathALOS");
    public final static File[] rootPathsALOS2 = loadFilePath("test.rootPathALOS2");
    public final static File[] rootPathsCosmoSkymed = loadFilePath("test.rootPathCosmoSkymed");

    public static int subsetX = 0;
    public static int subsetY = 0;
    public static int subsetWidth = 0;
    public static int subsetHeight = 0;

    public static int maxIteration = 0;

    public static boolean canTestReadersOnAllProducts = false;
    public static boolean canTestProcessingOnAllProducts = false;

    static {
        if (testPreferences != null) {
            rootPathTestProducts = testPreferences.get("test.rootPathTestProducts", "");

            subsetX = Integer.parseInt(testPreferences.get("test.subsetX", "100"));
            subsetY = Integer.parseInt(testPreferences.get("test.subsetY", "100"));
            subsetWidth = Integer.parseInt(testPreferences.get("test.subsetWidth", "100"));
            subsetHeight = Integer.parseInt(testPreferences.get("test.subsetHeight", "100"));

            maxIteration = Integer.parseInt(testPreferences.get("test.maxProductsPerRootFolder", "1"));
            String testReadersOnAllProducts = testPreferences.get("test.ReadersOnAllProducts", "");
            String testProcessingOnAllProducts = testPreferences.get("test.ProcessingOnAllProducts", "");

            canTestReadersOnAllProducts = testReadersOnAllProducts != null && testReadersOnAllProducts.equalsIgnoreCase("true");
            canTestProcessingOnAllProducts = testProcessingOnAllProducts != null && testProcessingOnAllProducts.equalsIgnoreCase("true");
        }
    }

    private static File[] loadFilePath(final String id) {
        if (testPreferences == null)
            return new File[]{};

        final List<File> fileList = new ArrayList<>(3);
        final String pathsStr = testPreferences.get(id, "");
        final StringTokenizer st = new StringTokenizer(pathsStr, ",");
        while (st.hasMoreTokens()) {
            fileList.add(new File(st.nextToken()));
        }
        return fileList.toArray(new File[fileList.size()]);
    }

    public static TestProcessor createS1TBXTestProcessor() {
        return new TestProcessor();
    }
}
