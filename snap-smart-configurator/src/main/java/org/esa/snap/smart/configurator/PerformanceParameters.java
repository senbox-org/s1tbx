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


package org.esa.snap.smart.configurator;


import org.apache.commons.lang.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.jai.JAIUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.EngineConfig;

import javax.media.jai.JAI;
import javax.media.jai.TileCache;
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
    private static final String PROPERTY_DEFAULT_TILE_SIZE = "snap.jai.defaultTileSize";

    /**
     * Preferences key for the default reader tile width
     */
    private static final String SYSPROP_READER_TILE_WIDTH = "snap.dataio.reader.tileWidth";

    /**
     * Preferences key for the default reader tile height
     */
    private static final String SYSPROP_READER_TILE_HEIGHT = "snap.dataio.reader.tileHeight";

    /**
     * Preferences key for the cache size in Mb
     */
    private static final String PROPERTY_JAI_CACHE_SIZE = "snap.jai.tileCacheSize";



    private VMParameters vmParameters;
    private Path cachePath;
    private int nbThreads;

    private int defaultTileSize;
    private String tileWidth;
    private String tileHeight;

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
        this.setTileHeight(clone.getTileHeight());
        this.setTileWidth(clone.getTileWidth());
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

    public String getTileWidth() {
        return tileWidth;
    }

    public void setTileWidth(String tileWidth) {
        this.tileWidth = tileWidth;
    }

    public String getTileHeight() {
        return tileHeight;
    }

    public void setTileHeight(String tileHeight) {
        this.tileHeight = tileHeight;
    }

    public String getTileDimension() {

        if (tileHeight == null || tileWidth == null) {
            return null;
        }
        String dimension = tileWidth;

        if(tileHeight.compareTo(tileWidth) != 0) {
            dimension = dimension + "," + tileHeight;
        }
        return dimension;
    }

    public void setTileDimension(String dimension) {

        this.tileWidth = getWidthFromTileDimensionString(dimension);
        this.tileHeight = getHeightFromTileDimensionString(dimension);
    }


    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }


    /**
     *
     * Reads the parameters files and system settings to retrieve the actual performance parameters.
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
        actualParameters.setTileWidth(preferences.get(SYSPROP_READER_TILE_WIDTH, null));
        actualParameters.setTileHeight(preferences.get(SYSPROP_READER_TILE_HEIGHT, null));
        actualParameters.setCacheSize(preferences.getInt(PROPERTY_JAI_CACHE_SIZE, 1024));

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
            confToSave.vmParameters.saveToVMOptions(VMParameters.getGptVmOptionsPath());
            confToSave.vmParameters.saveToVMOptions(VMParameters.getPconvertVmOptionsPath());
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

        JAI ff = JAI.getDefaultInstance();


        int parallelism = confToSave.getNbThreads();
        int defaultTileSize = confToSave.getDefaultTileSize();
        int jaiCacheSize = confToSave.getCacheSize();
        String tileWidth = confToSave.getTileWidth();
        String tileHeight = confToSave.getTileHeight();

        preferences.putInt(SystemUtils.SNAP_PARALLELISM_PROPERTY_NAME, parallelism);

        if(tileWidth == null) {
            preferences.remove(SYSPROP_READER_TILE_WIDTH);
        } else {
            preferences.put(SYSPROP_READER_TILE_WIDTH,tileWidth);
        }
        if(tileHeight == null) {
            preferences.remove(SYSPROP_READER_TILE_HEIGHT);
        } else {
            preferences.put(SYSPROP_READER_TILE_HEIGHT,tileHeight);
        }

        preferences.putInt(PROPERTY_DEFAULT_TILE_SIZE, defaultTileSize);
        preferences.putInt(PROPERTY_JAI_CACHE_SIZE, jaiCacheSize);

        preferences.flush();

        // effective change of jai parameters
        JAIUtils.setDefaultTileCacheCapacity(jaiCacheSize);
        JAI.getDefaultInstance().getTileScheduler().setParallelism(parallelism);
        JAI.setDefaultTileSize(new Dimension(defaultTileSize,defaultTileSize));

    }

    //dimension: width,height
    public static String getWidthFromTileDimensionString (String dimension) {
        if(dimension == null) {
            return null;
        }
        String[] splitted = StringUtils.split(dimension, ',');
        if(splitted.length <= 0) {
            return null;
        }
        return splitted[0];
    }

    public static String getHeightFromTileDimensionString (String dimension) {
        if(dimension == null) {
            return null;
        }
        String[] splitted = StringUtils.split(dimension, ',');
        if(splitted.length <= 0) {
            return null;
        }
        if(splitted.length>1) {
            return splitted[1];
        }
        return splitted[0];
    }

    public static String getDimensionStringFromWidthAndHeight(String width, String height) {
        if (height == null || width == null) {
            return null;
        }
        if (height.compareTo(width) == 0) {
            return height;
        }
        return (width + "," + height);
    }

    public static boolean isValidDimension(String dimension) {
        if (dimension == null) {
            return false;
        }

        String[] splitted = StringUtils.split(dimension, ',');
        if(splitted.length>2 || splitted.length <= 0) {
            return false;
        }

        for (String dimensionSubstring : splitted) {
            if(!isValidDimensionSubstring(dimensionSubstring)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValidDimensionSubstring(String dimensionSubstring) {
        if (dimensionSubstring == null) {
            return false;
        }
        try{
            int width = Integer.parseInt(dimensionSubstring);
            if(width<=0) {
                return false;
            }
        } catch (NumberFormatException ex) {
            if(dimensionSubstring.compareTo("*") != 0) {
                return false;
            }
        }
        return true;
    }

}
