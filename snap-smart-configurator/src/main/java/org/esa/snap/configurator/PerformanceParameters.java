/*
 *
 * Copyright (C) 2014-2015 CS SI
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 *
 */


package org.esa.snap.configurator;


import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.EngineConfig;

import javax.media.jai.JAI;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Storage for performance parameters
 *
 * @author Nicolas Ducoin
 * @version $Revisions$ $Dates$
 */
public class PerformanceParameters {


    /**
     * Preferences key for the default tile size in pixels
     */
    public static final String PROPERTY_DEFAULT_TILE_SIZE = "snap.jai.defaultTileSize";

    /**
     * Preferences key for the cache size in Mb
     */
    public static final String PROPERTY_JAI_CACHE_SIZE = "snap.jai.tileCacheSize";



    private VMParameters vmParameters;
    private Path cachePath;
    private int nbThreads;

    private int defaultTileSize;
    private int cacheSize;

    /**
     * Default constructor
     */
    public PerformanceParameters() {
        vmParameters = new VMParameters("");
    }

    /**
     * Cloning constructor
     *
     * @param clone the performance parameter to copy
     */
    public PerformanceParameters(PerformanceParameters clone) {
        this.setVMParameters(clone.vmParameters.toString());
        this.setCachePath(clone.getCachePath());

        this.setNbThreads(clone.getNbThreads());
        this.setDefaultTileSize(clone.getDefaultTileSize());
        this.setCacheSize(clone.getCacheSize());
    }

    public void setVmXMX(long vmXMX) {
        vmParameters.setVmXMX(vmXMX);
    }

    public void setVmXMS(long vmXMS) {
        vmParameters.setVmXMS(vmXMS);
    }

    public long getVmXMX() {
        return vmParameters.getVmXMX();
    }


    public Path getCachePath() {
        return cachePath;
    }

    public void setCachePath(Path largeTileCache) {
        this.cachePath = largeTileCache;
    }

    public int getNbThreads() {
        return nbThreads;
    }

    public void setNbThreads(int nbThreads) {
        this.nbThreads = nbThreads;
    }

    /**
     * Build a string from vm parameters
     *
     * @return VM parameters as a String
     */
    public String getVMParameters() {
        return vmParameters.toString();
    }


    /**
     * set the Xmx, Xms and other parameters from a string
     *
     * @param vmParametersLine the vm parameters string to parse
     */
    public void setVMParameters(String vmParametersLine) {
        vmParameters = new VMParameters(vmParametersLine);
    }



    public int getDefaultTileSize() {
        return defaultTileSize;
    }

    public void setDefaultTileSize(int defaultTileSize) {
        this.defaultTileSize = defaultTileSize;
    }
    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }


    /**
     *
     * Reads the parameters files and system settings to retreive the actual performance parameters.
     * It updates the "actualParameters" according to the configuration loaded.
     *
     * @return the actual performance parameters, loaded by this method
     */
    synchronized static PerformanceParameters loadConfiguration() {

        Config configuration = Config.instance().load();
        Preferences preferences = configuration.preferences();

        PerformanceParameters actualParameters = new PerformanceParameters();

        VMParameters netBeansVmParameters = VMParameters.load();
        actualParameters.setVMParameters(netBeansVmParameters.toString());
        actualParameters.setCachePath(SystemUtils.getCacheDir().toPath());

        final int defaultNbThreads = JavaSystemInfos.getInstance().getNbCPUs();
        actualParameters.setNbThreads(preferences.getInt(SystemUtils.SNAP_PARALLELISM_PROPERTY_NAME, defaultNbThreads));
        actualParameters.setDefaultTileSize(preferences.getInt(PROPERTY_DEFAULT_TILE_SIZE, 0));
        actualParameters.setCacheSize(preferences.getInt(PROPERTY_JAI_CACHE_SIZE, 0));

        return actualParameters;
    }

    /**
     * Save the actual configuration
     *
     * @param confToSave The configuration to save
     */
    synchronized static void saveConfiguration(PerformanceParameters confToSave) throws IOException, BackingStoreException {

        if(!loadConfiguration().getVMParameters().equals(confToSave.getVMParameters())) {
            confToSave.vmParameters.save();
        }


        Config configuration = EngineConfig.instance().load();
        Preferences preferences = configuration.preferences();

        Path cachePath = confToSave.getCachePath();

        if(!Files.exists(cachePath)) {
            Files.createDirectories(cachePath);
        }


        if(Files.exists(cachePath)) {
            preferences.put(SystemUtils.SNAP_CACHE_DIR_PROPERTY_NAME, cachePath.toAbsolutePath().toString());
        } else {
            SystemUtils.LOG.severe("Directory for cache path does not exist");
        }

        int parallelism = confToSave.getNbThreads();
        int defaultTileSize = confToSave.getDefaultTileSize();
        int jaiCacheSize = confToSave.getCacheSize();
        preferences.putInt(SystemUtils.SNAP_PARALLELISM_PROPERTY_NAME, parallelism);
        preferences.putInt(PROPERTY_DEFAULT_TILE_SIZE, defaultTileSize);
        preferences.putInt(PROPERTY_JAI_CACHE_SIZE, jaiCacheSize);

        // effective change of jai parameters
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(jaiCacheSize);
        JAI.getDefaultInstance().getTileScheduler().setParallelism(parallelism);
        JAI.setDefaultTileSize(new Dimension(defaultTileSize, defaultTileSize));

        preferences.flush();
    }
}
