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

package org.esa.snap.configurator;

import org.esa.snap.util.SystemUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Benchmark calculs to determine best performance parameters
 *
 * @author Manuel Campomanes
 */
public class Benchmark {

    /**
     * list of calculs
     */
    private List<BenchmarkSingleCalcul> benchmarkCalculs;

    public Benchmark(List<Integer> tileSizes, List<Integer> cacheSizes, List<Integer> nbThreads){
        if(tileSizes.isEmpty() || cacheSizes.isEmpty() || nbThreads.isEmpty()){
            throw new IllegalArgumentException("All benchmark parameters need to be filled");
        }
        benchmarkCalculs = new ArrayList<>();
        //generate possible calculs list
        for(Integer tileSize : tileSizes){
            for(Integer cacheSize : cacheSizes){
                for(Integer nbThread : nbThreads){
                    benchmarkCalculs.add(new BenchmarkSingleCalcul(tileSize, cacheSize, nbThread));
                }
            }
        }
    }

    /**
     * Get benchmark params with the lower execution time
     * @return PerformanceParameters
     */
    public BenchmarkSingleCalcul getFasterBenchmarkSingleCalcul(){
        Collections.sort(this.benchmarkCalculs);
        return this.benchmarkCalculs.get(0);
    }

    /**
     * Load performance parameters.
     *
     * @param performanceParameters
     */
    public void loadBenchmarkPerfParams(PerformanceParameters performanceParameters){
        ConfigurationOptimizer confOptimizer = ConfigurationOptimizer.getInstance();
        confOptimizer.updateCustomisedParameters(performanceParameters);
        try {
            confOptimizer.saveCustomisedParameters();
        } catch (IOException e) {
            SystemUtils.LOG.severe("Could not save performance parameters: " + e.getMessage());
        }
    }

    public String toString(){
        String benchmarksPrint = "Benchmark results sorted by execution time\n";
        benchmarksPrint += "(Tile size, Cache size, Nb threads) = Execution time \n\n";
        for(BenchmarkSingleCalcul benchmarkSingleCalcul : this.benchmarkCalculs){
            benchmarksPrint += benchmarkSingleCalcul.toString() + "\n";
        }
        return benchmarksPrint;
    }

    public List<BenchmarkSingleCalcul> getBenchmarkCalculs() {
        return benchmarkCalculs;
    }
}
