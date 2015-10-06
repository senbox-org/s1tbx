/*
 *
 *  * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.binning.operator;

import com.bc.ceres.core.VirtualDir;
import org.esa.snap.binning.TemporalBin;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A list implementation for storing the results of the temporal binning.
 * The implementation creates at most 100 temporary files to hold the results of the binning.
 */
class TemporalBinList extends AbstractList<TemporalBin> {

    public static final int DEFAULT_MAX_CACHE_FILES = 10000;
    public static final int DEFAULT_BINS_PER_FILE = 1000;

    private static final String FILE_NAME_PATTERN = "temporal-bins-%05d.tmp"; // at least 5 digits; zero padded
    private static final Logger logger = SystemUtils.LOG;


    private final long numberOfBins;
    private final int binsPerFile;
    private final List<TemporalBin> currentBinList;
    private final File tempDir;
    private int size;
    private int lastFileIndex;
    private boolean firstGet;

    public TemporalBinList(int numberOfBins) throws IOException {
        this(numberOfBins, DEFAULT_MAX_CACHE_FILES, DEFAULT_BINS_PER_FILE);
    }

    TemporalBinList(int numberOfBins, int maxNumberOfCacheFiles, int preferredBinsPerFile) throws IOException {
        tempDir = VirtualDir.createUniqueTempDir();
        Runtime.getRuntime().addShutdownHook(new DeleteDirThread(tempDir));
        this.numberOfBins = numberOfBins;
        binsPerFile = computeBinsPerFile(numberOfBins, maxNumberOfCacheFiles, preferredBinsPerFile);
        currentBinList = new LinkedList<TemporalBin>();
        size = 0;
        lastFileIndex = 0;
        firstGet = true;
    }

    @Override
    public boolean add(TemporalBin temporalBin) {
        if (size >= numberOfBins) {
            throw new IllegalStateException("Number of add operation exceeds maximum number of bins");
        }
        synchronized (currentBinList) {
            try {
                int currentFileIndex = calculateFileIndex(size);
                if (currentFileIndex != lastFileIndex) {
                    writeBinList(lastFileIndex, currentBinList);
                    currentBinList.clear();
                    readBinList(currentFileIndex, currentBinList);
                    lastFileIndex = currentFileIndex;
                }
                currentBinList.add(temporalBin);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error adding temporal bins.", e);
                return false;
            }
        }
        size++;
        return true;
    }

    @Override
    public TemporalBin get(int index) {
        if (index >= numberOfBins) {
            throw new IllegalStateException(String.format("Index out of range. Maximum is %d but was %d", numberOfBins - 1, index));
        }
        if (firstGet) {
            try {
                writeBinList(lastFileIndex, currentBinList);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error storing temporal bins.", e);
                return null;
            }finally {
                firstGet = false;
            }
        }
        synchronized (currentBinList) {
            try {
                int currentFileIndex = calculateFileIndex(index);
                if (currentFileIndex != lastFileIndex) {
                    currentBinList.clear();
                    readBinList(currentFileIndex, currentBinList);
                    lastFileIndex = currentFileIndex;
                }

                int fileBinOffset = binsPerFile * currentFileIndex;
                return currentBinList.get(index - fileBinOffset);

            } catch (IOException e) {
                logger.log(Level.SEVERE, String.format("Error getting temporal bin at index %d.", index), e);
            }
        }
        return null;
    }

    @Override
    public int size() {
        return size;
    }

    public void close() {
        FileUtils.deleteTree(tempDir);
    }

    static int computeBinsPerFile(int numberOfBins, int maxNumberOfCacheFiles, int preferredBinsPerFile) {
        int numCacheFiles = (int) Math.ceil(numberOfBins / (float) preferredBinsPerFile);
        numCacheFiles = Math.min(numCacheFiles, maxNumberOfCacheFiles);
        int binsPerFile = (int) Math.ceil(numberOfBins / (float) numCacheFiles);
        return binsPerFile < preferredBinsPerFile ? preferredBinsPerFile : binsPerFile;
    }


    private int calculateFileIndex(int index) {
        return index / binsPerFile;
    }

    private File getFile(int fileIndex) throws IOException {
        return new File(tempDir, String.format(FILE_NAME_PATTERN, fileIndex));
    }

    private void writeBinList(int currentFileIndex, List<TemporalBin> binList) throws IOException {
        File file = getFile(currentFileIndex);
        writeToFile(binList, file);
    }

    private void readBinList(int currentFileIndex, List<TemporalBin> binList) throws IOException {
        File file = getFile(currentFileIndex);
        if (file.exists()) {
            readFromFile(file, binList);
        }
    }

    private static void writeToFile(List<TemporalBin> temporalBins, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos, 5 * 1024 * 1024));
        try {
            for (TemporalBin bin : temporalBins) {
                dos.writeLong(bin.getIndex());
                bin.write(dos);
            }
        } finally {
            dos.close();
        }
    }

    private static void readFromFile(File file, List<TemporalBin> temporalBins) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        DataInputStream dis = new DataInputStream(new BufferedInputStream(fis, 5 * 1024 * 1024));
        try {
            while (dis.available() != 0) {
                long binIndex = dis.readLong();
                temporalBins.add(TemporalBin.read(binIndex, dis));
            }
        } finally {
            dis.close();
        }

    }
}
