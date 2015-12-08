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

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * Provides Operators that are suitable for benchmark for the smart configurator
 *
 * The getBenchmarkOperatorAliases() needs to return aliases from OperatorSpis that can be benchmarked.
 *
 * @author Nicolas Ducoin
 */
public abstract class BenchmarkOperatorProvider {


    private Set<OperatorSpi> benchmarkOperatorSpis = new HashSet<>();

    protected abstract List<String> getBenchmarkOperatorAliases();

    public BenchmarkOperatorProvider() {

        OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        Set<OperatorSpi> operatorSpis = spiRegistry.getOperatorSpis();
        List<String> benchmarkOperatorAliases = getBenchmarkOperatorAliases();

        for(OperatorSpi operatorSpi : operatorSpis) {
            if(benchmarkOperatorAliases.contains(operatorSpi.getOperatorAlias())) {
                benchmarkOperatorSpis.add(operatorSpi);
            }
        }
    }

    public Set<OperatorSpi> getBenchmarkOperators() {
        return benchmarkOperatorSpis;
    }
}
