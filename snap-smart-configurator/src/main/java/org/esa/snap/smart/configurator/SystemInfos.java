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


import java.io.IOException;

/**
 * Iterface for system information retreival, such as RAM, CPUs, disks, etc.
 *
 * @author Nicolas Ducoin
 * @version $Revisions$ $Dates$
 */
public interface SystemInfos {

    /**
     *
     * @return The number of CPUs for this machine
     */
    int getNbCPUs();

    /**
     *
     * @return The size of the RAM for this machine in MB
     */
    long getRAM();


    /**
     *
     * @return The amount of unused memory for this machine in MB
     */
    long getFreeRAM();


    /**
     *
     * @return the RAM currently reserved by SNAP
     */
    long getReservedRam();


    /**
     *
     * @return The name of the disks, it should uniquely identify the disks
     */
    String[] getDisksNames();


    /**
     *
     * @param diskId The disk id, one of the string returned by getDisks()
     * @return The free size of the disk, in MegaBytes
     */
    long getDiskFreeSize(String diskId);

    /**
     *
     * @param diskId The disk id, one of the string returned by getDisks()
     * @return The write speed ot the disk in MegaBytes per seconds
     */
    double getDiskWriteSpeed(String diskId) throws IOException;

    /**
     *
     * @param diskId The disk id, one of the string returned by getDisks()
     * @return The read speed ot the disk in MegaBytes per seconds
     */
    double getDiskReadSpeed(String diskId) throws IOException;

}
