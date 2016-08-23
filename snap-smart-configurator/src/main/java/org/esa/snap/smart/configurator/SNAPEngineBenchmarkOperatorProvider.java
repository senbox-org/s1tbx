/*
 * Copyright (C) 2015 CS SI
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
package org.esa.snap.smart.configurator;

import java.util.Arrays;
import java.util.List;

/**
 * Provides OperatorSpis from SNAP Engine that are suitable for the configuration optimiser benchmark
 *
 * @author Nicolas Ducoin
 */
public class SNAPEngineBenchmarkOperatorProvider extends BenchmarkOperatorProvider {

    @Override
    protected List<String> getBenchmarkOperatorAliases() {
        String[] snapEngineBenchmarkOperatorNames = {"GLCM", "Reproject", "KMeansClusterAnalysis", "Write", "StoredGraph"};
        return Arrays.asList(snapEngineBenchmarkOperatorNames);
    }
}
