package org.esa.beam.binning.operator;

import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.SpatialBin;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An implementation of {@link SpatialBinCollector} which stores the consumed
 * {@link SpatialBin spatial bins} into multiple files.
 *
 * @see MapBackedSpatialBinCollector
 * @see GeneralSpatialBinCollector
 */
class FileBackedSpatialBinCollector implements SpatialBinCollector {

    private static final int NUM_BINS_PER_FILE = 100000;
    private static final int MAX_NUMBER_OF_CACHE_FILES = 100;
    private static final File DEFAULT_TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));
    private static final File TEMP_DIRECTORY = new File(DEFAULT_TEMP_DIR, "beam-spatial-binning");
    private static final String FILE_NAME_PATTERN = "bins-%03d.tmp";

    private final SortedMap<Long, List<SpatialBin>> map;
    private final TreeSet<Long> binIndexSet;
    private final AtomicBoolean consumingCompleted;
    private long lastFileIndex;
    private long maximumNumberOfBins;

    public FileBackedSpatialBinCollector() throws Exception {
        if (!TEMP_DIRECTORY.exists() && !TEMP_DIRECTORY.mkdir()) {
            throw new IOException("Could not create temporary directory.");
        }
        clearDirectory(TEMP_DIRECTORY);
        binIndexSet = new TreeSet<Long>();
        map = new TreeMap<Long, List<SpatialBin>>();
        consumingCompleted = new AtomicBoolean(false);
        lastFileIndex = -1;
        maximumNumberOfBins = -1;
    }

    public void setMaximumNumberOfBins(long numBins) {
        maximumNumberOfBins = numBins;
    }

    @Override
    public void consumeSpatialBins(BinningContext ignored, List<SpatialBin> spatialBins) throws Exception {
        if (consumingCompleted.get()) {
            throw new IllegalStateException("Consuming of bins has already been completed.");
        }
        if (maximumNumberOfBins == -1) {
            throw new IllegalStateException("Maximum number of bins has to be set before consuming bins.");
        }
        synchronized (map) {
            for (SpatialBin spatialBin : spatialBins) {
                long spatialBinIndex = spatialBin.getIndex();
                int currentFileIndex = calculateFileIndex(spatialBinIndex, maximumNumberOfBins);
                if (currentFileIndex != lastFileIndex) {
                    // write map back to file, if it contains data
                    if (!map.isEmpty()) {
                        File file = getFile(lastFileIndex);
                        writeToFile(map, file);
                        map.clear();
                    }

                    File file = getFile(currentFileIndex);
                    if (file.exists()) {
                        readIntoMap(file, map);
                    } else {
                        map.clear();
                    }
                    lastFileIndex = currentFileIndex;
                }
                binIndexSet.add(spatialBinIndex);
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
            if (!map.isEmpty()) {
                File file = getFile(lastFileIndex);
                writeToFile(map, file);
                map.clear();
            }
        }
    }

    @Override
    public SpatialBinCollection getSpatialBinCollection() throws IOException {
        return new FileBackedBinCollection(binIndexSet);
    }


    private void writeToFile(SortedMap<Long, List<SpatialBin>> map, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos, 1024 * 1024));
        try {
            writeToStream(map, dos);
        } finally {
            dos.close();
        }
    }

    private static void readIntoMap(File file, SortedMap<Long, List<SpatialBin>> map) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        DataInputStream dis = new DataInputStream(new BufferedInputStream(fis, 1024 * 1024));
        try {
            readFromStream(dis, map);
        } finally {
            dis.close();
        }
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

    private void clearDirectory(File tempDirectory) {
        File[] files = tempDirectory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.delete()) {
                try {
                    String msgPattern = "Could not delete temporary binning file '%s'.";
                    BeamLogManager.getSystemLogger().warning(String.format(msgPattern, file.getCanonicalPath()));
                } catch (IOException e) {
                    // should not happen - no special handling
                    e.printStackTrace();
                }
            }
        }
    }

    private static File getFile(long fileIndex) throws IOException {
        return new File(TEMP_DIRECTORY, String.format(FILE_NAME_PATTERN, fileIndex));
    }

    private static int calculateFileIndex(long binIndex, long maximumNumberOfBins) {
        int numBinsPerFile = getNumBinsPerFile(maximumNumberOfBins);
        return (int) (binIndex / numBinsPerFile);
    }

    private static int getNumBinsPerFile(long maxBinCount) {
        int numCacheFiles = (int) Math.ceil(maxBinCount / NUM_BINS_PER_FILE);
        numCacheFiles = Math.min(numCacheFiles, MAX_NUMBER_OF_CACHE_FILES);
        int binsPerFile = (int) Math.ceil(maxBinCount / (float) numCacheFiles);
        return binsPerFile < NUM_BINS_PER_FILE ? NUM_BINS_PER_FILE : binsPerFile;
    }

    private static class FileBackedBinCollection implements SpatialBinCollection {

        private TreeSet<Long> binIndexSet;

        public FileBackedBinCollection(TreeSet<Long> binIndexSet) {
            this.binIndexSet = binIndexSet;
        }

        @Override
        public Iterable<List<SpatialBin>> getCollectedBins() {
            return new Iterable<List<SpatialBin>>() {
                @Override
                public Iterator<List<SpatialBin>> iterator() {
                    return new FileBackedBinIterator(binIndexSet.iterator(), size());
                }
            };
        }

        @Override
        public long size() {
            return binIndexSet.size();
        }

        @Override
        public boolean isEmpty() {
            return binIndexSet.isEmpty();
        }

        @Override
        public void clear() {
            // nothing to do
        }

        private class FileBackedBinIterator implements Iterator<List<SpatialBin>> {

            private final Iterator<Long> binIterator;
            private final long maxBinCount;
            private final SortedMap<Long, List<SpatialBin>> currentMap;
            private int lastFileIndex = -1;
            private long currentBinIndex;

            private FileBackedBinIterator(Iterator<Long> iterator, long maxBinCount) {
                binIterator = iterator;
                this.maxBinCount = maxBinCount;
                currentMap = new TreeMap<Long, List<SpatialBin>>();
            }

            @Override
            public boolean hasNext() {
                return binIterator.hasNext();
            }

            @Override
            public List<SpatialBin> next() {
                currentBinIndex = getNextExistingBinIndex();
                try {
                    int currentFileIndex = calculateFileIndex(currentBinIndex, maxBinCount);
                    if (currentFileIndex != lastFileIndex) {
                        File currentFile = getFile(currentFileIndex);
                        currentMap.clear();
                        readIntoMap(currentFile, currentMap);
                        File lastFile = getFile(lastFileIndex);
                        if (!lastFile.delete()) {
                            lastFile.deleteOnExit();
                        }
                    }
                    lastFileIndex = currentFileIndex;
                } catch (IOException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }

                return currentMap.get(currentBinIndex);
            }

            private Long getNextExistingBinIndex() {
                return binIterator.next();
            }

            @Override
            public void remove() {
                // nothing to do
            }
        }
    }
}
