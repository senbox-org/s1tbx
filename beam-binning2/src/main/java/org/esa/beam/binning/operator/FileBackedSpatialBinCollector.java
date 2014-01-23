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

package org.esa.beam.binning.operator;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.VirtualDir;
import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.SpatialBin;
import org.esa.beam.util.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
    private final SortedMap<Long, List<SpatialBin>> map;
    private final AtomicBoolean consumingCompleted;
    private final File tempDir;
    private long currentFileIndex;
    private long numBinsComsumed;

    public FileBackedSpatialBinCollector(long maximumNumberOfBins) throws IOException {
        Assert.argument(maximumNumberOfBins > 0, "maximumNumberOfBins > 0");
        numBinsPerFile = getNumBinsPerFile(maximumNumberOfBins);
        tempDir = VirtualDir.createUniqueTempDir();
        Runtime.getRuntime().addShutdownHook(new DeleteDirThread(tempDir));
        map = new TreeMap<Long, List<SpatialBin>>();
        consumingCompleted = new AtomicBoolean(false);
        currentFileIndex = 0;
        numBinsComsumed = 0;
    }

    @Override
    public void consumeSpatialBins(BinningContext ignored, List<SpatialBin> spatialBins) throws Exception {
        if (consumingCompleted.get()) {
            throw new IllegalStateException("Consuming of bins has already been completed.");
        }
        synchronized (map) {
            for (SpatialBin spatialBin : spatialBins) {
                numBinsComsumed++;
                long spatialBinIndex = spatialBin.getIndex();
                int nextFileIndex = calculateNextFileIndex(spatialBinIndex);
                if (nextFileIndex != currentFileIndex) {
                    // write map back to file, if it contains data
                    writeMapToFile(currentFileIndex);
                    readFromFile(nextFileIndex);
                    currentFileIndex = nextFileIndex;
                }
                List<SpatialBin> spatialBinList = map.get(spatialBinIndex);
                if (spatialBinList == null) {
                    spatialBinList = new ArrayList<SpatialBin>();
                    map.put(spatialBinIndex, spatialBinList);
                }
                spatialBinList.add(spatialBin);
            }
        }
    }

    @Override
    public void consumingCompleted() throws IOException {
        consumingCompleted.set(true);
        synchronized (map) {
            writeMapToFile(currentFileIndex);
        }
    }

    @Override
    public SpatialBinCollection getSpatialBinCollection() throws IOException {
        return new FileBackedBinCollection(numBinsComsumed);
    }

    public void close() {
        FileUtils.deleteTree(tempDir);
    }

    static void writeToStream(SortedMap<Long, List<SpatialBin>> map, DataOutputStream dos) throws IOException {
        for (Map.Entry<Long, List<SpatialBin>> entry : map.entrySet()) {
            dos.writeLong(entry.getKey());
            List<SpatialBin> binList = entry.getValue();
            dos.writeInt(binList.size());
            for (SpatialBin spatialBin : binList) {
                spatialBin.write(dos);
            }
        }
    }

    static void readFromStream(DataInputStream dis, SortedMap<Long, List<SpatialBin>> map) throws IOException {
        while (dis.available() != 0) {
            long binIndex = dis.readLong();
            int numBins = dis.readInt();
            List<SpatialBin> spatialBins = map.get(binIndex);
            if (spatialBins == null) {
                spatialBins = new ArrayList<SpatialBin>(numBins);
            }
            for (int i = numBins; i > 0; i--) {
                spatialBins.add(SpatialBin.read(binIndex, dis));
            }
            map.put(binIndex, spatialBins);
        }
    }

    private static int getNumBinsPerFile(long maxBinCount) {
        int numCacheFiles = (int) Math.ceil(maxBinCount / (float) DEFAULT_NUM_BINS_PER_FILE);
        numCacheFiles = Math.min(numCacheFiles, MAX_NUMBER_OF_CACHE_FILES);
        int binsPerFile = (int) Math.ceil(maxBinCount / (float) numCacheFiles);
        return Math.max(DEFAULT_NUM_BINS_PER_FILE, binsPerFile);
    }

    private void writeMapToFile(long fileIndex) throws IOException {
        if (!map.isEmpty()) {
            File file = getFile(fileIndex);
            writeToFile(map, file);
            map.clear();
        }
    }

    private void writeToFile(SortedMap<Long, List<SpatialBin>> map, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos, 5 * 1024 * 1024));
        try {
            writeToStream(map, dos);
        } finally {
            dos.close();
        }
    }

    private void readFromFile(long nextFileIndex) throws IOException {
        File file = getFile(nextFileIndex);
        if (file.exists()) {
            readIntoMap(file, map);
        } else {
            map.clear();
        }
    }

    private static void readIntoMap(File file, SortedMap<Long, List<SpatialBin>> map) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        DataInputStream dis = new DataInputStream(new BufferedInputStream(fis, 5 * 1024 * 1024));
        try {
            readFromStream(dis, map);
        } finally {
            dis.close();
        }
    }

    private File getFile(long fileIndex) throws IOException {
        return new File(tempDir, String.format(FILE_NAME_PATTERN, fileIndex));
    }

    private int calculateNextFileIndex(long binIndex) {
        return (int) (binIndex / numBinsPerFile);
    }

    private class FileBackedBinCollection implements SpatialBinCollection {


        private final long size;

        public FileBackedBinCollection(long size) {
            this.size = size;
        }

        @Override
        public Iterable<List<SpatialBin>> getBinCollection() {
            return new Iterable<List<SpatialBin>>() {
                @Override
                public Iterator<List<SpatialBin>> iterator() {
                    return new FileBackedBinIterator(tempDir);
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


        private class FileBackedBinIterator implements Iterator<List<SpatialBin>> {

            private final List<File> cacheFiles;
            private final List<List<SpatialBin>> currentList;

            private FileBackedBinIterator(File tempCacheDir) {
                this.cacheFiles = getCacheFiles(tempCacheDir);
                // We use a linked list here because we use the remove method, which is expensive for an ArrayList
                currentList = new LinkedList<List<SpatialBin>>();
            }

            @Override
            public boolean hasNext() {
                return !(currentList.isEmpty() && cacheFiles.isEmpty());
            }

            @Override
            public List<SpatialBin> next() {
                if (currentList.isEmpty()) {
                    File currentFile = cacheFiles.remove(0);
                    if (currentFile.exists()) {
                        try {
                            readIntoList(currentFile, currentList);
                        } catch (IOException e) {
                            throw new IllegalStateException(e.getMessage(), e);
                        }
                        if (!currentFile.delete()) {
                            currentFile.deleteOnExit();
                        }
                    }
                }
                return currentList.remove(0);

            }

            private List<File> getCacheFiles(File cacheFileDir) {
                File[] files = cacheFileDir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        String fileName = file.getName();
                        return file.isFile() && fileName.startsWith("bins-") && fileName.endsWith(".tmp");
                    }
                });
                List<File> fileList = new LinkedList<File>();
                Collections.addAll(fileList, files);
                Collections.sort(fileList);
                return fileList;

            }

            private void readIntoList(File file, List<List<SpatialBin>> lists) throws IOException {
                FileInputStream fis = new FileInputStream(file);
                DataInputStream dis = new DataInputStream(new BufferedInputStream(fis, 5 * 1024 * 1024));
                try {
                    readIntoList(dis, lists);
                } finally {
                    dis.close();
                }
            }

            private void readIntoList(DataInputStream dis, List<List<SpatialBin>> list) throws IOException {
                while (dis.available() != 0) {
                    long binIndex = dis.readLong();
                    int numBins = dis.readInt();
                    List<SpatialBin> spatialBins = new ArrayList<SpatialBin>(numBins);
                    for (int i = numBins; i > 0; i--) {
                        spatialBins.add(SpatialBin.read(binIndex, dis));
                    }
                    list.add(spatialBins);
                }
            }


            @Override
            public void remove() {
                // nothing to do
            }
        }
    }

}
