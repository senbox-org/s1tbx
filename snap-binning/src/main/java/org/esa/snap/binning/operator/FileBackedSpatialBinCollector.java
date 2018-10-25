/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning.operator;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.VirtualDir;
import org.esa.snap.binning.BinningContext;
import org.esa.snap.binning.SpatialBin;
import org.esa.snap.core.util.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An implementation of {@link SpatialBinCollector} which stores the consumed
 * {@link SpatialBin spatial bins} into multiple files.
 *
 * @see MapBackedSpatialBinCollector
 * @see GeneralSpatialBinCollector
 */
class FileBackedSpatialBinCollector implements SpatialBinCollector {

    private static final int DEFAULT_NUM_BINS_PER_FILE = 100000;
    private static final int MAX_NUMBER_OF_CACHE_FILES = 10000;
    private static final String FILE_NAME_PATTERN = "bins-%05d.tmp"; // at least 5 digits; zero padded

    private final int numBinsPerFile;
    private final List<SpatialBin> binList;
    private final AtomicBoolean consumingCompleted;
    private final File tempDir;
    private int currentFileIndex;
    private long numBinsComsumed;

    FileBackedSpatialBinCollector(long maximumNumberOfBins) throws IOException {
        Assert.argument(maximumNumberOfBins > 0, "maximumNumberOfBins > 0");
        numBinsPerFile = getNumBinsPerFile(maximumNumberOfBins);
        tempDir = VirtualDir.createUniqueTempDir();
        Runtime.getRuntime().addShutdownHook(new DeleteDirThread(tempDir));
        binList = new ArrayList<>();
        consumingCompleted = new AtomicBoolean(false);
        currentFileIndex = 0;
        numBinsComsumed = 0;
    }

    @Override
    public void consumeSpatialBins(BinningContext ignored, List<SpatialBin> spatialBins) throws Exception {
        if (consumingCompleted.get()) {
            throw new IllegalStateException("Consuming of bins has already been completed.");
        }
        synchronized (binList) {
            for (SpatialBin spatialBin : spatialBins) {
                numBinsComsumed++;
                long spatialBinIndex = spatialBin.getIndex();
                int nextFileIndex = calculateNextFileIndex(spatialBinIndex);
                if (nextFileIndex != currentFileIndex) {
                    // write map back to file, if it contains data
                    writeListToFile(currentFileIndex);
                    currentFileIndex = nextFileIndex;
                }
                binList.add(spatialBin);
            }
        }
    }

    @Override
    public void consumingCompleted() throws IOException {
        consumingCompleted.set(true);
        synchronized (binList) {
            writeListToFile(currentFileIndex);
        }
    }

    @Override
    public SpatialBinCollection getSpatialBinCollection() throws IOException {
        List<File> cacheFiles = getCacheFiles(tempDir);
        return new FileBackedBinCollection(cacheFiles, numBinsComsumed);
    }

    public void close() {
        FileUtils.deleteTree(tempDir);
    }

    static void writeToStream(List<SpatialBin> spatialBins, DataOutputStream dos) throws IOException {
        for (SpatialBin spatialBin : spatialBins) {
            dos.writeLong(spatialBin.getIndex());
            spatialBin.write(dos);
        }
    }

    static void readFromStream(DataInputStream dis, SortedMap<Long, List<SpatialBin>> map) throws IOException {
        while (true) {
            try {
                long binIndex = dis.readLong();
                List<SpatialBin> spatialBins = map.get(binIndex);
                if (spatialBins == null) {
                    spatialBins = new ArrayList<>();
                    map.put(binIndex, spatialBins);
                }
                spatialBins.add(SpatialBin.read(binIndex, dis));
            } catch (EOFException eof) {
                return;
            }
        }
    }

    private static int getNumBinsPerFile(long maxBinCount) {
        int numCacheFiles = (int) Math.ceil(maxBinCount / (float) DEFAULT_NUM_BINS_PER_FILE);
        numCacheFiles = Math.min(numCacheFiles, MAX_NUMBER_OF_CACHE_FILES);
        int binsPerFile = (int) Math.ceil(maxBinCount / (float) numCacheFiles);
        return Math.max(DEFAULT_NUM_BINS_PER_FILE, binsPerFile);
    }

    private void writeListToFile(int fileIndex) throws IOException {
        if (!binList.isEmpty()) {
            File file = getFile(fileIndex);
            writeToFile(binList, file);
            binList.clear();
        }
    }

    private void writeToFile(List<SpatialBin> spatialBins, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file, true);
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos, 5 * 1024 * 1024))) {
            writeToStream(spatialBins, dos);
        }
    }

    private static void readIntoMap(File file, SortedMap<Long, List<SpatialBin>> map) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(fis, 5 * 1024 * 1024))) {
            readFromStream(dis, map);
        }
    }

    private File getFile(int fileIndex) throws IOException {
        return new File(tempDir, String.format(FILE_NAME_PATTERN, fileIndex));
    }

    private int calculateNextFileIndex(long binIndex) {
        return (int) (binIndex / numBinsPerFile);
    }

    private static List<File> getCacheFiles(File cacheFileDir) {
        File[] files = cacheFileDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                String fileName = file.getName();
                return file.isFile() && fileName.startsWith("bins-") && fileName.endsWith(".tmp");
            }
        });
        if (files == null) {
            return Collections.emptyList();
        }
        Arrays.sort(files);
        List<File> fileList = new ArrayList<>(files.length);
        Collections.addAll(fileList, files);
        return fileList;
    }

    private static class FileBackedBinCollection implements SpatialBinCollection {

        private final List<File> cacheFiles;
        private final long size;

        private FileBackedBinCollection(List<File> cacheFiles, long size) {
            this.cacheFiles = cacheFiles;
            this.size = size;
        }

        @Override
        public Iterable<List<SpatialBin>> getBinCollection() {
            return new Iterable<List<SpatialBin>>() {
                @Override
                public Iterator<List<SpatialBin>> iterator() {
                    return new FileBackedBinIterator(cacheFiles.iterator());
                }
            };
        }

        @Override
        public long size() {
            return size;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    private static class FileBackedBinIterator implements Iterator<List<SpatialBin>> {

        private final Iterator<File> binFiles;
        private Iterator<List<SpatialBin>> binIterator;

        private FileBackedBinIterator(Iterator<File> binFiles) {
            this.binFiles = binFiles;
        }

        @Override
        public boolean hasNext() {
            return iteratorHasBins() || binFiles.hasNext();
        }

        @Override
        public List<SpatialBin> next() {
            if (!iteratorHasBins()) {
                File currentFile = binFiles.next();
                if (currentFile.exists()) {
                    final SortedMap<Long, List<SpatialBin>> map = new TreeMap<>();
                    try {
                        readIntoMap(currentFile, map);
                    } catch (IOException e) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                    if (!currentFile.delete()) {
                        currentFile.deleteOnExit();
                    }
                    binIterator = map.values().iterator();
                }
            }
            return binIterator.next();
        }

        private boolean iteratorHasBins() {
            return binIterator != null && binIterator.hasNext();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
