package org.jlinda.core.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;

public final class FlatBinaryDouble extends FlatBinary {

    private double [][] data;
    private int lines;
    private int pixels;

    public FlatBinaryDouble() {
        this.byteOrder = ByteOrder.BIG_ENDIAN;
    }

    public void setData(double[][] data) {
        this.data = data;
    }

    public double[][] getData() {
        return data;
    }

    private void setLinesPixels() {
        lines = (int) dataWindow.lines();
        pixels = (int) dataWindow.pixels();
    }

    @Override
    public void readFromStream() throws FileNotFoundException {

        setLinesPixels();

        data = new double[lines][pixels];
        for (int i = 0; i < lines; i++) {
            for (int j = 0; j < pixels; j++) {
                try {
                    if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                        data[i][j] = ByteSwapper.swap(inStream.readDouble());
                    } else {
                        data[i][j] = inStream.readDouble();
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
                        outStream.writeDouble(ByteSwapper.swap(data[i][j]));
                    } else {
                        outStream.writeDouble(data[i][j]);
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
