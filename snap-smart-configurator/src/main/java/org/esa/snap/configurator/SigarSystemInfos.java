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
import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Iterface for system information retreival, such as RAM, CPUs, disks, etc.
 *
 * @author Nicolas Ducoin
 * @version $Revisions$ $Dates$
 */
public class SigarSystemInfos implements SystemInfos {


    private static SystemInfos theInstance = null;

    private Sigar sigar;
    private int nbCPUs = 0;
    private long ram = 0;
    private String[] dirNames = null;
    private Map<String, DiskBenchmarker> disksBenchMarkers = null;
    private int benchmarkFileSize = 0;
    private int benchmarkNbSamples = 0;

    /**
     * Constructor is private, this is class is a singleton. This way it not to recompute all information each time we
     * need an instance of SigarSystemInfos.
     */
    private SigarSystemInfos(int benchmarkFileSize, int benchmarkNbSamples) {
        this.benchmarkNbSamples = benchmarkNbSamples;
        this.benchmarkFileSize = benchmarkFileSize;


        updateSystemInfos();
    }


    /**
     * Retreive or compute all required informations, then returns an instance of SystemInfos
     * @param benchmarkFileSize the size of the file used to perform bechmarks, in MegaByte
     * @param benchmarkNbSamples the number of samples used to perform benchmarks
     * @return an instance of SigarSystemInfos
     */
    public static SystemInfos getInstance(int benchmarkFileSize, int benchmarkNbSamples) {


        if(theInstance == null) {
            theInstance = new SigarSystemInfos(benchmarkFileSize, benchmarkNbSamples);
        }


        return theInstance;
    }

    /**
     * Retreive or compute all required informations, then returns an instance of SystemInfos
     *
     * @return an instance of SigarSystemInfos
     */
    public static SystemInfos getInstance() {

        if(theInstance == null) {
            theInstance = new SigarSystemInfos(0, 0);
        }

        return theInstance;
    }

    /**
     * get or compute the System informations
     */
    private void updateSystemInfos() {
        sigar = new Sigar();
        try {
            Cpu[] cpuList = sigar.getCpuList();
            nbCPUs = cpuList.length;

            Map<String, FileSystem> fileSystems = sigar.getFileSystemMap();
            Set<String> keySet = fileSystems.keySet();

            dirNames = new String[keySet.size()];
            keySet.toArray(dirNames);

            disksBenchMarkers = new HashMap<>();
            for(String dirName : dirNames){
                DiskBenchmarker diskBenchmarker = new DiskBenchmarker(dirName);
                if(benchmarkFileSize != 0 && benchmarkNbSamples != 0 ){
                    diskBenchmarker.setFileSize(benchmarkFileSize);
                    diskBenchmarker.setNbSamples(benchmarkNbSamples);
                }
                disksBenchMarkers.put(dirName,diskBenchmarker);
            }

            Mem mem = sigar.getMem();
            ram = mem.getRam();


        } catch (SigarException e) {
            SystemUtils.LOG.severe(e.getMessage());
        }
    }

    @Override
    public int getNbCPUs() {
        return nbCPUs;
    }

    @Override
    public long getRAM() {
        return ram;
    }

    @Override
    public long getFreeRAM() {
        long freeRAM = 0;
        try {
            freeRAM = sigar.getMem().getActualFree();
        } catch (SigarException e) {
            SystemUtils.LOG.severe(e.getMessage());
        }

        return freeRAM;
    }

    @Override
    public long getThisAppRam() {
        long thisAppMem = 0;
        try {
            long thisPid = sigar.getPid();
            thisAppMem = sigar.getProcMem(thisPid).getSize();
        } catch (SigarException e) {
            e.printStackTrace();
        }
        return thisAppMem;
    }

    @Override
    public String[] getDisksNames() {
        return dirNames;
    }


    @Override
    public long getDiskFreeSize(String diskId) {
        long diskFreeSizeInMB = 0;
        try {
            long diskFreeSizeInKiloByte = sigar.getFileSystemUsage(diskId).getFree();
            diskFreeSizeInMB = diskFreeSizeInKiloByte / 1024;
        } catch (SigarException e) {
            e.printStackTrace();
        }
        return diskFreeSizeInMB;
    }

    @Override
    public double getDiskWriteSpeed(String diskId) throws IOException {
        DiskBenchmarker benchmarker = disksBenchMarkers.get(diskId);
        return benchmarker.getWriteSpeed();
    }

    @Override
    public double getDiskReadSpeed(String diskId) throws IOException {
        DiskBenchmarker benchmarker = disksBenchMarkers.get(diskId);
        return benchmarker.getReadSpeed();
    }
}
