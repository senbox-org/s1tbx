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


import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.EngineConfig;
import org.esa.snap.util.SystemUtils;
import org.esa.snap.util.io.FileUtils;


import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Path;
import java.util.List;
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

    /**
     * Preferences key for the number of processors which may be employed for JAI image processing.
     */
    public static final String PROPERTY_JAI_PARALLELISM = "snap.jai.parallelism";


    private VMParameters vmParameters;
    private Path userDir;
    private int nbThreads;

    private int defaultTileSize;
    private int cacheSize;

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

        VMParameters netBeansVmParameters = retreiveNBVMParameters();
        String vmParameters = preferences.get("default_options", netBeansVmParameters.toString());
        actualParameters.setVMParameters(vmParameters);
        actualParameters.setUserDir(configuration.userDir());

        final int defaultNbThreads = JavaSystemInfos.getInstance().getNbCPUs();
        actualParameters.setNbThreads(preferences.getInt(PROPERTY_JAI_PARALLELISM, defaultNbThreads));
        actualParameters.setDefaultTileSize(preferences.getInt(PROPERTY_DEFAULT_TILE_SIZE, 0));
        actualParameters.setCacheSize(preferences.getInt(PROPERTY_JAI_CACHE_SIZE, 0));

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
    synchronized static void saveConfiguration(PerformanceParameters confToSave) throws IOException, BackingStoreException {
        Config configuration = EngineConfig.instance().load();
        Preferences preferences = configuration.preferences();

        preferences.put("default_options", confToSave.getVMParameters());

        String userDirString = confToSave.getUserDir().toString();

        String[] diskNames = JavaSystemInfos.getInstance().getDisksNames();
        for(String diskName : diskNames) {
            if (diskName.equalsIgnoreCase(userDirString)) {
                userDirString = userDirString + File.separator + "." + SystemUtils.getApplicationContextId();
                File contextFolderAsFile = new File(userDirString);
                if(!contextFolderAsFile.mkdir()) {
                    SystemUtils.LOG.severe("Could not create user dir " + userDirString);
                } else {
                    confToSave.setUserDir(FileUtils.getPathFromURI(contextFolderAsFile.toURI()));
                }
                break;
            }
        }
        preferences.put(EngineConfig.PROPERTY_USER_DIR, userDirString);

        preferences.putInt(PROPERTY_JAI_PARALLELISM, confToSave.getNbThreads());
        preferences.putInt(PROPERTY_DEFAULT_TILE_SIZE, confToSave.getDefaultTileSize());
        preferences.putInt(PROPERTY_JAI_CACHE_SIZE, confToSave.getCacheSize());
        preferences.flush();
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
