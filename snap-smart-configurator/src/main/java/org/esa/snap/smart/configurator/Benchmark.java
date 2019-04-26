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

import org.esa.snap.core.util.SystemUtils;

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

    public Benchmark(List<Integer> tileSizes, List<String> readerTileDimension, List<Integer> cacheSizes, List<Integer> nbThreads){

        if(tileSizes.isEmpty() || readerTileDimension.isEmpty() || cacheSizes.isEmpty() || nbThreads.isEmpty()){
            throw new IllegalArgumentException("All benchmark parameters need to be filled");
        }

        benchmarkCalculus = new ArrayList<>();

        // duplicate the first values, as the first run is always slower, but do not show the output
        benchmarkCalculus.add(new BenchmarkSingleCalculus(tileSizes.get(0), readerTileDimension.get(0), cacheSizes.get(0), nbThreads.get(0), true));

        //generate possible calculus list

        for(Integer tileSize : tileSizes) {
            for (String dim : readerTileDimension) {
                for (Integer cacheSize : cacheSizes) {
                    for (Integer nbThread : nbThreads) {
                        benchmarkCalculus.add(new BenchmarkSingleCalculus(tileSize, dim, cacheSize, nbThread));
                    }
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
        benchmarkPerformanceParameters.setTileWidth(benchmarkSingleCalculus.getTileWidth());
        benchmarkPerformanceParameters.setTileHeight(benchmarkSingleCalculus.getTileHeight());
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
            if(benchmarkSingleCalcul.isHidden()) {
                continue;
            }
            benchmarksPrint += benchmarkSingleCalcul.toString() + "\n";
        }
        return benchmarksPrint;
    }

    public String[] getColumnsNames(){
        return BenchmarkSingleCalculus.getColumnNames();
    }

    public String[] getColumnsNamesWithoutTileDimension(){
        return BenchmarkSingleCalculus.getColumnNamesWithoutTileDimension();
    }

    public int[][] getRows(){
        int numRows = this.benchmarkCalculus.size();
        int numColumns = getColumnsNames().length;
        if (numRows == 0 || numColumns == 0) {
            return null;
        }
        int[][] rows = new int[numRows][numColumns];
        for(int i = 0 ; i < numRows ; i++) {
            rows[i] = this.benchmarkCalculus.get(i).getData();
        }

        return rows;
    }

    public int[][] getRowsToShow() {
        int numRows = 0;
        int numColumns = getColumnsNames().length;

        for (int i = 0; i < this.benchmarkCalculus.size(); i++) {
            if (!this.benchmarkCalculus.get(i).isHidden()) {
                numRows++;
            }
        }
        if (numRows == 0 || numColumns == 0) {
            return null;
        }
        int[][] rows = new int[numRows][numColumns];
        int index = 0;
        for (int i = 0; i < this.benchmarkCalculus.size(); i++) {
            if (!this.benchmarkCalculus.get(i).isHidden()) {
                rows[index] = this.benchmarkCalculus.get(i).getData();
                index++;
            }
        }

        return rows;
    }

    public int[][] getRowsToShowWhitoutTileDimension() {
        int numRows = 0;
        int numColumns = getColumnsNamesWithoutTileDimension().length;

        for (int i = 0; i < this.benchmarkCalculus.size(); i++) {
            if (!this.benchmarkCalculus.get(i).isHidden()) {
                numRows++;
            }
        }
        if (numRows == 0 || numColumns == 0) {
            return null;
        }
        int[][] rows = new int[numRows][numColumns];
        int index = 0;
        for (int i = 0; i < this.benchmarkCalculus.size(); i++) {
            if (!this.benchmarkCalculus.get(i).isHidden()) {
                rows[index] = this.benchmarkCalculus.get(i).getDataWithoutTileDimension();
                index++;
            }
        }

        return rows;
    }

    public List<BenchmarkSingleCalculus> getBenchmarkCalculus() {
        return benchmarkCalculus;
    }

    public void addBenchmarkCalcul(BenchmarkSingleCalculus benchmarkSingleCalcul){
        this.benchmarkCalculus.add(benchmarkSingleCalcul);
    }

    public boolean isAlreadyInList(BenchmarkSingleCalculus benchmarkSingleCalculNew) {
        for(BenchmarkSingleCalculus benchmarkSingleCalcul : this.benchmarkCalculus){
            if(benchmarkSingleCalcul.hasIdenticalParameters(benchmarkSingleCalculNew)) {
                return true;
            }
        }
        return false;
    }
}
