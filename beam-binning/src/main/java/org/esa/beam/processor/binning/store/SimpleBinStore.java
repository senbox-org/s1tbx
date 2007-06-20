package org.esa.beam.processor.binning.store;

import java.io.File;
import java.io.IOException;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.esa.beam.processor.binning.database.Bin;
import org.esa.beam.processor.binning.database.BinLocator;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 15.07.2005
 * Time: 15:02:47
 * To change this template use File | Settings | File Templates.
 */
public class SimpleBinStore extends AbstractLinearBinStore {
    // the number of floats to be written at once during initialization
    private static final int numInitFloats = 32768;

    private float[] data;
    private float[] tempBinContent;

    private int numVarsPerBin;
    private File dbFile;
    private boolean dataChanged = false;

    /**
     * Open a new SimpleBinStore.
     *
     * @param dbDir
     * @param dbName
     * @param locator
     * @param numVarsPerBin
     */
    public SimpleBinStore(File dbDir, String dbName, BinLocator locator, int numVarsPerBin) throws IOException {
        this.locator = locator;
        this.numVarsPerBin = numVarsPerBin;
        dbFile = new File(dbDir, dbName);
        dbFile.createNewFile();

        final int numFloats = locator.getNumCells() * numVarsPerBin;

        tempBinContent = new float[numVarsPerBin];
        data = new float[numFloats];
        dataChanged = true;
    }

    /**
     * Create a existing SimpleBinStore.
     *
     * @param dbDir
     * @param dbName
     * @param locator
     * @throws IOException
     */
    public SimpleBinStore(File dbDir, String dbName, BinLocator locator) throws IOException {
        this.locator = locator;
        dbFile = new File(dbDir, dbName);
        if(!dbFile.exists()) {
            throw new IOException("The specified datbase is missing: " + dbFile.getPath());
        }
        if (!dbFile.isFile()) {
            throw new IOException("The database location passed in is not a file" +  dbFile.getPath());
        }

        ImageInputStream inStream = new FileImageInputStream(dbFile);
        final int numFloats = (int) inStream.length() / (Float.SIZE / 8);
        numVarsPerBin = numFloats / locator.getNumCells();

        tempBinContent = new float[numVarsPerBin];
        data = new float[numFloats];

        inStream.readFully(data, 0, numFloats);
        inStream.close();
    }

    public void write(int index, Bin bin) {
        bin.save(tempBinContent);
        System.arraycopy(tempBinContent, 0, data, index * numVarsPerBin, numVarsPerBin);
        dataChanged = true;
    }

    public void read(int index, Bin bin) {
        System.arraycopy(data, index * numVarsPerBin, tempBinContent, 0, numVarsPerBin);
        bin.load(tempBinContent);
    }

    public void flush() throws IOException {
        if (dataChanged && data != null) {
            ImageOutputStream outStream = new FileImageOutputStream(dbFile);

            int toGo = data.length;
            int toWrite = numInitFloats;
            int offset = 0;

            while (toGo > 0) {
                if (toGo < numInitFloats) {
                    toWrite = toGo;
                }
                outStream.writeFloats(data, offset, toWrite);
                offset += toWrite;
                toGo -= toWrite;
            }
            outStream.close();
            dataChanged = false;
        }
    }

    public void close() throws IOException {
        flush();
        data = null;
    }

    public void delete() throws IOException {
        close();
        dbFile.delete();
    }
}
