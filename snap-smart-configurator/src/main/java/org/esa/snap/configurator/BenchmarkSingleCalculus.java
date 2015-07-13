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

/**
 * Benchmark calcul (test one processing with a set of parameters)
 *
 * @author Manuel Campomanes
 */
public class BenchmarkSingleCalculus implements Comparable<BenchmarkSingleCalculus> {

    /**
     * tile size (px)
     */
    private int tileSize;

    /**
     * cache size (MB)
     */
    private int cacheSize;

    /**
     * number of processor threads
     */
    private int nbThreads;

    /**
     * execution time
     */
    private long executionTime;

    public BenchmarkSingleCalculus(int tileSize, int cacheSize, int nbThreads){
        this.tileSize = tileSize;
        this.cacheSize = cacheSize;
        this.nbThreads = nbThreads;
        this.executionTime = Long.MAX_VALUE;
    }

   public String toString(){
       return "("+this.getTileSize()+", "+this.getCacheSize()+", "+this.nbThreads+") = "+ this.executionTime+" ms";
   }

    @Override
    public int compareTo(BenchmarkSingleCalculus compareBenchmarkSingleCalcul) {
        int order;
        long compareExecutionTime = compareBenchmarkSingleCalcul.executionTime;
        if(compareExecutionTime < this.executionTime){
            order = 1;
        }
        else {
            order = -1;
        }
        return order;
    }

    public int getTileSize() {
        return tileSize;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public int getNbThreads() {
        return nbThreads;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }
}
