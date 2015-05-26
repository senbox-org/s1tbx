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

import javax.media.jai.JAI;
import javax.media.jai.TileCache;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

/**
 * Storage for performance parameters
 *
 * @author Nicolas Ducoin
 * @version $Revisions$ $Dates$
 */
public class PerformanceParameters {

    private VMParameters vmParameters;

    private String vmTmpDir;
    private String auxDataPath;
    private String userDir;

    private long tileCacheCapacity;
    private long tileSize;
    private int nbThreads;

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
        this.setVmTmpDir(clone.vmTmpDir);
        this.setAuxDataPath(clone.auxDataPath);
        this.setVMParameters(clone.vmParameters.toString());
        this.setUserDir(clone.userDir);
    }

    public void setVmXMX(long vmXMX) {
        vmParameters.setVmXMX(vmXMX);
    }

    public void setVmXMS(long vmXMS) {
        vmParameters.setVmXMS(vmXMS);
    }

    public String getVmTmpDir() {
        return vmTmpDir;
    }

    public void setVmTmpDir(String vmTmpDir) {
        this.vmTmpDir = vmTmpDir;
    }


    public void setAuxDataPath(String auxDataPath) {
        this.auxDataPath = auxDataPath;
    }

    public String getUserDir() {
        return userDir;
    }

    public void setUserDir(String largeTileCache) {
        this.userDir = largeTileCache;
    }

    public long getTileCacheCapacity() {
        return tileCacheCapacity;
    }

    public void setTileCacheCapacity(long cacheSize) {
        this.tileCacheCapacity = cacheSize;
    }

    public long getTileSize() {
        return tileSize;
    }

    public void setTileSize(long tileSize) {
        this.tileSize = tileSize;
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


    /**
     *
     * Reads the parameters files and system settings to retreive the actual performance parameters.
     *
     * @return the actual performance parameters
     */
    synchronized static PerformanceParameters loadConfiguration() {

        if(actualParameters == null) {
            actualParameters = new PerformanceParameters();

            actualParameters.setVmTmpDir(System.getProperty("java.io.tmpdir"));
            actualParameters.setUserDir(System.getProperty("user.dir"));
            actualParameters.vmParameters = retreiveVMParameters();

            TileCache tileCache = JAI.getDefaultInstance().getTileCache();
            long memoryCapacity = tileCache.getMemoryCapacity();
            actualParameters.setTileCacheCapacity(memoryCapacity);
            actualParameters.setTileSize(128);
            actualParameters.setNbThreads(2);
        }

        return actualParameters;
    }

    private static VMParameters retreiveVMParameters() {
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
    synchronized static void saveConfiguration(PerformanceParameters confToSave) {
        //TODO save the configuration..
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
                    } else if (thisArg.equalsIgnoreCase("-Xms")) {
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
