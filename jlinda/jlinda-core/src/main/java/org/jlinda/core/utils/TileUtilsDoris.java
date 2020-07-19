package org.jlinda.core.utils;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.engine_utilities.gpf.TileIndex;
import org.jblas.ComplexDouble;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.FloatMatrix;

import java.awt.*;

/**
* User: pmar@ppolabs.com
* Date: 6/20/11
* Time: 11:19 PM
*/
public class TileUtilsDoris {

    // see javadoc for Tile (interface)
    public static ComplexDoubleMatrix pullComplexDoubleMatrix(final Tile tile1, final Tile tile2) {

        final int height = tile1.getHeight();
        final int width = tile1.getWidth();
        final ComplexDoubleMatrix result = new ComplexDoubleMatrix(height, width);

        final ProductData samples1 = tile1.getRawSamples();

        if (tile2 != null) {
            final ProductData samples2 = tile2.getRawSamples();

            for (int y = 0; y < height; y++) {
                final int stride = y * width;
                for (int x = 0; x < width; x++) {
                    result.put(y, x, new ComplexDouble(samples1.getElemDoubleAt(stride + x),
                            samples2.getElemDoubleAt(stride + x)));
                }
            }
        } else {
            for (int y = 0; y < height; y++) {
                final int stride = y * width;
                for (int x = 0; x < width; x++) {
                    result.put(y, x, new ComplexDouble(samples1.getElemDoubleAt(stride + x), 0.0));
                }
            }
        }

        //samples1.dispose();
        //samples2.dispose();
        return result;
    }

    // see javadoc for Tile (interface)
    public static DoubleMatrix pullDoubleMatrix(final Tile tile) {

        final int height = tile.getHeight();
        final int width = tile.getWidth();
        final DoubleMatrix result = new DoubleMatrix(height, width);

        final ProductData samples = tile.getRawSamples();

        for (int y = 0; y < height; y++) {
            final int stride = y * width;
            for (int x = 0; x < width; x++) {
                result.put(y, x, samples.getElemDoubleAt(stride + x));
            }
        }

        samples.dispose();

        return result;
    }

    public static void pushFloatMatrix(FloatMatrix data, Tile tile, Rectangle rect) {
        ProductData samples = tile.getRawSamples(); // checkout
        final int width = tile.getWidth();
        for (int y = 0, rowIdx = 0; y < rect.getHeight(); y++, rowIdx++) {
            final int stride = y * width;
            for (int x = 0, columnIdx = 0; x < rect.getWidth(); x++, columnIdx++) {
                samples.setElemFloatAt(stride + x, data.get(rowIdx, columnIdx));
            }
        }
        tile.setRawSamples(samples); // commit
        samples.dispose();
    }

    public static void pushFloatMatrix(DoubleMatrix data, Tile tile, Rectangle rect) {

        final ProductData samples = tile.getRawSamples(); // checkout
        final int width = (int) rect.getWidth();

        for (int y = 0, rowIdx = 0; y < rect.getHeight(); y++, rowIdx++) {
            final int stride = y * width;
            for (int x = 0, columnIdx = 0; x < rect.getWidth(); x++, columnIdx++) {
                samples.setElemFloatAt(stride + x, (float) data.get(rowIdx, columnIdx));
            }
        }
        tile.setRawSamples(samples); // commit
        samples.dispose();
    }

    public static void pushDoubleMatrix(final DoubleMatrix data, final Tile tile, final Rectangle rect) {

        final int maxX = rect.x + rect.width;
        final int maxY = rect.y + rect.height;
        final ProductData samples = tile.getDataBuffer();
        final TileIndex tgtIndex = new TileIndex(tile);
        for (int y = rect.y; y < maxY; y++) {
            tgtIndex.calculateStride(y);
            final int yy = y - rect.y;
            for (int x = rect.x; x < maxX; x++) {
                samples.setElemFloatAt(tgtIndex.getIndex(x), (float)data.get(yy, x - rect.x));
            }
        }
    }

