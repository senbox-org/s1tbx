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
package org.esa.s1tbx;

import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
import org.esa.snap.util.Config;

/**
 * Constants for benchmarking
 */
public class BenchConstants {
    public static int numIterations = 1;
    public static int maxDimensions = 3000;

    private static final PropertyMap testPreferences = Config.getAppConfigPropertyMap();
    private final static String contextID = SystemUtils.getApplicationContextId();

    private static final String testBenchmarks = testPreferences.getPropertyString(contextID + ".test.RunBenchmarks");
    public static final boolean runBenchmarks = testBenchmarks != null && testBenchmarks.equalsIgnoreCase("true");
}
