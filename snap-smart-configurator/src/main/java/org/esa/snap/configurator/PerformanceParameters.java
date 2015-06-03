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


import org.esa.snap.framework.gpf.internal.OperatorExecutor;
import org.esa.snap.runtime.Config;
import org.esa.snap.util.SystemUtils;


import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Storage for performance parameters
 *
 * @author Nicolas Ducoin
 * @version $Revisions$ $Dates$
 */
public class PerformanceParameters {


    /**
     * Preferences key for the memory capacity of the JAI tile cache in megabytes
     */
    public static final String PROPERTY_KEY_JAI_TILE_CACHE_CAPACITY = "jai.tileCache.memoryCapacity";
    /**
     * Preferences key for the number of processors which may be employed for JAI image processing.
     */
    public static final String PROPERTY_KEY_JAI_PARALLELISM = "snap.jai.parallelism";


    private VMParameters vmParameters;
    private Path userDir;
    private int nbThreads;

    private int readerTileWidth;
    private int readerTileHeight;

    private int defaultTileSize;
    private int cacheSize;

    private boolean pixelGeoCodingFractionAccuracy;
    private boolean pixelGeoCodingUseTiling;
    private boolean useAlternatePixelGeoCoding;

    private OperatorExecutor.ExecutionOrder gpfExecutionOrder;
    private boolean gpfUseFileTileCache;
    private boolean gpfDisableTileCache;

    private static PerformanceParameters actualParameters = null;


    /**
     * Default constructor
     */
    public PerformanceParameters() {}

