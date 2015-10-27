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

import org.esa.snap.core.util.SystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

/**
 * Performing benchmarks on a particular disk.
 *
 * This is based on reading and writing a file of a particular size on the disk. The file size and the number of times
 * the file will be written and redden can be changed.
 *
 * This supposes to have write access to the disk, otherwise an IOException is raised
 *
 * @author Nicolas Ducoin
 * @version $Revisions$ $Dates$
 */
public class DiskBenchmarker {

    /**
     * Default file sise in MB
     */
    public static final int DEFAULT_FILE_SIZE = 5;

    /**
     * Default number of samples the
     */
    public static final int DEFAULT_NB_SAMPLES = 4;


    private String dirName;
    private int fileSize;
    private int nbSamples;
    private double readSpeed = 0;
    private double writeSpeed = 0;

    /**
     * Number of milliseconds in a second...
     */
    private static final double MILLISEC_IN_SEC = 1000;


    /**
     * Constructor with default file size and default number of samples.
     *
     * @param dirName the size of the file than will be copied and read
     */
    public DiskBenchmarker(String dirName) {
        this(dirName, DEFAULT_FILE_SIZE, DEFAULT_NB_SAMPLES);
    }

    /**
     * Full constructor, set up de bechchmark caracteristics.
     *
     * @param dirName The directory name, where the files will be copied and read
     * @param fileSize the size of the file than will be copied and read, in Mega Bytes
     * @param nbSamples the number of times the file will be copied and read
     */
    public DiskBenchmarker(String dirName,
                           int fileSize,
                           int nbSamples) {
        this.fileSize = fileSize;
        this.nbSamples = nbSamples;
        this.dirName = dirName;
    }

    /**
     * Performs the bechmark if speed it 0, then returns the read speed.
     *
     * @return the read speed in Bytes per seconds
     * @throws IOException if reading or writing to dirName failed
     */
    public double getReadSpeed() throws IOException {
        if(0 == readSpeed) {
            performBenchMark();
        }
        return readSpeed;
    }

    /**
     * Performs the bechmark if speed it 0, then returns the write speed
     *
     * @return the write speed in Bytes per seconds
     * @throws IOException if reading or writing to dirName failed
     */
    public double getWriteSpeed() throws IOException {
        if(0 == writeSpeed) {
            performBenchMark();
        }
        return writeSpeed;
    }

    /**
     * Peforms the effective benchmark
     * @throws IOException if reading or writing to dirName failed
     */
    public void performBenchMark() throws IOException {
        String uniqueName = UUID.randomUUID().toString();
        try {
            this.writeSpeed = computeWriteSpeed(uniqueName, dirName, fileSize, nbSamples);
            this.readSpeed = computeReadSpeed(uniqueName, dirName, fileSize, nbSamples);
        } catch (IOException ex) {
            // could not write or read to the output
            // we log an info message and continue
            SystemUtils.LOG.warning("Could not read or write on target " + dirName);
            this.writeSpeed = -1;
            this.readSpeed = -1;
        } finally {
            cleanup(uniqueName, dirName, nbSamples);
        }
    }

    /**
     *
     * Compute the write speed, by writing nbSamples times the file to the destination
     *
     * @param uniqueName a unique string (the files shouldn't exist)
     * @param dirName The directory name, where the files will be copied and read
     * @param fileSize the size of the file than will be copied and read, in Mega Bytes
     * @param nbSamples the number of times the file will be copied and read
     * @return the write speed in Bytes per seconds
     */
    private static double computeWriteSpeed(String uniqueName, String dirName, int fileSize, int nbSamples) throws IOException {
        byte[] dataToWrite = new byte[fileSize * 1024 * 1024];
        new Random().nextBytes(dataToWrite);

        long startTime = System.currentTimeMillis();

        for(int i=0 ; i<nbSamples ; i++) {
            String fileName = dirName + File.separator + uniqueName + i;
            FileOutputStream outputStream = new FileOutputStream(fileName);
            outputStream.write(dataToWrite);
            outputStream.close();
        }

        long endTime = System.currentTimeMillis();
        long writeTimeMili = endTime - startTime;
        double writeTimeSec = writeTimeMili / MILLISEC_IN_SEC;
        long totalSize = fileSize*nbSamples;

        return totalSize / writeTimeSec;
    }

    /**
     *
     * Compute the read speed, by reading nbSamples times a file on the destination
     *
     * @param uniqueName a unique name, same the one passed in computeWriteTime
     * @param dirName The directory name, where the files will be copied and read
     * @param fileSize the size of the file than will be copied and read, in Mega Bytes
     * @param nbSamples the number of times the file will be copied and read
     * @return the read speed in Bytes per seconds
     */
    private static double computeReadSpeed(String uniqueName,
                                           String dirName,
                                           int fileSize,
                                           int nbSamples) throws IOException {
        long startTime = System.currentTimeMillis();

        int numberOfBytesToRead = fileSize * 1024 * 1024;
        byte[] dataToRead = new byte[numberOfBytesToRead];
        for (int i = 0; i < nbSamples ; i++) {
            String fileName = dirName + File.separator + uniqueName + i;
            FileInputStream inputStream = new FileInputStream(fileName);
            int readResult = inputStream.read(dataToRead);

            if(readResult != numberOfBytesToRead) {
                SystemUtils.LOG.warning("Did not all of " + fileName);
            }

            inputStream.close();
        }

        long endTime = System.currentTimeMillis();
        long readTimeMilliSec = endTime - startTime;

        double readTimeSec = readTimeMilliSec / MILLISEC_IN_SEC;
        long totalSize = fileSize*nbSamples;

        return totalSize / readTimeSec;
    }

    /**
     *
     * Suppress all files created by computeWriteSpeed
     *
     * @param uniqueName the unique name base of the file name
     * @param dirName the directory where the files are created
     * @param nbSamples the number of files created
     * @throws IOException
     */
    private static void cleanup(String uniqueName, String dirName, int nbSamples) throws IOException {

        for (int i = 0; i < nbSamples ; i++) {
            String fileName = dirName + File.separator + uniqueName + i;
            File file = new File(fileName);
            if(!file.delete()) {
                SystemUtils.LOG.warning("Could not delete temporary benchmark file " + fileName);
            }
        }
    }
}
