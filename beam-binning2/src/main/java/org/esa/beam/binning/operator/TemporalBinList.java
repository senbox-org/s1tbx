package org.esa.beam.binning.operator;

import org.esa.beam.binning.TemporalBin;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Marco Peters
 */
class TemporalBinList extends AbstractList<TemporalBin> {

    public static final int DEFAULT_MAX_CACHE_FILES = 100;
    public static final int DEFAULT_BINS_PER_FILE = 10000;

    private static final File DEFAULT_TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));
    private static final File TEMP_DIRECTORY = new File(DEFAULT_TEMP_DIR, "beam-temporal-binning");
    private static final String FILE_NAME_PATTERN = "temporal-bins-%03d.tmp";


    private final long numberOfBins;
    private final int binsPerFile;

    private int size;
    private int lastFileIndex;
    private ArrayList<TemporalBin> currentBinList;
    private Logger logger = BeamLogManager.getSystemLogger();

    public TemporalBinList(int numberOfBins) throws IOException {
        this(numberOfBins, DEFAULT_MAX_CACHE_FILES, DEFAULT_BINS_PER_FILE);
    }

    TemporalBinList(int numberOfBins, int maxNumberOfCacheFiles, int preferredBinsPerFile) throws IOException {
        if (!TEMP_DIRECTORY.exists() && !TEMP_DIRECTORY.mkdir()) {
            throw new IOException("Could not create temporary directory.");
        }
        clearDirectory(TEMP_DIRECTORY);
        this.numberOfBins = numberOfBins;
        binsPerFile = computeBinsPerFile(numberOfBins, maxNumberOfCacheFiles, preferredBinsPerFile);
        currentBinList = new ArrayList<TemporalBin>();
        size = 0;
        lastFileIndex = -1;
    }

    @Override
    public boolean add(TemporalBin temporalBin) {
        if (size >= numberOfBins) {
            throw new IllegalStateException("Number of add operation exceeds maximum number of bins");
        }
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
        size++;
        return true;
    }

    @Override
    public TemporalBin get(int index) {
        if (index >= numberOfBins) {
            throw new IllegalStateException("Number of add operation exceeds maximum number of bins");
        }
        try {
            int currentFileIndex = calculateFileIndex(index);
            if (currentFileIndex != lastFileIndex) {
                writeBinList(lastFileIndex, currentBinList);
                currentBinList.clear();
                readBinList(currentFileIndex, currentBinList);
                lastFileIndex = currentFileIndex;
            }

            int fileBinOffset = binsPerFile * currentFileIndex;
            return currentBinList.get(index - fileBinOffset);

        } catch (IOException e) {
            logger.log(Level.SEVERE, String.format("Error getting temporal bin at index %d.", index), e);
        }
        return null;
    }

    @Override
    public int size() {
        return size;
    }

    static int computeBinsPerFile(int numberOfBins, int maxNumberOfCacheFiles, int preferredBinsPerFile) {
        int numCacheFiles = (int) Math.ceil(numberOfBins / (float) preferredBinsPerFile);
        numCacheFiles = Math.min(numCacheFiles, maxNumberOfCacheFiles);
        int binsPerFile = (int) Math.ceil(numberOfBins / (float) numCacheFiles);
        return binsPerFile < preferredBinsPerFile ? preferredBinsPerFile : binsPerFile;
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

    private int calculateFileIndex(int index) {
        return index / binsPerFile;
    }

    private File getFile(int fileIndex) throws IOException {
        return new File(TEMP_DIRECTORY, String.format(FILE_NAME_PATTERN, fileIndex));
    }

    private void writeBinList(int currentFileIndex, ArrayList<TemporalBin> binList) throws IOException {
        File file = getFile(currentFileIndex);
        writeToFile(binList, file);
    }

    private void readBinList(int currentFileIndex, ArrayList<TemporalBin> binList) throws IOException {
        File file = getFile(currentFileIndex);
        if (file.exists()) {
            readFromFile(file, binList);
        }
    }

    private static void writeToFile(ArrayList<TemporalBin> temporalBins, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos, 1024 * 1024));
        try {
            for (TemporalBin bin : temporalBins) {
                dos.writeLong(bin.getIndex());
                bin.write(dos);
            }
        } finally {
            dos.close();
        }
    }

    private static void readFromFile(File file, ArrayList<TemporalBin> temporalBins) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        DataInputStream dis = new DataInputStream(new BufferedInputStream(fis, 1024 * 1024));
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
