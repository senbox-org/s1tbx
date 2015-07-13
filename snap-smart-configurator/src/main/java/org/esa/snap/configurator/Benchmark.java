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
import java.util.prefs.BackingStoreException;

/**
 * Benchmark calculs to determine best performance parameters
 *
 * @author Manuel Campomanes
 */
public class Benchmark {

    /**
     * list of calculs
     */
    private List<BenchmarkSingleCalculus> benchmarkCalculus;

    public Benchmark(List<Integer> tileSizes, List<Integer> cacheSizes, List<Integer> nbThreads){

        if(tileSizes.isEmpty() || cacheSizes.isEmpty() || nbThreads.isEmpty()){
            throw new IllegalArgumentException("All benchmark parameters need to be filled");
        }

        benchmarkCalculus = new ArrayList<>();

        // duplicate the first values, as the first run is allways slower
        benchmarkCalculus.add(new BenchmarkSingleCalculus(tileSizes.get(0), cacheSizes.get(0), nbThreads.get(0)));

        //generate possible calculs list
        for(Integer tileSize : tileSizes){
            for(Integer cacheSize : cacheSizes){
                for(Integer nbThread : nbThreads){
                    benchmarkCalculus.add(new BenchmarkSingleCalculus(tileSize, cacheSize, nbThread));
                }
            }
        }
    }

    /**
     * Get benchmark params with the lower execution time
     * @return PerformanceParameters
     */
    public BenchmarkSingleCalculus getFasterBenchmarkSingleCalculus(){
        Collections.sort(this.benchmarkCalculus);
        return this.benchmarkCalculus.get(0);
    }

    /**
     * Save Benchmark performance parameters.
     *
     * @param benchmarkSingleCalculus the object containing the tile size, nb threads, cache size for the
     *                                benchmark to be performed
     */
    public void loadBenchmarkPerfParams(BenchmarkSingleCalculus benchmarkSingleCalculus){
        ConfigurationOptimizer confOptimizer = ConfigurationOptimizer.getInstance();
        PerformanceParameters benchmarkPerformanceParameters = confOptimizer.getActualPerformanceParameters();
        benchmarkPerformanceParameters.setDefaultTileSize(benchmarkSingleCalculus.getTileSize());
        benchmarkPerformanceParameters.setCacheSize(benchmarkSingleCalculus.getCacheSize());
        benchmarkPerformanceParameters.setNbThreads(benchmarkSingleCalculus.getNbThreads());
        confOptimizer.updateCustomisedParameters(benchmarkPerformanceParameters);
        try {
            confOptimizer.saveCustomisedParameters();
        } catch (IOException|BackingStoreException e) {
            SystemUtils.LOG.severe("Could not save performance parameters: " + e.getMessage());
        }
    }

    public String toString(){
        String benchmarksPrint = "Benchmark results sorted by execution time\n";
        benchmarksPrint += "(Tile size, Cache size, Nb threads) = Execution time \n\n";
        for(BenchmarkSingleCalculus benchmarkSingleCalcul : this.benchmarkCalculus){
            benchmarksPrint += benchmarkSingleCalcul.toString() + "\n";
        }
        return benchmarksPrint;
    }

    public List<BenchmarkSingleCalculus> getBenchmarkCalculus() {
        return benchmarkCalculus;
    }

    public void addBenchmarkCalcul(BenchmarkSingleCalculus benchmarkSingleCalcul){
        this.benchmarkCalculus.add(benchmarkSingleCalcul);
    }
}
