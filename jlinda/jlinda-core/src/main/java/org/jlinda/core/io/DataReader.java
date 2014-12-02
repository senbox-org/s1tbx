package org.jlinda.core.io;

import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.FloatMatrix;
import org.jlinda.core.Window;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteOrder;

public class DataReader {

    public static ComplexDoubleMatrix readCplxDoubleData(final String fileName, final int rows, final int columns, final ByteOrder byteOrder) throws FileNotFoundException {

        final FlatBinaryDouble inRealFile = new FlatBinaryDouble();
        inRealFile.setFile(new File(fileName));
        inRealFile.setByteOrder(byteOrder);
        inRealFile.setDataWindow(new Window(0, (rows - 1), 0, (2 * columns - 1)));
        inRealFile.setInStream();
        inRealFile.readFromStream();

        // parse data from :: assume it is stored in "major-row order"
        DoubleMatrix realData = new DoubleMatrix(rows, columns);
        DoubleMatrix imgData = new DoubleMatrix(rows, columns);
        final double[][] data = inRealFile.getData();
        int cnt;
        for (int i = 0; i < rows; i++) {
            cnt = 0;
            for (int j = 0; j < 2 * columns; j = j + 2) {
                realData.put(i, cnt, data[i][j]);
                imgData.put(i, cnt, data[i][j+1]);
                cnt++;
            }
        }

        return new ComplexDoubleMatrix(realData, imgData);

    }

    public static ComplexDoubleMatrix readCplxDoubleData(String fileName, int rows, int columns) throws FileNotFoundException {
        return readCplxDoubleData(fileName, rows, columns, ByteOrder.BIG_ENDIAN);
    }

    public static FloatMatrix readFloatData(final String fileName, final int rows, final int columns, final ByteOrder byteOrder) throws FileNotFoundException {
        final FlatBinaryFloat inRealFile = new FlatBinaryFloat();
        inRealFile.setFile(new File(fileName));
        inRealFile.setByteOrder(byteOrder);
        inRealFile.setDataWindow(new Window(0, rows - 1, 0, columns - 1));
        inRealFile.setInStream();
        inRealFile.readFromStream();

        return new FloatMatrix(inRealFile.getData());
    }

    public static FloatMatrix readFloatData(final String fileName, final int rows, final int columns) throws FileNotFoundException {
        return readFloatData(fileName, rows, columns, ByteOrder.BIG_ENDIAN);
    }

    public static DoubleMatrix readDoubleData(final String fileName, final int rows, final int columns, final ByteOrder byteOrder) throws FileNotFoundException {
        final FlatBinaryDouble inRealFile = new FlatBinaryDouble();
        inRealFile.setFile(new File(fileName));
        inRealFile.setByteOrder(byteOrder);
        inRealFile.setDataWindow(new Window(0, rows - 1, 0, columns - 1));
        inRealFile.setInStream();
        inRealFile.readFromStream();

        return new DoubleMatrix(inRealFile.getData());
    }

    public static DoubleMatrix readDoubleData(final String fileName, final int rows, final int columns) throws FileNotFoundException {
        return readDoubleData(fileName, rows, columns, ByteOrder.BIG_ENDIAN);
    }

    public static ComplexDoubleMatrix readCplxFloatData(final String fileName, final int rows, final int columns, final ByteOrder byteOrder) throws FileNotFoundException {

        final FlatBinaryFloat inRealFile = new FlatBinaryFloat();
        inRealFile.setFile(new File(fileName));
        inRealFile.setByteOrder(byteOrder);
        inRealFile.setDataWindow(new Window(0, (rows - 1), 0, (2 * columns - 1)));
        inRealFile.setInStream();
        inRealFile.readFromStream();

        // parse data from :: assume it is stored in "major-row order"
        DoubleMatrix realData = new DoubleMatrix(rows, columns);
        DoubleMatrix imgData = new DoubleMatrix(rows, columns);
        final float[][] data = inRealFile.getData();
        int cnt;
        for (int i = 0; i < rows; i++) {
            cnt = 0;
            for (int j = 0; j < 2 * columns; j = j + 2) {
                realData.put(i, cnt, data[i][j]);
                imgData.put(i, cnt, data[i][j + 1]);
                cnt++;
            }
        }

        return new ComplexDoubleMatrix(realData, imgData);

    }

    public static ComplexDoubleMatrix readCplxFloatData(final String fileName, final int rows, final int columns) throws FileNotFoundException {
        return readCplxFloatData(fileName, rows, columns, ByteOrder.BIG_ENDIAN);
    }

}
