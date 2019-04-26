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

import org.apache.commons.lang.StringUtils;

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
     * tile Width (px)
     */
    private String tileWidth;

    /**
     * tile Height (px)
     */
    private String tileHeight;

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

    /**
     * hide in output or not
     */
    private boolean hideOutput;

    /**
     * execution order
     */
    private int executionOrder;

    public BenchmarkSingleCalculus(int tileSize, String tileHeight, String tileWidth, int cacheSize, int nbThreads){
        this(tileSize, tileHeight, tileWidth, cacheSize, nbThreads, false);
    }

    public BenchmarkSingleCalculus(int tileSize, String dimension, int cacheSize, int nbThreads){
        this(tileSize, dimension, cacheSize, nbThreads, false);
    }

    public BenchmarkSingleCalculus(int tileSize, String tileHeight, String tileWidth, int cacheSize, int nbThreads, boolean hideOutput){
        this.tileSize = tileSize;
        this.tileHeight = tileHeight;
        this.tileWidth = tileWidth;
        this.cacheSize = cacheSize;
        this.nbThreads = nbThreads;
        this.hideOutput = hideOutput;
        this.executionTime = null;
    }

    public BenchmarkSingleCalculus(int tileSize, String dimension, int cacheSize, int nbThreads, boolean hideOutput){

        this(tileSize,
             PerformanceParameters.getHeightFromTileDimensionString(dimension),
             PerformanceParameters.getWidthFromTileDimensionString(dimension),
             cacheSize,
             nbThreads,
             hideOutput);
    }

    public String toString() {
        String toDiaplay = "(" + this.getTileHeight() + ", " + this.getCacheSize() + ", " + this.nbThreads + ") = ";
        if (this.executionTime != null) {
            toDiaplay += this.executionTime + " ms";
        } else {
            toDiaplay += "not computed";
        }
        return toDiaplay;
    }

    public static String[] getColumnNames(){
        String[] columnsNames = {"Execution Order", "Tile size",  "Tile Width", "Tile Height", "Cache size (MB)", "Nb threads", "Execution time (ms)"};
        return columnsNames;
    }

    public static String[] getColumnNamesWithoutTileDimension(){
        String[] columnsNames = {"Execution Order", "Tile size", "Cache size (MB)", "Nb threads", "Execution time (ms)"};
        return columnsNames;
    }

    public int[] getData() {
        int execution = -1;
        if (this.executionTime != null) {
            execution = this.executionTime.intValue();
        }

        int tileHeight = -1;
        int tileWidth = -1;
        try {
            tileHeight = Integer.parseInt(this.tileHeight);
        } catch (Exception e) {
            //Ignore
        }

        try {
            tileWidth = Integer.parseInt(this.tileWidth);
        } catch (Exception e) {
            //Ignore
        }

        int[] calculus = {this.executionOrder,tileSize, tileWidth, tileHeight, this.cacheSize, this.nbThreads, execution};
        return calculus;
    }

    public int[] getDataWithoutTileDimension() {
        int execution = -1;
        if (this.executionTime != null) {
            execution = this.executionTime.intValue();
        }
        int[] calculus = {this.executionOrder,this.tileSize, this.cacheSize, this.nbThreads, execution};
        return calculus;
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
    

    public String getTileWidth() {
        return tileWidth;
    }

    public String getTileHeight() {
        return tileHeight;
    }

    public int getTileSize() {
        return tileSize;
    }

    public String getDimensionString () {
        return PerformanceParameters.getDimensionStringFromWidthAndHeight(this.tileWidth, this.tileHeight);
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

    public void setHideOutput(boolean hide) {
        this.hideOutput = hide;
    }

    public boolean isHidden() {
        return this.hideOutput;
    }

    public boolean hasIdenticalParameters(BenchmarkSingleCalculus benchmarkSingleCalculus) {
        if(this.nbThreads != benchmarkSingleCalculus.getNbThreads()) {
            return false;
        }

        if(this.cacheSize != benchmarkSingleCalculus.getCacheSize()) {
            return false;
        }


        /*if(this.tileHeight.compareTo(benchmarkSingleCalculus.getTileHeight()) != 0 ) {
            return false;
        }

        if(this.tileWidth.compareTo(benchmarkSingleCalculus.getTileWidth()) != 0 ) {
            return false;
        }*/

        if(this.hideOutput != benchmarkSingleCalculus.isHidden()) {
            return false;
        }

        return true;
    }

    public void setExecutionOrder(int executionOrder) {
        this.executionOrder = executionOrder;
    }


}
