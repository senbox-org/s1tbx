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
import org.esa.snap.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * Compute optimum parameters
 *
 * @author Nicolas Ducoin
 * @version $Revisions$ $Dates$
 */
public class ConfigurationOptimizer {


    // Free space available for other applications in MegaBytes
    private static long DEFAULT_MIN_FREE_RAM = 512;

    // defalut minumal XMS if enough memory is available in MegaBytes.
    private static long DEFAULT_MIN_XMS = 2048;

    // Minimum free space on a disk for the java temporary files
    // (as JAI cache), in MegaByte
    private static long MIN_FREE_TMP_DISK_SPACE = 512;

    // Minimum free space on a disk for the large cache, in MegaByte
    private static long MIN_FREE_LARGE_CACHE_DISK_SPACE = 1024;

    // The minimum peformance increase to propose a change of parameters
    private static int DISK_MIN_SPEED_INCREASE = 20;

    // System information: ram, disks, cpus, etc.
    SystemInfos sysInfos = null;

    private PerformanceParameters actualPerformanceParameters = null;
    private PerformanceParameters custommisedPerformanceParameters = null;

    /**
     * Default constructor. Performs initialisations.
     */
    public ConfigurationOptimizer() {
        sysInfos = JavaSystemInfos.getInstance();
    }


    public PerformanceParameters getActualPerformanceParameters() {
        if(actualPerformanceParameters == null) {
            actualPerformanceParameters = PerformanceParameters.loadConfiguration();
        }
        return actualPerformanceParameters;
    }

    /**
     * Computes all optimised system parameters
     */
    public PerformanceParameters computeOptimisedSystemParameters() {

        // we reset optimized values to actual values
        PerformanceParameters optimisedPerformanceParameters =
                new PerformanceParameters(actualPerformanceParameters);

        computeOptimisedRAMParams(optimisedPerformanceParameters);
        computeOptimisedPathParams(optimisedPerformanceParameters);

        return optimisedPerformanceParameters;
    }

    public void updateCustomisedParameters(PerformanceParameters updatedParams) {
        custommisedPerformanceParameters = updatedParams;
    }

    public void saveCustomisedParameters() throws IOException {
        PerformanceParameters.saveConfiguration(custommisedPerformanceParameters);
        actualPerformanceParameters = new PerformanceParameters(custommisedPerformanceParameters);
    }

    /**
     * Compute the optimum JVM ram parameters (Xms & Xmx)
     *
     * Xmx:
     * If the actual free ram is more than DEFAULT_MIN_FREE_RAM we try to keep
     * this free ram, otherwise we set Xmx to use all the ram.
     *
     * Xms:
     * If Xmx is bigger than DEFAULT_MIN_XMS we se Xms to DEFAULT_MIN_XMS
     * Otherwise we set Xms = Xmx
     *
     * @param performanceParameters the performance parameters to optimize, the object is updated
     *
     */
    private void computeOptimisedRAMParams(PerformanceParameters performanceParameters) {
        long freeRAM = sysInfos.getFreeRAM();
        long thisAppRam = sysInfos.getThisAppRam();

        long optisedJVMMem;
        if(freeRAM > DEFAULT_MIN_FREE_RAM) {
            optisedJVMMem = thisAppRam + freeRAM - DEFAULT_MIN_FREE_RAM;
        } else {
            optisedJVMMem = thisAppRam + freeRAM;
        }
        performanceParameters.setVmXMX(optisedJVMMem);

        if(optisedJVMMem > DEFAULT_MIN_XMS) {
            performanceParameters.setVmXMS(DEFAULT_MIN_XMS);
        } else {
            performanceParameters.setVmXMS(optisedJVMMem);
        }
    }

    /**
     * Compute the optimised path parameters.
     * These depends on disk speed and disk free space.
     *
     * @param performanceParameters the performance parameters to optimize, the object is updated
     */
    private void computeOptimisedPathParams(PerformanceParameters performanceParameters) {
        String[] disks = sysInfos.getDisksNames();

        double fastestForTmpSpeed = 0;
        Path fastestForUserDir = null;
        double fastestForUserDirSpeed = 0;

        for(String diskName : disks) {
            try {
                double writeSpeed = sysInfos.getDiskWriteSpeed(diskName);
                if(writeSpeed > fastestForTmpSpeed &&
                        sysInfos.getDiskFreeSize(diskName) > MIN_FREE_TMP_DISK_SPACE) {
                    fastestForTmpSpeed = writeSpeed;
                }
                if(writeSpeed > fastestForUserDirSpeed &&
                        sysInfos.getDiskFreeSize(diskName) > MIN_FREE_LARGE_CACHE_DISK_SPACE) {
                    fastestForUserDirSpeed = writeSpeed;
                    File diskNameAsFile = new File(diskName);
                    fastestForUserDir = FileUtils.getPathFromURI(diskNameAsFile.toURI());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



        if(fastestForUserDir != null) {
            String actualLargeCacheDir = actualPerformanceParameters.getUserDir().toString();
            DiskBenchmarker benchmarker = new DiskBenchmarker(actualLargeCacheDir);
            try {
                double userDirWriteSpeed = benchmarker.getWriteSpeed();
                double minSpeedToChange = userDirWriteSpeed * (1 + DISK_MIN_SPEED_INCREASE / 100);
                if(minSpeedToChange < fastestForUserDirSpeed) {
                    performanceParameters.setUserDir(fastestForUserDir);
                }
            } catch (IOException e) {
                // we could check actual large cache speed, so we change directory
                performanceParameters.setUserDir(fastestForUserDir);

                SystemUtils.LOG.warning(
                        "Could not check performance of large cache dir: " +
                                actualPerformanceParameters.getUserDir() +
                                " error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
