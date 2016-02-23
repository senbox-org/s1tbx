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
    private Long executionTime;

    public BenchmarkSingleCalculus(int tileSize, int cacheSize, int nbThreads){
        this.tileSize = tileSize;
        this.cacheSize = cacheSize;
        this.nbThreads = nbThreads;
        this.executionTime = null;
    }

   public String toString(){
       String toDiaplay = "("+this.getTileSize()+", "+this.getCacheSize()+", "+this.nbThreads+") = ";
       if(this.executionTime != null) {
           toDiaplay += this.executionTime+" ms";
       } else {
           toDiaplay += "not computed";
       }
       return toDiaplay;
   }

    @Override
    public int compareTo(BenchmarkSingleCalculus compareBenchmarkSingleCalcul) {
        int order;
        Long compareExecutionTime = compareBenchmarkSingleCalcul.executionTime;

        if(compareExecutionTime == null && this.executionTime == null) {
            order = 0;
        } else if(compareExecutionTime == null) {
            order = -1;
        } else if(this.executionTime == null) {
            order = 1;
        } else if(compareExecutionTime < this.executionTime){
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
