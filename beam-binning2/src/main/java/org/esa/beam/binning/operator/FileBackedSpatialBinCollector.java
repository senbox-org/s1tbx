package org.esa.beam.binning.operator;

import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.SpatialBin;
import org.esa.beam.util.io.FileUtils;

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

class FileBackedSpatialBinCollector implements SpatialBinCollector {

    private final static int NUM_BINS_PER_FILE = 10000;

    private static final File DEFAULT_TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));
    private static final File TEMP_DIRECTORY = new File(DEFAULT_TEMP_DIR, "beam-spatial-binning");
    private static final String FILE_NAME_PATTERN = "bins-%08d.tmp";
    private long lastFileIndex;
    private SortedMap<Long, List<SpatialBin>> map;
    private boolean consumingCompleted;
    private final TreeSet<Long> binIndexSet;

    public FileBackedSpatialBinCollector() throws Exception {
        FileUtils.deleteTree(TEMP_DIRECTORY);
        if (!TEMP_DIRECTORY.exists() && !TEMP_DIRECTORY.mkdir()) {
            throw new IOException("Could not create temporary directory.");
        }
        lastFileIndex = -1;
        consumingCompleted = false;
        binIndexSet = new TreeSet<Long>();
    }

    @Override
    public void consumeSpatialBins(BinningContext ignored, List<SpatialBin> spatialBins) throws Exception {
        if (consumingCompleted) {
            throw new IllegalStateException("Consuming of bins has already been completed.");
        }

        for (SpatialBin spatialBin : spatialBins) {
            long spatialBinIndex = spatialBin.getIndex();
            long currentFileIndex = calculateFileIndex(spatialBinIndex);
            if (currentFileIndex != lastFileIndex) {
                // write map back to file, if exists
                if (map != null) {
                    File file = getFile(lastFileIndex);
                    writeToFile(map, file);
                    map.clear();
                }

                File file = getFile(currentFileIndex);
                if (file.exists()) {
                    map = readIntoMap(file);
                } else {
                    map = new TreeMap<Long, List<SpatialBin>>();
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

    @Override
    public void consumingCompleted() throws IOException {
        consumingCompleted = true;
        if (map != null) {
            File file = getFile(lastFileIndex);
            writeToFile(map, file);
            map.clear();
        }
        map = null;
    }

    @Override
    public SpatialBinCollection getSpatialBinCollection() throws IOException {
        return new LazyBinCollection(binIndexSet);
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

    private static SortedMap<Long, List<SpatialBin>> readIntoMap(File file) throws IOException {
        TreeMap<Long, List<SpatialBin>> map = new TreeMap<Long, List<SpatialBin>>();
        FileInputStream fis = new FileInputStream(file);
        DataInputStream dis = new DataInputStream(new BufferedInputStream(fis, 1024 * 1024));
        try {
            readFromStream(dis, map);
        } finally {
            dis.close();
        }
        return map;
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

    static void readFromStream(DataInputStream dis, TreeMap<Long, List<SpatialBin>> map) throws IOException {
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

    public static File getFile(long fileIndex) throws IOException {
        return new File(TEMP_DIRECTORY, String.format(FILE_NAME_PATTERN, fileIndex));
    }

    private static long calculateFileIndex(long binIndex) {
        return binIndex / NUM_BINS_PER_FILE;
    }

    private static class LazyBinCollection implements SpatialBinCollection {

        private TreeSet<Long> binSet;

        public LazyBinCollection(TreeSet<Long> binSet) {
            this.binSet = binSet;
        }

        @Override
        public Iterable<List<SpatialBin>> getCollectedBins() {
            return new Iterable<List<SpatialBin>>() {
                @Override
                public Iterator<List<SpatialBin>> iterator() {
                    return new BinIterator();
                }
            };
        }

        @Override
        public long size() {
            return binSet.size();
        }

        @Override
        public boolean isEmpty() {
            return binSet.isEmpty();
        }

        @Override
        public void clear() {
            // nothing to do
        }

        private class BinIterator implements Iterator<List<SpatialBin>> {

            //            private long currentBinIndex;
            private long lastFileIndex = -1;
            private SortedMap<Long, List<SpatialBin>> currentMap;
            private Iterator<Long> binIterator;

            private BinIterator() {
                binIterator = binSet.iterator();
            }

            @Override
            public boolean hasNext() {
                return binIterator.hasNext();
            }

            @Override
            public List<SpatialBin> next() {
                long currentBinIndex = binIterator.next();
                try {
                    long currentFileIndex = calculateFileIndex(currentBinIndex);
                    if (currentFileIndex != lastFileIndex) {
                        File currentFile = FileBackedSpatialBinCollector.getFile(currentFileIndex);
                        currentMap = readIntoMap(currentFile);
                        File lastFile = FileBackedSpatialBinCollector.getFile(lastFileIndex);
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

            @Override
            public void remove() {
                // nothing to do
            }
        }
    }
}
