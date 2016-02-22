/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.sar.gpf;

import org.esa.snap.smart.configurator.BenchmarkOperatorProvider;

import java.util.Arrays;
import java.util.List;

/**
 * Provides OperatorSpis from S1TBX that are suitable for the configuration optimizer benchmark
 *
 * @author Luis Veci
 */
public class S1TBXSarProcessingBenchmarkOperatorProvider extends BenchmarkOperatorProvider {

    @Override
    protected List<String> getBenchmarkOperatorAliases() {
        String[] s1tbxBenchmarkOperatorNames = {"Terrain-Correction", "Speckle-Filter", "Multilook", "Calibration",
                    "TOPSAR-Deburst", "Interferogram",
                    "Polarimetric-Decomposition", "Polarimetric-Speckle-Filter", "Polarimetric-Classification"};
        return Arrays.asList(s1tbxBenchmarkOperatorNames);
    }
}
