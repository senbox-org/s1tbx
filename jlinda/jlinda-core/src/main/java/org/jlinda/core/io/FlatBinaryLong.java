package org.jlinda.core.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;

public final class FlatBinaryLong extends FlatBinary {

    private long[][] data;
    private int lines;
    private int pixels;

    public FlatBinaryLong() {
        this.byteOrder = ByteOrder.BIG_ENDIAN;
    }

    public void setData(long[][] data) {
        this.data = data;
    }

    public long[][] getData() {
        return data;
    }

    private void setLinesPixels() {
        lines = (int) dataWindow.lines();
        pixels = (int) dataWindow.pixels();
    }

    @Override
    public void readFromStream() throws FileNotFoundException {

        setLinesPixels();

        data = new long[lines][pixels];
        for (int i = 0; i < lines; i++) {
            for (int j = 0; j < pixels; j++) {
                try {
                    if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                        data[i][j] = ByteSwapper.swap(inStream.readLong());
                    } else {
                        data[i][j] = inStream.readLong();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    @Override
    public void writeToStream() throws FileNotFoundException {

        setLinesPixels();

        for (int i = 0; i < lines; i++) {
            for (int j = 0; j < pixels; j++) {
                try {
                    if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                        outStream.writeLong(ByteSwapper.swap(data[i][j]));
                    } else {
                        outStream.writeLong(data[i][j]);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            this.outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