    /**
     * Cloning constructor
     *
     * @param clone the performance parameter to copy
     */
    public PerformanceParameters(PerformanceParameters clone) {
        this.setVMParameters(clone.vmParameters.toString());
        this.setUserDir(clone.getUserDir());

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


    public Path getUserDir() {
        return userDir;
    }

    public void setUserDir(Path largeTileCache) {
        this.userDir = largeTileCache;
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

    public int getReaderTileWidth() {
        return readerTileWidth;
    }

    public void setReaderTileWidth(int readerTileWidth) {
        this.readerTileWidth = readerTileWidth;
    }

    public int getReaderTileHeight() {
        return readerTileHeight;
    }

    public void setReaderTileHeight(int readerTileHeight) {
        this.readerTileHeight = readerTileHeight;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public boolean isPixelGeoCodingFractionAccuracy() {
        return pixelGeoCodingFractionAccuracy;
    }

    public void setPixelGeoCodingFractionAccuracy(boolean pixelGeoCodingFractionAccuracy) {
        this.pixelGeoCodingFractionAccuracy = pixelGeoCodingFractionAccuracy;
    }

    public boolean isPixelGeoCodingUseTiling() {
        return pixelGeoCodingUseTiling;
    }

    public void setPixelGeoCodingUseTiling(boolean pixelGeoCodingUseTiling) {
        this.pixelGeoCodingUseTiling = pixelGeoCodingUseTiling;
    }

    public boolean isUseAlternatePixelGeoCoding() {
        return useAlternatePixelGeoCoding;
    }

    public void setUseAlternatePixelGeoCoding(boolean useAlternatePixelGeoCoding) {
        this.useAlternatePixelGeoCoding = useAlternatePixelGeoCoding;
    }

    public OperatorExecutor.ExecutionOrder getGpfExecutionOrder() {
        return gpfExecutionOrder;
    }

    public void setGpfExecutionOrder(OperatorExecutor.ExecutionOrder gpfExecutionOrder) {
        this.gpfExecutionOrder = gpfExecutionOrder;
    }

    public boolean isGpfUseFileTileCache() {
        return gpfUseFileTileCache;
    }

    public void setGpfUseFileTileCache(boolean gpfUseFileTileCache) {
        this.gpfUseFileTileCache = gpfUseFileTileCache;
    }

    public boolean isGpfDisableTileCache() {
        return gpfDisableTileCache;
    }

    public void setGpfDisableTileCache(boolean gpfDisableTileCache) {
        this.gpfDisableTileCache = gpfDisableTileCache;
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

        actualParameters = new PerformanceParameters();

        VMParameters netBeansVmParameters = retreiveNBVMParameters();
        String vmParameters = preferences.get("default_options", netBeansVmParameters.toString());
        actualParameters.setVMParameters(vmParameters);
        actualParameters.setUserDir(configuration.userDir());
        actualParameters.setNbThreads(preferences.getInt("snap.parallelism", 1));

        actualParameters.setDefaultTileSize(preferences.getInt("snap.jai.defaultTileSize", 1));
        actualParameters.setReaderTileWidth(preferences.getInt("snap.dataio.reader.tileWidth", 1));
        actualParameters.setReaderTileHeight(preferences.getInt("snap.dataio.reader.tileHeight", 1));
        actualParameters.setCacheSize(preferences.getInt("snap.jai.tileCacheSize", 1));

        actualParameters.setPixelGeoCodingFractionAccuracy(preferences.getBoolean("snap.pixelGeoCoding.fractionAccuracy", false));
        actualParameters.setPixelGeoCodingUseTiling(preferences.getBoolean("snap.pixelGeoCoding.useTiling", true));
        actualParameters.setUseAlternatePixelGeoCoding(preferences.getBoolean("snap.useAlternatePixelGeoCoding", false));

        String executionOrder = preferences.get("snap.gpf.executionOrder", "SCHEDULE_ROW_COLUMN_BAND");
        actualParameters.setGpfExecutionOrder(OperatorExecutor.ExecutionOrder.valueOf(executionOrder));
        actualParameters.setGpfUseFileTileCache(preferences.getBoolean("snap.gpf.useFileTileCache", false));
        actualParameters.setGpfDisableTileCache(preferences.getBoolean("snap.gpf.disableTileCache", false));

        return actualParameters;
    }

    private static VMParameters retreiveNBVMParameters() {
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        List<String> vmParamsList = bean.getInputArguments();
        String[] vmParamsArray = new String[vmParamsList.size()];
        vmParamsList.toArray(vmParamsArray);

        return new VMParameters(vmParamsArray);
    }



    /**
     * Save the actual configuration
     *
     * @param confToSave The configuration to save
     */
    synchronized static void saveConfiguration(PerformanceParameters confToSave) throws IOException {
        Config configuration = Config.instance().load();
        Preferences preferences = configuration.preferences();

        preferences.put("default_options", confToSave.getVMParameters());

        String userDirString = confToSave.getUserDir().toString();

        String[] diskNames = JavaSystemInfos.getInstance().getDisksNames();
        if(Arrays.asList(diskNames).contains(userDirString)) {
            userDirString = userDirString + File.separator + "." + SystemUtils.getApplicationContextId();
            File contextFolderAsFile = new File(userDirString);
            contextFolderAsFile.createNewFile();
        }
        preferences.put("user.dir", userDirString);

        preferences.putInt("snap.parallelism", confToSave.getNbThreads());
        preferences.putInt("snap.jai.defaultTileSize", confToSave.getDefaultTileSize());
        preferences.putInt("snap.jai.tileCacheSize", confToSave.getCacheSize());

/* not implemented in this version.
        preferences.putInt("snap.dataio.reader.tileWidth", confToSave.getReaderTileWidth());
        preferences.putInt("snap.dataio.reader.tileHeight", confToSave.getReaderTileHeight());
        preferences.putBoolean("snap.pixelGeoCoding.fractionAccuracy", confToSave.isPixelGeoCodingFractionAccuracy());
        preferences.putBoolean("snap.pixelGeoCoding.ugetiling", confToSave.isPixelGeoCodingUseTiling());
        preferences.putBoolean("snap.useAlternatePixelGeoCoding", confToSave.isUseAlternatePixelGeoCoding());

        OperatorExecutor.ExecutionOrder executionOrderEnumVal = confToSave.getGpfExecutionOrder();
        preferences.put("snap.gpf.executionOrder", executionOrderEnumVal.toString());
        preferences.putBoolean("snap.gpf.useFileTileCache", confToSave.isGpfUseFileTileCache());
        preferences.putBoolean("snap.gpf.disableTileCache", confToSave.isGpfDisableTileCache());
 */
    }


    private static class VMParameters {

        private long vmXMX = 0;
        private long vmXMS = 0;
        private String otherVMOptions;

        public long getVmXMX() {
            return vmXMX;
        }

        public void setVmXMX(long vmXMX) {
            this.vmXMX = vmXMX;
        }

        public long getVmXMS() {
            return vmXMS;
        }

        public void setVmXMS(long vmXMS) {
            this.vmXMS = vmXMS;
        }

        public String getOtherVMOptions() {
            return otherVMOptions;
        }

        public void setOtherVMOptions(String otherVMOptions) {
            this.otherVMOptions = otherVMOptions;
        }

        public VMParameters(String vmParametersString) {
            if (vmParametersString != null) {
                String[] stringArray = vmParametersString.split(" ");
                fromStringArray(stringArray);
            }
        }

        public VMParameters(String[] vmParametersStringArray){
            fromStringArray(vmParametersStringArray);
        }

        public void fromStringArray(String[] vmParametersStringArray) {
            String otherVMParams = "";
            for (String thisArg : vmParametersStringArray) {

                if (thisArg != null) {
                    if (thisArg.startsWith("-Xmx")) {
                        try {
                            setVmXMX(getMemVmSettingValue(thisArg));
                        } catch (NumberFormatException ex) {
                            SystemUtils.LOG.warning("VM Parameters, bad XMX: " + thisArg);
                        }
                    } else if (thisArg.startsWith("-Xms")) {
                        try {
                            setVmXMS(getMemVmSettingValue(thisArg));
                        } catch (NumberFormatException ex) {
                            SystemUtils.LOG.warning("VM Parameters, bad XMS: " + thisArg);
                        }
                    } else {
                        otherVMParams += thisArg + " ";
                    }
                }
                setOtherVMOptions(otherVMParams);
            }
        }


        private int getMemVmSettingValue(String vmStringSetting) throws NumberFormatException{

            String memStringValue = vmStringSetting.substring(4);
            float multValue;
            if(memStringValue.endsWith("g") || memStringValue.endsWith("G")) {
                multValue = 1024;
                memStringValue = memStringValue.substring(0, memStringValue.length()-1);
            } else if(memStringValue.endsWith("m") || memStringValue.endsWith("M")) {
                multValue = 1;
                memStringValue = memStringValue.substring(0, memStringValue.length()-1);
            } else if(memStringValue.endsWith("k") || memStringValue.endsWith("K")) {
                multValue = 1/1024;
                memStringValue = memStringValue.substring(0, memStringValue.length()-1);
            } else {
                multValue = 1/(1024*1024);
            }

            return Math.round(Integer.parseInt(memStringValue) * multValue);
        }


        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if(getVmXMX() != 0) {
                builder.append(" -Xmx");
                builder.append(getVmXMX());
                builder.append("m ");
            }
            if(getVmXMS() != 0) {
                builder.append(" -Xms");
                builder.append(getVmXMS());
                builder.append("m ");
            }
            if(getOtherVMOptions() != null) {
                builder.append(getOtherVMOptions());
            }
            return builder.toString();
        }
    }
}