    public static void pushDoubleMatrix(DoubleMatrix data, Tile tile, Rectangle rect, int y0, int x0) {

        final ProductData samples = tile.getRawSamples(); // checkout
        final int width = (int) rect.getWidth();

        for (int y = 0, rowIdx = y0; y < rect.getHeight(); y++, rowIdx++) {
            final int stride = y * width;
            for (int x = 0, columnIdx = x0; x < rect.getWidth(); x++, columnIdx++) {
                samples.setElemDoubleAt(stride + x, data.get(rowIdx, columnIdx));
            }
        }
        tile.setRawSamples(samples); // commit
        samples.dispose();
    }

    public static void pushDoubleArray2D(double[][] data, Tile tile, Rectangle rect) {

        /*
        final ProductData samples = tile.getRawSamples(); // checkout
        final int width = (int) rect.getWidth();

        for (int y = 0, rowIdx = 0; y < rect.getHeight(); y++, rowIdx++) {
            final int stride = y * width;
            for (int x = 0, columnIdx = 0; x < rect.getWidth(); x++, columnIdx++) {
                samples.setElemDoubleAt(stride + x, data[rowIdx][columnIdx]);
            }
        }
        tile.setRawSamples(samples); // commit
        samples.dispose();
        */
        final int maxX = rect.x + rect.width;
        final int maxY = rect.y + rect.height;
        final ProductData samples = tile.getDataBuffer();
        final TileIndex tgtIndex = new TileIndex(tile);
        for (int y = rect.y; y < maxY; y++) {
            tgtIndex.calculateStride(y);
            final int yy = y - rect.y;
            for (int x = rect.x; x < maxX; x++) {
                samples.setElemFloatAt(tgtIndex.getIndex(x), (float)data[yy][x - rect.x]);
            }
        }
    }

    public static void pushDoubleMatrix(FloatMatrix data, Tile tile, Rectangle rect) {

        final ProductData samples = tile.getRawSamples(); // checkout
        final int width = (int) rect.getWidth();

        for (int y = 0, rowIdx = 0; y < rect.getHeight(); y++, rowIdx++) {
            final int stride = y * width;
            for (int x = 0, columnIdx = 0; x < rect.getWidth(); x++, columnIdx++) {
                samples.setElemDoubleAt(stride + x, (double) data.get(rowIdx, columnIdx));
            }
        }
        tile.setRawSamples(samples); // commit
        samples.dispose();
    }

    public static void pushComplexFloatMatrix(ComplexDoubleMatrix cplxData, Tile tileReal, Tile tileImag, Rectangle rect) {

        // TOO SLOW?
        final int width = (int) rect.getWidth();
        final int height = (int) rect.getHeight();
        final ProductData samplesReal = tileReal.getRawSamples(); // checkout
        final ProductData samplesImag = tileImag.getRawSamples(); // checkout

        int arrayIdx;
        int row = 0;
        int column;

        for (int y = 0; y < height; y++) {
            column = 0;
            for (int x = 0; x < width; x++) {
                arrayIdx = y * width + x;
                samplesReal.setElemFloatAt(arrayIdx, (float) cplxData.real().get(row, column));
                samplesImag.setElemFloatAt(arrayIdx, (float) cplxData.imag().get(row, column));
                column++;
            }
            row++;
        }

        tileReal.setRawSamples(samplesReal); // commit
        tileImag.setRawSamples(samplesImag); // commit

        samplesImag.dispose();
        samplesReal.dispose();

    }

    public static void pushComplexDoubleMatrix(
            final ComplexDoubleMatrix data, final Tile tileI, final Tile tileQ, final Rectangle rect) {

        final int maxX = rect.x + rect.width;
        final int maxY = rect.y + rect.height;

        final ProductData samplesReal = tileI.getDataBuffer();
        final ProductData samplesImag = tileQ.getDataBuffer();
        final DoubleMatrix dataReal = data.real();
        final DoubleMatrix dataImag = data.imag();

        final TileIndex tgtIndex = new TileIndex(tileI);
        for (int y = rect.y; y < maxY; y++) {
            tgtIndex.calculateStride(y);
            final int yy = y - rect.y;
            for (int x = rect.x; x < maxX; x++) {
                final int xx = x - rect.x;
                samplesReal.setElemFloatAt(tgtIndex.getIndex(x), (float)dataReal.get(yy, xx));
                samplesImag.setElemFloatAt(tgtIndex.getIndex(x), (float)dataImag.get(yy, xx));
            }
        }
    }
}
